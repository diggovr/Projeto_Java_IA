package br.edu.aula.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Cliente mínimo da API Messages da Anthropic.
 *
 * Não usa SDK de terceiros de propósito: a chamada HTTP aparece aqui inteira,
 * para que os alunos vejam exatamente o que trafega entre o Java e o Claude.
 */
public class ClaudeClient {

    private static final URI ENDPOINT = URI.create("https://api.anthropic.com/v1/messages");
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    private final String apiKey;
    private final String modelo;

    public ClaudeClient(String apiKey, String modelo) {
        this.apiKey = apiKey;
        this.modelo = modelo;
    }

    /** Indica se existe chave configurada. Sem chave, o serviço usa o fallback. */
    public boolean configurado() {
        return apiKey != null && !apiKey.isBlank();
    }

    /** Nome do modelo que está sendo usado (exibido no front). */
    public String modelo() {
        return modelo;
    }

    /**
     * Envia uma mensagem ao Claude e devolve o texto da resposta.
     *
     * @throws IOException se a API responder com erro ou a resposta não puder ser lida
     */
    public String enviar(String promptSistema, String mensagemUsuario)
            throws IOException, InterruptedException {

        Map<String, Object> corpo = Map.of(
                "model", modelo,
                "max_tokens", 600,
                "temperature", 0.2,
                "system", promptSistema,
                "messages", List.of(Map.of("role", "user", "content", mensagemUsuario)));

        HttpRequest requisicao = HttpRequest.newBuilder(ENDPOINT)
                .timeout(TIMEOUT)
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(corpo)))
                .build();

        HttpResponse<String> resposta = http.send(requisicao, HttpResponse.BodyHandlers.ofString());
        if (resposta.statusCode() != 200) {
            throw new IOException("A API do Claude respondeu com HTTP " + resposta.statusCode());
        }

        // A resposta traz uma lista de blocos; o texto fica em content[0].text
        JsonNode raiz = mapper.readTree(resposta.body());
        return raiz.path("content").path(0).path("text").asText("");
    }
}
