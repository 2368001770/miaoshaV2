package com.czj.miaoshaV2.redis;

public interface KeyPrefix {

    public int expireSeconds();

    public String getPrefix();
}
