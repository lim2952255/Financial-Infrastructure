package com.finance.infrastructure.kftc;

import com.finance.infrastructure.kftc.config.KftcProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(KftcProperties.class)
public class KftcServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(KftcServiceApplication.class, args);
    }
}
