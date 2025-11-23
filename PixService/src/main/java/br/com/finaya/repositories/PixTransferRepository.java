package br.com.finaya.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.com.finaya.model.PixTransfer;

@Repository
public interface PixTransferRepository extends JpaRepository<PixTransfer, UUID> {
    
    Optional<PixTransfer> findByEndToEndId(UUID endToEndId);
    
    @Query(value = "SELECT * FROM pix_transfers p WHERE p.idempotency_key = UNHEX(REPLACE(:idempotencyKey, '-', ''))", 
           nativeQuery = true)
    Optional<PixTransfer> findByIdempotencyKey(@Param("idempotencyKey") UUID idempotencyKey);
    
    @Query("SELECT p FROM PixTransfer p WHERE p.fromWalletId = :walletId OR p.toWalletId = :walletId")
    List<PixTransfer> findByWalletId(@Param("walletId") UUID walletId);
}
