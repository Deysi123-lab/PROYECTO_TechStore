package com.upeu.pago.consumer;

import com.upeu.pago.entity.Pago;
import com.upeu.pago.event.OrdenCreadaEvent;
import com.upeu.pago.repository.PagoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrdenEventConsumer {

    private final PagoRepository pagoRepository;

    @KafkaListener(
            topics = "${app.kafka.topics.orden-eventos:orden-eventos}",
            groupId = "${spring.kafka.consumer.group-id:pago-consumer}"
    )
    @Transactional
    public void consumirOrdenCreada(OrdenCreadaEvent event) {
        log.info("[PAGO] Evento OrdenCreada recibido: ordenId={}, cliente={}, estado={}",
                event.getOrdenId(), event.getCliente(), event.getEstado());

        Pago pago = Pago.builder()
                .idPedido(event.getOrdenId())
                .monto(BigDecimal.ZERO)
                .metodo("PENDIENTE")
                .estado("PENDIENTE")
                .build();
        pagoRepository.save(pago);

        log.info("[PAGO] Registro de pago PENDIENTE creado para pedido {}", event.getOrdenId());
    }
}
