package com.paulathamyres.voicebudget;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class VoiceBudgetApplication {

    public static void main(String[] args) {
        SpringApplication.run(VoiceBudgetApplication.class, args);
    }
}
