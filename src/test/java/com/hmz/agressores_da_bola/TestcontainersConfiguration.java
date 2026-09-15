package com.hmz.agressores_da_bola;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * MySQL descartável para os testes que precisam de banco. O
 * {@code @ServiceConnection} entrega URL, usuário e senha do contêiner ao
 * datasource e ao Flyway, então a suíte não depende do MySQL da máquina nem
 * de credenciais locais — só do Docker em execução.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    MySQLContainer mysqlContainer() {
        return new MySQLContainer(DockerImageName.parse("mysql:8.0"));
    }
}
