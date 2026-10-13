package com.example.agenticorderservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AgenticOrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgenticOrderServiceApplication.class, args);
    }

    // Temporary intentional SonarQube probe for S1144 and S106; remove after verification.
    private static void sonarQualityGateProbe() {
        System.out.println("Intentional SonarQube quality gate probe");
    }
}
