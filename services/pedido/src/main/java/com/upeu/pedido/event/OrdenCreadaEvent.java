package com.upeu.pedido.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrdenCreadaEvent {

    private Long ordenId;
    private String cliente;
    private String estado;
    private String observacion;
    private Instant ocurridoEn;
}
