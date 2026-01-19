package com.example.pedidos.repository;

import com.example.pedidos.model.EstadoPedido;
import com.example.pedidos.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    Optional<Pedido> findByGmailMessageId(String gmailMessageId);

    List<Pedido> findByEstado(EstadoPedido estado);

    List<Pedido> findByMesPedidoAndAnioPedido(Integer mes, Integer anio);

    @Query("SELECT p FROM Pedido p WHERE p.estado = :estado AND p.fechaEntrega BETWEEN :fechaInicio AND :fechaFin")
    List<Pedido> findByEstadoAndFechaEntregaBetween(
            @Param("estado") EstadoPedido estado,
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin
    );

    @Query("SELECT p FROM Pedido p WHERE p.estado = 'ENVIADO_PLANTA' AND p.fechaEnvioPlanta < :fecha AND p.fechaRecordatorioPlanta IS NULL")
    List<Pedido> findPedidosParaRecordatorioPlanta(@Param("fecha") java.time.LocalDateTime fecha);

    @Query("SELECT p FROM Pedido p WHERE p.estado IN ('EN_FABRICACION', 'TURNO_CONFIRMADO', 'PREPARADO_ENTREGA') " +
           "AND p.fechaEntrega = :fechaEntrega AND p.fechaRecordatorioLogistica IS NULL")
    List<Pedido> findPedidosParaRecordatorioLogistica(@Param("fechaEntrega") LocalDate fechaEntrega);

    @Query("SELECT p FROM Pedido p WHERE p.estado = 'FACTURA_DADA_ALTA' AND p.fechaEsperadaEcheck <= :fecha")
    List<Pedido> findPedidosEsperandoEcheck(@Param("fecha") LocalDate fecha);

    List<Pedido> findByAnioPedidoOrderByFechaRecepcionDesc(Integer anio);
}

