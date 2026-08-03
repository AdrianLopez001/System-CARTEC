package com.cartec.sistema.repository;

import com.cartec.sistema.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    Optional<Cliente> findByTelefone(String telefone);

    Optional<Cliente> findByNomeIgnoreCase(String nome);

    Optional<Cliente> findByCodigoExterno(String codigoExterno);

    java.util.List<Cliente> findByEmpresaId(Long empresaId);

    java.util.List<Cliente> findByDemoTrue();

    java.util.List<Cliente> findByDemoFalse();

    List<Cliente> findByTagIgnoreCaseOrderByNomeAsc(String tag);

    @Query("select distinct c.tag from Cliente c where c.tag is not null and c.tag <> '' order by c.tag")
    List<String> listarTagsDistintas();
}
