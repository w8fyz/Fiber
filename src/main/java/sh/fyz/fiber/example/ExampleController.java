package sh.fyz.fiber.example;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import sh.fyz.fiber.annotations.Controller;
import sh.fyz.fiber.annotations.Param;
import sh.fyz.fiber.annotations.RequestBody;
import sh.fyz.fiber.annotations.RequestMapping;
import sh.fyz.fiber.core.ResponseEntity;
import sh.fyz.fiber.validation.Email;
import sh.fyz.fiber.validation.Min;
import sh.fyz.fiber.validation.NotBlank;
import sh.fyz.fiber.validation.NotNull;

import java.util.HashMap;
import java.util.Map;

@Controller("/api")
public class ExampleController {
    
    @RequestMapping(value = "/hello", method = RequestMapping.Method.GET)
    public ResponseEntity<Map<String, String>> hello() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Hello, World!");
        return ResponseEntity.ok(response);
    }

    @RequestMapping(value = "/greet", method = RequestMapping.Method.GET)
    public ResponseEntity<Map<String, String>> greet(@NotBlank @Param("name") String name) {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Hello, " + name + "!");
        return ResponseEntity.ok(response);
    }

    @RequestMapping(value = "/calculate", method = RequestMapping.Method.GET)
    public ResponseEntity<Map<String, Object>> calculate(
            @Min(value = 0) @Param("a") int a,
            @Min(value = 0) @Param("b") int b,
            @Param(value = "operation", required = false) String operation) {
        
        Map<String, Object> response = new HashMap<>();
        int result;
        
        if (operation == null || operation.equals("add")) {
            result = a + b;
            operation = "add";
        } else if (operation.equals("subtract")) {
            result = a - b;
        } else if (operation.equals("multiply")) {
            result = a * b;
        } else {
            return ResponseEntity.badRequest(Map.of("error", "Invalid operation"));
        }
        
        response.put("operation", operation);
        response.put("result", result);
        return ResponseEntity.ok(response);
    }

    @RequestMapping(value = "/post", method = RequestMapping.Method.POST)
    public ResponseEntity<Map<String, Object>> handlePost(@RequestBody Map<String, Object> body) {
        Map<String, Object> response = new HashMap<>();
        response.put("received", body);
        return ResponseEntity.ok(response);
    }
} 