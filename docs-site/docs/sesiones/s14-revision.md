# S14 - Revisión técnica

## Objetivo

Estabilizar documentación, despliegue local y evidencias del proyecto TechStore.

## Documentación del repositorio

| Archivo / carpeta | Contenido |
|---|---|
| `Guia.txt` | Arranque DEV/PROD |
| `PRUEBAS.md` | Comandos de prueba |
| `KAFKA_GUIA.md` | Mensajería asíncrona |
| `PROMETHEUS_GUIA.md` | Métricas |
| `GRAFANA_GUIA.md` | Dashboards y logs |
| `docs-site/` | Esta documentación web |

## Estructura esperada

```text
infra/
kafka/
observability/
services/
e-commerce/
```

## Revisión antes de la defensa

1. Verificar que `docker compose` levanta sin errores.
2. Confirmar que `mvn spring-boot:run` funciona en cada servicio.
3. Actualizar README de cada módulo si hubo cambios.
4. Probar flujo completo con `admin` y `user`.
