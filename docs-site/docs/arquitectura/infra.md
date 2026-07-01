# Infraestructura

Módulo `infra/` con los componentes base del sistema distribuido.

## Componentes

| Carpeta | Rol | Puerto DEV |
|---|---|---:|
| `config-server/` | Configuración centralizada | 7071 |
| `registry-server/` | Eureka (registro/descubrimiento) | 7081 |
| `gateway/` | API Gateway + JWT + rutas | 7091 |
| `config-repo/` | YAML externos por servicio | — |

## Red Docker

```text
ms-net
```

Los servicios en producción se conectan a esta red para resolver `config-server` y `registry-server` por nombre.

## Rutas del Gateway

- `/auth/**` → `lb://auth`
- `/api/v1/categorias/**` → `lb://catalogo`
- `/api/v1/productos/**` → `lb://producto`
- `/api/v1/pedidos/**` → `lb://pedido`
- `/api/v1/pagos/**` → `lb://pago`

## Arranque DEV

```powershell
cd infra/config-server
mvn spring-boot:run

cd ..\registry-server
mvn spring-boot:run

cd ..\gateway
mvn spring-boot:run
```
