package com.myspringmvc.myspringcore.annotation;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface MyRequerstMapping {
    String value() default "";
}
