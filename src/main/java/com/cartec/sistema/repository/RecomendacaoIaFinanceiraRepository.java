package com.cartec.sistema.repository;

import com.cartec.sistema.model.RecomendacaoIaFinanceira;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RecomendacaoIaFinanceiraRepository extends JpaRepository<RecomendacaoIaFinanceira, Long> {

    Optional<RecomendacaoIaFinanceira> findByMes(String mes);
}
