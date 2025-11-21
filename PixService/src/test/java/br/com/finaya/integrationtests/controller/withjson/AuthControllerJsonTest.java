 

package br.com.finaya.integrationtests.controller.withjson;

import static io.restassured.RestAssured.given;
import static org.junit.Assert.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import br.com.finaya.configs.TestConfigs;
import br.com.finaya.data.vo.v1.security.AccountCredentialsVO;
import br.com.finaya.data.vo.v1.security.TokenVO;
import br.com.finaya.integrationtests.testcontainers.AbstractIntegrationTest;
import io.restassured.RestAssured;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(OrderAnnotation.class)
@ActiveProfiles("test")
public class AuthControllerJsonTest extends AbstractIntegrationTest {

	private static TokenVO tokenVO;
	
    @LocalServerPort
    private int port;
	
    @BeforeEach
    void setup() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;    	
    	
    }

	@Test
	@Order(1)
	public void testSigin() throws JsonMappingException, JsonProcessingException {
		AccountCredentialsVO user = new AccountCredentialsVO("marcelo", "admin234");

		tokenVO = given().basePath("/auth/signin")
				.contentType(TestConfigs.CONTENT_TYPE_JSON).body(user).when().post().then()
			    //.log().all()
				.statusCode(200).extract()
				.body().as(TokenVO.class);

		assertNotNull(tokenVO.getAccessToken());
		assertNotNull(tokenVO.getRefreshToken());

	}

	@Test
	@Order(2)
	public void testRefresh() throws JsonMappingException, JsonProcessingException {

		var newtokenVO = given().basePath("/auth/refresh")
				.contentType(TestConfigs.CONTENT_TYPE_JSON).pathParam("username", tokenVO.getUsername())
				.header(TestConfigs.HEADER_PARAM_AUTHORIZATION, "Bearer " + tokenVO.getRefreshToken()).when().put("{username}")
				.then().statusCode(200).extract().body().as(TokenVO.class);

		assertNotNull(newtokenVO.getAccessToken());
		assertNotNull(newtokenVO.getRefreshToken());

	}

}
