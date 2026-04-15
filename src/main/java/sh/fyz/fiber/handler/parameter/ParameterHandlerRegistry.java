package sh.fyz.fiber.handler.parameter;

import java.lang.reflect.Parameter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ParameterHandlerRegistry {
    private static final List<ParameterHandler> handlers = new CopyOnWriteArrayList<>();

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

    public static void initialize() {
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