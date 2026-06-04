D:\ms1\ProyectoMS2026\PRUEBAS.md

INFRA 
mvn spring-boot:run

CONF SERVE
http://localhost:7071/producto/dev
http://localhost:7071/catalogo/dev
http://localhost:7071/gateway/dev
http://localhost:7071/auth/dev
http://localhost:7071/pedido/dev
http://localhost:7071/pago/dev

EUREKA
http://localhost:7081/

GATEWAY
http://localhost:7091/actuator/health

MICROSERVICIOS
 docker compose -f docker-compose-dev.yml up 
mvn spring-boot:run
http://localhost:8041/swagger-ui/index.html
http://localhost:8081/swagger-ui/index.html
http://localhost:9091/swagger-ui/index.html
http://localhost:9101/swagger-ui/index.html
http://localhost:9111/swagger-ui/index.html

Cómo probar en dev:EN CUAL QUIER LUGAR POR SEPARADO

$body = @{
  username = "user"
  password = "user123"
} | ConvertTo-Json


$response = Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:7091/auth/login" `
  -ContentType "application/json" `
  -Body $body

$token = $response.accessToken


$response


Write-Host "TOKEN: $token"


-*

============================================================
SESION OBSERVABILIDAD (PROMETHEUS + LOKI + GRAFANA)
PROYECTO COMPLETO: gateway + auth + catalogo + producto + pedido + pago
============================================================

Herramientas:
  Prometheus -> metricas
  Loki       -> logs centralizados
  Promtail   -> envio de logs a Loki
  Grafana    -> dashboards, exploracion y alertas


Flujo del proyecto:
  Cliente -> Gateway -> Auth (login)
                    -> Catalogo
                    -> Producto -> Catalogo (Feign)
                    -> Pedido   -> KAFKA -> Pago
                       |          |        |
                       +-- logs y metricas de TODOS --+
                                  |
                        Prometheus + Loki + Grafana


============================================================
PASO 0. PREPARACION
============================================================

# Levantar observability
cd D:\ms1\ProyectoMS2026\observability
docker compose -f docker-compose-dev.yml up -d

# Verificar que esten arriba los 4
docker ps --filter "name=prometheus" --filter "name=grafana" --filter "name=loki" --filter "name=promtail" --format "table {{.Names}}\t{{.Status}}"

# URLs
Prometheus: http://localhost:19090
Grafana:    http://localhost:13000   (admin / admin)
Loki:       http://localhost:13100   (se consulta via Grafana, no por navegador)


============================================================
CASO 1. METRICAS ESENCIALES
"que esta pasando en el sistema?"
============================================================

# 1.1 — Validar servicios vivos en Prometheus
http://localhost:19090/targets

  Se espera UP en:
    - gateway-dev
    - auth-dev          (si aun no esta, agregar al prometheus-dev.yml)
    - catalogo-dev
    - producto-dev
    - pedido-dev
    - pago-dev

Consulta PromQL:
  up

  Lectura: "Prometheus, dime si cada target esta disponible"
  1 = puede leer metricas
  0 = no puede leer metricas


# 1.2 — Generar trafico

# Login (necesario para POST pedidos)
$body  = @{ username = "admin"; password = "admin123" } | ConvertTo-Json
$res   = Invoke-RestMethod -Method Post -Uri "http://localhost:7091/auth/login" -ContentType "application/json" -Body $body
$token = $res.accessToken
$h     = @{ Authorization = "Bearer $token" }

# Ráfaga de trafico (lectura GETs publicos)
1..30 | ForEach-Object {
    Invoke-RestMethod -Method Get -Uri "http://localhost:7091/api/v1/categorias"            | Out-Null
    Invoke-RestMethod -Method Get -Uri "http://localhost:7091/api/v1/productos"             | Out-Null
    Invoke-RestMethod -Method Get -Uri "http://localhost:7091/api/v1/productos/detalle/1"   | Out-Null
    Invoke-RestMethod -Method Get -Uri "http://localhost:7091/api/v1/pedidos"               | Out-Null
    Invoke-RestMethod -Method Get -Uri "http://localhost:7091/api/v1/pagos"                 | Out-Null
    Invoke-RestMethod -Method Get -Uri "http://localhost:7091/api/v1/catalogo/instancia"    | Out-Null
    Invoke-RestMethod -Method Get -Uri "http://localhost:7091/api/v1/producto/instancia"    | Out-Null
    Invoke-RestMethod -Method Get -Uri "http://localhost:7091/api/v1/pedido/instancia"      | Out-Null
    Invoke-RestMethod -Method Get -Uri "http://localhost:7091/api/v1/pago/instancia"        | Out-Null
    Write-Host "Vuelta $_"
    Start-Sleep -Milliseconds 150
}

# Crear 5 pedidos (escritura — usa Auth y Kafka)
1..5 | ForEach-Object {
    $p = @{ cliente = "Cliente_$_"; estado = "NUEVO"; observacion = "demo $_" } | ConvertTo-Json
    Invoke-RestMethod -Method Post -Uri "http://localhost:7091/api/v1/pedidos" -ContentType "application/json" -Headers $h -Body $p | Out-Null
    Write-Host "Pedido $_ creado"
    Start-Sleep -Milliseconds 300
}


# 1.3 — Requests por segundo por servicio

Consulta PromQL:
  sum by (job) (rate(http_server_requests_seconds_count[1m]))

  Panel sugerido:
    tipo:      Time series
    titulo:    Requests por segundo
    leyenda:   {{job}}

  Lectura: "cuantas requests/seg recibe cada servicio en el ultimo minuto"
  Esperado al lanzar la rafaga:
    gateway-dev sube primero
    producto-dev sube (por GET productos)
    catalogo-dev sube (Feign desde producto)
    pedido-dev y pago-dev suben cuando creas pedidos


# 1.4 — Errores HTTP 5xx

Consulta PromQL:

  sum by (job, status) (rate(http_server_requests_seconds_count{status=~"5.."}[1m]))

  Esperado: cero en condiciones normales.
  Si sube en producto-dev al apagar catalogo, ahi se ve la utilidad.


# 1.5 — Errores HTTP 4xx

Consulta PromQL:

  sum by (job, status) (rate(http_server_requests_seconds_count{status=~"4.."}[1m]))

  Sirve para detectar 401 (sin token) o 403 (rol insuficiente).
  Sube cuando intentas crear/borrar productos con un user sin rol ADMIN.


# 1.6 — Latencia promedio (ms)

Consulta PromQL:

  sum by (job) (rate(http_server_requests_seconds_sum[1m])) / sum by (job) (rate(http_server_requests_seconds_count[1m])) * 1000

  Lectura: tiempo promedio (en ms) que tarda cada servicio en responder.


# 1.7 — CPU del proceso

Consulta PromQL:

  avg by (job) (process_cpu_usage) * 100

  Esperado: <5% en reposo. Sube cuando lanzas la rafaga.


# 1.8 — Memoria heap (MB)

Consulta PromQL:
  sum by (job) (jvm_memory_used_bytes{area="heap"}) / 1024 / 1024


# 1.9 — Threads vivos

Consulta PromQL:
  avg by (job) (jvm_threads_live_threads)





  *****

 LOKI LOKI *LOKI* LOKI LOKI 
============================================================
CASO 2. LOGS EN LOKI
"que paso dentro de los servicios?"
============================================================

# Ir a Grafana
http://localhost:13000
  Menu izquierdo -> Explore
  Datasource: Loki
  Range: Last 15 minutes

# 2.1 — Confirmar que Loki recibe logs de TODOS los servicios

Consultas LogQL (probar una por una):

  {service="gateway"}
  {service="auth"}
  {service="catalogo"}
  {service="producto"}
  {service="pedido"}
  {service="pago"}

Si una no devuelve resultados:
  - revisa que el servicio este corriendo
  - revisa que escriba a services/<nombre>/logs/*.log
  - revisa promtail/config.yml


# 2.2 — Ver flujo completo de una peticion

Consulta LogQL:

  {service=~"gateway|auth|catalogo|producto|pedido|pago"}

  Sirve para ver TODOS los logs juntos y seguir el flujo:
    gateway recibe peticion
    auth valida JWT
    producto procesa
    catalogo responde (Feign)
    pedido publica a kafka
    pago consume de kafka


# 2.3 — Filtrar por texto (reducir ruido)

  {service="gateway"}  |= "GATEWAY"
  {service="auth"}     |= "login"
  {service="auth"}     |= "AUTH"
  {service="catalogo"} |= "CATALOGO"
  {service="producto"} |= "PRODUCTO"
  {service="pedido"}   |= "PEDIDO"
  {service="pago"}     |= "PAGO"

  Solo errores:
  {service=~".+"} |= "ERROR"

  Cuando arranca un servicio:
  {service=~".+"} |= "Started"


# 2.4 — Logs de KAFKA (eventos)

  # Productor (pedido publica)
  {service="pedido"} |= "OrdenCreada"
  {service="pedido"} |= "publicado"

  # Consumidor (pago consume)
  {service="pago"} |= "OrdenCreada"
  {service="pago"} |= "recibido"

  # Si tu codigo loguea con corchetes [PEDIDO][KAFKA]:
  {service="pedido"} |= "[KAFKA]"
  {service="pago"}   |= "[KAFKA]"


# 2.5 — Seguir un traceId (correlacion entre servicios)

  # Primero ejecuta una peticion y copia el x-trace-id del response header
  Invoke-WebRequest -Uri "http://localhost:7091/api/v1/productos/detalle/1" |
    Select-Object -ExpandProperty Headers

  # Toma el valor de "x-trace-id" y busca en Loki:
  {service=~".+"} |= "TU_TRACE_ID_AQUI"

  Vas a ver el camino completo: gateway -> producto -> catalogo con el mismo ID.


============================================================
CASO 3. FALLAS CONTROLADAS (combinar metricas + logs)
============================================================

# 3.1 — Apagar CATALOGO (impacta a producto vía Feign)

  Ctrl + C en la terminal de catalogo
  
  Probar:
    Invoke-RestMethod -Uri "http://localhost:7091/api/v1/productos/detalle/1"

  En Prometheus:
    up{job="catalogo-dev"}                         -> debe ser 0
    rate(http_server_requests_seconds_count{status=~"5.."}[1m])
    # Si tienes Circuit Breaker bien configurado, el detalle retorna producto
    # con categoria = null (fallback). Si NO esta bien, ves 5xx en producto.

  En Loki:
    {service="producto"} |= "ERROR"
    {service="producto"} |= "Feign"
    {service="producto"} |= "CircuitBreaker"

  Volver a levantar:
    cd D:\ms1\ProyectoMS2026\services\catalogo
    mvn spring-boot:run


# 3.2 — Apagar AUTH (impacta a TODO el sistema con JWT)

  Ctrl + C en la terminal de auth
  
  Probar:
    $body = @{ username = "admin"; password = "admin123" } | ConvertTo-Json
    Invoke-RestMethod -Method Post -Uri "http://localhost:7091/auth/login" -ContentType "application/json" -Body $body
    # Debe fallar (timeout o 503)

  En Prometheus:
    up{job="auth-dev"}                             -> 0

  En Loki:
    {service="gateway"} |= "auth"
    {service="gateway"} |= "ERROR"


# 3.3 — Apagar KAFKA (impacta a la SAGA pedido->pago)

  cd D:\ms1\ProyectoMS2026\kafka
  docker compose -f docker-compose-dev.yml stop kafka

  Crear pedido:
    $body  = @{ username = "admin"; password = "admin123" } | ConvertTo-Json
    $res   = Invoke-RestMethod -Method Post -Uri "http://localhost:7091/auth/login" -ContentType "application/json" -Body $body
    $token = $res.accessToken
    $p     = @{ cliente = "TEST"; estado = "NUEVO"; observacion = "kafka caido" } | ConvertTo-Json
    Invoke-RestMethod -Method Post -Uri "http://localhost:7091/api/v1/pedidos" -ContentType "application/json" -Headers @{ Authorization = "Bearer $token" } -Body $p

  Esperado:
    - Pedido se crea OK en su base de datos
    - PERO no aparece pago automatico (kafka no recibio el evento)
    - En los logs de pedido vas a ver intento de conexion fallido a kafka

  En Loki:
    {service="pedido"} |= "kafka"
    {service="pedido"} |= "broker"

  Volver a levantar:
    docker compose -f docker-compose-dev.yml start kafka


# 3.4 — Apagar PEDIDO (consumidor de la API, pago sigue vivo)

  Ctrl + C en la terminal de pedido

  En Prometheus:
    up{job="pedido-dev"}                           -> 0

  En el navegador (debe fallar):
    http://localhost:7091/api/v1/pedidos


# 3.5 — Apagar PAGO (consumidor de Kafka)

  Ctrl + C en la terminal de pago

  Crear pedidos:
    Aunque pago este caido, pedido publica los eventos a Kafka.
    Cuando pago vuelva a subir, debe consumir los eventos pendientes
    (eso demuestra que Kafka guarda los mensajes).

  En Loki cuando pago vuelva:
    {service="pago"} |= "OrdenCreada"


============================================================
CASO 4. ALERTAS EN GRAFANA
============================================================

Crear alertas para los servicios criticos:

# 4.1 — Alerta: AUTH caido
Grafana -> Alerting -> Alert rules -> New alert rule
  Nombre:     Auth caido
  Datasource: Prometheus
  Consulta:   up{job="auth-dev"}
  Condicion:  IS BELOW 1
  Evaluacion: Every 30s for 1m
  Mensaje:    "Auth no responde. Ningun login JWT funcionara."

# 4.2 — Alerta: GATEWAY caido
  Consulta:   up{job="gateway-dev"}
  Mensaje:    "Gateway caido. Todo el trafico publico esta interrumpido."

# 4.3 — Alerta: CATALOGO caido
  Consulta:   up{job="catalogo-dev"}
  Mensaje:    "Catalogo caido. Producto/detalle perdera la categoria."

# 4.4 — Alerta: PRODUCTO caido
  Consulta:   up{job="producto-dev"}

# 4.5 — Alerta: PEDIDO caido
  Consulta:   up{job="pedido-dev"}

# 4.6 — Alerta: PAGO caido
  Consulta:   up{job="pago-dev"}

# 4.7 — Alerta: Errores 5xx altos
  Consulta:   sum by (job) (rate(http_server_requests_seconds_count{status=~"5.."}[1m]))
  Condicion:  IS ABOVE 0.1
  Evaluacion: Every 30s for 2m
  Mensaje:    "Algun servicio esta devolviendo mas de 0.1 errores 5xx por segundo"

# 4.8 — Alerta: Memoria heap alta
  Consulta:   max by (job) (jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}) * 100
  Condicion:  IS ABOVE 80
  Mensaje:    "Heap por encima de 80% — posible fuga de memoria"


============================================================
DASHBOARDS LISTOS PARA IMPORTAR
============================================================

Grafana -> Dashboards -> New -> Import -> escribir ID -> Load -> datasource Prometheus -> Import

  4701    JVM Micrometer            (CPU, memoria, threads, GC por servicio)
  12900   Spring Boot APM Dashboard (latencia, throughput, errores)
  14430   Spring Boot Statistics    (alternativo)
  7362    MySQL Overview            (si quieres ver tus 5 BDs)


============================================================
KAFKA — RESUMEN SIMPLE (8 PASOS) — ADAPTADO A TU PROYECTO
============================================================

Equivalencias entre la guia original y tu proyecto:

   ORIGINAL              TU PROYECTO
   ---------             ----------------------
   orden-ms              pedido      (services/pedido)
   pago-ms               pago        (services/pago)
   orden-py-del          (no aplica) tu proyecto no usa Python
   topic orden-eventos   orden-eventos  (igual)
   puerto 19051          7091 (gateway)  o  9101 (pedido directo)
   endpoint /api/v1/ordenes   /api/v1/pedidos


------------------------------------------------------------
PASO 1 — CLONAR PROYECTOS (NO APLICA)
------------------------------------------------------------

Ya tienes todo el codigo en D:\ms1\ProyectoMS2026.
No necesitas clonar nada.


------------------------------------------------------------
PASO 2 — LEVANTAR KAFKA
------------------------------------------------------------

cd D:\ms1\ProyectoMS2026\kafka
docker compose -f docker-compose-dev.yml up -d

# Verificar
docker ps --filter "name=kafka"


------------------------------------------------------------
PASO 3 — ENTRAR AL CONTENEDOR KAFKA
------------------------------------------------------------

docker compose -f docker-compose-dev.yml exec kafka bash

# Estaras dentro del contenedor. Para salir:
exit


------------------------------------------------------------
PASO 4 — CREAR TOPIC
------------------------------------------------------------

# DENTRO del contenedor (despues del paso 3):
/opt/kafka/bin/kafka-topics.sh --create --topic orden-eventos --bootstrap-server kafka:9092 --partitions 1 --replication-factor 1

# DESDE FUERA del contenedor (PowerShell normal):
docker compose -f docker-compose-dev.yml exec kafka /opt/kafka/bin/kafka-topics.sh --create --topic orden-eventos --bootstrap-server kafka:9092 --partitions 1 --replication-factor 1

# NOTA: si el topic ya existe, vas a ver un error. Eso esta bien.
# Para verificar que existe:
docker compose -f docker-compose-dev.yml exec kafka /opt/kafka/bin/kafka-topics.sh --list --bootstrap-server kafka:9092


------------------------------------------------------------
PASO 5 — PROBAR MENSAJES (PRODUCER Y CONSUMER MANUALES)
------------------------------------------------------------

>>> PRODUCER (terminal 1 — escribe mensajes a mano) <<<

docker compose -f docker-compose-dev.yml exec -it kafka /opt/kafka/bin/kafka-console-producer.sh --topic orden-eventos --bootstrap-server kafka:9092

# Cuando aparezca el cursor ">", escribe:
hola

# Aprieta Enter. Puedes escribir mas mensajes.
# Para salir: Ctrl + C


>>> CONSUMER (terminal 2 — lee los mensajes) <<<

docker compose -f docker-compose-dev.yml exec kafka /opt/kafka/bin/kafka-console-consumer.sh --topic orden-eventos --bootstrap-server kafka:9092 --from-beginning

# Debes ver:
hola

# Y todos los mensajes que escribas en el producer apareceran aqui en tiempo real.
# Para salir: Ctrl + C


------------------------------------------------------------
PASO 6 — PROBAR PRODUCER Y CONSUMER (equivalente a Python)
------------------------------------------------------------

>>> ORIGINAL DEL PROFE (Python) <<<
   docker compose exec orden-py python /app/producer_ordenes.py
   docker compose exec orden-py python /app/consumer_ordenes.py

>>> EN TU PROYECTO (Java) <<<
   El producer ya esta DENTRO de "pedido"   (clase OrdenEventProducer)
   El consumer ya esta DENTRO de "pago"     (clase OrdenEventConsumer @KafkaListener)

   No necesitas correr scripts aparte: cuando levantas pedido y pago
   (paso 7), el producer y el consumer ya estan ARRIBA y trabajando.


>>> CODIGO REAL EN TU PROYECTO <<<

  PRODUCER  -> services/pedido/src/main/java/com/upeu/pedido/producer/OrdenEventProducer.java
              metodo: publicarOrdenCreada(event)
              hace:   kafkaTemplate.send("orden-eventos", ordenId, event);

  CONSUMER  -> services/pago/src/main/java/com/upeu/pago/consumer/OrdenEventConsumer.java
              metodo: consumirOrdenCreada(event)   (anotado con @KafkaListener)
              hace:   crea un Pago con estado PENDIENTE
                      asociado al ordenId del evento


>>> COMO DISPARAR EL PRODUCER (equivale a producer_ordenes.py) <<<

# Login para sacar el token
$body  = @{ username = "admin"; password = "admin123" } | ConvertTo-Json
$token = (Invoke-RestMethod -Method Post -Uri "http://localhost:7091/auth/login" -ContentType "application/json" -Body $body).accessToken
$h     = @{ Authorization = "Bearer $token" }

# 1 evento
Invoke-RestMethod -Method Post -Uri "http://localhost:7091/api/v1/pedidos" -ContentType "application/json" -Headers $h -Body '{"cliente":"Juan","estado":"NUEVO","observacion":"demo"}'

# 10 eventos seguidos (estilo "producer_ordenes.py" en bucle)
1..10 | ForEach-Object {
    $p = @{ cliente = "Cliente_$_"; estado = "NUEVO"; observacion = "evento $_" } | ConvertTo-Json
    Invoke-RestMethod -Method Post -Uri "http://localhost:7091/api/v1/pedidos" -ContentType "application/json" -Headers $h -Body $p | Out-Null
    Write-Host "[PRODUCER] Pedido $_ publicado a Kafka"
    Start-Sleep -Milliseconds 300
}


>>> COMO VER QUE EL CONSUMER FUNCIONA (equivale a consumer_ordenes.py) <<<

# Opcion A — en los LOGS de pago (la terminal donde corre mvn spring-boot:run)
# Debes ver lineas como:
#   [PAGO] Evento OrdenCreada recibido: ordenId=5, cliente=Juan, estado=NUEVO
#   [PAGO] Registro de pago PENDIENTE creado para pedido 5

# Opcion B — pedir los pagos por API (deberian haber crecido)
Invoke-RestMethod -Method Get -Uri "http://localhost:7091/api/v1/pagos"

# Opcion C — leer el topic directo desde Kafka (como hace el script python por dentro)
docker compose -f docker-compose-dev.yml exec kafka /opt/kafka/bin/kafka-console-consumer.sh --topic orden-eventos --bootstrap-server kafka:9092 --from-beginning


>>> EXPLICACION PARA SUSTENTAR <<<

"Mi profe usa producer_ordenes.py y consumer_ordenes.py como scripts
sueltos en Python para enviar y recibir mensajes de Kafka.
En MI proyecto la misma logica esta integrada en microservicios Java:
 - PEDIDO actua como PRODUCER (publica eventos en cada POST /pedidos).
 - PAGO   actua como CONSUMER (escucha el topic con @KafkaListener y
   crea automaticamente un pago PENDIENTE por cada evento recibido).
Asi demuestro el mismo flujo asincrono pero con microservicios reales,
no scripts."


------------------------------------------------------------
PASO 7 — LEVANTAR MICROSERVICIOS (pedido = producer, pago = consumer)
------------------------------------------------------------

>>> PEDIDO (equivale a orden-ms) <<<

cd D:\ms1\ProyectoMS2026\services\pedido

# Levantar la base de datos:
docker compose -f docker-compose-dev.yml up -d

# Arrancar la app Spring Boot:
mvn spring-boot:run

# Esperar ver: Started PedidoApplication


>>> PAGO (equivale a pago-ms) <<<

cd D:\ms1\ProyectoMS2026\services\pago

# Levantar la base de datos:
docker compose -f docker-compose-dev.yml up -d

# Arrancar la app Spring Boot:
mvn spring-boot:run

# Esperar ver: Started PagoApplication


------------------------------------------------------------
PASO 8 — PROBAR LA API (CREAR PEDIDO -> KAFKA -> PAGO)
------------------------------------------------------------

# Login para sacar el JWT (tu proyecto pide token, el de la guia no)
$body  = @{ username = "admin"; password = "admin123" } | ConvertTo-Json
$res   = Invoke-RestMethod -Method Post -Uri "http://localhost:7091/auth/login" -ContentType "application/json" -Body $body
$token = $res.accessToken
$h     = @{ Authorization = "Bearer $token" }

# Crear pedido (esto publica un evento a Kafka)
Invoke-RestMethod -Method Post -Uri "http://localhost:7091/api/v1/pedidos" -ContentType "application/json" -Headers $h -Body '{"cliente":"Juan","estado":"NUEVO","observacion":"demo"}'

# Esperar 3 segundos y ver los pagos (debe aparecer uno PENDIENTE creado por pago automaticamente)
Start-Sleep 3
Invoke-RestMethod -Method Get -Uri "http://localhost:7091/api/v1/pagos"


==> QUE DEBE PASAR (flujo)

   PEDIDO recibe POST
      |
      v
   publica evento OrdenCreada en Kafka (topic: orden-eventos)
      |
      v
   PAGO (@KafkaListener) consume el evento
      |
      v
   PAGO crea un registro de pago en estado PENDIENTE
      |
      v
   GET /api/v1/pagos lo muestra


==> QUE DEMUESTRAS

   [+] Kafka funcionando
   [+] Producer funcionando        (pedido publica)
   [+] Consumer funcionando        (pago consume con @KafkaListener)
   [+] Eventos en tiempo real      (pago < 1 seg despues del pedido)
   [+] Arquitectura distribuida    (cada microservicio en su BD)
   [+] Comunicacion asincrona      (pedido no espera respuesta de pago)
   [+] Microservicios desacoplados (si pago se cae, pedido sigue OK)


============================================================
KAFKA — VERSION AVANZADA (OBSERVABILIDAD DEL PIPELINE)
(adaptado de la Sesion U2 S9 P2: Observabilidad de pipelines)
============================================================

En esta sesion el "pipeline observable" es:

    pedido (producer) -> Kafka -> pago (consumer)

La capa de observabilidad minima es:

    Kafka -> kafka-exporter -> Prometheus -> Grafana

Lo que queremos demostrar:
  1. Kafka esta disponible y recibiendo eventos
  2. pedido (producer) genera eventos al topic
  3. pago (consumer) consume y procesa los eventos
  4. Se puede medir latencia produccion -> consumo
  5. Se puede medir throughput (eventos/seg)
  6. Existen logs estructurados de pedido y pago
  7. Prometheus recolecta metricas via kafka-exporter
  8. Grafana muestra un tablero minimo


------------------------------------------------------------
PASO 0 — PREPARACION
------------------------------------------------------------

# Levantar kafka + exporter + kafka-ui
cd D:\ms1\ProyectoMS2026\kafka
docker compose -f docker-compose-dev.yml up -d

# Verificar
docker ps --filter "name=kafka" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

# Levantar observability (si no esta)
cd ..\observability
docker compose -f docker-compose-dev.yml up -d

# URLs del entorno DEV
Kafka desde host:        localhost:41092
Kafka entre contenedor:  kafka:9092
Kafka exporter:          http://localhost:41308/metrics
Kafka UI:                http://localhost:41085
Prometheus:              http://localhost:19090
Grafana:                 http://localhost:13000   (admin/admin)
Topic:                   orden-eventos


------------------------------------------------------------
8.1 — VERIFICAR EL ENDPOINT DE METRICAS DEL EXPORTER
------------------------------------------------------------

Abrir en navegador:
  http://localhost:41308/metrics

Buscar estas lineas en el texto plano que sale:

  kafka_brokers 1
  kafka_broker_info{address="kafka:9092",id="1"} 1

Interpretacion:
  - el exporter esta arriba
  - el exporter SI esta viendo el broker Kafka


------------------------------------------------------------
8.2 — VERIFICAR PROMETHEUS (targets)
------------------------------------------------------------

Abrir:
  http://localhost:19090/targets

Targets que deben estar en UP:
  - prometheus            (se scrapea a si mismo)
  - kafka-exporter-dev    (Prometheus leyendo metricas de Kafka)
  - pedido-dev            (microservicio productor)
  - pago-dev              (microservicio consumidor)

Si "kafka-exporter-dev" no aparece o esta DOWN:
  - revisar el job en observability/prometheus/prometheus-dev.yml
  - revisar que el contenedor kafka-exporter este UP


------------------------------------------------------------
8.3 — EXPLORAR METRICAS EN GRAFANA
------------------------------------------------------------

Abrir Grafana:
  http://localhost:13000   (admin / admin)

Menu izquierdo -> Explore -> datasource Prometheus

Queries para probar (una por una):

  # ¿Hay broker visible?
  kafka_brokers

  # Info del broker
  kafka_broker_info

  # ¿El exporter responde?
  up{job="kafka-exporter-dev"}

  # Lag del consumer group de pago
  kafka_consumergroup_lag

  # Lag solo del grupo pago
  kafka_consumergroup_lag{consumergroup="pago-group"}

  # Offset actual del topic orden-eventos
  kafka_topic_partition_current_offset{topic="orden-eventos"}

Interpretacion:
  kafka_broker_info = 1              -> Prometheus ve al broker
  up{kafka-exporter-dev} = 1         -> Prometheus puede leer el exporter
  kafka_consumergroup_lag = 0        -> pago esta al dia (sin mensajes pendientes)
  kafka_consumergroup_lag > 0        -> hay mensajes pendientes de consumo


------------------------------------------------------------
8.4 — GENERAR EVENTOS DESDE EL PRODUCER (pedido)
------------------------------------------------------------

El producer en este proyecto es el microservicio "pedido".
Cuando creas un pedido, pedido publica un evento al topic "orden-eventos".

Cada evento debe contener como minimo:
  tipoEvento, ordenId, total, estado, timestamp, origen

# Login
$body  = @{ username = "admin"; password = "admin123" } | ConvertTo-Json
$res   = Invoke-RestMethod -Method Post -Uri "http://localhost:7091/auth/login" -ContentType "application/json" -Body $body
$token = $res.accessToken
$h     = @{ Authorization = "Bearer $token" }

# Generar 20 eventos (crear 20 pedidos -> publica 20 mensajes al topic)
1..20 | ForEach-Object {
    $p = @{ cliente = "Cliente_$_"; estado = "NUEVO"; observacion = "evento $_" } | ConvertTo-Json
    Invoke-RestMethod -Method Post -Uri "http://localhost:7091/api/v1/pedidos" -ContentType "application/json" -Headers $h -Body $p | Out-Null
    Write-Host "Pedido $_ publicado a Kafka"
    Start-Sleep -Milliseconds 300
}

# Validar que se publicaron mirando los mensajes en el topic
docker compose -f docker-compose-dev.yml exec kafka /opt/kafka/bin/kafka-console-consumer.sh --topic orden-eventos --bootstrap-server kafka:9092 --from-beginning --max-messages 5

# Tambien puedes ver Kafka UI
http://localhost:41085   (Topics -> orden-eventos -> Messages)

# Logs estructurados del producer (pedido) en Loki:
Grafana -> Explore -> Loki:
  {service="pedido"} |= "OrdenCreada"
  {service="pedido"} |= "publicado"


------------------------------------------------------------
8.5 — CONSUMIR EVENTOS CON pago (consumer)
------------------------------------------------------------

En este proyecto el consumer es el microservicio "pago" (en vez de Spark).
Cuando pedido publica un evento, pago lo consume con un @KafkaListener y
crea un pago en estado PENDIENTE asociado al pedido.

Validacion:

# 1. Ver pagos ANTES de crear pedidos
Write-Host "=== PAGOS ANTES ==="
Invoke-RestMethod -Method Get -Uri "http://localhost:7091/api/v1/pagos"

# 2. Crear 1 pedido
$pedido = @{ cliente = "Juan"; estado = "NUEVO"; observacion = "demo pipeline" } | ConvertTo-Json
$nuevo = Invoke-RestMethod -Method Post -Uri "http://localhost:7091/api/v1/pedidos" -ContentType "application/json" -Headers $h -Body $pedido
$nuevo

# 3. Esperar a que pago consuma el evento
Start-Sleep -Seconds 3

# 4. Ver pagos DESPUES — debe aparecer un PENDIENTE nuevo
Write-Host "`n=== PAGOS DESPUES (debe haber uno PENDIENTE nuevo) ==="
Invoke-RestMethod -Method Get -Uri "http://localhost:7091/api/v1/pagos"

