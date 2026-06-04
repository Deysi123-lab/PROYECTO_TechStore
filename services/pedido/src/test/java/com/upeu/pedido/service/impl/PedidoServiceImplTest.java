package com.upeu.pedido.service.impl;

import com.upeu.pedido.dto.PedidoRequest;
import com.upeu.pedido.dto.PedidoResponse;
import com.upeu.pedido.entity.Pedido;
import com.upeu.pedido.exception.ResourceNotFoundException;
import com.upeu.pedido.mapper.PedidoMapper;
import com.upeu.pedido.repository.PedidoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PedidoServiceImplTest {

	@Mock
	private PedidoRepository pedidoRepository;

	@Spy
	private PedidoMapper pedidoMapper = new PedidoMapper();

	@InjectMocks
	private PedidoServiceImpl pedidoService;

	@Test
	void shouldCreatePedido() {
		PedidoRequest request = PedidoRequest.builder()
				.cliente("Ana")
				.estado("PENDIENTE")
				.observacion("Urgente")
				.build();
		Pedido saved = Pedido.builder()
				.id(1L)
				.cliente("Ana")
				.estado("PENDIENTE")
				.observacion("Urgente")
				.build();
		when(pedidoRepository.save(any(Pedido.class))).thenReturn(saved);

		PedidoResponse response = pedidoService.create(request);

		assertThat(response.getId()).isEqualTo(1L);
		assertThat(response.getCliente()).isEqualTo("Ana");
	}

	@Test
	void shouldThrowWhenPedidoNotFound() {
		when(pedidoRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> pedidoService.findById(99L))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("99");
	}
}
