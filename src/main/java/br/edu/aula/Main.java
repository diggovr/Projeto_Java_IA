package br.edu.aula;

import br.edu.aula.dao.AlunoDao;
import br.edu.aula.dao.CursoDao;
import br.edu.aula.dao.UsuarioDao;
import br.edu.aula.llm.ClaudeClient;
import br.edu.aula.llm.RelatorioService;
import br.edu.aula.web.AlunosServlet;
import br.edu.aula.web.CursosServlet;
import br.edu.aula.web.HealthServlet;
import br.edu.aula.web.LoginServlet;
import br.edu.aula.web.RelatorioServlet;
import br.edu.aula.web.UsuariosServlet;
import org.eclipse.jetty.ee10.servlet.DefaultServlet;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.resource.ResourceFactory;

/**
 * Ponto de entrada da aplicação.
 *
 * Sobe um Jetty embutido (sem Tomcat instalado), registra as rotas da API
 * e serve o front-end (páginas HTML, CSS, JS e PWA) a partir de /static.
 */
public class Main {

    public static void main(String[] args) throws Exception {
        // O Cloud Run injeta PORT; localmente o padrão é 8080
        int porta = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));

        // Dependências montadas à mão (sem framework de injeção), para ficar didático
        UsuarioDao usuarios = new UsuarioDao();
        CursoDao cursos = new CursoDao();
        AlunoDao alunos = new AlunoDao(cursos);
        ClaudeClient claude = new ClaudeClient(
                System.getenv("ANTHROPIC_API_KEY"),
                System.getenv().getOrDefault("CLAUDE_MODEL", "claude-haiku-5-5"));
        RelatorioService relatorio = new RelatorioService(usuarios, cursos, alunos, claude);

        criarAdminInicial(usuarios);

        Server servidor = new Server(porta);

        ServletContextHandler contexto = new ServletContextHandler();
        contexto.setContextPath("/");
        contexto.setWelcomeFiles(new String[] {"index.html"});
        contexto.getMimeTypes().addMimeMapping("webmanifest", "application/manifest+json");

        // Rotas da API (mesmos caminhos que o front-end já usa)
        contexto.addServlet(new ServletHolder(new HealthServlet()), "/health");
        contexto.addServlet(new ServletHolder(new LoginServlet(usuarios)), "/login");
        contexto.addServlet(new ServletHolder(new UsuariosServlet(usuarios)), "/usuarios/*");
        contexto.addServlet(new ServletHolder(new CursosServlet(cursos)), "/cursos/*");
        contexto.addServlet(new ServletHolder(new AlunosServlet(alunos, cursos)), "/alunos/*");
        contexto.addServlet(new ServletHolder(new RelatorioServlet(relatorio)), "/relatorio/*");

        // Páginas e arquivos estáticos vindos de src/main/resources/static
        contexto.setBaseResource(ResourceFactory.of(servidor).newClassLoaderResource("static/"));
        contexto.addServlet(DefaultServlet.class, "/");

        servidor.setHandler(contexto);
        servidor.start();
        System.out.println("Sistema escolar rodando em http://localhost:" + porta);
        servidor.join();
    }

    /**
     * Sem usuários não há como fazer login. Se ADMIN_EMAIL e ADMIN_SENHA estiverem
     * definidas, cria o administrador inicial na subida (só se ainda não existir).
     */
    private static void criarAdminInicial(UsuarioDao usuarios) {
        String email = System.getenv("ADMIN_EMAIL");
        String senha = System.getenv("ADMIN_SENHA");
        if (email == null || senha == null) {
            return;
        }
        try {
            usuarios.garantirAdmin(email, senha);
        } catch (Exception e) {
            System.err.println("[aviso] Não foi possível criar o administrador inicial: " + e);
        }
    }
}
