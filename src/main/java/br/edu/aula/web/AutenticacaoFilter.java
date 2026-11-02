package br.edu.aula.web;

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
 * Rotas de /usuarios exigem também o perfil "admin".
 *
 * Um filtro fica fora do servlet: nenhuma rota protegida pode esquecer a checagem.
 */
public class AutenticacaoFilter implements Filter {

    private final Tokens tokens;

    public AutenticacaoFilter(Tokens tokens) {
        this.tokens = tokens;
    }

    @Override
    public void doFilter(ServletRequest pedido, ServletResponse resposta, FilterChain cadeia)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) pedido;
        HttpServletResponse resp = (HttpServletResponse) resposta;

        Map<String, Object> usuario = tokens.verificar(extrairToken(req));
        if (usuario == null) {
            Json.erro(resp, 401, "Faça login para continuar.");
            return;
        }

        boolean rotaDeAdmin = req.getServletPath().startsWith("/usuarios");
        if (rotaDeAdmin && !"admin".equals(usuario.get("perfil"))) {
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
