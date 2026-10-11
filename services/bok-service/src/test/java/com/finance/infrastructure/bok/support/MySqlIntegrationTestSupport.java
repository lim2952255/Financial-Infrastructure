package com.finance.infrastructure.bok.support;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

// BOK 결제 테스트를 수행할때 사용할 MySQL을 TestContainer로 실행한다.
@Testcontainers
@ActiveProfiles("testcontainers")
public abstract class MySqlIntegrationTestSupport {

    @Container
    @ServiceConnection
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4")
            .withDatabaseName("bok_test")
            .withUsername("bok_test_user")
            .withPassword("bok_test_password");
}
