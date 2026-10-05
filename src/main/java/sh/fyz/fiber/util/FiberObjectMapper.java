package sh.fyz.fiber.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import sh.fyz.fiber.core.dto.DTOConvertible;

import java.io.IOException;

public class FiberObjectMapper extends ObjectMapper {

    public FiberObjectMapper() {
        registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        deactivateDefaultTyping();
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Mapper for response bodies: a {@link DTOConvertible} is written as its DTO wherever it sits,
     * as it already was at the top level and in maps and collections. Nested in a plain object it
     * would otherwise go through the bean serializer, which exposes its {@code @IgnoreDTO} fields.
     */
    public static FiberObjectMapper forResponses() {
        FiberObjectMapper mapper = new FiberObjectMapper();
        mapper.registerModule(new SimpleModule("FiberDTO").addSerializer(DTOConvertible.class, new DTOSerializer()));
        return mapper;
    }

    private static final class DTOSerializer extends StdSerializer<DTOConvertible> {
        DTOSerializer() {
            super(DTOConvertible.class);
        }

        @Override
        public void serialize(DTOConvertible value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            provider.defaultSerializeValue(value.asDTO(), gen);
        }

        /** The DTO is a plain map, written without type id, like a top-level DTO. */
        @Override
        public void serializeWithType(DTOConvertible value, JsonGenerator gen, SerializerProvider provider,
                                      TypeSerializer typeSer) throws IOException {
            serialize(value, gen, provider);
        }
    }
}
