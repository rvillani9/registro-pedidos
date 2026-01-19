package com.example.pedidos.service;

import com.example.pedidos.model.EstadoPedido;
import com.example.pedidos.model.Pedido;
import com.example.pedidos.repository.PedidoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledTasksService {

    private final EmailProcessingService emailProcessingService;
    private final PedidoRepository pedidoRepository;
    private final EmailNotificationService emailNotificationService;

    /**
     * Procesa emails de pedidos cada 10 minutos
     */
    @Scheduled(fixedDelay = 600000) // 10 minutos
    public void procesarEmailsPedidos() {
        log.info("Iniciando procesamiento de emails de pedidos...");
        emailProcessingService.procesarEmailsPedidos();
    }

    /**
     * Envía recordatorios a la planta si no hay respuesta (cada hora)
     * Se envía si pasaron más de 24 horas sin respuesta
     */
    @Scheduled(cron = "0 0 * * * *") // Cada hora
    @Transactional
    public void enviarRecordatoriosPlanta() {
        log.info("Verificando pedidos pendientes de respuesta de planta...");

        LocalDateTime hace24Horas = LocalDateTime.now().minusHours(24);
        List<Pedido> pedidosPendientes = pedidoRepository.findPedidosParaRecordatorioPlanta(hace24Horas);

        for (Pedido pedido : pedidosPendientes) {
            try {
                emailNotificationService.enviarRecordatorioPlanta(pedido);
                pedido.setFechaRecordatorioPlanta(LocalDateTime.now());
                pedido.setEstado(EstadoPedido.RECORDATORIO_PLANTA_ENVIADO);
                pedidoRepository.save(pedido);
                log.info("Recordatorio enviado a planta para pedido: {}", pedido.getNumeroPedido());
            } catch (Exception e) {
                log.error("Error enviando recordatorio a planta para pedido {}: {}",
                        pedido.getNumeroPedido(), e.getMessage());
            }
        }
    }

    /**
     * Envía recordatorios de logística 48 horas antes de la entrega (cada 6 horas)
     */
    @Scheduled(cron = "0 0 */6 * * *") // Cada 6 horas
    @Transactional
    public void enviarRecordatoriosLogistica() {
        log.info("Verificando entregas próximas para recordatorios de logística...");

        LocalDate en48Horas = LocalDate.now().plusDays(2);
        List<Pedido> pedidosProximos = pedidoRepository.findPedidosParaRecordatorioLogistica(en48Horas);

        for (Pedido pedido : pedidosProximos) {
            try {
                emailNotificationService.enviarRecordatorioLogistica(pedido);
                pedido.setFechaRecordatorioLogistica(LocalDateTime.now());
                pedido.setEstado(EstadoPedido.RECORDATORIO_LOGISTICA_ENVIADO);
                pedidoRepository.save(pedido);
                log.info("Recordatorio de logística enviado para pedido: {}", pedido.getNumeroPedido());
            } catch (Exception e) {
                log.error("Error enviando recordatorio de logística para pedido {}: {}",
                        pedido.getNumeroPedido(), e.getMessage());
            }
        }
    }

    /**
     * Verifica pedidos esperando e-check (diariamente a las 9:00)
     */
    @Scheduled(cron = "0 0 9 * * *") // Diariamente a las 9 AM
    @Transactional
    public void verificarEchecks() {
        log.info("Verificando pedidos esperando e-check...");

        LocalDate hoy = LocalDate.now();
        List<Pedido> pedidosEsperandoEcheck = pedidoRepository.findPedidosEsperandoEcheck(hoy);

        for (Pedido pedido : pedidosEsperandoEcheck) {
            log.info("Pedido {} esperando e-check desde {}. Fecha esperada: {}",
                    pedido.getNumeroPedido(),
                    pedido.getFechaEntregaReal(),
                    pedido.getFechaEsperadaEcheck());

            // Actualizar estado si corresponde
            if (pedido.getFechaEsperadaEcheck().isBefore(hoy)) {
                pedido.setEstado(EstadoPedido.ECHECK_PENDIENTE);
                pedidoRepository.save(pedido);
            }
        }
    }

    /**
     * Reporte diario de pedidos (diariamente a las 8:00)
     */
    @Scheduled(cron = "0 0 8 * * *") // Diariamente a las 8 AM
    public void generarReporteDiario() {
        log.info("=== REPORTE DIARIO DE PEDIDOS ===");

        for (EstadoPedido estado : EstadoPedido.values()) {
            List<Pedido> pedidos = pedidoRepository.findByEstado(estado);
            if (!pedidos.isEmpty()) {
                log.info("{}: {} pedidos", estado.getDescripcion(), pedidos.size());
            }
        }

        log.info("=================================");
    }
}

