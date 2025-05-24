package com.ynan.test;

import com.ynan.service.HelloService;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;
import org.junit.runner.RunWith;
import org.junit.runner.Runner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * SpringBoot项目使用Junit4，只需要@SpringBootTest注解即可
 * classes属性可以无需指定
 * 如果没有指定会默认以测试类的包和子包找 @SpringBootApplication的类，如果没找到或找到多个，报错
 */
@RunWith(SpringRunner.class)
//@SpringBootTest(classes = RootMain.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
public class TestMain4Junit4SpringBoot {

	@Autowired
	private HelloService helloService;

	@Test
	public void test01() {
		helloService.say(" ----------- junit4 -----------");
	}

	public static void main(String[] args) {
		Request request = Request.aClass(TestMain4Junit4SpringBoot.class);
		Runner runner = request.getRunner();

		JUnitCore jUnitCore = new JUnitCore();
		Result result = jUnitCore.run(runner);
		System.out.println(" ---------------- end ----------------- ");
	}

	@BeforeClass
	public static void beforeClass() {
		System.out.println("---------------- beforeClass ---------------");
	}

	@AfterClass
	public static void afterClass() {
		System.out.println("---------------- afterClass ---------------");
	}

	@Before
	public void before() {
		System.out.println("---------------- before ---------------");
	}

	@After
	public void after() {
		System.out.println("---------------- after ---------------");
	}
}
