# ---- Build: gera os estáticos do Angular ----
FROM node:24-alpine AS build
WORKDIR /build

# Dependências primeiro, em camada própria: só é refeita quando o lock muda
COPY package.json package-lock.json ./
RUN npm ci

# O cliente da API é versionado, então o build não depende do backend no ar
COPY . .
RUN npm run build

# ---- Runtime: só o nginx e os arquivos ----
# Imagem não-privilegiada: nginx roda sem root e escuta na 8080
FROM nginxinc/nginx-unprivileged:alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /build/dist/frontend/browser /usr/share/nginx/html

EXPOSE 8080
