package com.example.pedidos.model;

public enum EstadoPedido {
    RECIBIDO("Pedido recibido por email"),
    CALENDARIO_CREADO("Agregado al calendario"),
    ENVIADO_PLANTA("Enviado a la planta"),
    RECORDATORIO_PLANTA_ENVIADO("Recordatorio enviado a la planta"),
    EN_FABRICACION("En fabricación"),
    RECORDATORIO_LOGISTICA_ENVIADO("Recordatorio de logística enviado (48hs antes)"),
    TURNO_SOLICITADO("Turno solicitado a BlancaLuna"),
    TURNO_CONFIRMADO("Turno confirmado con BlancaLuna"),
    PREPARADO_ENTREGA("Preparado para entrega (Remito, Etiqueta RNPA)"),
    ENTREGADO("Pedido entregado"),
    DOCUMENTOS_RECIBIDOS("Remito y factura sellados recibidos"),
    FACTURA_DADA_ALTA("Factura dada de alta"),
    ECHECK_PENDIENTE("Esperando e-check (30 días desde entrega)"),
    ECHECK_RECIBIDO("E-check recibido"),
    COBRADO("Cobrado (60 días desde entrega)"),
    COMISION_CALCULADA("Comisión calculada (8%)"),
    FINALIZADO("Pedido finalizado");

    private final String descripcion;

    EstadoPedido(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}

