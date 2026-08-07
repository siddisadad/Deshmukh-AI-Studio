package com.aistudio.infrastructure.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.DefaultApplicationArguments;

class ProductionCorsValidatorTest {

  @Test
  void acceptsHttpsProductionOrigins() {
    var validator = new ProductionCorsValidator(new CorsProperties(List.of("https://app.example.com")));
    assertDoesNotThrow(() -> validator.run(new DefaultApplicationArguments()));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "http://localhost:5173",
        "https://localhost",
        "http://app.example.com",
        "https://[::1]",
        "https://127.0.0.1",
        "https://127.0.0.2",
        "ftp://app.example.com",
        "app.example.com"
      })
  void rejectsInsecureOrLoopbackOrigins(String origin) {
    var validator = new ProductionCorsValidator(new CorsProperties(List.of(origin)));
    assertThrows(IllegalStateException.class, () -> validator.run(new DefaultApplicationArguments()));
  }

  @Test
  void rejectsEmptyOrigins() {
    var validator = new ProductionCorsValidator(new CorsProperties(List.of()));
    assertThrows(IllegalStateException.class, () -> validator.run(new DefaultApplicationArguments()));
  }
}
