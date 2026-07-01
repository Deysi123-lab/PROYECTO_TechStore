# Arranque y endpoints

Guía operativa para levantar TechStore en DEV y PROD.

## Orden de arranque (PROD)

1. **Infraestructura** — `infra/` → `docker compose up -d --build`
2. **Observabilidad** — `observability/` → `docker compose up -d --build`
3. **Kafka** — `kafka/` → `docker compose -f docker-compose-dev.yml up -d`
4. **Microservicios** — `services/auth`, `catalogo`, `producto`, `pedido`, `pago`

## Orden de arranque (DEV)

### Docker (solo dependencias)

```powershell
cd observability
docker compose -f docker-compose-dev.yml up -d

cd ..\kafka
docker compose -f docker-compose-dev.yml up -d

cd ..\services\auth
docker compose -f docker-compose-dev.yml up -d
# Repetir en catalogo, producto, pedido, pago
```

### Spring Boot (en este orden)

| # | Servicio | Puerto |
|---|---|---|
| 1 | infra/config-server | 7071 |
| 2 | infra/registry-server | 7081 |
| 3 | infra/gateway | 7091 |
| 4 | services/auth | 8041 |
| 5 | services/catalogo | 8081 |
| 6 | services/producto | 9091 |
| 7 | services/pedido | 9101 |
| 8 | services/pago | 9111 |

### Frontend

```powershell
cd e-commerce
ng serve
```

## Gateway (entrada única)

| Endpoint | Descripción |
|---|---|
| `POST /auth/login` | Login público |
| `POST /auth/register` | Registro público |
| `GET /api/v1/productos` | Catálogo público |
| `POST /api/v1/pedidos` | Crear pedido (JWT) |
| `GET /api/v1/pagos` | Listar pagos (JWT) |

## Usuarios de prueba

- `admin` / `admin123` (ADMIN)
- `user` / `user123` (USER)

## Observabilidad

- Prometheus: http://localhost:19090
- Grafana: http://localhost:13000 (admin/admin)
- Kafka UI: http://localhost:41085
