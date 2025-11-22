package br.com.finaya.configs;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Profile("test")
@Order(1)
public class DatabaseResetComponent implements BeanFactoryPostProcessor {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseResetComponent.class);

   
    private String datasourceUrl = "jdbc:mysql://localhost:3306/finaya_test";

    private String username = "root";

    private String password = "local";

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
            logger.info("🔄 BeanFactoryPostProcessor - Resetando banco ANTES de qualquer bean...");
            resetDatabaseDirectly();
    }

    private void resetDatabaseDirectly() {
        // Extrai informações da URL
        String cleanUrl = datasourceUrl.split("\\?")[0]; // Remove query parameters
        String baseUrl = cleanUrl.substring(0, cleanUrl.lastIndexOf("/"));
        String databaseName = "finaya_test";

        logger.info("Conectando em: {}", baseUrl);
        
        try (Connection connection = DriverManager.getConnection(baseUrl, username, password)) {
            
            // Drop e create database
            connection.createStatement().execute("DROP DATABASE IF EXISTS " + databaseName);
            connection.createStatement().execute("CREATE DATABASE " + databaseName + " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            
            logger.info("✅ Banco resetado com sucesso via BeanFactoryPostProcessor!");
            
        } catch (SQLException e) {
            logger.error("❌ Falha no reset direto do banco: {}", e.getMessage());
            // Não relançar para não parar a aplicação
        }
    }
}
