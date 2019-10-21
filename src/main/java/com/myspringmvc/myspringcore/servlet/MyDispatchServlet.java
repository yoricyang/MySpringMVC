package com.myspringmvc.myspringcore.servlet;

import com.myspringmvc.myspringcore.annotation.MyAutowired;
import com.myspringmvc.myspringcore.annotation.MyController;
import com.myspringmvc.myspringcore.annotation.MyRequerstMapping;
import com.myspringmvc.myspringcore.annotation.MyService;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

public class MyDispatchServlet extends HttpServlet {

    //配置文件
    private Properties configProperties = new Properties();
    //类名
    private List<String> classList = new ArrayList<String>();

    //Ioc
    private Map<String, Object> iocMap = new HashMap<String, Object>();
    private Map<String, Method> handleMapping = new HashMap<String, Method>();


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        dispatch(req,resp);
    }

    /**
     * 处理请求拦截，分发
     * @param req
     * @param resp
     */
    private void dispatch(HttpServletRequest req, HttpServletResponse resp){
        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        url = url.replaceAll(contextPath, "").replaceAll("/+", "/");
        System.out.println("[INFO-7]请求的url为:" + url);

        if(!handleMapping.containsKey(url)){
            try {
                resp.getWriter().write("404 not Found");
                return ;
            } catch (IOException e) {
                System.out.println("[ERROR-7]write response error!");
                e.printStackTrace();
            }
        }

        Method method = handleMapping.get(url);
        //获取该method对应的className
        String clazzName = lowerFirstCase(method.getDeclaringClass().getSimpleName());
        System.out.println("[INFO-7]IocMap-->" + clazzName);
        //iocMapping中找到该clazz的instance
        Object instance = iocMap.get(clazzName);
        //反射调用方法，包括req和resp（这里说明springMVC的controller函数里都统一有req和resp）
        try {
            method.invoke(instance, req, resp);
            System.out.println("[INFO-7]method invoke:" + method.getName());
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }


    }

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        //1. 读取配置文件 ServletConfig
        doLoadProperties(servletConfig.getInitParameter("mvcConfigLocation"));
        //2. 扫描类名
        doScanClass(configProperties.getProperty("scan-package"));
        //3. init IOC
        doInitIoc();
        //4. init AutoWired
        doAutowired();
        //5. handleMapping
        doHandleMapping();
        //6. printData Sucess
        doPrint();

        super.init();
    }


    /**
     * 1.load the properties
     * @param configPath web.xml --> servlet/init-param
     */
    public void doLoadProperties(String configPath){
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(configPath);
        //保存到内存中
        try{
            configProperties.load(inputStream);

            System.out.println("[INFO-1]:配置文件加载成功！");
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if(inputStream != null){
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 2.do scan class
     */
    public void doScanClass(String packagePath){
        //package .==>/
        URL sourcePath = this.getClass().getClassLoader().getResource("/" + packagePath.replaceAll("\\.","/"));
        if(sourcePath == null){
            return;
        }
        File filePath = new File(sourcePath.getFile());
        for(File file : filePath.listFiles()) {
            if (file.isDirectory()) {
                System.out.println("[INFO-2]:扫描到当前目录:" + file.getName());
                doScanClass(packagePath + "." + file.getName());
            }else{
                if(!file.getName().endsWith(".class")){
                    System.out.println("[INFO-2]:扫描到不是class的文件:" + file.getName());
                    continue;
                }

                String className = packagePath + "." + file.getName();
                System.out.println("[INFO-2]:即将加载的class文件:" + className);
                this.classList.add(className.replace(".class", ""));
            }
        }

    }

    private String lowerFirstCase(String name){
        char[] chars = name.toCharArray();
        if(chars[0] > 64 && chars[0] < 91){
            chars[0]+= 32;
        }
        return String.copyValueOf(chars);
    }

    /**
     * 3. init Ioc
     */
    public void doInitIoc(){
        if(classList.isEmpty()){
            return;
        }

        for(String className : classList){
            Class clazz;
            try {
                clazz = Class.forName(className);
                if(clazz.isAnnotationPresent(MyController.class)){
                    String beanName = lowerFirstCase(clazz.getSimpleName());
                    Object instance = clazz.newInstance();
                    iocMap.put(beanName, instance);
                    System.out.println("[INFO-3]已加载的Controller类:" + beanName);
                }else if(clazz.isAnnotationPresent(MyService.class)){
                    String beanName = lowerFirstCase(clazz.getSimpleName());
                    MyService myServiceAnno = (MyService) clazz.getAnnotation(MyService.class);
                    //指定Service注解值
                    if(!"".equals(myServiceAnno.value())){
                        beanName = myServiceAnno.value();
                    }
                    Object instance = clazz.newInstance();
                    iocMap.put(beanName,instance);
                    System.out.println("[INFO-3]已加载的Service类:" + beanName);

                    for(Class i : clazz.getInterfaces()){
                        if(iocMap.containsKey(i.getSimpleName())){
                            throw new Exception("[ERROR-3]暂不支持多个Service类实现同一个接口，存在冲突的Bean名称:" + i.getName());
                        }
                        iocMap.put(lowerFirstCase(i.getSimpleName()), instance);
                        System.out.println("[INFO-3]已加载的Service接口类:" + clazz.getSimpleName() + ":" + lowerFirstCase(i.getSimpleName()));
                    }
                }


            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }catch (IllegalAccessException ex){
                ex.printStackTrace();
            }catch (InstantiationException ex){
                ex.fillInStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    /**
     * 4. do autoWired 处理依赖注入
     */
    public void doAutowired(){
        if(iocMap.isEmpty()){
            return;
        }
        String beanName;

        for(Map.Entry entry : iocMap.entrySet()){
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            for(Field field : fields){
                if(!field.isAnnotationPresent(MyAutowired.class)){
                    continue;
                }
                //获取注解对象
                MyAutowired myAutowired = field.getAnnotation(MyAutowired.class);
                //注解中没有指定注入对象，则依据接口来注入
                if("".equals(myAutowired.value())){
                    beanName = lowerFirstCase(field.getType().getSimpleName());
                }else {
                    beanName = lowerFirstCase(myAutowired.value().trim());
                }
                //只要注解了private也需要注入
                field.setAccessible(true);

                try {
                    field.set(entry.getValue(),iocMap.get(beanName));
                    System.out.println("[INFO-4]已做依赖注入的实例:" + beanName + ",变量名:" + field.getName());
                } catch (IllegalAccessException e) {
                    System.out.println("[ERROR-4]依赖注入失败变量:" + field.getName());
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 5. init handlerMapping 初始化handleMapping
     */
    public void doHandleMapping(){
        if(iocMap.isEmpty()){
            return;
        }

        for(Map.Entry entry : iocMap.entrySet()){
            Class clazz = entry.getValue().getClass();
            if(!clazz.isAnnotationPresent(MyController.class)){
                return;
            }
            //处理类上面的requestMapping注解
            String baseUrl = "";
            if(clazz.isAnnotationPresent(MyRequerstMapping.class)){
                MyRequerstMapping myRequerstMapping = (MyRequerstMapping) clazz.getAnnotation(MyRequerstMapping.class);
                baseUrl = myRequerstMapping.value();
            }
            //处理方法上面的requestMapping注解
            for(Method method : clazz.getMethods()){
                if(!method.isAnnotationPresent(MyRequerstMapping.class)){
                    return;
                }
                MyRequerstMapping methodRequestMapping = method.getAnnotation(MyRequerstMapping.class);
                String url = ("/" + baseUrl + "/" + methodRequestMapping.value()).replaceAll("/+", "/");
                handleMapping.put(url, method);
                System.out.println("[INFO-5]已加载HandleMapping方法：{" + url + "}:" + method);

            }
        }
    }

    /**
     * 6. print data
     */
    public void doPrint(){
        System.out.println("[INFO-6]---------------data------------------");
        System.out.println("configPropertiesName:" + configProperties.propertyNames());
        System.out.println("[classNameList]-->");
        for(String str : classList){
            System.out.println(str);
        }
        System.out.println("[iocMap]-->");
        for(Map.Entry entry : iocMap.entrySet()){
            System.out.println(entry);
        }
        System.out.println("[handleMapping]-->");
        for(Map.Entry entry : iocMap.entrySet()){
            System.out.println(entry);
        }
        System.out.println("[INFO-6]---------------done------------------");
        System.out.println("======启动成功======");



    }
}
