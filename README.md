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
- **ADMIN**: revisa y verifica contadores, resuelve disputas y ve el panel de datos (ver "Revisión del administrador").

## Novedades
- Página de inicio pública (`/`) y preguntas frecuentes (`/ayuda`).
- 11 tipos de trámite con año gravable, urgencia, presupuesto y situación del cliente.
- El cliente puede editar, cancelar (queda en historial) o eliminar solicitudes abiertas.
- El contador puede editar o retirar sus ofertas mientras estén pendientes.
- Notificaciones en la campana del menú y en `/notificaciones`.
- Historial de acuerdos en `/acuerdos` y paneles con tarjetas de resumen.

## Revisión del administrador: verificación de contadores y disputas

Las acciones importantes del administrador siguen siempre el mismo camino: **ver → revisar → decidir → confirmar → registrar**.
Ninguna se ejecuta con un solo clic: cada decisión abre un modal con una casilla de confirmación obligatoria, el servidor vuelve a validar todo y el resultado queda registrado (quién, cuándo y por qué).

### Verificación de contadores
- Estados (`EstadoVerificacion`): `PENDIENTE`, `CORRECCION_SOLICITADA`, `APROBADO`, `RECHAZADO`. Las personas y el administrador no tienen estado.
- `verificado` sigue existiendo y se sincroniza: solo es `true` cuando el estado es `APROBADO` (por eso siguen funcionando las reglas de ofertas, mentores y practicantes).
- **Registro**: los contadores públicos y practicantes adjuntan el soporte de su tarjeta profesional (obligatorio) y su documento de identidad (opcional): PDF, JPG o PNG de máximo 10 MB, guardados en `uploads/verificaciones` con nombre aleatorio. La solicitud queda `PENDIENTE` y se notifica a los administradores.
- **Revisión** (`/admin/verificaciones/{id}`): datos del contador, documentos (se abren en línea), observaciones y motivos previos, quién revisó y cuándo. Si está `PENDIENTE` se puede:
  - **Aprobar** (observación opcional). Requiere que exista el soporte de la tarjeta profesional.
  - **Rechazar** (motivo obligatorio de 10 a 500 caracteres y observación opcional).
  - **Solicitar corrección** (observación obligatoria con lo que debe corregir).
- El contador ve el aviso en su panel y en `/perfil` puede subir un soporte nuevo: la solicitud vuelve a `PENDIENTE` con fecha nueva y se avisa al administrador.
- Los soportes solo los ven el administrador y el propio contador. En la edición de usuarios el estado de verificación es de solo lectura.
- Al arrancar, los contadores que existían antes de este cambio quedan `APROBADO` si ya estaban verificados y `PENDIENTE` si no (sin inventar fechas ni revisores).

### Resolución de disputas
- Una disputa está **activa** mientras está `ABIERTA` o `EN_REVISION`. Mientras está activa, el administrador puede ver el chat y las partes pueden seguir escribiendo en él, aunque el acuerdo esté finalizado.
- **Detalle** (`/admin/disputas/{id}`): resumen, partes, acuerdo (servicio, año gravable, urgencia, valor, comisión y fecha de inicio), conversación en solo lectura, evidencias e historial. Abrir el detalle no cambia el estado. El modelo no tiene fecha límite, así que se muestra "No registrada" junto con la urgencia de la solicitud.
- **Mensajes**: el administrador escribe al cliente o al contador (solicitud de información o mensaje). El texto se publica en el chat del acuerdo con el prefijo `[Administración → Cliente]` o `[Administración → Contador]`, se notifica al destinatario y queda en el historial. Si la disputa estaba `ABIERTA`, pasa a `EN_REVISION`.
- **Resolución** ("Tomar decisión"): decisión (a favor del cliente, a favor del contador o cerrar sin sanción), justificación de 20 a 1000 caracteres y confirmación. Las decisiones a favor de una parte dejan la disputa `RESUELTA` y "sin sanción" la deja `CERRADA`. Se notifica a ambas partes. **No modifica el acuerdo ni ningún pago**: la plataforma no maneja reembolsos.
- Las disputas resueltas o cerradas se consultan en `/admin/disputas/cerradas`, en solo lectura y sin acceso al chat (política de datos).
- Cada acción (apertura, mensajes y resolución) queda en la tabla `historial_disputas` con el estado anterior, el estado nuevo, la decisión y el detalle.

