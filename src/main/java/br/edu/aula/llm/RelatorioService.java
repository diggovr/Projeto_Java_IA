package br.edu.aula.llm;

import br.edu.aula.dao.AlunoDao;
import br.edu.aula.dao.CursoDao;
import br.edu.aula.dao.UsuarioDao;
import br.edu.aula.model.Aluno;
import br.edu.aula.model.Curso;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Monta o relatório com IA: conta os dados em Java, pede ao Claude o texto
 * e devolve esse texto junto com os números.
 *
 * O Claude recebe só contagens e nomes de cursos. Nenhum aluno ou e-mail é enviado.
 */
public class RelatorioService {

    private static final String PROMPT_SISTEMA =
            "Você é um analista educacional da RS Treinamento & Consultoria. "
            + "Responda somente com o texto da narrativa, em português.";

    private static final String MODELO_PROMPT = """
            Analise os dados do sistema escolar abaixo e escreva uma narrativa profissional em português.
            Use no máximo 4 parágrafos curtos. Mencione destaques e, se relevante, pontos de atenção.

            DADOS DO SISTEMA:
            - Total de alunos matriculados: %d
            - Total de cursos disponíveis: %d
            - Total de usuários no sistema: %d

            DISTRIBUIÇÃO POR CURSO:
            %s

            Escreva a narrativa agora:""";

    private final UsuarioDao usuarios;
    private final CursoDao cursos;
    private final AlunoDao alunos;
    private final ClaudeClient claude;

    public RelatorioService(UsuarioDao usuarios, CursoDao cursos, AlunoDao alunos, ClaudeClient claude) {
        this.usuarios = usuarios;
        this.cursos = cursos;
        this.alunos = alunos;
        this.claude = claude;
    }

    /**
     * @throws IllegalStateException se a chave do Claude não estiver configurada
     * @throws java.io.IOException   se a API do Claude falhar
     */
    public Map<String, Object> gerarNarrativa() throws Exception {
        if (!claude.configurado()) {
            throw new IllegalStateException(
                    "ANTHROPIC_API_KEY não configurada. Defina a variável de ambiente com sua chave.");
        }

        List<Aluno> listaAlunos = alunos.listar();
        List<Curso> listaCursos = cursos.listar();
        int totalUsuarios = usuarios.listar().size();

        // Quantos alunos em cada curso (id -> total)
        Map<String, Long> porCurso = listaAlunos.stream()
                .filter(a -> a.cursoId() != null)
                .collect(Collectors.groupingBy(Aluno::cursoId, Collectors.counting()));

        String distribuicao = listaCursos.stream()
                .map(c -> "  - %s (%s): %d aluno(s)".formatted(
                        c.nome(), c.modalidade(), porCurso.getOrDefault(c.id(), 0L)))
                .collect(Collectors.joining("\n"));
        if (distribuicao.isEmpty()) {
            distribuicao = "  (Nenhum curso cadastrado ainda)";
        }

        String prompt = MODELO_PROMPT.formatted(
                listaAlunos.size(), listaCursos.size(), totalUsuarios, distribuicao);
        String narrativa = claude.enviar(PROMPT_SISTEMA, prompt);

        return Map.of(
                "narrativa", narrativa,
                "modelo", claude.modelo(),
                "dados", Map.of(
                        "total_alunos", listaAlunos.size(),
                        "total_cursos", listaCursos.size(),
                        "total_usuarios", totalUsuarios));
    }
}
