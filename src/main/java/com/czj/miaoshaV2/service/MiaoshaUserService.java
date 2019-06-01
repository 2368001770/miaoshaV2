package com.czj.miaoshaV2.service;

import com.czj.miaoshaV2.dao.MiaoshaUserDao;
import com.czj.miaoshaV2.domain.MiaoshaUser;
import com.czj.miaoshaV2.exception.GlobalException;
import com.czj.miaoshaV2.redis.MiaoshaUserKey;
import com.czj.miaoshaV2.redis.RedisService;
import com.czj.miaoshaV2.result.CodeMsg;
import com.czj.miaoshaV2.util.MD5Util;
import com.czj.miaoshaV2.util.UUIDUtil;
import com.czj.miaoshaV2.vo.LoginVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

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
