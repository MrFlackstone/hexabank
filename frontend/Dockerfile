# --- Etapa de build: compila el SPA con Vite ---
FROM node:24.22.1-alpine AS build
WORKDIR /app
COPY package.json package-lock.json* ./
RUN npm ci
COPY . .
RUN npm run build

# --- Etapa de runtime: Nginx sirve el estatico y hace de reverse-proxy /api ---
FROM nginx:1.30.6-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
