package sh.fyz.fiber.handler.parameter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.lang.reflect.Parameter;

/**
 * Interface définissant le contrat pour les handlers de paramètres.
 * Chaque handler est responsable de la gestion d'un type spécifique de paramètre.
 */
public interface ParameterHandler {
    /**
     * Vérifie si ce handler peut gérer le paramètre donné.
     *
     * @param parameter Le paramètre à vérifier
     * @return true si ce handler peut gérer le paramètre
     */
    boolean canHandle(Parameter parameter);

    /**
     * Extrait et convertit la valeur du paramètre à partir de la requête.
     *
     * @param parameter Le paramètre à traiter
     * @param request La requête HTTP
     * @param response La réponse HTTP
     * @param pathMatcher Le matcher pour les variables de chemin
     * @return La valeur convertie du paramètre
     * @throws Exception Si une erreur survient pendant le traitement
     */
    Object handle(Parameter parameter, HttpServletRequest request, HttpServletResponse response, java.util.regex.Matcher pathMatcher) throws Exception;
} 