const {
  default: makeWASocket,
  useMultiFileAuthState,
  DisconnectReason,
  fetchLatestBaileysVersion,
} = require("@whiskeysockets/baileys");

const qrcode = require("qrcode-terminal");
const axios = require("axios");

// ─────────────────────────────────────────
// CONFIGURAÇÃO
// ─────────────────────────────────────────
const ANTHROPIC_API_KEY = process.env.ANTHROPIC_API_KEY || "";
const CARTEC_SISTEMA_URL = process.env.CARTEC_SISTEMA_URL || "http://localhost:8080";
const CARTEC_SISTEMA_TOKEN = process.env.CARTEC_SISTEMA_TOKEN || "";

// Histórico de conversas em memória { telefone: [mensagens] }
const historicoConversas = {};

// ─────────────────────────────────────────
// CLASSIFICADOR IA (Claude API)
// ─────────────────────────────────────────
async function classificarConversa(telefone, historico) {
  const textoConversa = historico
    .map((m) => `[${m.hora}] ${m.de}: ${m.texto}`)
    .join("\n");

  const prompt = `Você é um classificador de conversas de uma oficina automotiva chamada Cartec (Bosch Car Service), em Natal/RN.

Analise a conversa abaixo e responda APENAS com um JSON válido, sem nenhum texto adicional, seguindo exatamente esta estrutura:

{
  "intencao": "<uma das opções: agendamento | orcamento | status_os | reclamacao | pecas | elogio | informacao | outro>",
  "urgencia": "<uma das opções: urgente | normal | baixa>",
  "consultor": "<uma das opções: Rosemberg | Luiz | Adrian | qualquer>",
  "resumo": "<resumo da conversa em 1 linha, máximo 100 caracteres>",
  "cliente_nome": "<nome do cliente se mencionado, ou 'desconhecido'>",
  "veiculo": "<veículo mencionado se houver, ou 'não mencionado'>"
}

Regras para consultor:
- Rosemberg: agendamentos, orçamentos, dúvidas gerais
- Luiz: status de peças, compras, fornecedores
- Adrian: suporte técnico, reclamações

Conversa (telefone: ${telefone}):
${textoConversa}`;

  try {
    const response = await axios.post(
      "https://api.anthropic.com/v1/messages",
      {
        model: "claude-sonnet-4-6",
        max_tokens: 500,
        messages: [{ role: "user", content: prompt }],
      },
      {
        headers: {
          "x-api-key": ANTHROPIC_API_KEY,
          "anthropic-version": "2023-06-01",
          "Content-Type": "application/json",
        },
      }
    );

    const texto = response.data.content[0].text.trim();
    return JSON.parse(texto);
  } catch (err) {
    return {
      intencao: "erro",
      urgencia: "normal",
      consultor: "qualquer",
      resumo: `Erro ao classificar: ${err.message}`,
      cliente_nome: "desconhecido",
      veiculo: "não mencionado",
    };
  }
}

// ─────────────────────────────────────────
// ENVIAR CLASSIFICACAO PRO SISTEMA CARTEC
// ─────────────────────────────────────────
async function enviarParaSistema(telefone, classificacao, totalMensagens) {
  try {
    const headers = { "Content-Type": "application/json" };
    if (CARTEC_SISTEMA_TOKEN) {
      headers["X-Whatsapp-Bot-Token"] = CARTEC_SISTEMA_TOKEN;
    }
    await axios.post(
      `${CARTEC_SISTEMA_URL}/api/whatsapp/mensagens`,
      {
        telefone: formatarTelefone(telefone),
        clienteNome: classificacao.cliente_nome,
        veiculo: classificacao.veiculo,
        intencao: classificacao.intencao,
        urgencia: classificacao.urgencia,
        consultor: classificacao.consultor,
        resumo: classificacao.resumo,
        totalMensagens: totalMensagens,
      },
      { headers, timeout: 5000 }
    );
  } catch (err) {
    console.log(
      `\n⚠️  Não consegui enviar a classificação pro sistema Cartec (${CARTEC_SISTEMA_URL}): ${err.message}`
    );
    console.log(`   O sistema precisa estar rodando (mvnw spring-boot:run) pra receber.\n`);
  }
}

// ─────────────────────────────────────────
// FORMATAÇÃO DO LOG NO TERMINAL
// ─────────────────────────────────────────
function formatarTelefone(jid) {
  return jid.replace("@s.whatsapp.net", "").replace("@g.us", " [GRUPO]");
}

function corUrgencia(urgencia) {
  const cores = { urgente: "\x1b[31m", normal: "\x1b[33m", baixa: "\x1b[32m" };
  return cores[urgencia] || "\x1b[37m";
}

function corIntencao(intencao) {
  const cores = {
    agendamento: "\x1b[36m",
    orcamento: "\x1b[35m",
    reclamacao: "\x1b[31m",
    elogio: "\x1b[32m",
    status_os: "\x1b[34m",
    pecas: "\x1b[33m",
  };
  return cores[intencao] || "\x1b[37m";
}

