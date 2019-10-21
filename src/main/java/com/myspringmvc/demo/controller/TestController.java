package com.myspringmvc.demo.controller;

import com.myspringmvc.demo.service.TestService;
import com.myspringmvc.myspringcore.annotation.MyAutowired;
import com.myspringmvc.myspringcore.annotation.MyController;
import com.myspringmvc.myspringcore.annotation.MyRequerstMapping;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@MyController
public class TestController {
    @MyAutowired
    public TestService testService;

    @MyRequerstMapping("/test")
    public void test(HttpServletRequest httpServletRequest, HttpServletResponse response) throws IOException {
        Cookie cookie = new Cookie("test", testService.test().replaceAll("[,|\\s]", "#"));
        response.addCookie(cookie);
        response.addHeader("test", testService.test());
        PrintWriter p = response.getWriter();
        p.write( testService.test());
    }
}
