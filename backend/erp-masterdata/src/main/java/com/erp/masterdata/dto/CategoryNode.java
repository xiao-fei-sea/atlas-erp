package com.erp.masterdata.dto;

import com.erp.masterdata.entity.MdCategory;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CategoryNode {

    private Long id;
    private Long parentId;
    private String code;
    private String name;
    private Integer sort;
    private Integer status;
    private List<CategoryNode> children = new ArrayList<>();

    public static CategoryNode from(MdCategory category) {
        CategoryNode node = new CategoryNode();
        node.setId(category.getId());
        node.setParentId(category.getParentId());
        node.setCode(category.getCode());
        node.setName(category.getName());
        node.setSort(category.getSort());
        node.setStatus(category.getStatus());
        return node;
    }
}