### Rutas nuevas
| Método | Ruta | Uso |
|---|---|---|
| GET | `/admin/verificaciones/{id}` | Revisión de un contador |
| GET | `/admin/verificaciones/{id}/documentos/{tarjeta\|identidad}` | Ver el soporte en línea |
| POST | `/admin/verificaciones/{id}/aprobar` · `/rechazar` · `/correccion` | Decisiones (con confirmación) |
| GET | `/admin/disputas/{id}` | Detalle de una disputa |
| POST | `/admin/disputas/{id}/mensaje` | Mensaje o solicitud de información a una parte |
| POST | `/admin/disputas/{id}/resolver` | Resolución (con confirmación) |
| GET | `/admin/disputas/cerradas` | Historial de disputas cerradas |
| POST | `/perfil/verificacion` | El contador envía un soporte nuevo |
| GET | `/perfil/verificacion/documentos/{tarjeta\|identidad}` | El contador ve su propio soporte |

Se eliminó `POST /admin/usuarios/{id}/verificar`: ya no se puede aprobar sin pasar por la revisión.

### Cómo probarlo
1. **Verificación**: crea una cuenta en `/registro` como contador público, adjuntando un PDF como soporte. Entra como `admin@declaraya.com` / `admin123`: el contador aparece en "Contadores pendientes de verificación". Pulsa **Revisar**, abre el soporte y prueba "Solicitar corrección". Entra con la cuenta del contador: verás el aviso en el panel y en **Mi perfil** podrás subir un soporte nuevo. Vuelve como admin y aprueba. El contador ya puede ofertar.
   - Las cuentas de ejemplo `natalia.quintero@example.com` y `esteban.arias@example.com` (clave `demo1234`) están pendientes y sin soporte: el admin no puede aprobarlas hasta que suban uno desde su perfil.
2. **Disputas**: como admin, en "Disputas activas" pulsa **Revisar disputa** sobre la disputa de ejemplo. Envía una solicitud de información al contador (la disputa pasa a `EN_REVISION` y el mensaje aparece en el chat). Luego pulsa **Tomar decisión**: el botón de confirmar se habilita solo con decisión, justificación de al menos 20 caracteres y la casilla marcada. Al confirmar, la disputa pasa a "Disputas cerradas" con su decisión y su historial.
3. **Pruebas automáticas**: `mvnw.cmd test`. `VerificacionServiceTest` y `DisputaServiceTest` son unitarias (Mockito, sin base de datos). `DeclarayaApplicationTests` necesita MySQL arriba (`docker compose up -d`).

## Mi perfil y Mis documentos

### Mi perfil (`/perfil`)
Las secciones cambian según el rol:
- **Persona**: información personal (foto, nombre, tipo y número de documento), contacto (correo en solo lectura, teléfono y ciudad), y cuenta y seguridad (rol y cambio de contraseña).
- **Contador público y practicante**: lo mismo, más:
  - **Información profesional**: tipo de contador, TP en solo lectura, descripción y tarifa base.
  - **Mentor asignado**: solo el practicante.
  - **Verificación**: estado, fechas, observación y soportes, con el botón "Ver".
    - Si la cuenta está **aprobada**, los soportes no se reemplazan. El botón **Solicitar nueva verificación** abre un modal con confirmación, admite un número de TP nuevo y deja la cuenta `PENDIENTE` y sin verificar. Se avisa al administrador.
    - En los demás estados se puede subir un soporte nuevo.
- **Administrador**: nombre, correo en solo lectura, foto, rol y cambio de contraseña.

`POST /perfil/datos` enlaza solo los campos de `PerfilDTO`, validados con Bean Validation, y `UsuarioService.actualizarPerfil` aplica únicamente los que corresponden al rol. Aunque se envíen `rol`, `email`, `tarjetaProfesional`, `verificado` o `estadoVerificacion`, se ignoran.
`POST /perfil/contrasena` exige la contraseña actual, una nueva de mínimo 8 caracteres y su confirmación, y la guarda con BCrypt.