# Logs estructurados del consumer (pago) en Loki:
Grafana -> Explore -> Loki:
  {service="pago"} |= "OrdenCreada"
  {service="pago"} |= "recibido"


------------------------------------------------------------
8.6 — CALCULAR LATENCIA
------------------------------------------------------------

Cada evento publicado por pedido lleva un campo "timestamp" (momento
en que se genero/publico). pago, al consumirlo, puede registrar el
momento de procesamiento ("processedAt") y calcular:

  latenciaMs = processedAt - timestamp

Validacion rapida (mirar logs en Loki):
  {service="pedido"} |= "timestamp"
  {service="pago"}   |= "processedAt"
  {service="pago"}   |= "latencyMs"

Registrar como evidencia:
  - latencia minima
  - latencia promedio
  - latencia maxima

En este proyecto, normalmente:
  latenciaMs < 50 ms      -> excelente
  latenciaMs 50-500 ms    -> normal
  latenciaMs > 1000 ms    -> investigar


------------------------------------------------------------
8.7 — ESTIMAR THROUGHPUT (eventos/seg)
------------------------------------------------------------

Calcular eventos publicados / tiempo:

  20 pedidos en 6 segundos = 3.3 eventos/seg

Tambien con metricas en Prometheus:

  # Tasa de publicacion al topic (mensajes/seg en el broker)
  rate(kafka_topic_partition_current_offset{topic="orden-eventos"}[1m])

  # Tasa de consumo por el grupo pago-group
  rate(kafka_consumergroup_current_offset{consumergroup="pago-group"}[1m])

  # Diferencia (lag) — si crece, pago no alcanza
  kafka_consumergroup_lag{consumergroup="pago-group"}


