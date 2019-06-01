# Java秒杀系统方案优化

标签（空格分隔）： 未分类

---
## 1、课程导学
### 1-1 课程介绍
#### 1、技术点介绍
前端
* Thymeleaf
* Bootstrap
* JQuery
后端
* SpringBoot
* JSR303
* Mybatis
中间件
* RabbitMQ
* Redis
* Druid

#### 2、秒杀模块
* 分布式回话
* 商品列表页
* 商品详情页
* 订单详情页
* 系统压测
* 缓存优化
* 消息队列
* 接口安全

#### 3、学习高并发方案
缓存、异步、分布式
### 1-2 章节介绍

## 2、项目框架搭建
###2-1 Spring Boot环境搭建
查看官网搭建Spring Boot环境
Quick start:https://projects.spring.io/spring-boot/
Reference:https://docs.spring.io/spring-boot/docs/1.5.8.RELEASE/reference/htmlsingle
注意：引入1.5.8版本的依赖时出现json格式转换错误，把版本提高为2.0.5后可解决
###2-2 集成Thymeleaf，Result结果封装
1、看文档，在pom.xml添加相关依赖和在application.properties中配置thymeleaf
```
     <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-thymeleaf</artifactId>
    </dependency>

```
在resourses目录下添加templates目录
```
spring.thymeleaf.prefix=classpath:/templates/
spring.thymeleaf.suffix=.
spring.thymeleaf.cache=false
spring.thymeleaf.content-type=text/html
spring.thymeleaf.enabled=true
spring.thymeleaf.encoding=UTF-8
spring.thymeleaf.mode=HTML5
```
2、编写Result<T>、CodeMsg
```
public class Result<T> {

    private int code;
    private String msg;
    private T data;

    //成功时调用
    public static <T> Result<T> success(T data){
        return new Result<T>(data);
    }

    //失败时调用
    public static <T> Result<T> error(CodeMsg codeMsg){
        return new Result<T>(codeMsg);
    }

    private Result(T data) {
        this.code = 0;
        this.msg = "success";
        this.data = data;
    }

    private Result(CodeMsg codeMsg){
        if(codeMsg == null){
            return;
        }else {
            this.code = codeMsg.getCode();
            this.msg = codeMsg.getMsg();
        }
    }
```
```
public class CodeMsg {

    private int code;
    private String msg;

    //通用错误码
    public static CodeMsg SUCCESS = new CodeMsg(0,"success");
    public static CodeMsg SERVER_ERROR = new CodeMsg(500100, "服务端异常");
    public static CodeMsg BIND_ERROR = new CodeMsg(500101, "参数校验异常：%s");
    public static CodeMsg REQUEST_ILLEGAL = new CodeMsg(500102, "请求非法");
    public static CodeMsg ACCESS_LIMIT_REACHED= new CodeMsg(500104, "访问太频繁！");

    //登录模块 5002XX
    public static CodeMsg SESSION_ERROR = new CodeMsg(500210, "Session不存在或者已经失效");
    public static CodeMsg PASSWORD_EMPTY = new CodeMsg(500211, "登录密码不能为空");
    public static CodeMsg MOBILE_EMPTY = new CodeMsg(500212, "手机号不能为空");
    public static CodeMsg MOBILE_ERROR = new CodeMsg(500213, "手机号格式错误");
    public static CodeMsg MOBILE_NOT_EXIST = new CodeMsg(500214, "手机号不存在");
    public static CodeMsg PASSWORD_ERROR = new CodeMsg(500215, "密码错误");

    //商品模块 5003XX

    //订单模块 5004XX
    public static CodeMsg ORDER_NOT_EXIST = new CodeMsg(500400, "订单不存在");

    //秒杀模块 5005XX
    public static CodeMsg MIAO_SHA_OVER = new CodeMsg(500500, "商品已经秒杀完毕");
    public static CodeMsg REPEATE_MIAOSHA = new CodeMsg(500501, "不能重复秒杀");
    public static CodeMsg MIAOSHA_FAIL = new CodeMsg(500502, "秒杀失败");

    public CodeMsg() {
    }

    public CodeMsg(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

```
###2-3 集成Mybatis+Druid
1、添加pom依赖：mybatis-spring-boot-starter
```
    <dependency>
      <groupId>org.mybatis.spring.boot</groupId>
      <artifactId>mybatis-spring-boot-starter</artifactId>
      <version>1.3.1</version>
    </dependency>
```
2、添加配置：mybatis.*
```
mybatis.type-aliases-package=com.czj.miaoshaV2.domain
mybatis.configuration.map-underscore-to-camel-case=true
mybatis.configuration.default-fetch-size=100
mybatis.configuration.default-statement-timeout=3000
mybatis.mapperLocations = classpath:com/czj/miaoshaV2/dao/*.xml
```
3、添加mysql、druid依赖及数据源的相关配置
```
    <dependency>
      <groupId>mysql</groupId>
      <artifactId>mysql-connector-java</artifactId>
    </dependency>

    <dependency>
      <groupId>com.alibaba</groupId>
      <artifactId>druid</artifactId>
      <version>1.0.5</version>
    </dependency>
```
```
spring.datasource.url=jdbc:mysql://localhost:3306/miaoshaV2?useUnicode=true&characterEncoding=utf-8&allowMultiQueries=true&useSSL=false
spring.datasource.username=root
spring.datasource.password=root
spring.datasource.driver-class-name=com.mysql.jdbc.Driver
spring.datasource.type=com.alibaba.druid.pool.DruidDataSource
spring.datasource.filters=stat
spring.datasource.maxActive=1000
spring.datasource.initialSize=100
spring.datasource.maxWait=60000
spring.datasource.minIdle=500
spring.datasource.timeBetweenEvictionRunsMillis=60000
spring.datasource.minEvictableIdleTimeMillis=300000
spring.datasource.validationQuery=select 'x'
spring.datasource.testWhileIdle=true
spring.datasource.testOnBorrow=false
spring.datasource.testOnReturn=false
spring.datasource.poolPreparedStatements=true
spring.datasource.maxOpenPreparedStatements=20
```
4、测试事务
加入@Transactional事务起作用
###2-4 集成Jedis+Redis安装+通用缓存Key封装
1、在linux中安装Redis
2、添加Jedis依赖、添加Fastjson依赖（不是最快但序列化后明文可读）
注：springboot集成redis可以引入spring-boot-starter-redis包，然后配置application.properties即可使用，下文不采用这种方式而是利用原生jedis自定义编写
```
    <dependency>
      <groupId>redis.clients</groupId>
      <artifactId>jedis</artifactId>
    </dependency>

    <dependency>
      <groupId>com.alibaba</groupId>
      <artifactId>fastjson</artifactId>
      <version>1.2.38</version>
    </dependency>
```
3、redis配置

* application.properties：定义相关参数
```
redis.host=127.0.0.1
redis.port=6379
redis.timeout=10
#redis.password=root
redis.poolMaxTotal=1000
redis.poolMaxIdle=500
redis.poolMaxWait=500
```
* RedisConfig.java:封装application.properties中配置的相关参数，并注入Bean
```
@Component
@ConfigurationProperties(prefix = "redis")
public class RedisConfig {

    private String host;
    private int port;
    private int timeout;//秒
    private String password;
    private int poolMaxTotal;
    private int poolMaxIdle;
    private int poolMaxWait;//秒
```
* RedisPoolFactory.java：根据RedisConfig获取JedisPool，并注入Bean
```
@Service
public class RedisPoolFactory {

    @Autowired
    RedisConfig redisConfig;

    @Bean
    public JedisPool JedisPoolFactory(){
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxIdle(redisConfig.getPoolMaxIdle());
        poolConfig.setMaxTotal(redisConfig.getPoolMaxTotal());
        poolConfig.setMaxWaitMillis(redisConfig.getPoolMaxWait() * 1000);
        JedisPool jp = new JedisPool(poolConfig, redisConfig.getHost(), redisConfig.getPort(),
                redisConfig.getTimeout()*1000, redisConfig.getPassword(), 0);
        return jp;
    }
}
```
* RedisService.java:获取可操作的Jedis对象，封装对Redis操作的相关方法
```
@Service
public class RedisService {

    @Autowired
    JedisPool jedisPool;

    /**
     * 获取当个对象
     * @param prefix
     * @param key
     * @param clazz
     * @param <T>
     * @return
     */
    public <T> T get(KeyPrefix prefix,String key,Class<T> clazz){
        Jedis jedis = null;
        try{
            jedis = jedisPool.getResource();
            //生成真正的Key
            String realKey = prefix.getPrefix() + key;
            String str = jedis.get(realKey);
            T t = stringToBean(str,clazz);
            return t;
        }finally {
            returnToPool(jedis);
        }
    }

    /**
     * 设置对象
     * @param prefix
     * @param key
     * @param value
     * @param <T>
     * @return
     */
    public <T> boolean set(KeyPrefix prefix,String key,T value){
        Jedis jedis = null;
        try{
            jedis = jedisPool.getResource();
            String str = beanToString(value);
            if(str == null || str.length() <= 0){
                return false;
            }
            //生成真正的Key
            String realKey = prefix.getPrefix() + key;
            int seconds = prefix.expireSeconds();
            if(seconds <= 0){
                jedis.set(realKey,str);
            }else{
                jedis.setex(realKey,seconds,str);
            }
            return true;
        }finally{
            returnToPool(jedis);
        }
    }

    /**
     * 判断Key是否存在
     * @param prefix
     * @param key
     * @param <T>
     * @return
     */
   public <T> boolean exists(KeyPrefix prefix,String key){
        Jedis jedis = null;
        try{
            jedis = jedisPool.getResource();
            //生成真正的Key
            String realKey = prefix.getPrefix() + key;
            return jedis.exists(realKey);
        }finally {
            returnToPool(jedis);
        }
   }

    /**
     * 删除
     * @param prefix
     * @param key
     * @return
     */
   public boolean delete(KeyPrefix prefix,String key){
       Jedis jedis = null;
       try{
           jedis = jedisPool.getResource();
           String realKey = prefix.getPrefix() + key;
           long ret = jedis.del(key);
           return ret > 0;
       }finally {
           returnToPool(jedis);
       }
   }

    /**
     * 增加值
     * @param prefix
     * @param key
     * @param <T>
     * @return
     */
   public <T> Long incr(KeyPrefix prefix,String key){
       Jedis jedis = null;
       try{
           jedis = jedisPool.getResource();
           String realKey = prefix.getPrefix() + key;
           return jedis.incr(realKey);
       }finally {
           returnToPool(jedis);
       }
   }

    /**
     * 减少值
     * @param prefix
     * @param key
     * @param <T>
     * @return
     */
   public <T> Long decr(KeyPrefix prefix,String key){
       Jedis jedis = null;
       try{
           jedis = jedisPool.getResource();
           String realKey = prefix.getPrefix() + key;
           return jedis.decr(realKey);
       }finally {
           returnToPool(jedis);
       }
   }


    private <T> String beanToString(T value) {
        if (value == null) {
            return null;
        }
        Class<?> clazz = value.getClass();
        if(clazz == int.class || clazz == Integer.class){
            return "" + value;
        }else if(clazz == String.class){
            return (String) value;
        }else if(clazz == long.class || clazz == Long.class){
            return "" + value;
        }else {
            return JSON.toJSONString(value);
        }
    }

    /**
     * 把连接返回到连接池
     * @param jedis
     */
    private void returnToPool(Jedis jedis) {
        if(jedis != null){
            jedis.close();
        }
    }

    private <T> T stringToBean(String str, Class<T> clazz) {
        if(str == null || str.length() <= 0 || clazz ==null){
            return null;
        }
        if(clazz == int.class || clazz == Integer.class){
            return (T) Integer.valueOf(str);
        }else if(clazz == String.class) {
            return (T)str;
        }else if(clazz == long.class || clazz == Long.class) {
            return  (T)Long.valueOf(str);
        }else {
            return JSON.toJavaObject(JSON.parseObject(str),clazz);
        }
    }
}
```
4、KeyPrefix、BasePrefix、UserKey/OrderKey编写(优雅的编写方式)
![此处输入图片的描述][1]
```
public interface KeyPrefix {

    public int expireSeconds();

    public String getPrefix();
}
```
```
public class BasePrefix  implements KeyPrefix{

    private int expireSeconds;      //0代表永不过期

    private String prefix;

    public BasePrefix(int expireSeconds, String prefix) {
        this.expireSeconds = expireSeconds;
        this.prefix = prefix;
    }

    public BasePrefix(String prefix) {
        this(0,prefix);
    }

    @Override
    public int expireSeconds() {
        return expireSeconds;
    }

    @Override
    public String getPrefix() {
        String className = getClass().getSimpleName();
        return className + ":" + prefix;
    }
}
```
```
public class UserKey extends BasePrefix{

    public UserKey(String prefix) {
        super(prefix);
    }

    public static UserKey getById = new UserKey("id");
    public static UserKey getByName = new UserKey("name");

}

```

