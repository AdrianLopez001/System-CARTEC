# Resumo da sessão — Classificação de contatos + Disparo automático WhatsApp

Data: 04/08/2026. Documento de referência do que foi decidido, construído e ainda está em aberto nesta sessão.

## 1. Ponto de partida

Pedido original: limpar e classificar os contatos do CRM (planilha `BASE COMPLETA DE CONTATOS.xlsx`, 5.401 linhas), separando PJ de PF, dando nota de atividade 0–10 e definindo temperatura (quente/morno/frio) para priorizar contato e conseguir agendamentos na Oficina Cartec.

## 2. Classificação de contatos (entregue)

Arquivo gerado: `BASE DE CONTATOS - CLASSIFICADA.xlsx` (na pasta `PROJETO CRM E ATENDIMENTO`), com abas `Resumo`, `PJ/PF - Quentes/Mornos/Frios` e `Base Completa`.

**Critérios usados** (referência: 04/08/2026):
- **Tipo**: direto do campo "Física/Jurídica" do CRM.
- **Temperatura**: Quente ≤ 7 meses sem comprar · Morno 7–13 meses · Frio > 13 meses · Frio (nunca comprou) = cadastrado sem nenhuma venda.
- **Score de Atividade 0–10**: Recência (50%) + Frequência/Qtd. de OS (30%) + Valor gasto (20%), por ranking percentual entre quem já comprou. Sem histórico = score 0.

**Resultado**:

| Segmento | PF | PJ | Total | Com telefone |
|---|---|---|---|---|
| Quente | 481 | 70 | 551 | — |
| Morno | 221 | 22 | 243 | — |
| Frio (com histórico) | 1.747 | 183 | 1.930 | PF: 1.711 |
| Frio (nunca comprou) | 2.525 | 152 | 2.677 | PF: 1.430 |

~1.182 contatos (22%) não têm telefone cadastrado — inalcançáveis por WhatsApp/ligação.

## 3. Conflito identificado com a estratégia já fechada

O projeto Java (`cartec-sistema`) já tinha decisões travadas em `CLAUDE.md` / `docs/estrategia-priorizada.docx`:
- Plataforma de atendimento decidida = **BotConversa**, não bot próprio.
- Ordem de execução: upsell no balcão (dia 1) → B2B (semana 1) → limpeza de base (2 semanas, paralelo) → **só na semana 3** configurar plataforma/disparo → funil testado com base interna nas semanas 5–6 → Ads/indicação só semana 7–8 **se** o funil converteu.
- Texto literal do documento: *"Não abrir BotConversa nem configurar Ads ainda — isso entra depois que upsell e B2B estiverem rodando de verdade."*

Isso foi sinalizado antes de qualquer implementação. **Decisão do usuário**: seguir em frente mesmo assim, com uma estratégia própria e mais específica (abaixo), usando o bot Baileys que já existia no projeto em vez do BotConversa.

## 4. Estratégia de disparo validada (decisão do usuário)

- **Automático**: só PF que **nunca comprou** (sem histórico/gancho) — mensagem genérica de primeiro contato faz sentido aqui.
- **Manual**: PF **frio com histórico** (tem gancho — veículo/serviço anterior) — consultor liga/manda mensagem citando o histórico. Usa a tela `/reengajamento` já existente no sistema (worklist priorizada por valor gasto).
- **Manual sempre**: **todo PJ** (quente, morno ou frio) — tratado como B2B, contato direto, nunca disparo em massa. Usa `/campanhas` (já existente, gera link wa.me manual).

Riscos levantados e ainda não resolvidos pelo usuário: número de WhatsApp dedicado separado do atendimento real (recomendado, não implementado), LGPD/opt-out (implementado no código, ver abaixo), risco de banimento por volume (mitigado com throttling, ver abaixo).

## 5. Descoberta: sistema já existente (`cartec-sistema`)

Antes de construir, foi mapeado o que já existia (para não duplicar):
- **Cliente** (`model/Cliente.java`): entidade única PF/PJ com `tipoPessoa`, `ultimaVendaData`, `totalGastoHistorico`, `qtdOsHistorico`.
- **SegmentoCliente** (`model/SegmentoCliente.java`): ATIVO (≤120 dias) / EM_RISCO (120–270 dias) / INATIVO (>270 dias) / SEM_HISTORICO — **atenção**: cortes diferentes dos 7/13 meses usados na planilha Excel; não foram unificados nesta sessão.
- **ClienteSegmentacaoService**: calcula o segmento acima, com fallback pro histórico importado quando não há OS vinculada no sistema.
- **`/reengajamento`** (`ReengajamentoController`): worklist manual de EM_RISCO/INATIVO, priorizada por valor gasto — é onde entra o fluxo manual de PF frio com gancho.
- **`/campanhas`** (`CampanhaController` + `CampanhaService`): gera xlsx com links `wa.me` prontos por tag ou segmento — envio 100% manual, um link por vez. É onde entra o fluxo manual de PJ.
- **`/whatsapp`** (`WhatsappController` + `WhatsappPageController` + `WhatsappSseService`): chat em tempo real já funcionando — conexão por QR Code (Baileys), status polling, envio/recebimento, classificação manual por IA, tudo via Server-Sent Events (SSE), sem WebSocket.
- **`whatsapp-bot/`** (Node.js + Baileys): bot que conecta ao WhatsApp via QR Code, expõe `/status`, `/send`, `/disconnect` na porta 3001, e ecoa toda mensagem (enviada ou recebida) pro sistema Java via `POST /api/whatsapp/mensagens/chat`.

