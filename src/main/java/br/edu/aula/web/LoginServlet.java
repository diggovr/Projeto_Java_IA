package br.edu.aula.web;

import br.edu.aula.dao.UsuarioDao;
import br.edu.aula.model.Usuario;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * /login: confere e-mail e senha e devolve os dados do usuário mais um token.
 *
 * Cada tentativa reserva uma vaga antes de checar a senha: por e-mail (5 por janela)
 * e por IP (50 por janela). Login certo devolve a vaga do IP, para que uma sala
 * inteira atrás do mesmo endereço não se bloqueie entre si.
 */
public class LoginServlet extends HttpServlet {

    private final UsuarioDao dao;
    private final Tokens tokens;
    private final LimiteTentativas limiteEmail;
    private final LimiteTentativas limiteIp;

    public LoginServlet(UsuarioDao dao, Tokens tokens, LimiteTentativas limiteEmail, LimiteTentativas limiteIp) {
        this.dao = dao;
        this.tokens = tokens;
        this.limiteEmail = limiteEmail;
        this.limiteIp = limiteIp;
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
        if (dados.email() == null || dados.senha() == null) {
            Json.erro(resp, 401, "Email ou senha inválidos.");
            return;
        }

        String email = dados.email().trim();
        String chaveEmail = email.toLowerCase(Locale.ROOT);
        String chaveIp = req.getRemoteAddr();

        // Reserva as vagas antes de checar a senha: requisições simultâneas não furam o limite
        if (!limiteEmail.reservar(chaveEmail) || !limiteIp.reservar(chaveIp)) {
            Json.erro(resp, 429, "Muitas tentativas. Aguarde 15 minutos e tente novamente.");
            return;
        }

        try {
            Usuario usuario = dao.autenticar(email, dados.senha());
            if (usuario == null) {
                // Falha: a vaga fica reservada, contando para o limite
                Json.erro(resp, 401, "Email ou senha inválidos.");
                return;
            }

            // Sucesso: a vaga do IP é devolvida; a do e-mail é zerada
            limiteIp.devolver(chaveIp);
            limiteEmail.devolver(chaveEmail);
            Map<String, Object> corpo = new HashMap<>();
            corpo.put("id", usuario.id());
            corpo.put("nome", usuario.nome());
            corpo.put("email", usuario.email());
            corpo.put("perfil", usuario.perfil());
            corpo.put("token", tokens.criar(usuario.id(), usuario.perfil()));
            Json.enviar(resp, 200, corpo);
        } catch (Exception e) {
            Json.falhaInterna(resp, "login", e);
        }
    }
}
