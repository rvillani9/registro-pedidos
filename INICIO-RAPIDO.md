# ⚡ Inicio Rápido

## 🎯 En 5 Pasos

### 1️⃣ Configura Google Cloud (15 minutos)
```
1. Ve a: https://console.cloud.google.com/
2. Crea proyecto nuevo
3. Habilita APIs:
   - Gmail API
   - Google Calendar API
4. Crea credenciales OAuth 2.0 (Aplicación de escritorio)
5. Descarga JSON → Renombra a "credentials.json"
6. Coloca en: C:\Users\l1008754\IdeaProjects\ingreso-pedidos\
```

### 2️⃣ Configura Emails (2 minutos)
Edita: `src/main/resources/application.properties`
```properties
app.email.planta=planta@tuempresa.com
app.email.blancaluna=blancaluna@distribucion.com
```

### 3️⃣ Ejecuta la Aplicación (1 minuto)
```powershell
.\start.ps1
```
O manualmente:
```powershell
mvn clean package -DskipTests -s settings.xml
java -jar target/ingreso-pedidos-1.0-SNAPSHOT.jar
```

### 4️⃣ Autoriza Google (2 minutos)
```
- Se abre el navegador automáticamente
- Selecciona tu cuenta Gmail
- Clic en "Avanzado" → "Ir a Sistema de Pedidos"
- Permite los permisos solicitados
```

### 5️⃣ Prueba el Sistema (5 minutos)
```
1. Abre: http://localhost:8080
2. Login: admin / admin123
3. Abre: ejemplo-email-pedido.html en navegador
4. Copia el HTML completo
5. Envía email a tu Gmail:
   - Asunto: "Nuevo Pedido"
   - Pega el HTML en Gmail
6. Espera 10 minutos o reinicia la app
7. Verifica en: http://localhost:8080/pedidos
```

## 📝 Checklist Rápido

### Antes de Empezar
- [ ] Java 21 instalado: `java -version`
- [ ] Maven instalado: `mvn -v`
- [ ] Cuenta de Gmail activa
- [ ] Cuenta de Google Calendar

### Configuración Inicial
- [ ] Proyecto creado en Google Cloud Console
- [ ] Gmail API habilitada
- [ ] Google Calendar API habilitada
- [ ] credentials.json descargado y colocado en raíz
- [ ] Emails configurados en application.properties

### Primera Ejecución
- [ ] Aplicación compilada sin errores
- [ ] Aplicación ejecutándose en puerto 8080
- [ ] Autorización de Google completada
- [ ] Carpeta `tokens/` creada automáticamente
- [ ] Login exitoso en http://localhost:8080

### Prueba del Sistema
- [ ] Email de prueba enviado
- [ ] Pedido creado en la aplicación
- [ ] Evento creado en Google Calendar
- [ ] Email enviado a la "planta"

## 🆘 Problemas Frecuentes

### ❌ Error al compilar
```powershell
# Limpiar y volver a intentar
mvn clean
mvn package -DskipTests -s settings.xml
```

### ❌ No encuentra credentials.json
```powershell
# Verificar ubicación
dir credentials.json
# Debe estar en la raíz del proyecto
```

### ❌ Puerto 8080 ocupado
En `application.properties` cambia:
```properties
server.port=8081
```

### ❌ No se procesan emails
- Verifica que el asunto contenga "pedido"
- Espera 10 minutos o reinicia la app
- Revisa los logs en la consola

## 📚 Archivos de Ayuda

| Archivo | Para qué sirve |
|---------|----------------|
| `RESUMEN.md` | Resumen completo del sistema |
| `README.md` | Documentación técnica |
| `CONFIGURACION.md` | Guía detallada paso a paso |
| `FAQ.md` | Preguntas frecuentes |
| `ejemplo-email-pedido.html` | Email de prueba |
| `start.ps1` | Script para iniciar fácilmente |

## 🎓 Aprende Más

### Flujo Completo
```
Email → Sistema (10min) → BD → Calendar → Planta → 
Recordatorio (24h) → Fabricación → Logística (48h antes) → 
Turno → Entrega → Documentos → Factura → E-check (30d) → 
Cobro (60d) → Comisión (8%)
```

### Comandos Útiles

**Iniciar aplicación:**
```powershell
.\start.ps1
```

**Ver logs en tiempo real:**
```powershell
# Ya se muestran en la consola
```

**Acceder a H2 Console:**
```
http://localhost:8080/h2-console
JDBC URL: jdbc:h2:file:./data/pedidos
Usuario: sa
Password: (vacío)
```

**Compilar sin ejecutar:**
```powershell
mvn clean package -DskipTests
```

**Ejecutar tests:**
```powershell
mvn test
```

### URLs Importantes

| URL | Descripción |
|-----|-------------|
| http://localhost:8080 | Aplicación principal |
| http://localhost:8080/pedidos | Gestión de pedidos |
| http://localhost:8080/calendario | Calendario de entregas |
| http://localhost:8080/reportes | Reportes y estadísticas |
| http://localhost:8080/h2-console | Consola de base de datos |

## 🚀 Próximos Pasos

Una vez que todo funcione:

1. ✅ **Personaliza** los templates HTML
2. ✅ **Ajusta** los textos de emails
3. ✅ **Configura** PostgreSQL para producción
4. ✅ **Cambia** el usuario/password por defecto
5. ✅ **Configura** tu dominio propio
6. ✅ **Habilita** HTTPS
7. ✅ **Implementa** backups automáticos

## 💬 ¿Necesitas Ayuda?

1. Lee el archivo correspondiente:
   - Dudas técnicas → `README.md`
   - Configuración → `CONFIGURACION.md`
   - Problemas → `FAQ.md`

2. Revisa los logs en la consola

3. Verifica que todos los pasos del checklist estén completos

## ⚡ Comandos de Un Vistazo

```powershell
# Iniciar todo
.\start.ps1

# O paso por paso:
mvn clean package -DskipTests -s settings.xml
java -jar target/ingreso-pedidos-1.0-SNAPSHOT.jar

# Acceder
Start-Process "http://localhost:8080"
# Login: admin / admin123

# Detener
Ctrl + C
```

## 🎉 ¡Listo!

Tu sistema de gestión de pedidos está configurado y listo para usar.

**¡Éxito! 🚀**

---

📌 **Tip**: Guarda este archivo en marcadores para acceso rápido.

