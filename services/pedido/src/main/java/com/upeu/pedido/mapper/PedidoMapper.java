package com.upeu.pedido.mapper;

import com.upeu.pedido.dto.PedidoRequest;
import com.upeu.pedido.dto.PedidoResponse;
import com.upeu.pedido.entity.Pedido;
import org.springframework.stereotype.Component;

@Component
public class PedidoMapper {

	public Pedido toEntity(PedidoRequest request) {
		if (request == null) {
			return null;
		}
		return Pedido.builder()
				.cliente(request.getCliente())
				.estado(request.getEstado())
				.observacion(request.getObservacion())
				.build();
	}

	public PedidoResponse toResponse(Pedido entity) {
		if (entity == null) {
			return null;
		}
		return PedidoResponse.builder()
				.id(entity.getId())
				.cliente(entity.getCliente())
				.estado(entity.getEstado())
				.observacion(entity.getObservacion())
				.build();
	}

	public void updateEntityFromRequest(Pedido entity, PedidoRequest request) {
		entity.setCliente(request.getCliente());
		entity.setEstado(request.getEstado());
		entity.setObservacion(request.getObservacion());
	}
}
