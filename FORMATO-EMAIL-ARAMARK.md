# 📧 Email de Prueba - Formato Aramark

## ✅ Email Correctamente Formateado

### Asunto:
```
Pedido test - OC 113648
```

### Cuerpo del Email:
```
Se envío OC 113648

En caso de que la orden correspondiente no les ingrese de manera automática, favor de avisar así les envío en este hilo de email.

Favor de solicitar turno de entrega a turnos@blancaluna.com.ar para ARAMARK (Central de Restaurantes)

Requisitos para entregar:

Tener observado el DESTINO en la FC :ARAMARK STOCK CD PACHECO
Presentarse con FACTURA "A"
Confirmar este correo y entregar los productos solicitados.
Solicitar turno con antelación mínima de 48 horas informándose: turnos@blancaluna.com.ar

Informar Articulo - Cantidad de Pallets – Tipo (SECO-FRIO-CONGELADO) – Tipo de Transporte . Ponerme en copia a mi

Respetar Horario de turno, pueden estar sujetos a rechazo sino se respeta.
Verificar que el valor unitario de la orden sea el correcto

Importante: En caso de haber sido entregada, reenviar factura sellada correspondiente.

Para

DEPOSITO ARAMARK

Se factura a

Central de Restaurantes / Centrapal / Aramark Servicios (Segun corresponda)

Entregar en

BLANCALUNA DEPOSITO PACHECO

Dirección de Entrega

Av. Juan Domingo Peron 3780 - General Pacheco - CP B1617AGS

Horario de entrega (con turno)

07:00hs - 13:00hs
```

### PDF Adjunto:
- Orden de Compra Aramark 113648.pdf
- Contiene tabla con productos, cantidades y precios

---

## 🔍 Campos Que Se Extraerán Automáticamente

### Del Cuerpo del Email:

| Campo | Valor Extraído | Método |
|-------|---------------|--------|
| **Número OC** | 113648 | `extraerNumeroOCDelTexto()` |
| **Para (Destinatario)** | DEPOSITO ARAMARK | `extraerCampoEspecifico("Para")` |
| **Se factura a** | Central de Restaurantes / Centrapal / Aramark Servicios (Segun corresponda) | `extraerCampoEspecifico("Se factura a")` |
| **Entregar en** | BLANCALUNA DEPOSITO PACHECO | `extraerCampoEspecifico("Entregar en")` |
| **Dirección** | Av. Juan Domingo Peron 3780 - General Pacheco - CP B1617AGS | `extraerCampoEspecifico("Dirección de Entrega")` |
| **Horario** | 07:00 (hora de inicio) | `extraerHorarioRango()` |
| **Fecha de Entrega** | **Calculada automáticamente: 7 días hábiles desde hoy** | `calcularFechaEntregaHabiles()` |

### Del PDF Adjunto:

| Campo | Método |
|-------|--------|
| **Productos** | `pdfProcessingService.extraerItemsDelPdf()` |
| **Número OC** (confirmación) | `pdfProcessingService.extraerNumeroOrdenCompra()` |
| **Total** | `pdfProcessingService.extraerTotalDelPdf()` |

---

## 📅 Cálculo de Fecha de Entrega

**Hoy:** 19/01/2026 (Domingo)

**Cálculo de 7 días hábiles:**

| Fecha | Día | ¿Hábil? | Días Hábiles Acumulados |
|-------|-----|---------|-------------------------|
| 20/01 | Lunes | ✅ | 1 |
| 21/01 | Martes | ✅ | 2 |
| 22/01 | Miércoles | ✅ | 3 |
| 23/01 | Jueves | ✅ | 4 |
| 24/01 | Viernes | ✅ | 5 |
| 25/01 | Sábado | ❌ | - |
| 26/01 | Domingo | ❌ | - |
| 27/01 | Lunes | ✅ | 6 |
| 28/01 | Martes | ✅ | 7 ✅ |

**Fecha de Entrega Calculada:** **28/01/2026**

