package com.cartec.sistema.repository;

import com.cartec.sistema.model.ItemServico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ItemServicoRepository extends JpaRepository<ItemServico, Long> {

    List<ItemServico> findByOrdemServicoId(Long ordemServicoId);

    List<ItemServico> findByGrupo(String grupo);
}
