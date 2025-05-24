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
 * 为什么/src/test/java下的代码可以直接访问/src/main/java下的类？
 * 问了元宝，得到的答案是IDEA插件或者Maven插件实现的，内部是通过创建特定的ClassLoader，
 * 可以同时拥有target/test-classes和target/classes的权限，前者优先级高
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = HelloServiceImpl.class)
public class TestMain4Junit4 {

	@Autowired
	private HelloService helloService;

	@Test
	public void test01() {
		helloService.say(" ----------- test junit4 -----------");
	}

	/**
	 * /src/test/java的测试类中使用main方法会报错:错误:
	 * 		在类 com.ynan.test.TestMain4Junit4 中找不到 main 方法
	 */
	public static void main(String[] args) {
		Request request = Request.aClass(TestMain4Junit4.class);
		Runner runner = request.getRunner();

		JUnitCore jUnitCore = new JUnitCore();
		Result result = jUnitCore.run(runner);
		System.out.println(" ---------------- end ----------------- ");
	}
}
