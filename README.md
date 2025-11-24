Pix Service - Microserviço de Carteira Digital

Um microserviço para gerenciamento de carteiras digitais com suporte a operações Pix, desenvolvido em Java com Spring Boot seguindo as principais boas praticas.
* Funcionalidades

    Gestão de Carteiras: Criação e consulta de carteiras digitais

    Chaves Pix: Registro e vinculação de chaves Pix únicas (email, telefone, EVP)

    Operações Financeiras: Depósito, saque e transferências Pix

    Consultas de Saldo: Saldo atual e histórico em timestamp específico

    Webhook Pix: Simulação de confirmações de transações (CONFIRMED/REJECTED)

    Idempotência: Garantia de processamento único para requisições duplicadas

    Concorrência: Controle de race conditions em operações simultâneas

* Arquitetura

O projeto segue as principais boas praticas com as seguintes camadas:

    Model: Camada de dominio da API
    
    Service : Camada de negocio e principais regras de negocio

    Repository : Camada de dados
    
    Controller : Camada de apresentação

📋 Pré-requisitos

    Java 18 ou superior

    Maven 3.6+

    Docker e Docker Compose (opcional)

🛠️ Instalação e Execução
Opção 1: Docker Compose (Recomendada)
bash

# Clone o repositório
git clone git@github.com:MarcelorjOliveira/finaya.git
cd finaya

# Execute banco de dados com Docker Compose
cd mysql_docker
docker-compose -f bancodedados.yml up -d

cd ..

# Execute a aplicação
cd PixService
mvn spring-boot:run


🧪 Testes
Executar Testes
bash

# Todos os testes
mvn test

# Apenas testes unitários
mvn test -Dtest="*ServiceTest"

# Apenas testes de integração
mvn test -Dtest="*IntegrationTest"

Cobertura de Testes

    Testes Unitários: Domínio e serviços

    Testes de Integração: Controladores

🔧 Decisões de Design
1. Idempotência

    Tabela de Idempotência: idempotency_records com chave única (scope, key)

2. Controle de Concorrência

    Optimistic Locking: Versão na entidade PixKey

    Pessimistic Write: Utilizei para selecionar a Wallet em operações que envolvem saque, debito e saldo para garantir que só uma operação é debitada por vez

3. Modelagem de Dados
    Usei binary(16) para armazenar os UUID para aumentar a eficiência de armazenamento e aumentar desempenho em índices

    Ledger com entradas imutáveis

    Saldo calculado a partir do ledger

    Timestamp-based querying para saldo histórico

4. Máquina de Estados
text

PENDING → CONFIRMED
         ↘
          REJECTED


Logs Estruturados

    Contexto de transações (endToEndId, eventId)

    Métricas de performance

5. Estrutura do Banco
Tabelas Principais

    wallets: Carteiras dos usuários

    pix_keys: Chaves Pix registradas

    ledger_entries: Lançamentos financeiros (event sourcing)

    idempotency_records: Controle de idempotência

    pix_transfers: Transferências Pix

    webhook_events: Eventos de webhook processados

6. Limitações e Trade-offs
Por Tempo de Desenvolvimento

    Testes : Os testes de integração precisam do banco de dados levantado porque eles gravam em uma base de teste

    Autenticação: Sem autenticação adequada - em produção exigiria OAuth2/JWT

    Cache: Não implementado - Redis melhoraria performance

    Message Broker não implementado - Kafka/RabbitMQ para assíncrono

    Circuit Breaker não implementado Resilência básica - Spring Cloud Circuit Breaker em produção

    API Documentation: OpenAPI básico - Swagger UI completo em produção

7. Time Tracking

    Implementação Core: 10 horas

    Testes e Idempotência: 6 horas

    Concorrência e Race Conditions: 4 horas

    Documentação e Ajustes: 2 horas

    Total Aproximado: 22 horas

8. Próximas Melhorias

    Cache Layer: Redis para consultas frequentes

    Async Processing: Message broker para operações demoradas

    API Versioning: Versionamento de endpoints

    Rate Limiting: Limitação de requisições

    Advanced Monitoring: APM e distributed tracing

    Security Enhancements: OAuth2, SSL, etc.
