package br.com.finaya.integrationtests.controller.withjson;

import static io.restassured.RestAssured.given;
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
