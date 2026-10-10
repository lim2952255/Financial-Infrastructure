package com.finance.infrastructure.bank.support;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

// 통합테스트환경에서 사용할 MySQL 테스트컨테이너를 설정하는 추상클래스, 실제 테스트 클래스에서 해당 클래스를 상속받으면 MySQL 테스틑 컨테이너를 활용할 수 있다.
@Testcontainers
@ActiveProfiles("testcontainers") // testcontainer 설정파일을 활성화한다
public abstract class MySqlIntegrationTestSupport {

    // 새로운 MySQL Database를 실행하는 컨테이너를 실행한다.
    // @Container가 붙으면 Testcontainers가 이를 관리해야 하는 컨테이너로 인식한다
    @Container
    @ServiceConnection // @TestContainers로 띄운 외부서비스(Mysql, Redis)등의 접속 정보를 Spring Boot에게 자동으로 알려주는 애노테이션
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4")
            .withDatabaseName("bank_test")
            .withUsername("bank_test_user")
            .withPassword("bank_test_password");
}
