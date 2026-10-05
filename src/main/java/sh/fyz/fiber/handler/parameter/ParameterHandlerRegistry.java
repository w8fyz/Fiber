package sh.fyz.fiber.handler.parameter;

import java.lang.reflect.Parameter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class ParameterHandlerRegistry {
    private static final List<ParameterHandler> handlers = new CopyOnWriteArrayList<>();
    private static final AtomicBoolean initialized = new AtomicBoolean();

    public static void register(ParameterHandler handler) {
        handlers.add(handler);
    }

    public static ParameterHandler findHandler(Parameter parameter) {
        for (ParameterHandler handler : handlers) {
            if (handler.canHandle(parameter)) {
                return handler;
            }
        }
        return null;
    }

    /** Registers the built-in handlers once, however many servers are created in the JVM. */
    public static void initialize() {
        if (!initialized.compareAndSet(false, true)) {
            return;
        }
        register(new ServletParameterHandler());
        register(new RequestBodyParameterHandler());
        register(new QueryParameterHandler());
        register(new PathVariableParameterHandler());
        register(new AuthenticatedUserParameterHandler());
        register(new SessionParameterHandler());
        register(new FileUploadParameterHandler());
        register(new OAuth2ApplicationInfoParameterHandler());
    }
} 