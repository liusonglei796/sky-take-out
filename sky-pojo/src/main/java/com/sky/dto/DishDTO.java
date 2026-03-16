package com.sky.dto;

import com.sky.entity.DishFlavor;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class DishDTO implements Serializable {

    private Long id;
    
    @NotBlank(message = "菜品名称不能为空")
    @Size(min = 2, max = 32, message = "菜品名称长度在 2-32 字符")
    private String name;
    
    @NotNull(message = "菜品分类不能为空")
    private Long categoryId;
    
    @NotNull(message = "菜品价格不能为空")
    @DecimalMin(value = "0.01", message = "菜品价格必须大于 0")
    @DecimalMax(value = "99999.99", message = "菜品价格不能超过 99999.99")
    private BigDecimal price;
    
    @NotBlank(message = "菜品图片不能为空")
    private String image;
    
    @Size(max = 255, message = "菜品描述不能超过 255 字符")
    private String description;
    
    @NotNull(message = "菜品状态不能为空")
    @Min(value = 0, message = "菜品状态只能为 0 或 1")
    @Max(value = 1, message = "菜品状态只能为 0 或 1")
    private Integer status;
    
    private List<DishFlavor> flavors = new ArrayList<>();
}
