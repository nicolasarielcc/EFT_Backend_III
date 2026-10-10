# Pasos para desplegar el sistema en la nube (AWS) — Banco XYZ (EFT)

Este documento explica el proceso para desplegar la solución (desarrollada localmente con Docker Compose) en un entorno de nube **AWS**, manteniendo la misma configuración e imágenes Docker y habilitando **escalabilidad horizontal**.

---

## 1. Estrategia de despliegue

El sistema ya está **containerizado** (un `Dockerfile` por servicio y un `docker-compose.yml` que orquesta todo). La migración a AWS consiste en:

1. Publicar las **imágenes** en un registro (Amazon **ECR**).
2. Desplegar los servicios con **Amazon ECS (Fargate)** o EC2.
3. Usar **RDS MySQL** (mismo motor 8.x que el local) para la base de datos.
4. Exponer los BFFs/API a través de un **Application Load Balancer (ALB)** con **HTTPS (ACM)**.
5. Gestionar secretos con **AWS Secrets Manager** y monitorear con **CloudWatch**.

### Mapeo local → AWS

| Recurso local | Recurso AWS | Detalle |
|---|---|---|
| `mysql` (Docker, :3307) | **Amazon RDS MySQL 8.x** | Mismo motor → migración de bajo riesgo |
| Servicios Spring Boot (imágenes) | **Amazon ECS (Fargate)** | Mismas imágenes, tareas por servicio |
| `api-gateway` / BFFs (:8082-8084, :8087) | **Application Load Balancer (ALB)** | Punto de entrada con reglas de enrutamiento |
| HTTPS autofirmado local | **ACM + ALB** | Certificado público, terminación TLS |
| Credenciales en `application.properties` | **AWS Secrets Manager / Parameter Store** | Gestión segura de secretos |
| `docker-compose` (red interna) | **VPC + subredes + Service Connect** | Descubrimiento y red segura |
| Kafka + Zookeeper | **Amazon MSK** (o Kafka en EC2) | Mensajería gestionada |
| `config-server` | ECS (servicio de configuración) | Igual que en local |
| `eureka-server` | ECS (service discovery) | Igual que en local |

---

## 2. Preparación del entorno AWS

### 2.1 Red (VPC)
```text
VPC (10.0.0.0/16)
├── Subred pública   (ALB)
├── Subredes privadas (ECS, RDS, MSK)
├── Internet Gateway
└── NAT Gateway (para que ECS acceda a internet/registro)
```

### 2.2 Base de datos (RDS)
1. Crear instancia **RDS MySQL 8.x** (Multi-AZ recomendado).
2. Crear la BD `banco_xyz` y el usuario `banco_user`.
3. Security Group: permitir acceso solo desde los grupos de seguridad de ECS (puerto 3306).
4. Habilitar `useSSL=true` en la URL JDBC en producción.

### 2.3 Mensajería (Kafka)
- Opción A: **Amazon MSK** (Kafka gestionado), con los tópicos `autenticaciones`, `clientes`, `retiros`, `transacciones`.
- Opción B: **Kafka + Zookeeper en EC2** con la misma configuración del `docker-compose` local.

#### 2.4 Kafka en una instancia EC2 (paso a paso — correctivo S3)

> El correctivo del profesor S3 requiere que la mensajería se ejecute en la **nube**, no solo en Docker local. Pasos recomendados:

1. **Lanzar una instancia EC2** (Amazon Linux/Ubuntu, t3.medium+, 20 GB EBS) e instalar Docker + Compose.
2. **Copiar el `docker-compose.yml`** de Kafka/Zookeeper (del proyecto) a la instancia:
   ```bash
   scp -i clave.pem docker-compose.yml ec2-user@<IP-PUBLICA>:~
   ```
   Usar el compose de Kafka que expone `29092`, y configurar:
   ```yaml
   KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092,PLAINTEXT_HOST://<IP-PUBLICA>:29092
   ```
3. **Abrir el Security Group** de la instancia: puerto **29092** (Kafka) desde tu IP y puertos 22/8080/8090 (SSH/Kafka UI).
4. **Apuntar los microservicios locales al broker remoto**:
   ```yaml
   # docker-compose.yml local (auth-server, ms-cuentas, ms-pagos, notificaciones-service)
   KAFKA_BOOTSTRAP_SERVERS: "<IP-PUBLICA>:29092"
   ```
5. **Levantar**:
   ```bash
   # en la instancia
   docker compose up -d
   ```
6. **Validad y captura evidencia**:
   - `docker compose ps` dentro de la instancia (kafka/zookeeper/kafka-ui sanos).
   - Kafka UI en `http://<IP-PUBLICA>:8090` apuntando al broker remoto.
   - `kafka-console-consumer --bootstrap-server <IP-PUBLICA>:29092 --topic retiros --from-beginning` consumiendo un evento publicado desde el entorno local.
   - Log del consumidor local (`notificaciones-service`) mostrando el evento recibido del broker remoto.

> Nota: si el laboratorio AWS Academy restringe permisos (p. ej. `AccessDenied` al listar S3), **verificar antes qué servicios están habilitados** (EC2, Security Groups) antes de intentar el despliegue; el proyecto queda 100 % listo para esta validación.

---

## 3. Publicar las imágenes (ECR)

