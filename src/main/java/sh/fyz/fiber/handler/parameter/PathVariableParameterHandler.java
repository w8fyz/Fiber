package sh.fyz.fiber.handler.parameter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import sh.fyz.fiber.annotations.params.PathVariable;
import sh.fyz.fiber.validation.ValidationRegistry;
import sh.fyz.fiber.validation.ValidationResult;

import java.lang.reflect.Parameter;
import java.util.regex.Matcher;

/**
 * Handler pour les paramètres annotés avec @PathVariable.
 */
public class PathVariableParameterHandler implements ParameterHandler {
    @Override
    public boolean canHandle(Parameter parameter) {
        return parameter.isAnnotationPresent(PathVariable.class);
    }

    @Override
    public Object handle(Parameter parameter, HttpServletRequest request, HttpServletResponse response, Matcher pathMatcher) throws Exception {
        PathVariable pathVar = parameter.getAnnotation(PathVariable.class);
        String value = pathMatcher.group(pathVar.value());
        
        Object convertedValue = convertValue(value, parameter.getType());
        ValidationResult result = ValidationRegistry.validateParameter(parameter, convertedValue);
        
        if (!result.isValid()) {
            throw new IllegalArgumentException(result.getFirstError());
        }
        
        return convertedValue;
    }

    private Object convertValue(String value, Class<?> type) {
        if (value == null) {
            return null;
        }

        if (type == String.class) {
            return value;
        } else if (type == Integer.class || type == int.class) {
            return Integer.parseInt(value);
        } else if (type == Long.class || type == long.class) {
            return Long.parseLong(value);
        } else if (type == Double.class || type == double.class) {
            return Double.parseDouble(value);
        } else if (type == Boolean.class || type == boolean.class) {
            return Boolean.parseBoolean(value);
        } else {
            throw new IllegalArgumentException("Unsupported type: " + type);
        }
    }
} 