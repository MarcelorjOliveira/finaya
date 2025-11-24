package br.com.finaya.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.finaya.model.LedgerEntry;
import br.com.finaya.model.Wallet;
import br.com.finaya.repositories.LedgerEntryRepository;
import br.com.finaya.repositories.WalletRepository;

@Service
@Transactional
public class WalletService {
    private static final Logger logger = LoggerFactory.getLogger(WalletService.class);
    
    private final WalletRepository walletRepository;
    private final LedgerEntryRepository ledgerRepository;
    private final IdempotencyService idempotencyService;

    public WalletService(WalletRepository walletRepository, 
                       LedgerEntryRepository ledgerRepository,
                       IdempotencyService idempotencyService) {
        this.walletRepository = walletRepository;
        this.ledgerRepository = ledgerRepository;
        this.idempotencyService = idempotencyService;
    }

    public Wallet createWallet(UUID userId) {
        logger.info("Creating wallet for user: {}", userId);
        Wallet wallet = new Wallet(userId);
        return walletRepository.save(wallet);
    }

    public void deposit(UUID walletId, BigDecimal amount, UUID idempotencyKey) {
        logger.info("Processing deposit - Wallet: {}, Amount: {}, IdempotencyKey: {}", walletId, amount, idempotencyKey);
        
        idempotencyService.executeWithIdempotency(
            idempotencyKey,
            () -> {
                Wallet wallet = walletRepository.findByIdWithLock(walletId)
                    .orElseThrow(() -> new RuntimeException("Wallet not found"));
                
                wallet.deposit(amount);
                Wallet savedWallet = walletRepository.save(wallet);
                
                LedgerEntry entry = new LedgerEntry(
                    walletId, 
                    UUID.randomUUID(), 
                    amount, 
                    LedgerEntry.EntryType.DEPOSIT,
                    savedWallet.getBalance(),
                    "Deposit"
                );
                ledgerRepository.save(entry);
                
                logger.info("Deposit completed - Wallet: {}, New Balance: {}", walletId, savedWallet.getBalance());
                return null;
            }
        );
    }

    public void withdraw(UUID walletId, BigDecimal amount, UUID idempotencyKey) {
        logger.info("Processing withdrawal - Wallet: {}, Amount: {}, IdempotencyKey: {}", walletId, amount, idempotencyKey);
        
        idempotencyService.executeWithIdempotency(
            idempotencyKey,
            () -> {
                Wallet wallet = walletRepository.findByIdWithLock(walletId)
                    .orElseThrow(() -> new RuntimeException("Wallet not found"));
                
                wallet.withdraw(amount);
                Wallet savedWallet = walletRepository.save(wallet);
                
                LedgerEntry entry = new LedgerEntry(
                    walletId, 
                    UUID.randomUUID(), 
                    amount.negate(), 
                    LedgerEntry.EntryType.WITHDRAWAL,
                    savedWallet.getBalance(),
                    "Withdrawal"
                );
                ledgerRepository.save(entry);
                
                logger.info("Withdrawal completed - Wallet: {}, New Balance: {}", walletId, savedWallet.getBalance());
                return null;
            }
        );
    }

    @Transactional(readOnly = true)
    public BigDecimal getCurrentBalance(UUID walletId) {
        return walletRepository.findBalanceById(walletId)
            .orElseThrow(() -> new RuntimeException("Wallet not found"));
    }

    @Transactional(readOnly = true)
    public BigDecimal getHistoricalBalance(UUID walletId, LocalDateTime timestamp) {
    	
         BigDecimal balance = ledgerRepository.findBalanceAtTimestamp(walletId, timestamp)
            .orElse(BigDecimal.ZERO);
         
         System.out.println("Balance:;"+balance);
         
         return balance; 
    }

    @Transactional(readOnly = true)
    public Wallet findById(UUID walletId) {
        return walletRepository.findById(walletId)
            .orElseThrow(() -> new RuntimeException("Wallet not found"));
    }
}