------------------------------------------------------------
8.8 — DASHBOARD MINIMO EN GRAFANA
------------------------------------------------------------

Grafana -> Dashboards -> New -> New Dashboard -> Add visualization

Crear 4 paneles:

Panel 1: Kafka Brokers
  Consulta: kafka_brokers
  Tipo:     Stat
  Titulo:   "Brokers visibles"

Panel 2: Kafka Exporter UP
  Consulta: up{job="kafka-exporter-dev"}
  Tipo:     Stat
  Titulo:   "Exporter disponible"

Panel 3: Mensajes publicados al topic
  Consulta: kafka_topic_partition_current_offset{topic="orden-eventos"}
  Tipo:     Time series
  Titulo:   "Offsets en orden-eventos"

Panel 4: Lag del consumer pago-group
  Consulta: kafka_consumergroup_lag{consumergroup="pago-group"}
  Tipo:     Time series
  Titulo:   "Lag de pago"

Guardar dashboard como: "Pipeline Kafka — pedido -> pago"


------------------------------------------------------------
8.9 — ALERTAS PROPUESTAS
------------------------------------------------------------

Tabla minima:

| Situacion             | Regla                                            | Accion                            |
|-----------------------|--------------------------------------------------|-----------------------------------|
| Kafka no visible      | kafka_brokers < 1                                | Revisar contenedor kafka          |
| Exporter caido        | up{job="kafka-exporter-dev"} == 0                | Revisar exporter y red docker     |
| Lag alto en pago      | kafka_consumergroup_lag{consumergroup="pago-group"} > 100 | Revisar pago, particiones, carga |
| pago caido            | up{job="pago-dev"} == 0                          | Revisar contenedor o proceso      |
| Pedidos sin pago tras N seg | (kafka_consumergroup_lag > 0 por 2m)        | Revisar consumer pago             |

