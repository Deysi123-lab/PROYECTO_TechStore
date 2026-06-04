package com.upeu.pago.mapper;

import com.upeu.pago.dto.PagoRequest;
import com.upeu.pago.dto.PagoResponse;
import com.upeu.pago.entity.Pago;
import org.springframework.stereotype.Component;

@Component
public class PagoMapper {

	public Pago toEntity(PagoRequest request) {
		if (request == null) {
			return null;
		}
		return Pago.builder()
				.idPedido(request.getIdPedido())
				.monto(request.getMonto())
				.metodo(request.getMetodo())
				.estado(request.getEstado())
				.build();
	}

	public PagoResponse toResponse(Pago entity) {
		if (entity == null) {
			return null;
		}
		return PagoResponse.builder()
				.id(entity.getId())
				.idPedido(entity.getIdPedido())
				.monto(entity.getMonto())
				.metodo(entity.getMetodo())
				.estado(entity.getEstado())
				.build();
	}

	public void updateEntityFromRequest(Pago entity, PagoRequest request) {
		entity.setIdPedido(request.getIdPedido());
		entity.setMonto(request.getMonto());
		entity.setMetodo(request.getMetodo());
		entity.setEstado(request.getEstado());
	}
}
