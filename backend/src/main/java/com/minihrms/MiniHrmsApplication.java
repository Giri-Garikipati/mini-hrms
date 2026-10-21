package com.minihrms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point. Running main() starts an embedded web server on port 8080.
 *
 * @SpringBootApplication tells Spring to scan this package (and sub-packages)
 * for classes marked @Entity, @Repository, @RestController, @Service etc.,
 * create objects for them, and wire them together automatically.
 */
@SpringBootApplication
public class MiniHrmsApplication {
    public static void main(String[] args) {
        SpringApplication.run(MiniHrmsApplication.class, args);
    }
}
