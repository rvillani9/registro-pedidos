package com.example.pedidos.service;

import com.example.pedidos.dto.ItemPedidoDTO;
import com.example.pedidos.dto.PedidoDTO;
import com.example.pedidos.model.Pedido;
import com.google.api.services.gmail.model.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailProcessingService {

    private final GmailService gmailService;
    private final PedidoService pedidoService;
    private final PdfProcessingService pdfProcessingService;

    /**
     * Procesa emails de pedidos no leídos
     */
    public void procesarEmailsPedidos() {
        log.info("╔══════════════════════════════════════════════════════════╗");
        log.info("║  INICIANDO PROCESAMIENTO DE EMAILS DE PEDIDOS            ║");
        log.info("╚══════════════════════════════════════════════════════════╝");

        try {
            // Buscar emails no leídos con asunto que contenga "pedido"
            String query = "is:unread subject:pedido";
            log.info("Búsqueda en Gmail: query = '{}'", query);

            List<Message> messages = gmailService.leerEmailsPedidos(query);

            log.info(">>> Encontrados {} emails de pedidos sin procesar <<<", messages.size());

            if (messages.isEmpty()) {
                log.info("No hay emails nuevos para procesar");
                log.info("Verifica que:");
                log.info("  1. Haya emails sin leer en tu bandeja");
                log.info("  2. El asunto contenga la palabra 'pedido'");
                log.info("  3. Las credenciales de Gmail estén configuradas");
            }

            for (Message message : messages) {
                try {
                    log.info("──────────────────────────────────────────────────────────");
                    log.info("Procesando email: {}", message.getId());
                    procesarEmailPedido(message);
                    gmailService.marcarComoLeido(message.getId());
                    log.info("✓ Email procesado y marcado como leído: {}", message.getId());
                } catch (Exception e) {
                    log.error("✗ Error procesando email {}: {}", message.getId(), e.getMessage(), e);
                }
            }

            log.info("╔══════════════════════════════════════════════════════════╗");
            log.info("║  PROCESAMIENTO COMPLETADO                                 ║");
            log.info("║  Emails procesados: {}                                    ", String.format("%-35s", messages.size()) + "║");
            log.info("╚══════════════════════════════════════════════════════════╝");

        } catch (Exception e) {
            log.error("╔══════════════════════════════════════════════════════════╗");
            log.error("║  ERROR CRÍTICO AL PROCESAR EMAILS                        ║");
            log.error("╚══════════════════════════════════════════════════════════╝");
            log.error("Error al procesar emails de pedidos", e);
            log.error("Detalles del error: {}", e.getMessage());
            if (e.getCause() != null) {
                log.error("Causa raíz: {}", e.getCause().getMessage());
            }
        }
    }

    /**
     * Procesa un email individual de pedido
     */
    private void procesarEmailPedido(Message message) throws Exception {
        String cuerpo = gmailService.extraerCuerpoEmail(message);
        String remitente = gmailService.extraerRemitente(message);
        String asunto = gmailService.extraerAsunto(message);

        log.info("   Asunto: {}", asunto);
        log.info("   Remitente: {}", remitente);
        log.info("   Tamaño del mensaje: {} caracteres", cuerpo.length());

        // Parsear el cuerpo del email para extraer información de entrega
        PedidoDTO pedidoDTO = parsearCuerpoEmail(cuerpo, remitente, message.getId());

        // Verificar si tiene PDF adjunto
        if (gmailService.tieneAdjuntoPdf(message)) {
            log.info("   ✓ Email con PDF adjunto detectado");

            byte[] pdfBytes = gmailService.descargarAdjuntoPdf(message);
            if (pdfBytes != null) {
                log.info("   ✓ PDF descargado: {} bytes", pdfBytes.length);
                String textoPdf = pdfProcessingService.extraerTextoDePdf(pdfBytes);

                // Extraer items del PDF
                List<ItemPedidoDTO> items = pdfProcessingService.extraerItemsDelPdf(textoPdf);
                pedidoDTO.setItems(items);
                log.info("   ✓ Items extraídos del PDF: {}", items.size());

                // Extraer número de OC del PDF si está disponible
                String numeroOC = pdfProcessingService.extraerNumeroOrdenCompra(textoPdf);
                if (numeroOC != null) {
                    log.info("   ✓ Número de OC extraído: {}", numeroOC);
                }
            } else {
                log.warn("   ⚠ No se pudo descargar el PDF adjunto");
            }
        } else {
            log.warn("   ⚠ Email sin PDF adjunto, intentando extraer items del cuerpo HTML");
            // Fallback: intentar extraer de tabla HTML (comportamiento anterior)
            Document doc = Jsoup.parse(cuerpo);
            List<ItemPedidoDTO> items = extraerItemsDeTabla(doc);
            pedidoDTO.setItems(items);
            log.info("   Items extraídos del HTML: {}", items.size());
        }

        if (pedidoDTO != null && validarPedido(pedidoDTO)) {
            // Crear el pedido
            log.info("   Creando pedido en la base de datos...");
            Pedido pedido = pedidoService.crearPedido(pedidoDTO);

            // Agregarlo al calendario
            log.info("   Agregando al calendario...");
            pedidoService.agregarACalendario(pedido.getId());

            // Enviarlo a la planta
            log.info("   Enviando notificación a la planta...");
            pedidoService.enviarAPlanta(pedido.getId());

            log.info("   ✓✓✓ Pedido procesado exitosamente: {} ✓✓✓", pedido.getNumeroPedido());
        } else {
            log.warn("   ✗ Pedido inválido o información incompleta - Email ID: {}", message.getId());
            log.warn("   Revisa el formato del email o la información del pedido");
        }
    }

    /**
     * Parsea el cuerpo del email para extraer información del pedido
     * Extrae: Cliente facturación, destinatario entrega, dirección, horario, fecha
     */
    private PedidoDTO parsearCuerpoEmail(String contenido, String remitente, String messageId) {
        try {
            PedidoDTO pedidoDTO = new PedidoDTO();
            pedidoDTO.setEmailOrigen(extraerEmail(remitente));
            pedidoDTO.setGmailMessageId(messageId);

            // Parsear como HTML o texto plano
            String texto = contenido;
            if (contenido.contains("<html") || contenido.contains("<body")) {
                Document doc = Jsoup.parse(contenido);
                texto = doc.text();
            }

            // Extraer "Para" (destinatario final)
            String para = extraerCampoEspecifico(texto, "Para");
            if (para != null) {
                pedidoDTO.setDestinatarioEntrega(para);
                log.debug("Para (Destinatario): {}", para);
            }

            // Extraer "Se factura a"
            String seFacturaA = extraerCampoEspecifico(texto, "Se factura a");
            if (seFacturaA != null) {
                pedidoDTO.setClienteFacturacion(seFacturaA);
                log.debug("Se factura a: {}", seFacturaA);
            }

            // Extraer "Entregar en"
            String entregarEn = extraerCampoEspecifico(texto, "Entregar en");
            if (entregarEn != null) {
                pedidoDTO.setLugarEntrega(entregarEn);
                log.debug("Entregar en: {}", entregarEn);
            }

            // Extraer "Dirección de Entrega"
            String direccion = extraerCampoEspecifico(texto, "Direcci[oó]n de Entrega");
            if (direccion != null) {
                pedidoDTO.setDireccionEntrega(direccion);
                log.debug("Dirección de entrega: {}", direccion);
            }

            // Extraer horario (puede ser rango: "07:00hs - 13:00hs")
            String horario = extraerHorarioRango(texto);
            if (horario != null) {
                try {
                    pedidoDTO.setHorarioEntrega(LocalTime.parse(horario));
                    log.debug("Horario entrega: {}", horario);
                } catch (Exception e) {
                    log.warn("Error parseando horario: {}", horario);
                }
            }

            // Calcular fecha de entrega: 7 días hábiles desde hoy
            LocalDate fechaEntrega = calcularFechaEntregaHabiles(LocalDate.now(), 7);
            pedidoDTO.setFechaEntrega(fechaEntrega);
            log.info("Fecha de entrega calculada (7 días hábiles): {}", fechaEntrega);

            // Extraer número de OC del texto
            String numeroOC = extraerNumeroOCDelTexto(texto);
            if (numeroOC != null) {
                log.info("Número de OC extraído del cuerpo: {}", numeroOC);
                // Guardar el número de OC (puedes agregarlo como campo del DTO si lo necesitas)
            }

            return pedidoDTO;
        } catch (Exception e) {
            log.error("Error parseando cuerpo del email", e);
            return null;
        }
    }

    /**
     * Extrae un campo del texto usando múltiples patrones
     */
    private String extraerCampo(String texto, String... patronesStr) {
        for (String patronStr : patronesStr) {
            Pattern pattern = Pattern.compile(patronStr + "[:\\s]+([^\\n.;]+)", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(texto);
            if (matcher.find()) {
                return matcher.group(1).trim();
            }
        }
        return null;
    }

    /**
     * Extrae un campo específico con formato "Campo\n\nValor"
     * Usado para el formato de Aramark donde viene "Para\n\nDEPOSITO ARAMARK"
     */
    private String extraerCampoEspecifico(String texto, String nombreCampo) {
        // Patrón: "Para\n\nDEPOSITO ARAMARK" o "Para\nDEPOSITO ARAMARK"
        Pattern pattern = Pattern.compile(
            nombreCampo + "\\s*\\n+\\s*([^\\n]+)",
            Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = pattern.matcher(texto);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    /**
     * Extrae número de OC del texto del email
     * Ejemplo: "Se envío OC 113648" -> "113648"
     */
    private String extraerNumeroOCDelTexto(String texto) {
        Pattern[] patrones = {
            Pattern.compile("OC[\\s#:Nº]*([0-9]+)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("Orden de Compra[\\s#:Nº]*([0-9]+)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("O\\.C\\.[\\s#:Nº]*([0-9]+)", Pattern.CASE_INSENSITIVE)
        };

        for (Pattern patron : patrones) {
            Matcher matcher = patron.matcher(texto);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return null;
    }

    /**
     * Extrae el horario de entrega del texto, soportando rangos
     * Ejemplo: "07:00hs - 13:00hs" -> retorna "07:00" (hora de inicio)
     */
    private String extraerHorarioRango(String texto) {
        // Buscar patrones como:
        // "07:00hs - 13:00hs"
        // "Horario de entrega (con turno)\n\n07:00hs - 13:00hs"

        Pattern[] patrones = {
            // Rango con "hs"
            Pattern.compile("(\\d{1,2}:\\d{2})\\s*hs\\s*-\\s*\\d{1,2}:\\d{2}\\s*hs", Pattern.CASE_INSENSITIVE),
            // Rango sin "hs"
            Pattern.compile("(\\d{1,2}:\\d{2})\\s*-\\s*\\d{1,2}:\\d{2}", Pattern.CASE_INSENSITIVE),
            // Horario simple
            Pattern.compile("horario[:\\s]+(\\d{1,2}:\\d{2})", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(\\d{1,2}:\\d{2})\\s*hs", Pattern.CASE_INSENSITIVE)
        };

        for (Pattern pattern : patrones) {
            Matcher matcher = pattern.matcher(texto);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }

        return null;
    }

    /**
     * Calcula la fecha de entrega sumando días hábiles (lunes a viernes)
     * @param fechaInicio Fecha desde la cual calcular
     * @param diasHabiles Cantidad de días hábiles a sumar
     * @return Fecha de entrega
     */
    private LocalDate calcularFechaEntregaHabiles(LocalDate fechaInicio, int diasHabiles) {
        LocalDate fecha = fechaInicio;
        int diasSumados = 0;

        while (diasSumados < diasHabiles) {
            fecha = fecha.plusDays(1);

            // Verificar si es día hábil (lunes=1 a viernes=5)
            if (fecha.getDayOfWeek().getValue() <= 5) {
                diasSumados++;
            }
        }

        return fecha;
    }

    /**
     * Parsea el contenido del email para extraer información del pedido
     * MÉTODO ANTIGUO - Mantenido para compatibilidad
     */
    private PedidoDTO parsearEmailPedido(String htmlContent, String remitente, String messageId) {
        try {
            Document doc = Jsoup.parse(htmlContent);
            PedidoDTO pedidoDTO = new PedidoDTO();
            pedidoDTO.setEmailOrigen(extraerEmail(remitente));
            pedidoDTO.setGmailMessageId(messageId);

            // Extraer fecha de entrega
            LocalDate fechaEntrega = extraerFechaEntrega(doc);
            if (fechaEntrega != null) {
                pedidoDTO.setFechaEntrega(fechaEntrega);
            }

            // Extraer lugar de entrega
            String lugarEntrega = extraerLugarEntrega(doc);
            if (lugarEntrega != null) {
                pedidoDTO.setLugarEntrega(lugarEntrega);
            }

            // Extraer items de la tabla
            List<ItemPedidoDTO> items = extraerItemsDeTabla(doc);
            if (!items.isEmpty()) {
                pedidoDTO.setItems(items);
            }

            return pedidoDTO;
        } catch (Exception e) {
            log.error("Error parseando email de pedido", e);
            return null;
        }
    }

    /**
     * Extrae items de la tabla HTML del pedido
     */
    private List<ItemPedidoDTO> extraerItemsDeTabla(Document doc) {
        List<ItemPedidoDTO> items = new ArrayList<>();

        // Buscar la tabla en el HTML
        Elements tables = doc.select("table");

        for (Element table : tables) {
            Elements rows = table.select("tr");

            // Saltar el header (primera fila)
            for (int i = 1; i < rows.size(); i++) {
                Element row = rows.get(i);
                Elements cols = row.select("td, th");

                // Esperamos 5 columnas: Producto, Cantidad, Precio Unitario, Subtotal, (y una extra si existe)
                if (cols.size() >= 3) {
                    try {
                        ItemPedidoDTO item = new ItemPedidoDTO();

                        // Columna 0: Producto
                        item.setProducto(cols.get(0).text().trim());

                        // Columna 1: Cantidad
                        String cantidadStr = cols.get(1).text().trim().replaceAll("[^0-9]", "");
                        if (!cantidadStr.isEmpty()) {
                            item.setCantidad(Integer.parseInt(cantidadStr));
                        }

                        // Columna 2: Precio (o columna 3 dependiendo del formato)
                        String precioStr = cols.size() >= 3 ? cols.get(2).text().trim() : "";
                        precioStr = precioStr.replaceAll("[^0-9.,]", "").replace(",", ".");
                        if (!precioStr.isEmpty()) {
                            item.setPrecioUnitario(new BigDecimal(precioStr));
                        }

                        // Solo agregar si tiene datos válidos
                        if (item.getProducto() != null && !item.getProducto().isEmpty()
                                && item.getCantidad() != null && item.getPrecioUnitario() != null) {
                            items.add(item);
                            log.debug("Item extraído: {} x{} - ${}",
                                    item.getProducto(), item.getCantidad(), item.getPrecioUnitario());
                        }
                    } catch (Exception e) {
                        log.warn("Error procesando fila de tabla: {}", e.getMessage());
                    }
                }
            }
        }

        return items;
    }

    /**
     * Extrae la fecha de entrega del contenido del email
     */
    private LocalDate extraerFechaEntrega(Document doc) {
        return extraerFechaEntrega(doc.text());
    }

    /**
     * Extrae la fecha de entrega del texto
     */
    private LocalDate extraerFechaEntrega(String texto) {

        // Patrones comunes de fecha
        Pattern[] patterns = {
                Pattern.compile("fecha de entrega[:\\s]*(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4})", Pattern.CASE_INSENSITIVE),
                Pattern.compile("entrega[:\\s]*(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4})", Pattern.CASE_INSENSITIVE),
                Pattern.compile("(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4})")
        };

        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(texto);
            if (matcher.find()) {
                String fechaStr = matcher.group(1);
                try {
                    // Intentar diferentes formatos
                    DateTimeFormatter[] formatters = {
                            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
                            DateTimeFormatter.ofPattern("dd/MM/yy"),
                            DateTimeFormatter.ofPattern("dd-MM-yy")
                    };

                    for (DateTimeFormatter formatter : formatters) {
                        try {
                            return LocalDate.parse(fechaStr, formatter);
                        } catch (Exception ignored) {
                        }
                    }
                } catch (Exception e) {
                    log.warn("Error parseando fecha: {}", fechaStr);
                }
            }
        }

        return null;
    }

    /**
     * Extrae el lugar de entrega del contenido del email
     */
    private String extraerLugarEntrega(Document doc) {
        String texto = doc.text();

        Pattern pattern = Pattern.compile("lugar de entrega[:\\s]*([^\\n.;]+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(texto);

        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        // Alternativa: buscar después de "entregar en" o "dirección"
        pattern = Pattern.compile("entregar en[:\\s]*([^\\n.;]+)", Pattern.CASE_INSENSITIVE);
        matcher = pattern.matcher(texto);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        return null;
    }

    /**
     * Extrae el email del formato "Nombre <email@dominio.com>"
     */
    private String extraerEmail(String remitente) {
        Pattern pattern = Pattern.compile("<([^>]+)>");
        Matcher matcher = pattern.matcher(remitente);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return remitente;
    }

    /**
     * Valida que el pedido tenga la información mínima necesaria
     */
    private boolean validarPedido(PedidoDTO pedidoDTO) {
        if (pedidoDTO == null) {
            return false;
        }

        if (pedidoDTO.getFechaEntrega() == null) {
            log.warn("Pedido sin fecha de entrega");
            return false;
        }

        if (pedidoDTO.getItems() == null || pedidoDTO.getItems().isEmpty()) {
            log.warn("Pedido sin items");
            return false;
        }

        return true;
    }
}

