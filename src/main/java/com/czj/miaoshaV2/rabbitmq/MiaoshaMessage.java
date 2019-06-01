package com.czj.miaoshaV2.rabbitmq;


import com.czj.miaoshaV2.domain.MiaoshaUser;

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
