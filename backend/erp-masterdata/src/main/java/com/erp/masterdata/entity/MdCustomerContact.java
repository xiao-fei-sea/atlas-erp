package com.erp.masterdata.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("md_customer_contact")
public class MdCustomerContact extends BaseEntity {

    private Long customerId;
    private String name;
    private String phone;
    private String email;
    private String position;
}
