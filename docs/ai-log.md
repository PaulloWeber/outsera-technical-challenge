# Registro de interações com IA

Este documento atende ao item 5.2 da especificação. Registra as interações que
conduziram a construção do projeto, organizadas em fases, com as decisões tomadas
em cada uma e a forma como o resultado foi verificado.

**Ferramenta:** Claude Opus 5 (Anthropic), via Claude Code no terminal.

Cada fase segue a mesma estrutura: o **prompt** que a iniciou, **o que foi
produzido**, as **decisões** tomadas e a **verificação** do resultado. O último
bloco é deliberado — o retorno da IA foi checado a cada etapa, não aceito por
padrão.

---

## Fase 1 — Análise do desafio

### Prompt

> Analise o documento de especificação do desafio e o arquivo `Movielist.csv`
> anexo. Traga uma visão dos passos necessários para a implementação,
> identificando requisitos funcionais, requisitos não funcionais e os pontos de
> atenção do conjunto de dados.

### O que foi produzido

Um levantamento dos requisitos separados por natureza:

| Origem | Requisito |
|---|---|
| Item 2.1 | Ler o CSV e popular a base ao iniciar a aplicação |
| Item 3.1 | Endpoint com maior e menor intervalo entre prêmios consecutivos |
| Item 4.1 | REST no nível 2 de maturidade de Richardson |
| Item 4.2 | Somente testes de integração |
| Item 4.3 | Banco em memória, SGBD embarcado, sem instalação externa |
| Item 4.4 | README com instruções de execução |
| Item 6 | Formato JSON exato: `min` / `max` com quatro campos |

### Pontos de atenção identificados no CSV

A leitura do arquivo revelou quatro características que qualquer implementação
correta precisa tratar, e que a caixa de *Atenção* da especificação reforça ao
avisar que outros conjuntos de dados serão usados na avaliação:

1. **Três formatos de crédito na mesma coluna** — `"Allan Carr"`,
   `"Ted Field and Robert W. Cort"` e
   `"Debra Hayward, Tim Bevan, Eric Fellner, and Tom Hooper"`, esta última com
   vírgula antes do `and`.
2. **Anos com mais de um vencedor** — 1986, 1990 e 2015.
3. **Anos sem vencedor marcado** — 2007.
4. **Nomes quase idênticos que são pessoas distintas** — `Michael DeLuca`
   (2008) e `Michael De Luca` (2015). Comparação literal é obrigatória.

### Decisão

Tratar `min` e `max` como listas desde o início, e não como objetos únicos. A
especificação mostra arrays no exemplo, e empates são inevitáveis em outros
conjuntos de dados.

---

## Fase 2 — Estruturação do projeto

### Prompt

> Estruture um projeto em Java organizado em **Clean Architecture**, com a regra
> de negócio isolada de framework, persistência e HTTP. A cobertura deve ser
> feita **exclusivamente por testes de integração**, conforme o item 4.2 — sem
> testes unitários e sem mocks.

### Stack definida

| Camada | Tecnologia | Motivo |
|---|---|---|
| Linguagem | Java 21 (LTS) | `record` para modelos imutáveis |
| Framework | Spring Boot 4 | Autoconfiguração e servidor embarcado |
| Web | Spring Web MVC | Controllers REST |
| Persistência | Spring Data JPA + Hibernate | Repositórios e mapeamento |
| Banco | H2 em memória | Atende ao item 4.3 sem instalação |
| Segurança | Spring Security + OAuth2 Resource Server | JWT HS256 autoemitido |
| Build | Maven Wrapper | Roda sem Maven instalado |
| Container | Docker multi-stage com `jlink` | Execução sem JDK local |

Nenhuma biblioteca de terceiros além do próprio Spring. O CSV é lido com
`BufferedReader`, sem OpenCSV; os DTOs são `record` nativos, sem Lombok.

### Organização em camadas

```
domain/          regra de negócio — importa apenas java.*
application/     casos de uso e as portas que eles declaram
adapter/         entrada (web) e saída (JPA, CSV)
infrastructure/  fiação do framework
```

