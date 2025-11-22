package br.com.finaya.controllers;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.finaya.services.PixKeyService;
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
    private final PixKeyService pixKeyService;
    
    public WalletController(WalletService walletService, PixKeyService pixKeyService) {
        this.walletService = walletService;
        this.pixKeyService = pixKeyService;
      
      }
   
    @Operation(
            summary = "Registrar chave Pix",
            description = "Registra uma nova chave Pix para uma carteira. " +
                         "Tipos suportados: EMAIL, PHONE, EVP (chave aleatória)."
        )
        @ApiResponses({
            @ApiResponse(
                responseCode = "200",
                description = "Chave Pix registrada com sucesso",
                content = @Content(schema = @Schema(implementation = RegisterPixKeyResponse.class))
            ),
            @ApiResponse(
                responseCode = "400",
                description = "Chave Pix inválida ou tipo não suportado"
            ),
            @ApiResponse(
                responseCode = "404",
                description = "Carteira não encontrada"
            ),
            @ApiResponse(
                responseCode = "409",
                description = "Chave Pix já está em uso"
            )
        })
        @PostMapping("/{walletId}/pix-keys")
        public ResponseEntity<RegisterPixKeyResponse> registerPixKey(
                @Parameter(description = "ID da carteira", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
                @PathVariable UUID walletId,
                
                @Parameter(description = "Dados da chave Pix a ser registrada", required = true)
                @RequestBody RegisterPixKeyRequest request) {
            
            var pixKey = pixKeyService.registerPixKey(
                walletId,
                request.key(),
                request.type()
            );
            
            return ResponseEntity.ok(new RegisterPixKeyResponse(
                pixKey.getId(),
                pixKey.getKeyValue(),
                pixKey.getType().name(),
                pixKey.getStatus().name()
            ));
        }
        
        @Schema(description = "Request para registro de chave Pix")
        public record RegisterPixKeyRequest(
            @Schema(
                description = "Valor da chave Pix (email, telefone com DDI + DDD, ou EVP)", 
                example = "fulano@email.com",
                required = true
            )
            String key,
            
            @Schema(
                description = "Tipo da chave Pix", 
                example = "EMAIL",
                allowableValues = {"EMAIL", "PHONE", "EVP"},
                required = true
            )
            String type
        ) {}

        @Schema(description = "Response de registro de chave Pix")
        public record RegisterPixKeyResponse(
            @Schema(description = "ID da chave Pix registrada", example = "123e4567-e89b-12d3-a456-426614174000")
            UUID pixKeyId,
            
            @Schema(description = "Valor da chave Pix", example = "fulano@email.com")
            String key,
            
            @Schema(description = "Tipo da chave Pix", example = "EMAIL")
            String type,
            
            @Schema(description = "Status da chave Pix", example = "ACTIVE")
            String status
        ) {}

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
