package com.czj.miaoshaV2.controller;

import com.czj.miaoshaV2.domain.MiaoshaUser;
import com.czj.miaoshaV2.domain.OrderInfo;
import com.czj.miaoshaV2.redis.RedisService;
import com.czj.miaoshaV2.result.CodeMsg;
import com.czj.miaoshaV2.result.Result;
import com.czj.miaoshaV2.service.GoodsService;
import com.czj.miaoshaV2.service.MiaoshaUserService;
import com.czj.miaoshaV2.service.OrderService;
import com.czj.miaoshaV2.vo.GoodsVo;
import com.czj.miaoshaV2.vo.OrderDetailVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;



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
