package com.paulathamyres.voicebudget.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(
        title = "Voice Budget API",
        version = "1.0",
        description = "Orçamento pessoal por voz: o áudio vira texto, a IA entende o pedido, registra ou consulta "
                + "as movimentações e devolve a resposta em texto e em voz."))
public class OpenApiConfig {
}
