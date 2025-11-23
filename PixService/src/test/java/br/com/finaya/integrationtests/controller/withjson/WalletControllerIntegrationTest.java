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

import br.com.finaya.controllers.WalletController.CreateWalletRequest;
import br.com.finaya.controllers.WalletController.WithdrawRequest;
import br.com.finaya.integrationtests.testcontainers.AbstractIntegrationTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Header;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(OrderAnnotation.class)
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
    void shouldWithdrawFromWalletSuccessfully() {
        // First create a wallet and deposit
        UUID userId = UUID.randomUUID();
        String walletId = given()
            .contentType(ContentType.JSON)
            .body(new CreateWalletRequest(userId))
        .when()
            .post("/wallets")
        .then()
            .extract().path("walletId");

        given()
            .contentType(ContentType.JSON)
            .header(new Header("Idempotency-Key", "deposit-test-2"))
            .body(new DepositRequest(new BigDecimal(200.00)))
        .when()
            .post("/wallets/{id}/deposit", walletId);

        // Then withdraw
        given()
            .contentType(ContentType.JSON)
            .header(new Header("Idempotency-Key", "withdraw-test-1"))
            .body(new WithdrawRequest(new BigDecimal(100.00)))
        .when()
            .post("/wallets/{id}/withdraw", walletId)
        .then()
            .statusCode(HttpStatus.OK.value());
    }
    
    record DepositRequest(BigDecimal amount) {}
    
    @Test
    void shouldDepositToWalletSuccessfully() {
        // First create a wallet
        UUID userId = UUID.randomUUID();
        String walletId = given()
            .contentType(ContentType.JSON)
            .body(new CreateWalletRequest(userId))
        .when()
            .post("/wallets")
        .then()
            .extract().path("walletId");

        // Then deposit
        given()
            .contentType(ContentType.JSON)
            .header(new Header("Idempotency-Key", "deposit-test-1"))
            .body(new DepositRequest(new BigDecimal(100.00)))
        .when()
            .post("/wallets/{id}/deposit", walletId)
        .then()
            .statusCode(HttpStatus.OK.value());
    }   

    @Test
    void shouldRegisterPixKeySuccessfully() {
        // First create a wallet
        UUID userId = UUID.randomUUID();
        String walletId = given()
            .contentType(ContentType.JSON)
            .body(new CreateWalletRequest(userId))
        .when()
            .post("/wallets")
        .then()
            .extract().path("walletId");

        // Then register Pix key
        given()
            .contentType(ContentType.JSON)
            .body(new RegisterPixKeyRequest("test@email.com", "EMAIL"))
        .when()
            .post("/wallets/{walletId}/pix-keys", walletId)
        .then()
            .statusCode(HttpStatus.OK.value())
            .body("pixKeyId", notNullValue())
            .body("key", equalTo("test@email.com"))
            .body("type", equalTo("EMAIL"))
            .body("status", equalTo("ACTIVE"));
    }
    
    @Test
    void shouldReturnErrorForInvalidEmailFormat() {
        UUID userId = UUID.randomUUID();
        String walletId = given()
            .contentType(ContentType.JSON)
            .body(new CreateWalletRequest(userId))
        .when()
            .post("/wallets")
        .then()
            .extract().path("walletId");

        given()
            .contentType(ContentType.JSON)
            .body(new RegisterPixKeyRequest("invalid-email", "EMAIL"))
        .when()
            .post("/wallets/{walletId}/pix-keys", walletId)
        .then()
            .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    void shouldReturnErrorForDuplicatePixKey() {
        UUID userId = UUID.randomUUID();
        String walletId = given()
            .contentType(ContentType.JSON)
            .body(new CreateWalletRequest(userId))
        .when()
            .post("/wallets")
        .then()
            .extract().path("walletId");

        String pixKey = "unique@email.com";

        // First registration
        given()
            .contentType(ContentType.JSON)
            .body(new RegisterPixKeyRequest(pixKey, "EMAIL"))
        .when()
            .post("/wallets/{walletId}/pix-keys", walletId)
        .then()
        	//.log().all()
            .statusCode(HttpStatus.OK.value());

        // Second registration with same key
        given()
            .contentType(ContentType.JSON)
            .body(new RegisterPixKeyRequest(pixKey, "EMAIL"))
        .when()
            .post("/wallets/{walletId}/pix-keys", walletId)
        .then()
            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    record RegisterPixKeyRequest(String key, String type) {}

    @Test
    void shouldCreateWalletSuccessfully() {
        UUID userId = UUID.randomUUID();

        given()
            .contentType(ContentType.JSON)
            .body(new CreateWalletRequest(userId))
        .when()
            .post("/wallets")
        .then()
            .statusCode(HttpStatus.OK.value())
            .body("walletId", notNullValue());
    }
}
