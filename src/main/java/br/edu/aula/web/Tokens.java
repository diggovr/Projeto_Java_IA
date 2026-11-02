package br.edu.aula.web;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

/**
 * Tokens de login assinados com HMAC-SHA256.
 *
 * Formato: base64url(payload JSON) + "." + base64url(assinatura).
 * O payload leva o id, o perfil e a validade. Quem não tem o segredo
 * (APP_SECRET) não consegue forjar um token.
 */
public class Tokens {

    private static final long VALIDADE_SEGUNDOS = 8 * 3600;

    private final byte[] segredo;

    public Tokens(String segredoDoAmbiente) {
        if (segredoDoAmbiente == null || segredoDoAmbiente.isBlank()) {
            // Sem APP_SECRET, o segredo muda a cada subida: logins antigos deixam de valer
            byte[] aleatorio = new byte[32];
            new SecureRandom().nextBytes(aleatorio);
            this.segredo = aleatorio;
            System.err.println("[aviso] APP_SECRET não definida. Os logins serão perdidos ao reiniciar.");
        } else {
            this.segredo = segredoDoAmbiente.getBytes(StandardCharsets.UTF_8);
        }
    }

    public String criar(String id, String perfil) throws IOException {
        long expiraEm = Instant.now().getEpochSecond() + VALIDADE_SEGUNDOS;
        Map<String, Object> payload = Map.of("sub", id, "perfil", perfil, "exp", expiraEm);
        String corpo = codificar(Json.MAPPER.writeValueAsBytes(payload));
        return corpo + "." + codificar(assinar(corpo));
    }

    /** @return o payload (sub, perfil, exp) se o token for válido e não tiver expirado; senão null */
    public Map<String, Object> verificar(String token) {
        if (token == null) {
            return null;
        }
        String[] partes = token.split("\\.");
        if (partes.length != 2) {
            return null;
        }
        try {
            byte[] esperada = assinar(partes[0]);
            byte[] recebida = Base64.getUrlDecoder().decode(partes[1]);
            if (!MessageDigest.isEqual(esperada, recebida)) {
                return null;
            }
            byte[] json = Base64.getUrlDecoder().decode(partes[0]);
            Map<?, ?> payload = Json.MAPPER.readValue(json, Map.class);
            long expiraEm = ((Number) payload.get("exp")).longValue();
            if (Instant.now().getEpochSecond() > expiraEm) {
                return null;
            }
            return Map.of("sub", String.valueOf(payload.get("sub")), "perfil", String.valueOf(payload.get("perfil")));
        } catch (IllegalArgumentException | IOException | NullPointerException e) {
            // Base64 malformado, JSON inválido ou campos faltando: token rejeitado
            return null;
        }
    }

    private byte[] assinar(String texto) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(segredo, "HmacSHA256"));
            return mac.doFinal(texto.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC-SHA256 indisponível na JVM", e);
        }
    }

    private static String codificar(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