### Mis documentos (`/cliente/documentos`, solo clientes)
- El cliente sube documentos (PDF, JPG o PNG de máximo 10 MB) con tipo, nombre y, si quiere, una solicitud propia. Se guardan en `uploads/documentos` con nombre aleatorio: la ruta del disco nunca se muestra.
- Se agrupan en **personales** (identidad, RUT, otro), **tributarios** (certificado, extracto, declaración anterior, otro) y **de solicitudes** (los que tienen solicitud asociada).
- Acciones: ver en línea, descargar, reemplazar el archivo, cambiar la solicitud asociada y eliminar (con confirmación en un modal; también borra el archivo).
- Si la solicitud asociada ya está `EN_PROCESO` o `CERRADA`, el documento queda **bloqueado** ("Bloqueado: en uso en un acuerdo"): se puede ver y descargar, pero no reemplazar, desvincular ni eliminar. El cliente puede subir uno nuevo.
- La relación vive solo en `documentos_cliente.solicitud_id`, así que "Mis documentos" y el detalle de la solicitud muestran siempre lo mismo. Si se elimina una solicitud abierta, sus documentos no se borran: vuelven a "Mis documentos" sin solicitud.

### Documentos dentro de la solicitud (`/solicitudes/{id}`)
La sección "Documentos de esta solicitud" la ven:
- **El cliente dueño**, que además puede subir documentos ya vinculados a la solicitud.
- **El contador del acuerdo y el mentor del practicante**, solo para ver y descargar.

Los demás contadores (incluidos los que ofertaron sin ganar) no la ven.

### Permisos
Un único método, `DocumentoClienteService.puedeVer`, decide el acceso en todas las rutas de ver y descargar. Permite al propietario, al contador o mentor de un acuerdo de la solicitud vinculada, y al admin solo con una disputa activa sobre ese acuerdo. En cualquier otro caso responde **403**.
- `/cliente/**` sigue siendo solo para `ROLE_PERSONA`.
- La ruta compartida `/documentos/{id}` exige sesión y pasa por `puedeVer`.

### Rutas nuevas
| Método | Ruta | Uso |
|---|---|---|
| POST | `/perfil/datos` | Guardar datos del perfil (solo campos permitidos) |
| POST | `/perfil/contrasena` | Cambiar contraseña |
| POST | `/perfil/verificacion/nueva` | Contador aprobado pide una nueva verificación |
| GET | `/cliente/documentos` | Mis documentos |
| POST | `/cliente/documentos` | Subir documento (multipart) |
| GET | `/cliente/documentos/{id}` · `/descargar` | Ver en línea / descargar (propietario) |
| POST | `/cliente/documentos/{id}/reemplazar` · `/eliminar` · `/vincular` | Acciones del propietario |
| GET | `/documentos/{id}` · `/descargar` | Ver / descargar para contador, mentor o admin con disputa |

### Tablas y campos nuevos
- `usuarios`: `tipo_documento` (CC, CE, PASAPORTE, PPT), `numero_documento` (20), `telefono` (20), `ciudad` (60) y `descripcion_profesional` (500). Todos son opcionales.
- `documentos_cliente`: `id`, `propietario_id`, `nombre` (120), `tipo`, `categoria`, `nombre_original`, `archivo`, `fecha_carga` y `solicitud_id` (puede ser nulo).

### Cómo probarlo
1. Entra como un cliente de ejemplo, por ejemplo `brayan.steven@example.com` / `demo1234`. En **Mi perfil**, edita el teléfono y la ciudad, y cambia la contraseña.
2. En **Mis documentos**, pulsa **Subir documento**, elige "RUT", adjunta un PDF y ábrelo con **Ver**.
3. Pulsa **Solicitud** en el documento y asócialo a una de tus solicitudes. Si está en proceso, verás "Bloqueado: en uso en un acuerdo".
4. Desde **Mis solicitudes**, abre esa solicitud con **Ver detalle**: el documento aparece en "Documentos de esta solicitud".
5. Entra con el contador del acuerdo (aparece en el chat del acuerdo): verá y podrá descargar el documento. Con otro contador la sección no aparece y `/documentos/{id}` responde 403.

## Flujo del acuerdo: etapas, documentos, entregas y calificaciones

### Fecha de entrega y etapas
- **Fecha estimada de entrega:** el contador la indica al ofertar o al editar su oferta. Es obligatoria, debe ser futura y no superar 120 días. El cliente la ve en cada oferta para comparar; en los acuerdos antiguos aparece "No registrada". El admin la ve como "fecha límite" en la disputa.
- **Etapas (`EtapaAcuerdo`):** complementan a `EstadoAcuerdo` (ACTIVO/FINALIZADO), no lo reemplazan. Solo cambian en `FlujoAcuerdoService.cambiarEtapa`, que rechaza cualquier paso no permitido:

