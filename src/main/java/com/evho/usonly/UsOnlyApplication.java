package com.evho.usonly;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class UsOnlyApplication {

    public static void main(String[] args) {
        SpringApplication.run(UsOnlyApplication.class, args);
        System.out.println("====================================");
        System.out.println("  UsOnly Server Started Successfully");
        System.out.println("====================================");
    }

}
