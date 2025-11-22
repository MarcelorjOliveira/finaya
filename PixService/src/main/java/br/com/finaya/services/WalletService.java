package br.com.finaya.services;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.finaya.model.Wallet;
import br.com.finaya.repositories.JpaWalletRepository;

@Service
@Transactional
public class WalletService {
    private final JpaWalletRepository walletRepository;

    public WalletService(JpaWalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    public Wallet createWallet(UUID userId) {
        Wallet wallet = new Wallet(userId);
        return walletRepository.save(wallet);
    }

}
