# DeclaraYa

Plataforma que conecta personas naturales con contadores públicos y practicantes para la declaración de renta.
Spring Boot 4 · Java 21 · MySQL · Thymeleaf · Spring Security.

## Cómo ejecutar (Docker)

1. Abre Docker Desktop y espera a que diga "Engine running".
2. En la carpeta del proyecto: `docker compose up -d`
3. Luego: `mvnw.cmd spring-boot:run`
4. Abre http://localhost:8080
5. Ver la base en vivo: http://localhost:8081 (Adminer) → Servidor `mysql`, usuario `root`, clave `root`, base `declaraya_db`.

Usuario administrador de prueba: `admin@declaraya.com` / `admin123`.
Apagar: `docker compose down` (conserva datos) o `docker compose down -v` (los borra).

## Roles
- **PERSONA**: publica solicitudes, recibe ofertas, acepta una y chatea con el contador.
- **CONTADOR_PUBLICO**: envía ofertas (requiere verificación del admin) y supervisa practicantes.
- **CONTADOR_PRACTICANTE**: envía ofertas bajo la supervisión de un mentor (un caso activo a la vez).
- **ADMIN**: verifica contadores, resuelve disputas y ve el panel de datos.

## Novedades
- Página de inicio pública (`/`) y preguntas frecuentes (`/ayuda`).
- 11 tipos de trámite con año gravable, urgencia, presupuesto y situación del cliente.
- El cliente puede editar, cancelar (queda en historial) o eliminar solicitudes abiertas.
- El contador puede editar o retirar sus ofertas mientras estén pendientes.
- Notificaciones en la campana del menú y en `/notificaciones`.
- Historial de acuerdos en `/acuerdos` y paneles con tarjetas de resumen.

## Datos de ejemplo y Power BI
- Al arrancar por primera vez se cargan **datos ficticios** (10 contadores públicos, 8 practicantes, 16 clientes, 48 solicitudes con ofertas y acuerdos repartidos en 2026). Todas las cuentas de ejemplo usan la clave `demo1234` (por ejemplo `mauricio.salazar@example.com`). Para desactivarlos: `declaraya.demo-data=false`.
- El administrador (`admin@declaraya.com`) puede crear, editar, verificar y eliminar usuarios (`/admin/usuarios`) y cambiar o eliminar solicitudes (`/admin/solicitudes`).
- Se cargan también **cifras públicas de la DIAN** (recaudo, declarantes y UVT) en la tabla `indicadores_dian`.
- En `/admin/datos` hay botones para exportar CSV (acuerdos, solicitudes, ofertas, usuarios e indicadores DIAN) y cargarlos en Power BI.
- Power BI también puede conectarse directo a MySQL: servidor `localhost:3307`, base `declaraya_db`, usuario `root`, clave `root`.
