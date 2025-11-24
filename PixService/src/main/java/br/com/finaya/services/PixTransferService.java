package br.com.finaya.services;

import java.math.BigDecimal;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.finaya.model.LedgerEntry;
import br.com.finaya.model.PixKey;
import br.com.finaya.model.PixTransfer;
import br.com.finaya.model.Wallet;
import br.com.finaya.repositories.LedgerEntryRepository;
import br.com.finaya.repositories.PixKeyRepository;
import br.com.finaya.repositories.PixTransferRepository;
import br.com.finaya.repositories.WalletRepository;

@Service
@Transactional
public class PixTransferService {
    private static final Logger logger = LoggerFactory.getLogger(PixTransferService.class);
    
    private final WalletRepository walletRepository;
    private final PixKeyRepository pixKeyRepository;
    private final PixTransferRepository pixTransferRepository;
    private final LedgerEntryRepository ledgerRepository;
    private final IdempotencyService idempotencyService;

    public PixTransferService(WalletRepository walletRepository,
                            PixKeyRepository pixKeyRepository,
                            PixTransferRepository pixTransferRepository,
                            LedgerEntryRepository ledgerRepository,
                            IdempotencyService idempotencyService) {
        this.walletRepository = walletRepository;
        this.pixKeyRepository = pixKeyRepository;
        this.pixTransferRepository = pixTransferRepository;
        this.ledgerRepository = ledgerRepository;
        this.idempotencyService = idempotencyService;
    }

    public PixTransfer initiatePixTransfer(UUID fromWalletId, String toPixKey, 
                                         BigDecimal amount, UUID idempotencyKey) {
        logger.info("Initiating PIX transfer - From: {}, ToPixKey: {}, Amount: {}, IdempotencyKey: {}", 
                   fromWalletId, toPixKey, amount, idempotencyKey);
        
        return idempotencyService.executeWithIdempotency(
            idempotencyKey,
            () -> {
                // Find destination wallet by Pix key
                PixKey pixKey = pixKeyRepository.findByKeyValueAndActive(toPixKey)
                    .orElseThrow(() -> new RuntimeException("Pix key not found: " + toPixKey));
                UUID toWalletId = pixKey.getWalletId();

                // Lock both wallets to prevent race conditions
                Wallet fromWallet = walletRepository.findByIdWithLock(fromWalletId)
                    .orElseThrow(() -> new RuntimeException("From wallet not found: " + fromWalletId));
                Wallet toWallet = walletRepository.findByIdWithLock(toWalletId)
                    .orElseThrow(() -> new RuntimeException("To wallet not found: " + toWalletId));

                // Create Pix transfer
                PixTransfer transfer = new PixTransfer(fromWalletId, toWalletId, amount, idempotencyKey);
                transfer = pixTransferRepository.save(transfer);

                // Reserve amount in from wallet
                fromWallet.withdraw(amount);
                walletRepository.save(fromWallet);

                // Create ledger entries
                LedgerEntry fromEntry = new LedgerEntry(
                    fromWalletId,
                    transfer.getEndToEndId(),
                    amount.negate(),
                    LedgerEntry.EntryType.PIX_RESERVED,
                    fromWallet.getBalance(),
                    "PIX transfer reserved - " + transfer.getEndToEndId()
                );
                ledgerRepository.save(fromEntry);

                logger.info("PIX transfer initiated successfully - EndToEndId: {}", transfer.getEndToEndId());
                return transfer;
            }
        );
    }

    public void processWebhook(UUID endToEndId, String eventId, String eventType, UUID idempotencyKey) {
        logger.info("Processing PIX webhook - EndToEndId: {}, EventId: {}, EventType: {}, IdempotencyKey: {}", 
                   endToEndId, eventId, eventType, idempotencyKey);
        
        idempotencyService.executeWithIdempotency(
            idempotencyKey,
            () -> {
                PixTransfer transfer = pixTransferRepository.findByEndToEndId(endToEndId)
                    .orElseThrow(() -> new RuntimeException("PIX transfer not found: " + endToEndId));

                // Lock wallets involved in the transfer
                Wallet fromWallet = walletRepository.findByIdWithLock(transfer.getFromWalletId())
                    .orElseThrow(() -> new RuntimeException("From wallet not found: " + transfer.getFromWalletId()));
                Wallet toWallet = walletRepository.findByIdWithLock(transfer.getToWalletId())
                    .orElseThrow(() -> new RuntimeException("To wallet not found: " + transfer.getToWalletId()));

                if ("CONFIRMED".equals(eventType)) {
                    if (transfer.getStatus() == PixTransfer.TransferStatus.PENDING) {
                        // Finalize the transfer - credit to destination wallet
                        toWallet.deposit(transfer.getAmount());
                        walletRepository.save(toWallet);

                        // Update ledger entries
                        LedgerEntry fromFinalEntry = new LedgerEntry(
                            transfer.getFromWalletId(),
                            transfer.getEndToEndId(),
                            transfer.getAmount().negate(),
                            LedgerEntry.EntryType.PIX_OUT,
                            fromWallet.getBalance(),
                            "PIX transfer completed - " + transfer.getEndToEndId()
                        );
                        ledgerRepository.save(fromFinalEntry);

                        LedgerEntry toEntry = new LedgerEntry(
                            transfer.getToWalletId(),
                            transfer.getEndToEndId(),
                            transfer.getAmount(),
                            LedgerEntry.EntryType.PIX_IN,
                            toWallet.getBalance(),
                            "PIX transfer received - " + transfer.getEndToEndId()
                        );
                        ledgerRepository.save(toEntry);

                        transfer.confirm();
                        pixTransferRepository.save(transfer);
                        
                        logger.info("PIX transfer confirmed: {}", endToEndId);
                    }
                } else if ("REJECTED".equals(eventType)) {
                    if (transfer.getStatus() == PixTransfer.TransferStatus.PENDING) {
                        // Return reserved amount to from wallet
                        fromWallet.deposit(transfer.getAmount());
                        walletRepository.save(fromWallet);

                        // Create reversal ledger entry
                        LedgerEntry reversalEntry = new LedgerEntry(
                            transfer.getFromWalletId(),
                            transfer.getEndToEndId(),
                            transfer.getAmount(),
                            LedgerEntry.EntryType.DEPOSIT,
                            fromWallet.getBalance(),
                            "PIX transfer rejected - " + transfer.getEndToEndId()
                        );
                        ledgerRepository.save(reversalEntry);

                        transfer.reject();
                        pixTransferRepository.save(transfer);
                        
                        logger.info("PIX transfer rejected: {}", endToEndId);
                    }
                }

                return null;
            }
        );
    }

 }