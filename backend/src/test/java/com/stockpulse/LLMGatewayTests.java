package com.stockpulse;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import org.springframework.test.web.client.MockRestServiceServer;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import org.springframework.web.client.RestClient;

import com.stockpulse.ai.LLMGateway;

class LLMGatewayTests {
    @Test
    void geminiGatewayPostsPromptAndParsesText() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        LLMGateway gateway = new LLMGateway(builder, "gemini", "gemini-test", "test-key", "https://llm.test");

        server.expect(requestTo("https://llm.test/v1beta/models/gemini-test:generateContent?key=test-key"))
                .andExpect(header(CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(content().json("{\"contents\":[{\"parts\":[{\"text\":\"test prompt\"}]}]}"))
                .andRespond(withSuccess("{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"{\\\"ok\\\":true}\"}]}}]}",
                        org.springframework.http.MediaType.APPLICATION_JSON));

        assertThat(gateway.callLLM("test prompt")).isEqualTo("{\"ok\":true}");
        server.verify();
    }
}
