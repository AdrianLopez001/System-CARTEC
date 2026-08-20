[![CI](https://github.com/AdrianLopez001/System-CARTEC/actions/workflows/ci.yml/badge.svg)](https://github.com/AdrianLopez001/System-CARTEC/actions)
[![Java 21](https://img.shields.io/badge/Java_21-ED8B00?style=flat&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot 3](https://img.shields.io/badge/Spring_Boot_3-6DB33F?style=flat&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Docker](https://img.shields.io/badge/Docker-2496ED?style=flat&logo=docker&logoColor=white)](https://www.docker.com/)
# Cartec ERP â€” Service e PeÃ§as

Sistema integrado de gestÃ£o, CRM, calendÃ¡rio, faturamento e atendimento WhatsApp em tempo real para a **Cartec Bosch Service (Natal/RN)**.

Desenvolvido em **Java 21 + Spring Boot 3 + Thymeleaf + H2 Database** e bot de mensageria em **Node.js (Baileys)**.

---

## ðŸš€ Como Iniciar o Sistema

### 1. Iniciar o ERP (Java Spring Boot)
No terminal da raiz do projeto, execute:

```bash
# No macOS / Linux:
./mvnw spring-boot:run

# No Windows:
mvnw.cmd spring-boot:run
```

O sistema estarÃ¡ acessÃ­vel em: **[http://localhost:8080](http://localhost:8080)**

---

### 2. Iniciar a ConexÃ£o do WhatsApp (Tempo Real & IA)
Em outro terminal, acesse a pasta `whatsapp-bot` e inicie o robÃ´:

```bash
cd whatsapp-bot
node index.js
```

---

## ðŸ“± Como Conectar e Atender pelo WhatsApp

1. Com o ERP e o bot rodando, acesse **[http://localhost:8080/whatsapp](http://localhost:8080/whatsapp)**.
2. O sistema exibirÃ¡ o **badge de status** e o **QR Code** diretamente na tela.
3. No celular da oficina:
   - Abra o WhatsApp > **Aparelhos Conectados** > **Conectar um Aparelho**.
   - Escaneie o QR Code exibido no navegador.
4. O status mudarÃ¡ automaticamente para **`CONECTADO`**.
5. As mensagens recebidas serÃ£o classificadas por IA (intenÃ§Ã£o, urgÃªncia, consultor) e vocÃª poderÃ¡ **responder diretamente pela interface do ERP**.

---

## ðŸ“¤ Como Fazer Carga de Dados Reais (ImportaÃ§Ã£o)

Para cadastrar seus clientes e histÃ³rico de faturamento sem digitar nada manualmente:

1. Acesse **[http://localhost:8080/importacao](http://localhost:8080/importacao)** (ou o menu *Importar Dados*).
2. VocÃª poderÃ¡ fazer o upload de:
   - **Planilha de Clientes (`ListaContatos.xlsx`)**: Importa nome, CPF, CNPJ, telefone, e-mail e tags.
   - **ConferÃªncia de OS (`ConferenciaOS.pdf` ou `.xlsx`)**: Importa ordens de serviÃ§o e histÃ³rico de faturamento.
   - **Vendas Por MÃªs (`VendasPorMes.pdf`)**: Atualiza faturamento mensal, ticket mÃ©dio e grÃ¡ficos.
   - **Agenda de ServiÃ§os (`Agenda.pdf`)**: Aloca automaticamente os agendamentos no calendÃ¡rio.

---

## âš™ï¸ Alternando do Modo DemonstraÃ§Ã£o para Dados Reais

Quando o sistema Ã© iniciado pela primeira vez, ele vem com o **Modo DemonstraÃ§Ã£o** opcional ativado com dados de exemplo.

Para iniciar a operaÃ§Ã£o real da oficina:
1. Acesse **[http://localhost:8080/configuracoes](http://localhost:8080/configuracoes)**.
2. Clique no botÃ£o **Desativar modo de demonstraÃ§Ã£o**.
3. O sistema limparÃ¡ com seguranÃ§a todos os dados de teste e manterÃ¡ a base limpa para a entrada dos seus dados reais via tela de ImportaÃ§Ã£o.

---

## ðŸ› ï¸ Estrutura do Projeto e MÃ³dulos principais

- `/` â€” **Dashboard**: Ticket mÃ©dio, variaÃ§Ã£o semanal, projeÃ§Ã£o mensal e grÃ¡ficos.
- `/calendario` â€” **CalendÃ¡rio**: VisÃ£o de agendamentos com modal interativo para visualizar, editar e excluir eventos.
- `/clientes` â€” **Clientes & Empresas**: Abas separadas para Pessoa FÃ­sica (PF), Pessoa JurÃ­dica (PJ) e Empresas/Frotas.
- `/ordens-servico` â€” **Faturamento**: Listagem e conferÃªncia de Ordens de ServiÃ§o.
- `/whatsapp` â€” **Central WhatsApp**: QR Code em tempo real, status da conexÃ£o, caixa de entrada IA e envio de mensagens pelo ERP.
- `/importacao` â€” **Carga de Dados**: Upload de planilhas de contatos e relatÃ³rios em PDF/XLS.
- `/configuracoes` â€” **ConfiguraÃ§Ãµes**: Chaveamento do modo demonstraÃ§Ã£o e reset de dados.

---

## ðŸ’» Console do Banco de Dados Local (H2)
O console (`/h2-console`) fica **desligado por padrÃ£o** (ficaria exposto sem senha). Para
usar durante desenvolvimento, rode com o profile local:

```bash
# Windows:
set SPRING_PROFILES_ACTIVE=local && mvnw.cmd spring-boot:run
# macOS/Linux:
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run
```

- **URL**: `http://localhost:8080/h2-console`
- **JDBC URL**: `jdbc:h2:file:./data/cartec`
- **UsuÃ¡rio**: `sa`
- **Senha**: *(em branco)*

## ðŸ” Login

O sistema pede login (usuÃ¡rio/senha em `application.properties`, padrÃ£o `admin`/`admin`
sÃ³ para teste local â€” trocar antes de qualquer deploy real).

## ðŸš€ Deploy em produÃ§Ã£o

Runbook completo (Docker Compose, Nginx, HTTPS) em [`DEPLOY.md`](DEPLOY.md).

