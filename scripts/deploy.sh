#!/usr/bin/env bash
# Despliegue completo del Banco XYZ (EFT) con Docker Compose.
set -euo pipefail

cd "$(dirname "$0")/.."

echo "==> Construyendo y levantando todos los servicios..."
docker compose up -d --build

echo ""
echo "==> Estado del stack:"
docker compose ps

echo ""
echo "Servicios y puertos:"
echo "  Config Server      http://localhost:8888"
echo "  Eureka             http://localhost:8761"
echo "  API Gateway        http://localhost:8087"
echo "  Auth Server        http://localhost:9000"
echo "  ms-clientes        http://localhost:8091"
echo "  ms-cuentas         http://localhost:8092"
echo "  ms-pagos           http://localhost:8093"
echo "  banco-batch        http://localhost:8080"
echo "  bff-web (HTTPS)    https://localhost:8082"
echo "  bff-mobile (HTTPS) https://localhost:8083"
echo "  bff-atm (HTTPS)    https://localhost:8084"
echo "  notificaciones     http://localhost:8085"
echo "  Kafka UI           http://localhost:8090"
