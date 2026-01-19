package com.example.pedidos.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class PedidoDTO {
    private LocalDate fechaEntrega;
    private String lugarEntrega;
    private String emailOrigen;
    private String gmailMessageId;
    private List<ItemPedidoDTO> items;
}

