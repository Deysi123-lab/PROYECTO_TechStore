# Pruebas del proyecto

## Health checks

```text
http://localhost:7071/actuator/health   # Config Server
http://localhost:7081/                  # Eureka
http://localhost:7091/actuator/health   # Gateway
http://localhost:8041/actuator/health   # Auth
http://localhost:8081/actuator/health   # Catálogo
http://localhost:9091/actuator/health   # Producto
http://localhost:9101/actuator/health   # Pedido
http://localhost:9111/actuator/health   # Pago
```

## Login y flujo JWT

```powershell
$body = @{ username = "admin"; password = "admin123" } | ConvertTo-Json
$res  = Invoke-RestMethod -Method Post `
         -Uri "http://localhost:7091/auth/login" `
         -ContentType "application/json" -Body $body
$token = $res.accessToken

Invoke-RestMethod -Method Get `
   -Uri "http://localhost:7091/api/v1/productos" `
   -Headers @{ Authorization = "Bearer $token" }
```

## Flujo pedido → pago

```powershell
$pedido = @{
  userId = 1
  cliente = "admin"
  estado = "PENDIENTE"
  direccionEnvio = "Lima, Perú"
  items = @(@{ productoId = 1; cantidad = 1 })
} | ConvertTo-Json -Depth 5

Invoke-RestMethod -Method Post `
   -Uri "http://localhost:7091/api/v1/pedidos" `
   -Headers @{ Authorization = "Bearer $token" } `
   -ContentType "application/json" -Body $pedido

Invoke-RestMethod -Method Get `
   -Uri "http://localhost:7091/api/v1/pagos" `
   -Headers @{ Authorization = "Bearer $token" }
```

## Swagger por servicio

- Auth: http://localhost:8041/swagger-ui/index.html
- Catálogo: http://localhost:8081/swagger-ui/index.html
- Producto: http://localhost:9091/swagger-ui/index.html
- Pedido: http://localhost:9101/swagger-ui/index.html
- Pago: http://localhost:9111/swagger-ui/index.html

Para más detalle, revisa `PRUEBAS.md` en la raíz del repositorio.
