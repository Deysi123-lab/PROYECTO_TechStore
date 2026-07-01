# Frontend Angular

Aplicación en `e-commerce/` que consume el backend exclusivamente vía **Gateway**.

## Arranque

```powershell
cd e-commerce
npm install
ng serve
```

URL: http://localhost:4200

## Proxy de desarrollo

El archivo `proxy.conf.json` redirige:

- `/auth` → `http://localhost:7091`
- `/api` → `http://localhost:7091`

## Integración

- Login contra `POST /auth/login`
- Token JWT en header `Authorization: Bearer <token>`
- Rutas protegidas validadas por Gateway y microservicios

## Funcionalidades

- Catálogo de productos
- Carrito y checkout
- Gestión de pedidos y pagos
- Perfil de usuario autenticado