```bash
# Autenticar con AWS
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin <account-id>.dkr.ecr.us-east-1.amazonaws.com

# Crear repositorios (uno por servicio)
for svc in config-server eureka-server api-gateway auth-server ms-clientes ms-cuentas ms-pagos banco-batch bff-web bff-mobile bff-atm notificaciones-service; do
  aws ecr create-repository --repository-name banco-xyz-$svc
done

# Construir y subir cada imagen
docker compose build
docker compose push   # requiere configurar image: <repo>/... en docker-compose o re-taggear
```

Alternativa: etiquetar y subir cada imagen manualmente:
```bash
docker tag banco-xyz-eft-ms-cuentas:latest <account-id>.dkr.ecr.us-east-1.amazonaws.com/banco-xyz-ms-cuentas:latest
docker push <account-id>.dkr.ecr.us-east-1.amazonaws.com/banco-xyz-ms-cuentas:latest
```

---

## 4. Desplegar con ECS (Fargate)

### 4.1 Definición de tareas
Crear una *task definition* por servicio. Ejemplo para `ms-cuentas`:

```json
{
  "family": "banco-xyz-ms-cuentas",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "512",
  "memory": "1024",
  "containerDefinitions": [
    {
      "name": "ms-cuentas",
      "image": "<account-id>.dkr.ecr.us-east-1.amazonaws.com/banco-xyz-ms-cuentas:latest",
      "portMappings": [{ "containerPort": 8092 }],
      "environment": [
        { "name": "DB_HOST", "value": "<rds-endpoint>" },
        { "name": "DB_PORT", "value": "3306" },
        { "name": "EUREKA_SERVER_URL", "value": "http://eureka-server:8761/eureka/" },
        { "name": "CONFIG_SERVER_URL", "value": "http://config-server:8888" },
        { "name": "JWK_SET_URI", "value": "http://auth-server:9000/oauth2/jwks" },
        { "name": "KAFKA_BOOTSTRAP_SERVERS", "value": "<msk-bootstrap>" }
      ],
      "secrets": [
        { "name": "DB_USER", "valueFrom": "arn:aws:ssm:...:parameter/banco-xyz/db_user" },
        { "name": "DB_PASSWORD", "valueFrom": "arn:aws:ssm:...:parameter/banco-xyz/db_password" }
      ],
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "banco-xyz",
          "awslogs-region": "us-east-1",
          "awslogs-stream-prefix": "ms-cuentas"
        }
      }
    }
  ]
}
```

### 4.2 Servicios ECS y descubrimiento
- Usar **AWS Cloud Map** (o Service Connect) para el *service discovery*: registrar `eureka-server`, `config-server`, `api-gateway`, `auth-server`, `ms-clientes`, `ms-cuentas`, `ms-pagos`, etc.
- Alternativa: desplegar `eureka-server` en ECS y apuntar `EUREKA_SERVER_URL` a su DNS interno. Los microservicios y el gateway se registran/descubren igual que en local.

### 4.3 Escalabilidad horizontal
- **Auto Scaling** en los servicios ECS (target: CPU ≥ 70% o peticiones por target en el ALB).
- El **ALB** balancea entre las réplicas; Eureka/Gateway reparten la carga entre instancias.
- Configurar `docker compose up --scale` localmente para validar el escalado antes de subir a AWS.

---

## 5. Load Balancer y HTTPS (ALB + ACM)

1. Crear un **Application Load Balancer** público.
2. Crear *target groups* por servicio (bff-web:8082, bff-mobile:8083, bff-atm:8084, api-gateway:8087, ...).
3. Solicitar un certificado en **AWS Certificate Manager (ACM)** para el dominio.
4. Crear *listeners* **HTTPS (443)** apuntando a los target groups; configurar reglas de enrutamiento (por *host* o *path*).
5. En los BFF, reemplazar el certificado autofirmado por el certificado ACM (terminación TLS en el ALB).

---

## 6. Secretos y configuración

- **AWS Secrets Manager / Parameter Store**: credenciales de BD, secretos OAuth (`web-secret`, `mobile-secret`, `atm-secret`) y claves JWT.
- Los servicios leen estas variables en tiempo de ejecución (ya están parametrizadas con variables de entorno: `DB_HOST`, `DB_USER`, `DB_PASSWORD`, `JWK_SET_URI`, `KAFKA_BOOTSTRAP_SERVERS`, etc.).
- El **Config Server** puede apuntar a un repositorio Git (en lugar de *native*) para centralizar la configuración en producción:
  ```properties
  spring.profiles.active=git
  spring.cloud.config.server.git.uri=https://github.com/<org>/<config-repo>
  ```

---

## 7. Monitoreo y operación

- **CloudWatch Logs**: logs de cada servicio (ya parametrizado en la task definition).
- **CloudWatch Metrics / Alarmas**: CPU, memoria, salud de los servicios y del ALB.
- **Healthchecks**: exponer `/actuator/health` y configurar healthchecks en el ALB y ECS.

---

## 8. Pasos resumidos

1. Preparar VPC (subredes públicas/privadas).
2. Crear RDS MySQL (BD `banco_xyz`) y Security Groups.
3. Crear Kafka (MSK o EC2) y los 4 tópicos.
4. Subir las 12 imágenes a **ECR**.
5. Crear *task definitions* y servicios **ECS Fargate** con las variables de entorno/secrets.
6. Crear **ALB** + certificado **ACM** y reglas de enrutamiento.
7. Validar escalabilidad (Auto Scaling) y resiliencia (reinicio automático + Resilience4j).
8. Monitorear con **CloudWatch** y documentar la evidencia.
