# pedido (equivalente a orden-ms del profe)

Microservicio que **PRODUCE** eventos a Kafka cada vez que se crea un pedido.

> Esta es la versión Java/Spring Boot de lo que el profe llama `orden-ms`.

---

## ¿Qué hace?

```
Cliente ──POST──▶  /api/v1/pedidos
                         │
                         ▼
                   [Guardar pedido en MySQL]
                         │
                         ▼
              [OrdenEventProducer.publicarOrdenCreada()]
                         │
                         ▼
                   ┌───────────────┐
                   │     KAFKA     │
                   │ orden-eventos │
                   └───────────────┘
```

- Recibe peticiones REST `POST /api/v1/pedidos`
- Guarda el pedido en su base de datos `db_pedido` (MySQL)
- **Publica un evento** `OrdenCreadaEvent` al topic Kafka `orden-eventos`
- El microservicio `pago` (consumer) lo recibe y crea un pago PENDIENTE

---

## Puertos

| Ambiente | Puerto |
|---|---|
| DEV | **9101** |
| PROD (docker) | 9102 |
| MySQL DEV | 3344 |

---

## Configuración Kafka

Archivo: `infra/config-repo/pedido-dev.yml`

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:41092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer

app:
  kafka:
    topics:
      orden-eventos: orden-eventos
```

---

## Clases clave

### Producer

`src/main/java/com/upeu/pedido/producer/OrdenEventProducer.java`

```java
@Component
public class OrdenEventProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topics.orden-eventos:orden-eventos}")
    private String ordenEventosTopic;

    public void publicarOrdenCreada(OrdenCreadaEvent event) {
        log.info("[PEDIDO] Publicando evento OrdenCreada al topic {} -> {}", ordenEventosTopic, event);
        kafkaTemplate.send(ordenEventosTopic, String.valueOf(event.getOrdenId()), event);
    }
}
```

### Evento

`src/main/java/com/upeu/pedido/event/OrdenCreadaEvent.java`

```java
public class OrdenCreadaEvent {
    private Long ordenId;
    private String cliente;
    private String estado;
    private Long timestamp;
    private String origen;
}
```

---

## Cómo levantarlo

```powershell
cd D:\ms1\ProyectoMS2026\services\pedido

# 1. Levantar MySQL del pedido
docker compose -f docker-compose-dev.yml up -d

# 2. Arrancar la app Spring Boot
mvn spring-boot:run
```

Espera ver:
```text
Started PedidoApplication in X.XXX seconds
```

---

## Cómo probarlo (disparar el producer)

```powershell
# Login
$body  = @{ username = "admin"; password = "admin123" } | ConvertTo-Json
$token = (Invoke-RestMethod -Method Post -Uri "http://localhost:7091/auth/login" -ContentType "application/json" -Body $body).accessToken
$h     = @{ Authorization = "Bearer $token" }

# Crear 1 pedido (publica 1 evento a Kafka)
Invoke-RestMethod -Method Post -Uri "http://localhost:7091/api/v1/pedidos" `
    -ContentType "application/json" -Headers $h `
    -Body '{"cliente":"Juan","estado":"NUEVO","observacion":"demo"}'

# Crear 10 pedidos en bucle (estilo producer_ordenes.py del profe)
1..10 | ForEach-Object {
    $p = @{ cliente = "Cliente_$_"; estado = "NUEVO"; observacion = "evento $_" } | ConvertTo-Json
    Invoke-RestMethod -Method Post -Uri "http://localhost:7091/api/v1/pedidos" -ContentType "application/json" -Headers $h -Body $p | Out-Null
    Write-Host "[PRODUCER] Pedido $_ publicado a Kafka"
    Start-Sleep -Milliseconds 500
}
```

---

## Cómo verificar que el evento llegó a Kafka

```powershell
docker compose -f D:\ms1\ProyectoMS2026\kafka\docker-compose-dev.yml exec kafka /opt/kafka/bin/kafka-console-consumer.sh --topic orden-eventos --bootstrap-server kafka:9092 --from-beginning
```

Verás JSON tipo:
```json
{"ordenId":15,"cliente":"Juan","estado":"NUEVO","timestamp":1779642...,"origen":"pedido"}
```

---

## Equivalencia con el profe

| Profe (`orden-ms`) | Tu proyecto (`pedido`) |
|---|---|
| Microservicio Spring Boot | ✅ Igual |
| Publica a topic `orden-eventos` | ✅ Igual |
| Recibe POST para crear órdenes | ✅ Igual (`/api/v1/pedidos`) |
| Usa KafkaTemplate | ✅ Igual |

---

## Endpoints

| Método | URL | Descripción |
|---|---|---|
| GET | `/api/v1/pedidos` | Lista todos los pedidos |
| GET | `/api/v1/pedidos/{id}` | Pedido por id |
| POST | `/api/v1/pedidos` | Crea pedido + publica a Kafka |
| GET | `/api/v1/pedido/instancia` | Diagnóstico (qué instancia respondió) |
| GET | `/actuator/health` | Salud del servicio |
| GET | `/actuator/prometheus` | Métricas para Prometheus |
| GET | `/swagger-ui/index.html` | Documentación Swagger |
