package com.upeu.pedido.client;

import com.upeu.pedido.dto.PagoRequestDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "pago")
public interface PagoClient {

	@PostMapping("/api/v1/pagos")
	void crearPago(@RequestBody PagoRequestDto request);
}
