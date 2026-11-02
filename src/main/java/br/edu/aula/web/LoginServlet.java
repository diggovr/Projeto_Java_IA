package br.edu.aula.web;

import br.edu.aula.dao.UsuarioDao;
import br.edu.aula.model.Usuario;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** /login: confere e-mail e senha e devolve os dados do usuário (sem a senha). */
public class LoginServlet extends HttpServlet {

    private final UsuarioDao dao;

    public LoginServlet(UsuarioDao dao) {
        this.dao = dao;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Usuario.Login dados;
        try {
            dados = Json.ler(req, Usuario.Login.class);
        } catch (JsonProcessingException e) {
            Json.erro(resp, 400, "Dados inválidos.");
            return;
        }

        try {
            Usuario usuario = dados.email() == null || dados.senha() == null
                    ? null
                    : dao.autenticar(dados.email(), dados.senha());
            if (usuario == null) {
                Json.erro(resp, 401, "Email ou senha inválidos.");
            } else {
                Json.enviar(resp, 200, usuario);
            }
        } catch (Exception e) {
            Json.falhaInterna(resp, "login", e);
        }
    }
}
