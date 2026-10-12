package com.finance.infrastructure.bok;

import com.finance.infrastructure.bok.config.BokDemoProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(BokDemoProperties.class)
public class BokServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BokServiceApplication.class, args);
    }
}
