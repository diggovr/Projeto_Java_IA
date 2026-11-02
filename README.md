# Sistema Escolar — Java + Claude

Sistema escolar para a disciplina de Java com integração a um LLM. Você cadastra usuários, cursos e alunos, e a tela **Relatório com IA** mostra um texto escrito pelo Claude a partir de contagens feitas pelo próprio Java.

Este guia é um passo a passo. Siga na ordem.

---

## Parte 1 — Preparar o computador

Você precisa de:

1. **JDK 21** (ou mais novo). Teste no terminal:
   ```bash
   java -version
   ```
   Deve aparecer `version "21"` ou superior.

2. **Maven 3.9+**. Teste:
   ```bash
   mvn -version
   ```

3. **Google Cloud SDK (`gcloud`)**, para rodar o banco de teste (emulador). Teste:
   ```bash
   gcloud --version
   ```
   Instale o componente do emulador, se ainda não tiver:
   ```bash
   gcloud components install cloud-firestore-emulator
   ```

4. **Chave da API do Claude** (opcional). Sem ela, o sistema funciona, mas a tela de relatório mostra um aviso. Você pega a chave no console da Anthropic.

---

## Parte 2 — Rodar o sistema no seu computador

Você vai usar **dois terminais** abertos ao mesmo tempo.

### Passo 1 — Terminal 1: ligar o banco de teste

```bash
gcloud emulators firestore start --host-port=localhost:8080
```

Deixe esse terminal aberto. Ele precisa ficar rodando.

### Passo 2 — Terminal 2: montar o projeto

Entre na pasta do projeto e gere o arquivo executável:

```bash
cd cadastro-alunos
mvn -q package
```

Ao terminar, deve existir o arquivo `target/app.jar`.

### Passo 3 — Terminal 2: configurar as variáveis

No mesmo terminal, copie e cole estas linhas:

```bash
export FIRESTORE_EMULATOR_HOST=localhost:8080
export GOOGLE_CLOUD_PROJECT=demo-cadastro-alunos
export ADMIN_EMAIL=admin@escola.com
export ADMIN_SENHA=senha123
export ANTHROPIC_API_KEY=sua-chave-aqui
export PORT=8081
```

- `ADMIN_EMAIL` e `ADMIN_SENHA` criam o primeiro usuário, que você usa para entrar. A senha precisa ter pelo menos 6 caracteres.
- Se não tiver chave do Claude, apague a linha `ANTHROPIC_API_KEY`. Um valor falso não serve: o sistema funciona, mas o relatório mostra erro de conexão com o Claude.
- No Windows PowerShell, use `$env:NOME="valor"` no lugar de `export NOME=valor`.

### Passo 4 — Ligar a aplicação

```bash
java -jar target/app.jar
```

Quando aparecer `Sistema escolar rodando em http://localhost:8081`, está pronto.

### Passo 5 — Abrir no navegador

Acesse **http://localhost:8081** e entre com `admin@escola.com` e `senha123` (ou os valores que você escolheu no passo 3).

### Passo 6 — Testar

Confira nesta ordem:

1. **Usuários:** cadastre um professor (perfil `professor`).
2. **Cursos:** cadastre um curso, por exemplo "Análise e Desenvolvimento de Sistemas".
3. **Alunos:** matricule um aluno no curso. Tente um e-mail sem `@` e veja a mensagem de erro.
4. **Relatório com IA:** abra a tela e clique em gerar. Com a chave configurada, o texto aparece. Sem chave, aparece um aviso.

### Para desligar

No Terminal 2, pressione `Ctrl+C`. No Terminal 1, faça o mesmo.

### Problemas comuns

