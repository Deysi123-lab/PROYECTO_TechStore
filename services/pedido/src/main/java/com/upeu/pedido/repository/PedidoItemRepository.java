package com.upeu.pedido.repository;

import com.upeu.pedido.entity.PedidoItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PedidoItemRepository extends JpaRepository<PedidoItem, Long> {

	List<PedidoItem> findByPedidoId(Long pedidoId);

	void deleteByPedidoId(Long pedidoId);
}