## 6. O que foi construído nesta sessão (novo)

Funcionalidade: **Disparo Automático** — só para PF sem histórico, com throttling e opt-out.

**Backend (Java)**:
- `model/StatusDisparo.java`, `model/StatusItemDisparo.java` — enums de ciclo de vida.
- `model/Disparo.java` — cabeçalho da campanha (nome, mensagem-template, status, limite diário, contadores de enviado/erro/opt-out).
- `model/DisparoItem.java` — um contato dentro da campanha (telefone, nome, status, data de envio, erro).
- `repository/DisparoRepository.java`, `repository/DisparoItemRepository.java`.
- `service/DisparoAutomaticoService.java` — núcleo da automação:
  - `listarAlvoAtual()`: filtra `Cliente` com `tipoPessoa` começando em "F" (Física) + segmento `SEM_HISTORICO` + telefone válido (`PhoneUtils.isValido`).
  - `criar()` / `iniciar()` / `pausar()`: ciclo de vida da campanha; `iniciar()` recusa se o bot não estiver `CONECTADO`.
  - `processarTick()` (`@Scheduled(fixedDelay = 45_000)`): a cada ~45s, só dentro da janela comercial (8h–19h, seg–sáb) e com 60% de chance (jitter, evita padrão robótico), envia **um** item pendente por campanha ativa, respeitando o limite diário.
  - `processarPossivelOptOut()`: se a mensagem recebida for "SAIR"/"PARAR"/"CANCELAR"/"DESCADASTRAR", cancela qualquer envio pendente pra aquele telefone, em qualquer campanha.
  - Envio de fato: `POST {whatsapp.bot.url}/send` (reaproveita o bot já existente, mesma chamada que `WhatsappController.enviarMensagem` já fazia manualmente).
- `controller/DisparoController.java` — API REST (`/api/disparos`, `/alvo-atual`, `/{id}/iniciar`, `/{id}/pausar`, `/{id}/itens`).
- `controller/DisparoPageController.java` — serve a tela em `/disparo-automatico`.
- `service/WhatsappSseService.java` — **modificado**: método genérico `emitir(String evento, Map payload)` adicionado, reaproveitado pelo disparo pra empurrar progresso (`disparo-progresso`) na mesma stream SSE do chat.
- `service/MensagemChatService.java` — **modificado**: injeta `DisparoAutomaticoService` e chama `processarPossivelOptOut()` toda vez que chega uma mensagem `ENTRADA`.
- `CartecSistemaApplication.java` — **modificado**: adicionado `@EnableScheduling` (necessário pro `@Scheduled` funcionar).

**Frontend**:
- `templates/disparo.html` — tela nova com:
  - Trilha de **5 sinais** (semáforo) que acendem em sequência: Conectar WhatsApp → Base selecionada → Mensagem validada → Envio em andamento → Concluído.
  - Painel de conexão com QR Code (mesma lógica de polling de `/whatsapp`).
  - Formulário de mensagem (suporta `{nome}`) e limite diário.
  - Contadores + barra de progresso (enviados/erros/opt-out/total).
  - **Log ao vivo** dos envios, atualizado via SSE (`/api/whatsapp/stream`, evento `disparo-progresso`).
- `templates/fragments/nav.html` — **modificado**: link "Disparo Automático" adicionado no menu.

**Verificação feita**: `mvnw.cmd compile` (sucesso) e `mvnw.cmd test` — os 7 testes existentes (`FunilAtendimentoFlowTest`, `DumpPdfTextTest`, `OrcamentoParserManualTest`) continuam passando, sem regressão.

## 7. Como rodar para testar

```bash
# Terminal 1 — ERP
mvnw.cmd spring-boot:run

# Terminal 2 — bot WhatsApp
cd whatsapp-bot
node index.js
```

Acessar `http://localhost:8080/disparo-automatico`, escanear o QR Code, escrever a mensagem, criar o disparo e iniciar — recomendado testar primeiro com um limite diário bem baixo (ex: 5) antes de qualquer volume maior.

## 8. Pendências / próximos passos em aberto

- **Número de WhatsApp dedicado** para o disparo automático (separado do número de atendimento real) — recomendado, não configurado.
- **Unificar os cortes de temperatura**: a planilha usa 7/13 meses, o sistema Java usa 120/270 dias (~4/9 meses) — ainda são critérios diferentes rodando em paralelo.
- **Decisão formal sobre o desvio da estratégia fechada** (BotConversa → bot próprio; ordem de execução) — o usuário optou por seguir, mas isso não foi refletido de volta no `CLAUDE.md`/`estrategia-priorizada.docx` ainda.
- Mensagem de disparo automático ainda não foi escrita/validada pelo usuário (só o placeholder de exemplo na tela).
