package com.erp.masterdata.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("md_warehouse")
public class MdWarehouse extends BaseEntity {

    private String code;
    private String name;
    private String address;
    private String manager;
    private Integer status;
}