## 3、实现用户登录以及分布式session功能
###3-1 数据库设计
秒杀用户表
```
CREATE TABLE `miaosha_user`(
`id` bigint(20) NOT NULL COMMENT '用户ID，手机号码',
`nickname` varchar(255) NOT NULL,
`password` varchar(32) DEFAULT NULL COMMENT 'MD5(MD5(pass明文+固定salt)+salt)',
`salt` varchar(10) DEFAULT NULL,
`head` varchar(128) DEFAULT NULL COMMENT '头像，云储存的ID',
`register_date` datetime DEFAULT NULL COMMENT '注册时间',
`last_login_date` datetime DEFAULT NULL COMMENT '上次登录时间',
`login_count` int(11) DEFAULT '0' COMMENT '登录次数',
PRIMARY KEY(`id`)
)ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
```
###3-2 明文密码两次MD5处理
用户端：PASS=MD5（明文+固定Salt）
服务端：PASS=MD5（用户输入+随机Salt）
1、引入MD5工具类：commons-codec、commons-lang3
```
    <dependency>
      <groupId>commons-codec</groupId>
      <artifactId>commons-codec</artifactId>
    </dependency>
    <dependency>
      <groupId>org.apache.commons</groupId>
      <artifactId>commons-lang3</artifactId>
      <version>3.6</version>
    </dependency>
```
2、编写MD5Util
```
import org.apache.commons.codec.digest.DigestUtils;

public class MD5Util {

    public static String md5(String string){
        return DigestUtils.md5Hex(string);
    }

    private static final String salt = "abc123";

    //把输入框中的数据加密
    public static String inputPassToFormPass(String input){
        String str = "" + salt.charAt(0) + salt.charAt(2) + input +salt.charAt(5) + salt.charAt(3);
        return md5(str);
    }

    //把后台获取到的表单数据再一次加密
    public static String formPassToDBPass(String formPass,String salt){
        String str = "" + salt.charAt(0) + salt.charAt(2) + formPass +salt.charAt(5) + salt.charAt(3);
        return md5(str);
    }

    //把输入框中的数据两次加密
    public static String inputPassToDBPass(String input,String saltDB){
        String formPass = inputPassToFormPass(input);
        String dbPass = formPassToDBPass(formPass,saltDB);
        return dbPass;
    }

    public static void main(String[] args) {
        String formPass = inputPassToFormPass("123456");
        String dbPass = formPassToDBPass(formPass,"123456");
        String dbPassV2 = inputPassToDBPass("123456","123456");
        System.out.println(formPass);
        System.out.println(dbPass);
        System.out.println(dbPassV2);
    }

}
```
3、登录功能实现

###3-3 JSR303参数验证
1、引入依赖
```
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
```
2、参照已有校验器自定义其他校验器
在Controller中需要校验的对象前添加@Valid，并在对象所属的类属性添加@NotNull等校验器
但是依赖并没有为我们提供是否是正确手机号的校验器，需要我们自定义
```
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(
        validatedBy = {IsMobileValidator.class}
)
public @interface IsMobile {
    boolean required() default true;

    String message() default "手机号格式错误";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

```
```
public class IsMobileValidator implements javax.validation.ConstraintValidator<IsMobile,String>{

    private boolean required;

    @Override
    public void initialize(IsMobile isMobile) {
        required = isMobile.required();
    }

    @Override
    public boolean isValid(String s, ConstraintValidatorContext constraintValidatorContext) {
        if(required){
            return ValidatorUtil.isMobile(s);
        }else {
            if(StringUtils.isEmpty(s)){
                return true;
            }else {
                return ValidatorUtil.isMobile(s);
            }
        }

    }
}
```
```
public class ValidatorUtil {

    private static final Pattern mobile_pattern = Pattern.compile("1\\d{10}");

    public static boolean isMobile(String src){
        if(StringUtils.isEmpty(src)){
            return false;
        }else {
            Matcher matcher = mobile_pattern.matcher(src);
            return matcher.matches();
        }
    }
}
```
###3-4 全局异常处理器
自定义异常
```
public class GlobalException extends RuntimeException{

    private CodeMsg codeMsg;

    public GlobalException(CodeMsg codeMsg){
        super(codeMsg.toString());
        this.codeMsg = codeMsg;
    }

    public CodeMsg getCodeMsg() {
        return codeMsg;
    }
}
```
全局异常处理类（处理绑定异常、自定义异常、其他异常）
```
@ControllerAdvice
@ResponseBody
public class GlobalExceptionHandler {

    @ExceptionHandler(value = Exception.class)
    public Result exceptionHandler(HttpServletRequest request,Exception e){
        if(e instanceof GlobalException){
            GlobalException globalException = (GlobalException)e;
            return Result.error(globalException.getCodeMsg());
        }else if(e instanceof BindException){
            BindException bindException = (BindException)e;
            List<ObjectError> errors = bindException.getAllErrors();
            String msg = errors.get(0).getDefaultMessage();
            return Result.error(CodeMsg.BIND_ERROR.fillArgs(msg));
        }else {
            return Result.error(CodeMsg.SERVER_ERROR);
        }
    }
}
```
###3-5 分布式Session
* 分布式Session一致性？

> 说白了就是服务器集群Session共享的问题

* Session的作用？

> Session 是客户端与服务器通讯会话跟踪技术，服务器与客户端保持整个通讯的会话基本信息。
> 客户端在第一次访问服务端的时候，服务端会响应一个sessionId并且将它存入到本地cookie中，在之后的访问会将cookie中的sessionId放入到请求头中去访问服务器，如果通过这个sessionid没有找到对应的数据那么服务器会创建一个新的sessionid并且响应给客户端。

* 分布式Session存在的问题？

> 假设第一次访问服务A生成一个sessionid并且存入cookie中，第二次却访问服务B客户端会在cookie中读取sessionid加入到请求头中，如果在服务B通过sessionid没有找到对应的数据那么它创建一个新的并且将sessionid返回给客户端,这样并不能共享我们的Session无法达到我们想要的目的。

* 解决方案

1、session复制

> session复制是早期的企业级的使用比较多的一种服务器集群session管理机制。应用服务器开启web容器的session复制功能，在集群中的几台服务器之间同步session对象，使得每台服务器上都保存所有的session信息，这样任何一台宕机都不会导致session的数据丢失，服务器使用session时，直接从本地获取。
> 这种方式在应用集群达到数千台的时候，就会出现瓶颈，每台都需要备份session，出现内存不够用的情况。

2、session绑定

> 利用hash算法，比如nginx的ip_hash,使得同一个Ip的请求分发到同一台服务器上。
> 这种方式不符合对系统的高可用要求，因为一旦某台服务器宕机，那么该机器上的session也就不复存在了，用户请求切换到其他机器后么有session，无法完成业务处理。

3、利用cookie记录session

> session记录在客户端，每次请求服务器的时候，将session放在请求中发送给服务器，服务器处理完请求后再将修改后的session响应给客户端。这里的客户端就是cookie。
> 利用cookie记录session的也有缺点，比如受cookie大小的限制，能记录的信息有限；每次请求响应都需要传递cookie，影响性能，如果用户关闭cookie，访问就不正常。但是由于
> cookie的简单易用，可用性高，支持应用服务器的线性伸缩，而大部分要记录的session信息比较小，因此事实上，许多网站或多或少的在使用cookie记录session。

4、session服务器

> session服务器可以解决上面的所有的问题，利用独立部署的session服务器（集群）统一管理session，服务器每次读写session时，都访问session服务器。
> 这种解决方案事实上是应用服务器的状态分离，分为无状态的应用服务器和有状态的session服务器，然后针对这两种服务器的不同特性分别设计架构。
> 对于有状态的session服务器，一种比较简单的方法是利用分布式缓存（memcached),数据库等。在这些产品的基础上进行包装，使其符合session的存储和访问要求。
> 如果业务场景对session管理有比较高的要求，比如利用session服务基层单点登录（sso),用户服务器等功能，需要开发专门的session服务管理平台。

