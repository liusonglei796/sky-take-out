package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class DishServiceImpl implements DishService {
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishFlavorMapper dishFlavorMapper;
    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;

    //新增菜品和对应的口味
    @Transactional
    public void savewithFlavor(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO,dish);

        //向菜品表插入数据
        dishMapper.insert(dish);
        //获取insert语句生成的主键值
        Long dishId = dish.getId();

        List<DishFlavor> flavors=dishDTO.getFlavors();
        if(flavors != null && flavors.size() > 0){
            flavors.forEach(dishFlavor -> dishFlavor.setDishId(dishId));
            //向口味表插入n条数据
            dishFlavorMapper.insertBatch( flavors);
        }
    }
    //菜品分页查询
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        PageHelper.startPage(dishPageQueryDTO.getPage(),dishPageQueryDTO.getPageSize());
        Page<DishVO> page= dishMapper.pageQuery(dishPageQueryDTO);
        Long total = page.getTotal();
        List<DishVO> records = page.getResult();
        return new PageResult(total,records);
    }

    /**
     * 条件查询菜品和口味
     * @param dish
     * @return
     */
    public List<DishVO> listWithFlavor(Dish dish) {
        List<Dish> dishList = dishMapper.list(dish);

        List<DishVO> dishVOList = new ArrayList<>();

        for (Dish d : dishList) {
            DishVO dishVO = new DishVO();
            BeanUtils.copyProperties(d,dishVO);

            //根据菜品id查询对应的口味
            List<DishFlavor> flavors = dishFlavorMapper.getByDishId(d.getId());

            dishVO.setFlavors(flavors);
            dishVOList.add(dishVO);
        }

        return dishVOList;
    }

    /**
     * 根据分类id查询菜品
     * @param categoryId
     * @return
     */
    public List<Dish> list(Long categoryId) {
        Dish dish = Dish.builder()
                .categoryId(categoryId)
                .status(StatusConstant.ENABLE)
                .build();
        return dishMapper.list(dish);
    }
    //菜品的批量删除
    public void deleteBatch(List<Long> ids){
        //判断菜品是否能够删除---是否在起售中的菜品
        for(Long id:ids){
           Dish dish = dishMapper.getById(id);
           if(dish.getStatus()==1){
               //菜品处于起售中，不能删除
               throw new DeletionNotAllowedException("当前菜品正在起售中，不能删除");
           }
        }
        //判断当前菜品是否能够删除---是否被套餐关联了
        List<Long> setmealIds = setmealDishMapper.getSetmealIdsByDishId(ids);
        if(setmealIds!=null && setmealIds.size()>0){
            throw new DeletionNotAllowedException("当前菜品被套餐关联，不能删除");
        }
        //删除菜品表中的菜品数据
        for(Long id:ids){
            dishMapper.deleteById(id);
            //删除和菜品的口味的关联数据
            dishFlavorMapper.deleteByDishId(id);
        }

    }
    //菜品的起售停售
    public void startOrStop(Integer status, Long id) {
        Dish dish = Dish.builder()
                .id(id)
                .status(status)
                .build();
        dishMapper.update(dish);
    }
    //根据ID查询菜品
    public DishVO getByIdWithFlavor(Long id) {
        //根据id查询菜品数据
        Dish dish = dishMapper.getById(id);
        //根据id查询口味数据
        List<DishFlavor> dishFlavors = dishFlavorMapper.getByDishId(id);
        //将查询到的数据封装到dishVO中
        //1.dishVO.setFlavors(dishFlavors);
        DishVO dishVO = new DishVO();
        BeanUtils.copyProperties(dish,dishVO);
        dishVO.setFlavors(dishFlavors);
        return dishVO;

    }
    //根据分类id查询菜品*******!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!有问题!!!!!!!
//    public List<Dish> listByCategoryId(Long categoryId) {
//        Dish dish = Dish.builder()
//                .categoryId(categoryId)
//                .status(StatusConstant.ENABLE)
//                .build();
//        return dishMapper.getDishByCgId(dish);
//    }
    //修改菜品和口味数据
//    public void updateWithFlavor(DishDTO dishDTO) {
//        Dish dish = new Dish();
//        BeanUtils.copyProperties(dishDTO,dish);
//        //修改菜品
//        dishMapper.update(dish);
//        //修改口味
//        Long dishId = dish.getId();
//        dishFlavorMapper.deleteByDishId(dishId);
//        List<DishFlavor> flavors = dishDTO.getFlavors();
//        if(flavors != null && flavors.size() > 0){
//            flavors.forEach(dishFlavor -> dishFlavor.setDishId(dishId));
//            dishFlavorMapper.insertBatch(flavors);
//        }
//
//    }

}
