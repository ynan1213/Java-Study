package com.ynan.feign;

import com.ynan.config.XxxCustomConfiguration;
import com.ynan.config.YyyCustomConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @Author yuannan
 * @Date 2021/11/22 19:31
 */
/*@FeignClient(
	value = "remote-demo"
	*//*,path = "/remote"*//*
	*//*,contextId = "bbb"*//*
	,configuration = YyyCustomConfiguration.class
)*/
public interface RemoteServiceXxx {

	@RequestMapping("/s")
	String remote(@RequestParam("name") String name);
}
