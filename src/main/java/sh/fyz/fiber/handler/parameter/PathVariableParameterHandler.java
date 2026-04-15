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
        String value = pathMatcher.group(pathVar.value());

        Object convertedValue = TypeConverter.convert(value, parameter.getType());
        ValidationResult result = ValidationRegistry.validateParameter(parameter, convertedValue);

        if (!result.isValid()) {
            throw new IllegalArgumentException(result.getFirstError());
        }

        return convertedValue;
    }
}
