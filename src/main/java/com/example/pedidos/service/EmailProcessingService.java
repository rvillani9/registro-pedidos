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

    /**
     * Procesa emails de pedidos no leídos
     */
    public void procesarEmailsPedidos() {
        try {
            // Buscar emails no leídos con asunto que contenga "pedido"
            String query = "is:unread subject:pedido";
            List<Message> messages = gmailService.leerEmailsPedidos(query);

            log.info("Encontrados {} emails de pedidos sin procesar", messages.size());

            for (Message message : messages) {
                try {
                    procesarEmailPedido(message);
                    gmailService.marcarComoLeido(message.getId());
                } catch (Exception e) {
                    log.error("Error procesando email {}: {}", message.getId(), e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            log.error("Error al procesar emails de pedidos", e);
        }
    }

    /**
     * Procesa un email individual de pedido
     */
    private void procesarEmailPedido(Message message) throws Exception {
        String cuerpo = gmailService.extraerCuerpoEmail(message);
        String remitente = gmailService.extraerRemitente(message);

        log.info("Procesando pedido de: {}", remitente);

        // Parsear el email para extraer información
        PedidoDTO pedidoDTO = parsearEmailPedido(cuerpo, remitente, message.getId());

        if (pedidoDTO != null && validarPedido(pedidoDTO)) {
            // Crear el pedido
            Pedido pedido = pedidoService.crearPedido(pedidoDTO);

            // Agregarlo al calendario
            pedidoService.agregarACalendario(pedido.getId());

            // Enviarlo a la planta
            pedidoService.enviarAPlanta(pedido.getId());

            log.info("Pedido procesado exitosamente: {}", pedido.getNumeroPedido());
        } else {
            log.warn("Pedido inválido o información incompleta en email: {}", message.getId());
        }
    }

    /**
     * Parsea el contenido del email para extraer información del pedido
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
        String texto = doc.text();

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

