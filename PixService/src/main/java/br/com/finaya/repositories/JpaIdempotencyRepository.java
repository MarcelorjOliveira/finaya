package br.com.finaya.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.com.finaya.model.IdempotencyRecordEntity;

@Repository
public interface JpaIdempotencyRepository extends JpaRepository<IdempotencyRecordEntity, Long>{
    
    Optional<IdempotencyRecordEntity> findByRecordKey(String recordKey);
    
    void deleteByRecordKey(String recordKey);

}