这里利用Redis实现Session共享
```
@Service
public class MiaoshaUserService {

    @Autowired
    MiaoshaUserDao miaoshaUserDao;

    @Autowired
    RedisService redisService;

    public static final String COOKIE_NAME_TOKEN = "token";

    //登录
    public String login(HttpServletResponse response, LoginVo loginVo){
        if(loginVo == null){
            throw new GlobalException(CodeMsg.SERVER_ERROR);
        }
        String mobile = loginVo.getMobile();
        String password = loginVo.getPassword();
        MiaoshaUser miaoshaUser = getById(Long.parseLong(mobile));
        if(miaoshaUser == null){
            throw new GlobalException(CodeMsg.MOBILE_NOT_EXIST);
        }
        if(!miaoshaUser.getPassword().equals(MD5Util.formPassToDBPass(password,miaoshaUser.getSalt()))){
            throw new GlobalException(CodeMsg.PASSWORD_ERROR);
        }
        String token = UUIDUtil.uuid();
        addCookie(response,token,miaoshaUser);
        return token;
    }

    private void addCookie(HttpServletResponse response, String token, MiaoshaUser miaoshaUser) {
        //把token信息存入Redis
        redisService.set(MiaoshaUserKey.token,token,miaoshaUser);
        Cookie cookie = new Cookie(COOKIE_NAME_TOKEN,token);
        cookie.setMaxAge(MiaoshaUserKey.token.expireSeconds());
        cookie.setPath("/");
        //为浏览器添加cookie
        response.addCookie(cookie);
    }

    private MiaoshaUser getById(long mobile) {
        //取缓存
        MiaoshaUser miaoshaUser = redisService.get(MiaoshaUserKey.getById,"" + mobile,MiaoshaUser.class);
        if(miaoshaUser == null){
            //取数据库
            miaoshaUser = miaoshaUserDao.getById(mobile);
            if(miaoshaUser != null){
                redisService.set(MiaoshaUserKey.getById,"" + mobile,miaoshaUser);
            }
        }
        return miaoshaUser;
    }


    public MiaoshaUser getByToken(HttpServletResponse response, String token) {
        if(StringUtils.isEmpty(token)){
            return null;
        }
        MiaoshaUser miaoshaUser = redisService.get(MiaoshaUserKey.token,token,MiaoshaUser.class);
        if(miaoshaUser != null){
            addCookie(response,token,miaoshaUser);
        }
        return miaoshaUser;
    }
}

```
验证是否已经登录
```
    @RequestMapping("/test")
    @ResponseBody
    public String test(HttpServletResponse response, Model model,
                                    @CookieValue(value = MiaoshaUserService.COOKIE_NAME_TOKEN,required = false) String cookieToken,
                                    @RequestParam(value = MiaoshaUserService.COOKIE_NAME_TOKEN,required = false) String paramToken){
       if(StringUtils.isEmpty(cookieToken) && StringUtils.isEmpty(paramToken)){
           return "login";
       }
       String token = StringUtils.isEmpty(paramToken)?cookieToken:paramToken;
       MiaoshaUser miaoshaUser = userService.getByToken(response,token);
       model.addAttribute("user",miaoshaUser);
       return "success";
    }
```
为了提高代码的可重用性和可维护性，把上面验证做优化(根据MiaoshaUser的值判断是否验证成功)
```
    @RequestMapping("/info")
    @ResponseBody
    public Result<MiaoshaUser> info(Model model, MiaoshaUser user) {
        return Result.success(user);
    }
```
```
@Configuration
public class WebConfig extends WebMvcConfigurerAdapter {

    @Autowired
    UserArgumentResolver userArgumentResolver;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        argumentResolvers.add(userArgumentResolver);
    }

}

```
```
@Service
public class UserArgumentResolver implements HandlerMethodArgumentResolver {

    @Autowired
    MiaoshaUserService userService;

    public boolean supportsParameter(MethodParameter parameter) {
        Class<?> clazz = parameter.getParameterType();
        return clazz== MiaoshaUser.class;
    }

    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        HttpServletResponse response = webRequest.getNativeResponse(HttpServletResponse.class);

        String paramToken = request.getParameter(MiaoshaUserService.COOKIE_NAME_TOKEN);
        String cookieToken = getCookieValue(request, MiaoshaUserService.COOKIE_NAME_TOKEN);
        if(StringUtils.isEmpty(cookieToken) && StringUtils.isEmpty(paramToken)) {
            return null;
        }
        String token = StringUtils.isEmpty(paramToken)?cookieToken:paramToken;
        return userService.getByToken(response, token);
    }

    private String getCookieValue(HttpServletRequest request, String cookiName) {
        Cookie[]  cookies = request.getCookies();
        if(cookies == null || cookies.length <= 0){
            return null;
        }
        for(Cookie cookie : cookies) {
            if(cookie.getName().equals(cookiName)) {
                return cookie.getValue();
            }
        }
        return null;
    }

}
```
##4、秒杀功能开发
###4-1数据库设计
商品表
```
DROP TABLE IF EXISTS `goods`;
CREATE TABLE `goods`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '商品ID',
  `goods_name` varchar(16) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '商品名称',
  `goods_title` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '商品标题',
  `goods_img` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '商品的图片',
  `goods_detail` longtext CHARACTER SET utf8 COLLATE utf8_general_ci NULL COMMENT '商品的详情介绍',
  `goods_price` decimal(10, 2) NULL DEFAULT 0.00 COMMENT '商品单价',
  `goods_stock` int(11) NULL DEFAULT 0 COMMENT '商品库存，-1代表没有限制',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = MyISAM AUTO_INCREMENT = 9223372036854775808 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;
```
秒杀商品表
```
CREATE TABLE `miaosha_goods`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '秒杀的商品表ID',
  `goods_id` bigint(20) NULL DEFAULT NULL COMMENT '商品ID',
  `second_kill_price` decimal(10, 2) NULL DEFAULT 0.00 COMMENT '秒杀价',
  `stock_count` int(11) NULL DEFAULT NULL COMMENT '库存数量',
  `start_date` datetime NULL DEFAULT NULL COMMENT '秒杀开始时间',
  `end_date` datetime NULL DEFAULT NULL COMMENT '秒杀结束时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = MyISAM AUTO_INCREMENT = 5 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Fixed;
```
订单表
```
CREATE TABLE `orders`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  `user_id` bigint(20) NULL DEFAULT NULL COMMENT '用户ID',
  `goods_id` bigint(20) NULL DEFAULT NULL COMMENT '商品ID',
  `receive_address_id` bigint(20) NULL DEFAULT NULL COMMENT '收获地址ID',
  `goods_name` varchar(16) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '冗余过来的商品名称',
  `goods_count` int(11) NULL DEFAULT NULL COMMENT '商品数量',
  `goods_price` decimal(10, 2) NULL DEFAULT 0.00 COMMENT '商品单价',
  `order_channel` tinyint(4) NULL DEFAULT 0 COMMENT '订单渠道，1：pc , 2：android , 3：ios',
  `order_status` tinyint(4) NULL DEFAULT 0 COMMENT '订单状态，0：新建未支付，1：已支付，2：已发货，3：已收货，4：已退款，5：已完成',
  `create_date` datetime NULL DEFAULT NULL COMMENT '订单的创建时间',
  `pay_date` datetime NULL DEFAULT NULL COMMENT '支付时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = MyISAM AUTO_INCREMENT = 8 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;
```
秒杀订单表
```
CREATE TABLE `miaosha_order`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '秒杀订单ID',
  `user_id` bigint(20) NULL DEFAULT NULL COMMENT '用户ID',
  `order_id` bigint(20) NULL DEFAULT NULL COMMENT '订单ID',
  `goods_id` bigint(20) NULL DEFAULT NULL COMMENT '商品ID',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = MyISAM AUTO_INCREMENT = 7 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Fixed;
```
踩坑：开发时数据库中商品表的字段与sql语句中的字段不一致，导致发生异常，虽然添加了事务但是事务并没有回滚，原因是把商品表设计成了MyISam存储引擎，不支持事务
###4-2商品列表展示
页面
```
<!DOCTYPE HTML>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>商品详情</title>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <!-- jquery -->
    <script type="text/javascript" th:src="@{/js/jquery.min.js}"></script>
    <!-- bootstrap -->
    <link rel="stylesheet" type="text/css" th:href="@{/bootstrap/css/bootstrap.min.css}" />
    <script type="text/javascript" th:src="@{/bootstrap/js/bootstrap.min.js}"></script>
    <!-- jquery-validator -->
    <script type="text/javascript" th:src="@{/jquery-validation/jquery.validate.min.js}"></script>
    <script type="text/javascript" th:src="@{/jquery-validation/localization/messages_zh.min.js}"></script>
    <!-- layer -->
    <script type="text/javascript" th:src="@{/layer/layer.js}"></script>
    <!-- md5.js -->
    <script type="text/javascript" th:src="@{/js/md5.min.js}"></script>
    <!-- common.js -->
    <script type="text/javascript" th:src="@{/js/common.js}"></script>
</head>
<body>

<div class="panel panel-default">
  <div class="panel-heading">秒杀商品详情</div>
  <div class="panel-body">
  	<span th:if="${user eq null}"> 您还没有登录，请登陆后再操作<br/></span>
  	<span>没有收货地址的提示。。。</span>
  </div>
  <table class="table" id="goodslist">
  	<tr>  
        <td>商品名称</td>  
        <td colspan="3" th:text="${goods.goodsName}"></td> 
     </tr>  
     <tr>  
        <td>商品图片</td>  
        <td colspan="3"><img th:src="@{${goods.goodsImg}}" width="200" height="200" /></td>  
     </tr>
     <tr>  
        <td>秒杀开始时间</td>  
        <td th:text="${#dates.format(goods.startDate, 'yyyy-MM-dd HH:mm:ss')}"></td>
        <td id="miaoshaTip">	
        	<input type="hidden" id="remainSeconds" th:value="${remainSeconds}" />
        	<span th:if="${miaoshaStatus eq 0}">秒杀倒计时：<span id="countDown" th:text="${remainSeconds}"></span>秒</span>
        	<span th:if="${miaoshaStatus eq 1}">秒杀进行中</span>
        	<span th:if="${miaoshaStatus eq 2}">秒杀已结束</span>
        </td>
        <td>
        	<form id="miaoshaForm" method="post" action="/miaosha/do_miaosha">
        		<button class="btn btn-primary btn-block" type="submit" id="buyButton">立即秒杀</button>
        		<input type="hidden" name="goodsId" th:value="${goods.id}" />
        	</form>
        </td>
     </tr>
     <tr>  
        <td>商品原价</td>  
        <td colspan="3" th:text="${goods.goodsPrice}"></td>  
     </tr>
      <tr>  
        <td>秒杀价</td>  
        <td colspan="3" th:text="${goods.miaoshaPrice}"></td>  
     </tr>
     <tr>  
        <td>库存数量</td>  
        <td colspan="3" th:text="${goods.stockCount}"></td>  
     </tr>
  </table>
</div>
</body>
<script>
$(function(){
	countDown();
});

function countDown(){
	var remainSeconds = $("#remainSeconds").val();
	var timeout;
	if(remainSeconds > 0){//秒杀还没开始，倒计时
		$("#buyButton").attr("disabled", true);
		timeout = setTimeout(function(){
			$("#countDown").text(remainSeconds - 1);
			$("#remainSeconds").val(remainSeconds - 1);
			countDown();
		},1000);
	}else if(remainSeconds == 0){//秒杀进行中
		$("#buyButton").attr("disabled", false);
		if(timeout){
			clearTimeout(timeout);
		}
		$("#miaoshaTip").html("秒杀进行中");
	}else{//秒杀已经结束
		$("#buyButton").attr("disabled", true);
		$("#miaoshaTip").html("秒杀已经结束");
	}
}

</script>
</html>

```
controller
```
    @RequestMapping("/to_list")
    public String list(Model model, MiaoshaUser miaoshaUser){
        model.addAttribute("user",miaoshaUser);
        List<GoodsVo> goodsList = goodsService.listGoodsVo();
        model.addAttribute("goodsList",goodsList);
        return "goods_list";
    }
```
service
```
    public List<GoodsVo> listGoodsVo(){
		return goodsDao.listGoodsVo();
	}
```
dao
```
    @Select("select g.*,mg.stock_count, mg.start_date, mg.end_date,mg.miaosha_price from miaosha_goods mg left join goods g on mg.goods_id = g.id")
	public List<GoodsVo> listGoodsVo();
```
注意点：在查询秒杀商品时对商品信息和秒杀商品信息进行联合查询，并对查询结果封装成包含两者信息的GoodsVo对象。
###4-3秒杀商品详情
页面
```
<!DOCTYPE HTML>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>商品详情</title>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <!-- jquery -->
    <script type="text/javascript" th:src="@{/js/jquery.min.js}"></script>
    <!-- bootstrap -->
    <link rel="stylesheet" type="text/css" th:href="@{/bootstrap/css/bootstrap.min.css}" />
    <script type="text/javascript" th:src="@{/bootstrap/js/bootstrap.min.js}"></script>
    <!-- jquery-validator -->
    <script type="text/javascript" th:src="@{/jquery-validation/jquery.validate.min.js}"></script>
    <script type="text/javascript" th:src="@{/jquery-validation/localization/messages_zh.min.js}"></script>
    <!-- layer -->
    <script type="text/javascript" th:src="@{/layer/layer.js}"></script>
    <!-- md5.js -->
    <script type="text/javascript" th:src="@{/js/md5.min.js}"></script>
    <!-- common.js -->
    <script type="text/javascript" th:src="@{/js/common.js}"></script>
</head>
<body>

<div class="panel panel-default">
  <div class="panel-heading">秒杀商品详情</div>
  <div class="panel-body">
  	<span th:if="${user eq null}"> 您还没有登录，请登陆后再操作<br/></span>
  	<span>没有收货地址的提示。。。</span>
  </div>
  <table class="table" id="goodslist">
  	<tr>  
        <td>商品名称</td>  
        <td colspan="3" th:text="${goods.goodsName}"></td> 
     </tr>  
     <tr>  
        <td>商品图片</td>  
        <td colspan="3"><img th:src="@{${goods.goodsImg}}" width="200" height="200" /></td>  
     </tr>
     <tr>  
        <td>秒杀开始时间</td>  
        <td th:text="${#dates.format(goods.startDate, 'yyyy-MM-dd HH:mm:ss')}"></td>
        <td id="miaoshaTip">	
        	<input type="hidden" id="remainSeconds" th:value="${remainSeconds}" />
        	<span th:if="${miaoshaStatus eq 0}">秒杀倒计时：<span id="countDown" th:text="${remainSeconds}"></span>秒</span>
        	<span th:if="${miaoshaStatus eq 1}">秒杀进行中</span>
        	<span th:if="${miaoshaStatus eq 2}">秒杀已结束</span>
        </td>
        <td>
        	<form id="miaoshaForm" method="post" action="/miaosha/do_miaosha">
        		<button class="btn btn-primary btn-block" type="submit" id="buyButton">立即秒杀</button>
        		<input type="hidden" name="goodsId" th:value="${goods.id}" />
        	</form>
        </td>
     </tr>
     <tr>  
        <td>商品原价</td>  
        <td colspan="3" th:text="${goods.goodsPrice}"></td>  
     </tr>
      <tr>  
        <td>秒杀价</td>  
        <td colspan="3" th:text="${goods.miaoshaPrice}"></td>  
     </tr>
     <tr>  
        <td>库存数量</td>  
        <td colspan="3" th:text="${goods.stockCount}"></td>  
     </tr>
  </table>
</div>
</body>
<script>
$(function(){
	countDown();
});

function countDown(){
	var remainSeconds = $("#remainSeconds").val();
	var timeout;
	if(remainSeconds > 0){//秒杀还没开始，倒计时
		$("#buyButton").attr("disabled", true);
		timeout = setTimeout(function(){
			$("#countDown").text(remainSeconds - 1);
			$("#remainSeconds").val(remainSeconds - 1);
			countDown();
		},1000);
	}else if(remainSeconds == 0){//秒杀进行中
		$("#buyButton").attr("disabled", false);
		if(timeout){
			clearTimeout(timeout);
		}
		$("#miaoshaTip").html("秒杀进行中");
	}else{//秒杀已经结束
		$("#buyButton").attr("disabled", true);
		$("#miaoshaTip").html("秒杀已经结束");
	}
}

</script>
</html>

```
controller
```
    @RequestMapping("/to_detail/{goodsId}")
    public String detail(Model model, MiaoshaUser miaoshaUser, @PathVariable("goodsId")long goodsId){

        model.addAttribute("user",miaoshaUser);

        GoodsVo goodsVo = goodsService.getGoodsVoByGoodsId(goodsId);
        model.addAttribute("goods",goodsVo);

        long startAt = goodsVo.getStartDate().getTime();
        long endAt = goodsVo.getEndDate().getTime();
        long now = System.currentTimeMillis();
        int miaoshaStatus = 0;
        int remainSeconds = 0;
        if(now < startAt){      //秒杀还没开始
            miaoshaStatus = 0;
            remainSeconds = (int)((startAt - now)/1000);
        }else if(now > endAt){  //秒杀已结束
            miaoshaStatus = 2;
            remainSeconds = -1;
        }else {                 //秒杀进行中
            miaoshaStatus = 1;
            remainSeconds = 0;
        }
        model.addAttribute("miaoshaStatus", miaoshaStatus);
        model.addAttribute("remainSeconds", remainSeconds);
        return "goods_detail";
    }
```
service
```
    public GoodsVo getGoodsVoByGoodsId(long goodsId) {
		return goodsDao.getGoodsVoByGoodsId(goodsId);
	}
```
dao
```
    @Select("select g.*,mg.stock_count, mg.start_date, mg.end_date,mg.miaosha_price from miaosha_goods mg left join goods g on mg.goods_id = g.id where g.id = #{goodsId}")
	public GoodsVo getGoodsVoByGoodsId(@Param("goodsId") long goodsId);
```
###4-4秒杀
controller
```
    @RequestMapping("/do_miaosha")
    public String list(Model model, MiaoshaUser user,
                       @RequestParam("goodsId")long goodsId) {
        model.addAttribute("user", user);
        if(user == null) {
            return "login";
        }
        //判断库存
        GoodsVo goods = goodsService.getGoodsVoByGoodsId(goodsId);
        int stock = goods.getStockCount();
        if(stock <= 0) {
            model.addAttribute("errmsg", CodeMsg.MIAO_SHA_OVER.getMsg());
            return "miaosha_fail";
        }
        //判断是否已经秒杀到了
        MiaoshaOrder order = orderService.getMiaoshaOrderByUserIdGoodsId(user.getId(), goodsId);
        if(order != null) {
            model.addAttribute("errmsg", CodeMsg.REPEATE_MIAOSHA.getMsg());
            return "miaosha_fail";
        }
        //减库存 下订单 写入秒杀订单
        OrderInfo orderInfo = miaoshaService.miaosha(user, goods);
        model.addAttribute("orderInfo", orderInfo);
        model.addAttribute("goods", goods);
        return "order_detail";
    }
```
service
```
    @Transactional
	public OrderInfo miaosha(MiaoshaUser user, GoodsVo goods) {
		//减库存 下订单 写入秒杀订单
		goodsService.reduceStock(goods);
		//order_info maiosha_order
		OrderInfo orderInfo = orderService.createOrder(user, goods);
		return orderInfo;
	}
```
```
    public GoodsVo getGoodsVoByGoodsId(long goodsId) {
		return goodsDao.getGoodsVoByGoodsId(goodsId);
	}

	public boolean reduceStock(GoodsVo goods) {
		MiaoshaGoods g = new MiaoshaGoods();
		g.setGoodsId(goods.getId());
		int ret=goodsDao.reduceStock(g);
		return ret>0;
	}
```
```
    public MiaoshaOrder getMiaoshaOrderByUserIdGoodsId(long userId, long goodsId) {
		return orderDao.getMiaoshaOrderByUserIdGoodsId(userId, goodsId);
	}

	@Transactional
	public OrderInfo createOrder(MiaoshaUser user, GoodsVo goods) {
		OrderInfo orderInfo = new OrderInfo();
		orderInfo.setCreateDate(new Date());
		orderInfo.setDeliveryAddrId(0L);
		orderInfo.setGoodsCount(1);
		orderInfo.setGoodsId(goods.getId());
		orderInfo.setGoodsName(goods.getGoodsName());
		orderInfo.setGoodsPrice(goods.getMiaoshaPrice());
		orderInfo.setOrderChannel(1);
		orderInfo.setStatus(0);
		orderInfo.setUserId(user.getId());
		long orderId = orderDao.insert(orderInfo);
		MiaoshaOrder miaoshaOrder = new MiaoshaOrder();
		miaoshaOrder.setGoodsId(goods.getId());
		miaoshaOrder.setOrderId(orderId);
		miaoshaOrder.setUserId(user.getId());
		orderDao.insertMiaoshaOrder(miaoshaOrder);
		return orderInfo;
	}
```
dao
```
    @Update("update miaosha_goods set stock_count = stock_count - 1 where goods_id = #{goodsId} and stock_count > 0")
	public int reduceStock(MiaoshaGoods g);
```
```
    @Select("select * from miaosha_order where user_id=#{userId} and goods_id=#{goodsId}")
	public MiaoshaOrder getMiaoshaOrderByUserIdGoodsId(@Param("userId") long userId, @Param("goodsId") long goodsId);

	@Insert("insert into order_info(user_id, goods_id, goods_name, goods_count, goods_price, order_channel, status, create_date)values("
			+ "#{userId}, #{goodsId}, #{goodsName}, #{goodsCount}, #{goodsPrice}, #{orderChannel},#{status},#{createDate} )")
	@SelectKey(keyColumn="id", keyProperty="id", resultType=long.class, before=false, statement="select last_insert_id()")
	public long insert(OrderInfo orderInfo);

	@Insert("insert into miaosha_order (user_id, goods_id, order_id)values(#{userId}, #{goodsId}, #{orderId})")
	public int insertMiaoshaOrder(MiaoshaOrder miaoshaOrder);
```
###4-5订单详情
页面
```
<!DOCTYPE HTML>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>订单详情</title>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <!-- jquery -->
    <script type="text/javascript" th:src="@{/js/jquery.min.js}"></script>
    <!-- bootstrap -->
    <link rel="stylesheet" type="text/css" th:href="@{/bootstrap/css/bootstrap.min.css}" />
    <script type="text/javascript" th:src="@{/bootstrap/js/bootstrap.min.js}"></script>
    <!-- jquery-validator -->
    <script type="text/javascript" th:src="@{/jquery-validation/jquery.validate.min.js}"></script>
    <script type="text/javascript" th:src="@{/jquery-validation/localization/messages_zh.min.js}"></script>
    <!-- layer -->
    <script type="text/javascript" th:src="@{/layer/layer.js}"></script>
    <!-- md5.js -->
    <script type="text/javascript" th:src="@{/js/md5.min.js}"></script>
    <!-- common.js -->
    <script type="text/javascript" th:src="@{/js/common.js}"></script>
</head>
<body>
<div class="panel panel-default">
  <div class="panel-heading">秒杀订单详情</div>
  <table class="table" id="goodslist">
        <tr>  
        <td>商品名称</td>  
        <td th:text="${goods.goodsName}" colspan="3"></td> 
     </tr>  
     <tr>  
        <td>商品图片</td>  
        <td colspan="2"><img th:src="@{${goods.goodsImg}}" width="200" height="200" /></td>  
     </tr>
      <tr>  
        <td>订单价格</td>  
        <td colspan="2" th:text="${orderInfo.goodsPrice}"></td>  
     </tr>
     <tr>
     		<td>下单时间</td>  
        	<td th:text="${#dates.format(orderInfo.createDate, 'yyyy-MM-dd HH:mm:ss')}" colspan="2"></td>  
     </tr>
     <tr>
     	<td>订单状态</td>  
        <td >
        	<span th:if="${orderInfo.status eq 0}">未支付</span>
        	<span th:if="${orderInfo.status eq 1}">待发货</span>
        	<span th:if="${orderInfo.status eq 2}">已发货</span>
        	<span th:if="${orderInfo.status eq 3}">已收货</span>
        	<span th:if="${orderInfo.status eq 4}">已退款</span>
        	<span th:if="${orderInfo.status eq 5}">已完成</span>
        </td>  
        <td>
        	<button class="btn btn-primary btn-block" type="submit" id="payButton">立即支付</button>
        </td>
     </tr>
      <tr>
     		<td>收货人</td>  
        	<td colspan="2">XXX  18812341234</td>  
     </tr>
     <tr>
     		<td>收货地址</td>  
        	<td colspan="2">北京市昌平区回龙观龙博一区</td>  
     </tr>
  </table>
</div>

</body>
</html>

```
##5、Jmeter压测
编写自定义变量，生成请求参数
```
public class DBUtil {
	
	private static Properties props;
	
	static {
		try {
			InputStream in = DBUtil.class.getClassLoader().getResourceAsStream("application.properties");
			props = new Properties();
			props.load(in);
			in.close();
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public static Connection getConn() throws Exception{
		String url = props.getProperty("spring.datasource.url");
		String username = props.getProperty("spring.datasource.username");
		String password = props.getProperty("spring.datasource.password");
		String driver = props.getProperty("spring.datasource.driver-class-name");
		Class.forName(driver);
		return DriverManager.getConnection(url,username, password);
	}
}

```
```
public class UserUtil {
	
	private static void createUser(int count) throws Exception{
		List<MiaoshaUser> users = new ArrayList<MiaoshaUser>(count);
		//生成用户
		for(int i=0;i<count;i++) {
			MiaoshaUser user = new MiaoshaUser();
			user.setId(13000000000L+i);
			user.setLoginCount(1);
			user.setNickname("user"+i);
			user.setRegisterDate(new Date());
			user.setSalt("1a2b3c");
			user.setPassword(MD5Util.inputPassToDBPass("123456", user.getSalt()));
			users.add(user);
		}
		System.out.println("create user");
		/*//插入数据库
		Connection conn = DBUtil.getConn();
		String sql = "insert into miaosha_user(login_count, nickname, register_date, salt, password, id)values(?,?,?,?,?,?)";
		PreparedStatement pstmt = conn.prepareStatement(sql);
		for(int i=0;i<users.size();i++) {
			MiaoshaUser user = users.get(i);
			pstmt.setInt(1, user.getLoginCount());
			pstmt.setString(2, user.getNickname());
			pstmt.setTimestamp(3, new Timestamp(user.getRegisterDate().getTime()));
			pstmt.setString(4, user.getSalt());
			pstmt.setString(5, user.getPassword());
			pstmt.setLong(6, user.getId());
			pstmt.addBatch();
		}
		pstmt.executeBatch();
		pstmt.close();
		conn.close();
		System.out.println("insert to db");*/
		//登录，生成token
		String urlString = "http://localhost:8080/login/do_login";
		File file = new File("F:/jmx/configVar/tokens.txt");
		if(file.exists()) {
			file.delete();
		}
		RandomAccessFile raf = new RandomAccessFile(file, "rw");
		file.createNewFile();
		raf.seek(0);
		for(int i=0;i<users.size();i++) {
			MiaoshaUser user = users.get(i);
			URL url = new URL(urlString);
			HttpURLConnection co = (HttpURLConnection)url.openConnection();
			co.setRequestMethod("POST");
			co.setDoOutput(true);
			OutputStream out = co.getOutputStream();
			String params = "mobile="+user.getId()+"&password="+MD5Util.inputPassToFormPass("123456");
			out.write(params.getBytes());
			out.flush();
			InputStream inputStream = co.getInputStream();
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			byte buff[] = new byte[1024];
			int len = 0;
			while((len = inputStream.read(buff)) >= 0) {
				bout.write(buff, 0 ,len);
			}
			inputStream.close();
			bout.close();
			String response = new String(bout.toByteArray());
			JSONObject jo = JSON.parseObject(response);
			String token = jo.getString("data");
			System.out.println("create token : " + user.getId());
			
			String row = user.getId()+","+token;
			raf.seek(raf.length());
			raf.write(row.getBytes());
			raf.write("\r\n".getBytes());
			System.out.println("write to file : " + user.getId());
		}
		raf.close();
		
		System.out.println("over");
	}
	
	public static void main(String[] args)throws Exception {
		createUser(5000);
	}
}

```
商品列表QPS为1267（5000*10）
秒杀QPS为1306（5000*10）
##6、页面优化技术
1、页面缓存+URL缓存+对象缓存
2、页面静态化，前后端分离
3、静态资源优化
4、CDN优化
###6-1页面缓存

