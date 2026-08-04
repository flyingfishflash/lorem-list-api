package net.flyingfishflash.loremlist.core.configuration;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.io.IOException;
import kotlinx.datetime.Instant;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers a Jackson module for {@code kotlinx.datetime.Instant}, which has no default Jackson
 * representation, since it's still used throughout the domain model.
 */
@Configuration
public class SerializationConfig {

  @Bean
  public SimpleModule kotlinxInstantModule() {
    SimpleModule module = new SimpleModule();
    module.addSerializer(
        Instant.class,
        new JsonSerializer<Instant>() {
          @Override
          public void serialize(Instant value, JsonGenerator gen, SerializerProvider serializers)
              throws IOException {
            gen.writeString(value.toString());
          }
        });
    module.addDeserializer(
        Instant.class,
        new JsonDeserializer<Instant>() {
          @Override
          public Instant deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            return new Instant(java.time.Instant.parse(p.getValueAsString()));
          }
        });
    return module;
  }
}
