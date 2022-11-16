# http-request-restrict-spring-boot-starter
[![Maven Central](https://img.shields.io/maven-central/v/io.github.weasley-j/http-request-restrict-spring-boot-starter)](https://search.maven.org/artifact/io.github.weasley-j/http-request-restrict-spring-boot-starter)

一个用于支持`springboot`项目`http`请求限制的`starter`，通过识别客户端传来`token`值(具有唯一标志性)来实现：同一个用户在一定的时间频次内最多只能点击`N`次特定接口的功能



## 1 映入pom坐标

版本号在`maven`仓库获取

```xml
<dependency>
  <groupId>io.github.weasley-j</groupId>
  <artifactId>http-request-restrict-spring-boot-starter</artifactId>
  <version>${latest.version}</version>
</dependency>
```



## 2 yaml里配置http-request-restrict元数据

**这里以`Sa-Token`作为`token`逻辑演示**

[Github示例链接](https://github.com/Weasley-J/http-request-restrict-spring-parent/blob/main/http-request-restrict-spring-boot-tests/src/main/resources/application-demo.yml)

```yaml
spring:

  redis:
    host: 127.0.0.1
    port: 6379
    password: 123456
    database: 0

  #  API restrict Config（组件主体配置）
  request:
    restrict:
      enable: on
      header-keys:
        - x-auth-token
      redis:
        enable-ssl: off
        host: ${spring.redis.host}
        port: ${spring.redis.port}
        password: ${spring.redis.password}
        database: ${spring.redis.database}

# Sa Token
sa-token:
  enable: true
  # token名称 (同时也是cookie名称)
  token-name: x-auth-token
  # token有效期，单位s 默认30天, -1代表永不过期
  timeout: 2592000
  # token临时有效期 (指定时间内无操作就视为token过期) 单位: 秒
  activity-timeout: -1
  # 是否允许同一账号并发登录 (为true时允许一起登录, 为false时新登录挤掉旧登录)
  is-concurrent: true
  # 在多人登录同一账号时，是否共用一个token (为true时所有登录共用一个token, 为false时每次登录新建一个token)
  is-share: true
  # token风格
  token-style: simple-uuid
  # 是否输出操作日志
  is-log: false
  plugins:
    redis:
      show-banner: true
      redis-base-prefix: 'uc:'
      independent-session: true
      independent-redis:
        host: ${spring.redis.host}
        port: ${spring.redis.port}
        password: ${spring.redis.password}
        database: ${spring.redis.database}
```



**补充：**`Sa-Token`的`pom`坐标（版本号在`maven`仓库获取）

```xml
<!-- sa-token  -->
<dependency>
    <groupId>cn.dev33</groupId>
    <artifactId>sa-token-spring-boot-starter</artifactId>
    <version>${sa-token.version}</version>
</dependency>
<!-- Sa-Token 整合 Redis （使用 jackson 序列化方式） -->
<dependency>
    <groupId>cn.dev33</groupId>
    <artifactId>sa-token-dao-redis-jackson</artifactId>
    <version>${sa-token.version}</version>
</dependency>
<!-- sa-plugin-redis -->
<dependency>
    <groupId>io.github.weasley-j</groupId>
    <artifactId>sa-plugin-redis-spring-boot-starter</artifactId>
    <version>1.0.2</version>
</dependency>
<!-- redis启动器 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```



## 3 通过注解装配bean开启功能

```java
import io.github.weasleyj.request.restrict.annotation.EnableApiRestrict;
import io.github.weasleyj.satoken.session.annotation.EnableSaIndependentRedisSession;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * DEMO APP
 */
@SpringBootApplication
@EnableApiRestrict //主体注解
@EnableSaIndependentRedisSession
public class HttpRequestRestrictDemoApp {

    public static void main(String[] args) {
        SpringApplication.run(HttpRequestRestrictDemoApp.class, args);
    }

}
```



## 4 启动应用访问接口观察异常

- 访问登录接口获取`token`

[源码](https://github.com/Weasley-J/http-request-restrict-spring-parent/blob/main/http-request-restrict-spring-boot-tests/src/main/java/com/example/request/controller/AuthenticationController.java)

http://localhost:8080/api/public/auth/login/token

![image-20221116132427745](https://alphahub-test-bucket.oss-cn-shanghai.aliyuncs.com/image/image-20221116132427745.png)



接口返回：

```json
{
  "tokenName": "x-auth-token",
  "tokenValue": "d0d4821c21404465895199be8203222c",
  "isLogin": true,
  "loginId": "10086"
}
```

- 使用`API`工具调用限流接口

[源码](https://github.com/Weasley-J/http-request-restrict-spring-parent/blob/main/http-request-restrict-spring-boot-tests/src/main/java/com/example/request/controller/ApiRestrictDemoController.java)

10秒内仅能点击2次: http://localhost:8080/api/public/demo/click2Times10Seconds

![image-20221116132857439](https://alphahub-test-bucket.oss-cn-shanghai.aliyuncs.com/image/image-20221116132857439.png)

**触发限流抛出异常方便开发者进行捕获处理，给前端发返回提示。**

```java
io.github.weasleyj.request.restrict.exception.FrequentRequestException: 操作太过频繁，请稍后再试（接口URI: /api/public/demo/click2Times10Seconds, 10(SECONDS)内仅能请求2次）
	at io.github.weasleyj.request.restrict.interceptor.DefaultRequestRestrictInterceptor.preHandle(DefaultRequestRestrictInterceptor.java:106) ~[classes/:na]
	at org.springframework.web.servlet.HandlerExecutionChain.applyPreHandle(HandlerExecutionChain.java:148) ~[spring-webmvc-5.3.23.jar:5.3.23]
	at org.springframework.web.servlet.DispatcherServlet.doDispatch(DispatcherServlet.java:1066) ~[spring-webmvc-5.3.23.jar:5.3.23]
	at org.springframework.web.servlet.DispatcherServlet.doService(DispatcherServlet.java:964) ~[spring-webmvc-5.3.23.jar:5.3.23]
	at org.springframework.web.servlet.FrameworkServlet.processRequest(FrameworkServlet.java:1006) ~[spring-webmvc-5.3.23.jar:5.3.23]
	at org.springframework.web.servlet.FrameworkServlet.doPost(FrameworkServlet.java:909) ~[spring-webmvc-5.3.23.jar:5.3.23]
```