Como crearlas en Grafana:
  1. Alerting -> Alert rules -> New alert rule
  2. Datasource: Prometheus
  3. Pegar la consulta
  4. Threshold: IS ABOVE 0 (lag) o IS BELOW 1 (up)
  5. Evaluate every 30s for 1m
  6. Save


------------------------------------------------------------
COMANDOS UTILES KAFKA
------------------------------------------------------------

# Listar topics
docker compose -f docker-compose-dev.yml exec kafka /opt/kafka/bin/kafka-topics.sh --list --bootstrap-server kafka:9092

# Describir orden-eventos (particiones, replicas)
docker compose -f docker-compose-dev.yml exec kafka /opt/kafka/bin/kafka-topics.sh --describe --topic orden-eventos --bootstrap-server kafka:9092

# Consumer (leer todos los mensajes desde el inicio)
docker compose -f docker-compose-dev.yml exec kafka /opt/kafka/bin/kafka-console-consumer.sh --topic orden-eventos --bootstrap-server kafka:9092 --from-beginning

# Listar consumer groups
docker compose -f docker-compose-dev.yml exec kafka /opt/kafka/bin/kafka-consumer-groups.sh --list --bootstrap-server kafka:9092

# Describir el grupo de pago (lag, offset, asignacion)
docker compose -f docker-compose-dev.yml exec kafka /opt/kafka/bin/kafka-consumer-groups.sh --describe --group pago-group --bootstrap-server kafka:9092

