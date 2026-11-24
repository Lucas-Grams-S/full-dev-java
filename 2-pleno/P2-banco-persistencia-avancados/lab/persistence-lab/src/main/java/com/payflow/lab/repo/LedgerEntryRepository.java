package com.payflow.lab.repo;

import com.payflow.lab.domain.LedgerEntry;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {

    /** Projeção por interface (proxy): só as colunas pedidas. */
    interface EntryView {
        UUID getId();
        String getType();
        java.math.BigDecimal getAmount();
        Instant getCreatedAt();
    }

    List<EntryView> findByAccount_IdOrderByCreatedAtDesc(UUID accountId, Limit limit);

    /** Projeção em record via expressão de construtor JPQL. */
    @Query("""
            select new com.payflow.lab.repo.EntryLine(e.id, e.type, e.amount, e.createdAt)
            from LedgerEntry e where e.account.id = :accountId order by e.createdAt desc, e.id desc""")
    List<EntryLine> lines(@Param("accountId") UUID accountId, Limit limit);

    /** Entidade completa (carrega a conta como proxy; traz todas as colunas). */
    List<LedgerEntry> findByAccount_IdOrderByCreatedAtDescIdDesc(UUID accountId, Limit limit);

    /** Keyset: a próxima página depois do último (created_at, id) visto. */
    @Query("""
            select new com.payflow.lab.repo.EntryLine(e.id, e.type, e.amount, e.createdAt)
            from LedgerEntry e
            where e.account.id = :accountId
              and (e.createdAt < :ts or (e.createdAt = :ts and e.id < :lastId))
            order by e.createdAt desc, e.id desc""")
    List<EntryLine> after(@Param("accountId") UUID accountId, @Param("ts") Instant ts, @Param("lastId") UUID lastId, Limit limit);
}
