package com.kampotourism;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class KampotourismApplication {

    public static void main(String[] args) {
        SpringApplication.run(KampotourismApplication.class, args);
    }
}
