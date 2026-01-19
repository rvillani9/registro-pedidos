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
     * Formato Aramark: SKU Descripción Precio Cant. Total
     * Ejemplo: PN001643 PLANCHA DE PASTA FROLA DE MEMBRILLO 30 X 40 18.500,00 16,00 296.000,00
     */
    public List<ItemPedidoDTO> extraerItemsDelPdf(String textoPdf) {
        List<ItemPedidoDTO> items = new ArrayList<>();

        log.info("=== INICIANDO EXTRACCIÓN DE ITEMS DEL PDF ===");

        // Dividir el texto en líneas
        String[] lineas = textoPdf.split("\n");

        // Patrón para formato Aramark con números en formato argentino
        // PN001643 PLANCHA DE PASTA FROLA DE MEMBRILLO 30 X 40 18.500,00 16,00 296.000,00
        Pattern patronAramark = Pattern.compile(
            "^([A-Z]{2}[0-9]{6})\\s+(.+?)\\s+([0-9]{1,3}(?:\\.[0-9]{3})*,[0-9]{2})\\s+([0-9]{1,3}(?:,[0-9]{2})?)\\s+([0-9]{1,3}(?:\\.[0-9]{3})*,[0-9]{2})\\s*$"
        );

        for (String linea : lineas) {
            linea = linea.trim();

            // Saltar líneas vacías o encabezados
            if (linea.isEmpty() ||
                linea.toLowerCase().contains("sku") ||
                linea.toLowerCase().contains("subtotal") ||
                linea.toLowerCase().contains("total neto") ||
                linea.toLowerCase().contains("condiciones")) {
                continue;
            }

            Matcher matcher = patronAramark.matcher(linea);
            if (matcher.matches()) {
                try {
                    ItemPedidoDTO item = new ItemPedidoDTO();

                    // Grupo 1: SKU
                    String sku = matcher.group(1);

                    // Grupo 2: Descripción del producto
                    item.setProducto(matcher.group(2).trim());

                    // Grupo 3: Precio Unitario (18.500,00 -> 18500.00)
                    String precioStr = matcher.group(3)
                        .replace(".", "")   // Quitar separadores de miles
                        .replace(",", "."); // Convertir coma decimal a punto
                    item.setPrecioUnitario(new BigDecimal(precioStr));

                    // Grupo 4: Cantidad (16,00 -> 16)
                    String cantidadStr = matcher.group(4).replace(",", ".");
                    item.setCantidad((int) Double.parseDouble(cantidadStr));

                    // Validar
                    if (item.getProducto() != null && !item.getProducto().isEmpty() &&
                        item.getCantidad() != null && item.getCantidad() > 0 &&
                        item.getPrecioUnitario() != null && item.getPrecioUnitario().compareTo(BigDecimal.ZERO) > 0) {

                        items.add(item);
                        BigDecimal subtotal = item.getPrecioUnitario().multiply(new BigDecimal(item.getCantidad()));
                        log.info("✅ {} | {} | Precio: ${} | Cant: {} | Subtotal: ${}",
                            sku, item.getProducto(), item.getPrecioUnitario(), item.getCantidad(), subtotal);
                    }
                } catch (Exception e) {
                    log.error("❌ Error procesando línea: {} | Error: {}", linea, e.getMessage());
                }
            } else {
                // Log líneas que empiezan con SKU pero no se procesaron
                if (linea.matches("^[A-Z]{2}[0-9]{6}.*")) {
                    log.warn("⚠️ Línea no procesada: {}", linea);
                }
            }
        }

        log.info("=== ✅ EXTRACCIÓN COMPLETADA: {} items encontrados ===", items.size());

        // Si no se encontró nada, mostrar debugging
        if (items.isEmpty()) {
            log.error("❌ NO SE ENCONTRARON ITEMS - Primeras 15 líneas del PDF:");
            for (int i = 0; i < Math.min(15, lineas.length); i++) {
                log.error("Línea {}: [{}]", i, lineas[i]);
            }
        }

        return items;
    }

    /**
     * Extrae el total del pedido del texto del PDF
     * Formato Aramark: "Total Neto $ 1.465.500,00"
     */
    public BigDecimal extraerTotalDelPdf(String textoPdf) {
        Pattern[] patrones = {
            Pattern.compile("Total Neto\\s+\\$?\\s*([0-9.]+,[0-9]{2})", Pattern.CASE_INSENSITIVE),
            Pattern.compile("Subtotal\\s+\\$?\\s*([0-9.]+,[0-9]{2})", Pattern.CASE_INSENSITIVE),
            Pattern.compile("TOTAL[:\\s]+\\$?\\s*([0-9.]+,[0-9]{2})", Pattern.CASE_INSENSITIVE)
        };

        for (Pattern patron : patrones) {
            Matcher matcher = patron.matcher(textoPdf);
            if (matcher.find()) {
                try {
                    // Formato argentino: 1.465.500,00 -> 1465500.00
                    String totalStr = matcher.group(1)
                        .replace(".", "")   // Eliminar separadores de miles
                        .replace(",", "."); // Convertir coma decimal a punto
                    BigDecimal total = new BigDecimal(totalStr);
                    log.info("💰 Total Neto extraído del PDF: ${}", total);
                    return total;
                } catch (Exception e) {
                    log.warn("Error parseando total: {} - {}", matcher.group(1), e.getMessage());
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
