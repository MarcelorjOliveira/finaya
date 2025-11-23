package br.com.finaya.integrationtests.controller.withjson;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

import br.com.finaya.controllers.WalletController;
import br.com.finaya.controllers.WalletController.CreateWalletRequest;
import br.com.finaya.controllers.WalletController.WithdrawRequest;
import br.com.finaya.integrationtests.testcontainers.AbstractIntegrationTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Header;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(OrderAnnotation.class)
@ActiveProfiles("test")
public class PixControllerIntegrationTest extends AbstractIntegrationTest {
	
    @LocalServerPort
    private int port;
	
    @BeforeEach
    void setup() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;    	
    	
    }
    
        @Test
        void shouldInitiatePixTransferSuccessfully() {
            // Create source wallet and deposit
            UUID sourceUserId = UUID.randomUUID();
            String sourceWalletId = given()
                .contentType(ContentType.JSON)
                .body(new WalletController.CreateWalletRequest(sourceUserId))
            .when()
                .post("/wallets")
            .then()
                .extract().path("walletId");
            
            UUID depositIdempotencyKey = UUID.randomUUID();

            given()
                .contentType(ContentType.JSON)
                .header(new Header("Idempotency-Key", depositIdempotencyKey.toString()))
                .body(new WalletController.DepositRequest(new BigDecimal(200.00)))
            .when()
                .post("/wallets/{id}/deposit", sourceWalletId);

            // Create target wallet and Pix key
            UUID targetUserId = UUID.randomUUID();
            String targetWalletId = given()
                .contentType(ContentType.JSON)
                .body(new WalletController.CreateWalletRequest(targetUserId))
            .when()
                .post("/wallets")
            .then()
                .extract().path("walletId");

            given()
                .contentType(ContentType.JSON)
                .body(new RegisterPixKeyRequest("target@email.com", "EMAIL"))
            .when()
                .post("/wallets/{walletId}/pix-keys", targetWalletId);

            // Initiate Pix transfer
            UUID pixTransferIdempotencyKey = UUID.randomUUID();
            given()
                .contentType(ContentType.JSON)
                .header(new Header("Idempotency-Key", pixTransferIdempotencyKey.toString()))
                .body(new PixTransferRequest(UUID.fromString(sourceWalletId), "target@email.com", new BigDecimal(100.00)))
            .when()
                .post("/pix/transfers")
            .then()
            	.log().all()
                .statusCode(HttpStatus.OK.value())
                .body("endToEndId", notNullValue())
                .body("status", equalTo("PENDING"));
        }
        
        record RegisterPixKeyRequest(String key, String type) {}
        record PixTransferRequest(UUID fromWalletId, String toPixKey, BigDecimal amount) {}

}
