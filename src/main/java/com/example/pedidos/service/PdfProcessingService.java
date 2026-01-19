package com.example.pedidos.service;

import com.example.pedidos.dto.ItemPedidoDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class PdfProcessingService {

    /**
     * Extrae el texto completo de un PDF
     */
    public String extraerTextoDePdf(byte[] pdfBytes) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String texto = stripper.getText(document);
            log.debug("Texto extraído del PDF ({} caracteres)", texto.length());
            return texto;
        }
    }

    /**
     * Extrae items de pedido del texto del PDF
     * Busca patrones comunes en tablas de productos
     */
    public List<ItemPedidoDTO> extraerItemsDelPdf(String textoPdf) {
        List<ItemPedidoDTO> items = new ArrayList<>();

        // Dividir el texto en líneas
        String[] lineas = textoPdf.split("\n");

        // Patrones para detectar líneas de productos
        // Formato esperado: Producto | Cantidad | Precio Unitario | Total
        // Ejemplos:
        // "Producto A    50    150.00    7500.00"
        // "Producto B | 100 | $85.50 | $8,550.00"

        Pattern patronProducto = Pattern.compile(
            "^(.+?)\\s+([0-9]+)\\s+\\$?([0-9,.]+)\\s+\\$?([0-9,.]+)$"
        );

        // Patrón alternativo con separadores (pipes, tabs, etc)
        Pattern patronProductoAlt = Pattern.compile(
            "^(.+?)[|\\t]+\\s*([0-9]+)\\s*[|\\t]+\\s*\\$?([0-9,.]+)\\s*[|\\t]+\\s*\\$?([0-9,.]+)\\s*$"
        );

        for (String linea : lineas) {
            linea = linea.trim();

            // Saltar líneas vacías o encabezados
            if (linea.isEmpty() ||
                linea.toLowerCase().contains("producto") && linea.toLowerCase().contains("cantidad") ||
                linea.toLowerCase().contains("descripción") ||
                linea.toLowerCase().contains("item")) {
                continue;
            }

            Matcher matcher = patronProducto.matcher(linea);
            if (!matcher.matches()) {
                matcher = patronProductoAlt.matcher(linea);
            }

            if (matcher.matches()) {
                try {
                    ItemPedidoDTO item = new ItemPedidoDTO();

                    // Grupo 1: Nombre del producto
                    item.setProducto(matcher.group(1).trim());

                    // Grupo 2: Cantidad
                    item.setCantidad(Integer.parseInt(matcher.group(2).trim()));

                    // Grupo 3: Precio Unitario
                    String precioStr = matcher.group(3).trim().replace(",", "");
                    item.setPrecioUnitario(new BigDecimal(precioStr));

                    // Validar que tenga datos válidos
                    if (item.getProducto() != null && !item.getProducto().isEmpty() &&
                        item.getCantidad() != null && item.getCantidad() > 0 &&
                        item.getPrecioUnitario() != null && item.getPrecioUnitario().compareTo(BigDecimal.ZERO) > 0) {

                        items.add(item);
                        log.debug("Item extraído del PDF: {} x{} - ${}",
                            item.getProducto(), item.getCantidad(), item.getPrecioUnitario());
                    }
                } catch (Exception e) {
                    log.warn("Error procesando línea del PDF: {} - {}", linea, e.getMessage());
                }
            }
        }

        log.info("Extraídos {} items del PDF", items.size());
        return items;
    }

    /**
     * Extrae el total del pedido del texto del PDF
     */
    public BigDecimal extraerTotalDelPdf(String textoPdf) {
        // Buscar patrones como:
        // "TOTAL: $33,050.00"
        // "Total General: 33050.00"
        // "TOTAL    $33,050.00"

        Pattern[] patrones = {
            Pattern.compile("TOTAL[:\\s]+\\$?([0-9,]+\\.?[0-9]*)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("Total General[:\\s]+\\$?([0-9,]+\\.?[0-9]*)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("Importe Total[:\\s]+\\$?([0-9,]+\\.?[0-9]*)", Pattern.CASE_INSENSITIVE)
        };

        for (Pattern patron : patrones) {
            Matcher matcher = patron.matcher(textoPdf);
            if (matcher.find()) {
                try {
                    String totalStr = matcher.group(1).replace(",", "");
                    BigDecimal total = new BigDecimal(totalStr);
                    log.info("Total extraído del PDF: ${}", total);
                    return total;
                } catch (Exception e) {
                    log.warn("Error parseando total: {}", e.getMessage());
                }
            }
        }

        log.warn("No se pudo extraer el total del PDF");
        return null;
    }

    /**
     * Extrae el número de orden de compra (OC) del PDF
     */
    public String extraerNumeroOrdenCompra(String textoPdf) {
        // Buscar patrones como:
        // "OC 113648"
        // "Orden de Compra: 113648"
        // "O.C. Nº 113648"

        Pattern[] patrones = {
            Pattern.compile("OC[\\s#:Nº]*([0-9]+)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("Orden de Compra[\\s#:Nº]*([0-9]+)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("O\\.C\\.[\\s#:Nº]*([0-9]+)", Pattern.CASE_INSENSITIVE)
        };

        for (Pattern patron : patrones) {
            Matcher matcher = patron.matcher(textoPdf);
            if (matcher.find()) {
                String numeroOC = matcher.group(1);
                log.info("Número de OC extraído del PDF: {}", numeroOC);
                return numeroOC;
            }
        }

        log.warn("No se pudo extraer el número de OC del PDF");
        return null;
    }
}

