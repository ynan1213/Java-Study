package com.sqsoft.controller;

import javax.servlet.http.HttpServletRequest;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.sqsoft.entity.User;

@Controller
public class UserController {
	
	@GetMapping("/login")
	public String toLoginPage(HttpServletRequest r) {
		Subject subject = SecurityUtils.getSubject();
		Session session = subject.getSession();
		session.setAttribute("user", new User());
		return "login";
	}
	
	@RequestMapping("/user")
	@RequiresPermissions("add")
	public String user() {
		return "success";
	}
	
	@GetMapping("/errer")
	public String toErrerPage() {
		return "errer";
	}
	
	@PostMapping("/login")
	public String login(String username,String password) {
		UsernamePasswordToken token = new UsernamePasswordToken(username,password,true);
		Subject subject = SecurityUtils.getSubject();
		try {
			subject.login(token);
			Session session = subject.getSession();
			session.setAttribute("user", subject.getPrincipal());
			return "success";
		}catch(UnknownAccountException e) {
			throw e;
		}catch(IncorrectCredentialsException e) {
			throw new IncorrectCredentialsException("密码不正确");
		}
	}
	
}
