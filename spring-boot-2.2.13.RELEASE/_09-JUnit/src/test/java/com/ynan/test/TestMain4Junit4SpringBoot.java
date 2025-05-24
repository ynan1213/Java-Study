package com.ynan.test;

import com.ynan.service.HelloService;
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
 * classes属性可以无需指定
 * 		如果没有指定会默认以测试类的包和子包找 @SpringBootApplication的类，如果没找到或找到多个，报错
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class TestMain4Junit4SpringBoot {

	@Autowired
	private HelloService helloService;

	@Test
	public void test01() {
		helloService.say(" ----------- test junit4 -----------");
	}

	/**
	 * /src/test/java的测试类中使用main方法会报错:错误:
	 * 在类 com.ynan.test.TestMain4Junit4SpringBoot 中找不到 main 方法
	 */
	public static void main(String[] args) {
		Request request = Request.aClass(TestMain4Junit4SpringBoot.class);
		Runner runner = request.getRunner();

		JUnitCore jUnitCore = new JUnitCore();
		Result result = jUnitCore.run(runner);
		System.out.println(" ---------------- end ----------------- ");
	}

}
