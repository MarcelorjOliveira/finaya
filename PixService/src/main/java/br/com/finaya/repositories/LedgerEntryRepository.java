package br.com.finaya.repositories;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.com.finaya.model.LedgerEntry;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {
    
    List<LedgerEntry> findByWalletIdOrderByCreatedAtDesc(UUID walletId);
    
    @Query(value = "SELECT * FROM ledger_entries le WHERE le.wallet_id = :walletId AND le.created_at <= :timestamp ORDER BY le.created_at DESC LIMIT 1", 
           nativeQuery = true)
    Optional<LedgerEntry> findLatestBeforeTimestamp(@Param("walletId") UUID walletId, @Param("timestamp") LocalDateTime timestamp);
    
    @Query(value = "SELECT le.balance_after FROM ledger_entries le WHERE le.wallet_id = :walletId AND le.created_at <= :timestamp ORDER BY le.created_at DESC LIMIT 1", 
           nativeQuery = true)
    Optional<BigDecimal> findBalanceAtTimestamp(@Param("walletId") UUID walletId, @Param("timestamp") LocalDateTime timestamp);
    
    @Query(value = "SELECT * FROM ledger_entries le WHERE le.wallet_id = :walletId ORDER BY le.created_at DESC LIMIT 1", 
           nativeQuery = true)
    Optional<LedgerEntry> findLatestByWalletId(@Param("walletId") UUID walletId);
    
    // Query adicional útil para performance
    @Query(value = "SELECT COUNT(*) FROM ledger_entries le WHERE le.wallet_id = :walletId", 
           nativeQuery = true)
    Long countByWalletId(@Param("walletId") UUID walletId);
    
    // Query para buscar entradas em um período específico
    @Query(value = "SELECT * FROM ledger_entries le WHERE le.wallet_id = :walletId AND le.created_at BETWEEN :startDate AND :endDate ORDER BY le.created_at DESC", 
           nativeQuery = true)
    List<LedgerEntry> findByWalletIdAndDateRange(@Param("walletId") UUID walletId, 
                                                @Param("startDate") LocalDateTime startDate, 
                                                @Param("endDate") LocalDateTime endDate);
    
    // Query para soma de valores por tipo em um período
    @Query(value = "SELECT COALESCE(SUM(le.amount), 0) FROM ledger_entries le WHERE le.wallet_id = :walletId AND le.type = :type AND le.created_at BETWEEN :startDate AND :endDate", 
           nativeQuery = true)
    BigDecimal sumAmountByWalletIdAndTypeAndDateRange(@Param("walletId") UUID walletId, 
                                                     @Param("type") String type,
                                                     @Param("startDate") LocalDateTime startDate, 
                                                     @Param("endDate") LocalDateTime endDate);
}
