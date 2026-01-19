package com.example.pedidos.controller;

import com.example.pedidos.dto.PedidoDTO;
import com.example.pedidos.model.EstadoPedido;
import com.example.pedidos.model.Pedido;
import com.example.pedidos.service.PedidoService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pedidos")
@RequiredArgsConstructor
public class PedidoController {

    private final PedidoService pedidoService;

    @GetMapping
    public ResponseEntity<List<Pedido>> obtenerTodos() {
        return ResponseEntity.ok(pedidoService.obtenerTodosPedidos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Pedido> obtenerPorId(@PathVariable Long id) {
        return pedidoService.obtenerPedidoPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/mes/{mes}/anio/{anio}")
    public ResponseEntity<List<Pedido>> obtenerPorMes(
            @PathVariable int mes,
            @PathVariable int anio) {
        return ResponseEntity.ok(pedidoService.obtenerPedidosPorMes(mes, anio));
    }

    @GetMapping("/anio/{anio}")
    public ResponseEntity<List<Pedido>> obtenerPorAnio(@PathVariable int anio) {
        return ResponseEntity.ok(pedidoService.obtenerPedidosPorAnio(anio));
    }

    @PostMapping
    public ResponseEntity<Pedido> crearPedido(@RequestBody PedidoDTO pedidoDTO) {
        Pedido pedido = pedidoService.crearPedido(pedidoDTO);
        return ResponseEntity.ok(pedido);
    }

    @PostMapping("/{id}/calendario")
    public ResponseEntity<Void> agregarACalendario(@PathVariable Long id) {
        try {
            pedidoService.agregarACalendario(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{id}/enviar-planta")
    public ResponseEntity<Void> enviarAPlanta(@PathVariable Long id) {
        try {
            pedidoService.enviarAPlanta(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/{id}/fabricacion")
    public ResponseEntity<Void> marcarEnFabricacion(@PathVariable Long id) {
        pedidoService.marcarEnFabricacion(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/solicitar-turno")
    public ResponseEntity<Void> solicitarTurno(@PathVariable Long id) {
        try {
            pedidoService.solicitarTurnoBlancaLuna(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/{id}/confirmar-turno")
    public ResponseEntity<Void> confirmarTurno(
            @PathVariable Long id,
            @RequestBody Map<String, Object> data) {
        String turno = (String) data.get("turno");
        LocalDateTime fechaTurno = LocalDateTime.parse((String) data.get("fechaTurno"));
        pedidoService.confirmarTurno(id, turno, fechaTurno);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/preparado-entrega")
    public ResponseEntity<Void> marcarPreparadoParaEntrega(@PathVariable Long id) {
        pedidoService.marcarPreparadoParaEntrega(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/entregado")
    public ResponseEntity<Void> marcarEntregado(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaEntrega) {
        pedidoService.marcarEntregado(id, fechaEntrega);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/documentos-recibidos")
    public ResponseEntity<Void> marcarDocumentosRecibidos(@PathVariable Long id) {
        pedidoService.marcarDocumentosRecibidos(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/factura-alta")
    public ResponseEntity<Void> marcarFacturaAlta(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaAlta) {
        pedidoService.marcarFacturaDadaDeAlta(id, fechaAlta);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/echeck-recibido")
    public ResponseEntity<Void> marcarEcheckRecibido(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaRecepcion) {
        pedidoService.marcarEcheckRecibido(id, fechaRecepcion);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/cobrado")
    public ResponseEntity<Void> marcarCobrado(@PathVariable Long id) {
        pedidoService.marcarCobrado(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/finalizar")
    public ResponseEntity<Void> finalizarPedido(@PathVariable Long id) {
        pedidoService.finalizarPedido(id);
        return ResponseEntity.ok().build();
    }
}

