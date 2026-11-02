package br.edu.aula.web;

import br.edu.aula.dao.UsuarioDao;
import br.edu.aula.model.Usuario;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

/** /usuarios: GET lista, POST cria, DELETE /usuarios/{id} exclui. */
public class UsuariosServlet extends HttpServlet {

    private final UsuarioDao dao;

    public UsuariosServlet(UsuarioDao dao) {
        this.dao = dao;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            Json.enviar(resp, 200, dao.listar());
        } catch (Exception e) {
            Json.falhaInterna(resp, "listar usuários", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Usuario.Entrada entrada;
        try {
            entrada = Json.ler(req, Usuario.Entrada.class);
        } catch (JsonProcessingException e) {
            Json.erro(resp, 400, "Dados inválidos.");
            return;
        }

        String erro = Usuario.validarEntrada(entrada);
        if (erro != null) {
            Json.erro(resp, 400, erro);
            return;
        }

        try {
            Usuario criado = dao.criar(entrada);
            if (criado == null) {
                Json.erro(resp, 400, "E-mail já cadastrado.");
            } else {
                Json.enviar(resp, 201, criado);
            }
        } catch (Exception e) {
            Json.falhaInterna(resp, "criar usuário", e);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String id = Json.idDaUrl(req);
        if (id == null) {
            Json.erro(resp, 400, "Informe o id do usuário.");
            return;
        }
        try {
            dao.excluir(id);
            Json.enviar(resp, 200, Map.of("mensagem", "Usuário excluído."));
        } catch (Exception e) {
            Json.falhaInterna(resp, "excluir usuário", e);
        }
    }
}
