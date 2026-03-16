package com.sky.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.io.Serializable;

@Data
public class EmployeeDTO implements Serializable {

    private Long id;

    @NotBlank(message = "员工用户名不能为空")
    @Size(min = 3, max = 20, message = "员工用户名长度在 3-20 字符")
    private String username;

    @NotBlank(message = "员工名称不能为空")
    @Size(min = 2, max = 32, message = "员工名称长度在 2-32 字符")
    private String name;

    @NotBlank(message = "员工电话不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "员工电话格式不正确")
    private String phone;

    @NotBlank(message = "员工性别不能为空")
    @Pattern(regexp = "^[男女]$", message = "员工性别只能为'男'或'女'")
    private String sex;

    @NotBlank(message = "员工身份证不能为空")
    @Pattern(regexp = "^\\d{17}[\\dXx]$", message = "员工身份证格式不正确")
    private String idNumber;
}
