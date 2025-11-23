package br.com.finaya.services;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.finaya.model.LedgerEntry;
import br.com.finaya.model.Wallet;
import br.com.finaya.repositories.JpaWalletRepository;
import br.com.finaya.repositories.LedgerEntryRepository;

@Service
@Transactional
public class WalletService {
    private final JpaWalletRepository walletRepository;
    private final LedgerEntryRepository ledgerRepository;
    private final IdempotencyService idempotencyService;

    public WalletService(JpaWalletRepository walletRepository, 
            LedgerEntryRepository ledgerRepository,
            IdempotencyService idempotencyService) {
    	
			this.walletRepository = walletRepository;
			this.ledgerRepository = ledgerRepository;
			this.idempotencyService = idempotencyService;
		}

    public Wallet createWallet(UUID userId) {
        Wallet wallet = new Wallet(userId);
        return walletRepository.save(wallet);
    }
    
    public void withdraw(UUID walletId, BigDecimal amount, String idempotencyKey) {
        idempotencyService.executeWithIdempotency(
            "withdraw:" + walletId + ":" + idempotencyKey,
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
                
                return null;
            }
        );
    }
    
    public void deposit(UUID walletId, BigDecimal amount, String idempotencyKey) {
        idempotencyService.executeWithIdempotency(
            "deposit:" + walletId + ":" + idempotencyKey,
            () -> {
                Wallet wallet = walletRepository.findByIdWithLock(walletId)
                    .orElseThrow(() -> new RuntimeException("Wallet not found"));
                
                wallet.deposit(amount);
                walletRepository.save(wallet);
                
                LedgerEntry entry = new LedgerEntry(
                    walletId, 
                    UUID.randomUUID(), 
                    amount, 
                    LedgerEntry.EntryType.DEPOSIT,
                    wallet.getBalance(),
                    "Deposit"
                );
                ledgerRepository.save(entry);
                
                return null;
            }
        );
    }

}