---

## 🚀 Logs Esperados

Cuando proceses el email, verás:

```log
INFO  - Iniciando procesamiento de emails de pedidos...
INFO  - Encontrados 1 emails de pedidos sin procesar
INFO  - Procesando pedido - Asunto: Pedido test - OC 113648 - Remitente: Ramiro villani <ramavillani@gmail.com>
INFO  - Email tiene PDF adjunto, procesando...
INFO  - Adjunto PDF descargado: Orden de Compra Aramark 113648.pdf (1395364 bytes)
INFO  - Extraídos X items del PDF
INFO  - Número de OC extraído del PDF: 113648
INFO  - Número de OC extraído: 113648
DEBUG - Para (Destinatario): DEPOSITO ARAMARK
DEBUG - Se factura a: Central de Restaurantes / Centrapal / Aramark Servicios (Segun corresponda)
DEBUG - Entregar en: BLANCALUNA DEPOSITO PACHECO
DEBUG - Dirección de entrega: Av. Juan Domingo Peron 3780 - General Pacheco - CP B1617AGS
DEBUG - Horario entrega: 07:00
INFO  - Fecha de entrega calculada (7 días hábiles): 2026-01-28
INFO  - Pedido creado: PED-20260119-0001 - Cliente: Central de Restaurantes / Centrapal / Aramark Servicios (Segun corresponda) - Entregar a: DEPOSITO ARAMARK
INFO  - Pedido agregado al calendario
INFO  - Pedido enviado a planta
INFO  - Pedido procesado exitosamente: PED-20260119-0001
INFO  - Email marcado como leído
```

---

## ✅ Cambios Implementados

### 1. **Nuevo Método: `extraerCampoEspecifico()`**
Extrae campos con formato específico de Aramark:
```
Campo

Valor
```

### 2. **Nuevo Método: `extraerNumeroOCDelTexto()`**
Extrae el número de OC del cuerpo del email:
- "Se envío OC 113648" → 113648
- "Orden de Compra: 113648" → 113648

### 3. **Nuevo Método: `extraerHorarioRango()`**
Extrae horarios con formato de rango:
- "07:00hs - 13:00hs" → 07:00
- Toma la hora de inicio del rango

### 4. **Nuevo Método: `calcularFechaEntregaHabiles()`**
Calcula automáticamente 7 días hábiles desde la fecha actual:
- Excluye sábados y domingos
- Cuenta solo días de lunes a viernes

### 5. **Método Actualizado: `parsearCuerpoEmail()`**
Ahora extrae:
- ✅ Para (destinatario)
- ✅ Se factura a (cliente facturación)
- ✅ Entregar en (lugar de entrega)
- ✅ Dirección de Entrega
- ✅ Horario de entrega (rango)
- ✅ Fecha de entrega (calculada automáticamente)
- ✅ Número de OC

---

## 🧪 Para Probar

1. **Reenvía el email** que ya enviaste (con el mismo contenido y PDF)
2. **Espera 10 minutos** o **reinicia la aplicación**
3. **Verifica en los logs** que ahora aparezca:
   ```
   INFO - Fecha de entrega calculada (7 días hábiles): 2026-01-28
   INFO - Pedido creado: PED-20260119-0001
   ```
4. **Revisa en la base de datos** (H2 Console) que el pedido se haya creado con todos los campos

---

## 📝 Nota Importante

El sistema ya NO requiere que el email incluya "Fecha de entrega: XX/XX/XXXX" porque **ahora la calcula automáticamente** sumando 7 días hábiles desde la fecha de recepción del email.

Si quieres cambiar la cantidad de días hábiles, modifica esta línea en `EmailProcessingService.java`:

```java
// Cambiar el 7 por la cantidad de días que necesites
LocalDate fechaEntrega = calcularFechaEntregaHabiles(LocalDate.now(), 7);
```

---

✅ **Sistema actualizado y listo para procesar emails en el formato de Aramark**

