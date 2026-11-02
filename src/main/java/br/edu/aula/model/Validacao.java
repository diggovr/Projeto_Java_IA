package br.edu.aula.model;

import java.util.regex.Pattern;

/** Regras de validação usadas pelos modelos. */
public final class Validacao {

    public static final int TAMANHO_MAXIMO = 120;

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private Validacao() {
    }

    public static boolean vazio(String texto) {
        return texto == null || texto.isBlank();
    }

    public static boolean emailValido(String email) {
        return EMAIL.matcher(email).matches();
    }

    public static boolean longo(String texto) {
        return texto != null && texto.length() > TAMANHO_MAXIMO;
    }
}
