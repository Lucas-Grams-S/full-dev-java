package com.payflow.lab.repo;

import com.payflow.lab.domain.Account;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    /** Trava as contas na ORDEM DO ID (a regra de ouro anti-deadlock). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.id in :ids order by a.id")
    List<Account> lockAllOrdered(@Param("ids") Collection<UUID> ids);

    /** Trava UMA conta; usado em ordem errada de propósito para provocar deadlock. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.id = :id")
    Optional<Account> lockById(@Param("id") UUID id);

    /** lock.timeout = 0 vira NOWAIT. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "0"))
    @Query("select a from Account a where a.id = :id")
    Optional<Account> lockByIdNowait(@Param("id") UUID id);

    /** lock.timeout = 500 (ms): o PostgreSQL só distingue NOWAIT e "espera"; o que acontece com 500? (ver LockTimeoutIT) */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "500"))
    @Query("select a from Account a where a.id = :id")
    Optional<Account> lockByIdHint500(@Param("id") UUID id);

    /** Duas coleções ao mesmo tempo: MultipleBagFetchException. */
    @Query("select a from Account a join fetch a.entries join fetch a.sent where a.id = :id")
    Optional<Account> withEntriesAndSent(@Param("id") UUID id);

    @Query("select distinct a from Account a join fetch a.entries where a.owner.id = :ownerId")
    List<Account> withEntriesOfOwner(@Param("ownerId") UUID ownerId);

    @Query(value = "select distinct a from Account a join fetch a.entries", countQuery = "select count(a) from Account a")
    org.springframework.data.domain.Page<Account> pageWithEntries(org.springframework.data.domain.Pageable pageable);

    @EntityGraph(attributePaths = "owner")
    List<Account> findAllByOrderById();
}
