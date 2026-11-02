package br.edu.aula.web;

import br.edu.aula.dao.AlunoDao;
import br.edu.aula.dao.CursoDao;
import br.edu.aula.model.Aluno;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

/** /alunos: GET lista, POST matricula, DELETE /alunos/{id} exclui. */
public class AlunosServlet extends HttpServlet {

    private final AlunoDao alunos;
    private final CursoDao cursos;

    public AlunosServlet(AlunoDao alunos, CursoDao cursos) {
        this.alunos = alunos;
        this.cursos = cursos;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            Json.enviar(resp, 200, alunos.listar());
        } catch (Exception e) {
            Json.falhaInterna(resp, "listar alunos", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Aluno entrada;
        try {
            entrada = Json.ler(req, Aluno.class);
        } catch (JsonProcessingException e) {
            Json.erro(resp, 400, "Dados inválidos.");
            return;
        }

        String erro = Aluno.validar(entrada);
        if (erro != null) {
            Json.erro(resp, 400, erro);
            return;
        }

        try {
            String cursoId = entrada.cursoId();
            if (cursoId != null && !cursoId.isBlank() && cursos.buscar(cursoId) == null) {
                Json.erro(resp, 400, "Curso não encontrado.");
                return;
            }
            Aluno criado = alunos.criar(entrada);
            if (criado == null) {
                Json.erro(resp, 400, "E-mail já cadastrado para outro aluno.");
            } else {
                Json.enviar(resp, 201, criado);
            }
        } catch (Exception e) {
            Json.falhaInterna(resp, "criar aluno", e);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String id = Json.idDaUrl(req);
        if (id == null) {
            Json.erro(resp, 400, "Informe o id do aluno.");
            return;
        }
        try {
            alunos.excluir(id);
            Json.enviar(resp, 200, Map.of("mensagem", "Aluno excluído."));
        } catch (Exception e) {
            Json.falhaInterna(resp, "excluir aluno", e);
        }
    }
}
