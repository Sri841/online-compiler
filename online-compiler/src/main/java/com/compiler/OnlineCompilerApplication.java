package com.compiler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the Online Compiler application.
 * Run this class to start the Spring Boot server.
 */
@SpringBootApplication
public class OnlineCompilerApplication {

    public static void main(String[] args) {
        SpringApplication.run(OnlineCompilerApplication.class, args);
        System.out.println("================================================");
        System.out.println("  Online Compiler is RUNNING!");
        System.out.println("  Backend URL : http://localhost:8080");
        System.out.println("  Health Check: http://localhost:8080/api/health");
        System.out.println("  Now open frontend/index.html in your browser");
        System.out.println("================================================");
    }
}
