package br.com.finaya.integrationtests.controller.withjson;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import br.com.finaya.integrationtests.testcontainers.AbstractIntegrationTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Header;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class PixControllerIntegrationTest extends AbstractIntegrationTest {

	@LocalServerPort
	private int port;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@BeforeEach
	void setup() {
		RestAssured.baseURI = "http://localhost";
		RestAssured.port = port;
		truncateAllTables();
	}

	public void truncateAllTables() {
		// Define a transação
		DefaultTransactionDefinition def = new DefaultTransactionDefinition();
		def.setName("MinhaTransacao");
		def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);

		TransactionStatus status = transactionManager.getTransaction(def);

		try {

		     jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
		        
		        List<String> tableNames = jdbcTemplate.queryForList(
		            "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = DATABASE()", 
		            String.class
		        );
		        
		        for (String tableName : tableNames) {
		            // Use DELETE + ALTER to reset auto-increment for tables that have it
		            jdbcTemplate.execute("DELETE FROM `" + tableName + "`");
		            jdbcTemplate.execute("ALTER TABLE `" + tableName + "` AUTO_INCREMENT = 1");
		        }
		        
		        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
			// Confirma (commit) a transação
			transactionManager.commit(status);
		} catch (Exception ex) {
			// Desfaz (rollback) a transação
			transactionManager.rollback(status);
			throw ex;
		}
	}

	private UUID createWallet(UUID userId) {
		String walletIdString = given().contentType(ContentType.JSON).body(new CreateWalletRequest(userId)).when()
				.post("/wallets").then().statusCode(HttpStatus.OK.value()).extract().path("walletId");

		return UUID.fromString(walletIdString);
	}

	private void depositToWallet(UUID walletId, String amount) {
		UUID depositKey = UUID.randomUUID();
		given().contentType(ContentType.JSON).header(new Header("Idempotency-Key", depositKey.toString()))
				.body(new DepositRequest(amount)).when().post("/wallets/{id}/deposit", walletId).then()
				.statusCode(HttpStatus.OK.value());
	}

	private void registerPixKey(UUID walletId, String key, String type) {
		given().contentType(ContentType.JSON).body(new RegisterPixKeyRequest(key, type)).when()
				.post("/wallets/{walletId}/pix-keys", walletId).then().statusCode(HttpStatus.OK.value());
	}

	private UUID setupSourceWallet() {
		UUID sourceUserId = UUID.randomUUID();
		UUID sourceWalletId = createWallet(sourceUserId);
		depositToWallet(sourceWalletId, "1000.00");
		registerPixKey(sourceWalletId, "source@email.com", "EMAIL");
		return sourceWalletId;
	}

	private UUID setupTargetWallet() {
		UUID targetUserId = UUID.randomUUID();
		UUID targetWalletId = createWallet(targetUserId);
		registerPixKey(targetWalletId, "target@email.com", "EMAIL");
		registerPixKey(targetWalletId, "+5511999999999", "PHONE");
		return targetWalletId;
	}

	@Test
	void shouldInitiatePixTransferSuccessfully() {
		// Setup
		UUID sourceWalletId = setupSourceWallet();
		UUID targetWalletId = setupTargetWallet();

		// Execute transfer
		UUID idempotencyKey = UUID.randomUUID();
		BigDecimal transferAmount = new BigDecimal("150.75");

		String endToEndIdString = given().contentType(ContentType.JSON)
				.header(new Header("Idempotency-Key", idempotencyKey.toString()))
				.body(new PixTransferRequest(sourceWalletId, "target@email.com", transferAmount)).when()
				.post("/pix/transfers").then().statusCode(HttpStatus.OK.value()).body("endToEndId", notNullValue())
				.body("status", equalTo("PENDING")).extract().path("endToEndId");

		UUID pixTransferEndToEndId = UUID.fromString(endToEndIdString);
		assertNotNull(pixTransferEndToEndId);

		// Verify source wallet balance was reserved
		given().when().get("/wallets/{id}/balance", sourceWalletId).then().statusCode(HttpStatus.OK.value())
				.body("balance", equalTo(849.25f)); // 1000 - 150.75
	}

	@Test
	void shouldInitiatePixTransferToPhoneNumber() {
		// Setup
		UUID sourceWalletId = setupSourceWallet();
		UUID targetWalletId = setupTargetWallet();

		// Execute transfer
		UUID idempotencyKey = UUID.randomUUID();
		BigDecimal transferAmount = new BigDecimal("50.25");

		String endToEndIdString = given().contentType(ContentType.JSON)
				.header(new Header("Idempotency-Key", idempotencyKey.toString()))
				.body(new PixTransferRequest(sourceWalletId, "+5511999999999", transferAmount)).when()
				.post("/pix/transfers").then().statusCode(HttpStatus.OK.value()).body("endToEndId", notNullValue())
				.body("status", equalTo("PENDING")).extract().path("endToEndId");

		assertNotNull(UUID.fromString(endToEndIdString));

		// Verify source wallet balance was reserved
		given().when().get("/wallets/{id}/balance", sourceWalletId).then().statusCode(HttpStatus.OK.value())
				.body("balance", equalTo(949.75f)); // 1000 - 50.25
	}

	@Test
	void shouldProcessConfirmedWebhookSuccessfully() {
		// Setup wallets and transfer
		UUID sourceWalletId = setupSourceWallet();
		UUID targetWalletId = setupTargetWallet();

		UUID transferIdempotencyKey = UUID.randomUUID();
		BigDecimal transferAmount = new BigDecimal("150.75");

		String endToEndIdString = given().contentType(ContentType.JSON)
				.header(new Header("Idempotency-Key", transferIdempotencyKey.toString()))
				.body(new PixTransferRequest(sourceWalletId, "target@email.com", transferAmount)).when()
				.post("/pix/transfers").then().extract().path("endToEndId");

		UUID pixTransferEndToEndId = UUID.fromString(endToEndIdString);

		// Process webhook
		UUID webhookIdempotencyKey = UUID.randomUUID();
		String eventId = "webhook-event-123";
		String eventType = "CONFIRMED";

		given().contentType(ContentType.JSON).header(new Header("Idempotency-Key", webhookIdempotencyKey.toString()))
				.body(new PixWebhookRequest(pixTransferEndToEndId, eventId, eventType, "2024-01-15T10:00:00Z")).when()
				.post("/pix/webhook").then().statusCode(HttpStatus.OK.value());

		// Verify target wallet received the funds
		given().when().get("/wallets/{id}/balance", targetWalletId).then().statusCode(HttpStatus.OK.value())
				.body("balance", equalTo(150.75f));
	}

	@Test
	void shouldProcessRejectedWebhookSuccessfully() {
		// Setup wallets and transfer
		UUID sourceWalletId = setupSourceWallet();
		UUID targetWalletId = setupTargetWallet();

		UUID transferIdempotencyKey = UUID.randomUUID();
		String rejectedTransferEndToEndIdString = given().contentType(ContentType.JSON)
				.header(new Header("Idempotency-Key", transferIdempotencyKey.toString()))
				.body(new PixTransferRequest(sourceWalletId, "target@email.com", new BigDecimal("100.00"))).when()
				.post("/pix/transfers").then().extract().path("endToEndId");

		UUID rejectedTransferEndToEndId = UUID.fromString(rejectedTransferEndToEndIdString);

		// Record balance before rejection
		Float balanceBeforeRejection = given().when().get("/wallets/{id}/balance", sourceWalletId).then().extract()
				.path("balance");

		// Process rejection webhook
		UUID webhookIdempotencyKey = UUID.randomUUID();
		given().contentType(ContentType.JSON).header(new Header("Idempotency-Key", webhookIdempotencyKey.toString()))
				.body(new PixWebhookRequest(rejectedTransferEndToEndId, "reject-event-456", "REJECTED",
						"2024-01-15T10:05:00Z"))
				.when().post("/pix/webhook").then().statusCode(HttpStatus.OK.value());

		// Verify funds were returned to source wallet
		given().when().get("/wallets/{id}/balance", sourceWalletId).then().statusCode(HttpStatus.OK.value())
				.body("balance", equalTo(balanceBeforeRejection + 100.0f));
	}

	@Test
	void shouldReturnErrorForNonExistentPixKey() {
		UUID sourceWalletId = setupSourceWallet();

		UUID idempotencyKey = UUID.randomUUID();
		String nonExistentPixKey = "nonexistent@email.com";

		given().contentType(ContentType.JSON).header(new Header("Idempotency-Key", idempotencyKey.toString()))
				.body(new PixTransferRequest(sourceWalletId, nonExistentPixKey, new BigDecimal("10.00"))).when()
				.post("/pix/transfers").then().statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
	}

	@Test
	void shouldReturnErrorForNonExistentSourceWallet() {
		UUID targetWalletId = setupTargetWallet();

		UUID idempotencyKey = UUID.randomUUID();
		UUID nonExistentWalletId = UUID.randomUUID();

		given().contentType(ContentType.JSON).header(new Header("Idempotency-Key", idempotencyKey.toString()))
				.body(new PixTransferRequest(nonExistentWalletId, "target@email.com", new BigDecimal("10.00"))).when()
				.post("/pix/transfers").then().statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
	}

	@Test
	void shouldReturnErrorForMissingIdempotencyKey() {
		UUID sourceWalletId = setupSourceWallet();
		UUID targetWalletId = setupTargetWallet();

		given().contentType(ContentType.JSON)
				.body(new PixTransferRequest(sourceWalletId, "target@email.com", new BigDecimal("10.00"))).when()
				.post("/pix/transfers").then().statusCode(HttpStatus.BAD_REQUEST.value());
	}

	@Test
	void shouldReturnErrorForInvalidIdempotencyKeyFormat() {
		UUID sourceWalletId = setupSourceWallet();
		UUID targetWalletId = setupTargetWallet();

		String invalidIdempotencyKey = "not-a-uuid";

		given().contentType(ContentType.JSON).header(new Header("Idempotency-Key", invalidIdempotencyKey))
				.body(new PixTransferRequest(sourceWalletId, "target@email.com", new BigDecimal("10.00"))).when()
				.post("/pix/transfers").then().statusCode(HttpStatus.BAD_REQUEST.value());
	}

	@Test
	void shouldHandleTransferToEVPKey() {
		UUID sourceWalletId = setupSourceWallet();
		UUID targetWalletId = setupTargetWallet();

		// Register an EVP key
		String evpKey = UUID.randomUUID().toString();
		given().contentType(ContentType.JSON).body(new RegisterPixKeyRequest(evpKey, "EVP")).when()
				.post("/wallets/{walletId}/pix-keys", targetWalletId).then().statusCode(HttpStatus.OK.value());

		UUID idempotencyKey = UUID.randomUUID();
		BigDecimal transferAmount = new BigDecimal("75.50");

		given().contentType(ContentType.JSON).header(new Header("Idempotency-Key", idempotencyKey.toString()))
				.body(new PixTransferRequest(sourceWalletId, evpKey, transferAmount)).when().post("/pix/transfers")
				.then().statusCode(HttpStatus.OK.value()).body("endToEndId", notNullValue())
				.body("status", equalTo("PENDING"));
	}

	@Test
	void shouldHandleDuplicatePixKeyRegistration() {
		UUID sourceWalletId = setupSourceWallet();

		String duplicateKey = "duplicate@email.com";

		// First registration
		given().contentType(ContentType.JSON).body(new RegisterPixKeyRequest(duplicateKey, "EMAIL")).when()
				.post("/wallets/{walletId}/pix-keys", sourceWalletId).then().statusCode(HttpStatus.OK.value());

		// Second registration with same key
		given().contentType(ContentType.JSON).body(new RegisterPixKeyRequest(duplicateKey, "EMAIL")).when()
				.post("/wallets/{walletId}/pix-keys", sourceWalletId).then()
				.statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
	}

	@Test
	void shouldHandleSelfTransfer() {
		UUID sourceWalletId = setupSourceWallet();

		UUID idempotencyKey = UUID.randomUUID();
		BigDecimal amount = new BigDecimal("10.00");

		given().contentType(ContentType.JSON).header(new Header("Idempotency-Key", idempotencyKey.toString()))
				.body(new PixTransferRequest(sourceWalletId, "source@email.com", amount)).when().post("/pix/transfers")
				.then().statusCode(HttpStatus.OK.value()).body("endToEndId", notNullValue())
				.body("status", equalTo("PENDING"));
	}

	// Record classes for request bodies
	record CreateWalletRequest(UUID userId) {
	}

	record DepositRequest(String amount) {
	}

	record RegisterPixKeyRequest(String key, String type) {
	}

	record PixTransferRequest(UUID fromWalletId, String toPixKey, BigDecimal amount) {
	}

	record PixWebhookRequest(UUID endToEndId, String eventId, String eventType, String occurredAt) {
	}

}