# Preguntas Frecuentes (FAQ)

## 🚀 Instalación y Configuración

### ¿Qué necesito para ejecutar la aplicación?

- Java 21 o superior
- Maven 3.6+
- Cuenta de Gmail
- Cuenta de Google Calendar
- Credenciales OAuth 2.0 de Google Cloud Console

### ¿Cómo obtengo las credenciales de Google?

Sigue la guía detallada en `CONFIGURACION.md`. En resumen:
1. Ve a Google Cloud Console
2. Crea un proyecto
3. Habilita Gmail API y Google Calendar API
4. Crea credenciales OAuth 2.0 para aplicación de escritorio
5. Descarga el archivo JSON y renómbralo a `credentials.json`

### ¿Dónde coloco el archivo credentials.json?

En la raíz del proyecto:
```
C:\Users\l1008754\IdeaProjects\ingreso-pedidos\credentials.json
```

### ¿Puedo usar una cuenta de Gmail diferente para cada API?

Sí, pero en la primera autorización deberás permitir ambas APIs con la misma cuenta de Gmail que quieres usar para recibir los pedidos.

---

## 📧 Procesamiento de Emails

### ¿Con qué frecuencia se procesan los emails?

El sistema verifica emails nuevos cada 10 minutos automáticamente.

### ¿Puedo forzar el procesamiento inmediato?

Sí, reinicia la aplicación o usa el botón "Procesar Nuevos Emails" en la interfaz web.

### ¿Qué pasa si el email no tiene el formato correcto?

El sistema lo marcará como leído pero no creará un pedido. Revisa los logs para ver el error específico.

### ¿El asunto debe ser exactamente "pedido"?

No, solo debe **contener** la palabra "pedido". Ejemplos válidos:
- "Nuevo Pedido"
- "Pedido #123"
- "PEDIDO URGENTE"
- "Re: Pedido Cliente XYZ"

### ¿Qué formatos de fecha acepta?

- DD/MM/YYYY (25/01/2026)
- DD-MM-YYYY (25-01-2026)
- DD/MM/YY (25/01/26)
- DD-MM-YY (25-01-26)

### ¿La tabla debe tener exactamente 5 columnas?

No, puede tener 3 o más columnas. El sistema busca:
- Columna 1: Producto (texto)
- Columna 2: Cantidad (número)
- Columna 3: Precio Unitario (número)

Las columnas adicionales se ignoran.

### ¿Qué pasa con los emails que ya procesé?

Se marcan como leídos y no se procesan nuevamente.

### ¿Puedo procesar emails de múltiples remitentes?

Sí, el sistema procesa todos los emails no leídos que cumplan con el formato, sin importar el remitente.

---

## 📅 Google Calendar

### ¿Dónde aparecen los eventos?

En tu Google Calendar principal (el de la cuenta que autorizaste).

### ¿Puedo usar un calendario específico?

Sí, pero debes modificar el código en `GoogleCalendarService.java`, línea:
```java
String calendarId = "primary"; // Cambiar por el ID de tu calendario
```

### ¿Los recordatorios se envían automáticamente?

Los recordatorios de Google Calendar se configuran en el evento. Los recordatorios por email los envía la aplicación.

### ¿Puedo desactivar la integración con Calendar?

No es recomendable, pero puedes comentar la línea en `PedidoService.java`:
```java
// pedidoService.agregarACalendario(pedido.getId());
```

---

## 🔄 Flujo de Pedidos

### ¿Cómo cambio el estado de un pedido?

Desde la interfaz web, haz clic en el botón de acción (flecha →) junto al pedido.
O usa la API REST con PUT a `/api/pedidos/{id}/{accion}`.

### ¿Puedo saltar estados?

Técnicamente sí, pero no es recomendable. El flujo está diseñado para seguirse en orden.

### ¿Qué pasa si la planta no responde?

Automáticamente, cada hora el sistema verifica pedidos enviados hace más de 24 horas sin respuesta y envía un recordatorio.

### ¿Cómo se calcula la comisión?

Automáticamente al marcar el pedido como "Cobrado". La comisión es el 8% del total del pedido.

### ¿Puedo cambiar el porcentaje de comisión?

Sí, en `Pedido.java`, método `calcularComision()`:
```java
this.comision = this.total.multiply(new BigDecimal("0.08")); // Cambiar 0.08
```

### ¿Los 60 días de cobro se cuentan desde la fecha de entrega?

Sí, exactamente. El sistema calcula:
- E-check esperado: Fecha de entrega + 30 días
- Cobro esperado: Fecha de entrega + 60 días

---

## 🔐 Seguridad

### ¿Cómo cambio el usuario y contraseña por defecto?

En `SecurityConfig.java`:
```java
UserDetails user = User.builder()
    .username("nuevo_usuario")
    .password(passwordEncoder().encode("nueva_contraseña"))
    .roles("ADMIN")
    .build();
```

### ¿Puedo agregar más usuarios?

Sí, en el mismo método `userDetailsService()` puedes agregar más usuarios:
```java
UserDetails user2 = User.builder()
    .username("usuario2")
    .password(passwordEncoder().encode("password2"))
    .roles("USER")
    .build();

return new InMemoryUserDetailsManager(user1, user2);
```

### ¿Es seguro usar InMemoryUserDetailsManager?

Para desarrollo sí, para producción es mejor usar una base de datos o LDAP.

### ¿Las credenciales de Google están seguras?

Sí, los tokens se guardan en la carpeta `tokens/` que está en `.gitignore`. Nunca subas esta carpeta a un repositorio público.

---

## 🗄️ Base de Datos

### ¿Dónde se guardan los datos en desarrollo?

En un archivo H2 en: `./data/pedidos.mv.db`

