package br.edu.aula.web;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

/**
 * Utilitários de JSON para os servlets.
 *
 * Os campos saem em snake_case (curso_id, data_nasc) para combinar com o front-end.
 * Os erros usam o formato {"detail": "..."}, igual ao que o front já espera.
 */
public final class Json {

    public static final ObjectMapper MAPPER = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private Json() {
    }

    /** Lê o corpo da requisição como objeto Java. */
    public static <T> T ler(HttpServletRequest req, Class<T> tipo) throws IOException {
        return MAPPER.readValue(req.getInputStream(), tipo);
    }

    public static void enviar(HttpServletResponse resp, int status, Object corpo) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json;charset=UTF-8");
        MAPPER.writeValue(resp.getOutputStream(), corpo);
    }

    public static void erro(HttpServletResponse resp, int status, String mensagem) throws IOException {
        enviar(resp, status, Map.of("detail", mensagem));
    }

    /** Erro inesperado: registra no log e responde 500 sem expor detalhes ao navegador. */
    public static void falhaInterna(HttpServletResponse resp, String contexto, Exception e) throws IOException {
        System.err.println("[erro] " + contexto + ": " + e);
        erro(resp, 500, "Não foi possível concluir a operação agora. Tente novamente.");
    }

    /** Para rotas como DELETE /cursos/{id}: devolve o id, ou null se a URL não tiver id. */
    public static String idDaUrl(HttpServletRequest req) {
        String caminho = req.getPathInfo();
        if (caminho == null || caminho.length() <= 1) {
            return null;
        }
        return caminho.substring(1);
    }
}
