package br.edu.aula.web;

import br.edu.aula.dao.CursoDao;
import br.edu.aula.model.Curso;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

/** /cursos: GET lista, POST cria, DELETE /cursos/{id} exclui. */
public class CursosServlet extends HttpServlet {

    private final CursoDao dao;

    public CursosServlet(CursoDao dao) {
        this.dao = dao;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            Json.enviar(resp, 200, dao.listar());
        } catch (Exception e) {
            Json.falhaInterna(resp, "listar cursos", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Curso entrada;
        try {
            entrada = Json.ler(req, Curso.class);
        } catch (JsonProcessingException e) {
            Json.erro(resp, 400, "Dados inválidos.");
            return;
        }

        String erro = Curso.validar(entrada);
        if (erro != null) {
            Json.erro(resp, 400, erro);
            return;
        }

        try {
            Json.enviar(resp, 201, dao.criar(entrada));
        } catch (Exception e) {
            Json.falhaInterna(resp, "criar curso", e);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String id = Json.idDaUrl(req);
        if (id == null) {
            Json.erro(resp, 400, "Informe o id do curso.");
            return;
        }
        try {
            dao.excluir(id);
            Json.enviar(resp, 200, Map.of("mensagem", "Curso excluído."));
        } catch (Exception e) {
            Json.falhaInterna(resp, "excluir curso", e);
        }
    }
}
