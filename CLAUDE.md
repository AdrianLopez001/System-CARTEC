# Projeto Cartec — Service e Peças

Contexto para o Claude Code. Este arquivo é lido automaticamente ao abrir uma sessão nesta pasta.

## Quem sou eu / papel do projeto

Adrian, consultor de atendimento e relacionamento com cliente na Cartec Bosch Service (Natal/RN).
Este projeto tem dois objetivos que se conectam:
1. Executar a estratégia de CRM/atendimento (documento de execução, ver `docs/estrategia-priorizada.docx`).
2. Construir um sistema em Java (Spring Boot) para automatizar dashboard, base histórica e ferramentas do dia a dia — incluindo padronização de número de telefone para disparos de CRM/promocionais.

## Estado atual — decisões já fechadas (não reabrir sem motivo forte)

- **Plataforma de atendimento:** BotConversa (Opção A) — decisão fechada, custo x tempo técnico não compensa alternativa.
- **Stack do sistema:** Java 21 + Spring Boot 3, banco relacional (H2 em arquivo, inclusive em produção — ver decisão de hospedagem abaixo), Apache PDFBox + Apache POI para ingestão de PDF/XLS, Chart.js no front, Spring Security (login único) a partir de 03/08/2026.
- **Hospedagem:** decisão fechada em 03/08/2026 — **Hostinger VPS** (Ubuntu 22.04, Docker Compose), não Railway/Render/Fly.io como estava previsto antes. Motivo: o VPS tem disco persistente (diferente de PaaS efêmero), então H2 em arquivo dentro de um volume Docker é suficiente — sem precisar de PostgreSQL gerenciado por enquanto. Runbook completo de deploy em `DEPLOY.md`. Migrar pra PostgreSQL fica em aberto pra quando/se o uso justificar.

## Ordem de execução (resumo da estratégia priorizada)

A lógica é inverter a ordem "tradicional" (fundação primeiro) porque quem executa é uma pessoa só:
1. Ticket médio via upsell no balcão — dia 1, custo zero.
2. B2B/frotas — semana 1, contato direto.
3. Limpeza de base e fluxo — paralelo, prazo travado em 2 semanas.
4. Configuração de plataforma (BotConversa) — só semana 3, depois que upsell e B2B já estiverem rodando de verdade.
5. Funil, Ads, indicação — só a partir da semana 7-8, e só se o funil converteu de forma consistente nas semanas 5-6.

Indicadores acompanhados desde a semana 1 (à mão, antes do sistema existir): ticket médio semanal, empresas B2B contatadas/propostas fechadas, margem por tipo de serviço (mensal), taxa de conversão do funil (a partir da semana 5).

## Arquitetura do sistema (resumo)

- **Camadas:** Ingestão (PDFBox/POI) → Persistência (JPA + H2/Postgres) → Regras/Cálculo (serviços Java) → API (Spring Boot REST) → Dashboard (Thymeleaf ou React + Chart.js).
- **Entidades principais:** OrdemServico, ItemServico, VendaDiaria, MetricaSemanal, Meta, Projecao.
- **Motor de projeção:** só variação percentual (sem modelo estatístico complexo) — variação (%) entre períodos, projeção do próximo período por média das últimas N semanas, % da meta atingida, alerta automático de estagnação (ticket médio parado por 3-4 semanas).
- **Fases de desenvolvimento:** 1) Base + carga histórico → 2) Entrada manual → 3) Motor de métricas/projeção → 4) Dashboard → 5) Alertas. MVP rápido possível em 2 semanas (fase 1 + parte da 2 e 3).

## Documentos de referência completos

- `docs/estrategia-priorizada.docx` — plano de execução solo, semana a semana, com metas de faturamento e ticket médio por mês.
- `docs/plano-sistema-java.docx` — arquitetura completa, modelo de dados, todos os módulos e fases.

Quando eu (Adrian) pedir para "puxar informações do documento", primeiro tente ler os arquivos em `docs/`. Se precisar de mais detalhe do que está aqui, pergunte antes de assumir.

## Estado do sistema em Java — o que já foi construído

Base da Fase 1 já criada e compilando/rodando (validado em 31/07/2026): Maven Wrapper,
entidades JPA, repositories, `MetricaService` (motor de variação %/projeção/% da meta),
`IngestaoService` (extração de PDF/XLS funcionando, mapeamento de campos ainda TODO),
dashboard Thymeleaf com Chart.js e CRUD REST básico. Detalhes de como rodar e o que
falta (parser de ingestão, telas adicionais, alertas no dashboard) estão em `README.md`
na raiz do projeto — ler esse arquivo antes de continuar o desenvolvimento.

Decisão tomada durante a construção: sem Lombok (o JDK 26 desta máquina não é
compatível com a versão trazida pelo Spring Boot 3.3.5 — falha silenciosa, sem gerar
getters/setters). Entidades usam getters/setters escritos à mão.

## Convenções de código

- Pacotes Java: `com.cartec.sistema` (`model`, `repository`, `service`, `controller`, `dto`)
- Padrão de commits: a definir
- Testes: `src/test/java` — `FunilAtendimentoFlowTest` (MockMvc, fluxo completo do funil de atendimento) é o principal; roda com `@WithMockUser` já que o login está ativo
