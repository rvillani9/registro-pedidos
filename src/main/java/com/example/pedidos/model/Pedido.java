package com.example.pedidos.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pedidos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_pedido", unique = true)
    private String numeroPedido;

    @Column(name = "fecha_recepcion")
    private LocalDateTime fechaRecepcion;

    @Column(name = "fecha_entrega")
    private LocalDate fechaEntrega;

    @Column(name = "horario_entrega")
    private LocalTime horarioEntrega;

    @Column(name = "cliente_facturacion")
    private String clienteFacturacion;

    @Column(name = "destinatario_entrega")
    private String destinatarioEntrega;

    @Column(name = "direccion_entrega", length = 500)
    private String direccionEntrega;

    @Column(name = "lugar_entrega")
    private String lugarEntrega;

    @Column(name = "email_origen")
    private String emailOrigen;

    @Column(name = "gmail_message_id")
    private String gmailMessageId;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado")
    private EstadoPedido estado;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<ItemPedido> items = new ArrayList<>();

    @Column(name = "total", precision = 10, scale = 2)
    private BigDecimal total;

    @Column(name = "comision", precision = 10, scale = 2)
    private BigDecimal comision;

    @Column(name = "google_calendar_event_id")
    private String googleCalendarEventId;

    @Column(name = "fecha_envio_planta")
    private LocalDateTime fechaEnvioPlanta;

    @Column(name = "fecha_recordatorio_planta")
    private LocalDateTime fechaRecordatorioPlanta;

    @Column(name = "fecha_recordatorio_logistica")
    private LocalDateTime fechaRecordatorioLogistica;

    @Column(name = "turno_blancaluna")
    private String turnoBlancaLuna;

    @Column(name = "fecha_turno")
    private LocalDateTime fechaTurno;

    @Column(name = "remito_generado")
    private Boolean remitoGenerado = false;

    @Column(name = "etiqueta_rnpa_generada")
    private Boolean etiquetaRnpaGenerada = false;

    @Column(name = "fecha_entrega_real")
    private LocalDate fechaEntregaReal;

    @Column(name = "documentos_sellados_recibidos")
    private Boolean documentosSelladosRecibidos = false;

    @Column(name = "fecha_alta_factura")
    private LocalDate fechaAltaFactura;

    @Column(name = "fecha_esperada_echeck")
    private LocalDate fechaEsperadaEcheck;

    @Column(name = "fecha_recepcion_echeck")
    private LocalDate fechaRecepcionEcheck;

    @Column(name = "fecha_cobro")
    private LocalDate fechaCobro;

    @Column(name = "notas", length = 2000)
    private String notas;

    @Column(name = "mes_pedido")
    private Integer mesPedido;

    @Column(name = "anio_pedido")
    private Integer anioPedido;

    public void calcularTotal() {
        this.total = items.stream()
                .map(item -> item.getSubtotal())
                .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));
    }

    public void calcularComision() {
        if (this.total != null) {
            this.comision = this.total.multiply(new BigDecimal("0.08"));
        }
    }
}

