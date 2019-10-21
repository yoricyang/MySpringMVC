# MySpringMVC
## 整体思路
1）配置阶段
配置 web.xml：
```
 <sevlet>
 XDispatchServlet
 设定 init-param： contextConfigLocation = applicationContext.properties
 <servlet-mapping>
 设定 url-pattern： /*
 配置 Annotation： @XController @XService @XAutowired @XRequestMapping
```

2）初始化阶段
IOC：
调用 init() 方法： 加载配置文件
IOC 容器初始化： Map<String, Object>
扫描相关的类： scan-package=“com.xiaopengwei”
创建实例化并保存到容器： 同过反射机制将类实例化放入 IOC 容器中
DI：
进行 DI 操作： 扫描 IOC 容器中的实例，给没有赋值的属性自动赋值
MVC：
初始化 HandlerMapping： 将一个 URL 和一个 Method 进行一对一的关联映射 Map<String, Method>

3）运行阶段
调用 doGet() / doPost() 方法： Web 容器调用 doGet() / doPost() 方法，获得 request/response 对象
匹配 HandleMapping： 从 request 对象中获得用户输入的 url，找到其对应的 Method
反射调用 method.invoker()： 利用反射调用方法并返回结果
response.getWrite().write()： 将返回结果输出到浏览器

#### [来源自Spring 5核心原理与30个类手写实战](https://zhuanlan.zhihu.com/p/81395470)
