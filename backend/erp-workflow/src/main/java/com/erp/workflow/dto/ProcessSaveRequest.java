package com.erp.workflow.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class ProcessSaveRequest {

    private String code;
    private String name;
    private String bizType;
    private Integer enabled;
    private String remark;
    private List<Node> nodes = new ArrayList<>();

    @Data
    public static class Node {
        private Integer seq;
        private String name;
        private String approverType;
        private Long approverId;
        private String signType;
        private String approverIds;
        private Long deptId;
        private Integer timeoutHours;
        private BigDecimal minAmount;
        private BigDecimal maxAmount;
    }
}
