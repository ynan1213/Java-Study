package com.ynan._03.annotatedElementUtils.autoproxy;

import org.springframework.stereotype.Component;

@Component
public class ServiceImpl implements Service
{

	@Override
	public String select(String str) throws Exception
	{
		System.out.println("执行目标方法 ,参数值为：");
		throw new FileNotException();
	}

}
