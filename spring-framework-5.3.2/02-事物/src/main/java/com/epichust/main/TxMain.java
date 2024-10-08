package com.epichust.main;

import com.epichust.config.AppConfig;
import com.epichust.entity.Book;
import com.epichust.service.BookService;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;


public class TxMain {

	public static void main(String[] args)
	{
		System.getProperties().setProperty("aaaa", "spring-application");

		/* 基于事物工厂的配置方式 */
		//ClassPathXmlApplicationContext ac = new ClassPathXmlApplicationContext("classpath:spring-application-1.xml");

		/* 基于tx标签的配置方式 */
		//ClassPathXmlApplicationContext ac = new ClassPathXmlApplicationContext("classpath:${aaaa}-2.xml") ;

		/* 基于@Aspect注解的方式 */
		//ClassPathXmlApplicationContext ac = new ClassPathXmlApplicationContext("classpath:spring-application-3.xml");

		/* 基于@Aspect注解的方式 */
		AnnotationConfigApplicationContext ac = new AnnotationConfigApplicationContext(AppConfig.class);
		BookService bs = (BookService) ac.getBean("bookServiceImpl");

		Book book = new Book("1111", 222222);

		try {
			System.out.println("--------------- 开始 ---------------");
			bs.update();
			System.out.println("--------------- 结束 ---------------");
//			bs.insert(book);
		} catch (Exception e) {
			System.out.println("主方法抛出异常:" + e.getMessage());
		}

		ac.close();
	}
}
