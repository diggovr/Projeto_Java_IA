package br.edu.aula.dao;

import br.edu.aula.model.Usuario;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Acesso à coleção "usuarios" do Firestore. */
public class UsuarioDao {

    private static final String COLECAO = "usuarios";

    private CollectionReference colecao() {
        return Firestores.get().collection(COLECAO);
    }

    public List<Usuario> listar() throws Exception {
        List<Usuario> usuarios = new ArrayList<>();
        for (QueryDocumentSnapshot doc : colecao().get().get().getDocuments()) {
            usuarios.add(paraUsuario(doc));
        }
        return usuarios;
    }

    /** @return o usuário criado, ou null se o e-mail já existir */
    public Usuario criar(Usuario.Entrada e) throws Exception {
        if (existeEmail(e.email())) {
            return null;
        }
        String perfil = e.perfil() == null ? "aluno" : e.perfil();

        Map<String, Object> dados = new HashMap<>();
        dados.put("nome", e.nome());
        dados.put("email", e.email());
        dados.put("senhaHash", Usuario.hashSenha(e.senha()));
        dados.put("perfil", perfil);

        DocumentReference ref = colecao().add(dados).get();
        return new Usuario(ref.getId(), e.nome(), e.email(), perfil);
    }

    /** @return o usuário com esse id, ou null se ele não existir (ex.: foi excluído) */
    public Usuario buscarPorId(String id) throws Exception {
        DocumentSnapshot doc = colecao().document(id).get().get();
        return doc.exists() ? paraUsuario(doc) : null;
    }

    public void excluir(String id) throws Exception {
        colecao().document(id).delete().get();
    }

    /** @return o usuário se e-mail e senha conferirem; senão null */
    public Usuario autenticar(String email, String senha) throws Exception {
        List<QueryDocumentSnapshot> docs = colecao().whereEqualTo("email", email)
                .limit(1).get().get().getDocuments();
        if (docs.isEmpty()) {
            return null;
        }
        QueryDocumentSnapshot doc = docs.get(0);
        boolean senhaConfere = Usuario.senhaConfere(senha, doc.getString("senhaHash"));
        return senhaConfere ? paraUsuario(doc) : null;
    }

    /** Cria o administrador inicial, se ainda não existir (usado na subida da aplicação). */
    public void garantirAdmin(String email, String senha) throws Exception {
        if (!existeEmail(email)) {
            criar(new Usuario.Entrada("Administrador", email, senha, "admin"));
        }
    }

    private boolean existeEmail(String email) throws Exception {
        return !colecao().whereEqualTo("email", email).limit(1).get().get().getDocuments().isEmpty();
    }

    private Usuario paraUsuario(DocumentSnapshot doc) {
        return new Usuario(doc.getId(), doc.getString("nome"), doc.getString("email"), doc.getString("perfil"));
    }
}
