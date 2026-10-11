# Banco XYZ — Desarrollo Backend Avanzado: Spring Cloud y Batch (EFT)

> **Nombre:** Nicolás Cavieres
> **Carrera:** Analista Programador
> **Fecha:** 09/10/2026
> **Profesor:** Alonso Castillo
> **Curso:** Backend III (PBY2203)

Evaluación Final Transversal — Semana 9.

---

## Tabla de contenidos

> **Nota de alcance:** el despliegue real en la nube (criterio 6 de la pauta) quedó **fuera de la evaluación** por decisión del docente (AWS Academy). El proyecto se consolidó y validó en **local con Docker Compose**; los pasos de despliegue en AWS se entregan documentados en [`despliegue.md`](despliegue.md).

1. [Resumen ejecutivo](#1-resumen-ejecutivo)
2. [Objetivo](#2-objetivo)
3. [Alcance](#3-alcance)
4. [Arquitectura](#4-arquitectura)
5. [Componentes y responsabilidades](#5-componentes-y-responsabilidades)
6. [Tecnologías utilizadas](#6-tecnologías-utilizadas)
7. [Estructura del proyecto](#7-estructura-del-proyecto)
8. [Requisitos previos](#8-requisitos-previos)
9. [Puesta en marcha](#9-puesta-en-marcha)
10. [Carga de datos (batch)](#10-carga-de-datos-batch)
11. [Seguridad: autenticación y autorización](#11-seguridad-autenticación-y-autorización)
12. [Pruebas de cada componente](#12-pruebas-de-cada-componente)
13. [Comparación con el sistema legacy](#13-comparación-con-el-sistema-legacy)
14. [Desafíos y soluciones](#14-desafíos-y-soluciones)

---

## 1. Resumen ejecutivo

El **Banco XYZ** moderniza su sistema legacy (COBOL + scripts Shell sobre mainframe) migrándolo a una **arquitectura de microservicios en la nube**, segura, resiliente y escalable. Este proyecto implementa de extremo a extremo:

- La **migración de los 3 procesos batch** legacy (Reporte de Transacciones Diarias, Cálculo de Intereses y Estados de Cuenta Anuales) a **Spring Batch**, con manejo avanzado de errores (retry/skip), detección de anomalías y paralelismo.
- El patrón **Backend for Frontend (BFF)** para 3 canales (Web, Móvil y Cajeros Automáticos), cada uno con su propio backend optimizado y seguro.
- **Microservicios resilientes con Spring Cloud**: Config Server, Eureka (service discovery), API Gateway, **OAuth 2.0**, **Resilience4j** y mensajería asíncrona con **Apache Kafka**.
- Despliegue con **Docker** y `docker-compose`, preparado para la nube (AWS) con **escalabilidad horizontal**.

## 2. Objetivo

Migrar el sistema legacy del Banco XYZ a una arquitectura moderna de microservicios alojada en la nube, cumpliendo con los requerimientos de **escalabilidad, seguridad y eficiencia operativa**, y manteniendo la **integridad y consistencia** de los datos durante la migración.

## 3. Alcance

| Área | Implementación |
|---|---|
| Migración batch | 3 jobs Spring Batch sobre [`fin_legacy_data`](https://github.com/KariVillagran/fin_legacy_data) |
| Canales | 3 BFFs independientes: Web, Móvil, Cajeros |
| Microservicios | Gestión de Clientes, Gestión de Cuentas, Procesamiento de Pagos |
| Spring Cloud | Config Server, Eureka, API Gateway |
| Seguridad | OAuth 2.0 (Spring Authorization Server) + HTTPS + autorización por canal |
| Resiliencia | Resilience4j (Circuit Breaker, Retry, Fallback) |
| Mensajería | Apache Kafka (productores y consumidores) |
| Despliegue | Docker + docker-compose + AWS (ECS/Fargate, RDS, ALB) |

## 4. Arquitectura

```
                        clientes
        ┌──────────────────┼────────────────────┐
        ▼                  ▼                    ▼
   BFF Web (HTTPS)    BFF Móvil (HTTPS)    BFF Cajero (HTTPS)
   :8082 (ROLE_WEB)   :8083 (ROLE_MOBILE)  :8084 (ROLE_ATM)
        │                  │                    │
        └──────────────┬───┴────────────────────┘
                       ▼  (token JWT relay)
                 API Gateway (:8087) ── enruta por Eureka ──┐
                       │                                     │
   ┌───────────────────┼───────────────────┐                 │
   ▼                   ▼                   ▼                 ▼
ms-clientes         ms-cuentas          ms-pagos        Eureka Server
  :8091               :8092               :8093            :8761
   │                   │                   │
   │            ┌──────┴─────┐            │
   │            ▼            ▼            │
   │       ms-cuentas   Kafka (productor/consumidor) ◄─────── notificaciones
   │       (consume      ▲          ▲                       :8085
   │        transacciones)  │          │
   └──────────┬─────────────┴──────────┘
              │  Kafka: clientes / retiros / transacciones / autenticaciones
              │
        Config Server (:8888)        Auth Server (:9000, OAuth 2.0)
        MySQL 8.4 (:3307)            ← banco-batch (:8080) carga fin_legacy_data
```

**Flujo de datos:**

1. `banco-batch` lee los CSV de `fin_legacy_data`, los limpia (retry/skip) y persiste en MySQL.
2. Los microservicios (`ms-clientes`, `ms-cuentas`, `ms-pagos`) exponen la lógica de negocio sobre MySQL, protegidos con OAuth 2.0.
3. Los BFFs adaptan las respuestas por canal y se comunican con los microservicios a través del **API Gateway** (enrutamiento + balanceo de carga por Eureka).
4. Los eventos de negocio (clientes, retiros, transacciones completadas, autenticaciones) se publican en **Kafka** y son consumidos de forma asíncrona (notificaciones, actualización de saldo).

## 5. Componentes y responsabilidades

| Componente | Puerto | Protocolo | Responsabilidad |
|---|---|---|---|
| `config-server` | 8888 | HTTP | Centraliza la configuración de los microservicios |
| `eureka-server` | 8761 | HTTP | Service Discovery (registro y descubrimiento dinámico) |
| `api-gateway` | 8087 | HTTP | Enrutamiento y balanceo de carga (`lb://`) |
| `auth-server` | 9000 | HTTP | Authorization Server OAuth 2.0 (emite JWT) |
| `ms-clientes` | 8091 | HTTP | Gestión de clientes (perfiles e información personal) |
| `ms-cuentas` | 8092 | HTTP | Gestión de cuentas (apertura, cierre, saldo, retiros) |
| `ms-pagos` | 8093 | HTTP | Pagos, transferencias y depósitos |
| `banco-batch` | 8080 | HTTP | 3 jobs Spring Batch (carga y limpieza de `fin_legacy_data`) |
| `bff-web` | 8082 | HTTPS | Canal Web (datos completos + resumen) |
| `bff-mobile` | 8083 | HTTPS | Canal Móvil (respuestas ligeras) |
| `bff-atm` | 8084 | HTTPS | Canal Cajeros (saldo y retiro) |
| `notificaciones-service` | 8085 | HTTP | Consumidor Kafka (notificaciones de retiro) |
| MySQL 8.4 | 3307 | — | Persistencia (BD `banco_xyz`) |
| Kafka + Zookeeper | 29092 | — | Mensajería asíncrona |
| Kafka UI | 8090 | HTTP | Visualización de tópicos |

## 6. Tecnologías utilizadas

- **Java 21**, **Spring Boot 3.3.5**, **Maven** (multi-módulo).
- **Spring Batch**: `ItemReader`/`ItemProcessor`/`ItemWriter`, chunking, `RetryPolicy`/`SkipPolicy`, listeners.
- **Spring Cloud 2023.0.4**: Config Server, Netflix Eureka, API Gateway, LoadBalancer.
- **Spring Security / Spring Authorization Server** (OAuth 2.0, JWT RS256).
- **Resilience4j 2.2.0**: Circuit Breaker, Retry, Fallback.
- **Apache Kafka** (Confluent 7.4.4) + Zookeeper + Kafka UI.
- **MySQL 8.4**, **Spring Data JPA**, **Docker / Docker Compose**.
- **Lombok**, **Nimbus JOSE+JWT**, **HTTPS** (certificado autofirmado PKCS12).

## 7. Estructura del proyecto

```
Banco_XYZ_EFT/
├── pom.xml                    # Parent agregador (Java 21, Boot 3.3.5, Spring Cloud)
├── docker-compose.yml         # Orquestación completa (13 servicios + infra)
├── keystore.p12               # Certificado autofirmado (HTTPS, desarrollo)
├── scripts/deploy.sh          # Despliegue completo con Docker Compose
├── common/                    # Librería compartida (JWT, seguridad, DTOs, errores)
├── config-server/             # Spring Cloud Config Server
├── eureka-server/             # Service Discovery
├── api-gateway/               # Spring Cloud Gateway
├── auth-server/               # Authorization Server OAuth 2.0
├── ms-clientes/               # Microservicio de Clientes
├── ms-cuentas/                # Microservicio de Cuentas
├── ms-pagos/                  # Microservicio de Pagos
├── banco-batch/               # 3 procesos batch (fin_legacy_data)
├── bff-web/                   # BFF canal Web
├── bff-mobile/                # BFF canal Móvil
├── bff-atm/                   # BFF canal Cajeros
└── notificaciones-service/    # Consumidor Kafka
```

## 8. Requisitos previos

- **Docker 29+** y **Docker Compose v2+**.
- Opcional: Maven 3.9 + JDK 21 (para ejecutar servicios sin Docker).

## 9. Puesta en marcha

```bash
# Despliegue completo (construye y levanta todo)
./scripts/deploy.sh
# o
docker compose up -d --build
```

El stack levanta MySQL, Kafka, Config Server, Eureka, Gateway, Auth Server, los 3 microservicios, el batch, los 3 BFF y notificaciones.

- **Eureka:** http://localhost:8761 (panel de servicios registrados)
- **Kafka UI:** http://localhost:8090 (tópicos y mensajes)

> El batch se ejecuta automáticamente al arrancar (`BATCH_RUN_ON_STARTUP=true`).

## 10. Carga de datos (batch)

El `banco-batch` procesa `fin_legacy_data` con **3 jobs**:

| Job | Endpoint | CSV de entrada | Salida |
|---|---|---|---|
| Reporte de Transacciones Diarias | `GET /api/batch/reporte-transacciones` | `movimientos_financieros_diarios.csv` | `movimiento` + `anomalia` + `resumen_transacciones` |
| Cálculo de Intereses | `GET /api/batch/calculo-intereses` | `intereses_trimestrales.csv` | `cuenta` |
| Estados de Cuenta Anuales | `GET /api/batch/estados-anuales` | `estados_financieros_anuales.csv` | `transaccion` |
| Todos | `GET /api/batch/procesar` | — | los 3 anteriores |

```bash
curl http://localhost:8080/api/batch/procesar
```

Cada job aplica **retry** (fallas transitorias de BD), **skip** (registros inválidos) y es **idempotente** (limpia sus tablas antes de ejecutar).

## 11. Seguridad: autenticación y autorización

Autenticación por **OAuth 2.0** (client credentials). Cada canal tiene su propio cliente y rol:

| Canal | BFF | Cliente OAuth | Rol |
|---|---|---|---|
| Web | https://localhost:8082 | `bff-web-client` / `web-secret` | `ROLE_WEB` |
| Móvil | https://localhost:8083 | `bff-mobile-client` / `mobile-secret` | `ROLE_MOBILE` |
| Cajero | https://localhost:8084 | `bff-atm-client` / `atm-secret` | `ROLE_ATM` |

Autorización por recurso (ejemplos):

- `POST /api/cuentas/{id}/retiros` → solo `ROLE_ATM`.
- `POST /api/clientes` → solo `ROLE_WEB`.
- Lecturas → cualquier canal autenticado.

El retiro publica un evento `retiros` en Kafka que consume `notificaciones-service` (arquitectura orientada a eventos).

## 12. Pruebas de cada componente

Ver [`instrucciones.md`](instrucciones.md): instrucciones paso a paso (curl/Postman) para autenticar cada canal, consumir los microservicios, verificar `401/403`, probar Resilience4j (fallback) y observar los eventos en Kafka.

## 13. Comparación con el sistema legacy

| Aspecto | Legacy (COBOL + Shell) | Nuevo sistema (Spring Cloud) |
|---|---|---|
| Arquitectura | Monolítica (mainframe) | Microservicios desacoplados |
| Escalabilidad | Limitada | Horizontal (Docker, Eureka, LoadBalancer) |
| Tolerancia a fallos | Frágil (fallo total) | Resilience4j (Circuit Breaker/Retry/Fallback) |
| Procesos batch | Scripts manuales | Spring Batch (retry/skip, paralelismo, re-ejecución) |
| Frontends | Backend monolítico único | BFF por canal (Web/Móvil/ATM) |
| Seguridad | Centralizada y limitada | OAuth 2.0 distribuido + HTTPS |
| Integración | Acoplada | Eventos Kafka (desacoplamiento) |
| Mantenimiento | Alto costo | Modular y evolutivo |

## 14. Desafíos y soluciones

| Desafío | Solución |
|---|---|
| Datos legacy sucios (negativos, fechas, nulos, duplicados) | `ItemProcessor` con validación/normalización + políticas de **retry/skip** + deduplicación en BD (`INSERT IGNORE`). |
| Procesadores con estado (anti-patrón Spring Batch) | Procesadores **stateless**; la deduplicación se resuelve con clave única en BD. |
| Consistencia en entorno distribuido | Eventos Kafka: `ms-pagos` publica "transacciones completadas" y `ms-cuentas` actualiza el saldo de forma asíncrona. |
| Fallos en llamadas a servicios externos | **Resilience4j** (Circuit Breaker + Retry + Fallback) en el consumidor. |
| Seguridad distribuida | **OAuth 2.0** (Authorization Server + Resource Servers) y autorización por canal. |
| Despliegue reproducible | **Docker** + `docker-compose` (un solo comando), preparado para AWS. |

---

*Proyecto desarrollado para la Evaluación Final Transversal (EFT) de Backend III — Duoc UC.*
