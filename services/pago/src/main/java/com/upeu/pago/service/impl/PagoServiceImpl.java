package com.upeu.pago.service.impl;

import com.upeu.pago.dto.PagoRequest;
import com.upeu.pago.dto.PagoResponse;
import com.upeu.pago.entity.Pago;
import com.upeu.pago.exception.ResourceNotFoundException;
import com.upeu.pago.mapper.PagoMapper;
import com.upeu.pago.repository.PagoRepository;
import com.upeu.pago.service.PagoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PagoServiceImpl implements PagoService {

	private final PagoRepository pagoRepository;
	private final PagoMapper pagoMapper;

	@Override
	@Transactional
	public PagoResponse create(PagoRequest request) {
		log.info("Registro de pago para pedido id={}", request.getIdPedido());
		Pago pago = pagoMapper.toEntity(request);
		return pagoMapper.toResponse(pagoRepository.save(pago));
	}

	@Override
	@Transactional(readOnly = true)
	public List<PagoResponse> findAll() {
		return pagoRepository.findAll().stream()
				.map(pagoMapper::toResponse)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public PagoResponse findById(Long id) {
		return pagoMapper.toResponse(getPagoById(id));
	}

	@Override
	@Transactional
	public PagoResponse update(Long id, PagoRequest request) {
		Pago pago = getPagoById(id);
		pagoMapper.updateEntityFromRequest(pago, request);
		return pagoMapper.toResponse(pagoRepository.save(pago));
	}

	@Override
	@Transactional
	public void delete(Long id) {
		getPagoById(id);
		pagoRepository.deleteById(id);
	}

	private Pago getPagoById(Long id) {
		return pagoRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Pago con id " + id + " no encontrado"));
	}
}
