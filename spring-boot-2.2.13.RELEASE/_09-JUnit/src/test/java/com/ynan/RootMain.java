package com.ynan;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 如果/src/test包也有启动类，且@SpringBootTest注解没有指定classes属性，会报错，
 * 因为会找到两个带有@SpringBootApplication，如果启动类名完全一致，则不会
 */
@SpringBootApplication(scanBasePackages = "com.ynan.service")
public class RootMain {


}
