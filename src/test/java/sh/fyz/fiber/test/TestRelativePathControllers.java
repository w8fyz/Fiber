package sh.fyz.fiber.test;

import sh.fyz.fiber.annotations.params.PathVariable;
import sh.fyz.fiber.annotations.request.Controller;
import sh.fyz.fiber.annotations.request.RequestMapping;

import java.util.Map;

/** Controllers whose paths are written without a leading {@code /}, as the annotations allow. */
public final class TestRelativePathControllers {

    private TestRelativePathControllers() {
    }

    /** Rooted base path, relative mapping. Registered as an instance. */
    @Controller("/mixed")
    public static class Mixed {
        @RequestMapping(value = "ping", method = RequestMapping.Method.GET)
        public Map<String, String> ping() {
            return Map.of("controller", "mixed");
        }
    }

    /** Relative base path and mappings. Registered as an instance. */
    @Controller("relative")
    public static class Relative {
        @RequestMapping(value = "ping", method = RequestMapping.Method.GET)
        public Map<String, String> ping() {
            return Map.of("controller", "relative");
        }

        @RequestMapping(value = "items/{id}", method = RequestMapping.Method.GET)
        public Map<String, String> item(@PathVariable("id") String id) {
            return Map.of("id", id);
        }
    }

    /** Rooted base path, relative mapping. Registered by class, which used to glue them into {@code /mixed-classping}. */
    @Controller("/mixed-class")
    public static class MixedByClass {
        @RequestMapping(value = "ping", method = RequestMapping.Method.GET)
        public Map<String, String> ping() {
            return Map.of("controller", "mixed-class");
        }
    }

    /** Relative base path and mapping. Registered by class. */
    @Controller("relative-class")
    public static class RelativeByClass {
        @RequestMapping(value = "ping", method = RequestMapping.Method.GET)
        public Map<String, String> ping() {
            return Map.of("controller", "relative-class");
        }
    }
}
