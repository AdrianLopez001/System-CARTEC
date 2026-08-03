# Cartec ERP — Service e Peças

Sistema integrado de gestão, CRM, calendário, faturamento e atendimento WhatsApp em tempo real para a **Cartec Bosch Service (Natal/RN)**.

Desenvolvido em **Java 21 + Spring Boot 3 + Thymeleaf + H2 Database** e bot de mensageria em **Node.js (Baileys)**.

---

## 🚀 Como Iniciar o Sistema

### 1. Iniciar o ERP (Java Spring Boot)
No terminal da raiz do projeto, execute:

```bash
# No macOS / Linux:
./mvnw spring-boot:run

# No Windows:
mvnw.cmd spring-boot:run
```

O sistema estará acessível em: **[http://localhost:8080](http://localhost:8080)**

---

### 2. Iniciar a Conexão do WhatsApp (Tempo Real & IA)
Em outro terminal, acesse a pasta `whatsapp-bot` e inicie o robô:

```bash
cd whatsapp-bot
node index.js
```

---

## 📱 Como Conectar e Atender pelo WhatsApp

1. Com o ERP e o bot rodando, acesse **[http://localhost:8080/whatsapp](http://localhost:8080/whatsapp)**.
2. O sistema exibirá o **badge de status** e o **QR Code** diretamente na tela.
3. No celular da oficina:
   - Abra o WhatsApp > **Aparelhos Conectados** > **Conectar um Aparelho**.
   - Escaneie o QR Code exibido no navegador.
4. O status mudará automaticamente para **`CONECTADO`**.
5. As mensagens recebidas serão classificadas por IA (intenção, urgência, consultor) e você poderá **responder diretamente pela interface do ERP**.

---

## 📤 Como Fazer Carga de Dados Reais (Importação)

Para cadastrar seus clientes e histórico de faturamento sem digitar nada manualmente:

1. Acesse **[http://localhost:8080/importacao](http://localhost:8080/importacao)** (ou o menu *Importar Dados*).
2. Você poderá fazer o upload de:
   - **Planilha de Clientes (`ListaContatos.xlsx`)**: Importa nome, CPF, CNPJ, telefone, e-mail e tags.
   - **Conferência de OS (`ConferenciaOS.pdf` ou `.xlsx`)**: Importa ordens de serviço e histórico de faturamento.
   - **Vendas Por Mês (`VendasPorMes.pdf`)**: Atualiza faturamento mensal, ticket médio e gráficos.
   - **Agenda de Serviços (`Agenda.pdf`)**: Aloca automaticamente os agendamentos no calendário.

---

## ⚙️ Alternando do Modo Demonstração para Dados Reais

Quando o sistema é iniciado pela primeira vez, ele vem com o **Modo Demonstração** opcional ativado com dados de exemplo.

Para iniciar a operação real da oficina:
1. Acesse **[http://localhost:8080/configuracoes](http://localhost:8080/configuracoes)**.
2. Clique no botão **Desativar modo de demonstração**.
3. O sistema limpará com segurança todos os dados de teste e manterá a base limpa para a entrada dos seus dados reais via tela de Importação.

---

## 🛠️ Estrutura do Projeto e Módulos principais

- `/` — **Dashboard**: Ticket médio, variação semanal, projeção mensal e gráficos.
- `/calendario` — **Calendário**: Visão de agendamentos com modal interativo para visualizar, editar e excluir eventos.
- `/clientes` — **Clientes & Empresas**: Abas separadas para Pessoa Física (PF), Pessoa Jurídica (PJ) e Empresas/Frotas.
- `/ordens-servico` — **Faturamento**: Listagem e conferência de Ordens de Serviço.
- `/whatsapp` — **Central WhatsApp**: QR Code em tempo real, status da conexão, caixa de entrada IA e envio de mensagens pelo ERP.
- `/importacao` — **Carga de Dados**: Upload de planilhas de contatos e relatórios em PDF/XLS.
- `/configuracoes` — **Configurações**: Chaveamento do modo demonstração e reset de dados.

---

## 💻 Console do Banco de Dados Local (H2)
O console (`/h2-console`) fica **desligado por padrão** (ficaria exposto sem senha). Para
usar durante desenvolvimento, rode com o profile local:

```bash
# Windows:
set SPRING_PROFILES_ACTIVE=local && mvnw.cmd spring-boot:run
# macOS/Linux:
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run
```

- **URL**: `http://localhost:8080/h2-console`
- **JDBC URL**: `jdbc:h2:file:./data/cartec`
- **Usuário**: `sa`
- **Senha**: *(em branco)*

## 🔐 Login

O sistema pede login (usuário/senha em `application.properties`, padrão `admin`/`admin`
só para teste local — trocar antes de qualquer deploy real).

## 🚀 Deploy em produção

Runbook completo (Docker Compose, Nginx, HTTPS) em [`DEPLOY.md`](DEPLOY.md).
