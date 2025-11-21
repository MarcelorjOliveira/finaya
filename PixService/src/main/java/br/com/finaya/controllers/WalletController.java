package br.com.finaya.controllers;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.finaya.services.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/wallets")
@Tag(name = "Carteiras", description = "Operações para gerenciamento de carteiras digitais")
public class WalletController {
    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @Operation(
        summary = "Criar nova carteira",
        description = "Cria uma nova carteira digital para um usuário. Cada usuário pode ter múltiplas carteiras."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Carteira criada com sucesso",
            content = @Content(schema = @Schema(implementation = CreateWalletResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Dados de entrada inválidos"
        )
    })
    @PostMapping
    public ResponseEntity<CreateWalletResponse> createWallet(
            @Parameter(description = "Dados para criação da carteira", required = true)
            @RequestBody CreateWalletRequest request) {
        
        UUID walletId = walletService.createWallet(request.userId()).getId();
        return ResponseEntity.ok(new CreateWalletResponse(walletId));
    }
    
    public record CreateWalletRequest(UUID userId) {}
    public record CreateWalletResponse(UUID walletId) {}
}
