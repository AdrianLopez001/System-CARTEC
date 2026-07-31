# Cartec Sistema

Base do sistema descrito em `docs/plano-sistema-java.docx`: dashboard, base histórica
e projeções por percentual para a Cartec Service e Peças. Java 21 + Spring Boot 3,
H2 embutido (fase local), pensado para crescer nas próximas fases sem trocar de stack.

## Como rodar

Não precisa ter Maven instalado — o projeto já vem com o Maven Wrapper.

```
mvnw.cmd spring-boot:run
```

Abre em `http://localhost:8080`. Os dados ficam em `./data/cartec.mv.db` (arquivo H2,
criado automaticamente na primeira execução — não é apagado ao reiniciar).

Console do banco (para inspecionar dados manualmente): `http://localhost:8080/h2-console`
— JDBC URL `jdbc:h2:file:./data/cartec`, usuário `sa`, senha em branco.

## O que já existe (Fase 1 de `docs/plano-sistema-java.docx`)

- **Modelo de dados**: `OrdemServico`, `ItemServico`, `VendaDiaria`, `MetricaSemanal`,
  `Meta`, `Projecao` — exatamente as entidades da seção 3 do plano.
- **Motor de métricas** (`MetricaService`): variação % entre períodos, projeção do
  próximo período, % da meta atingida e alerta de estagnação — as 4 fórmulas da seção 6.
- **Dashboard** (`/`): tela "Visão da semana" (ticket médio atual, variação vs. semana
  anterior, % da meta, gráfico de tendência).
- **API REST**: CRUD básico de ordens de serviço, vendas diárias, metas e métricas
  semanais (`/api/ordens-servico`, `/api/vendas-diarias`, `/api/metas`,
  `/api/metricas-semanais`), mais os cálculos do motor de métricas expostos em
  `/api/metricas/variacao`, `/api/metricas/projecao`, `/api/metricas/percentual-meta`.
- **Ingestão** (`/api/ingestao/*`): já lê o PDF/XLS enviado e conta linhas, mas o
  mapeamento campo-a-campo para as entidades ainda é TODO (ver `IngestaoService`) —
  falta confirmar o layout exato do export atual da Oficina Inteligente antes de escrever
  o parser (colunas do XLS, formato das tabelas do PDF).

## Próximos passos sugeridos (Fases 2-5 do plano)

1. Confirmar o layout de `ConferenciaOS.pdf` / `ConferenciaOSItem.xls` / `VendaPorDia.pdf`
   e completar o parser em `IngestaoService`.
2. Tela de conferência antes de gravar (total importado x esperado).
3. Formulário semanal no próprio dashboard (hoje só existe via API).
4. Telas adicionais: B2B/Frotas, Margem, Projeção, Histórico completo.
5. Alertas automáticos de estagnação no dashboard (o cálculo já existe em
   `MetricaService.verificarEstagnacao`, falta ligar na tela).

## Notas de stack

- Sem Lombok: o JDK usado nesta máquina (26) ainda não é compatível com a versão de
  Lombok trazida pelo Spring Boot 3.3.5, então as entidades têm getters/setters
  escritos à mão. Se um dia trocar para um JDK mais antigo (21 LTS) e quiser reduzir
  boilerplate, dá para reavaliar.
- `spring-boot-devtools` incluído para reiniciar automaticamente ao salvar código.
