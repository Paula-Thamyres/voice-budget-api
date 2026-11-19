package com.paulathamyres.voicebudget;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ApiDocumentationIntegrationTest extends IntegrationTest {

    @Test
    @DisplayName("Gera a documentação do Swagger com os códigos que a API realmente devolve")
    void generatesOpenApiDocumentation() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/assistant/voice'].post.responses['201']").exists())
                .andExpect(jsonPath("$.paths['/assistant/voice'].post.responses['200']").doesNotExist())
                .andExpect(jsonPath("$.paths['/transactions'].post.responses['201']").exists())
                .andExpect(jsonPath("$.paths['/transactions/{id}'].delete.responses['204']").exists());
    }
}
