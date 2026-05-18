package com.evho.usonly;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@Slf4j
@SpringBootApplication
@EnableScheduling
@EnableAsync
@EnableCaching
public class UsOnlyApplication {

    public static void main(String[] args) {
        SpringApplication.run(UsOnlyApplication.class, args);
        log.info("""

                ╔══════════════════════════════════════╗
                ║     UsOnly Server Started Successfully     ║
                ╚══════════════════════════════════════╝
                """);
    }

}
