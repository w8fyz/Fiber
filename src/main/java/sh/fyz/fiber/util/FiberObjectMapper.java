package sh.fyz.fiber.util;

import com.fasterxml.jackson.databind.ObjectMapper;

public class FiberObjectMapper extends ObjectMapper {

    public FiberObjectMapper() {
        registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
    }

}
