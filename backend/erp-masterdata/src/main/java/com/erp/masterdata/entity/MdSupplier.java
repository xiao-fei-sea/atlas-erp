package com.erp.masterdata.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("md_supplier")
public class MdSupplier extends BaseEntity {

    private String code;
    private String name;
    private String contactName;
    private String phone;
    private String email;
    private String taxNo;
    private String address;
    private Integer status;
    private String remark;
}
