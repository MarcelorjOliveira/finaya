package br.com.finaya.repositories;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import javax.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.com.finaya.model.Wallet;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, UUID> {
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.id = :id")
    Optional<Wallet> findByIdWithLock(@Param("id") UUID id);
    
    @Query("SELECT w.balance FROM Wallet w WHERE w.id = :id")
    Optional<BigDecimal> findBalanceById(@Param("id") UUID id);
}