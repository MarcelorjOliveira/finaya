package br.com.finaya.services;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.finaya.model.PixKey;
import br.com.finaya.model.Wallet;
import br.com.finaya.repositories.PixKeyRepository;
import br.com.finaya.repositories.WalletRepository;

@Service
@Transactional
public class PixKeyService {
    private static final Logger logger = LoggerFactory.getLogger(PixKeyService.class);
    
    private final PixKeyRepository pixKeyRepository;
    private final WalletRepository walletRepository;

    public PixKeyService(PixKeyRepository pixKeyRepository, WalletRepository walletRepository) {
        this.pixKeyRepository = pixKeyRepository;
        this.walletRepository = walletRepository;
    }

    public PixKey registerPixKey(UUID walletId, String key, String type) {
        logger.info("Registering Pix key for wallet: {}, key: {}, type: {}", walletId, key, type);
        
        // Validate wallet exists
        Wallet wallet = walletRepository.findById(walletId)
            .orElseThrow(() -> new RuntimeException("Wallet not found with id: " + walletId));
        
        // Validate Pix key type
        PixKey.PixKeyType pixKeyType;
        try {
            pixKeyType = PixKey.PixKeyType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid Pix key type: " + type);
        }
        
        // Validate key format based on type
        validatePixKeyFormat(key, pixKeyType);
        
        // Check if key is already registered and active
        pixKeyRepository.findByKeyValueAndActive(key)
            .ifPresent(existingKey -> {
                throw new RuntimeException("Pix key already registered: " + key);
            });
        
        // Create and save Pix key
        PixKey pixKey = new PixKey(walletId, key, pixKeyType);
        PixKey savedPixKey = pixKeyRepository.save(pixKey);
        
        logger.info("Pix key registered successfully: {}", savedPixKey.getId());
        return savedPixKey;
    }

    public List<PixKey> findByWalletId(UUID walletId) {
        logger.info("Finding Pix keys for wallet: {}", walletId);
        
        // Validate wallet exists
        walletRepository.findById(walletId)
            .orElseThrow(() -> new RuntimeException("Wallet not found with id: " + walletId));
        
        return pixKeyRepository.findByWalletId(walletId);
    }

    public void inactivatePixKey(UUID pixKeyId) {
        logger.info("Inactivating Pix key: {}", pixKeyId);
        
        PixKey pixKey = pixKeyRepository.findById(pixKeyId)
            .orElseThrow(() -> new RuntimeException("Pix key not found with id: " + pixKeyId));
        
       pixKeyRepository.delete(pixKey);
        
        logger.info("Pix key inactivated successfully: {}", pixKeyId);
    }

    public PixKey findByKey(String pixKey) {
        logger.info("Finding Pix key by value: {}", pixKey);
        
        return pixKeyRepository.findByKeyValueAndActive(pixKey)
            .orElseThrow(() -> new RuntimeException("Pix key not found: " + pixKey));
    }

    private void validatePixKeyFormat(String key, PixKey.PixKeyType type) {
        switch (type) {
            case EMAIL:
                if (!isValidEmail(key)) {
                    throw new IllegalArgumentException("Invalid email format: " + key);
                }
                break;
            case PHONE:
                if (!isValidPhone(key)) {
                    throw new IllegalArgumentException("Invalid phone format. Expected format: +5511999999999");
                }
                break;
            case EVP:
                if (key.length() != 36) { // UUID format
                    throw new IllegalArgumentException("Invalid EVP format. Expected UUID format");
                }
                break;
            default:
                throw new IllegalArgumentException("Unsupported Pix key type: " + type);
        }
    }

    private boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        return email != null && email.matches(emailRegex);
    }

    private boolean isValidPhone(String phone) {
        // Basic international phone format validation
        String phoneRegex = "^\\+[1-9]\\d{1,14}$";
        return phone != null && phone.matches(phoneRegex);
    }
}
