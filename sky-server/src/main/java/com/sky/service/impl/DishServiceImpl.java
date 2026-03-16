package com.sky.service.impl;

import lombok.RequiredArgsConstructor;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.BusinessException;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.exception.EntityNotFoundException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DishServiceImpl implements DishService {
    private final DishMapper dishMapper;
    private final DishFlavorMapper dishFlavorMapper;
    private final SetmealMapper setmealMapper;
    private final SetmealDishMapper setmealDishMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String DISH_CACHE_PREFIX = "dish_";
    private static final long CACHE_EXPIRE_TIME = 3600; // 1小时

    /**
     * 新增菜品和对应的口味
     */
    @Transactional
    public void savewithFlavor(DishDTO dishDTO) {
        // 参数验证
        if (dishDTO == null || dishDTO.getCategoryId() == null) {
            throw new BusinessException("菜品信息不完整");
        }

        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);

        // 向菜品表插入数据
        dishMapper.insert(dish);
        Long dishId = dish.getId();

        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && !flavors.isEmpty()) {
            flavors.forEach(dishFlavor -> dishFlavor.setDishId(dishId));
            // 向口味表插入n条数据
            dishFlavorMapper.insertBatch(flavors);
        }

        log.info("新增菜品成功，菜品ID: {}", dishId);
    }

    /**
     * 菜品分页查询
     */
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
        Page<DishVO> page = dishMapper.pageQuery(dishPageQueryDTO);
        Long total = page.getTotal();
        List<DishVO> records = page.getResult();

        // 为每个菜品填充口味数据
        if (!records.isEmpty()) {
            fillDishFlavors(records);
        }

        return new PageResult(total, records);
    }

    /**
     * 条件查询菜品和口味 - 优化版本，使用批量查询
     */
    public List<DishVO> listWithFlavor(Dish dish) {
        List<Dish> dishList = dishMapper.list(dish);
        if (dishList.isEmpty()) {
            return new ArrayList<>();
        }

        // 提取所有菜品ID
        List<Long> dishIds = dishList.stream()
                .map(Dish::getId)
                .collect(Collectors.toList());

        // 批量查询所有菜品的口味 - 解决 N+1 问题
        List<DishFlavor> allFlavors = dishFlavorMapper.getFlavorsByDishIds(dishIds);

        // 按菜品ID分组口味
        Map<Long, List<DishFlavor>> flavorMap = allFlavors.stream()
                .collect(Collectors.groupingBy(DishFlavor::getDishId));

        // 组装结果
        return dishList.stream()
                .map(d -> {
                    DishVO dishVO = new DishVO();
                    BeanUtils.copyProperties(d, dishVO);
                    dishVO.setFlavors(flavorMap.getOrDefault(d.getId(), new ArrayList<>()));
                    return dishVO;
                })
                .collect(Collectors.toList());
    }

    /**
     * 根据分类id查询菜品 - 带缓存
     */
    public List<Dish> list(Long categoryId) {
        if (categoryId == null || categoryId <= 0) {
            throw new BusinessException("分类ID无效");
        }

        // 构建缓存key
        String cacheKey = DISH_CACHE_PREFIX + categoryId;

        // 尝试从缓存获取
        @SuppressWarnings("unchecked")
        List<Dish> cachedDishes = (List<Dish>) redisTemplate.opsForValue().get(cacheKey);
        if (cachedDishes != null) {
            log.debug("从缓存获取菜品列表，分类ID: {}", categoryId);
            return cachedDishes;
        }

        // 查询数据库
        Dish dish = Dish.builder()
                .categoryId(categoryId)
                .status(StatusConstant.ENABLE)
                .build();
        List<Dish> dishes = dishMapper.list(dish);

        // 存入缓存
        redisTemplate.opsForValue().set(cacheKey, dishes, CACHE_EXPIRE_TIME, java.util.concurrent.TimeUnit.SECONDS);
        log.debug("菜品列表已缓存，分类ID: {}", categoryId);

        return dishes;
    }

    /**
     * 菜品的批量删除
     */
    @Transactional
    public void deleteBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("删除的菜品ID不能为空");
        }

        // 判断菜品是否能够删除 --- 是否在起售中的菜品
        for (Long id : ids) {
            Dish dish = dishMapper.getById(id);
            if (dish == null) {
                throw new EntityNotFoundException("菜品ID: " + id + " 不存在");
            }
            if (dish.getStatus() == 1) {
                // 菜品处于起售中，不能删除
                throw new DeletionNotAllowedException("菜品 " + dish.getName() + " 正在起售中，不能删除");
            }
        }

        // 判断当前菜品是否能够删除 --- 是否被套餐关联了
        List<Long> setmealIds = setmealDishMapper.getSetmealIdsByDishId(ids);
        if (setmealIds != null && !setmealIds.isEmpty()) {
            throw new DeletionNotAllowedException("菜品已被套餐关联，不能删除");
        }

        // 删除菜品表中的菜品数据
        dishMapper.deleteByIds(ids);

        // 批量删除口味数据 - 优化版本
        dishFlavorMapper.deleteByDishIds(ids);

        // 清理所有菜品的缓存
        clearDishCache();
        log.info("批量删除菜品成功，菜品ID列表: {}", ids);
    }

    /**
     * 菜品的起售停售
     */
    @Transactional
    public void startOrStop(Integer status, Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException("菜品ID无效");
        }
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException("菜品状态只能为 0 或 1");
        }

        Dish dish = dishMapper.getById(id);
        if (dish == null) {
            throw new EntityNotFoundException("菜品不存在");
        }

        Dish updateDish = Dish.builder()
                .id(id)
                .status(status)
                .build();
        dishMapper.update(updateDish);

        // 清理缓存
        clearDishCache();
        log.info("菜品状态更新成功，菜品ID: {}, 新状态: {}", id, status);
    }

    /**
     * 根据ID查询菜品 - 优化版本
     */
    public DishVO getByIdWithFlavor(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException("菜品ID无效");
        }

        // 根据id查询菜品数据
        Dish dish = dishMapper.getById(id);
        if (dish == null) {
            throw new EntityNotFoundException("菜品不存在");
        }

        // 根据id查询口味数据
        List<DishFlavor> dishFlavors = dishFlavorMapper.getByDishId(id);

        // 将查询到的数据封装到dishVO中
        DishVO dishVO = new DishVO();
        BeanUtils.copyProperties(dish, dishVO);
        dishVO.setFlavors(dishFlavors);

        return dishVO;
    }

    /**
     * 修改菜品和口味数据
     */
    @Transactional
    public void updateWithFlavor(DishDTO dishDTO) {
        if (dishDTO == null || dishDTO.getId() == null) {
            throw new BusinessException("菜品信息不完整");
        }

        // 检查菜品是否存在
        Dish existingDish = dishMapper.getById(dishDTO.getId());
        if (existingDish == null) {
            throw new EntityNotFoundException("菜品不存在");
        }

        // 修改菜品
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        dishMapper.update(dish);

        // 修改口味
        Long dishId = dish.getId();
        dishFlavorMapper.deleteByDishId(dishId);

        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && !flavors.isEmpty()) {
            flavors.forEach(dishFlavor -> dishFlavor.setDishId(dishId));
            dishFlavorMapper.insertBatch(flavors);
        }

        // 清理缓存
        clearDishCache();
        log.info("菜品修改成功，菜品ID: {}", dishId);
    }

    /**
     * 为菜品列表填充口味数据 - 解决 N+1 问题
     */
    private void fillDishFlavors(List<DishVO> dishes) {
        if (dishes.isEmpty()) {
            return;
        }

        // 提取所有菜品ID
        List<Long> dishIds = dishes.stream()
                .map(DishVO::getId)
                .collect(Collectors.toList());

        // 批量查询所有菜品的口味
        List<DishFlavor> allFlavors = dishFlavorMapper.getFlavorsByDishIds(dishIds);

        // 按菜品ID分组口味
        Map<Long, List<DishFlavor>> flavorMap = allFlavors.stream()
                .collect(Collectors.groupingBy(DishFlavor::getDishId));

        // 为每个菜品设置对应的口味
        dishes.forEach(dishVO -> dishVO.setFlavors(flavorMap.getOrDefault(dishVO.getId(), new ArrayList<>())));
    }

    /**
     * 清理所有菜品相关的缓存
     */
    private void clearDishCache() {
        // 使用 SCAN 命令非阻塞地查找 key
        String pattern = DISH_CACHE_PREFIX + "*";
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(100).build();

        try (Cursor<byte[]> cursor = redisTemplate.getConnectionFactory().getConnection().scan(options)) {
            List<String> keys = new ArrayList<>();
            while (cursor.hasNext()) {
                keys.add(new String(cursor.next()));
            }

            if (!keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.debug("菜品缓存已清理，共 {} 条", keys.size());
            }
        } catch (Exception e) {
            log.error("清理菜品缓存失败", e);
        }
    }
}
