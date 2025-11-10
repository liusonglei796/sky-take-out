package com.sky.service;

import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;

import java.util.List;

public interface ShoppingCartService {
    void addShoppingCart(ShoppingCartDTO shoppingCartDTO);

    //查询购物车列表
    List<ShoppingCart> showShoppingCart();
    //清空购物车
    void cleanShoppingCart();
}
