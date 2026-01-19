# Sistema de Gestión de Pedidos

Sistema automatizado para gestionar el ciclo completo de pedidos desde la recepción por email hasta el cobro y cálculo de comisiones.

## Características

### 🚀 Funcionalidades Principales

1. **Recepción Automática de Pedidos**
   - Lectura automática de emails de Gmail cada 10 minutos
   - Extracción de información de tablas HTML (productos, cantidades, precios)
   - Detección automática de fecha y lugar de entrega
   - Creación automática de pedidos en el sistema

2. **Integración con Google Calendar**
   - Creación automática de eventos para fechas de entrega
   - Recordatorios configurables (48 horas antes de la entrega)
   - Sincronización bidireccional

3. **Gestión de Estados del Pedido**
   - RECIBIDO: Pedido recibido por email
   - CALENDARIO_CREADO: Agregado al calendario
   - ENVIADO_PLANTA: Enviado a la planta
   - RECORDATORIO_PLANTA_ENVIADO: Recordatorio enviado si no hay respuesta
   - EN_FABRICACION: Confirmado por la planta
   - RECORDATORIO_LOGISTICA_ENVIADO: Recordatorio 48hs antes
   - TURNO_SOLICITADO: Turno solicitado a BlancaLuna
   - TURNO_CONFIRMADO: Turno confirmado
   - PREPARADO_ENTREGA: Remito y Etiqueta RNPA listos
   - ENTREGADO: Pedido entregado
   - DOCUMENTOS_RECIBIDOS: Remito y factura sellados
   - FACTURA_DADA_ALTA: Factura dada de alta
   - ECHECK_PENDIENTE: Esperando e-check (30 días)
   - ECHECK_RECIBIDO: E-check recibido
   - COBRADO: Cobrado (60 días desde entrega)
   - COMISION_CALCULADA: Comisión calculada (8%)
   - FINALIZADO: Proceso completado

4. **Notificaciones Automáticas**
   - Envío automático a la planta al recibir pedido
   - Recordatorio a planta si no hay respuesta en 24 horas
   - Recordatorio de logística 48 horas antes de entrega
   - Solicitud de turno a BlancaLuna

5. **Gestión de Documentación**
   - Control de Remito
   - Control de Etiqueta RNPA
   - Seguimiento de documentos sellados

6. **Control Financiero**
   - Cálculo automático de fecha de e-check (30 días desde entrega)
   - Cálculo automático de fecha de cobro (60 días desde entrega)
   - Cálculo automático de comisión del 8%
   - Reportes mensuales de ventas y comisiones

7. **Reportes y Estadísticas**
   - Vista de pedidos por mes y año
   - Estadísticas de estados
   - Gráficos de ventas
   - Reporte de comisiones mensual y anual

## 🛠️ Tecnologías Utilizadas

- **Backend**: Spring Boot 3.2.1
- **Base de Datos**: H2 (desarrollo) / PostgreSQL (producción)
- **Frontend**: Thymeleaf, Bootstrap 5, jQuery
- **APIs**: Gmail API, Google Calendar API
- **Seguridad**: Spring Security
- **Procesamiento HTML**: JSoup
- **Tareas Programadas**: Spring Scheduler

## 📋 Requisitos Previos

1. Java 21 o superior
2. Maven 3.6+
3. Cuenta de Google con Gmail y Calendar habilitados
4. Credenciales OAuth 2.0 de Google Cloud Console

## 🔧 Configuración

### 1. Configurar Google Cloud Console

