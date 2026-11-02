package br.edu.aula.model;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.List;

/**
 * Usuário do sistema (quem faz login). A senha nunca sai da API:
 * no banco fica só o hash, e a resposta traz apenas id, nome, e-mail e perfil.
 */
public record Usuario(String id, String nome, String email, String perfil) {

    public static final List<String> PERFIS = List.of("aluno", "professor", "admin");

    private static final int ITERACOES = 210_000;
    private static final int TAMANHO_SAL = 16;
    private static final int TAMANHO_CHAVE = 256;
    private static final SecureRandom ALEATORIO = new SecureRandom();

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
     * Gera o hash da senha com PBKDF2 (com sal aleatório e muitas iterações).
     * Formato guardado: pbkdf2$iterações$sal$hash
     */
    public static String hashSenha(String senha) {
        byte[] sal = new byte[TAMANHO_SAL];
        ALEATORIO.nextBytes(sal);
        byte[] hash = derivar(senha, sal, ITERACOES);
        return "pbkdf2$" + ITERACOES + "$"
                + Base64.getEncoder().encodeToString(sal) + "$"
                + Base64.getEncoder().encodeToString(hash);
    }

    /** Confere uma senha digitada com o hash guardado, em tempo constante. */
    public static boolean senhaConfere(String senha, String armazenado) {
        if (senha == null || armazenado == null) {
            return false;
        }
        String[] partes = armazenado.split("\\$");
        if (partes.length != 4 || !"pbkdf2".equals(partes[0])) {
            return false;
        }
        int iteracoes = Integer.parseInt(partes[1]);
        byte[] sal = Base64.getDecoder().decode(partes[2]);
        byte[] esperado = Base64.getDecoder().decode(partes[3]);
        return MessageDigest.isEqual(esperado, derivar(senha, sal, iteracoes));
    }

    private static byte[] derivar(String senha, byte[] sal, int iteracoes) {
        try {
            KeySpec spec = new PBEKeySpec(senha.toCharArray(), sal, iteracoes, TAMANHO_CHAVE);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("PBKDF2 indisponível na JVM", e);
        }
    }
}
