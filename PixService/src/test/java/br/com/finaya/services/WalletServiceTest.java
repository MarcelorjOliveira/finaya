package br.com.finaya.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import br.com.finaya.repositories.LedgerEntryRepository;
import br.com.finaya.repositories.WalletRepository;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private LedgerEntryRepository ledgerRepository;

    @Mock
    private IdempotencyService idempotencyService;

    @InjectMocks
    private WalletService walletService;

    private UUID walletId;
    private UUID userId;
    private Wallet wallet;
    private UUID idempotencyKey;

    @BeforeEach
    void setUp() {
        walletId = UUID.randomUUID();
        userId = UUID.randomUUID();
        wallet = new Wallet(userId);
        wallet.setId(walletId);
        idempotencyKey = UUID.randomUUID();
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

        when(walletRepository.findByIdWithLock(walletId)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(wallet);
        
        when(idempotencyService.executeWithIdempotency(any(UUID.class), any(Supplier.class)))
            .thenAnswer(invocation -> {
                Supplier<?> operation = invocation.getArgument(1);
                return operation.get();
            });

        // When
        walletService.deposit(walletId, amount, idempotencyKey);

        // Then
        verify(walletRepository).findByIdWithLock(walletId);
        verify(walletRepository).save(any(Wallet.class));
        verify(ledgerRepository).save(any(LedgerEntry.class));
        verify(idempotencyService).executeWithIdempotency(eq(idempotencyKey), any(Supplier.class));
    }

    @Test
    @DisplayName("Should withdraw amount successfully")
    void shouldWithdrawAmountSuccessfully() {
        // Given
        BigDecimal initialBalance = new BigDecimal("200.00");
        BigDecimal withdrawAmount = new BigDecimal("100.00");

        wallet.deposit(initialBalance);

        when(walletRepository.findByIdWithLock(walletId)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(wallet);
        
        when(idempotencyService.executeWithIdempotency(any(UUID.class), any(Supplier.class)))
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
        verify(idempotencyService).executeWithIdempotency(eq(idempotencyKey), any(Supplier.class));
    }

    @Test
    @DisplayName("Should throw exception when wallet not found for deposit")
    void shouldThrowExceptionWhenWalletNotFoundForDeposit() {
        // Given
        BigDecimal amount = new BigDecimal("100.00");

        when(walletRepository.findByIdWithLock(walletId)).thenReturn(Optional.empty());
        when(idempotencyService.executeWithIdempotency(any(UUID.class), any(Supplier.class)))
            .thenAnswer(invocation -> {
                Supplier<?> operation = invocation.getArgument(1);
                return operation.get();
            });

        // When & Then
        assertThrows(RuntimeException.class, () -> 
            walletService.deposit(walletId, amount, idempotencyKey));

        verify(walletRepository).findByIdWithLock(walletId);
    }

    @Test
    @DisplayName("Should throw exception when withdrawing with insufficient balance")
    void shouldThrowExceptionWhenWithdrawingWithInsufficientBalance() {
        // Given
        BigDecimal withdrawAmount = new BigDecimal("100.00");

        when(walletRepository.findByIdWithLock(walletId)).thenReturn(Optional.of(wallet));
        when(idempotencyService.executeWithIdempotency(any(UUID.class), any(Supplier.class)))
            .thenAnswer(invocation -> {
                Supplier<?> operation = invocation.getArgument(1);
                return operation.get();
            });

        // When & Then
        assertThrows(RuntimeException.class, () -> 
            walletService.withdraw(walletId, withdrawAmount, idempotencyKey));

        verify(walletRepository).findByIdWithLock(walletId);
    }

    @Test
    @DisplayName("Should find wallet by ID successfully")
    void shouldFindWalletByIdSuccessfully() {
        // Given
        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));

        // When
        Wallet result = walletService.findById(walletId);

        // Then
        assertNotNull(result);
        assertEquals(walletId, result.getId());
        verify(walletRepository).findById(walletId);
    }

    @Test
    @DisplayName("Should throw exception when wallet not found by ID")
    void shouldThrowExceptionWhenWalletNotFoundById() {
        // Given
        when(walletRepository.findById(walletId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> walletService.findById(walletId));
        verify(walletRepository).findById(walletId);
    }
}