package br.edu.aula.web;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Conta falhas de login por chave (e-mail ou IP) e bloqueia temporariamente
 * depois de um número máximo de erros dentro de uma janela de tempo.
 *
 * Fica em memória: reinicia com o servidor e vale por instância.
 */
public class LimiteTentativas {

    private static final long JANELA_MS = 15 * 60_000L;

    private final int maximo;
    private final Map<String, Registro> registros = new ConcurrentHashMap<>();

    private record Registro(int falhas, long inicioMs) {
    }

    public LimiteTentativas(int maximo) {
        this.maximo = maximo;
    }

    public boolean bloqueado(String chave) {
        Registro r = registros.get(chave);
        if (r == null) {
            return false;
        }
        if (System.currentTimeMillis() - r.inicioMs() > JANELA_MS) {
            registros.remove(chave);
            return false;
        }
        return r.falhas() >= maximo;
    }

    public void registrarFalha(String chave) {
        long agora = System.currentTimeMillis();
        registros.compute(chave, (k, r) -> {
            if (r == null || agora - r.inicioMs() > JANELA_MS) {
                return new Registro(1, agora);
            }
            return new Registro(r.falhas() + 1, r.inicioMs());
        });
    }

    public void limpar(String chave) {
        registros.remove(chave);
    }
}
