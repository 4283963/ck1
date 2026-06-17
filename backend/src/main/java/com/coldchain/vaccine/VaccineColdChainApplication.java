package com.coldchain.vaccine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class VaccineColdChainApplication {
    public static void main(String[] args) {
        SpringApplication.run(VaccineColdChainApplication.class, args);
    }
}