const RESET = "\x1b[0m";
const BOLD = "\x1b[1m";

function imprimirClassificacao(telefone, classificacao, totalMensagens) {
  const linha = "═".repeat(60);
  const cor = corUrgencia(classificacao.urgencia);
  const corInt = corIntencao(classificacao.intencao);

  console.log(`\n${BOLD}${linha}${RESET}`);
  console.log(`${BOLD}📱 NOVA MENSAGEM CLASSIFICADA${RESET}`);
  console.log(`${linha}`);
  console.log(`📞 Telefone  : ${BOLD}${formatarTelefone(telefone)}${RESET}`);
  console.log(`👤 Cliente   : ${BOLD}${classificacao.cliente_nome}${RESET}`);
  console.log(`🚗 Veículo   : ${classificacao.veiculo}`);
  console.log(
    `🎯 Intenção  : ${corInt}${BOLD}${classificacao.intencao.toUpperCase()}${RESET}`
  );
  console.log(
    `⚡ Urgência  : ${cor}${BOLD}${classificacao.urgencia.toUpperCase()}${RESET}`
  );
  console.log(`👨‍💼 Consultor : ${BOLD}${classificacao.consultor}${RESET}`);
  console.log(`📝 Resumo    : ${classificacao.resumo}`);
  console.log(`💬 Mensagens : ${totalMensagens} na conversa`);
  console.log(`${linha}\n`);
}

// ─────────────────────────────────────────
// INICIAR WHATSAPP
// ─────────────────────────────────────────
async function iniciarWhatsApp() {
  console.clear();
  console.log(`\n${"═".repeat(60)}`);
  console.log(`${BOLD}  🚗 CARTEC - CLASSIFICADOR DE MENSAGENS WHATSAPP${RESET}`);
  console.log(`${"═".repeat(60)}`);
  console.log(`  Bosch Car Service | Natal/RN`);
  console.log(`  Aguardando conexão...\n`);

  if (!ANTHROPIC_API_KEY) {
    console.error(
      `\n❌ ERRO: Variável ANTHROPIC_API_KEY não definida!\n` +
        `   Execute: export ANTHROPIC_API_KEY="sua_chave_aqui"\n`
    );
    process.exit(1);
  }

  const { state, saveCreds } = await useMultiFileAuthState("./sessao");
  const { version } = await fetchLatestBaileysVersion();

  const sock = makeWASocket({
    version,
    auth: state,
    printQRInTerminal: false,
    logger: require("pino")({ level: "silent" }),
  });

  // QR Code
  sock.ev.on("connection.update", ({ connection, lastDisconnect, qr }) => {
    if (qr) {
      console.log(
        `\n📷 Escaneie o QR Code abaixo com o WhatsApp da Cartec:\n`
      );
      qrcode.generate(qr, { small: true });
      console.log(`\n⏳ Aguardando leitura...\n`);
    }

    if (connection === "close") {
      const codigo = lastDisconnect?.error?.output?.statusCode;
      const reconectar = codigo !== DisconnectReason.loggedOut;

      if (reconectar) {
        console.log(`\n🔄 Reconectando...`);
        iniciarWhatsApp();
      } else {
        console.log(
          `\n🚪 Desconectado. Delete a pasta 'sessao' e reinicie para conectar novamente.`
        );
      }
    }

    if (connection === "open") {
      console.log(`\n✅ ${BOLD}WhatsApp conectado com sucesso!${RESET}`);
      console.log(
        `   Aguardando mensagens para classificar...\n`
      );
    }
  });

  sock.ev.on("creds.update", saveCreds);

  // Receber mensagens
  sock.ev.on("messages.upsert", async ({ messages, type }) => {
    if (type !== "notify") return;

    for (const msg of messages) {
      if (!msg.message) continue;
      if (msg.key.fromMe) continue; // Ignora mensagens enviadas por você

      const telefone = msg.key.remoteJid;
      if (!telefone) continue;

      // Extrair texto da mensagem
      const texto =
        msg.message?.conversation ||
        msg.message?.extendedTextMessage?.text ||
        msg.message?.imageMessage?.caption ||
        msg.message?.videoMessage?.caption ||
        "[mídia sem texto]";

      const agora = new Date().toLocaleTimeString("pt-BR");

      // Adicionar ao histórico
      if (!historicoConversas[telefone]) {
        historicoConversas[telefone] = [];
      }

      historicoConversas[telefone].push({
        hora: agora,
        de: "cliente",
        texto: texto,
      });

      console.log(
        `\n💬 [${agora}] ${formatarTelefone(telefone)}: "${texto}"`
      );
      console.log(`   🤔 Classificando com IA...`);

      // Classificar conversa completa
      const classificacao = await classificarConversa(
        telefone,
        historicoConversas[telefone]
      );

      imprimirClassificacao(
        telefone,
        classificacao,
        historicoConversas[telefone].length
      );

      await enviarParaSistema(
        telefone,
        classificacao,
        historicoConversas[telefone].length
      );
    }
  });
}

// Iniciar
iniciarWhatsApp().catch(console.error);