> 1、取缓存 
2、手动渲染模板 
3、输出结果

注意设置失效时间（60s），一般用于变化不大的页面
商品列表页面缓存（QPS：2884）
```
    @RequestMapping(value="/to_list", produces="text/html")
    @ResponseBody
    public String list(HttpServletRequest request, HttpServletResponse response, Model model, MiaoshaUser user) {
        model.addAttribute("user", user);
        //取缓存
        String html = redisService.get(GoodsKey.getGoodsList, "", String.class);
        if(!StringUtils.isEmpty(html)) {
            return html;
        }
        List<GoodsVo> goodsList = goodsService.listGoodsVo();
        model.addAttribute("goodsList", goodsList);
//    	 return "goods_list";
        WebContext ctx = new WebContext(request,response,
                request.getServletContext(),request.getLocale(), model.asMap());
        //手动渲染
        html = thymeleafViewResolver.getTemplateEngine().process("goods_list", ctx);
        if(!StringUtils.isEmpty(html)) {
            redisService.set(GoodsKey.getGoodsList, "", html);
        }
        return html;
    }
```
商品详情URL缓存（url参数不同缓存不同，和页面缓存类似）
```
    @RequestMapping(value="/to_detail2/{goodsId}",produces="text/html")
    @ResponseBody
    public String detail2(HttpServletRequest request, HttpServletResponse response, Model model,MiaoshaUser user,
                          @PathVariable("goodsId")long goodsId) {
        model.addAttribute("user", user);

        //取缓存
        String html = redisService.get(GoodsKey.getGoodsDetail, ""+goodsId, String.class);
        if(!StringUtils.isEmpty(html)) {
            return html;
        }
        //手动渲染
        GoodsVo goods = goodsService.getGoodsVoByGoodsId(goodsId);
        model.addAttribute("goods", goods);

        long startAt = goods.getStartDate().getTime();
        long endAt = goods.getEndDate().getTime();
        long now = System.currentTimeMillis();

        int miaoshaStatus = 0;
        int remainSeconds = 0;
        if(now < startAt ) {//秒杀还没开始，倒计时
            miaoshaStatus = 0;
            remainSeconds = (int)((startAt - now )/1000);
        }else  if(now > endAt){//秒杀已经结束
            miaoshaStatus = 2;
            remainSeconds = -1;
        }else {//秒杀进行中
            miaoshaStatus = 1;
            remainSeconds = 0;
        }
        model.addAttribute("miaoshaStatus", miaoshaStatus);
        model.addAttribute("remainSeconds", remainSeconds);
//        return "goods_detail";

        WebContext ctx = new WebContext(request,response,
                request.getServletContext(),request.getLocale(), model.asMap());
        html = thymeleafViewResolver.getTemplateEngine().process("goods_detail", ctx);
        if(!StringUtils.isEmpty(html)) {
            redisService.set(GoodsKey.getGoodsDetail, ""+goodsId, html);
        }
        return html;
    }
```
###6-2对象缓存
把对象放在redis缓存中
```
    private MiaoshaUser getById(long mobile) {
        //取缓存
        MiaoshaUser miaoshaUser = redisService.get(MiaoshaUserKey.getById,"" + mobile,MiaoshaUser.class);
        if(miaoshaUser == null){
            //取数据库
            miaoshaUser = miaoshaUserDao.getById(mobile);
            if(miaoshaUser != null){
                redisService.set(MiaoshaUserKey.getById,"" + mobile,miaoshaUser);
            }
        }
        return miaoshaUser;
    }

    public boolean updatePassword(String token, long id, String formPass) {
        //取user
        MiaoshaUser user = getById(id);
        if(user == null) {
            throw new GlobalException(CodeMsg.MOBILE_NOT_EXIST);
        }
        //更新数据库
        MiaoshaUser toBeUpdate = new MiaoshaUser();
        toBeUpdate.setId(id);
        toBeUpdate.setPassword(MD5Util.formPassToDBPass(formPass, user.getSalt()));
        miaoshaUserDao.update(toBeUpdate);
        //处理缓存
        redisService.delete(MiaoshaUserKey.getById, ""+id);
        user.setPassword(toBeUpdate.getPassword());
        redisService.set(MiaoshaUserKey.token, token, user);
        return true;
    }
```
更新：先把数据存到数据库中，成功后，在让缓存失效（数据一致性）
注意其他service调用时不要直接操作dao层接口，而是service层的接口，因为可能在service中设置了缓存，注意数据库数据和缓存的一致性
###6-3商品详情静态化
常用技术：AngularJS、Vue.js

