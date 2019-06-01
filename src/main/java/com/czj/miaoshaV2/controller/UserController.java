package com.czj.miaoshaV2.controller;

import com.czj.miaoshaV2.domain.MiaoshaUser;
import com.czj.miaoshaV2.redis.RedisService;
import com.czj.miaoshaV2.result.Result;
import com.czj.miaoshaV2.service.MiaoshaUserService;
import com.czj.miaoshaV2.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;

@Controller
@RequestMapping("/user")
public class UserController {

	@Autowired
    MiaoshaUserService userService;
	
	@Autowired
    RedisService redisService;

	@Autowired
    UserService userTxService;

	
    @RequestMapping("/info")
    @ResponseBody
    public Result<MiaoshaUser> info(Model model, MiaoshaUser user) {
        return Result.success(user);
    }

    @RequestMapping("/test_login")
    @ResponseBody
    public String testLogin(Model model, MiaoshaUser user) {
        if(user == null){
            return "您还没有登录";
        }
        return user.toString();
    }

    /**
     * 测试
     * @param response
     * @param model
     * @param cookieToken
     * @param paramToken
     * @return
     */
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

    /**
     * 测试事务（Myisam不支持事务）
     * @return
     */
    @RequestMapping("/tx")
    @ResponseBody
    public String testTx(){
        userTxService.tx1();
        return "success";
    }
    
}
