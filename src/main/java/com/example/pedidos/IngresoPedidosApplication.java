package com.example.pedidos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class IngresoPedidosApplication {

    public static void main(String[] args) {
        SpringApplication.run(IngresoPedidosApplication.class, args);
    }
}
