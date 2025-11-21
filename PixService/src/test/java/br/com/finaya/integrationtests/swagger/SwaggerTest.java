package br.com.finaya.integrationtests.swagger;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import br.com.finaya.integrationtests.testcontainers.AbstractIntegrationTest;
import io.restassured.RestAssured;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class SwaggerTest extends AbstractIntegrationTest {
	
    @LocalServerPort
    private int port;
	
    @BeforeEach
    void setup() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;    	
    }

	@Test
	public void shoudlDisplaySwaggerUiPage() {
		var content = 
		given()
			.basePath("/swagger-ui/index.html")
			.when()
				.get()
			.then()
				.statusCode(200)
			.extract()
				.body()
					.asString();
		assertTrue(content.contains("Swagger UI"));
	}
}
