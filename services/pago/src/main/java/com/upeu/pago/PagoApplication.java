package com.upeu.pago;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class PagoApplication {

	public static void main(String[] args) {
		SpringApplication.run(PagoApplication.class, args);
	}
}
