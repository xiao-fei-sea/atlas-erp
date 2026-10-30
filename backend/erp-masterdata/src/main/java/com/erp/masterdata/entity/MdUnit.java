package com.erp.masterdata.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("md_unit")
public class MdUnit extends BaseEntity {

    private String code;
    private String name;
    private Integer precisionNum;
    private Integer status;
}