优点：利用浏览器的缓存

之前的逻辑：点击链接，访问后端controller 访问业务层，成功获取数据，将数据渲染到html页面再将整个html页面返回给客户显示

现在：点击链接，除了第一次访问。访问直接访问用户本地的缓存的html页面 （浏览器会缓存下来静态static下文件），静态资源，然后通过前端ajAx来访问后端，获取页面需要显示的数据返回即可。（应该不是static下所有的文件都会缓存下来，只是静态页面中所关联到的资源，根据浏览器厂商而定？？）

模拟：编写goods_detail.htm页面（不要和goods_detail.html重复），并放在static目录下（页面在客户端），商品列表请求详情页面的url改为/goods_detail.htm?goodsId='+${goods.id}，即会请求客户端的goods_detail.htm纯html页面，获取url参数，然后再通过ajax请求向服务器请求数据，所以controller接口也要修改，返回响应数据，动态把数据填充到页面中。
大概：将页面缓存到客户的浏览器上，当用户访问页面的时候，直接不与服务器有交互，直接从本地缓存中拿取页面，节省网络流量。

```
    @RequestMapping(value="/detail/{goodsId}")
    @ResponseBody
    public Result<GoodsDetailVo> detail(HttpServletRequest request, HttpServletResponse response, Model model,MiaoshaUser user,
                                        @PathVariable("goodsId")long goodsId) {
        GoodsVo goods = goodsService.getGoodsVoByGoodsId(goodsId);
        long startAt = goods.getStartDate().getTime();
        long endAt = goods.getEndDate().getTime();
        long now = System.currentTimeMillis();
        int miaoshaStatus = 0;
        int remainSeconds = 0;
        if(now < startAt ) {//秒杀还没开始，倒计时
            miaoshaStatus = 0;
            remainSeconds = (int)((startAt - now )/1000);
        }else  if(now > endAt){//秒杀已经结束
            miaoshaStatus = 2;
            remainSeconds = -1;
        }else {//秒杀进行中
            miaoshaStatus = 1;
            remainSeconds = 0;
        }
        GoodsDetailVo vo = new GoodsDetailVo();
        vo.setGoods(goods);
        vo.setUser(user);
        vo.setRemainSeconds(remainSeconds);
        vo.setMiaoshaStatus(miaoshaStatus);
        return Result.success(vo);
    }
```
```
<!DOCTYPE HTML>
<html >
<head>
    <title>商品详情</title>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <!-- jquery -->
    <script type="text/javascript" src="/js/jquery.min.js"></script>
    <!-- bootstrap -->
    <link rel="stylesheet" type="text/css" href="/bootstrap/css/bootstrap.min.css" />
    <script type="text/javascript" src="/bootstrap/js/bootstrap.min.js"></script>
    <!-- jquery-validator -->
    <script type="text/javascript" src="/jquery-validation/jquery.validate.min.js"></script>
    <script type="text/javascript" src="/jquery-validation/localization/messages_zh.min.js"></script>
    <!-- layer -->
    <script type="text/javascript" src="/layer/layer.js"></script>
    <!-- md5.js -->
    <script type="text/javascript" src="/js/md5.min.js"></script>
    <!-- common.js -->
    <script type="text/javascript" src="/js/common.js"></script>
</head>
<body>

<div class="panel panel-default">
  <div class="panel-heading">秒杀商品详情</div>
  <div class="panel-body">
  	<span id="userTip"> 您还没有登录，请登陆后再操作<br/></span>
  	<span>没有收货地址的提示。。。</span>
  </div>
  <table class="table" id="goodslist">
  	<tr>  
        <td>商品名称</td>  
        <td colspan="3" id="goodsName"></td> 
     </tr>  
     <tr>  
        <td>商品图片</td>  
        <td colspan="3"><img  id="goodsImg" width="200" height="200" /></td>  
     </tr>
     <tr>  
        <td>秒杀开始时间</td>  
        <td id="startTime"></td>
        <td >	
        	<input type="hidden" id="remainSeconds" />
        	<span id="miaoshaTip"></span>
        </td>
        <td>
        <!--  
        	<form id="miaoshaForm" method="post" action="/miaosha/do_miaosha">
        		<button class="btn btn-primary btn-block" type="submit" id="buyButton">立即秒杀</button>
        		<input type="hidden" name="goodsId"  id="goodsId" />
        	</form>-->
        	<button class="btn btn-primary btn-block" type="button" id="buyButton"onclick="doMiaosha()">立即秒杀</button>
        	<input type="hidden" name="goodsId"  id="goodsId" />
        </td>
     </tr>
     <tr>  
        <td>商品原价</td>  
        <td colspan="3" id="goodsPrice"></td>  
     </tr>
      <tr>  
        <td>秒杀价</td>  
        <td colspan="3"  id="miaoshaPrice"></td>  
     </tr>
     <tr>  
        <td>库存数量</td>  
        <td colspan="3"  id="stockCount"></td>  
     </tr>
  </table>
</div>
</body>
<script>

function doMiaosha(){
	$.ajax({
		url:"/miaosha/do_miaosha",
		type:"POST",
		data:{
			goodsId:$("#goodsId").val(),
		},
		success:function(data){
			if(data.code == 0){
				window.location.href="/order_detail.htm?orderId="+data.data.id;
			}else{
				layer.msg(data.msg);
			}
		},
		error:function(){
			layer.msg("客户端请求有误");
		}
	});
	
}

function render(detail){
	var miaoshaStatus = detail.miaoshaStatus;
	var  remainSeconds = detail.remainSeconds;
	var goods = detail.goods;
	var user = detail.user;
	if(user){
		$("#userTip").hide();
	}
	$("#goodsName").text(goods.goodsName);
	$("#goodsImg").attr("src", goods.goodsImg);
	$("#startTime").text(new Date(goods.startDate).format("yyyy-MM-dd hh:mm:ss"));
	$("#remainSeconds").val(remainSeconds);
	$("#goodsId").val(goods.id);
	$("#goodsPrice").text(goods.goodsPrice);
	$("#miaoshaPrice").text(goods.miaoshaPrice);
	$("#stockCount").text(goods.stockCount);
	countDown();
}

$(function(){
	//countDown();
	getDetail();
});

function getDetail(){
	var goodsId = g_getQueryString("goodsId");
	$.ajax({
		url:"/goods/detail/"+goodsId,
		type:"GET",
		success:function(data){
			if(data.code == 0){
				render(data.data);
			}else{
				layer.msg(data.msg);
			}
		},
		error:function(){
			layer.msg("客户端请求有误");
		}
	});
}

function countDown(){
	var remainSeconds = $("#remainSeconds").val();
	var timeout;
	if(remainSeconds > 0){//秒杀还没开始，倒计时
		$("#buyButton").attr("disabled", true);
	   $("#miaoshaTip").html("秒杀倒计时："+remainSeconds+"秒");
		timeout = setTimeout(function(){
			$("#countDown").text(remainSeconds - 1);
			$("#remainSeconds").val(remainSeconds - 1);
			countDown();
		},1000);
	}else if(remainSeconds == 0){//秒杀进行中
		$("#buyButton").attr("disabled", false);
		if(timeout){
			clearTimeout(timeout);
		}
		$("#miaoshaTip").html("秒杀进行中");
	}else{//秒杀已经结束
		$("#buyButton").attr("disabled", true);
		$("#miaoshaTip").html("秒杀已经结束");
	}
}

</script>
</html>

```
```
// 获取url参数
function g_getQueryString(name) {
	var reg = new RegExp("(^|&)" + name + "=([^&]*)(&|$)");
	var r = window.location.search.substr(1).match(reg);
	if(r != null) return unescape(r[2]);
	return null;
};
```
###6-4秒杀及订单静态化
商品详情页中为秒杀按钮添加事件
```
function doMiaosha(){
	$.ajax({
		url:"/miaosha/do_miaosha",
		type:"POST",
		data:{
			goodsId:$("#goodsId").val(),
		},
		success:function(data){
			if(data.code == 0){
				window.location.href="/order_detail.htm?orderId="+data.data.id;
			}else{
				layer.msg(data.msg);
			}
		},
		error:function(){
			layer.msg("客户端请求有误");
		}
	});
	
}
```
秒杀操作返回数据
```
@RequestMapping(value="/do_miaosha", method=RequestMethod.POST)
    @ResponseBody
    public Result<OrderInfo> miaosha(Model model,MiaoshaUser user,
    		@RequestParam("goodsId")long goodsId) {
    	model.addAttribute("user", user);
    	if(user == null) {
    		return Result.error(CodeMsg.SESSION_ERROR);
    	}
    	//判断库存
    	GoodsVo goods = goodsService.getGoodsVoByGoodsId(goodsId);//10个商品，req1 req2
    	int stock = goods.getStockCount();
    	if(stock <= 0) {
    		return Result.error(CodeMsg.MIAO_SHA_OVER);
    	}
    	//判断是否已经秒杀到了
    	MiaoshaOrder order = orderService.getMiaoshaOrderByUserIdGoodsId(user.getId(), goodsId);
    	if(order != null) {
    		return Result.error(CodeMsg.REPEATE_MIAOSHA);
    	}
    	//减库存 下订单 写入秒杀订单
    	OrderInfo orderInfo = miaoshaService.miaosha(user, goods);
        return Result.success(orderInfo);
    }
```
转到浏览器缓存的订单详情页面，并根据参数订单号发送ajax请求，返回数据渲染页面
```
<!DOCTYPE HTML>
<html>
<head>
    <title>订单详情</title>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <!-- jquery -->
    <script type="text/javascript" src="/js/jquery.min.js"></script>
    <!-- bootstrap -->
    <link rel="stylesheet" type="text/css" href="/bootstrap/css/bootstrap.min.css" />
    <script type="text/javascript" src="/bootstrap/js/bootstrap.min.js"></script>
    <!-- jquery-validator -->
    <script type="text/javascript" src="/jquery-validation/jquery.validate.min.js"></script>
    <script type="text/javascript" src="/jquery-validation/localization/messages_zh.min.js"></script>
    <!-- layer -->
    <script type="text/javascript" src="/layer/layer.js"></script>
    <!-- md5.js -->
    <script type="text/javascript" src="/js/md5.min.js"></script>
    <!-- common.js -->
    <script type="text/javascript" src="/js/common.js"></script>
</head>
<body>
<div class="panel panel-default">
  <div class="panel-heading">秒杀订单详情</div>
  <table class="table" id="goodslist">
        <tr>  
        <td>商品名称</td>  
        <td colspan="3" id="goodsName"></td> 
     </tr>  
     <tr>  
        <td>商品图片</td>  
        <td colspan="2"><img  id="goodsImg" width="200" height="200" /></td>  
     </tr>
      <tr>  
        <td>订单价格</td>  
        <td colspan="2"  id="orderPrice"></td>  
     </tr>
     <tr>
     		<td>下单时间</td>  
        	<td id="createDate" colspan="2"></td>  
     </tr>
     <tr>
     	<td>订单状态</td>  
        <td id="orderStatus">
        </td>  
        <td>
        	<button class="btn btn-primary btn-block" type="submit" id="payButton">立即支付</button>
        </td>
     </tr>
      <tr>
     		<td>收货人</td>  
        	<td colspan="2">XXX  18812341234</td>  
     </tr>
     <tr>
     		<td>收货地址</td>  
        	<td colspan="2">北京市昌平区回龙观龙博一区</td>  
     </tr>
  </table>
</div>
</body>
</html>
<script>
function render(detail){
	var goods = detail.goods;
	var order = detail.order;
	$("#goodsName").text(goods.goodsName);
	$("#goodsImg").attr("src", goods.goodsImg);
	$("#orderPrice").text(order.goodsPrice);
	$("#createDate").text(new Date(order.createDate).format("yyyy-MM-dd hh:mm:ss"));
	var status = "";
	if(order.status == 0){
		status = "未支付"
	}else if(order.status == 1){
		status = "待发货";
	}
	$("#orderStatus").text(status);
	
}

$(function(){
	getOrderDetail();
})

function getOrderDetail(){
	var orderId = g_getQueryString("orderId");
	$.ajax({
		url:"/order/detail",
		type:"GET",
		data:{
			orderId:orderId
		},
		success:function(data){
			if(data.code == 0){
				render(data.data);
			}else{
				layer.msg(data.msg);
			}
		},
		error:function(){
			layer.msg("客户端请求有误");
		}
	});
}
</script>

```
根据订单号查找订单信息
```
@Controller
@RequestMapping("/order")
public class OrderController {

	@Autowired
	MiaoshaUserService userService;
	
	@Autowired
	RedisService redisService;
	
	@Autowired
	OrderService orderService;
	
	@Autowired
	GoodsService goodsService;
	
    @RequestMapping("/detail")
    @ResponseBody
    public Result<OrderDetailVo> info(Model model,MiaoshaUser user,
    		@RequestParam("orderId") long orderId) {
    	if(user == null) {
    		return Result.error(CodeMsg.SESSION_ERROR);
    	}
    	OrderInfo order = orderService.getOrderById(orderId);
    	if(order == null) {
    		return Result.error(CodeMsg.ORDER_NOT_EXIST);
    	}
    	long goodsId = order.getGoodsId();
    	GoodsVo goods = goodsService.getGoodsVoByGoodsId(goodsId);
    	OrderDetailVo vo = new OrderDetailVo();
    	vo.setOrder(order);
    	vo.setGoods(goods);
    	return Result.success(vo);
    }
}

```
application.properties可配置浏览器缓存
```
spring.resources.add-mappings=true
spring.resources.cache.period=3600
spring.resources.chain.cache=true 
spring.resources.chain.enabled=true
spring.resources.chain.gzipped=true
spring.resources.chain.html-application-cache=true
spring.resources.static-locations=classpath:/static/
```
###6-5超买问题
1、sql中where判断库存大于0
```
    @Update("update miaosha_goods set stock_count = stock_count - 1 where goods_id = #{goodsId} and stock_count > 0")
	public int reduceStock(MiaoshaGoods g);
```
2、虽然在controller中有getMiaoshaOrderByUserIdGoodsId（）方法判断是否已经秒杀到，但是如果两个秒杀请求同时到达，都可以穿过判断方法，并进行秒杀操作（减库存、下订单、写入秒杀订单），这样会出现重复秒杀的情况
解决办法：可以为表设置联合唯一性索引（user_id,goods_id），但是如果在普通订单表上面设置，因为在普通订单表中同一个人可以多次购买同一件商品，这样当我们重复购买普通商品时会出错。这时候秒杀订单派上用场了，当我们进行秒杀操作写订单的时候，先把数据写入普通订单表，再写入秒杀订单表，由于秒杀订单表设置了唯一性索引，报错回滚事务，普通订单表的数据也不会写进去。当我们购买普通商品时写入普通订单表即可。 （注意使用Innodb存储引擎，MyISam不支持事务）
```
CREATE TABLE `miaosha_order` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '秒杀订单ID',
  `user_id` bigint(20) DEFAULT NULL COMMENT '用户ID',
  `order_id` bigint(20) DEFAULT NULL COMMENT '订单ID',
  `goods_id` bigint(20) DEFAULT NULL COMMENT '商品ID',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `u_uid_gid` (`user_id`,`goods_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1205 DEFAULT CHARSET=utf8 ROW_FORMAT=FIXED
