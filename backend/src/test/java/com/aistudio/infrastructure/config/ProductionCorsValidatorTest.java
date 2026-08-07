package com.aistudio.infrastructure.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

class ProductionCorsValidatorTest {

  @Test
  void acceptsHttpsProductionOrigins() {
    var validator = new ProductionCorsValidator(new CorsProperties(List.of("https://app.example.com")));
    assertDoesNotThrow(() -> validator.run(new DefaultApplicationArguments()));
  }

  @Test
  void rejectsLocalhostInProduction() {
    var validator = new ProductionCorsValidator(new CorsProperties(List.of("http://localhost:5173")));
    assertThrows(IllegalStateException.class, () -> validator.run(new DefaultApplicationArguments()));
  }

  @Test
  void rejectsEmptyOrigins() {
    var validator = new ProductionCorsValidator(new CorsProperties(List.of()));
    assertThrows(IllegalStateException.class, () -> validator.run(new DefaultApplicationArguments()));
  }
}