# Kafka UI (interfaz web — recomendada)
http://localhost:41085
  Topics -> orden-eventos -> Messages
  Consumers -> pago-group     (lag, offset, asignacion)
  Brokers                      (salud del broker)


------------------------------------------------------------
EVIDENCIAS A ENTREGAR (sustentacion)
------------------------------------------------------------

  [ ] Captura del endpoint http://localhost:41308/metrics
  [ ] Captura de http://localhost:19090/targets con kafka-exporter-dev UP
  [ ] Captura de queries en Grafana Explore:
        kafka_brokers, kafka_broker_info, kafka_consumergroup_lag
  [ ] Captura del dashboard "Pipeline Kafka — pedido -> pago"
  [ ] Captura del Kafka UI con mensajes en orden-eventos
  [ ] Captura de logs de pedido en Loki (producer)
  [ ] Captura de logs de pago en Loki (consumer)
  [ ] Tabla de latencia min/promedio/max
  [ ] Tabla de throughput (eventos/seg)
  [ ] Lista de alertas propuestas con umbrales


============================================================
CHECKLIST DE PRUEBAS (para sustentacion)
============================================================

OBSERVABILITY — METRICAS
  [ ] http://localhost:19090/targets   -> los 6 servicios UP
  [ ] Query "up" devuelve 1 para los 6
  [ ] Query rate(http_server_requests_seconds_count[1m]) muestra trafico tras la rafaga
  [ ] Query errores 5xx muestra 0 en estado normal
  [ ] Query latencia promedio funciona
  [ ] Query CPU funciona
  [ ] Query memoria heap funciona

