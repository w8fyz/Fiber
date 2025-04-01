package sh.fyz.fiber.docs;

import sh.fyz.fiber.annotations.RequestMapping;
import sh.fyz.fiber.core.ResponseEntity;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EndpointDoc {
    private final String path;
    private final RequestMapping.Method method;
    private final String description;
    private final List<ParameterDoc> parameters;
    private final Class<?> requestBodyType;
    private final Class<?> responseType;
    private final Object requestBodyExample;
    private final Object responseExample;

    public EndpointDoc(String path, RequestMapping.Method method, String description,
                       List<ParameterDoc> parameters, Class<?> requestBodyType, Class<?> responseType) {
        this.path = path;
        this.method = method;
        this.description = description;
        this.parameters = parameters;
        this.requestBodyType = requestBodyType;
        this.responseType = responseType;
        this.requestBodyExample = requestBodyType != null ? generateExample(requestBodyType) : null;
        this.responseExample = responseType != null ? generateExample(responseType) : null;
    }

    private static Object generateExample(Class<?> type) {
        if (type == String.class) {
            return "example_string";
        } else if (type == Integer.class || type == int.class) {
            return 0;
        } else if (type == Long.class || type == long.class) {
            return 0L;
        } else if (type == Double.class || type == double.class) {
            return 0.0;
        } else if (type == Float.class || type == float.class) {
            return 0.0f;
        } else if (type == Boolean.class || type == boolean.class) {
            return false;
        } else if (type == List.class || type.getName().startsWith("java.util.List")) {
            List<Object> list = new ArrayList<>();
            list.add("example_item");
            return list;
        } else if (type == Map.class || type.getName().startsWith("java.util.Map")) {
            Map<String, Object> map = new HashMap<>();
            map.put("key", "example_value");
            return map;
        } else if (type.isArray()) {
            return new String[]{"example_item"};
        } else if (type == ResponseEntity.class) {
            Map<String, Object> example = new HashMap<>();
            example.put("status", 200);
            example.put("message", "Success");
            example.put("data", generateDataExample());
            return example;
        } else {
            try {
                Map<String, Object> example = new HashMap<>();
                for (Field field : type.getDeclaredFields()) {
                    field.setAccessible(true);
                    example.put(field.getName(), generateExample(field.getType()));
                }
                return example;
            } catch (Exception e) {
                return "example_" + type.getSimpleName().toLowerCase();
            }
        }
    }

    private static Object generateDataExample() {
        Map<String, Object> data = new HashMap<>();
        data.put("id", 1);
        data.put("name", "example_name");
        data.put("email", "example@email.com");
        data.put("createdAt", "2024-03-21T12:00:00Z");
        return data;
    }

    public static EndpointDoc fromMethod(Method method, String basePath) {
        String path = basePath + method.getAnnotation(sh.fyz.fiber.annotations.RequestMapping.class).value();
        RequestMapping.Method httpMethod = method.getAnnotation(sh.fyz.fiber.annotations.RequestMapping.class).method();
        List<ParameterDoc> parameters = new ArrayList<>();
        Class<?> requestBodyType = null;

        for (Parameter param : method.getParameters()) {
            if (param.isAnnotationPresent(sh.fyz.fiber.annotations.RequestBody.class)) {
                requestBodyType = param.getType();
            } else if (param.isAnnotationPresent(sh.fyz.fiber.annotations.Param.class)) {
                sh.fyz.fiber.annotations.Param paramAnnotation = param.getAnnotation(sh.fyz.fiber.annotations.Param.class);
                parameters.add(new ParameterDoc(
                    paramAnnotation.value(),
                    param.getType().getSimpleName(),
                    paramAnnotation.required(),
                    getValidationMessages(param)
                ));
            }
        }

        return new EndpointDoc(
            path,
            httpMethod,
            getMethodDescription(method),
            parameters,
            requestBodyType,
            method.getReturnType()
        );
    }

    private static List<String> getValidationMessages(Parameter param) {
        List<String> messages = new ArrayList<>();
        if (param.isAnnotationPresent(sh.fyz.fiber.validation.NotNull.class)) {
            messages.add(param.getAnnotation(sh.fyz.fiber.validation.NotNull.class).message());
        }
        if (param.isAnnotationPresent(sh.fyz.fiber.validation.NotBlank.class)) {
            messages.add(param.getAnnotation(sh.fyz.fiber.validation.NotBlank.class).message());
        }
        if (param.isAnnotationPresent(sh.fyz.fiber.validation.Min.class)) {
            sh.fyz.fiber.validation.Min min = param.getAnnotation(sh.fyz.fiber.validation.Min.class);
            messages.add(min.message().replace("{value}", String.valueOf(min.value())));
        }
        if (param.isAnnotationPresent(sh.fyz.fiber.validation.Email.class)) {
            messages.add(param.getAnnotation(sh.fyz.fiber.validation.Email.class).message());
        }
        return messages;
    }

    private static String getMethodDescription(Method method) {
        // You can add a custom annotation for descriptions if needed
        return method.getName();
    }

    // Getters
    public String getPath() { return path; }
    public RequestMapping.Method getMethod() { return method; }
    public String getDescription() { return description; }
    public List<ParameterDoc> getParameters() { return parameters; }
    public Class<?> getRequestBodyType() { return requestBodyType; }
    public Class<?> getResponseType() { return responseType; }
    public Object getRequestBodyExample() { return requestBodyExample; }
    public Object getResponseExample() { return responseExample; }

    public static class ParameterDoc {
        private final String name;
        private final String type;
        private final boolean required;
        private final List<String> validationMessages;

        public ParameterDoc(String name, String type, boolean required, List<String> validationMessages) {
            this.name = name;
            this.type = type;
            this.required = required;
            this.validationMessages = validationMessages;
        }

        // Getters
        public String getName() { return name; }
        public String getType() { return type; }
        public boolean isRequired() { return required; }
        public List<String> getValidationMessages() { return validationMessages; }
    }
} 