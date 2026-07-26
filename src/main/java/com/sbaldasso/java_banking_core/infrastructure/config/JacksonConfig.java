package com.sbaldasso.java_banking_core.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Boot 4 auto-configures a Jackson 3 (tools.jackson.databind.ObjectMapper)
 * bean by default and no longer registers a classic Jackson 2
 * (com.fasterxml.jackson.databind.ObjectMapper) bean. The outbox event
 * serialization code still relies on the classic Jackson 2 API, so it needs
 * its own bean here.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }
}
