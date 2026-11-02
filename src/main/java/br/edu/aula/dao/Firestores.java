package br.edu.aula.dao;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;

/**
 * Guarda a única conexão com o Firestore da aplicação.
 *
 * A conexão é criada só no primeiro uso. Assim a aplicação sobe mesmo sem
 * projeto GCP configurado: a casca e o /health continuam funcionando.
 */
public final class Firestores {

    private static Firestore db;

    private Firestores() {
    }

    public static synchronized Firestore get() {
        if (db == null) {
            // Localmente, defina GOOGLE_CLOUD_PROJECT; no Cloud Run ele é detectado
            String projeto = System.getenv("GOOGLE_CLOUD_PROJECT");
            FirestoreOptions opcoes = projeto == null
                    ? FirestoreOptions.getDefaultInstance()
                    : FirestoreOptions.newBuilder().setProjectId(projeto).build();
            db = opcoes.getService();
        }
        return db;
    }
}
