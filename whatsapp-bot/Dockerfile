FROM node:20-alpine
WORKDIR /app

COPY package.json package-lock.json* ./
RUN npm install --omit=dev

COPY . .

# Sessao do WhatsApp (credenciais de login) fica em volume, ver docker-compose.yml
VOLUME ["/app/sessao"]

EXPOSE 3001
CMD ["node", "index.js"]