1. Ve a [Google Cloud Console](https://console.cloud.google.com/)
2. Crea un nuevo proyecto
3. Habilita las APIs:
   - Gmail API
   - Google Calendar API
4. Crea credenciales OAuth 2.0
5. Descarga el archivo `credentials.json`
6. Coloca `credentials.json` en la raíz del proyecto

### 2. Configurar la Aplicación

Edita `src/main/resources/application.properties`:

```properties
# Emails de contacto
app.email.planta=tu-planta@ejemplo.com
app.email.blancaluna=blancaluna@ejemplo.com

# Para producción, usar PostgreSQL
spring.datasource.url=jdbc:postgresql://localhost:5432/pedidos_db
spring.datasource.username=tu_usuario
spring.datasource.password=tu_password
```

### 3. Compilar y Ejecutar

```bash
mvn clean install
mvn spring-boot:run
```

La aplicación estará disponible en: `http://localhost:8080`

**Usuario por defecto**: admin  
**Contraseña**: admin123

## 📧 Formato del Email de Pedido

El sistema espera emails con el siguiente formato:

```
Asunto: Pedido [número o descripción]

Fecha de Entrega: DD/MM/YYYY
Lugar de Entrega: Dirección completa

[Tabla HTML con 5 columnas]
Producto | Cantidad | Precio Unitario | Subtotal | ...
---------|----------|-----------------|----------|----
Producto 1 | 10 | 100.00 | 1000.00 | ...
Producto 2 | 5 | 50.00 | 250.00 | ...
```

## 🔄 Tareas Automáticas

- **Cada 10 minutos**: Procesa nuevos emails de pedidos
- **Cada hora**: Verifica pedidos sin respuesta de planta (envía recordatorio después de 24 horas)
- **Cada 6 horas**: Verifica entregas próximas (envía recordatorio 48 horas antes)
- **Diariamente a las 9:00**: Verifica pedidos esperando e-check
- **Diariamente a las 8:00**: Genera reporte diario de estados

## 📱 Endpoints API

### Pedidos
- `GET /api/pedidos` - Listar todos los pedidos
- `GET /api/pedidos/{id}` - Obtener pedido por ID
- `GET /api/pedidos/mes/{mes}/anio/{anio}` - Pedidos por mes
- `GET /api/pedidos/anio/{anio}` - Pedidos por año
- `POST /api/pedidos` - Crear pedido manual
- `POST /api/pedidos/{id}/calendario` - Agregar al calendario
- `POST /api/pedidos/{id}/enviar-planta` - Enviar a planta
- `PUT /api/pedidos/{id}/fabricacion` - Marcar en fabricación
- `POST /api/pedidos/{id}/solicitar-turno` - Solicitar turno BlancaLuna
- `PUT /api/pedidos/{id}/confirmar-turno` - Confirmar turno
- `PUT /api/pedidos/{id}/preparado-entrega` - Marcar preparado
- `PUT /api/pedidos/{id}/entregado` - Marcar entregado
- `PUT /api/pedidos/{id}/documentos-recibidos` - Documentos recibidos
- `PUT /api/pedidos/{id}/factura-alta` - Factura dada de alta
- `PUT /api/pedidos/{id}/echeck-recibido` - E-check recibido
- `PUT /api/pedidos/{id}/cobrado` - Marcar cobrado
- `PUT /api/pedidos/{id}/finalizar` - Finalizar pedido

## 🗄️ Base de Datos

El sistema utiliza JPA/Hibernate con dos entidades principales:

- **Pedido**: Información del pedido, estado, fechas, montos
- **ItemPedido**: Productos del pedido con cantidad y precios

## 🚀 Despliegue en Producción

### Opción 1: Servidor Propio

```bash
# Compilar JAR
mvn clean package

# Ejecutar
java -jar target/ingreso-pedidos-1.0-SNAPSHOT.jar
```

### Opción 2: Docker

```dockerfile
FROM openjdk:21-jdk-slim
COPY target/ingreso-pedidos-1.0-SNAPSHOT.jar app.jar
COPY credentials.json /credentials.json
ENTRYPOINT ["java","-jar","/app.jar"]
```

### Opción 3: Cloud (Heroku, AWS, Google Cloud)

Configurar variables de entorno y credenciales según la plataforma.

## 📝 Notas Importantes

1. **Primera Ejecución**: Al ejecutar por primera vez, se abrirá un navegador para autorizar el acceso a Gmail y Calendar. Esto creará el archivo `tokens/StoredCredential`.

2. **Credenciales**: Nunca subas `credentials.json` o `tokens/` a repositorios públicos.

3. **Base de Datos**: En desarrollo se usa H2 (archivo). Para producción, configurar PostgreSQL.

4. **Seguridad**: Cambiar las credenciales de usuario por defecto en producción.

5. **Zona Horaria**: El sistema está configurado para Argentina (America/Argentina/Buenos_Aires). Ajustar según tu ubicación.

## 🐛 Solución de Problemas

### Error de autenticación con Google
- Verificar que `credentials.json` esté en la raíz del proyecto
- Eliminar la carpeta `tokens/` y volver a autorizar

### No se procesan los emails
- Verificar que el asunto contenga "pedido"
- Revisar los logs para ver errores de parseo
- Verificar que el formato del email sea correcto

### No se crean eventos en Calendar
- Verificar la autorización de Google Calendar API
- Revisar permisos en Google Cloud Console

## 📄 Licencia

Este proyecto es propietario. Todos los derechos reservados.

## 👤 Autor

Sistema desarrollado para gestión de pedidos empresariales.

## 📞 Soporte

Para soporte o consultas, contactar al administrador del sistema.

