package br.com.finaya.services;

import java.util.function.Supplier;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.finaya.model.IdempotencyRecordEntity;
import br.com.finaya.repositories.JpaIdempotencyRepository;

@Service
@Transactional
public class IdempotencyService {
    private final JpaIdempotencyRepository idempotencyRepository;

    public IdempotencyService(JpaIdempotencyRepository idempotencyRepository) {
        this.idempotencyRepository = idempotencyRepository;
    }

    public <T> T executeWithIdempotency(String key, Supplier<T> operation) {
        return idempotencyRepository.findByRecordKey(key)
            .map(record -> {
                // If record exists, return the cached result or throw same exception
                if (record.getResultData() != null) {
                    return (T) record.getResultData();
                } else if (record.getErrorType() != null) {
                    throw new RuntimeException("Duplicate request with previous error");
                }
                throw new RuntimeException("Duplicate request in progress");
            })
            .orElseGet(() -> {
                // Create pending record
                IdempotencyRecordEntity record = new IdempotencyRecordEntity(key);
                idempotencyRepository.save(record);

                try {
                    T result = operation.get();
                    record.markSuccess(result);
                    idempotencyRepository.save(record);
                    return result;
                } catch (Exception e) {
                    record.markError(e.getClass().getName(), e.getMessage());
                    idempotencyRepository.save(record);
                    throw e;
                }
            });
    }
}