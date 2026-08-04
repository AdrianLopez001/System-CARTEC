package com.cartec.sistema.model;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * As 8 iniciativas do Plano de Atividade (Plano_Atividade_Cartec_Aprovacao.docx,
 * secoes 2 e 3) - usada para marcar qual iniciativa gerou uma OrdemServico.
 * Bonificacao por item fica a criterio do dono (nao modelada aqui - texto
 * "a definir" ate aprovacao formal).
 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum CategoriaPlano {
    PF1_PREMIUM(
            "PF-1 Premium",
            "Foco digital/redes sociais em clientes com serviço rápido e alto valor agregado (~R$3.000, 2x o TKM)",
            "Origem = rede social; valor da OS ≥ ~R$3.000 e serviço ≤ 3h",
            GrupoQuota.PF1),
    PF2_CRM(
            "PF-2 CRM",
            "Disparo de lembrete de revisão preventiva sem deixar passar prazo (1 mês antes do vencimento)",
            "% de lembretes enviados no prazo + conversão em agendamento",
            GrupoQuota.PF2),
    PF3_CAMPANHAS(
            "PF-3 Campanhas",
            "Disparos pontuais vinculados a estoque parado (ex: baterias Hilux)",
            "Tag por campanha + conversão vinculada",
            GrupoQuota.PF3),
    PF4_CONTEUDO(
            "PF-4 Conteúdo",
            "1 post/semana (multi-mídia) com foco em autoridade e vínculo com o cliente",
            "Piso mínimo cumprido + agendamento com origem \"conteúdo\"",
            GrupoQuota.PF4),
    PJ1_DIGITAL(
            "PJ-1 Digital",
            "Manter presença institucional mostrando capacidade e vínculos já existentes",
            "Registro de atividade (sem meta de conversão)",
            GrupoQuota.PJ),
    PJ2_CAPTACAO(
            "PJ-2 Frotas — captação",
            "Captar novas frotas para fidelização, por porte",
            "Nº de frotas novas/mês",
            GrupoQuota.PJ),
    PJ2_RECORRENCIA(
            "PJ-2 Frotas — recorrência",
            "Manter atividade mensal da frota",
            "Nº de atendimentos/mês por frota",
            GrupoQuota.PJ),
    PJ3_REATIVACAO(
            "PJ-3 Reativação",
            "Retomar frotas da lista de clientes perdidos",
            "Mínimo de 10 atendimentos por frota reativada",
            GrupoQuota.PJ),
    PJ4_FIDELIZACAO(
            "PJ-4 Fidelização",
            "Brindes/diferenciais em vez de desconto — foco em volume de veículos",
            "Nº de veículos únicos atendidos/mês por frota",
            GrupoQuota.PJ);

    private final String rotulo;
    private final String comoAgir;
    private final String comoEhMedido;
    private final GrupoQuota grupoQuota;

    CategoriaPlano(String rotulo, String comoAgir, String comoEhMedido, GrupoQuota grupoQuota) {
        this.rotulo = rotulo;
        this.comoAgir = comoAgir;
        this.comoEhMedido = comoEhMedido;
        this.grupoQuota = grupoQuota;
    }

    /** Nome da constante (ex: "PF1_PREMIUM") - usado pelo front pra enviar de volta nas requisições. */
    public String getChave() {
        return name();
    }

    public String getRotulo() {
        return rotulo;
    }

    public String getComoAgir() {
        return comoAgir;
    }

    public String getComoEhMedido() {
        return comoEhMedido;
    }

    public GrupoQuota getGrupoQuota() {
        return grupoQuota;
    }
}
