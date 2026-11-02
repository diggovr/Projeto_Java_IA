package br.edu.aula.model;

import java.util.List;

/**
 * Curso oferecido pela escola. Ao receber do formulário, id vem nulo;
 * o DAO preenche o id ao salvar.
 */
public record Curso(String id, String nome, String descricao, Integer cargaHoraria, String modalidade) {

    public static final List<String> MODALIDADES = List.of("Presencial", "EAD", "Híbrido");

    /** @return mensagem de erro em português, ou null se estiver tudo certo */
    public static String validar(Curso c) {
        if (Validacao.vazio(c.nome())) {
            return "O nome do curso é obrigatório.";
        }
        if (Validacao.longo(c.nome())) {
            return "O nome do curso deve ter no máximo 120 caracteres.";
        }
        if (c.descricao() != null && c.descricao().length() > 500) {
            return "A descrição deve ter no máximo 500 caracteres.";
        }
        if (c.cargaHoraria() != null && (c.cargaHoraria() < 1 || c.cargaHoraria() > 5000)) {
            return "A carga horária deve estar entre 1 e 5000 horas.";
        }
        if (c.modalidade() != null && !MODALIDADES.contains(c.modalidade())) {
            return "Modalidade inválida.";
        }
        return null;
    }
}
