# S13 - Validación end-to-end

## Objetivo

Validar el flujo completo de TechStore desde el frontend hasta la persistencia en cada microservicio.

## Checklist

- [ ] Infraestructura UP (Config, Eureka, Gateway)
- [ ] Microservicios registrados en Eureka
- [ ] Login y JWT funcionando
- [ ] Listado de productos desde Angular
- [ ] Creación de pedido con JWT
- [ ] Pago registrado automáticamente
- [ ] Kafka publica y consume eventos (si está habilitado)
- [ ] Métricas visibles en Prometheus/Grafana

## Flujo a demostrar

```text
Login → Ver productos → Agregar al carrito → Crear pedido → Ver pago PENDIENTE
```

## Evidencias

- Captura de Eureka con todos los servicios
- Captura de Grafana con métricas `up`
- Respuesta JSON de `GET /api/v1/pagos` tras crear pedido
