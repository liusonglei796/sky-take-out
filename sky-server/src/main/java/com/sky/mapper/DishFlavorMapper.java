package com.sky.mapper;

import com.sky.entity.DishFlavor;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface DishFlavorMapper {
    /**
     * 批量插入口味数据
     */
    void insertBatch(List<DishFlavor> dishFlavors);
    
    /**
     * 根据菜品id查询对应的口味数据
     */
    @Select("select * from dish_flavor where dish_id = #{dishId}")
    List<DishFlavor> getByDishId(Long id);
    
    /**
     * 批量查询多个菜品的口味数据 - 解决 N+1 查询问题
     */
    @Select("<script>"
            + "select * from dish_flavor where dish_id in "
            + "<foreach item='id' collection='dishIds' open='(' separator=',' close=')'>"
            + "#{id}"
            + "</foreach>"
            + "</script>")
    List<DishFlavor> getFlavorsByDishIds(List<Long> dishIds);
    
    /**
     * 根据菜品id删除对应的口味数据
     */
    @Delete("delete from dish_flavor where dish_id = #{dishId}")
    void deleteByDishId(Long dishId);
    
    /**
     * 批量删除口味数据
     */
    @Delete("<script>"
            + "delete from dish_flavor where dish_id in "
            + "<foreach item='id' collection='dishIds' open='(' separator=',' close=')'>"
            + "#{id}"
            + "</foreach>"
            + "</script>")
    void deleteByDishIds(List<Long> dishIds);
}
