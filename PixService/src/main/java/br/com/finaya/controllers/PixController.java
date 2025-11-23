package br.com.finaya.controllers;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.finaya.services.PixTransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/pix")
@Tag(name = "Pix", description = "Operações para transferências Pix e webhooks de confirmação")
public class PixController {
    private final PixTransferService pixTransferService;

    public PixController(PixTransferService pixTransferService) {
        this.pixTransferService = pixTransferService;
    }

    @Operation(
        summary = "Iniciar transferência Pix",
        description = "Inicia uma transferência Pix entre carteiras. A transferência fica com status PENDING " +
                     "até a confirmação via webhook. Requer chave de idempotência UUID para evitar duplicações."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Transferência Pix iniciada com sucesso",
            content = @Content(schema = @Schema(implementation = PixTransferResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Dados inválidos (valor negativo, chave Pix inválida, etc)"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Carteira de origem ou chave Pix de destino não encontrada"
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Saldo insuficiente ou conflito de idempotência"
        ),
        @ApiResponse(
            responseCode = "422",
            description = "Chave Pix já está em uso"
        )
    })
    @SecurityRequirement(name = "IdempotencyKey")
    @PostMapping("/transfers")
    public ResponseEntity<PixTransferResponse> initiateTransfer(
            @Parameter(description = "Chave de idempotência UUID para evitar duplicações", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @RequestHeader("Idempotency-Key") UUID idempotencyKey,
            
            @Parameter(description = "Dados da transferência Pix", required = true)
            @RequestBody PixTransferRequest request) {
        
        var transfer = pixTransferService.initiatePixTransfer(
            request.fromWalletId(), 
            request.toPixKey(), 
            request.amount(), 
            idempotencyKey
        );
        
        return ResponseEntity.ok(new PixTransferResponse(
            transfer.getEndToEndId(), 
            transfer.getStatus().name()
        ));
    }


    // Records para request/response
    @Schema(description = "Request para transferência Pix")
    public record PixTransferRequest(
        @Schema(description = "ID da carteira de origem", example = "123e4567-e89b-12d3-a456-426614174000", required = true)
        UUID fromWalletId,
        
        @Schema(
            description = "Chave Pix de destino (email, telefone ou chave aleatória)", 
            example = "fulano@email.com", 
            required = true
        )
        String toPixKey,
        
        @Schema(
            description = "Valor da transferência (deve ser positivo)", 
            example = "150.75", 
            required = true, 
            minimum = "0.01"
        )
        BigDecimal amount
    ) {}

    @Schema(description = "Response de transferência Pix")
    public record PixTransferResponse(
        @Schema(
            description = "ID end-to-end da transferência (identificador único)", 
            example = "123e4567-e89b-12d3-a456-426614174000",
            required = true
        )
        UUID endToEndId,
        
        @Schema(
            description = "Status atual da transferência", 
            example = "PENDING",
            allowableValues = {"PENDING", "CONFIRMED", "REJECTED"},
            required = true
        )
        String status
    ) {}

}