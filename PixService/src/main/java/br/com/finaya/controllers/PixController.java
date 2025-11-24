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
    
    @Operation(
            summary = "Webhook de confirmação Pix",
            description = "Endpoint para receber confirmações ou rejeições de transferências Pix. " +
                         "Suporta idempotência por eventId e processamento fora de ordem. Requer chave de idempotência UUID."
        )
        @ApiResponses({
            @ApiResponse(
                responseCode = "200",
                description = "Webhook processado com sucesso"
            ),
            @ApiResponse(
                responseCode = "400",
                description = "Dados do webhook inválidos"
            ),
            @ApiResponse(
                responseCode = "404",
                description = "Transferência Pix não encontrada"
            ),
            @ApiResponse(
                responseCode = "409",
                description = "Conflito de idempotência - eventId duplicado"
            ),
            @ApiResponse(
                responseCode = "422",
                description = "Transição de estado inválida (ex: confirmar transferência já rejeitada)"
            )
        })
        @SecurityRequirement(name = "IdempotencyKey")
        @PostMapping("/webhook")
        public ResponseEntity<Void> webhook(
                @Parameter(description = "Chave de idempotência UUID baseada no eventId", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
                @RequestHeader("Idempotency-Key") UUID idempotencyKey,
                
                @Parameter(description = "Dados do webhook Pix", required = true)
                @RequestBody PixWebhookRequest request) {
            
            pixTransferService.processWebhook(
                request.endToEndId(),
                request.eventId(),
                request.eventType(),
                idempotencyKey
            );
            return ResponseEntity.ok().build();
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

        @Schema(description = "Request de webhook Pix")
        public record PixWebhookRequest(
            @Schema(
                description = "ID end-to-end da transferência", 
                example = "123e4567-e89b-12d3-a456-426614174000",
                required = true
            )
            UUID endToEndId,
            
            @Schema(
                description = "ID único do evento (usado para idempotência)", 
                example = "event-12345",
                required = true
            )
            String eventId,
            
            @Schema(
                description = "Tipo do evento", 
                example = "CONFIRMED",
                allowableValues = {"CONFIRMED", "REJECTED"},
                required = true
            )
            String eventType,
            
            @Schema(
                description = "Data e hora em que o evento ocorreu (formato ISO 8601)", 
                example = "2024-01-15T14:30:00Z",
                required = true
            )
            String occurredAt
        ) {}
}