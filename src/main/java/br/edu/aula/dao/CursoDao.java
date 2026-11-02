package br.edu.aula.dao;

import br.edu.aula.model.Curso;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Acesso à coleção "cursos" do Firestore. */
public class CursoDao {

    private static final String COLECAO = "cursos";

    private CollectionReference colecao() {
        return Firestores.get().collection(COLECAO);
    }

    public List<Curso> listar() throws Exception {
        List<Curso> cursos = new ArrayList<>();
        for (QueryDocumentSnapshot doc : colecao().get().get().getDocuments()) {
            cursos.add(paraCurso(doc));
        }
        return cursos;
    }

    /** @return o curso com esse id, ou null se não existir */
    public Curso buscar(String id) throws Exception {
        DocumentSnapshot doc = colecao().document(id).get().get();
        return doc.exists() ? paraCurso(doc) : null;
    }

    public Curso criar(Curso c) throws Exception {
        String descricao = c.descricao() == null ? "" : c.descricao();
        String modalidade = c.modalidade() == null ? "Presencial" : c.modalidade();

        Map<String, Object> dados = new HashMap<>();
        dados.put("nome", c.nome());
        dados.put("descricao", descricao);
        dados.put("carga_horaria", c.cargaHoraria());
        dados.put("modalidade", modalidade);

        String id = colecao().add(dados).get().getId();
        return new Curso(id, c.nome(), descricao, c.cargaHoraria(), modalidade);
    }

    public void excluir(String id) throws Exception {
        colecao().document(id).delete().get();
    }

    private Curso paraCurso(DocumentSnapshot doc) {
        Long cargaHoraria = doc.getLong("carga_horaria");
        return new Curso(
                doc.getId(),
                doc.getString("nome"),
                doc.getString("descricao"),
                cargaHoraria == null ? null : cargaHoraria.intValue(),
                doc.getString("modalidade"));
    }
}
