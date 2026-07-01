# Microservicios

Cada servicio vive en `services/` con su propia base de datos MySQL en Docker.

## Servicios

| Servicio | Puerto | Base de datos | Puerto MySQL DEV |
|---|---:|---|---:|
| auth | 8041 | db_auth | 3341 |
| catalogo | 8081 | db_catalogo | 3381 |
| producto | 9091 | db_producto | 3391 |
| pedido | 9101 | db_pedido | 3401 |
| pago | 9111 | db_pago | 3411 |

## Responsabilidades

### auth
Autenticación, registro, perfiles y emisión de JWT.

### catalogo
Gestión de categorías del catálogo.

### producto
CRUD de productos, imágenes, consulta a catálogo vía Feign.

### pedido
Creación de pedidos, descuento de stock, publicación de eventos Kafka.

### pago
Registro y consulta de pagos, consumo de eventos de pedidos.

## Patrón por servicio

```text
services/<nombre>/
  src/
  docker-compose-dev.yml   # MySQL local
  docker-compose.yml       # PROD
  pom.xml
  README.md
```
