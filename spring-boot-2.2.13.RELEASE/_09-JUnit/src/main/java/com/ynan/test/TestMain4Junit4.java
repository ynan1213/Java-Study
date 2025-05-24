package com.ynan.test;

import com.ynan.service.HelloService;
import com.ynan.service.impl.HelloServiceImpl;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;
import org.junit.runner.RunWith;
import org.junit.runner.Runner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * 正常情况下在SpringBoot项目中使用Junit只需要引入spring-boot-starter-test即可
 * 但是想要在/src/main/java下写Junit4代码，需要单独引入junit、spring-test、hamcrest
 * 虽然spring-boot-starter-test间接引入了junit、spring-test等，但scope是test
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = HelloServiceImpl.class)
public class TestMain4Junit4 {

	@Autowired
	private HelloService helloService;

	@Test
	public void test01() {
		helloService.say(" ----------- junit4 -----------");
	}

	public static void main(String[] args) {
		Request request = Request.aClass(TestMain4Junit4.class);
		Runner runner = request.getRunner();

		JUnitCore jUnitCore = new JUnitCore();
		Result result = jUnitCore.run(runner);
		System.out.println(" ---------------- end ----------------- ");
	}

}
