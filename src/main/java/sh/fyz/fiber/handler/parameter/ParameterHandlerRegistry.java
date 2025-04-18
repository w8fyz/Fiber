package sh.fyz.fiber.handler.parameter;

import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

/**
 * Registre central pour tous les handlers de paramètres.
 */
public class ParameterHandlerRegistry {
    private static final List<ParameterHandler> handlers = new ArrayList<>();

    /**
     * Enregistre un nouveau handler de paramètres.
     *
     * @param handler Le handler à enregistrer
     */
    public static void register(ParameterHandler handler) {
        handlers.add(handler);
    }

    /**
     * Trouve le premier handler capable de gérer le paramètre donné.
     *
     * @param parameter Le paramètre à gérer
     * @return Le handler approprié ou null si aucun handler n'est trouvé
     */
    public static ParameterHandler findHandler(Parameter parameter) {
        return handlers.stream()
                .filter(handler -> handler.canHandle(parameter))
                .findFirst()
                .orElse(null);
    }

    /**
     * Initialise les handlers par défaut.
     */
    public static void initialize() {
        register(new ServletParameterHandler());
        register(new RequestBodyParameterHandler());
        register(new QueryParameterHandler());
        register(new PathVariableParameterHandler());
        register(new AuthenticatedUserParameterHandler());
        register(new FileUploadParameterHandler());
        register(new OAuth2ApplicationInfoParameterHandler());
    }
} 