package com.beautyshop;

import com.beautyshop.bootstrap.LocalDatabaseMigration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BeautyShopApplication {
    public static void main(String[] args) {
        if (args.length == 1 && "--migrate-only".equals(args[0])) {
            LocalDatabaseMigration.run();
            return;
        }
        SpringApplication.run(BeautyShopApplication.class, args);
    }
}
