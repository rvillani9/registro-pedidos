# Guía de Configuración Inicial

## Paso 1: Configurar Google Cloud Console

### 1.1 Crear un proyecto en Google Cloud

1. Ve a [Google Cloud Console](https://console.cloud.google.com/)
2. Haz clic en "Seleccionar proyecto" en la parte superior
3. Haz clic en "Nuevo proyecto"
4. Nombre del proyecto: "Ingreso Pedidos" (o el nombre que prefieras)
5. Haz clic en "Crear"

### 1.2 Habilitar las APIs necesarias

1. En el menú lateral, ve a "APIs y servicios" > "Biblioteca"
2. Busca "Gmail API"
   - Haz clic en "Gmail API"
   - Haz clic en "Habilitar"
3. Busca "Google Calendar API"
   - Haz clic en "Google Calendar API"
   - Haz clic en "Habilitar"

### 1.3 Crear credenciales OAuth 2.0

1. Ve a "APIs y servicios" > "Credenciales"
2. Haz clic en "+ CREAR CREDENCIALES"
3. Selecciona "ID de cliente de OAuth"
4. Si es la primera vez, deberás configurar la pantalla de consentimiento:
   - Tipo de usuario: Externo (o Interno si es para tu organización)
   - Nombre de la aplicación: "Sistema de Pedidos"
   - Correo electrónico de asistencia: tu email
   - Completa los campos obligatorios
   - Guarda y continúa
   - En "Alcances", no necesitas agregar nada, simplemente continúa
   - Guarda y continúa
5. Vuelve a "Credenciales" > "+ CREAR CREDENCIALES" > "ID de cliente de OAuth"
6. Tipo de aplicación: **Aplicación de escritorio**
7. Nombre: "Cliente Desktop Pedidos"
8. Haz clic en "Crear"
9. **IMPORTANTE**: Descarga el archivo JSON que aparece
10. Renombra el archivo descargado a `credentials.json`
11. Copia `credentials.json` a la raíz del proyecto (carpeta ingreso-pedidos)

### 1.4 Agregar usuarios de prueba (si tu app está en desarrollo)

1. Ve a "APIs y servicios" > "Pantalla de consentimiento de OAuth"
2. En la sección "Usuarios de prueba", haz clic en "+ AGREGAR USUARIOS"
3. Agrega el email de Gmail que usarás para recibir los pedidos
4. Guarda

## Paso 2: Configurar la Aplicación

### 2.1 Verificar credentials.json

Asegúrate de que el archivo `credentials.json` esté en:
```
C:\Users\l1008754\IdeaProjects\ingreso-pedidos\credentials.json
```

### 2.2 Configurar emails en application.properties

Edita el archivo `src/main/resources/application.properties`:

```properties
# Email de la planta (donde se fabrican los productos)
app.email.planta=planta@tuempresa.com

# Email del centro de distribución BlancaLuna
app.email.blancaluna=blancaluna@distribucion.com
```

### 2.3 Base de datos (opcional para desarrollo)

Por defecto, el sistema usa H2 (base de datos en archivo).
Los datos se guardarán en: `./data/pedidos.mv.db`

**Para producción con PostgreSQL:**

1. Edita `application.properties`:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/pedidos_db
spring.datasource.username=tu_usuario
spring.datasource.password=tu_password
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=update
```

2. Comenta o elimina las líneas de H2

## Paso 3: Ejecutar la Aplicación

### Opción 1: Usando el script (recomendado)

```powershell
.\start.ps1
```

### Opción 2: Manualmente

```powershell
# Compilar
mvn clean package -DskipTests -s settings.xml

# Ejecutar
java -jar target/ingreso-pedidos-1.0-SNAPSHOT.jar
```

### Opción 3: Desde el IDE (IntelliJ IDEA)

1. Abre el proyecto en IntelliJ IDEA
2. Espera a que Maven descargue las dependencias
3. Busca la clase `IngresoPedidosApplication.java`
4. Click derecho > Run 'IngresoPedidosApplication'

## Paso 4: Primera Ejecución

### 4.1 Autorización de Google

La primera vez que ejecutes la aplicación:

1. Se abrirá automáticamente tu navegador predeterminado
2. Selecciona la cuenta de Gmail que configuraste
3. Google te advertirá que la app no está verificada (es normal):
   - Haz clic en "Avanzado"
   - Haz clic en "Ir a Sistema de Pedidos (no seguro)"
4. Otorga los permisos solicitados:
   - Ver, redactar, enviar y eliminar permanentemente todos tus correos de Gmail
   - Ver, editar, compartir y eliminar permanentemente todos los calendarios
5. Haz clic en "Permitir"

Esto creará el archivo `tokens/StoredCredential` que guardará la autorización.

### 4.2 Acceder a la aplicación

1. Abre tu navegador en: **http://localhost:8080**
2. Usuario: **admin**
3. Contraseña: **admin123**

## Paso 5: Configurar Gmail para Pedidos

### 5.1 Formato de email esperado

Los emails de pedidos deben:

1. **Asunto**: Contener la palabra "pedido" (ej: "Nuevo Pedido", "Pedido #123")
2. **Contenido**: 
   - Línea con: `Fecha de Entrega: DD/MM/YYYY`
   - Línea con: `Lugar de Entrega: Dirección completa`
   - Tabla HTML con columnas: Producto | Cantidad | Precio

### 5.2 Ejemplo de email

```html
Asunto: Nuevo Pedido - Cliente XYZ

Fecha de Entrega: 25/01/2026
Lugar de Entrega: Av. Corrientes 1234, CABA

<table>
  <tr>
    <th>Producto</th>
    <th>Cantidad</th>
    <th>Precio Unitario</th>
  </tr>
  <tr>
    <td>Producto A</td>
    <td>10</td>
    <td>100.00</td>
  </tr>
  <tr>
    <td>Producto B</td>
    <td>5</td>
    <td>50.00</td>
  </tr>
</table>
```

### 5.3 Crear una etiqueta en Gmail (opcional)

Para filtrar mejor los pedidos:

1. Ve a Gmail
2. Crea una etiqueta llamada "Pedidos"
3. Configura un filtro para etiquetar automáticamente emails con asunto "pedido"

## Paso 6: Probar el Sistema

### 6.1 Enviar un pedido de prueba

1. Desde tu Gmail (o pídele a alguien que te envíe):
2. Envía un email a tu cuenta con el formato indicado arriba
3. Espera 10 minutos (o reinicia la aplicación para forzar el procesamiento)
4. Ve a la aplicación web > "Pedidos"
5. Deberías ver el nuevo pedido creado

### 6.2 Verificar Google Calendar

1. Ve a [Google Calendar](https://calendar.google.com/)
2. Deberías ver un evento creado para la fecha de entrega

## Paso 7: Flujo Completo de un Pedido

1. **Email recibido** → Se crea el pedido automáticamente
2. **Calendario** → Se crea evento en Google Calendar
3. **Planta** → Se envía email automático a la planta
4. **Recordatorio** → Si no hay respuesta en 24hs, se envía recordatorio
5. **Fabricación** → Cambias el estado manualmente a "En Fabricación"
6. **Logística** → 48hs antes, se envía recordatorio automático
7. **Turno BlancaLuna** → Solicitas turno (botón en la app)
8. **Entrega** → Marcas como entregado con la fecha
9. **Documentos** → Marcas cuando recibes remito y factura sellados
10. **Factura Alta** → Marcas cuando se da de alta
11. **E-check** → 30 días después de entrega, se espera el e-check
12. **Cobrado** → 60 días después de entrega, marcas como cobrado
13. **Comisión** → Se calcula automáticamente el 8%

## Solución de Problemas

### La aplicación no compila

```powershell
# Limpia el caché de Maven
mvn clean

# Descarga dependencias forzadamente
mvn dependency:resolve -U -s settings.xml

# Vuelve a compilar
mvn package -DskipTests -s settings.xml
```

### No se conecta a Gmail

1. Verifica que `credentials.json` esté en la raíz
2. Elimina la carpeta `tokens/` y vuelve a autorizar
3. Verifica que las APIs estén habilitadas en Google Cloud Console
4. Verifica que tu email esté en "Usuarios de prueba"

### No se procesan los emails

1. Verifica que los emails tengan "pedido" en el asunto
2. Revisa los logs en la consola para ver errores
3. Verifica que el formato del email sea correcto
4. Los emails se procesan cada 10 minutos automáticamente

### Error de puerto 8080 en uso

Cambia el puerto en `application.properties`:
```properties
server.port=8081
```

## URLs Útiles

- **Aplicación**: http://localhost:8080
- **H2 Console** (desarrollo): http://localhost:8080/h2-console
  - JDBC URL: `jdbc:h2:file:./data/pedidos`
  - Usuario: `sa`
  - Contraseña: (vacío)
- **Google Cloud Console**: https://console.cloud.google.com/
- **Gmail**: https://mail.google.com/
- **Google Calendar**: https://calendar.google.com/

## Seguridad en Producción

Antes de desplegar en producción:

1. **Cambiar credenciales**:
```java
// En SecurityConfig.java
UserDetails user = User.builder()
    .username("tu_usuario")  // Cambiar
    .password(passwordEncoder().encode("contraseña_segura"))  // Cambiar
    .roles("ADMIN")
    .build();
```

2. **Habilitar CSRF**:
```java
// En SecurityConfig.java
.csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
```

3. **Usar HTTPS**
4. **Proteger credentials.json** (nunca subirlo a repositorios públicos)
5. **Usar base de datos PostgreSQL** en lugar de H2
6. **Configurar backups** de la base de datos

## Próximos Pasos

Una vez que todo funcione:

1. Personaliza los templates HTML en `src/main/resources/templates/`
2. Ajusta los emails enviados en `EmailNotificationService.java`
3. Configura el dominio propio para producción
4. Considera agregar más usuarios con diferentes roles
5. Implementa un sistema de respaldo automático

¿Necesitas ayuda? Revisa el archivo `README.md` para más información.

