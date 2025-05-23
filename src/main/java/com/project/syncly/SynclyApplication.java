package com.project.syncly;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication(
        exclude = {
                io.awspring.cloud.autoconfigure.s3.S3AutoConfiguration.class
        }
)
@EnableJpaAuditing
public class SynclyApplication {

    public static void main(String[] args) {
        SpringApplication.run(SynclyApplication.class, args);
    }

}