| Desde | Hacia | Qué lo provoca |
|---|---|---|
| (creación) | APROBACION_MENTOR / DOCUMENTACION | Se acepta la oferta de un practicante o de un contador público |
| APROBACION_MENTOR | DOCUMENTACION | El mentor aprueba el caso |
| DOCUMENTACION | PREPARACION | El contador pulsa "Iniciar preparación". Se bloquea si hay documentos solicitados pendientes; si faltan requisitos obligatorios del catálogo, solo se advierte |
| PREPARACION | REVISION_MENTOR / ENTREGADO | Entrega un practicante (pasa por el mentor) o un contador público (va directo al cliente) |
| REVISION_MENTOR | ENTREGADO / PREPARACION | El mentor aprueba o pide corrección |
| ENTREGADO | FINALIZADO / PREPARACION | El cliente confirma la recepción o reporta un inconveniente |

- **Migración al arrancar:** los acuerdos sin etapa quedan en FINALIZADO si ya terminaron, en APROBACION_MENTOR si su supervisión sigue pendiente, y en DOCUMENTACION en los demás casos.
- **Historial (`actividades_acuerdo`):** registra automáticamente la creación del acuerdo y la aprobación del mentor, los cambios de etapa, los documentos subidos, solicitados y entregados, las entregas y su revisión, la recepción, los inconvenientes, las disputas, los recordatorios y la calificación. No se edita ni se borra.
- **Detalle `/acuerdos/{id}`:** datos del acuerdo (con los días que faltan para la entrega), indicador de etapas (para contadores públicos se omiten las del mentor), lista de verificación, documentos, entregas, calificación e historial, más los botones que correspondan a cada rol y etapa. Lo ven los participantes, el mentor y el admin con una disputa activa; los demás reciben 403.

### Requisitos documentales
- **`CatalogoRequisitos`:** define los requisitos de cada trámite (renta, IVA, retención, ICA y RUT; el resto usa una lista general).
- **Requisito de cada documento:** al subir un documento vinculado a una solicitud, el cliente indica qué requisito cumple (`documentos_cliente.requisito`). La lista de verificación del detalle de la solicitud y del acuerdo muestra cada requisito como Entregado o Pendiente.
- **Documentos adicionales:** el contador del acuerdo o su mentor pueden pedirlos en DOCUMENTACION o PREPARACION (`requerimientos_documento`). El cliente los cumple con "Subir": el pedido queda ENTREGADO y se avisa al contador.

### Entregas
- **Entrega (`entregas_trabajo`):** descripción más archivo PDF, JPG o PNG de máximo 10 MB, guardado en `uploads/entregas` con nombre aleatorio.
  - La de un practicante queda en revisión del mentor, que puede aprobarla o pedir corrección con una observación obligatoria.
  - El panel del contador público muestra "Entregas por revisar" de sus practicantes.
- **Respuesta del cliente:** puede **confirmar la recepción**, lo que finaliza el acuerdo y cierra la solicitud, o **reportar un inconveniente**, que devuelve el acuerdo a preparación y le ofrece abrir una disputa. El modal aclara que confirmar la recepción no equivale a la aceptación del trámite por parte de la DIAN.
- **"Finalizar acuerdo" del chat:** solo aparece si hay una entrega enviada, y hace lo mismo que confirmar la recepción.
- **Archivos de entrega:** los ven los participantes, el mentor y el admin con disputa activa. El cliente no ve los borradores que todavía revisa el mentor.

### Perfil público, calificaciones y notificaciones
- **Mi perfil (contador):** se agregan especialidades (trámites del catálogo) y años de experiencia (0 a 60).
- **Perfil público `/contadores/{id}`:** foto, tipo, insignia "Verificado por DeclaraYa" (solo si está aprobado), TP, mentor, especialidades, experiencia, descripción, tarifa, promedio, acuerdos finalizados y últimas reseñas. Nunca muestra correo, teléfono, documento ni archivos. Se enlaza desde las ofertas y desde el detalle del acuerdo.
- **Calificación (`calificaciones`, una por acuerdo):** solo la persona del acuerdo califica, una vez y con el acuerdo finalizado. El admin, en `/admin/calificaciones`, puede ocultar un comentario con un motivo obligatorio; la calificación no se borra y deja de contar en el promedio.
- **Notificaciones:**
  - Se envían cuando se crea un acuerdo (a ambas partes), cuando se pide o se sube un documento, cuando hay una entrega enviada o por revisar, cuando se pide una corrección, cuando se confirma la recepción o se reporta un inconveniente, cuando se abre o resuelve una disputa y cuando llega una nueva calificación.
  - `/notificaciones` tiene los filtros Pendientes y Leídas.
  - `/notificaciones/{id}/abrir` comprueba que la notificación sea del usuario, la marca leída y solo redirige a rutas internas.