A regra que sustenta o desenho: **as dependências apontam só para dentro**. O
domínio não conhece ninguém; a aplicação conhece o domínio; os adaptadores
conhecem os dois. `MoviePersistenceAdapter` depende de `MovieRepositoryPort`, e
não o contrário — é a inversão de dependência que permite trocar H2 por outro
banco mexendo apenas em `adapter/`.

### Estratégia de testes

Testes de integração, conforme exigido. Todos sobem a aplicação completa em porta
aleatória com `@SpringBootTest(RANDOM_PORT)` e chamam a API por HTTP real via
`TestRestTemplate`. Sem `@MockBean`, sem `@WebMvcTest`.

Uma consequência de projeto vale registrar: como não há testes unitários, o código
**não precisa** ser desenhado para ser mockável. Isso dispensa interfaces
artificiais em volta de cada serviço e permite um desenho mais direto.

---

## Fase 3 — Implementação dos requisitos funcionais

### Prompt

> Implemente os requisitos funcionais seguindo boas práticas: regra de negócio
> isolada no domínio, validação completa antes de qualquer escrita, resposta no
> formato exato da especificação e todos os empates retornados.

### O que foi implementado

**Carga na inicialização (item 2.1).** `MovieCsvLoader` roda via
`SmartInitializingSingleton`, garantindo que os repositórios já existam. O caminho
vem da propriedade `app.movies-csv`, que aceita `classpath:` ou `file:` — assim
outro arquivo pode ser usado sem recompilar.

**Validação antes da escrita.** O parser valida o arquivo inteiro e **acumula**
os erros com número de linha, em vez de abortar no primeiro. A gravação é
transacional: ou o conjunto inteiro entra, ou nada muda. Um upload malformado
devolve `400` e deixa os dados anteriores intactos.

**A regra de negócio.** `AwardIntervalCalculator` recebe uma lista de vitórias,
agrupa por produtor, ordena os anos e percorre os pares consecutivos. Quem venceu
uma vez só não gera intervalo e desaparece naturalmente do resultado. A classe não
tem anotação nenhuma: pode ser instanciada com `new` e exercitada sem subir nada.

**Empates.** O método que apura mínimo e máximo devolve **todos** os produtores
com aquele intervalo, ordenados por `previousWin` e depois pelo nome.

### Decisão de projeto e seu custo

Uma versão anterior calculava os intervalos com uma *window function* no banco:

```sql
lag(m.release_year) over (partition by p.name order by m.release_year)
```

Era mais curta e mais rápida — porém colocava a regra de negócio na camada de
persistência. A escolha foi movê-la para o domínio, aceitando o custo em
performance para ganhar independência de tecnologia: a regra hoje roda sem banco,
sem Spring e sem H2. O repositório voltou a fazer só o que lhe cabe, que é buscar
pares `(produtor, ano)`.

### Verificação

Uma inconsistência de arquitetura foi encontrada e corrigida: o tratador de
exceções respondia `"Invalid CSV file"` para uma `InvalidMovieDataException` — mas
essa exceção é o contrato de `MovieParserPort`, que existe justamente para ser
agnóstico de formato. Um adaptador JSON futuro lançaria a mesma exceção e o cliente
receberia uma mensagem sobre CSV. A mensagem passou a ser `"Invalid movie data"`,
e o detalhe específico ficou no array `errors`.

---

## Fase 4 — Endurecimento de segurança

### Prompt

> Externalize o segredo de assinatura JWT para variável de ambiente, de modo que
> nenhuma credencial de assinatura fique versionada — sem criar atrito para quem
> for avaliar o projeto.

### O problema

O segredo de assinatura estava fixo no `application.properties`. Mesmo sendo um
valor de demonstração, ficaria no histórico do Git permanentemente, e é a chave
que permite forjar tokens.

A solução ingênua — exigir a variável de ambiente — quebraria o requisito
implícito de que o avaliador consiga rodar o projeto sem configuração.

### O que foi implementado

```properties
app.security.jwt-secret=${JWT_SECRET:}
```

O comportamento passou a depender do ambiente, em três caminhos:

| `JWT_SECRET` | Comportamento |
|---|---|
| Não definido | Gera chave aleatória de 256 bits na subida e registra aviso no log |
| Definido, ≥ 32 bytes | Usa a chave informada |
| Definido, < 32 bytes | Falha na subida com mensagem explícita |

