package br.com.finaya.integrationtests.controller.withjson;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

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
import br.com.finaya.integrationtests.testcontainers.AbstractIntegrationTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

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
        	.log().all()
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
