package com.noh.stpclient;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class StpClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(StpClientApplication.class, args);
    }

}
