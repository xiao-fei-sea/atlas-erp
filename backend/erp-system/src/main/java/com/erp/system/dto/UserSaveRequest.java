package com.erp.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class UserSaveRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(max = 64, message = "用户名过长")
    private String username;

    /** 创建时必填；编辑时为空表示不修改 */
    private String password;

    @NotBlank(message = "姓名不能为空")
    private String realName;

    private Long deptId;
    private String phone;
    private String email;
    private Integer status = 1;
    private String remark;
    private List<Long> roleIds;
}
