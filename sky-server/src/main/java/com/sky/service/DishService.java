package com.sky.service;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.vo.DishVO;

import java.util.List;

public interface DishService {
    /**
     * 新增菜品和对应的口味
     */
    void savewithFlavor(DishDTO dishDTO);

    /**
     * 菜品分页查询
     */
    PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO);

    /**
     * 条件查询菜品和口味
     */
    List<DishVO> listWithFlavor(Dish dish);

    /**
     * 批量删除菜品
     */
    void deleteBatch(List<Long> ids);

    /**
     * 菜品起售或停售
     */
    void startOrStop(Integer status, Long id);

    /**
     * 根据id查询菜品及其口味
     */
    DishVO getByIdWithFlavor(Long id);

    /**
     * 根据分类id查询菜品（带缓存）
     */
    List<Dish> list(Long categoryId);

    /**
     * 修改菜品和口味
     */
    void updateWithFlavor(DishDTO dishDTO);
}
