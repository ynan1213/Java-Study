package com.ynan;

import org.junit.*;
import org.junit.runner.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.Repeat;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = RootConfig.class)
//@ContextHierarchy({
//		@ContextConfiguration(classes = ParentConfig.class),
//		@ContextConfiguration(classes = RootConfig.class)
//})
//@IfProfileValue(name = "xxx", value = "xxx")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class JunitTestMain {

	@Autowired
	private PrintService printService;

	@Test
	@DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
	public void test01() {
		System.out.println(" ------- test01 -------");
		printService.print();
	}

	@Test
	@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
	public void test02() {
		System.out.println(" ------- test02 -------");
		printService.print();
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

	public static void main(String[] args) {
		Request request = Request.aClass(JunitTestMain.class);
		Runner runner = request.getRunner();

		JUnitCore jUnitCore = new JUnitCore();
		Result result = jUnitCore.run(runner);
		System.out.println(" ---------------- end ----------------- ");
	}

}
