package br.com.finaya.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.com.finaya.model.PixKey;

@Repository
public interface PixKeyRepository extends JpaRepository<PixKey, UUID>{
    
    @Query("SELECT p FROM PixKey p WHERE p.keyValue = :key AND p.status = 'ACTIVE'")
    Optional<PixKey> findByKeyValueAndActive(@Param("key") String key);
    
    @Query("SELECT COUNT(p) > 0 FROM PixKey p WHERE p.keyValue = :key AND p.status = 'ACTIVE'")
    boolean existsByKeyValueAndActive(@Param("key") String key);
    
    @Modifying
    @Query("UPDATE PixKey p SET p.status = 'INACTIVE' WHERE p.id = :id")
    void inactivateById(@Param("id") UUID id);
    
    @Query("SELECT p FROM PixKey p WHERE p.walletId = :walletId")
    List<PixKey> findByWalletId(@Param("walletId") UUID walletId);
    
    default boolean existsActiveByKey(String key) {
        return existsByKeyValueAndActive(key);
    }
    
    default void inactivatePixKey(UUID id) {
        inactivateById(id);
    }
}
