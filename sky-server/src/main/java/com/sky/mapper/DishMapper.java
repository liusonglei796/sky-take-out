package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.vo.DishVO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface DishMapper {

    /**
     * 根据分类id查询菜品数量
     * @param categoryId
     * @return
     */
    @Select("select count(id) from dish where category_id = #{categoryId}")
    Integer countByCategoryId(Long categoryId);

    void insert(Dish dish);
    //菜品的分页查询
    Page<DishVO> pageQuery(DishPageQueryDTO dishPageQueryDTO);

    /**
     * 动态条件查询菜品
     * @param dish
     * @return
     */
    List<Dish> list(Dish dish);

    /**
     * 根据套餐id查询菜品
     * @param setmealId
     * @return
     */
    @Select("select a.* from dish a left join setmeal_dish b on a.id = b.dish_id where b.setmeal_id = #{setmealId}")
    List<Dish> getBySetmealId(Long setmealId);

    //根据ID查询菜品
    @Select("select * from dish where id = #{id}")
    Dish getById(Long id);

    //根据id删除菜品数据
    @Delete("delete from dish where id = #{id}")
    void deleteById(Long id);
    //菜品的起售停售
    @Update("update dish d set d.status=#{status} where id=#{id}")
    void update(Dish dish);
    //根据id查询菜品数据
    @Select("select * from dish where id = #{id}")
    DishVO getDishById(Long id);
    //根据分类id查询菜品数据
    @Select("select * from dish where category_id = #{categoryId}")
    List<Dish> getDishByCgId(Dish dish);
}
