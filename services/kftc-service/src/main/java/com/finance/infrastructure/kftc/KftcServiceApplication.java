package com.finance.infrastructure.kftc;

import com.finance.infrastructure.kftc.config.KftcProperties;
import com.finance.infrastructure.kftc.config.KftcDemoProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({KftcProperties.class, KftcDemoProperties.class})
public class KftcServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(KftcServiceApplication.class, args);
    }
}
