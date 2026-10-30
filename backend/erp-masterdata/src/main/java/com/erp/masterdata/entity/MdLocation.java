package com.erp.masterdata.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("md_location")
public class MdLocation extends BaseEntity {

    private Long warehouseId;
    private String code;
    private String name;
    private Integer status;
}
