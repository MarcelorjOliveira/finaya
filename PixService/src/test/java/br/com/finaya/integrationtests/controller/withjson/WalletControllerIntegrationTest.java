package br.com.finaya.integrationtests.controller.withjson;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

import br.com.finaya.integrationtests.testcontainers.AbstractIntegrationTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class WalletControllerIntegrationTest extends AbstractIntegrationTest {

    @LocalServerPort
    private int port;
    
    @BeforeEach
    void setup() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
    }
    
    @Test
    void shouldCreateWalletWithZeroBalance() {
        // Given
        UUID userId = UUID.randomUUID();
        
        // When & Then - Create wallet and verify initial balance
        String walletId = given()
            .contentType(ContentType.JSON)
            .body(new CreateWalletRequest(userId))
        .when()
            .post("/wallets")
        .then()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .path("walletId");

        // Verify initial balance is zero
        given()
        .when()
            .get("/wallets/{id}/balance", walletId)
        .then()
            .statusCode(HttpStatus.OK.value())
            .body("balance", equalTo(0.0f));
    }

    @Test
    void shouldGetCurrentBalanceAfterMultipleDeposits() {
        // Given - Create wallet
        UUID userId = UUID.randomUUID();
        String walletId = createWallet(userId);
        
        // Make multiple deposits
        depositAmount(walletId, "100.00", UUID.randomUUID(), UUID.randomUUID());
        depositAmount(walletId, "50.25", UUID.randomUUID(), UUID.randomUUID());
        depositAmount(walletId, "25.75", UUID.randomUUID(), UUID.randomUUID());

        // When & Then - Get current balance
        given()
        .when()
            .get("/wallets/{id}/balance", walletId)
        .then()
            .statusCode(HttpStatus.OK.value())
            .body("balance", equalTo(176.0f)); // 100 + 50.25 + 25.75
    }

    @Test
    void shouldGetHistoricalBalanceAtSpecificTime() {
        // Given - Create wallet and make initial deposits
        UUID userId = UUID.randomUUID();
        String walletId = createWallet(userId);
        
        depositAmount(walletId, "100.00", UUID.randomUUID(), UUID.randomUUID());
        // Wait a moment to ensure timestamp difference
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        depositAmount(walletId, "50.25", UUID.randomUUID(), UUID.randomUUID());
        // Wait a moment to ensure timestamp difference
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        depositAmount(walletId, "25.75", UUID.randomUUID(), UUID.randomUUID());

        // Wait a moment to ensure timestamp difference
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Record current time after deposits
        LocalDateTime afterDepositsTime = LocalDateTime.now();
        
        String alternativeTimestamp = afterDepositsTime.format(DateTimeFormatter.ISO_DATE_TIME);
        
        given()
        .when()
            .get("/wallets/{id}/balance?at={timestamp}", walletId, alternativeTimestamp)
        .then()
            .log().all()
            .statusCode(HttpStatus.OK.value())
            .body("balance", equalTo(176.0f));
       
    }

    @Test
    void shouldGetHistoricalBalanceUsingDifferentApproach() {
        // Alternative approach: Test historical balance step by step
        
        UUID userId = UUID.randomUUID();
        String walletId = createWallet(userId);
        
        // Step 1: Initial deposit
        depositAmount(walletId, "100.00", UUID.randomUUID(), UUID.randomUUID());

        // Wait a bit
        try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        
        // Get timestamp after first deposit
        LocalDateTime afterFirstDeposit = LocalDateTime.now();
        
        // Wait a bit
        try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        
        // Step 2: Second deposit
        depositAmount(walletId, "50.00", UUID.randomUUID(), UUID.randomUUID());
 
        // Wait a bit
        try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        
        // Test historical balance at time after first deposit but before second
        String timestamp = afterFirstDeposit.format(DateTimeFormatter.ISO_DATE_TIME);
        
        given()
        .when()
            .get("/wallets/{id}/balance?at={timestamp}", walletId, timestamp)
        .then()
//            .log().all()
            .statusCode(HttpStatus.OK.value())
            .body("balance", equalTo(100.0f));
    }

    @Test
    void shouldHandleMixedDepositsAndWithdrawals() {
        // Given - Create wallet
        UUID userId = UUID.randomUUID();
        String walletId = createWallet(userId);
        
        // Make various transactions
        depositAmount(walletId, "200.00", UUID.randomUUID(), UUID.randomUUID());
        withdrawAmount(walletId, "50.00", UUID.randomUUID(), UUID.randomUUID());
        depositAmount(walletId, "25.50", UUID.randomUUID(), UUID.randomUUID());
        withdrawAmount(walletId, "75.25", UUID.randomUUID(), UUID.randomUUID());

        // When & Then - Verify final balance
        given()
        .when()
            .get("/wallets/{id}/balance", walletId)
        .then()
            .statusCode(HttpStatus.OK.value())
            .body("balance", equalTo(100.25f)); // 200 - 50 + 25.50 - 75.25
    }

    @Test
    void shouldHandleWithdrawalWithInsufficientBalance() {
        // Given - Create wallet with small balance
        UUID userId = UUID.randomUUID();
        String walletId = createWallet(userId);
        depositAmount(walletId, "50.00", UUID.randomUUID(), UUID.randomUUID());

        // When & Then - Try to withdraw more than available
        given()
            .contentType(ContentType.JSON)
            .header("Idempotency-Key", UUID.randomUUID().toString())
            .body(new TransactionRequest(new BigDecimal("100.00"), UUID.randomUUID()))
        .when()
            .post("/wallets/{id}/withdraw", walletId)
        .then()
            .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    // Test without historical balance to verify basic functionality works
    @Test
    void shouldGetCurrentBalanceSimple() {
        UUID userId = UUID.randomUUID();
        String walletId = createWallet(userId);
        
        depositAmount(walletId, "100.00", UUID.randomUUID(), UUID.randomUUID());
        
        given()
        .when()
            .get("/wallets/{id}/balance", walletId)
        .then()
            .log().all()
            .statusCode(HttpStatus.OK.value())
            .body("balance", equalTo(100.0f));
    }

    // Helper method to create wallet
    private String createWallet(UUID userId) {
        return given()
            .contentType(ContentType.JSON)
            .body(new CreateWalletRequest(userId))
        .when()
            .post("/wallets")
        .then()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .path("walletId");
    }

    // Helper methods for deposits and withdrawals with idempotency key
    private void depositAmount(String walletId, String amount, UUID transactionId, UUID idempotencyKey) {
        given()
            .contentType(ContentType.JSON)
            .header("Idempotency-Key", idempotencyKey.toString())
            .body(new TransactionRequest(new BigDecimal(amount), transactionId))
        .when()
            .post("/wallets/{id}/deposit", walletId)
        .then()
            .statusCode(HttpStatus.OK.value());
    }

    private void withdrawAmount(String walletId, String amount, UUID transactionId, UUID idempotencyKey) {
        given()
            .contentType(ContentType.JSON)
            .header("Idempotency-Key", idempotencyKey.toString())
            .body(new TransactionRequest(new BigDecimal(amount), transactionId))
        .when()
            .post("/wallets/{id}/withdraw", walletId)
        .then()
            .statusCode(HttpStatus.OK.value());
    }

    // Inner classes for request objects
    static class CreateWalletRequest {
        private final UUID userId;

        public CreateWalletRequest(UUID userId) {
            this.userId = userId;
        }

        public UUID getUserId() {
            return userId;
        }
    }

    static class TransactionRequest {
        private final BigDecimal amount;
        private final UUID transactionId;

        public TransactionRequest(BigDecimal amount, UUID transactionId) {
            this.amount = amount;
            this.transactionId = transactionId;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public UUID getTransactionId() {
            return transactionId;
        }
    }
}