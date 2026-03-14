package fr.project.planning;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan("fr.project.planning")
public class TestSpringConfig {

    @Bean
    MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter(@org.springframework.lang.NonNull ObjectMapper objectMapper) {
        return new MappingJackson2HttpMessageConverter(objectMapper);
    }
}