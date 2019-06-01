package com.czj.miaoshaV2.controller;


import com.czj.miaoshaV2.rabbitmq.MQSender;
import com.czj.miaoshaV2.redis.RedisService;
import com.czj.miaoshaV2.result.CodeMsg;
import com.czj.miaoshaV2.result.Result;
import com.czj.miaoshaV2.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/demo")
public class SampleController {

	@Autowired
    UserService userService;
	
	@Autowired
    RedisService redisService;

	@Autowired
    MQSender mqSender;

	/*@RequestMapping("/mq/header")
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
    }*/


    @RequestMapping("/hello")
    @ResponseBody
    public Result<String> home() {
        return Result.success("Hello，world");
    }
    
    @RequestMapping("/error")
    @ResponseBody
    public Result<String> error() {
        return Result.error(CodeMsg.SESSION_ERROR);
    }

    
}
