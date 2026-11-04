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
                // O Haiku 5.5 rejeita temperature fora do padrão (HTTP 400), então não enviamos.
                // Raciocínio desligado: o relatório é um texto simples e isso economiza tokens.
                "max_tokens", 1500,
                "thinking", Map.of("type", "disabled"),
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
            // O corpo do erro explica o motivo (ex.: parâmetro inválido, saldo insuficiente)
            String detalhe = "";
            try {
                detalhe = ": " + mapper.readTree(resposta.body()).path("error").path("message").asText("");
            } catch (IOException e) {
                // corpo não é JSON: fica só o código HTTP
            }
            throw new IOException("A API do Claude respondeu com HTTP " + resposta.statusCode() + detalhe);
        }

        // A resposta traz uma lista de blocos; o texto fica em content[0].text
        JsonNode raiz = mapper.readTree(resposta.body());
        StringBuilder texto = new StringBuilder();
        for (JsonNode bloco : raiz.path("content")) {
            if ("text".equals(bloco.path("type").asText())) {
                texto.append(bloco.path("text").asText(""));
            }
        }
        return texto.toString();
    }
}
