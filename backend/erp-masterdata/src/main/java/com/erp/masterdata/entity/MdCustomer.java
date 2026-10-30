package com.erp.masterdata.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("md_customer")
public class MdCustomer extends BaseEntity {

    private String code;
    private String name;
    private String level;
    private String contactName;
    private String phone;
    private String email;
    private String taxNo;
    private String address;
    private BigDecimal creditLimit;
    private Integer status;
    private String remark;
}
