package com.myspringmvc.demo.service.impl;

import com.myspringmvc.demo.service.TestService;
import com.myspringmvc.myspringcore.annotation.MyService;

@MyService
public class TestServiceImpl implements TestService {
    public String test() {
        return "Hello, I am a new mvc";
    }
}
