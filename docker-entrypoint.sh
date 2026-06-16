#!/usr/bin/env bash
set -e

# Start the Spring Boot backend
java -jar /app/app.jar &
BACKEND_PID=$!

# Start the Nuxt (Nitro) frontend server
node /app/.output/server/index.mjs &
FRONTEND_PID=$!

terminate() {
  kill "$BACKEND_PID" "$FRONTEND_PID" 2>/dev/null || true
  exit 0
}
trap terminate TERM INT

# Exit as soon as either process stops
wait -n "$BACKEND_PID" "$FRONTEND_PID"
EXIT_CODE=$?
terminate
exit "$EXIT_CODE"