OBSERVABILITY — LOGS
  [ ] {service="gateway"} muestra logs
  [ ] {service="auth"} muestra logs
  [ ] {service="catalogo"} muestra logs
  [ ] {service="producto"} muestra logs
  [ ] {service="pedido"} muestra logs
  [ ] {service="pago"} muestra logs
  [ ] Filtro por texto |= "ERROR" funciona
  [ ] traceId puede seguirse entre servicios

OBSERVABILITY — FALLAS
  [ ] Apagar catalogo -> up=0 + fallback en producto
  [ ] Apagar auth     -> login falla
  [ ] Apagar kafka    -> pedido se crea pero no aparece pago

OBSERVABILITY — ALERTAS
  [ ] Alerta "Catalogo caido" funciona
  [ ] Alerta "Auth caido" funciona
  [ ] Alerta de errores 5xx funciona

OBSERVABILITY — DASHBOARDS
  [ ] Dashboard 4701 importado
  [ ] Dashboard 12900 importado

KAFKA
  [ ] docker ps muestra kafka, kafka-ui, kafka-exporter UP
  [ ] kafka-topics --list muestra "orden-eventos"
  [ ] Crear pedido -> mensaje aparece en consumer console
  [ ] Crear pedido -> aparece pago PENDIENTE automaticamente
  [ ] Logs de pago muestran "Evento OrdenCreada recibido"
  [ ] Kafka UI (41085) abre y muestra el topic
  [ ] Consumer group pago-group con lag 0


============================================================
CIERRE CONCEPTUAL
============================================================

  "Las metricas me dicen QUE algo paso."
  "Los logs me ayudan a entender POR QUE paso."
  "Las alertas me avisan CUANDO debo mirar."
  "Kafka me permite que los servicios se comuniquen SIN bloquearse."


============================================================
ATAJOS UTILES
============================================================

# Ver todos los contenedores
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

# Apagar observability
cd D:\ms1\ProyectoMS2026\observability
docker compose -f docker-compose-dev.yml down

# Apagar kafka
cd ..\kafka
docker compose -f docker-compose-dev.yml down

# Limpiar variables PowerShell
Remove-Variable body, res, response, token, h, pedido, nuevo, p -ErrorAction SilentlyContinue
Clear-Host


KAFKA
___________________________________________________________________
docker compose -f docker-compose-dev.yml up 


