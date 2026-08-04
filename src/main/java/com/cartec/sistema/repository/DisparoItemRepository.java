package com.cartec.sistema.repository;

import com.cartec.sistema.model.DisparoItem;
import com.cartec.sistema.model.StatusItemDisparo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DisparoItemRepository extends JpaRepository<DisparoItem, Long> {

    Optional<DisparoItem> findFirstByDisparoIdAndStatusOrderByIdAsc(Long disparoId, StatusItemDisparo status);

    long countByDisparoIdAndStatus(Long disparoId, StatusItemDisparo status);

    long countByDisparoIdAndStatusAndDataEnvioGreaterThanEqual(Long disparoId, StatusItemDisparo status, LocalDateTime desde);

    List<DisparoItem> findByTelefoneAndStatus(String telefone, StatusItemDisparo status);

    List<DisparoItem> findByDisparoIdOrderByIdDesc(Long disparoId);
}
