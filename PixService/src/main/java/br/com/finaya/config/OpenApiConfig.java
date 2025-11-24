package br.com.finaya.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI pixServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Pix Service API")
                        .description("""
                            Microserviço de carteira digital com suporte a transferências Pix. 
                            Garante consistência sob concorrência e idempotência em todas as operações.
                            
                            ## Funcionalidades Principais:
                            -  Criação e gerenciamento de carteiras
                            -  Registro de chaves Pix (EMAIL, PHONE, EVP)
                            -  Consulta de saldo atual e histórico
                            -  Depósitos e saques com idempotência
                            -  Transferências Pix entre carteiras
                            -  Webhooks para confirmação/rejeição de Pix
                            -  Controle de concorrência e consistência
                            
                            ## Idempotência:
                            Todas as operações que modificam saldo requerem o header `Idempotency-Key` 
                            com um UUID para garantir processamento exatamente uma vez.
                            """)
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Equipe Pix Service")
                                .email("suporte@pixservice.com")
                                .url("http://localhost:8080"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Servidor de Desenvolvimento")
                ))
                .components(new Components()
                        .addSecuritySchemes("IdempotencyKey", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("Idempotency-Key")
                                .description("UUID para garantir idempotência nas operações")));
    }
}