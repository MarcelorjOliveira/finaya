package br.com.finaya.dto;

import javax.validation.constraints.NotNull;

public class CreateWalletRequest {

    @NotNull(message = "userId is required")
    private Long userId;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}
