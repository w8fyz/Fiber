package sh.fyz.fiber.unit;

import org.junit.jupiter.api.Test;
import sh.fyz.fiber.core.PathTrie;

import static org.junit.jupiter.api.Assertions.*;

class PathTrieTest {

    @Test
    void literalRouteIsPreferredOverParam() {
        PathTrie<String> trie = new PathTrie<>();
        trie.add("/users/{id}", "GET", "param");
        trie.add("/users/me", "GET", "literal");

        assertEquals("literal", trie.find("/users/me", "GET"));
        assertEquals("param", trie.find("/users/42", "GET"));
    }

    @Test
    void paramRouteIsPreferredOverWildcard() {
        PathTrie<String> trie = new PathTrie<>();
        trie.add("/files/*", "GET", "wildcard");
        trie.add("/files/{name}", "GET", "param");

        // {name} captures a single segment, so /files/a goes to param; /files/a/b goes to wildcard
        assertEquals("param", trie.find("/files/a", "GET"));
        assertEquals("wildcard", trie.find("/files/a/b", "GET"));
    }

    @Test
    void httpMethodIsolated() {
        PathTrie<String> trie = new PathTrie<>();
        trie.add("/users/{id}", "GET", "GET:handler");
        trie.add("/users/{id}", "POST", "POST:handler");

        assertEquals("GET:handler", trie.find("/users/1", "GET"));
        assertEquals("POST:handler", trie.find("/users/1", "POST"));
        assertNull(trie.find("/users/1", "DELETE"));
    }

    @Test
    void unknownRouteReturnsNull() {
        PathTrie<String> trie = new PathTrie<>();
        trie.add("/users/{id}", "GET", "handler");

        assertNull(trie.find("/unknown/1", "GET"));
        assertNull(trie.find("/", "GET"));
    }

    @Test
    void trailingSlashIsIgnored() {
        PathTrie<String> trie = new PathTrie<>();
        trie.add("/users/{id}", "GET", "handler");
        assertEquals("handler", trie.find("/users/42/", "GET"));
    }

    @Test
    void addReplacesExistingValue() {
        PathTrie<String> trie = new PathTrie<>();
        trie.add("/a", "GET", "v1");
        trie.add("/a", "GET", "v2");
        assertEquals("v2", trie.find("/a", "GET"));
    }
}