```

注：对象缓存小优化，在对用户是否已经秒杀了商品判断时，会访问数据库，为了减少这方面对数据库的访问，可使用Redis缓存。在我们成功秒杀了商品时，会在redis按照一定规则（userid+goodsid）存入秒杀订单对象，并设置为永不过期，当我们在对是否已经秒杀到商品时可根据上述规则取到redis缓存数据，若为空则是该用户未对该商品进行秒杀，若有数据这表示已经秒杀了该商品。
###6-6静态资源优化
除了对静态页面优化，我们还可以通过一些方式减少流量，提供访问的速度!当用户的请求越小，性能也就相对越好!
1.JS/CSS压缩，静态资源尽量使用压缩版的库和包，以减少浏览器加载和请求的流量
2. 多个js/css组合到一个请求，减少连接数（正常30个，从服务端获取，多次访问，通过http获取），把多个文件通过一个js/css一次性请求下来 配置 tengine模块实现!
3.将多个Js/css的请求合并为一个
4.CDN：内容分发网络，将数据缓存到网络节点上，用户请求来根据位置定向访问到距离最近的节点，可用于解决网络拥挤，跟代码层面关系不是很大，在请求没到网站之前，CDN会根据客户的位置将请求分发到就近地网路节点上，如果节点有则直接返回!

注意：tengine与nginx的功能基本是一样的，继承了nginx所有特性，并且拥有可以对静态资源压缩合并请求等功能，详情可参考tengine.taobaoorg ，可以参照Nginx的方式来配置Tengine!
###6-7关于缓存
对于一个项目来说，其瓶颈往往是在于数据库的瓶颈。除了业务代码的稳定性，我们所做的操作目的都是在减少数据库的压力。通过Redis页面缓存，通过浏览器缓存，通过页面静态化，通过Redis页面静态缓存，通过Redis热点数据缓存，Nginx缓存，Tengine缓存，CDN缓存等，层层拦截，以减少对数据库的访问!

但是对于数据来说，一切的缓存都是建立在不影响客户体验的基础上，缓存因为其特点，仍热存在着数据不同步的问题!

针对这个问题，能处理的只有?
定时刷新缓存，存储变化非常小的页面信息!
主动监控更新缓存！

参考：https://blog.csdn.net/qq_36505948/article/details/82620908
##7、接口优化
1、Redis预减库存减少数据库访问
2、内存标记减少Redis访问
3、请求先入队缓冲，异步下单，增强用户体验
4、RabbitMQ安装与Spring Boot集成
5、Nginx水平扩展
6、压测
###7-1安装RabbitMQ
###7-2Spring Boot集成RabbitMQ
1、添加依赖
```
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-amqp</artifactId>
    </dependency>
```
2、配置
```
spring.rabbitmq.host=127.0.0.1
spring.rabbitmq.port=5672
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest
spring.rabbitmq.virtual-host=/
#\u6D88\u8D39\u8005\u6570\u91CF
spring.rabbitmq.listener.simple.concurrency= 10
spring.rabbitmq.listener.simple.max-concurrency= 10
#\u6D88\u8D39\u8005\u6BCF\u6B21\u4ECE\u961F\u5217\u83B7\u53D6\u7684\u6D88\u606F\u6570\u91CF
spring.rabbitmq.listener.simple.prefetch= 1
#\u6D88\u8D39\u8005\u81EA\u52A8\u542F\u52A8
spring.rabbitmq.listener.simple.auto-startup=true
#\u6D88\u8D39\u5931\u8D25\uFF0C\u81EA\u52A8\u91CD\u65B0\u5165\u961F
spring.rabbitmq.listener.simple.default-requeue-rejected= true
#\u542F\u7528\u53D1\u9001\u91CD\u8BD5
spring.rabbitmq.template.retry.enabled=true 
spring.rabbitmq.template.retry.initial-interval=1000 
spring.rabbitmq.template.retry.max-attempts=3
spring.rabbitmq.template.retry.max-interval=10000
spring.rabbitmq.template.retry.multiplier=1.0
```
3、RabbitMQ四种交换机工作模式
发送方
```
@Service
public class MQSender {
    private static Logger log=LoggerFactory.getLogger(MQReceiver.class);

    @Autowired
    AmqpTemplate amqpTemplate;

	public void send(Object message) {
		String msg = RedisService.beanToString(message);
		log.info("send message:"+msg);
		amqpTemplate.convertAndSend(MQConfig.QUEUE, msg);
	}

	public void sendTopic(Object message) {
		String msg = RedisService.beanToString(message);
		log.info("send topic message:"+msg);
		amqpTemplate.convertAndSend(MQConfig.TOPIC_EXCHANGE, "topic.key1", msg+"1");
		amqpTemplate.convertAndSend(MQConfig.TOPIC_EXCHANGE, "topic.key2", msg+"2");
	}

	public void sendFanout(Object message) {
		String msg = RedisService.beanToString(message);
		log.info("send fanout message:"+msg);
		amqpTemplate.convertAndSend(MQConfig.FANOUT_EXCHANGE, "", msg);
	}

	public void sendHeader(Object message) {
		String msg = RedisService.beanToString(message);
		log.info("send fanout message:"+msg);
		MessageProperties properties = new MessageProperties();
		properties.setHeader("header1", "value1");
		properties.setHeader("header2", "value2");
		Message obj = new Message(msg.getBytes(), properties);
		amqpTemplate.convertAndSend(MQConfig.HEADERS_EXCHANGE, "", obj);
	}
}
```
MQConfig
```
@Configuration
public class MQConfig {

    public static final String MIAOSHA_QUEUE = "miaosha.queue";
    public static final String QUEUE = "queue";
    public static final String TOPIC_QUEUE1 = "topic.queue1";
    public static final String TOPIC_QUEUE2 = "topic.queue2";
    public static final String HEADER_QUEUE = "header.queue";
    public static final String TOPIC_EXCHANGE = "topicExchage";
    public static final String FANOUT_EXCHANGE = "fanoutxchage";
    public static final String HEADERS_EXCHANGE = "headersExchage";

    /**
     * Direct模式
     */
    @Bean
    public Queue queue(){
        return new Queue(QUEUE,true);
    }

    /**
     * Topic模式 交换机Exchange
     * */
    @Bean
    public Queue topicQueue1() {
        return new Queue(TOPIC_QUEUE1, true);
    }
    @Bean
    public Queue topicQueue2() {
        return new Queue(TOPIC_QUEUE2, true);
    }
    @Bean
    public TopicExchange topicExchage(){
        return new TopicExchange(TOPIC_EXCHANGE);
    }
    @Bean
    public Binding topicBinding1() {
        return BindingBuilder.bind(topicQueue1()).to(topicExchage()).with("topic.key1");
    }
    @Bean
    public Binding topicBinding2() {
        return BindingBuilder.bind(topicQueue2()).to(topicExchage()).with("topic.#");
    }

    /**
     * Fanout模式 交换机Exchange
     * */
    @Bean
    public FanoutExchange fanoutExchage(){
        return new FanoutExchange(FANOUT_EXCHANGE);
    }
    @Bean
    public Binding FanoutBinding1() {
        return BindingBuilder.bind(topicQueue1()).to(fanoutExchage());
    }
    @Bean
    public Binding FanoutBinding2() {
        return BindingBuilder.bind(topicQueue2()).to(fanoutExchage());
    }

    /**
     * Header模式 交换机Exchange
     * */
    @Bean
    public HeadersExchange headersExchage(){
        return new HeadersExchange(HEADERS_EXCHANGE);
    }
    @Bean
    public Queue headerQueue1() {
        return new Queue(HEADER_QUEUE, true);
    }
    @Bean
    public Binding headerBinding() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("header1", "value1");
        map.put("header2", "value2");
        return BindingBuilder.bind(headerQueue1()).to(headersExchage()).whereAll(map).match();
    }
}

```
接收方
```
@Service
public class MQReceiver {

    @Autowired
    MiaoshaUserService userService;

    @Autowired
    RedisService redisService;

    @Autowired
    GoodsService goodsService;

    @Autowired
    MiaoshaService miaoshaService;

    @Autowired
    OrderService orderService;

    private static Logger log=LoggerFactory.getLogger(MQReceiver.class);

    @RabbitListener(queues=MQConfig.QUEUE)
		public void receive(String message) {
			log.info("receive message:"+message);
		}

    @RabbitListener(queues=MQConfig.TOPIC_QUEUE1)
    public void receiveTopic1(String message) {
        log.info(" topic  queue1 message:"+message);
    }

    @RabbitListener(queues=MQConfig.TOPIC_QUEUE2)
    public void receiveTopic2(String message) {
        log.info(" topic  queue2 message:"+message);
    }


    @RabbitListener(queues=MQConfig.HEADER_QUEUE)
		public void receiveHeaderQueue(byte[] message) {
			log.info(" header  queue message:"+new String(message));
		}
}

```
测试
```
@Controller
@RequestMapping("/demo")
public class SampleController {

	@Autowired
    UserService userService;
	
	@Autowired
    RedisService redisService;

	@Autowired
    MQSender mqSender;

	@RequestMapping("/mq/header")
    @ResponseBody
    public Result<String> header() {
		mqSender.sendHeader("hello,world");
        return Result.success("Hello，world");
    }

