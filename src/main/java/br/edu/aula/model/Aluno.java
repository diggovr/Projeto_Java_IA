package br.edu.aula.model;

/**
 * Aluno matriculado. O curso é guardado pelo id; o nome do curso
 * (curso_nome) é preenchido na leitura, para a lista mostrar o nome.
 */
public record Aluno(
        String id,
        String nome,
        String email,
        String cpf,
        String dataNasc,
        String telefone,
        String cursoId,
        String cursoNome) {

    /** @return mensagem de erro em português, ou null se estiver tudo certo */
    public static String validar(Aluno a) {
        if (Validacao.vazio(a.nome())) {
            return "O nome é obrigatório.";
        }
        if (Validacao.vazio(a.email()) || !Validacao.emailValido(a.email())) {
            return "Informe um e-mail válido.";
        }
        if (Validacao.longo(a.nome()) || Validacao.longo(a.email())) {
            return "Nome e e-mail devem ter no máximo 120 caracteres.";
        }
        return null;
    }
}
