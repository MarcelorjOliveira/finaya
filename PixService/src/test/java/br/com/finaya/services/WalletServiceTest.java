package br.com.finaya.services;

// Importações para assertions
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
// Importações para Mockito
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.finaya.model.LedgerEntry;
import br.com.finaya.model.Wallet;
import br.com.finaya.repositories.JpaWalletRepository;
import br.com.finaya.repositories.LedgerEntryRepository;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private JpaWalletRepository walletRepository;

    @Mock
    private LedgerEntryRepository ledgerRepository;

    @Mock
    private IdempotencyService idempotencyService;

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

    @Test
    @DisplayName("Should deposit amount successfully")
    void shouldDepositAmountSuccessfully() {
        // Given
        BigDecimal amount = new BigDecimal("100.00");
        String idempotencyKey = "deposit-key-123";

        when(walletRepository.findByIdWithLock(walletId)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(wallet);
        
        // CORREÇÃO: Usar Supplier em vez de Runnable
        when(idempotencyService.executeWithIdempotency(anyString(), any(Supplier.class)))
            .thenAnswer(invocation -> {
                Supplier<?> operation = invocation.getArgument(1);
                return operation.get(); // Chama get() em vez de run()
            });

        // When
        walletService.deposit(walletId, amount, idempotencyKey);

        // Then
        verify(walletRepository).findByIdWithLock(walletId);
        verify(walletRepository).save(any(Wallet.class));
        verify(ledgerRepository).save(any(LedgerEntry.class));
        verify(idempotencyService).executeWithIdempotency(anyString(), any(Supplier.class));
    }
    
    @Test
    @DisplayName("Should withdraw amount successfully")
    void shouldWithdrawAmountSuccessfully() {
        // Given
        BigDecimal initialBalance = new BigDecimal("200.00");
        BigDecimal withdrawAmount = new BigDecimal("100.00");
        String idempotencyKey = "withdraw-key-123";

        wallet.deposit(initialBalance); 

        when(walletRepository.findByIdWithLock(walletId)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(wallet);
        
        when(idempotencyService.executeWithIdempotency(anyString(), any(Supplier.class)))
            .thenAnswer(invocation -> {
                Supplier<?> operation = invocation.getArgument(1);
                return operation.get();
            });

        // When
        walletService.withdraw(walletId, withdrawAmount, idempotencyKey);

        // Then
        verify(walletRepository).findByIdWithLock(walletId);
        verify(walletRepository).save(any(Wallet.class));
        verify(ledgerRepository).save(any(LedgerEntry.class));
    }
    
}
