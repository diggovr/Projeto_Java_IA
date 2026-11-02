package br.edu.aula.web;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limita tentativas de login por chave (e-mail ou IP) dentro de uma janela de tempo.
 *
 * Cada tentativa reserva uma vaga de forma atômica (compute), antes de checar a senha.
 * Assim, requisições simultâneas não conseguem passar do limite ao mesmo tempo.
 * Fica em memória: reinicia com o servidor e vale por instância.
 */
public class LimiteTentativas {

    private static final long JANELA_MS = 15 * 60_000L;

    private final int maximo;
    private final Map<String, Registro> registros = new ConcurrentHashMap<>();

    private record Registro(int tentativas, long inicioMs) {
    }

    public LimiteTentativas(int maximo) {
        this.maximo = maximo;
    }

    /** @return true se a tentativa foi permitida (e já contada); false se o limite foi atingido */
    public boolean reservar(String chave) {
        boolean[] permitida = {false};
        long agora = System.currentTimeMillis();
        registros.compute(chave, (k, r) -> {
            if (r == null || agora - r.inicioMs() > JANELA_MS) {
                permitida[0] = true;
                return new Registro(1, agora);
            }
            if (r.tentativas() >= maximo) {
                permitida[0] = false;
                return r;
            }
            permitida[0] = true;
            return new Registro(r.tentativas() + 1, r.inicioMs());
        });
        return permitida[0];
    }

    /** Devolve uma vaga, usado quando o login dá certo e a tentativa não deve contar. */
    public void devolver(String chave) {
        registros.computeIfPresent(chave, (k, r) ->
                r.tentativas() <= 1 ? null : new Registro(r.tentativas() - 1, r.inicioMs()));
    }
}
