package com.czj.miaoshaV2.service;

import com.czj.miaoshaV2.dao.GoodsDao;
import com.czj.miaoshaV2.domain.MiaoshaGoods;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

	@Autowired
	GoodsDao goodsDao;

	@Autowired
	GoodsService goodsService;

	/**
	 * 测试事务
	 * @return
	 */
	@Transactional
	public boolean tx1(){
		MiaoshaGoods miaoshaGoods = new MiaoshaGoods();
		miaoshaGoods.setGoodsId((long)1);
		tx2();
		goodsDao.reduceStock(miaoshaGoods);
		return true;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public boolean tx2(){
		MiaoshaGoods miaoshaGoods = new MiaoshaGoods();
		miaoshaGoods.setGoodsId((long)5);
		goodsDao.reduceStock(miaoshaGoods);
		int a = 1/0;
		return true;
	}

}
