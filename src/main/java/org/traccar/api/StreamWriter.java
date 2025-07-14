package org.traccar.api;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.stream.Stream;

@Provider
@Produces(MediaType.APPLICATION_JSON)
public class StreamWriter implements MessageBodyWriter<Stream<?>> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return Stream.class.isAssignableFrom(type);
    }

    @Override
    public void writeTo(Stream<?> stream, Class<?> type, Type genericType, Annotation[] annotations,
                        MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
            throws IOException {
        try (stream;
             JsonGenerator jsonGenerator = objectMapper.createGenerator(entityStream)) {
            jsonGenerator.writeStartArray();
            stream.forEach(object -> {
                try {
                    objectMapper.writeValue(jsonGenerator, object);
                } catch (IOException io) {
                    throw new UncheckedIOException(io);
                }
            });
            jsonGenerator.writeEndArray();
        }
    }
}