- **Recordatorio diario (8:00, hora de Bogotá):** avisa a ambas partes cuando faltan 2 días o menos para la fecha estimada de un acuerdo no entregado. Se envía una sola vez por acuerdo (`acuerdos.recordatorio_enviado`).

### Rutas nuevas
| Método | Ruta | Quién |
|---|---|---|
| GET | `/acuerdos/{id}` | Participantes, mentor, admin con disputa activa |
| POST | `/acuerdos/{id}/requerimientos` | Contador del acuerdo o mentor |
| POST | `/acuerdos/{id}/preparacion` | Contador del acuerdo |
| POST | `/acuerdos/{id}/entregas` | Contador del acuerdo (multipart) |
| POST | `/acuerdos/{id}/entregas/{e}/aprobar` · `/correccion` | Mentor |
| POST | `/acuerdos/{id}/entregas/{e}/recepcion` · `/inconveniente` | Cliente |
| GET | `/acuerdos/{id}/entregas/{e}` · `/descargar` | Participantes, mentor, admin con disputa |
| POST | `/acuerdos/{id}/calificacion` | Cliente (acuerdo finalizado) |
| GET | `/contadores/{id}` | Usuarios autenticados (solo perfiles de contadores) |
| GET / POST | `/admin/calificaciones` · `/admin/calificaciones/{id}/ocultar` | Admin |
| GET | `/notificaciones?filtro=pendientes\|leidas` · `/notificaciones/{id}/abrir` | Dueño de la notificación |

### Tablas y campos nuevos
- **`ofertas`:** `fecha_entrega_estimada`.
- **`acuerdos`:** `etapa` y `recordatorio_enviado`.
- **`usuarios`:** `especialidades` y `anios_experiencia`.
- **`documentos_cliente`:** `requisito`.
- **Tablas nuevas:** `actividades_acuerdo`, `requerimientos_documento`, `entregas_trabajo` y `calificaciones` (con restricción única en `acuerdo_id`).

### Cómo probarlo (clave `demo1234`)
1. **Contador público:** como `brayan.steven@example.com`, crea una solicitud de "Declaración de renta". Como `andres.felipe@example.com`, oferta con una fecha estimada. Vuelve como cliente: acepta la oferta, abre **Ver detalle del acuerdo** y sube el RUT con **Subir** en la lista de verificación.
2. Como contador, pulsa **Solicitar documento**. Como cliente, súbelo desde "Documentos que pidió el contador". Como contador, pulsa **Iniciar preparación** y luego **Entregar trabajo**.
3. Como cliente, pulsa **Confirmar recepción** y después **Calificar contador**. La reseña aparece en `/contadores/{id}`.
4. **Practicante:** repite el flujo ofertando con `camila.herrera@example.com`. Su mentor `mauricio.salazar@example.com` aprueba el caso, pide una corrección de la entrega (el practicante la ve y vuelve a entregar) y luego la aprueba.
5. **Admin:** como `admin@declaraya.com`, en **Calificaciones** pulsa "Ocultar comentario" con un motivo.

## Datos de ejemplo y Power BI
- Al arrancar por primera vez se cargan **datos ficticios** (10 contadores públicos, 8 practicantes, 16 clientes, 48 solicitudes con ofertas y acuerdos repartidos en 2026). Todas las cuentas de ejemplo usan la clave `demo1234` (por ejemplo `mauricio.salazar@example.com`). Para desactivarlos: `declaraya.demo-data=false`.
- El administrador (`admin@declaraya.com`) puede crear, editar, verificar y eliminar usuarios (`/admin/usuarios`) y cambiar o eliminar solicitudes (`/admin/solicitudes`).
- Se cargan también **cifras públicas de la DIAN** (recaudo, declarantes y UVT) en la tabla `indicadores_dian`.
- En `/admin/datos` hay botones para exportar CSV (acuerdos, solicitudes, ofertas, usuarios e indicadores DIAN) y cargarlos en Power BI.
- Power BI también puede conectarse directo a MySQL: servidor `localhost:3307`, base `declaraya_db`, usuario `root`, clave `root`.
