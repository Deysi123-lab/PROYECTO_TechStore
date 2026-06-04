package com.upeu.pedido.mapper;

import com.upeu.pedido.dto.PedidoRequest;
import com.upeu.pedido.dto.PedidoResponse;
import com.upeu.pedido.entity.Pedido;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PedidoMapperTest {

	private final PedidoMapper mapper = new PedidoMapper();

	@Test
	void shouldMapRequestToEntity() {
		PedidoRequest request = PedidoRequest.builder()
				.cliente("Ana")
				.estado("PENDIENTE")
				.observacion("Nota")
				.build();

		Pedido entity = mapper.toEntity(request);

		assertThat(entity).isNotNull();
		assertThat(entity.getCliente()).isEqualTo("Ana");
		assertThat(entity.getEstado()).isEqualTo("PENDIENTE");
	}

	@Test
	void shouldMapEntityToResponse() {
		Pedido entity = Pedido.builder()
				.id(1L)
				.cliente("Ana")
				.estado("PENDIENTE")
				.observacion("X")
				.build();

		PedidoResponse response = mapper.toResponse(entity);

		assertThat(response.getId()).isEqualTo(1L);
		assertThat(response.getCliente()).isEqualTo("Ana");
	}
}
