package com.example.pedidos.service;

import com.example.pedidos.model.Pedido;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationService {

    private final GmailService gmailService;

    @Value("${app.email.planta:planta@ejemplo.com}")
    private String emailPlanta;

    @Value("${app.email.blancaluna:blancaluna@ejemplo.com}")
    private String emailBlancaLuna;

    /**
     * Envía notificación a la planta
     */
    public void enviarNotificacionPlanta(Pedido pedido) throws Exception {
        StringBuilder cuerpo = new StringBuilder();
        cuerpo.append("NUEVO PEDIDO PARA FABRICACIÓN\n\n");
        cuerpo.append("Número de Pedido: ").append(pedido.getNumeroPedido()).append("\n");
        cuerpo.append("Fecha de Entrega: ").append(pedido.getFechaEntrega().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("\n");
        cuerpo.append("Lugar de Entrega: ").append(pedido.getLugarEntrega()).append("\n");
        cuerpo.append("Total: $").append(pedido.getTotal()).append("\n\n");

        cuerpo.append("PRODUCTOS:\n");
        cuerpo.append("----------------------------------------\n");
        pedido.getItems().forEach(item -> {
            cuerpo.append(String.format("- %s x%d - $%.2f c/u = $%.2f\n",
                    item.getProducto(),
                    item.getCantidad(),
                    item.getPrecioUnitario(),
                    item.getSubtotal()));
        });
        cuerpo.append("----------------------------------------\n");
        cuerpo.append("TOTAL: $").append(pedido.getTotal()).append("\n\n");
        cuerpo.append("Por favor confirmar recepción y tiempo estimado de fabricación.");

        gmailService.enviarEmail(
                emailPlanta,
                "Nuevo Pedido - " + pedido.getNumeroPedido(),
                cuerpo.toString()
        );

        log.info("Notificación enviada a planta para pedido: {}", pedido.getNumeroPedido());
    }

    /**
     * Envía recordatorio a la planta
     */
    public void enviarRecordatorioPlanta(Pedido pedido) throws Exception {
        String cuerpo = String.format(
                "RECORDATORIO - Pedido pendiente de respuesta\n\n" +
                "Número de Pedido: %s\n" +
                "Enviado el: %s\n" +
                "Fecha de Entrega: %s\n\n" +
                "Por favor confirmar estado del pedido.",
                pedido.getNumeroPedido(),
                pedido.getFechaEnvioPlanta().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                pedido.getFechaEntrega().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        );

        gmailService.enviarEmail(
                emailPlanta,
                "RECORDATORIO - Pedido " + pedido.getNumeroPedido(),
                cuerpo
        );

        log.info("Recordatorio enviado a planta para pedido: {}", pedido.getNumeroPedido());
    }

    /**
     * Envía recordatorio de logística 48hs antes de la entrega
     */
    public void enviarRecordatorioLogistica(Pedido pedido) throws Exception {
        StringBuilder cuerpo = new StringBuilder();
        cuerpo.append("RECORDATORIO - Entrega en 48 horas\n\n");
        cuerpo.append("Número de Pedido: ").append(pedido.getNumeroPedido()).append("\n");
        cuerpo.append("Fecha de Entrega: ").append(pedido.getFechaEntrega().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("\n");
        cuerpo.append("Lugar de Entrega: ").append(pedido.getLugarEntrega()).append("\n\n");

        cuerpo.append("CHECKLIST PARA LA ENTREGA:\n");
        cuerpo.append("☐ Turno confirmado con BlancaLuna: ").append(pedido.getTurnoBlancaLuna() != null ? pedido.getTurnoBlancaLuna() : "PENDIENTE").append("\n");
        cuerpo.append("☐ Remito generado: ").append(pedido.getRemitoGenerado() ? "SÍ" : "NO").append("\n");
        cuerpo.append("☐ Etiqueta RNPA: ").append(pedido.getEtiquetaRnpaGenerada() ? "SÍ" : "NO").append("\n\n");

        cuerpo.append("RECORDAR:\n");
        cuerpo.append("- Llevar Remito (2 copias para sellar)\n");
        cuerpo.append("- Llevar Factura (para sellar)\n");
        cuerpo.append("- Etiqueta RNPA en los productos\n");
        cuerpo.append("- Confirmar turno en BlancaLuna\n");

        gmailService.enviarEmail(
                "logistica@ejemplo.com", // Cambiar según corresponda
                "Recordatorio Logística - " + pedido.getNumeroPedido() + " - Entrega en 48hs",
                cuerpo.toString()
        );

        log.info("Recordatorio de logística enviado para pedido: {}", pedido.getNumeroPedido());
    }

    /**
     * Solicita turno a BlancaLuna
     */
    public void solicitarTurnoBlancaLuna(Pedido pedido) throws Exception {
        String cuerpo = String.format(
                "Solicitud de Turno - Centro de Distribución\n\n" +
                "Número de Pedido: %s\n" +
                "Fecha de Entrega Solicitada: %s\n" +
                "Destino Final: %s\n" +
                "Total del Pedido: $%.2f\n\n" +
                "Por favor confirmar disponibilidad de turno para carga/entrega.\n\n" +
                "Gracias.",
                pedido.getNumeroPedido(),
                pedido.getFechaEntrega().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                pedido.getLugarEntrega(),
                pedido.getTotal()
        );

        gmailService.enviarEmail(
                emailBlancaLuna,
                "Solicitud de Turno - Pedido " + pedido.getNumeroPedido(),
                cuerpo
        );

        log.info("Solicitud de turno enviada a BlancaLuna para pedido: {}", pedido.getNumeroPedido());
    }
}

