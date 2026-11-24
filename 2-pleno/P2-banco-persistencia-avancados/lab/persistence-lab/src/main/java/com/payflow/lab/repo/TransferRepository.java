package com.payflow.lab.repo;

import com.payflow.lab.domain.Transfer;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransferRepository extends JpaRepository<Transfer, UUID> {

    /** Ingênuo: source e target são LAZY; ler o dono de cada um gera N+1. */
    List<Transfer> findAllByOrderById(Pageable pageable);

    @EntityGraph(attributePaths = {"source.owner", "target.owner"})
    List<Transfer> findAllWithGraphByOrderById(Pageable pageable);
}
