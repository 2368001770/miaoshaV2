package com.czj.miaoshaV2.redis;

public class MiaoshaUserKey extends BasePrefix{

    public static final int TOKEN_EXPIRE = 3600*24 * 2;

    public MiaoshaUserKey(int expireSeconds, String prefix) {
        super(expireSeconds, prefix);
    }

    public static MiaoshaUserKey token = new MiaoshaUserKey(TOKEN_EXPIRE,"tk");

    public static MiaoshaUserKey getById = new MiaoshaUserKey(TOKEN_EXPIRE,"id");
}
