package br.edu.aula.web;

import br.edu.aula.llm.RelatorioService;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** /relatorio/narrativa: devolve o texto escrito pelo Claude a partir dos dados agregados. */
public class RelatorioServlet extends HttpServlet {

    private final RelatorioService servico;

    public RelatorioServlet(RelatorioService servico) {
        this.servico = servico;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            Json.enviar(resp, 200, servico.gerarNarrativa());
        } catch (IllegalStateException e) {
            // Chave do Claude não configurada
            Json.erro(resp, 503, e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Json.erro(resp, 502, "A chamada ao Claude foi interrompida.");
        } catch (IOException e) {
            System.err.println("[erro] Claude: " + e.getMessage());
            Json.erro(resp, 502, "Erro ao consultar a API do Claude. Tente novamente.");
        } catch (Exception e) {
            Json.falhaInterna(resp, "gerar relatório", e);
        }
    }
}