A chave aleatória por execução é o que preserva o zero-atrito: o comando de
subida continua sendo apenas `./mvnw spring-boot:run`. O único efeito é que um
token não sobrevive a um reinício — irrelevante para um banco que também é
apagado junto.

A validação de tamanho mínimo é uma melhoria independente: antes, uma chave curta
demais falhava dentro da biblioteca Nimbus com erro obscuro; agora falha na
criação do bean, informando quantos bytes vieram.

### Verificação

Os três caminhos foram exercitados com a aplicação real:

- **Sem a variável** — 19 testes passaram e o aviso apareceu no log
- **Com chave válida** — subida em 6,9 s sem aviso; token emitido e validado
- **Com chave de 12 bytes** — `JWT_SECRET must be at least 32 bytes for HS256, got 12`

As senhas de demonstração foram mantidas no arquivo de propriedades de forma
consciente: elas dão acesso apenas a um banco em memória, e removê-las impediria
o avaliador de autenticar sem configuração prévia. A decisão está documentada no
README.

---

## Fase 5 — Escrita do README

### Prompt

> Escreva o README em formato de referência: quickstart antes de qualquer
> explicação, a arquitetura como tese central do documento, e tabelas no lugar de
> prosa onde a tabela informar melhor.

### Estrutura adotada

O documento foi organizado pela ordem em que o leitor precisa das informações, e
não por tópico:

1. **Quickstart** — um comando, a saída real do log e a resposta JSON esperada
2. **Arquitetura** — a regra de dependência enunciada, com diagrama e tabela por camada
3. **Fluxos** — carga na inicialização e ciclo de uma requisição
4. **Referência da API** — rotas, permissões e contratos em tabelas
5. **Testes** — o que cada classe cobre e os dois testes mais relevantes
6. **Configuração** — propriedades sobrescrevíveis
7. **Decisões e trade-offs**

### A escolha que define o documento

A tabela de decisões tem três colunas: a decisão, o motivo e **o que ela custou**.
Essa terceira coluna é deliberada — registra que a versão com *window function*
era mais rápida, que separar `record` de entidade JPA custa uma etapa de
mapeamento, que expor a rota de leitura duas vezes é o mesmo recurso sob dois
URIs. Um README que só lista virtudes não informa; um que declara o preço, sim.

### Verificação

Cada afirmação do README foi conferida contra o código: as rotas existem nos
controllers, as propriedades existem no `application.properties`, as classes
citadas existem nos pacotes indicados, os nomes de teste existem nas classes de
teste, e o alvo `test` existe no `Dockerfile`.

---

## Fase 6 — Revisão pré-commit

### Prompt

> Antes do primeiro commit, liste exatamente o que entraria no repositório,
> separando o que é código do que não é.

### O que a revisão encontrou

Esta fase produziu o achado mais relevante de toda a construção.

**Um bug que teria quebrado o repositório.** O inventário revelou uma divergência:
38 arquivos `.java` no disco, mas apenas 28 seriam versionados. A causa era a
regra `out/` do `.gitignore`, herdada do template do IntelliJ, onde ela serve para
ignorar o diretório de build. Sem barra inicial, o Git aplica a regra a **qualquer**
pasta chamada `out` em qualquer nível — e a Clean Architecture usa exatamente esse
nome:

```
application/port/out/     MovieRepositoryPort, MovieParserPort, InvalidMovieDataException
adapter/out/persistence/  MoviePersistenceAdapter, entidades JPA, repositórios
adapter/out/csv/          CsvMovieParser
```

Dez arquivos — toda a camada de adaptadores de saída e todas as portas de saída —
estavam sendo excluídos silenciosamente. O primeiro commit teria subido um projeto
que **não compila após o clone**. Corrigido para `/out/`, ancorando a regra na
raiz do repositório.

**Licença sem titularidade.** O repositório continha uma GPL-3.0 de 35 KB com os
marcadores `<year>` e `<name of author>` nunca preenchidos — maior que todo o
código Java somado. Como a especificação não pede licenciamento e um repositório
sem licença é simplesmente "todos os direitos reservados", o arquivo foi removido.

