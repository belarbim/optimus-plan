package com.utmost.optimusplan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OptimusPlanApplication {

    public static void main(String[] args) {
        SpringApplication.run(OptimusPlanApplication.class, args);
    }
}
