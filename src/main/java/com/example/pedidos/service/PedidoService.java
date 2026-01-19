package com.example.pedidos.service;

import com.example.pedidos.dto.PedidoDTO;
import com.example.pedidos.model.EstadoPedido;
import com.example.pedidos.model.ItemPedido;
import com.example.pedidos.model.Pedido;
import com.example.pedidos.repository.PedidoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final GoogleCalendarService calendarService;
    private final EmailNotificationService emailNotificationService;

    @Transactional
    public Pedido crearPedido(PedidoDTO pedidoDTO) {
        Pedido pedido = new Pedido();
        pedido.setNumeroPedido(generarNumeroPedido());
        pedido.setFechaRecepcion(LocalDateTime.now());
        pedido.setFechaEntrega(pedidoDTO.getFechaEntrega());
        pedido.setLugarEntrega(pedidoDTO.getLugarEntrega());
        pedido.setEmailOrigen(pedidoDTO.getEmailOrigen());
        pedido.setGmailMessageId(pedidoDTO.getGmailMessageId());
        pedido.setEstado(EstadoPedido.RECIBIDO);
        pedido.setMesPedido(LocalDateTime.now().getMonthValue());
        pedido.setAnioPedido(LocalDateTime.now().getYear());

        // Agregar items
        for (var itemDTO : pedidoDTO.getItems()) {
            ItemPedido item = new ItemPedido();
            item.setProducto(itemDTO.getProducto());
            item.setCantidad(itemDTO.getCantidad());
            item.setPrecioUnitario(itemDTO.getPrecioUnitario());
            item.calcularSubtotal();
            item.setPedido(pedido);
            pedido.getItems().add(item);
        }

        pedido.calcularTotal();
        pedido = pedidoRepository.save(pedido);

        log.info("Pedido creado: {}", pedido.getNumeroPedido());
        return pedido;
    }

    @Transactional
    public void agregarACalendario(Long pedidoId) throws Exception {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));

        String horario = pedido.getHorarioEntrega() != null ?
            pedido.getHorarioEntrega().toString() : "A confirmar";

        String detalles = String.format(
                "Pedido: %s\n" +
                "Cliente Facturación: %s\n" +
                "Destinatario Entrega: %s\n" +
                "Dirección: %s\n" +
                "Horario: %s\n" +
                "Total: $%.2f\n\n" +
                "Recordar:\n" +
                "- Solicitar turno a BlancaLuna\n" +
                "- Preparar Remito\n" +
                "- Preparar Etiqueta RNPA",
                pedido.getNumeroPedido(),
                pedido.getClienteFacturacion() != null ? pedido.getClienteFacturacion() : "N/A",
                pedido.getDestinatarioEntrega() != null ? pedido.getDestinatarioEntrega() : "N/A",
                pedido.getDireccionEntrega() != null ? pedido.getDireccionEntrega() : pedido.getLugarEntrega(),
                horario,
                pedido.getTotal()
        );

        String eventId = calendarService.crearEventoEntrega(
                pedido.getNumeroPedido(),
                pedido.getFechaEntrega(),
                pedido.getDireccionEntrega() != null ? pedido.getDireccionEntrega() : pedido.getLugarEntrega(),
                detalles
        );

        pedido.setGoogleCalendarEventId(eventId);
        pedido.setEstado(EstadoPedido.CALENDARIO_CREADO);
        pedidoRepository.save(pedido);

        log.info("Pedido agregado al calendario: {}", pedido.getNumeroPedido());
    }

    @Transactional
    public void enviarAPlanta(Long pedidoId) throws Exception {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));

        emailNotificationService.enviarNotificacionPlanta(pedido);

        pedido.setFechaEnvioPlanta(LocalDateTime.now());
        pedido.setEstado(EstadoPedido.ENVIADO_PLANTA);
        pedidoRepository.save(pedido);

        log.info("Pedido enviado a planta: {}", pedido.getNumeroPedido());
    }

    @Transactional
    public void marcarEnFabricacion(Long pedidoId) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));

        pedido.setEstado(EstadoPedido.EN_FABRICACION);
        pedidoRepository.save(pedido);

        log.info("Pedido en fabricación: {}", pedido.getNumeroPedido());
    }

    @Transactional
    public void solicitarTurnoBlancaLuna(Long pedidoId) throws Exception {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));

        emailNotificationService.solicitarTurnoBlancaLuna(pedido);

        pedido.setEstado(EstadoPedido.TURNO_SOLICITADO);
        pedidoRepository.save(pedido);

        log.info("Turno solicitado a BlancaLuna para pedido: {}", pedido.getNumeroPedido());
    }

    @Transactional
    public void confirmarTurno(Long pedidoId, String turno, LocalDateTime fechaTurno) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));

        pedido.setTurnoBlancaLuna(turno);
        pedido.setFechaTurno(fechaTurno);
        pedido.setEstado(EstadoPedido.TURNO_CONFIRMADO);
        pedidoRepository.save(pedido);

        log.info("Turno confirmado para pedido: {}", pedido.getNumeroPedido());
    }

    @Transactional
    public void marcarPreparadoParaEntrega(Long pedidoId) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));

        pedido.setRemitoGenerado(true);
        pedido.setEtiquetaRnpaGenerada(true);
        pedido.setEstado(EstadoPedido.PREPARADO_ENTREGA);
        pedidoRepository.save(pedido);

        log.info("Pedido preparado para entrega: {}", pedido.getNumeroPedido());
    }

    @Transactional
    public void marcarEntregado(Long pedidoId, LocalDate fechaEntregaReal) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));

        pedido.setFechaEntregaReal(fechaEntregaReal);
        pedido.setEstado(EstadoPedido.ENTREGADO);

        // Calcular fecha esperada de e-check (30 días desde entrega)
        pedido.setFechaEsperadaEcheck(fechaEntregaReal.plusDays(30));

        pedidoRepository.save(pedido);

        log.info("Pedido entregado: {}", pedido.getNumeroPedido());
    }

    @Transactional
    public void marcarDocumentosRecibidos(Long pedidoId) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));

        pedido.setDocumentosSelladosRecibidos(true);
        pedido.setEstado(EstadoPedido.DOCUMENTOS_RECIBIDOS);
        pedidoRepository.save(pedido);

        log.info("Documentos sellados recibidos para pedido: {}", pedido.getNumeroPedido());
    }

    @Transactional
    public void marcarFacturaDadaDeAlta(Long pedidoId, LocalDate fechaAlta) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));

        pedido.setFechaAltaFactura(fechaAlta);
        pedido.setEstado(EstadoPedido.FACTURA_DADA_ALTA);
        pedidoRepository.save(pedido);

        log.info("Factura dada de alta para pedido: {}", pedido.getNumeroPedido());
    }

    @Transactional
    public void marcarEcheckRecibido(Long pedidoId, LocalDate fechaRecepcion) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));

        pedido.setFechaRecepcionEcheck(fechaRecepcion);
        pedido.setEstado(EstadoPedido.ECHECK_RECIBIDO);

        // Calcular fecha de cobro (60 días desde entrega)
        if (pedido.getFechaEntregaReal() != null) {
            pedido.setFechaCobro(pedido.getFechaEntregaReal().plusDays(60));
        }

        pedidoRepository.save(pedido);

        log.info("E-check recibido para pedido: {}", pedido.getNumeroPedido());
    }

    @Transactional
    public void marcarCobrado(Long pedidoId) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));

        pedido.setEstado(EstadoPedido.COBRADO);
        pedido.calcularComision();
        pedidoRepository.save(pedido);

        log.info("Pedido cobrado. Comisión 8%: ${} - Pedido: {}",
                pedido.getComision(), pedido.getNumeroPedido());
    }

    @Transactional
    public void finalizarPedido(Long pedidoId) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));

        pedido.setEstado(EstadoPedido.FINALIZADO);
        pedidoRepository.save(pedido);

        log.info("Pedido finalizado: {}", pedido.getNumeroPedido());
    }

    public List<Pedido> obtenerPedidosPorMes(int mes, int anio) {
        return pedidoRepository.findByMesPedidoAndAnioPedido(mes, anio);
    }

    public List<Pedido> obtenerPedidosPorAnio(int anio) {
        return pedidoRepository.findByAnioPedidoOrderByFechaRecepcionDesc(anio);
    }

    public Optional<Pedido> obtenerPedidoPorId(Long id) {
        return pedidoRepository.findById(id);
    }

    public List<Pedido> obtenerTodosPedidos() {
        return pedidoRepository.findAll();
    }

    private String generarNumeroPedido() {
        LocalDateTime now = LocalDateTime.now();
        long count = pedidoRepository.count() + 1;
        return String.format("PED-%04d-%02d-%05d",
                now.getYear(), now.getMonthValue(), count);
    }
}

