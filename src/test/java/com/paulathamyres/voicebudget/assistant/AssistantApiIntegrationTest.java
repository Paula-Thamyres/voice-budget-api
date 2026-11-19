package com.paulathamyres.voicebudget.assistant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.paulathamyres.voicebudget.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

class AssistantApiIntegrationTest extends IntegrationTest {

    private static final String COMMAND = "Gastei 45 reais no mercado";
    private static final String REPLY = "Pronto, registrei a despesa de 45 reais com mercado.";

    @Test
    @DisplayName("Transcreve o áudio, responde e salva a interação de voz")
    void answersVoiceCommand() throws Exception {
        when(transcriptionModel.transcribe(any(Resource.class))).thenReturn(COMMAND);
        when(budgetAssistant.reply(COMMAND)).thenReturn(REPLY);

        mockMvc.perform(multipart("/assistant/voice").file(audio("comando.m4a")))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.channel").value("VOICE"))
                .andExpect(jsonPath("$.userMessage").value(COMMAND))
                .andExpect(jsonPath("$.reply").value(REPLY));
    }

    @Test
    @DisplayName("Devolve a resposta em MP3")
    void speaksReply() throws Exception {
        byte[] mp3 = {1, 2, 3};
        when(budgetAssistant.reply(COMMAND)).thenReturn(REPLY);
        when(textToSpeechModel.call(anyString())).thenReturn(mp3);

        String json = mockMvc.perform(post("/assistant/text")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"message": "%s"}
                                """.formatted(COMMAND)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.channel").value("TEXT"))
                .andReturn().getResponse().getContentAsString();
        Number id = JsonPath.read(json, "$.id");

        mockMvc.perform(get("/assistant/interactions/{id}/speech", id))
                .andExpect(status().isOk())
                .andExpect(content().contentType("audio/mpeg"))
                .andExpect(content().bytes(mp3));
    }

    @Test
    @DisplayName("Recusa arquivo que não é áudio sem chamar a IA")
    void rejectsUnsupportedFile() throws Exception {
        mockMvc.perform(multipart("/assistant/voice").file(audio("anotacao.txt")))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.title").value("Regra de negócio violada"));

        verifyNoInteractions(transcriptionModel, budgetAssistant);
    }

    @Test
    @DisplayName("Devolve 502 quando o serviço de transcrição está fora do ar")
    void returnsBadGatewayWhenTranscriptionFails() throws Exception {
        when(transcriptionModel.transcribe(any(Resource.class))).thenThrow(new IllegalStateException("timeout"));

        mockMvc.perform(multipart("/assistant/voice").file(audio("comando.m4a")))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.detail").value("Não foi possível transcrever o áudio agora. Tente novamente em instantes."));
    }

    @Test
    @DisplayName("Devolve 404 ao pedir o áudio de uma interação que não existe")
    void rejectsSpeechOfUnknownInteraction() throws Exception {
        mockMvc.perform(get("/assistant/interactions/{id}/speech", 999_999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Interação não encontrada: 999999"));

        verifyNoInteractions(textToSpeechModel);
    }

    private static MockMultipartFile audio(String filename) {
        return new MockMultipartFile("audio", filename, "audio/mp4", new byte[] {1, 2, 3});
    }
}
