package com.evho.usonly;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class UsOnlyApplication {

    public static void main(String[] args) {
        SpringApplication.run(UsOnlyApplication.class, args);
        System.out.println("====================================");
        System.out.println("  UsOnly Server Started Successfully");
        System.out.println("====================================");
    }

}
