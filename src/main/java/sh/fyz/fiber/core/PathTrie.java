package sh.fyz.fiber.core;

import java.util.HashMap;
import java.util.Map;

/**
 * Path-segment trie used by the router for dynamic routes (those containing
 * {@code {param}} or wildcards). Lookup is O(P) where P = number of path segments,
 * independent of the number of registered endpoints.
 *
 * <p>Three kinds of segments are recognised:
 * <ul>
 *     <li><b>literal</b> — exact match (e.g. {@code users})</li>
 *     <li><b>parameter</b> — {@code {name}} captures one segment</li>
 *     <li><b>wildcard</b> — {@code *} matches the rest of the path (greedy)</li>
 * </ul>
 *
 * <p>The trie stores values keyed by HTTP method, so lookup is method-aware.
 * Static routes (no {@code {} or *}) should be kept in the existing flat
 * {@code ConcurrentHashMap} for O(1) lookup; only dynamic routes are added here.
 */
public class PathTrie<V> {

    private static final class Node<V> {
        Map<String, Node<V>> literalChildren;
        Node<V> paramChild;
        String paramName;
        Node<V> wildcardChild;
        Map<String, V> values;
    }

    private final Node<V> root = new Node<>();

    /**
     * Add a route. {@code path} is the registered pattern (with leading "/"), and
     * {@code httpMethod} is e.g. {@code "GET"}. Existing entries with the same key
     * are silently replaced.
     */
    public synchronized void add(String path, String httpMethod, V value) {
        Node<V> node = root;
        for (String segment : split(path)) {
            if (segment.equals("*")) {
                if (node.wildcardChild == null) {
                    node.wildcardChild = new Node<>();
                }
                node = node.wildcardChild;
            } else if (segment.startsWith("{") && segment.endsWith("}")) {
                if (node.paramChild == null) {
                    node.paramChild = new Node<>();
                    node.paramName = segment.substring(1, segment.length() - 1);
                }
                node = node.paramChild;
            } else {
                if (node.literalChildren == null) {
                    node.literalChildren = new HashMap<>();
                }
                node = node.literalChildren.computeIfAbsent(segment, k -> new Node<>());
            }
        }
        if (node.values == null) {
            node.values = new HashMap<>();
        }
        node.values.put(httpMethod, value);
    }

    /**
     * Lookup the most-specific match for {@code path} + {@code httpMethod}. Literal
     * matches are preferred over parameter matches, which are preferred over wildcard
     * matches.
     */
    public V find(String path, String httpMethod) {
        return find(root, split(path), 0, httpMethod);
    }

    private V find(Node<V> node, String[] segments, int idx, String httpMethod) {
        if (idx == segments.length) {
            if (node.values != null) {
                return node.values.get(httpMethod);
            }
            return null;
        }
        String segment = segments[idx];

        if (node.literalChildren != null) {
            Node<V> child = node.literalChildren.get(segment);
            if (child != null) {
                V hit = find(child, segments, idx + 1, httpMethod);
                if (hit != null) return hit;
            }
        }
        if (node.paramChild != null) {
            V hit = find(node.paramChild, segments, idx + 1, httpMethod);
            if (hit != null) return hit;
        }
        if (node.wildcardChild != null && node.wildcardChild.values != null) {
            V hit = node.wildcardChild.values.get(httpMethod);
            if (hit != null) return hit;
        }
        return null;
    }

    private static String[] split(String path) {
        if (path == null || path.isEmpty() || path.equals("/")) return new String[0];
        String stripped = path.startsWith("/") ? path.substring(1) : path;
        if (stripped.endsWith("/")) {
            stripped = stripped.substring(0, stripped.length() - 1);
        }
        if (stripped.isEmpty()) return new String[0];
        return stripped.split("/");
    }
}
