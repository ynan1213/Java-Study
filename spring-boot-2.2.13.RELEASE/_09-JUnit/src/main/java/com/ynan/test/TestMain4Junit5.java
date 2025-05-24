//package com.ynan.test;
//
//import com.ynan.RootMain;
//import com.ynan.service.HelloService;
//import org.junit.jupiter.api.BeforeAll;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.platform.engine.discovery.ClassSelector;
//import org.junit.platform.engine.discovery.DiscoverySelectors;
//import org.junit.platform.launcher.Launcher;
//import org.junit.platform.launcher.LauncherDiscoveryRequest;
//import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
//import org.junit.platform.launcher.core.LauncherFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//
//@SpringBootTest(classes = RootMain.class)
////@SpringBootTest
//public class TestMain01 {
//
//	@Autowired
//	private HelloService helloService;
//
//	public TestMain01() {
//		System.out.println("------ 构造函数 ------");
//	}
//
//
//	@BeforeAll
//	public static void beforeAll() {
//		System.out.println("----------- beforeAll ----------------");
//	}
//
//	@BeforeEach
//	public void beforeEach() {
//		System.out.println("------------- beforeEach ----------------");
//	}
//
//	@Test
//	public void test01() {
//		helloService.say(" test01 ");
//	}
//
//	@Test
//	public void test02() {
//		helloService.say(" test02 ");
//	}
//
//	/**
//	 * 通常情况下想使用SpringBootTest只需要引入spring-boot-starter-test依赖即可。
//	 * 但是在实际情况下发现在/srv/main/java包下通过main方法的方式会缺少依赖，为什么呢？
//	 * 因为spring-boot-starter-test的parent的parent是spring-boot-parent，spring-boot-parent的pom文件中对JUnit相关的依赖
//	 * 加了<scope>test</scope>，所以/srv/main/java包下通过main方法的方式运行时会缺少依赖
//	 * <p>
//	 * 解决方式：
//	 * 方式一：在/srv/test/java中写demo；
//	 * 方式二：单独手工引入缺少的依赖。该方式运行时还是报sun.reflect.annotation.TypeNotPresentExceptionProxy错误，具体原因未知。
//	 */
//	public static void main(String[] args) {
//		ClassSelector classSelector = DiscoverySelectors.selectClass(TestMain01.class);
//
//		LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder
//				.request()
//				.selectors(classSelector)
//				.build();
//
//		Launcher launcher = LauncherFactory.create();
//		launcher.execute(request);
//
//		System.out.println("hello world!");
//
//
//	}
//
//}
