# 🚗 Cartec - Classificador de Mensagens WhatsApp

Sistema que conecta ao WhatsApp via Baileys e classifica automaticamente
as mensagens recebidas usando IA (Claude da Anthropic).

---

## 📋 Pré-requisitos

- Node.js 18 ou superior
- Chave de API da Anthropic (Claude)
- Número de WhatsApp separado da Cartec

---

## ⚙️ Instalação

```bash
# 1. Entrar na pasta do projeto
cd cartec-whatsapp

# 2. Instalar dependências
npm install
```

---

## 🚀 Como usar

### Passo 1 — Definir a chave da API

**Windows (PowerShell):**
```powershell
$env:ANTHROPIC_API_KEY="sk-ant-xxxxxxxxxxxxxxxx"
```

**Windows (CMD):**
```cmd
set ANTHROPIC_API_KEY=sk-ant-xxxxxxxxxxxxxxxx
```

**Linux/Mac:**
```bash
export ANTHROPIC_API_KEY="sk-ant-xxxxxxxxxxxxxxxx"
```

### Passo 2 — Iniciar o sistema

```bash
npm start
```

### Passo 3 — Escanear o QR Code

- Abra o WhatsApp no celular da Cartec
- Vá em: **Configurações → Aparelhos conectados → Conectar aparelho**
- Escaneie o QR Code que aparece no terminal

### Passo 4 — Pronto!

Cada mensagem recebida será automaticamente classificada, exibida no terminal e enviada
pro sistema Cartec principal (aparece na tela **WhatsApp** do sistema, em `/whatsapp`) —
o sistema precisa estar rodando (`mvnw spring-boot:run`) em `http://localhost:8080` pra
receber. Se o sistema estiver em outro endereço, defina `CARTEC_SISTEMA_URL` antes de
iniciar. Nada é enviado automaticamente pro cliente — a IA só classifica, o disparo/resposta
continua manual (botão "Abrir no WhatsApp" na tela do sistema).

```
════════════════════════════════════════════════════════════
📱 NOVA MENSAGEM CLASSIFICADA
════════════════════════════════════════════════════════════
📞 Telefone  : 5584999999999
👤 Cliente   : João Silva
🚗 Veículo   : HB20 2022
🎯 Intenção  : AGENDAMENTO
⚡ Urgência  : NORMAL
👨‍💼 Consultor : Rosemberg
📝 Resumo    : Cliente quer agendar revisão do HB20
💬 Mensagens : 3 na conversa
════════════════════════════════════════════════════════════
```

---

## 🎯 Classificações possíveis

| Campo | Opções |
|-------|--------|
| **Intenção** | agendamento, orcamento, status_os, reclamacao, pecas, elogio, informacao, outro |
| **Urgência** | urgente, normal, baixa |
| **Consultor** | Rosemberg, Luiz, Adrian, qualquer |

---

## 🔄 Reconexão

Se desconectar, o sistema tenta reconectar automaticamente.

Para conectar um número diferente:
```bash
# Deletar a sessão salva e reiniciar
rm -rf sessao/
npm start
```

---

## ⚠️ Avisos importantes

- O Baileys usa o WhatsApp Web — **não use o mesmo número no celular e no sistema ao mesmo tempo** de forma intensiva
- Este é um projeto de **testes** — para produção, use a API oficial da Meta
- A pasta `sessao/` contém dados de autenticação — **não compartilhe**

---

## 📁 Estrutura do projeto

```
cartec-whatsapp/
├── index.js          ← Sistema principal
├── package.json      ← Dependências
├── README.md         ← Este arquivo
└── sessao/           ← Criada automaticamente (dados de sessão)
```
