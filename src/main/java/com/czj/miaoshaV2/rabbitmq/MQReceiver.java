package com.czj.miaoshaV2.rabbitmq;

import com.czj.miaoshaV2.domain.MiaoshaOrder;
import com.czj.miaoshaV2.domain.MiaoshaUser;
import com.czj.miaoshaV2.redis.RedisService;
import com.czj.miaoshaV2.service.GoodsService;
import com.czj.miaoshaV2.service.MiaoshaService;
import com.czj.miaoshaV2.service.MiaoshaUserService;
import com.czj.miaoshaV2.service.OrderService;
import com.czj.miaoshaV2.vo.GoodsVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    /*@RabbitListener(queues=MQConfig.QUEUE)
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
		}*/
}
