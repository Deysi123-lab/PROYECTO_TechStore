package com.upeu.pago.service.impl;

import com.upeu.pago.dto.PagoRequest;
import com.upeu.pago.dto.PagoResponse;
import com.upeu.pago.entity.Pago;
import com.upeu.pago.exception.ResourceNotFoundException;
import com.upeu.pago.mapper.PagoMapper;
import com.upeu.pago.repository.PagoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PagoServiceImplTest {

	@Mock
	private PagoRepository pagoRepository;

	@Spy
	private PagoMapper pagoMapper = new PagoMapper();

	@InjectMocks
	private PagoServiceImpl pagoService;

	@Test
	void shouldCreatePago() {
		PagoRequest request = PagoRequest.builder()
				.idPedido(1L)
				.monto(new BigDecimal("50.00"))
				.metodo("TARJETA")
				.estado("PENDIENTE")
				.build();
		Pago saved = Pago.builder()
				.id(1L)
				.idPedido(1L)
				.monto(new BigDecimal("50.00"))
				.metodo("TARJETA")
				.estado("PENDIENTE")
				.build();
		when(pagoRepository.save(any(Pago.class))).thenReturn(saved);

		PagoResponse response = pagoService.create(request);

		assertThat(response.getId()).isEqualTo(1L);
		assertThat(response.getMonto()).isEqualByComparingTo("50.00");
	}

	@Test
	void shouldThrowWhenPagoNotFound() {
		when(pagoRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> pagoService.findById(99L))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("99");
	}
}
