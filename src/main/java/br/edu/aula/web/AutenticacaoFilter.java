package br.edu.aula.web;

import br.edu.aula.dao.UsuarioDao;
import br.edu.aula.model.Usuario;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

/**
 * Exige um token de login válido antes de chegar aos servlets.
 *
 * O token só diz quem é o usuário. O perfil e a existência vêm do banco a cada
 * requisição, então um usuário excluído perde o acesso na hora, e uma troca de
 * perfil vale sem esperar o token expirar. Rotas de /usuarios exigem "admin".
 */
public class AutenticacaoFilter implements Filter {

    private final Tokens tokens;
    private final UsuarioDao usuarios;

    public AutenticacaoFilter(Tokens tokens, UsuarioDao usuarios) {
        this.tokens = tokens;
        this.usuarios = usuarios;
    }

    @Override
    public void doFilter(ServletRequest pedido, ServletResponse resposta, FilterChain cadeia)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) pedido;
        HttpServletResponse resp = (HttpServletResponse) resposta;

        Map<String, Object> token = tokens.verificar(extrairToken(req));
        if (token == null) {
            Json.erro(resp, 401, "Faça login para continuar.");
            return;
        }

        Usuario atual;
        try {
            atual = usuarios.buscarPorId((String) token.get("sub"));
        } catch (Exception e) {
            Json.falhaInterna(resp, "autenticação", e);
            return;
        }
        if (atual == null) {
            Json.erro(resp, 401, "Sua sessão não é mais válida. Faça login novamente.");
            return;
        }

        boolean rotaDeAdmin = req.getServletPath().startsWith("/usuarios");
        if (rotaDeAdmin && !"admin".equals(atual.perfil())) {
            Json.erro(resp, 403, "Apenas administradores podem gerenciar usuários.");
            return;
        }

        cadeia.doFilter(pedido, resposta);
    }

    /** Lê o cabeçalho "Authorization: Bearer <token>". */
    private static String extrairToken(HttpServletRequest req) {
        String cabecalho = req.getHeader("Authorization");
        if (cabecalho == null || !cabecalho.startsWith("Bearer ")) {
            return null;
        }
        return cabecalho.substring("Bearer ".length()).trim();
    }
}
