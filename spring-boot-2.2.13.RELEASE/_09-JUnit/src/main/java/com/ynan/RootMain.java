package com.ynan;

import com.ynan.service.HelloService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication(scanBasePackages = "com.ynan.service")
public class RootMain {

	public static void main(String[] args) {

		ConfigurableApplicationContext applicationContext = SpringApplication.run(RootMain.class, args);
		HelloService helloService = applicationContext.getBean(HelloService.class);
		helloService.say("hello world !!!");

	}

}
