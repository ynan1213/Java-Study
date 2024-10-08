package com.ynan;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

/**
 * @Author yuannan
 * @Date 2022/7/16 21:39
 */
@SpringBootApplication
@RestController
@EnableDiscoveryClient
public class RibbonMain {

	@Autowired
	private RestTemplate restTemplate;

	public static void main(String[] args) {
		SpringApplication.run(RibbonMain.class);
	}

	@RequestMapping("/server")
	public String remote() {
		return restTemplate.getForObject("http://PROVIDER-00/hello", String.class);
	}
}
