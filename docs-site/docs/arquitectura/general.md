# Arquitectura general

TechStore implementa una arquitectura de microservicios con punto único de acceso, configuración centralizada y descubrimiento dinámico de servicios.

## Flujo principal

```text
Angular (4200) → Gateway (7091) → Microservicios → MySQL
                      ↓
                   Eureka (7081)
                      ↓
                Config Server (7071)
```

## Comunicación entre servicios

| Origen | Destino | Mecanismo |
|---|---|---|
| producto | catalogo | OpenFeign + Circuit Breaker |
| pedido | producto | OpenFeign |
| pedido | pago | OpenFeign / Kafka |
| pago | pedido | Kafka consumer |

## Seguridad

- `auth` emite JWT con roles (`ROLE_ADMIN`, `ROLE_USER`).
- `gateway` valida el token y protege rutas.
- `producto` valida roles en operaciones de administración.

## Resiliencia

- Circuit Breaker en llamadas de `producto` hacia `catalogo`.
- Configuración externa en `infra/config-repo/producto-dev.yml`.

## Consistencia

- Pedido y pago coordinan el flujo de compra.
- Kafka permite desacoplar la creación de pedidos y el registro de pagos.
