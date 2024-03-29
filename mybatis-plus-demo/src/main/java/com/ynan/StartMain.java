package com.ynan;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @Author yuannan
 * @Date 2021/11/8 14:23
 */
@SpringBootApplication
@MapperScan("com.ynan.dao")
public class StartMain {

    public static void main(String[] args) {
        SpringApplication.run(StartMain.class, args);
    }

}
