package com.exam408;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.exam408.mapper")
public class Exam408Application {
    public static void main(String[] args) {
        SpringApplication.run(Exam408Application.class, args);
    }
}
