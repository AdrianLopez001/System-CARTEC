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
const CARTEC_SISTEMA_URL = process.env.CARTEC_SISTEMA_URL || "http://localhost:8080";
const CARTEC_SISTEMA_TOKEN = process.env.CARTEC_SISTEMA_TOKEN || "";

// ─────────────────────────────────────────
// ENVIAR MENSAGEM PRO SISTEMA CARTEC (thread completa, sem classificacao
// automatica - o operador classifica manualmente na tela do ERP)
// ─────────────────────────────────────────
async function enviarMensagemParaSistema({ telefone, texto, direcao, idExternoWhatsapp, pushName }) {
  try {
    const headers = { "Content-Type": "application/json" };
    if (CARTEC_SISTEMA_TOKEN) {
      headers["X-Whatsapp-Bot-Token"] = CARTEC_SISTEMA_TOKEN;
    }
    await axios.post(
      `${CARTEC_SISTEMA_URL}/api/whatsapp/mensagens/chat`,
      {
        telefone: formatarTelefone(telefone),
        texto,
        direcao,
        idExternoWhatsapp,
        pushName,
      },
      { headers, timeout: 5000 }
    );
  } catch (err) {
    console.log(
      `\n⚠️  Não consegui enviar a mensagem pro sistema Cartec (${CARTEC_SISTEMA_URL}): ${err.message}`
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

const RESET = "\x1b[0m";
const BOLD = "\x1b[1m";

// ─────────────────────────────────────────
// INICIAR WHATSAPP
// ─────────────────────────────────────────
async function iniciarWhatsApp() {
  console.clear();
  console.log(`\n${"═".repeat(60)}`);
  console.log(`${BOLD}  🚗 CARTEC - CHAT WHATSAPP EM TEMPO REAL${RESET}`);
  console.log(`${"═".repeat(60)}`);
  console.log(`  Bosch Car Service | Natal/RN`);
  console.log(`  Aguardando conexão...\n`);

  const { state, saveCreds } = await useMultiFileAuthState("./sessao");
  const { version } = await fetchLatestBaileysVersion();

  const sock = makeWASocket({
    version,
    auth: state,
    printQRInTerminal: false,
    logger: require("pino")({ level: "silent" }),
  });

  // HTTP Management Server on Port 3001 for ERP integration
  const http = require("http");
  const BOT_PORT = process.env.BOT_PORT || 3001;

  let botEstado = "DESCONECTADO";
  let qrCodeAtual = null;
  let numeroConectado = null;

  const server = http.createServer(async (req, res) => {
    // CORS headers
    res.setHeader("Access-Control-Allow-Origin", "*");
    res.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
    res.setHeader("Access-Control-Allow-Headers", "Content-Type");

    if (req.method === "OPTIONS") {
      res.writeHead(204);
      res.end();
      return;
    }

    const url = req.url;

    if (req.method === "GET" && url === "/status") {
      res.writeHead(200, { "Content-Type": "application/json" });
      res.end(JSON.stringify({
        status: botEstado,
        qrRaw: qrCodeAtual,
        qrCodeUrl: qrCodeAtual ? `https://api.qrserver.com/v1/create-qr-code/?size=260x260&data=${encodeURIComponent(qrCodeAtual)}` : null,
        phone: numeroConectado
      }));
      return;
    }

    if (req.method === "POST" && url === "/send") {
      let body = "";
      req.on("data", chunk => { body += chunk.toString(); });
      req.on("end", async () => {
        try {
          const data = JSON.parse(body || "{}");
          if (!data.telefone || !data.texto) {
            res.writeHead(400, { "Content-Type": "application/json" });
            res.end(JSON.stringify({ erro: "Campos telefone e texto são obrigatórios." }));
            return;
          }
          let jid = data.telefone.replace(/\D/g, "");
          if (!jid.endsWith("@s.whatsapp.net")) {
            jid = jid + "@s.whatsapp.net";
          }
          const enviada = await sock.sendMessage(jid, { text: data.texto });
          res.writeHead(200, { "Content-Type": "application/json" });
          res.end(JSON.stringify({ ok: true, mensagem: "Mensagem enviada com sucesso!", id: enviada?.key?.id || null }));
        } catch (err) {
          res.writeHead(500, { "Content-Type": "application/json" });
          res.end(JSON.stringify({ ok: false, erro: err.message }));
        }
      });
      return;
    }

    if (req.method === "POST" && url === "/disconnect") {
      try {
        await sock.logout();
        botEstado = "DESCONECTADO";
        qrCodeAtual = null;
        numeroConectado = null;
        res.writeHead(200, { "Content-Type": "application/json" });
        res.end(JSON.stringify({ ok: true, mensagem: "Desconectado com sucesso." }));
      } catch (err) {
        res.writeHead(500, { "Content-Type": "application/json" });
        res.end(JSON.stringify({ ok: false, erro: err.message }));
      }
      return;
    }

    res.writeHead(404, { "Content-Type": "application/json" });
    res.end(JSON.stringify({ erro: "Rota não encontrada" }));
  });

  server.listen(BOT_PORT, () => {
    console.log(`\n🚀 API de integração do Bot WhatsApp rodando na porta ${BOT_PORT}`);
  });

  // Connection status event updates
  sock.ev.on("connection.update", ({ connection, lastDisconnect, qr }) => {
    if (qr) {
      botEstado = "AGUARDANDO_QR";
      qrCodeAtual = qr;
      console.log(`\n📷 Escaneie o QR Code no terminal ou no ERP:\n`);
      qrcode.generate(qr, { small: true });
      console.log(`\n⏳ Aguardando leitura...\n`);
    }

    if (connection === "close") {
      botEstado = "DESCONECTADO";
      qrCodeAtual = null;
      numeroConectado = null;
      const codigo = lastDisconnect?.error?.output?.statusCode;
      const reconectar = codigo !== DisconnectReason.loggedOut;

      if (reconectar) {
        console.log(`\n🔄 Reconectando...`);
        iniciarWhatsApp();
      } else {
        console.log(`\n🚪 Desconectado pelo usuário.`);
      }
    }

    if (connection === "open") {
      botEstado = "CONECTADO";
      qrCodeAtual = null;
      numeroConectado = sock.user ? sock.user.id.split(":")[0] : "WhatsApp";
      console.log(`\n✅ ${BOLD}WhatsApp conectado com sucesso!${RESET}`);
    }
  });

  sock.ev.on("creds.update", saveCreds);

  // Receber mensagens (entrada e saida - saida cobre mensagens mandadas
  // por outro dispositivo do operador direto no WhatsApp, fora do ERP)
  sock.ev.on("messages.upsert", async ({ messages, type }) => {
    if (type !== "notify") return;

    for (const msg of messages) {
      if (!msg.message) continue;

      let telefone = msg.key.remoteJid;
      if (!telefone) continue;
      if (telefone.endsWith("@g.us") || telefone.endsWith("@broadcast")) continue;

      // WhatsApp vem migrando alguns contatos pra "LID" (@lid), um id opaco
      // de privacidade que nao e o numero de telefone - precisa resolver pro
      // JID @s.whatsapp.net de verdade antes de mandar pro ERP, senao vira
      // um "telefone" com um numero gigante sem sentido (ex: LID de 15
      // digitos virando "telefone" invalido no cadastro do cliente).
      if (telefone.endsWith("@lid")) {
        let resolvido = msg.key.remoteJidAlt || null;
        if (!resolvido) {
          try {
            resolvido = await sock.signalRepository.lidMapping.getPNForLID(telefone);
          } catch (err) {
            resolvido = null;
          }
        }
        if (!resolvido) {
          console.log(`\n⚠️  Não consegui resolver o LID ${telefone} pro número de telefone real - mensagem ignorada.`);
          continue;
        }
        telefone = resolvido.includes("@") ? resolvido : `${resolvido}@s.whatsapp.net`;
      }

      const texto =
        msg.message?.conversation ||
        msg.message?.extendedTextMessage?.text ||
        msg.message?.imageMessage?.caption ||
        msg.message?.videoMessage?.caption ||
        "[mídia sem texto]";

      const direcao = msg.key.fromMe ? "SAIDA" : "ENTRADA";
      const agora = new Date().toLocaleTimeString("pt-BR");
      const seta = direcao === "ENTRADA" ? "💬" : "📤";

      console.log(`\n${seta} [${agora}] ${formatarTelefone(telefone)} (${direcao}): "${texto}"`);

      await enviarMensagemParaSistema({
        telefone,
        texto,
        direcao,
        idExternoWhatsapp: msg.key.id,
        pushName: direcao === "ENTRADA" ? msg.pushName : null,
      });
    }
  });
}

// Iniciar
iniciarWhatsApp().catch(console.error);
