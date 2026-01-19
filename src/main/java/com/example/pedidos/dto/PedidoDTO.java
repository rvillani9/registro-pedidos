package com.example.pedidos.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
public class PedidoDTO {
    private LocalDate fechaEntrega;
    private LocalTime horarioEntrega;
    private String clienteFacturacion;
    private String destinatarioEntrega;
    private String direccionEntrega;
    private String lugarEntrega;
    private String emailOrigen;
    private String gmailMessageId;
    private List<ItemPedidoDTO> items;
}

