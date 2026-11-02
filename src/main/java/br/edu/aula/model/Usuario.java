package br.edu.aula.model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

/**
 * Usuário do sistema (quem faz login). A senha nunca sai da API:
 * no banco fica só o hash, e a resposta traz apenas id, nome, e-mail e perfil.
 */
public record Usuario(String id, String nome, String email, String perfil) {

    public static final List<String> PERFIS = List.of("aluno", "professor", "admin");

    /** Dados para criar um usuário (vêm do formulário). */
    public record Entrada(String nome, String email, String senha, String perfil) {
    }

    /** Credenciais enviadas no login. */
    public record Login(String email, String senha) {
    }

    /** @return mensagem de erro em português, ou null se estiver tudo certo */
    public static String validarEntrada(Entrada e) {
        if (Validacao.vazio(e.nome())) {
            return "O nome é obrigatório.";
        }
        if (Validacao.vazio(e.email()) || !Validacao.emailValido(e.email())) {
            return "Informe um e-mail válido.";
        }
        if (e.senha() == null || e.senha().length() < 6) {
            return "A senha deve ter pelo menos 6 caracteres.";
        }
        if (e.perfil() != null && !PERFIS.contains(e.perfil())) {
            return "Perfil inválido.";
        }
        if (Validacao.longo(e.nome()) || Validacao.longo(e.email())) {
            return "Nome e e-mail devem ter no máximo 120 caracteres.";
        }
        return null;
    }

    /**
     * Gera o hash SHA-256 da senha, em hexadecimal.
     * Didático: em produção use um algoritmo com sal, como bcrypt ou argon2.
     */
    public static String hashSenha(String senha) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(senha.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível na JVM", e);
        }
    }
}
