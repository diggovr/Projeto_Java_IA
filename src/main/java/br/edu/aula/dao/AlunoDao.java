package br.edu.aula.dao;

import br.edu.aula.model.Aluno;
import br.edu.aula.model.Curso;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Acesso à coleção "alunos" do Firestore. */
public class AlunoDao {

    private static final String COLECAO = "alunos";

    private final CursoDao cursos;

    public AlunoDao(CursoDao cursos) {
        this.cursos = cursos;
    }

    private CollectionReference colecao() {
        return Firestores.get().collection(COLECAO);
    }

    public List<Aluno> listar() throws Exception {
        // Um único passe pelos cursos para descobrir o nome de cada id
        Map<String, String> nomesDosCursos = new HashMap<>();
        for (Curso curso : cursos.listar()) {
            nomesDosCursos.put(curso.id(), curso.nome());
        }

        List<Aluno> alunos = new ArrayList<>();
        for (QueryDocumentSnapshot doc : colecao().get().get().getDocuments()) {
            alunos.add(paraAluno(doc, nomesDosCursos));
        }
        return alunos;
    }

    /** @return o aluno criado, ou null se o e-mail já estiver em uso */
    public Aluno criar(Aluno a) throws Exception {
        if (existeEmail(a.email())) {
            return null;
        }
        String cursoId = a.cursoId() == null || a.cursoId().isBlank() ? null : a.cursoId();
        Curso curso = cursoId == null ? null : cursos.buscar(cursoId);

        Map<String, Object> dados = new HashMap<>();
        dados.put("nome", a.nome());
        dados.put("email", a.email());
        dados.put("cpf", textoOuVazio(a.cpf()));
        dados.put("data_nasc", textoOuVazio(a.dataNasc()));
        dados.put("telefone", textoOuVazio(a.telefone()));
        dados.put("curso_id", cursoId);

        String id = colecao().add(dados).get().getId();
        return new Aluno(id, a.nome(), a.email(), textoOuVazio(a.cpf()), textoOuVazio(a.dataNasc()),
                textoOuVazio(a.telefone()), cursoId, curso == null ? null : curso.nome());
    }

    public void excluir(String id) throws Exception {
        colecao().document(id).delete().get();
    }

    private boolean existeEmail(String email) throws Exception {
        return !colecao().whereEqualTo("email", email).limit(1).get().get().getDocuments().isEmpty();
    }

    private Aluno paraAluno(DocumentSnapshot doc, Map<String, String> nomesDosCursos) {
        String cursoId = doc.getString("curso_id");
        return new Aluno(
                doc.getId(),
                doc.getString("nome"),
                doc.getString("email"),
                doc.getString("cpf"),
                doc.getString("data_nasc"),
                doc.getString("telefone"),
                cursoId,
                cursoId == null ? null : nomesDosCursos.get(cursoId));
    }

    private static String textoOuVazio(String texto) {
        return texto == null ? "" : texto;
    }
}
