package com.cartec.sistema.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Cliente da base de CRM: usado para segmentar campanhas de disparo por tag
 * (ex: B2B, Frotas, Varejo, Inativo) e gerar os links wa.me com a mensagem
 * definida na campanha.
 */
@Entity
@Table(name = "cliente")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", nullable = false)
    private String nome;

    @Column(name = "telefone", nullable = false, unique = true)
    private String telefone;

    @Column(name = "email")
    private String email;

    @Column(name = "tag")
    private String tag;

    @Column(name = "observacoes")
    private String observacoes;

    @Column(name = "data_cadastro")
    private LocalDate dataCadastro;

    @ManyToOne
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;

    @Column(name = "codigo_externo", unique = true)
    private String codigoExterno;

    @Column(name = "sexo")
    private String sexo;

    @Column(name = "data_nascimento")
    private LocalDate dataNascimento;

    @Column(name = "tipo_pessoa")
    private String tipoPessoa;

    @Column(name = "cpf")
    private String cpf;

    @Column(name = "rg")
    private String rg;

    @Column(name = "cnpj")
    private String cnpj;

    @Column(name = "razao_social")
    private String razaoSocial;

    @Column(name = "nome_fantasia")
    private String nomeFantasia;

    @Column(name = "inscricao_estadual")
    private String inscricaoEstadual;

    @Column(name = "endereco_logradouro")
    private String enderecoLogradouro;

    @Column(name = "endereco_complemento")
    private String enderecoComplemento;

    @Column(name = "bairro")
    private String bairro;

    @Column(name = "cidade")
    private String cidade;

    @Column(name = "uf")
    private String uf;

    @Column(name = "cep")
    private String cep;

    @Column(name = "ultima_venda_data")
    private LocalDate ultimaVendaData;

    @Column(name = "ultima_venda_veiculo")
    private String ultimaVendaVeiculo;

    @Column(name = "ultima_venda_placa")
    private String ultimaVendaPlaca;

    @Column(name = "total_gasto_historico", precision = 14, scale = 2)
    private BigDecimal totalGastoHistorico;

    @Column(name = "qtd_os_historico")
    private Integer qtdOsHistorico;

    @Column(name = "media_gasto_historico", precision = 12, scale = 2)
    private BigDecimal mediaGastoHistorico;

    @Column(name = "demo", nullable = false, columnDefinition = "boolean default false")
    private boolean demo = false;

    @PrePersist
    public void aoCriar() {
        if (dataCadastro == null) {
            dataCadastro = LocalDate.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    public LocalDate getDataCadastro() {
        return dataCadastro;
    }

    public void setDataCadastro(LocalDate dataCadastro) {
        this.dataCadastro = dataCadastro;
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }

    public boolean isDemo() {
        return demo;
    }

    public void setDemo(boolean demo) {
        this.demo = demo;
    }

    public String getCodigoExterno() {
        return codigoExterno;
    }

    public void setCodigoExterno(String codigoExterno) {
        this.codigoExterno = codigoExterno;
    }

    public String getSexo() {
        return sexo;
    }

    public void setSexo(String sexo) {
        this.sexo = sexo;
    }

    public LocalDate getDataNascimento() {
        return dataNascimento;
    }

    public void setDataNascimento(LocalDate dataNascimento) {
        this.dataNascimento = dataNascimento;
    }

    public String getTipoPessoa() {
        return tipoPessoa;
    }

    public void setTipoPessoa(String tipoPessoa) {
        this.tipoPessoa = tipoPessoa;
    }

    public String getEnderecoLogradouro() {
        return enderecoLogradouro;
    }

    public void setEnderecoLogradouro(String enderecoLogradouro) {
        this.enderecoLogradouro = enderecoLogradouro;
    }

    public String getEnderecoComplemento() {
        return enderecoComplemento;
    }

    public void setEnderecoComplemento(String enderecoComplemento) {
        this.enderecoComplemento = enderecoComplemento;
    }

    public String getBairro() {
        return bairro;
    }

    public void setBairro(String bairro) {
        this.bairro = bairro;
    }

    public String getCidade() {
        return cidade;
    }

    public void setCidade(String cidade) {
        this.cidade = cidade;
    }

    public String getUf() {
        return uf;
    }

    public void setUf(String uf) {
        this.uf = uf;
    }

    public String getCep() {
        return cep;
    }

    public void setCep(String cep) {
        this.cep = cep;
    }

    public LocalDate getUltimaVendaData() {
        return ultimaVendaData;
    }

    public void setUltimaVendaData(LocalDate ultimaVendaData) {
        this.ultimaVendaData = ultimaVendaData;
    }

    public String getUltimaVendaVeiculo() {
        return ultimaVendaVeiculo;
    }

    public void setUltimaVendaVeiculo(String ultimaVendaVeiculo) {
        this.ultimaVendaVeiculo = ultimaVendaVeiculo;
    }

    public String getUltimaVendaPlaca() {
        return ultimaVendaPlaca;
    }

    public void setUltimaVendaPlaca(String ultimaVendaPlaca) {
        this.ultimaVendaPlaca = ultimaVendaPlaca;
    }

    public BigDecimal getTotalGastoHistorico() {
        return totalGastoHistorico;
    }

    public void setTotalGastoHistorico(BigDecimal totalGastoHistorico) {
        this.totalGastoHistorico = totalGastoHistorico;
    }

    public Integer getQtdOsHistorico() {
        return qtdOsHistorico;
    }

    public void setQtdOsHistorico(Integer qtdOsHistorico) {
        this.qtdOsHistorico = qtdOsHistorico;
    }

    public BigDecimal getMediaGastoHistorico() {
        return mediaGastoHistorico;
    }

    public void setMediaGastoHistorico(BigDecimal mediaGastoHistorico) {
        this.mediaGastoHistorico = mediaGastoHistorico;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public String getRg() {
        return rg;
    }

    public void setRg(String rg) {
        this.rg = rg;
    }

    public String getCnpj() {
        return cnpj;
    }

    public void setCnpj(String cnpj) {
        this.cnpj = cnpj;
    }

    public String getRazaoSocial() {
        return razaoSocial;
    }

    public void setRazaoSocial(String razaoSocial) {
        this.razaoSocial = razaoSocial;
    }

    public String getNomeFantasia() {
        return nomeFantasia;
    }

    public void setNomeFantasia(String nomeFantasia) {
        this.nomeFantasia = nomeFantasia;
    }

    public String getInscricaoEstadual() {
        return inscricaoEstadual;
    }

    public void setInscricaoEstadual(String inscricaoEstadual) {
        this.inscricaoEstadual = inscricaoEstadual;
    }
}
