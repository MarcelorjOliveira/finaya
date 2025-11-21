package br.com.finaya.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.finaya.model.Wallet;
import br.com.finaya.repositories.WalletRepository;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;
    
    @InjectMocks
    private WalletService walletService;

    private UUID walletId;
    private UUID userId;
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        walletId = UUID.randomUUID();
        userId = UUID.randomUUID();
        wallet = new Wallet(userId);
    }

    @Test
    @DisplayName("Should create wallet successfully")
    void shouldCreateWalletSuccessfully() {
        // Given
        when(walletRepository.save(any(Wallet.class))).thenReturn(wallet);

        // When
        Wallet result = walletService.createWallet(userId);

        // Then
        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        verify(walletRepository).save(any(Wallet.class));
    }
    
}