**Autoria indefinida.** O bloco `<developers>` do `pom.xml` estava vazio, junto de
`<scm>`, `<url>` e `<licenses>` — todos placeholders do Spring Initializr. O bloco
de desenvolvedores foi preenchido e os demais, removidos.

### Verificação

Após a correção do `.gitignore`, a contagem passou a bater: 38 arquivos `.java` no
disco, 38 versionáveis. Uma varredura confirmou que nenhum arquivo do projeto
permanecia ignorado. O commit final levou 51 arquivos e 2.729 linhas.

---

## Fase 7 — Verificação funcional

### Prompt

> Suba a aplicação e rode uma bateria de testes contra os endpoints reais, para
> confirmar que o comportamento em execução corresponde ao que os testes
> automatizados afirmam.

### Por que esta fase existe

Testes automatizados verdes provam que o código faz o que os testes esperam. Não
provam que a aplicação sobe, que a configuração externa resolve, nem que os
endpoints respondem como documentado. Esta fase fecha essa lacuna.

### Subida

```
No JWT_SECRET configured - signing with a key generated for this run.
Loaded 206 movies (42 winners, 359 distinct producers) from classpath:Movielist.csv
Started GoldenRaspberryApiApplication in 6.978 seconds
```

Sem nenhuma configuração prévia: chave gerada, CSV carregado, aplicação no ar em
sete segundos.

### Cenários exercitados

| # | Cenário | Esperado | Resultado |
|---|---|---|---|
| 1 | `GET` rota pública | `200` + payload | Joel Silver (1) / Matthew Vaughn (13) |
| 2 | `GET` rota protegida sem token | `401` | `401` |
| 3 | `GET` rota protegida com token forjado | `401` | `401` |
| 4 | `POST /auth/token` com senha errada | `401` | `401` |
| 5 | Emissão de token para `user` e `admin` | tokens válidos | emitidos |
| 6 | `GET` rota protegida com token | `200` | `200` |
| 7 | Payload da rota aberta vs. protegida | idênticos | `diff` limpo |
| 8 | `POST` import sem token | `401` | `401` |
| 9 | `POST` import como `user` | `403` | `403` |
| 10 | Import com cabeçalho inválido | `400` | aponta o cabeçalho correto |
| 11 | Import com ano `20XX` | `400` | `"Line 3: year \"20XX\" is not a number"` |
| 12 | Import de arquivo `.txt` | `400` | `"Only .csv files are accepted"` |
| 13 | Import de arquivo vazio | `400` | `"Uploaded file is empty"` |
| 14 | Dataset após as quatro rejeições | intacto | dados originais preservados |
| 15 | Import válido como `admin` | `200` + resumo | `{"movies":9,"winners":8,"producers":5}` |
| 16 | Conjunto com empates em `min` e `max` | todos retornados | 4 produtores, ordenados por `previousWin` |
| 17 | Conjunto sem ninguém repetindo | listas vazias | `{"min":[],"max":[]}` |

### Resultado

Todos os cenários corresponderam ao esperado. Os dois últimos são os que a caixa
de *Atenção* da especificação torna obrigatórios: empates múltiplos retornados por
completo, e ausência de vencedores repetidos respondida com listas vazias em vez
de erro.

---

## Suíte automatizada

19 testes de integração, todos passando:

| Classe | Testes | Cobre |
|---|---:|---|
| `SecurityIntegrationTest` | 8 | Token, credencial errada, `401`, `403`, paridade entre rotas |
| `CsvUploadIntegrationTest` | 7 | Upload válido, empates, ninguém repetindo, 4 rejeições |
| `AwardIntervalsIntegrationTest` | 3 | Contrato JSON, valores esperados, recálculo independente |
| `GoldenRaspberryApiApplicationTests` | 1 | Contexto sobe |

O teste mais relevante é `agreesWithAnIndependentRecalculationOfTheSourceFile`:
ele recalcula a resposta a partir do CSV por um caminho deliberadamente
diferente — ordenando as vitórias por `(produtor, ano)` e percorrendo a lista uma
vez — e compara com o que a API devolveu. Se o cálculo de produção derivar, o
teste acusa mesmo que os valores esperados tenham sido atualizados junto.