	@RequestMapping("/mq/fanout")
    @ResponseBody
    public Result<String> fanout() {
		mqSender.sendFanout("hello,world");
        return Result.success("Hello，world");
    }

	@RequestMapping("/mq/topic")
    @ResponseBody
    public Result<String> topic() {
		mqSender.sendTopic("hello,world");
        return Result.success("Hello，world");
    }

	@RequestMapping("/mq")
    @ResponseBody
    public Result<String> mq() {
		mqSender.send("hello,world");
        return Result.success("Hello，world");
    }
}

```
###7-3秒杀接口优化
思路：减少数据库访问
1、系统初始化，把商品库存数量加载到Redis
2、收到请求，Redis预减库存，库存不足，直接返回，否则进入3
3、请求入队，立即返回排队中
4、请求出队，生成订单，减少库存
5、客户端轮询，是否秒杀成功
QPS:2114
```
@Controller
@RequestMapping("/miaosha")
public class MiaoshaController implements InitializingBean {

    @Autowired
    GoodsService goodsService;

    @Autowired
    MiaoshaService miaoshaService;

    @Autowired
    OrderService orderService;

    @Autowired
    RedisService redisService;

    @Autowired
    MQSender mqSender;

    private HashMap<Long,Boolean> localOverMap = new HashMap<>();


    /**
     * 系统初始化
     * 预加载秒杀商品数量
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {

        List<GoodsVo> goodsVoList = goodsService.listGoodsVo();
        if(goodsVoList == null){
            return;
        }
        for(GoodsVo goodsVo : goodsVoList){
            redisService.set(GoodsKey.getMiaoshaGoodsStock,""+goodsVo.getId(),goodsVo.getStockCount());
            localOverMap.put(goodsVo.getId(),false);
        }
    }


    /**
     * 秒杀
     * @param model
     * @param user
     * @param goodsId
     * @return
     */
    @RequestMapping(value="/do_miaosha", method= RequestMethod.POST)
    @ResponseBody
    public Result<Integer> miaosha(Model model, MiaoshaUser user,
                                     @RequestParam("goodsId")long goodsId) {
        model.addAttribute("user", user);
        if(user == null) {
            return Result.error(CodeMsg.SESSION_ERROR);
        }

        //内存标记，减少redis访问
        boolean over = localOverMap.get(goodsId);
        if(over){
            return Result.error(CodeMsg.MIAO_SHA_OVER);
        }

        //预减库存
        Long stock = redisService.decr(GoodsKey.getMiaoshaGoodsStock,""+goodsId);
        if(stock < 0){
            localOverMap.put(goodsId,true);
            return Result.error(CodeMsg.MIAO_SHA_OVER);
        }

        //判断是否已经秒杀到了
        MiaoshaOrder order = orderService.getMiaoshaOrderByUserIdGoodsId(user.getId(),goodsId);
        if(order != null){
            return Result.error(CodeMsg.REPEATE_MIAOSHA);
        }

        //入队
        MiaoshaMessage mm = new MiaoshaMessage();
        mm.setUser(user);
        mm.setGoodsid(goodsId);
        mqSender.sendMiaoshaMessage(mm);
        return Result.success(0);


//        //判断库存
//        GoodsVo goods = goodsService.getGoodsVoByGoodsId(goodsId);//10个商品，req1 req2
//        int stock = goods.getStockCount();
//        if(stock <= 0) {
//            return Result.error(CodeMsg.MIAO_SHA_OVER);
//        }
//        //判断是否已经秒杀到了
//        MiaoshaOrder order = orderService.getMiaoshaOrderByUserIdGoodsId(user.getId(), goodsId);
//        if(order != null) {
//            return Result.error(CodeMsg.REPEATE_MIAOSHA);
//        }
//        //减库存 下订单 写入秒杀订单
//        OrderInfo orderInfo = miaoshaService.miaosha(user, goods);
//        return Result.success(orderInfo);
    }

    /**
     * @param model
     * @param user
     * @param goodsId
     * @return
     *
     * orderId：成功
     * -1：秒杀失败
     * 0：排队中
     */
    @RequestMapping(value="/result", method=RequestMethod.GET)
    @ResponseBody
    public Result<Long> miaoshaResult(Model model,MiaoshaUser user,
                                      @RequestParam("goodsId")long goodsId) {
        model.addAttribute("user", user);
        if(user == null) {
            return Result.error(CodeMsg.SESSION_ERROR);
        }
        long result  =miaoshaService.getMiaoshaResult(user.getId(), goodsId);
        return Result.success(result);
    }


   /* @RequestMapping("/do_miaosha")
    public String list(Model model, MiaoshaUser user,
                       @RequestParam("goodsId")long goodsId) {
        model.addAttribute("user", user);
        if(user == null) {
            return "login";
        }
        //判断库存
        GoodsVo goods = goodsService.getGoodsVoByGoodsId(goodsId);
        int stock = goods.getStockCount();
        if(stock <= 0) {
            model.addAttribute("errmsg", CodeMsg.MIAO_SHA_OVER.getMsg());
            return "miaosha_fail";
        }
        //判断是否已经秒杀到了
        MiaoshaOrder order = orderService.getMiaoshaOrderByUserIdGoodsId(user.getId(), goodsId);
        if(order != null) {
            model.addAttribute("errmsg", CodeMsg.REPEATE_MIAOSHA.getMsg());
            return "miaosha_fail";
        }
        //减库存 下订单 写入秒杀订单
        OrderInfo orderInfo = miaoshaService.miaosha(user, goods);
        model.addAttribute("orderInfo", orderInfo);
        model.addAttribute("goods", goods);
        return "order_detail";
    }*/
}

```
```
@Service
public class MiaoshaService {
	
	@Autowired
	GoodsService goodsService;
	
	@Autowired
	OrderService orderService;

	@Autowired
	RedisService redisService;

	/**
	 * 进行秒杀操作
	 * @param user
	 * @param goods
	 * @return
	 */
	@Transactional
	public OrderInfo miaosha(MiaoshaUser user, GoodsVo goods) {
		//减库存 下订单 写入秒杀订单
		boolean success = goodsService.reduceStock(goods);
		if(success){	//减库存成功
			//order_info maiosha_order
			OrderInfo orderInfo = orderService.createOrder(user, goods);
			return orderInfo;
		}else {			//减库存失败
			setGoodsOver(goods.getId());	//在redis中标记该商品已经没有库存
			return null;
		}
	}

	/**
	 * 得到秒杀结果
	 * @param userId
	 * @param goodsId
	 * @return
	 */
	public long getMiaoshaResult(Long userId, long goodsId) {
		MiaoshaOrder order = orderService.getMiaoshaOrderByUserIdGoodsId(userId,goodsId);
		if(order != null){	//秒杀成功
			return order.getOrderId();
		}else {
			boolean isOver = getGoodsOver(goodsId);
			if (isOver){			//没有库存
				return -1;
			}else {					//还在排队中
				return 0;
			}
		}
	}

	private void setGoodsOver(Long goodsId) {
		redisService.set(MiaoshaKey.isGoodsOver, ""+goodsId, true);
	}

	private boolean getGoodsOver(long goodsId) {
		return redisService.exists(MiaoshaKey.isGoodsOver,""+goodsId);
	}
}

```
```
@Configuration
public class MQConfig {

    public static final String MIAOSHA_QUEUE = "miaosha.queue";
    public static final String QUEUE = "queue";
    public static final String TOPIC_QUEUE1 = "topic.queue1";
    public static final String TOPIC_QUEUE2 = "topic.queue2";
    public static final String HEADER_QUEUE = "header.queue";
    public static final String TOPIC_EXCHANGE = "topicExchage";
    public static final String FANOUT_EXCHANGE = "fanoutxchage";
    public static final String HEADERS_EXCHANGE = "headersExchage";


    @Bean
    public Queue queue(){
        return new Queue(MIAOSHA_QUEUE,true);
    }

```
```
public class MiaoshaMessage {
    private MiaoshaUser user;
    private long goodsid;


    public MiaoshaUser getUser() {
        return user;
    }

    public void setUser(MiaoshaUser user) {
        this.user = user;
    }

    public long getGoodsid() {
        return goodsid;
    }

    public void setGoodsid(long goodsid) {
        this.goodsid = goodsid;
    }
}
```
```
@Service
public class MQSender {
    private static Logger log=LoggerFactory.getLogger(MQReceiver.class);

    @Autowired
    AmqpTemplate amqpTemplate;

    public void sendMiaoshaMessage(MiaoshaMessage mm) {
        String msg = RedisService.beanToString(mm);
        log.info("send message:"+msg);
		amqpTemplate.convertAndSend(MQConfig.MIAOSHA_QUEUE, msg);
    }

```
```
@Service
public class MQReceiver {

    @Autowired
    MiaoshaUserService userService;

    @Autowired
    RedisService redisService;

    @Autowired
    GoodsService goodsService;

    @Autowired
    MiaoshaService miaoshaService;

    @Autowired
    OrderService orderService;

    private static Logger log=LoggerFactory.getLogger(MQReceiver.class);

    @RabbitListener(queues = MQConfig.MIAOSHA_QUEUE)
    public void receive(String message){
        log.info("receive message:"+message );
        MiaoshaMessage mm = RedisService.stringToBean(message, MiaoshaMessage.class);
        MiaoshaUser user = mm.getUser();
        long goodsid = mm.getGoodsid();

        //判断库存
        GoodsVo goods = goodsService.getGoodsVoByGoodsId(goodsid);//10个商品，req1 req2
    	int stock = goods.getStockCount();
    	if(stock <= 0) {
    		return ;
    	}

        // 判断是否已经秒杀到了
    	MiaoshaOrder order = orderService.getMiaoshaOrderByUserIdGoodsId(user.getId(), goodsid);
    	if(order != null) {
    		return ;
    	}

        //减库存 下订单 写入秒杀订单
        miaoshaService.miaosha(user, goods);
    }
```
```
@Controller
@RequestMapping("/order")
public class OrderController {

	@Autowired
	MiaoshaUserService userService;
	
	@Autowired
	RedisService redisService;
	
	@Autowired
	OrderService orderService;
	
	@Autowired
	GoodsService goodsService;
	
    @RequestMapping("/detail")
    @ResponseBody
    public Result<OrderDetailVo> info(Model model, MiaoshaUser user,
									  @RequestParam("orderId") long orderId) {
    	if(user == null) {
    		return Result.error(CodeMsg.SESSION_ERROR);
    	}
    	OrderInfo order = orderService.getOrderById(orderId);
    	if(order == null) {
    		return Result.error(CodeMsg.ORDER_NOT_EXIST);
    	}
    	long goodsId = order.getGoodsId();
    	GoodsVo goods = goodsService.getGoodsVoByGoodsId(goodsId);
    	OrderDetailVo vo = new OrderDetailVo();
    	vo.setOrder(order);
    	vo.setGoods(goods);
    	return Result.success(vo);
    }
    
}

```
##8、安全优化
###8-1隐藏秒杀地址
思路：秒杀开始之前，先去请求接口获取秒杀地址
1、接口改造，带上PathVariable参数
2、添加生成地址的接口
3、秒杀收到请求，先验证PathVariable
```
function getMiaoshaPath(){
	var goodsId = $("#goodsId").val();
	g_showLoading();
	$.ajax({
		url:"/miaosha/path",
		type:"GET",
		data:{
			goodsId:goodsId,
			verifyCode:$("#verifyCode").val()
		},
		success:function(data){
			if(data.code == 0){
				var path = data.data;
				doMiaosha(path);
			}else{
				layer.msg(data.msg);
			}
		},
		error:function(){
			layer.msg("客户端请求有误");
		}
	});
}

function doMiaosha(path){
	$.ajax({
		url:"/miaosha/"+path+"/do_miaosha",
		type:"POST",
		data:{
			goodsId:$("#goodsId").val()
		},
		success:function(data){
			if(data.code == 0){
				//window.location.href="/order_detail.htm?orderId="+data.data.id;
				getMiaoshaResult($("#goodsId").val());
			}else{
				layer.msg(data.msg);
			}
		},
		error:function(){
			layer.msg("客户端请求有误");
		}
	});
	
}
```
```
    @RequestMapping(value = "/path",method = RequestMethod.GET)
    @ResponseBody
    public Result<String> getMiaoShaPath(HttpServletRequest request, MiaoshaUser user, @RequestParam("goodsId") long goodsId,
                                         @RequestParam(value="verifyCode", defaultValue="0") int verifyCode){
        if (user==null){
            return Result.error(CodeMsg.SESSION_ERROR);
        }
        //查看验证码是否正确
        boolean check=miaoshaService.checkVerifyCode(user,goodsId,verifyCode);
        if (!check){
            return Result.error(CodeMsg.REQUEST_ILLEGAL);
        }
        //获取path 一个user对应一个path  用于请求秒杀接口时验证
        String path=miaoshaService.createMiaoShaPath(user,goodsId);
        return Result.success(path);
    }
    
    
    @RequestMapping(value="/{path}/do_miaosha", method= RequestMethod.POST)
    @ResponseBody
    public Result<Integer> miaosha(Model model, MiaoshaUser user,
                                   @RequestParam("goodsId")long goodsId,
                                   @PathVariable("path") String path) {
        model.addAttribute("user", user);
        if(user == null) {
            return Result.error(CodeMsg.SESSION_ERROR);
        }

        //验证path
        boolean check = miaoshaService.checkPath(user,goodsId,path);
        if(!check){
            return Result.error(CodeMsg.REQUEST_ILLEGAL);
        }

        //内存标记，减少redis访问
        boolean over = localOverMap.get(goodsId);
        if(over){
            return Result.error(CodeMsg.MIAO_SHA_OVER);
        }

        //预减库存
        Long stock = redisService.decr(GoodsKey.getMiaoshaGoodsStock,""+goodsId);
        if(stock < 0){
            localOverMap.put(goodsId,true);
            return Result.error(CodeMsg.MIAO_SHA_OVER);
        }

        //判断是否已经秒杀到了
        MiaoshaOrder order = orderService.getMiaoshaOrderByUserIdGoodsId(user.getId(),goodsId);
        if(order != null){
            return Result.error(CodeMsg.REPEATE_MIAOSHA);
        }

        //入队
        MiaoshaMessage mm = new MiaoshaMessage();
        mm.setUser(user);
        mm.setGoodsid(goodsId);
        mqSender.sendMiaoshaMessage(mm);
        return Result.success(0);
    }
```
```
     /**
	 * 根据userId、goodsId 创建隐藏路径
	 * @param user
	 * @param goodsId
	 * @return
	 */
	public String createMiaoShaPath(MiaoshaUser user, long goodsId) {
		if(user == null || goodsId < 0){
			return null;
		}
		String str = MD5Util.md5(UUIDUtil.uuid() + "123456");
		redisService.set(MiaoshaKey.getMiaoshaPath,""+user.getId()+"_"+goodsId,str);
		return str;
	}
	
