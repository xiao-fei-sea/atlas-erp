package com.erp.masterdata.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("md_product_category")
public class MdCategory extends BaseEntity {

    private Long parentId;
    private String code;
    private String name;
    private Integer sort;
    private Integer status;
}
