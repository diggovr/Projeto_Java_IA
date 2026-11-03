package br.edu.aula.web;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Raiz do site (/): redireciona para index.html.
 *
 * No Cloud Run o Jetty não aplica a página padrão (welcome file) sobre o JAR,
 * então a raiz daria 404. Este redirecionamento resolve isso.
 */
public class InicioServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.sendRedirect("index.html");
    }
}
