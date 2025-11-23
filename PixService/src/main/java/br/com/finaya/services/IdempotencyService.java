package br.com.finaya.services;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.finaya.model.IdempotencyRecord;
import br.com.finaya.repositories.IdempotencyRecordRepository;

@Service
@Transactional
public class IdempotencyService {
    private static final Logger logger = LoggerFactory.getLogger(IdempotencyService.class);
    
    private final IdempotencyRecordRepository idempotencyRecordRepository;

    public IdempotencyService(IdempotencyRecordRepository idempotencyRecordRepository) {
        this.idempotencyRecordRepository = idempotencyRecordRepository;
    }

    public <T> T executeWithIdempotency(UUID idempotencyKey, Supplier<T> operation) {
        logger.info("Executing operation with idempotency key: {}", idempotencyKey);

        ObjectMapper objectMapper = new ObjectMapper();;
        
        Optional<IdempotencyRecord> idempotencyRecord = idempotencyRecordRepository.findByRecordKey(idempotencyKey);
        
        return (T) idempotencyRecord
            .map(record -> {
                logger.info("Found existing idempotency record for key: {}", idempotencyKey);
                
                if (record.getResultType() != null) {
                    logger.info("Returning cached result for idempotency key: {}", idempotencyKey);
                    return null; // Em implementação real, desserializaria o resultado
                } else if (record.getErrorType() != null) {
                    logger.warn("Duplicate request with previous error for idempotency key: {}", idempotencyKey);
                    throw new RuntimeException("Duplicate request with previous error: " + record.getErrorMessage());
                }
                logger.warn("Duplicate request in progress for idempotency key: {}", idempotencyKey);
                throw new RuntimeException("Duplicate request in progress");
            })
            .orElseGet(() -> {
                logger.info("Creating new idempotency record for key: {}", idempotencyKey);
                IdempotencyRecord record = new IdempotencyRecord(idempotencyKey);
                idempotencyRecordRepository.save(record);

                try {
                    T result = operation.get();
                    record.markSuccess(result);
                    
                    try {
                        // Always serialize to valid JSON
                        String jsonResult = objectMapper.writeValueAsString(result);
                        record.setResultData(jsonResult);
                    } catch (JsonProcessingException e) {
                        // Fallback to null or empty object
                        record.setResultData("{}");
                    }
                    
                    idempotencyRecordRepository.save(record);
                    logger.info("Operation completed successfully for idempotency key: {}", record);
                    return result;
                } catch (Exception e) {
                    logger.error("Operation failed for idempotency key: {}, error: {}", idempotencyKey, e.getMessage());
                    record.markError(e.getClass().getName(), e.getMessage());
                    idempotencyRecordRepository.save(record);
                    throw e;
                }
            });
    }

    // Sobrecarga para operações void
    public void executeWithIdempotency(UUID idempotencyKey, Runnable operation) {
        executeWithIdempotency(idempotencyKey, () -> {
            operation.run();
            return null;
        });
    }

    // Método para limpar registros antigos (opcional)
    public void cleanUpOldRecords() {
        // Implementação para limpar registros antigos se necessário
    }
}