| O que aparece | O que fazer |
|---|---|
| `Não foi possível concluir a operação` nas listas | O emulador não está rodando. Confira o Terminal 1 e o `FIRESTORE_EMULATOR_HOST`. |
| `Email ou senha inválidos` ao entrar | Confira `ADMIN_EMAIL` e `ADMIN_SENHA`. O administrador só é criado se ainda não existir. |
| `ANTHROPIC_API_KEY não configurada` | Defina a variável no Terminal 2 e reinicie o `java -jar`. |
| Porta em uso | Troque `PORT` por outro número, como `8082`. |
| Tela não atualiza depois de uma mudança no front | No navegador, recarregue com `Ctrl+Shift+R`. Se for PWA, aumente `CACHE_NAME` em `sw.js`. |

---

## Parte 3 — Colocar no ar (Google Cloud Run)

Use este roteiro quando quiser publicar o sistema na internet. Isso usa o seu projeto no Google Cloud e pode gerar cobrança. Configure um alerta de orçamento antes.

### Passo 1 — Escolher o projeto e ativar os serviços

```bash
gcloud config set project SEU-PROJETO-ID
gcloud services enable run.googleapis.com firestore.googleapis.com secretmanager.googleapis.com
gcloud firestore databases create --location=southamerica-east1
```

### Passo 2 — Guardar a chave do Claude

```bash
echo -n "SUA_CHAVE" | gcloud secrets create anthropic-api-key --data-file=-
```

### Passo 3 — Dar permissões à conta do Cloud Run

Descubra o número do projeto:

```bash
gcloud projects describe SEU-PROJETO-ID --format="value(projectNumber)"
```

Com esse número, conceda os papéis (troque `NUMERO`):

```bash
gcloud projects add-iam-policy-binding SEU-PROJETO-ID \
  --member="serviceAccount:NUMERO-compute@developer.gserviceaccount.com" --role="roles/datastore.user"
gcloud secrets add-iam-policy-binding anthropic-api-key \
  --member="serviceAccount:NUMERO-compute@developer.gserviceaccount.com" --role="roles/secretmanager.secretAccessor"
```

### Passo 4 — Publicar

Na pasta `cadastro-alunos`:

```bash
gcloud run deploy sistema-escolar --source . --region southamerica-east1 \
  --allow-unauthenticated --min-instances 0 --max-instances 3 \
  --set-secrets ANTHROPIC_API_KEY=anthropic-api-key:latest \
  --set-env-vars ADMIN_EMAIL=admin@escola.com,ADMIN_SENHA=troque-esta-senha
```

Ao final, o terminal mostra a URL do sistema. Abra no navegador e entre com o administrador.

### Passo 5 — Depois do primeiro acesso

- Troque a senha do administrador ou remova `ADMIN_SENHA` do deploy. A variável só cria o usuário se ele ainda não existir, então não causa problema depois.
- No dia da aula, para evitar demora no primeiro acesso:
  ```bash
  gcloud run services update sistema-escolar --region southamerica-east1 --min-instances 1
  ```
  Depois da aula, volte para `0`.

---

## Parte 4 — Entender o projeto

- **`src/main/java/br/edu/aula/`:** código Java, separado em `model` (dados e validação), `dao` (Firestore), `llm` (Claude), `web` (rotas).
- **`src/main/resources/static/`:** telas em HTML, CSS e JavaScript.

### Rotas da API

| Método | Caminho | Função |
|---|---|---|
| GET | `/health` | Verifica se o servidor está no ar |
| POST | `/login` | Entrada com e-mail e senha |
| GET, POST | `/usuarios` | Lista e cria usuários |
| DELETE | `/usuarios/{id}` | Exclui usuário |
| GET, POST | `/cursos` | Lista e cria cursos |
| DELETE | `/cursos/{id}` | Exclui curso |
| GET, POST | `/alunos` | Lista e matricula alunos |
| DELETE | `/alunos/{id}` | Exclui aluno |
| GET | `/relatorio/narrativa` | Relatório escrito pelo Claude |

Erros voltam como `{"detail": "mensagem"}`.

### Segurança

- A chave do Claude fica no servidor. O navegador nunca a vê.
- O Claude recebe só contagens e nomes de cursos. Nenhum nome ou e-mail de aluno é enviado.
- Senhas são guardadas como hash SHA-256. Em um sistema real, use bcrypt ou argon2.
