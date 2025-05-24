package com.ynan.test;

import com.ynan.service.HelloService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

//@SpringBootTest(classes = RootMain.class)
@SpringBootTest
public class TtestMain4Junit5 {

	@Autowired
	private HelloService helloService;

	@BeforeAll
	public static void beforeAll() {
		System.out.println("----------- beforeAll ----------------");
	}

	@BeforeEach
	public void beforeEach() {
		System.out.println("------------- beforeEach ----------------");
	}

	@Test
	@org.junit.Test
	public void test01() {
		helloService.say(" test01 ");
	}

	public static void main(String[] args) {
//		ClassSelector classSelector = DiscoverySelectors.selectClass(TestMain01.class);

//		LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder
//				.request()
//				.selectors(classSelector)
//				.build();
//
//		Launcher launcher = LauncherFactory.create();
//		launcher.execute(request);

		System.out.println("hello world!");

	}
}