### ¿Puedo ver los datos en la base de datos?

Sí, ve a: http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:file:./data/pedidos`
- Usuario: `sa`
- Contraseña: (dejar vacío)

### ¿Cómo migro a PostgreSQL?

1. Instala PostgreSQL
2. Crea una base de datos: `CREATE DATABASE pedidos_db;`
3. Edita `application.properties`:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/pedidos_db
spring.datasource.username=postgres
spring.datasource.password=tu_password
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
```
4. Comenta las líneas de H2

### ¿Pierdo los datos si reinicio la aplicación?

No, los datos persisten en el archivo de base de datos.

### ¿Cómo hago backup de los datos?

Copia el archivo `./data/pedidos.mv.db` a un lugar seguro.

---

## 📊 Reportes

### ¿Los reportes se actualizan en tiempo real?

Sí, cada vez que cargas la página se consultan los datos más recientes.

### ¿Puedo exportar los reportes?

Actualmente no está implementado, pero puedes:
- Usar la API REST para obtener los datos en JSON
- Exportar desde H2 Console a CSV
- Implementar exportación a Excel (requiere modificación del código)

### ¿Cómo veo las comisiones de un mes específico?

En la página de Reportes, selecciona el año y verás el desglose mensual con comisiones.

---

## 🚀 Despliegue

### ¿Puedo desplegar en Heroku?

Sí, pero necesitas:
1. Configurar PostgreSQL (Heroku no soporta archivos persistentes)
2. Subir `credentials.json` como variable de entorno o archivo de configuración
3. Configurar el Procfile

### ¿Funciona en Windows, Mac y Linux?

Sí, Java es multiplataforma. Solo asegúrate de tener Java 21 instalado.

### ¿Cómo configuro el puerto?

En `application.properties`:
```properties
server.port=8081  # Cambiar el puerto
```

### ¿Puedo usar HTTPS?

Sí, necesitas configurar un certificado SSL en Spring Boot:
```properties
server.port=8443
server.ssl.key-store=classpath:keystore.p12
server.ssl.key-store-password=tu_password
server.ssl.keyStoreType=PKCS12
```

---

## 🔧 Problemas Comunes

### Error: "Could not resolve dependencies"

Maven no puede descargar las dependencias. Soluciones:
1. Verifica tu conexión a internet
2. Usa el archivo `settings.xml` incluido
3. Ejecuta: `mvn clean install -U -s settings.xml`

### Error: "credentials.json not found"

El archivo no está en la ubicación correcta. Debe estar en la raíz del proyecto.

### Error: "Port 8080 already in use"

El puerto está ocupado. Opciones:
1. Cierra la aplicación que usa el puerto 8080
2. Cambia el puerto en `application.properties`

### No se abren las páginas web

Verifica que estás accediendo a `http://localhost:8080` (no https).

### Los emails no se procesan

Verifica:
1. Que `credentials.json` esté configurado
2. Que hayas autorizado Gmail API
3. Que los emails tengan "pedido" en el asunto
4. Que los emails no estén ya marcados como leídos
5. Los logs de la aplicación para errores

### No puedo autorizar Google APIs

Verifica:
1. Que las APIs estén habilitadas en Google Cloud Console
2. Que tu email esté en "Usuarios de prueba"
3. Que el puerto 8888 esté libre (lo usa el servidor de autorización)

### Los gráficos no aparecen

Verifica tu conexión a internet (Chart.js y FullCalendar se cargan desde CDN).

---

## 🎨 Personalización

### ¿Puedo cambiar los colores de la interfaz?

Sí, edita los archivos HTML en `src/main/resources/templates/`.
Los colores se definen en Bootstrap o en las etiquetas `<style>`.

### ¿Puedo agregar más campos al pedido?

Sí:
1. Agrega el campo en `Pedido.java`
2. Agrega la columna en la base de datos (se crea automáticamente con JPA)
3. Actualiza los formularios HTML
4. Actualiza los DTOs si es necesario

### ¿Puedo cambiar el texto de los emails?

Sí, edita `EmailNotificationService.java`. Cada método construye el cuerpo del email.

### ¿Puedo agregar notificaciones por WhatsApp o SMS?

Sí, pero requiere integración con APIs externas como Twilio. No está implementado por defecto.

---

## 💡 Mejoras Futuras

### ¿Qué funcionalidades puedo agregar?

Ideas:
- Exportación de reportes a PDF/Excel
- Notificaciones por WhatsApp
- Sistema de roles (admin, vendedor, logística)
- Panel de métricas en tiempo real
- Integración con sistema de facturación
- Generación automática de remitos
- Firma digital de documentos
- Chat interno entre usuarios
- Notificaciones push en navegador
- App móvil

### ¿Dónde puedo pedir ayuda?

- Revisa la documentación: `README.md`, `CONFIGURACION.md`, `RESUMEN.md`
- Revisa los logs de la aplicación
- Verifica los issues comunes en este FAQ
- Contacta al desarrollador del sistema

---

## 📚 Recursos Adicionales

### Documentación Oficial

- [Spring Boot](https://spring.io/projects/spring-boot)
- [Gmail API](https://developers.google.com/gmail/api)
- [Google Calendar API](https://developers.google.com/calendar)
- [Bootstrap 5](https://getbootstrap.com/)
- [Chart.js](https://www.chartjs.org/)
- [FullCalendar](https://fullcalendar.io/)

### Tutoriales Recomendados

- [Google OAuth 2.0](https://developers.google.com/identity/protocols/oauth2)
- [Spring Security](https://spring.io/guides/gs/securing-web/)
- [JPA y Hibernate](https://spring.io/guides/gs/accessing-data-jpa/)

---

¿Tienes más preguntas? Revisa la documentación completa o contacta al soporte técnico.

