# Deploy em produção — Hostinger VPS

Runbook para colocar o sistema no ar no VPS da Hostinger (Ubuntu 22.04, Docker). Todos os comandos abaixo rodam **via SSH no VPS**, não na sua máquina Windows.

Dados do VPS usado nesta configuração:
- IP: `2.24.87.18`
- SO: Ubuntu 22.04 LTS
- Acesso: `ssh root@2.24.87.18`

---

## 0. Ligar o VPS e conectar

No painel da Hostinger (hPanel → VPS → Visão geral), clique em **Iniciar VPS** (hoje aparece "Stopped"). Espere ficar "Running", depois conecte:

```bash
ssh root@2.24.87.18
```

## 1. Limpar uso anterior (esse VPS já foi usado antes)

```bash
docker ps -a
docker compose -h >/dev/null 2>&1 && echo "docker compose ok" || echo "precisa instalar"
```

Se aparecer algum container/projeto antigo rodando:

```bash
cd /caminho/do/projeto/antigo 2>/dev/null && docker compose down -v
cd ~
```

Se quiser começar 100% limpo (cuidado — isso apaga containers, imagens e volumes não usados):

```bash
docker system prune -a --volumes
```

## 2. Confirmar Docker instalado

O painel Hostinger já mostra "Gerenciador Docker", então o Docker provavelmente já vem instalado nessa imagem. Confirme:

```bash
docker --version
docker compose version
```

Se algum dos dois falhar, instale via script oficial:

```bash
curl -fsSL https://get.docker.com | sh
```

## 3. Clonar o repositório

**Antes disso**: os commits desta sessão (login, correções de segurança, Docker) precisam estar enviados pro GitHub (`git push`) a partir da sua máquina Windows — combine isso comigo antes de continuar aqui.

```bash
cd ~
git clone https://github.com/AdrianLopez001/System-CARTEC.git cartec-sistema
cd cartec-sistema
```

Se o repositório for privado, vai pedir usuário/token do GitHub (crie um Personal Access Token em github.com/settings/tokens, escopo `repo`).

## 4. Configurar segredos (`.env`)

```bash
cp .env.example .env
nano .env
```

Preencha com valores reais:
- `APP_SECURITY_ADMIN_PASSWORD` — senha forte, **não** deixe `admin`.
- `WHATSAPP_BOT_TOKEN` — gere um valor aleatório: `openssl rand -hex 24`
- `ANTHROPIC_API_KEY` — opcional, chave da API do Claude (console.anthropic.com) pra ativar a análise por IA do Agente Financeiro. Sem ela a tela mostra "IA não configurada" e o resto do sistema segue normal.

Salve (Ctrl+O, Enter, Ctrl+X no nano).

## 5. Subir os containers

```bash
docker compose up -d --build
```

A primeira build demora alguns minutos (baixa dependências Maven e npm). Acompanhe:

```bash
docker compose logs -f app
```

Espere aparecer `Started CartecSistemaApplication`. Ctrl+C sai do `logs -f` sem parar o container.

## 6. Abrir o firewall (só o necessário)

No painel Hostinger (Segurança → Regras de firewall) ou via `ufw` no próprio VPS, libere só:
- **22** (SSH — já deve estar liberado)
- **80** (HTTP, usado pelo Nginx no próximo passo)
- **443** (HTTPS, quando tiver domínio + certificado)

**Não** libere a 8080 nem a 3001 publicamente — o Nginx do passo 7 é quem fala com a 8080 internamente, e a 3001 (bot) já não é nem publicada pelo Docker.

## 7. Nginx como proxy reverso (porta 80 → app)

```bash
apt update && apt install -y nginx
```

Crie `/etc/nginx/sites-available/cartec`:

```nginx
server {
    listen 80;
    server_name 2.24.87.18;  # trocar pelo dominio quando tiver um

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

```bash
ln -s /etc/nginx/sites-available/cartec /etc/nginx/sites-enabled/
rm -f /etc/nginx/sites-enabled/default
nginx -t && systemctl restart nginx
```

Acesse **http://2.24.87.18/** — deve cair na tela de login.

## 8. HTTPS (só quando tiver domínio apontado pro IP)

Aponte o domínio (registro `A`) pra `2.24.87.18` no seu provedor de DNS, espere propagar (pode levar algumas horas), troque `server_name` no arquivo do Nginx pelo domínio, depois:

```bash
apt install -y certbot python3-certbot-nginx
certbot --nginx -d seu-dominio.com.br
```

O Certbot ajusta o Nginx automaticamente e renova o certificado sozinho.

Depois disso, adicione no `application.properties` do projeto (e faça novo `docker compose up -d --build`):

```properties
server.forward-headers-strategy=framework
```

Isso garante que o Spring Security gere links/redirects corretos sabendo que está atrás de um proxy HTTPS.

## 9. Conectar o WhatsApp

Acesse `/whatsapp` no sistema (logado), escaneie o QR Code uma vez com o celular da oficina. A sessão fica salva no volume `./whatsapp-bot/sessao` no VPS — sobrevive a `docker compose restart`/reboot, não precisa escanear de novo depois.

---

## Comandos do dia a dia

```bash
# Ver logs
docker compose logs -f app
docker compose logs -f whatsapp-bot

# Reiniciar depois de uma atualização de código
git pull
docker compose up -d --build

# Parar tudo
docker compose down

# Backup manual do banco (H2 é um arquivo só)
cp data/cartec.mv.db ~/backup-cartec-$(date +%Y%m%d).mv.db

# Agente Financeiro automático: solte o relatório exportado (PDF/XLS/XLSX)
# em data/entrada-financeiro/ (dentro da pasta do projeto no VPS) - o
# sistema detecta o tipo, importa e (com ANTHROPIC_API_KEY configurada)
# gera a análise por IA em até 1 minuto. Confira o resultado em
# data/entrada-financeiro/processados/ ou /erro/ (o .txt ao lado explica o motivo).
```

## Verificação pós-deploy

- [ ] `http://2.24.87.18/login` abre e o login com a senha configurada funciona
- [ ] `/agendar/{token}` (gerar um em `/funil`) abre **sem** estar logado
- [ ] `/whatsapp` conecta o QR Code e persiste após `docker compose restart`
- [ ] `curl http://2.24.87.18:3001` de outra máquina falha/recusa (bot não exposto)
- [ ] Upload de planilha em `/importacao` funciona normalmente

## Próximos passos (fora desta rodada)

- HTTPS com domínio (passo 8, assim que tiver um domínio)
- Backup automático agendado (`cron` + `cp`/`rsync` do `data/cartec.mv.db`)
- Migração pra PostgreSQL, se o uso crescer e justificar
- CI/CD (push no GitHub → deploy automático no VPS)
