package com.upeu.pedido.service.impl;

import com.upeu.pedido.dto.PedidoRequest;
import com.upeu.pedido.dto.PedidoResponse;
import com.upeu.pedido.entity.Pedido;
import com.upeu.pedido.event.OrdenCreadaEvent;
import com.upeu.pedido.exception.ResourceNotFoundException;
import com.upeu.pedido.mapper.PedidoMapper;
import com.upeu.pedido.producer.OrdenEventProducer;
import com.upeu.pedido.repository.PedidoRepository;
import com.upeu.pedido.service.PedidoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PedidoServiceImpl implements PedidoService {

	private final PedidoRepository pedidoRepository;
	private final PedidoMapper pedidoMapper;
	private final OrdenEventProducer ordenEventProducer;

	@Override
	@Transactional
	public PedidoResponse create(PedidoRequest request) {
		log.info("Creación de pedido para cliente: {}", request.getCliente());
		Pedido pedido = pedidoMapper.toEntity(request);
		Pedido saved = pedidoRepository.save(pedido);

		OrdenCreadaEvent event = OrdenCreadaEvent.builder()
				.ordenId(saved.getId())
				.cliente(saved.getCliente())
				.estado(saved.getEstado())
				.observacion(saved.getObservacion())
				.ocurridoEn(Instant.now())
				.build();
		ordenEventProducer.publicarOrdenCreada(event);

		return pedidoMapper.toResponse(saved);
	}

	@Override
	@Transactional(readOnly = true)
	public List<PedidoResponse> findAll() {
		return pedidoRepository.findAll().stream()
				.map(pedidoMapper::toResponse)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public PedidoResponse findById(Long id) {
		Pedido pedido = getPedidoById(id);
		return pedidoMapper.toResponse(pedido);
	}

	@Override
	@Transactional
	public PedidoResponse update(Long id, PedidoRequest request) {
		Pedido pedido = getPedidoById(id);
		pedidoMapper.updateEntityFromRequest(pedido, request);
		return pedidoMapper.toResponse(pedidoRepository.save(pedido));
	}

	@Override
	@Transactional
	public void delete(Long id) {
		getPedidoById(id);
		pedidoRepository.deleteById(id);
	}

	private Pedido getPedidoById(Long id) {
		return pedidoRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Pedido con id " + id + " no encontrado"));
	}
}
