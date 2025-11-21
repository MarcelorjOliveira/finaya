package br.com.finaya.repositories;

import br.com.finaya.model.Wallet;

public interface WalletRepository {
    Wallet save(Wallet wallet);
}
