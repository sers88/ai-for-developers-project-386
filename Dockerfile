# syntax=docker/dockerfile:1

# ---- Build frontend (Nuxt SPA) ----
FROM node:22-alpine AS frontend-build
WORKDIR /app
COPY frontend/package.json frontend/package-lock.json* ./
RUN npm install
COPY frontend/ .
RUN npm run build

# ---- Runtime: single HTTP server on $PORT (hexlet runs -e PORT=8080) ----
FROM node:22-alpine AS runtime
RUN apk add --no-cache wget
WORKDIR /app
ENV HOST=0.0.0.0
ENV PORT=8080
ENV NUXT_PUBLIC_API_BASE=http://localhost:8080
COPY --from=frontend-build /app/.output /app/.output
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=5s --start-period=15s --retries=5 \
  CMD wget --spider -q "http://localhost:${PORT}/" || exit 1
CMD ["node", ".output/server/index.mjs"]
