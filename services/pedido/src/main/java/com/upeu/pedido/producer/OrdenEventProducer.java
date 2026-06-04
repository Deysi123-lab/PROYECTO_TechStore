package com.upeu.pedido.producer;

import com.upeu.pedido.event.OrdenCreadaEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrdenEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topics.orden-eventos:orden-eventos}")
    private String ordenEventosTopic;

    public void publicarOrdenCreada(OrdenCreadaEvent event) {
        log.info("[PEDIDO] Publicando evento OrdenCreada al topic {} -> {}", ordenEventosTopic, event);
        kafkaTemplate.send(ordenEventosTopic, String.valueOf(event.getOrdenId()), event);
    }
}
