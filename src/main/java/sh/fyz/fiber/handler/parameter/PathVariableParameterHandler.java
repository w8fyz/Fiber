package sh.fyz.fiber.handler.parameter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import sh.fyz.fiber.annotations.params.PathVariable;
import sh.fyz.fiber.util.TypeConverter;
import sh.fyz.fiber.validation.ValidationRegistry;
import sh.fyz.fiber.validation.ValidationResult;

import java.lang.reflect.Parameter;
import java.util.regex.Matcher;

public class PathVariableParameterHandler implements ParameterHandler {
    @Override
    public boolean canHandle(Parameter parameter) {
        return parameter.isAnnotationPresent(PathVariable.class);
    }

    @Override
    public Object handle(Parameter parameter, HttpServletRequest request, HttpServletResponse response, Matcher pathMatcher) throws Exception {
        PathVariable pathVar = parameter.getAnnotation(PathVariable.class);
        String name = pathVar.value();

        String value;
        try {
            value = pathMatcher.group(name);
        } catch (IllegalArgumentException e) {
            // Endpoint route is missing the named capture group — programmer error.
            throw new IllegalStateException(
                    "Path variable '" + name + "' is not declared in the route pattern", e);
        }

        if (value == null) {
            throw new IllegalArgumentException("Missing path variable: " + name);
        }

        Object convertedValue = TypeConverter.convert(value, parameter.getType());
        if (convertedValue == null && parameter.getType().isPrimitive()) {
            throw new IllegalArgumentException("Invalid value for path variable: " + name);
        }

        ValidationResult result = ValidationRegistry.validateParameter(parameter, convertedValue);
        if (!result.isValid()) {
            throw new IllegalArgumentException(result.getFirstError());
        }

        return convertedValue;
    }
}
