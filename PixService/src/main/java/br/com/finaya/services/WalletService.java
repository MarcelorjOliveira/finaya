package br.com.finaya.services;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.finaya.model.Wallet;
import br.com.finaya.repositories.WalletRepository;

@Service
@Transactional
public class WalletService {
    private final WalletRepository walletRepository;

    public WalletService(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    public Wallet createWallet(UUID userId) {
        Wallet wallet = new Wallet(userId);
        return walletRepository.save(wallet);
    }

}
