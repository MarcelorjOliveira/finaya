package br.com.finaya.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.com.finaya.model.IdempotencyRecord;

@Repository
public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, Long> {
    
    @Query(value = "SELECT * FROM idempotency_records ir WHERE ir.record_key = UNHEX(REPLACE(:recordKey, '-', ''))", 
           nativeQuery = true)
    Optional<IdempotencyRecord> findByRecordKey(@Param("recordKey") UUID recordKey); 
    
    @Query(value = "DELETE FROM idempotency_records ir WHERE ir.record_key = UNHEX(REPLACE(:recordKey, '-', ''))", 
           nativeQuery = true)
    void deleteByRecordKey(@Param("recordKey") UUID recordKey);
}