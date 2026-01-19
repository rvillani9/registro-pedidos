package com.example.pedidos.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ItemPedidoDTO {
    private String producto;
    private Integer cantidad;
    private BigDecimal precioUnitario;
}

