package br.com.finaya.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.finaya.model.PixKey;
import br.com.finaya.model.Wallet;
import br.com.finaya.repositories.PixKeyRepository;
import br.com.finaya.repositories.WalletRepository;

@ExtendWith(MockitoExtension.class)
class PixKeyServiceTest {
	
    @Mock
    private PixKeyRepository pixKeyRepository;

    @Mock
    private WalletRepository walletRepository;

    @InjectMocks
    private PixKeyService pixKeyService;

    private UUID walletId;
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        walletId = UUID.randomUUID();
        wallet = new Wallet(UUID.randomUUID());
    }

    @Test
    @DisplayName("Should register email Pix key successfully")
    void shouldRegisterEmailPixKeySuccessfully() {
        // Given
        String key = "test@email.com";
        String type = "EMAIL";

        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
        when(pixKeyRepository.findByKeyValueAndActive(key)).thenReturn(Optional.empty());
        when(pixKeyRepository.save(any(PixKey.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        PixKey result = pixKeyService.registerPixKey(walletId, key, type);

        // Then
        assertNotNull(result);
        assertEquals(key, result.getKeyValue());
        assertEquals(PixKey.PixKeyType.EMAIL, result.getType());
        assertEquals(PixKey.PixKeyStatus.ACTIVE, result.getStatus());

        verify(walletRepository).findById(walletId);
        verify(pixKeyRepository).findByKeyValueAndActive(key);
        verify(pixKeyRepository).save(any(PixKey.class));
    }

    @Test
    @DisplayName("Should throw exception when wallet not found")
    void shouldThrowExceptionWhenWalletNotFound() {
        // Given
        String key = "test@email.com";
        String type = "EMAIL";

        when(walletRepository.findById(walletId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> 
            pixKeyService.registerPixKey(walletId, key, type));

        verify(walletRepository).findById(walletId);
        verify(pixKeyRepository, never()).save(any(PixKey.class));
    }

    @Test
    @DisplayName("Should throw exception when Pix key already exists")
    void shouldThrowExceptionWhenPixKeyAlreadyExists() {
        // Given
        String key = "test@email.com";
        String type = "EMAIL";
        PixKey existingKey = new PixKey(walletId, key, PixKey.PixKeyType.EMAIL);

        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
        when(pixKeyRepository.findByKeyValueAndActive(key)).thenReturn(Optional.of(existingKey));

        // When & Then
        assertThrows(RuntimeException.class, () -> 
            pixKeyService.registerPixKey(walletId, key, type));

        verify(pixKeyRepository).findByKeyValueAndActive(key);
        verify(pixKeyRepository, never()).save(any(PixKey.class));
    }

    @Test
    @DisplayName("Should throw exception for invalid email format")
    void shouldThrowExceptionForInvalidEmailFormat() {
        // Given
        String key = "invalid-email";
        String type = "EMAIL";

        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            pixKeyService.registerPixKey(walletId, key, type));

        verify(pixKeyRepository, never()).save(any(PixKey.class));
    }

    @Test
    @DisplayName("Should find Pix keys by wallet ID")
    void shouldFindPixKeysByWalletId() {
        // Given
        List<PixKey> expectedKeys = List.of(
            new PixKey(walletId, "test1@email.com", PixKey.PixKeyType.EMAIL),
            new PixKey(walletId, "+5511999999999", PixKey.PixKeyType.PHONE)
        );

        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
        when(pixKeyRepository.findByWalletId(walletId)).thenReturn(expectedKeys);

        // When
        List<PixKey> result = pixKeyService.findByWalletId(walletId);

        // Then
        assertEquals(2, result.size());
        verify(walletRepository).findById(walletId);
        verify(pixKeyRepository).findByWalletId(walletId);
    }

    @Test
    @DisplayName("Should inactivate Pix key")
    void shouldInactivatePixKey() {
        // Given
        UUID pixKeyId = UUID.randomUUID();
        PixKey pixKey = new PixKey(walletId, "test@email.com", PixKey.PixKeyType.EMAIL);

        when(pixKeyRepository.findById(pixKeyId)).thenReturn(Optional.of(pixKey));

        // When
        pixKeyService.inactivatePixKey(pixKeyId);

        // Then
        verify(pixKeyRepository).findById(pixKeyId);
        verify(pixKeyRepository).delete(pixKey);
    }

    @Test
    @DisplayName("Should throw exception when Pix key not found for inactivation")
    void shouldThrowExceptionWhenPixKeyNotFoundForInactivation() {
        // Given
        UUID pixKeyId = UUID.randomUUID();

        when(pixKeyRepository.findById(pixKeyId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> 
            pixKeyService.inactivatePixKey(pixKeyId));

        verify(pixKeyRepository).findById(pixKeyId);
        verify(pixKeyRepository, never()).delete(any(PixKey.class));
    }
    
}
