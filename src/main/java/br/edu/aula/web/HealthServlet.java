package br.edu.aula.web;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

/** /health: usado pelo Cloud Run e pelo front para saber se a API está no ar. */
public class HealthServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Json.enviar(resp, 200, Map.of("status", "online", "sistema", "Sistema Escolar"));
    }
}
