package sh.fyz.fiber.dashboard;

import sh.fyz.fiber.annotations.request.RequestMapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class DashboardAction {
    private final String id;
    private final String label;
    private final RequestMapping.Method method; // optional if using executor only
    private final String path;                  // optional if using executor only
    private final Map<String, String> inputSchema;
    private final DashboardActionExecutor executor;
    private final List<DashboardRequestFilter> requestFilters;
    private final List<DashboardRequestTransformer> requestTransformers;
    private final List<DashboardResponseTransformer> responseTransformers;

    public DashboardAction(String id, String label, RequestMapping.Method method, String path, Map<String, String> inputSchema) {
        this(id, label, method, path, inputSchema, null);
    }

    public DashboardAction(String id, String label, RequestMapping.Method method, String path, Map<String, String> inputSchema, DashboardActionExecutor executor) {
        this.id = Objects.requireNonNull(id);
        this.label = Objects.requireNonNull(label);
        this.method = method; // may be null
        this.path = path;     // may be null
        this.inputSchema = inputSchema == null ? Collections.emptyMap() : Collections.unmodifiableMap(inputSchema);
        this.executor = executor;
        this.requestFilters = new ArrayList<>();
        this.requestTransformers = new ArrayList<>();
        this.responseTransformers = new ArrayList<>();
    }

    public String getId() { return id; }
    public String getLabel() { return label; }
    public RequestMapping.Method getMethod() { return method; }
    public String getPath() { return path; }
    public Map<String, String> getInputSchema() { return inputSchema; }
    public DashboardActionExecutor getExecutor() { return executor; }

    public List<DashboardRequestFilter> getRequestFilters() { return requestFilters; }
    public List<DashboardRequestTransformer> getRequestTransformers() { return requestTransformers; }
    public List<DashboardResponseTransformer> getResponseTransformers() { return responseTransformers; }

    public DashboardAction addRequestFilter(DashboardRequestFilter filter) {
        this.requestFilters.add(filter);
        return this;
    }

    public DashboardAction addRequestTransformer(DashboardRequestTransformer transformer) {
        this.requestTransformers.add(transformer);
        return this;
    }

    public DashboardAction addResponseTransformer(DashboardResponseTransformer transformer) {
        this.responseTransformers.add(transformer);
        return this;
    }
}