	/**
	 * 检查隐藏路径是否正确
	 * @param user
	 * @param goodsId
	 * @param path
	 * @return
	 */
	public boolean checkPath(MiaoshaUser user, long goodsId, String path) {
		if(user == null || path == null){
			return false;
		}
		String pathOld = redisService.get(MiaoshaKey.getMiaoshaPath,""+user.getId()+"_"+goodsId,String.class);
		return path.equals(pathOld);
	}
```
###8-2图形验证码
思路：点击秒杀之前，先输入验证码，分散用户的请求，防止机器人
1、添加生成验证码的接口
2、在获取秒杀路径的时候，验证验证码
3、ScripEngine使用
```
function refreshVerifyCode(){
	$("#verifyCodeImg").attr("src", "/miaosha/verifyCode?goodsId="+$("#goodsId").val()+"&timestamp="+new Date().getTime());
}
```
```
    @RequestMapping(value="/verifyCode", method=RequestMethod.GET)
    @ResponseBody
    public Result<String> getMiaoshaVerifyCod(HttpServletResponse response, MiaoshaUser user,
                                              @RequestParam("goodsId")long goodsId) {
        if(user == null) {
            return Result.error(CodeMsg.SESSION_ERROR);
        }
        try {
            BufferedImage image  = miaoshaService.createVerifyCode(user, goodsId);
            OutputStream out = response.getOutputStream();
            ImageIO.write(image, "JPEG", out);
            out.flush();
            out.close();
            return null;
        }catch(Exception e) {
            e.printStackTrace();
            return Result.error(CodeMsg.MIAOSHA_FAIL);
        }
```
```
    /**
	 * 获取验证码
	 * @param user
	 * @param goodsId
	 * @return
	 */
	public BufferedImage createVerifyCode(MiaoshaUser user, long goodsId) {
		if(user == null || goodsId <=0) {
			return null;
		}
		int width = 80;
		int height = 32;
		//create the image
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics g = image.getGraphics();
		// set the background color
		g.setColor(new Color(0xDCDCDC));
		g.fillRect(0, 0, width, height);
		// draw the border
		g.setColor(Color.black);
		g.drawRect(0, 0, width - 1, height - 1);
		// create a random instance to generate the codes
		Random rdm = new Random();
		// make some confusion
		for (int i = 0; i < 50; i++) {
			int x = rdm.nextInt(width);
			int y = rdm.nextInt(height);
			g.drawOval(x, y, 0, 0);
		}
		// generate a random code
		String verifyCode = generateVerifyCode(rdm);
		g.setColor(new Color(0, 100, 0));
		g.setFont(new Font("Candara", Font.BOLD, 24));
		g.drawString(verifyCode, 8, 24);
		g.dispose();
		//把验证码存到redis中
		int rnd = calc(verifyCode);
		redisService.set(MiaoshaKey.getMiaoshaVerifyCode, user.getId()+","+goodsId, rnd);
		//输出图片
		return image;
	}

	/**
	 * 检查验证码
	 * @param user
	 * @param goodsId
	 * @param verifyCode
	 * @return
	 */
	public boolean checkVerifyCode(MiaoshaUser user, long goodsId, int verifyCode) {
		if(user == null || goodsId <=0) {
			return false;
		}
		Integer codeOld = redisService.get(MiaoshaKey.getMiaoshaVerifyCode, user.getId()+","+goodsId, Integer.class);
		if(codeOld == null || codeOld - verifyCode != 0 ) {
			return false;
		}
		redisService.delete(MiaoshaKey.getMiaoshaVerifyCode, user.getId()+","+goodsId);
		return true;
	}

	/**
	 * 利用ScriptEngine 进行字符串数学运算
	 * @param exp
	 * @return
	 */
	private static int calc(String exp) {
		try {
			ScriptEngineManager manager = new ScriptEngineManager();
			ScriptEngine engine = manager.getEngineByName("JavaScript");
			return (Integer)engine.eval(exp);
		}catch(Exception e) {
			e.printStackTrace();
			return 0;
		}
	}

	private static char[] ops = new char[] {'+', '-', '*'};

	/** 生成数学图形验证码字符串
	 * + - *
	 * */
	private String generateVerifyCode(Random rdm) {
		int num1 = rdm.nextInt(10);
		int num2 = rdm.nextInt(10);
		int num3 = rdm.nextInt(10);
		char op1 = ops[rdm.nextInt(3)];
		char op2 = ops[rdm.nextInt(3)];
		String exp = ""+ num1 + op1 + num2 + op2 + num3;
		return exp;
	}
```

###8-3接口限流防刷
思路：利用redis存储数据的有效期，根据userId和请求url作为键，值自增，当值超过指定值的时候可认为用户在指定时间内对某个url访问超过指定值，return。

直接在Controller中进行限流会对业务代码的入侵，可以利用拦截器减少对业务侵入

通过拦截器对方法进行拦截，判断方法是否有@AccessLimit注解，有就进行判断处理，没有就放行返回true
```
    @AccessLimit(seconds=5, maxCount=5, needLogin=true)
    @RequestMapping(value = "/path",method = RequestMethod.GET)
    @ResponseBody
    public Result<String> getMiaoShaPath(HttpServletRequest request, MiaoshaUser user,
                                         @RequestParam("goodsId") long goodsId,
                                         @RequestParam(value="verifyCode", defaultValue="0") int verifyCode){
        if (user==null){
            return Result.error(CodeMsg.SESSION_ERROR);
        }

        //查看验证码是否正确
        boolean check=miaoshaService.checkVerifyCode(user,goodsId,verifyCode);
        if (!check){
            return Result.error(CodeMsg.REQUEST_ILLEGAL);
        }

        //获取path 一个user对应一个path  用于请求秒杀接口时验证
        String path=miaoshaService.createMiaoShaPath(user,goodsId);
        return Result.success(path);
    }

```
```
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AccessLimit {
    int seconds();
    int maxCount();
    boolean needLogin() default true;
}
```
```
**
 * 实现拦截器，在方法之前进行拦截
 */

@Service
public class AccessInterceptor extends HandlerInterceptorAdapter {

    @Autowired
    MiaoshaUserService miaoshaUserService;

    @Autowired
    RedisService redisService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if(handler instanceof HandlerMethod){
            MiaoshaUser user = getUser(request,response);
            UserContext.setUser(user);
            HandlerMethod hm = (HandlerMethod)handler;
            AccessLimit accessLimit = hm.getMethodAnnotation(AccessLimit.class);
            if(accessLimit == null) {
                return true;
            }
            int seconds = accessLimit.seconds();
            int maxCount = accessLimit.maxCount();
            boolean needLogin = accessLimit.needLogin();
            String key = request.getRequestURI();
            if(needLogin){
                if(user == null){
                    render(response, CodeMsg.SESSION_ERROR);
                    return false;
                }
                key += "_" + user.getId();
            }else {
                //do nothing
            }
            AccessKey accessKey = AccessKey.withExpire(seconds);
            Integer count = redisService.get(accessKey,key,Integer.class);
            if(count == null){
                redisService.set(accessKey,key,1);          // TODO 【bug】 不需要登录的时候直接以URI 作为key，那么这个key 会被所有用户共享，造成指定时间内所有用户访问总数超过特定值就会开启防刷措施
            }else if(count < maxCount){
                redisService.incr(accessKey,key);
            }else {
                render(response,CodeMsg.ACCESS_LIMIT_REACHED);
                return false;
            }
        }
        return true;
    }

    /**
     * 通过OutputStream返回数据给前台ajax处理
     * @param response
     * @param cm
     * @throws Exception
     */
    private void render(HttpServletResponse response, CodeMsg cm)throws Exception {
        response.setContentType("application/json;charset=UTF-8");
        OutputStream out = response.getOutputStream();
        String str  = JSON.toJSONString(Result.error(cm));
        out.write(str.getBytes("UTF-8"));
        out.flush();
        out.close();
    }

    /**
     * 获取用户登录信息
     * @param request
     * @param response
     * @return
     */
    private MiaoshaUser getUser(HttpServletRequest request, HttpServletResponse response) {
        String paramToken = request.getParameter(MiaoshaUserService.COOKIE_NAME_TOKEN);
        String cookieToken = getCookieValue(request, MiaoshaUserService.COOKIE_NAME_TOKEN);
        if(StringUtils.isEmpty(cookieToken) && StringUtils.isEmpty(paramToken)) {
            return null;
        }
        String token = StringUtils.isEmpty(paramToken)?cookieToken:paramToken;
        return miaoshaUserService.getByToken(response, token);
    }

    private String getCookieValue(HttpServletRequest request, String cookiName) {
        Cookie[]  cookies = request.getCookies();
        if(cookies == null || cookies.length <= 0){
            return null;
        }
        for(Cookie cookie : cookies) {
            if(cookie.getName().equals(cookiName)) {
                return cookie.getValue();
            }
        }
        return null;
    }
}

```
```
@Configuration
public class WebConfig extends WebMvcConfigurerAdapter {

    @Autowired
    UserArgumentResolver userArgumentResolver;

    @Autowired
    AccessInterceptor accessInterceptor;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        argumentResolvers.add(userArgumentResolver);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(accessInterceptor);
    }
}
```
```
public class UserContext {

    private static ThreadLocal<MiaoshaUser> userHolder = new ThreadLocal<>();

    public static void setUser(MiaoshaUser user){
        userHolder.set(user);
    }

    public static MiaoshaUser getUser() {
        return userHolder.get();
    }
}
```
上述对获取用户登录信息进行了封装，并放在ThreadLocal变量中，这样在Controller中有MiaoshaUser参数时就可以直接获取ThreadLocal的值，注入参数对象
```
@Service
public class UserArgumentResolver implements HandlerMethodArgumentResolver {

    @Autowired
    MiaoshaUserService userService;

    public boolean supportsParameter(MethodParameter parameter) {
        Class<?> clazz = parameter.getParameterType();
        return clazz== MiaoshaUser.class;
    }

    @Override
    public Object resolveArgument(MethodParameter methodParameter, ModelAndViewContainer modelAndViewContainer, NativeWebRequest nativeWebRequest, WebDataBinderFactory webDataBinderFactory) throws Exception {
        return UserContext.getUser();
    }

    /*public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        HttpServletResponse response = webRequest.getNativeResponse(HttpServletResponse.class);

        String paramToken = request.getParameter(MiaoshaUserService.COOKIE_NAME_TOKEN);
        String cookieToken = getCookieValue(request, MiaoshaUserService.COOKIE_NAME_TOKEN);
        if(StringUtils.isEmpty(cookieToken) && StringUtils.isEmpty(paramToken)) {
            return null;
        }
        String token = StringUtils.isEmpty(paramToken)?cookieToken:paramToken;
        return userService.getByToken(response, token);
    }

    private String getCookieValue(HttpServletRequest request, String cookiName) {
        Cookie[]  cookies = request.getCookies();
        if(cookies == null || cookies.length <= 0){
            return null;
        }
        for(Cookie cookie : cookies) {
            if(cookie.getName().equals(cookiName)) {
                return cookie.getValue();
            }
        }
        return null;
    }
*/
}

```

  [1]: http://pr4gg6olg.bkt.clouddn.com/%E7%A7%92%E6%9D%80%E4%BC%98%E5%8C%96%E6%96%B9%E6%A1%882-1key%E5%B0%81%E8%A3%85.png