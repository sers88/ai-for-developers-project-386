# syntax=docker/dockerfile:1

# ---- Build backend (Spring Boot / Kotlin) ----
FROM gradle:8.13-jdk21-alpine AS backend-build
WORKDIR /app
COPY backend/settings.gradle.kts backend/build.gradle.kts ./
RUN gradle --no-daemon dependencies
COPY backend/src src
# bootJar produces a single runnable fat jar (skips the plain jar task)
RUN gradle --no-daemon bootJar -x test

# ---- Build frontend (Nuxt SPA) ----
FROM node:22-alpine AS frontend-build
WORKDIR /app
COPY frontend/package.json frontend/package-lock.json* ./
RUN npm install
COPY frontend/ .
RUN npm run build

# ---- Runtime: full-stack single image (backend :8080 + frontend :3000) ----
FROM node:22-alpine AS runtime
COPY --from=eclipse-temurin:21-jre-alpine /opt/java/openjdk /opt/java/openjdk
ENV JAVA_HOME=/opt/java/openjdk
ENV PATH="/opt/java/openjdk/bin:${PATH}"
ENV HOST=0.0.0.0
ENV PORT=3000
ENV NUXT_PUBLIC_API_BASE=http://localhost:8080
RUN apk add --no-cache wget bash
WORKDIR /app
COPY --from=backend-build /app/build/libs/*.jar /app/app.jar
COPY --from=frontend-build /app/.output /app/.output
COPY docker-entrypoint.sh /usr/local/bin/docker-entrypoint.sh
RUN chmod +x /usr/local/bin/docker-entrypoint.sh
EXPOSE 3000 8080
HEALTHCHECK --interval=15s --timeout=5s --start-period=40s --retries=5 \
  CMD wget --spider -q http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["/usr/local/bin/docker-entrypoint.sh"]
