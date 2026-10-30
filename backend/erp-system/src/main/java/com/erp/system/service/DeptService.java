package com.erp.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.erp.common.api.ErrorCode;
import com.erp.common.exception.BizException;
import com.erp.system.dto.MenuNode;
import com.erp.system.entity.SysDept;
import com.erp.system.entity.SysUser;
import com.erp.system.mapper.SysDeptMapper;
import com.erp.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeptService {

    private final SysDeptMapper deptMapper;
    private final SysUserMapper userMapper;

    public List<SysDept> listAll() {
        return deptMapper.selectList(new LambdaQueryWrapper<SysDept>().orderByAsc(SysDept::getSort));
    }

    public List<Map<String, Object>> tree() {
        List<SysDept> depts = listAll();
        return build(depts.stream().map(d -> {
            Map<String, Object> node = new java.util.LinkedHashMap<>();
            node.put("id", d.getId());
            node.put("parentId", d.getParentId());
            node.put("name", d.getName());
            node.put("code", d.getCode());
            node.put("leader", d.getLeader());
            node.put("phone", d.getPhone());
            node.put("sort", d.getSort());
            node.put("status", d.getStatus());
            node.put("children", new ArrayList<>());
            return node;
        }).toList(), depts);
    }

    private List<Map<String, Object>> build(List<Map<String, Object>> nodes, List<SysDept> depts) {
        Map<Long, Map<String, Object>> map = nodes.stream()
                .collect(Collectors.toMap(n -> (Long) n.get("id"), n -> n));
        List<Map<String, Object>> roots = new ArrayList<>();
        for (SysDept dept : depts) {
            Map<String, Object> node = map.get(dept.getId());
            Long parentId = dept.getParentId();
            if (parentId == null || parentId == 0 || !map.containsKey(parentId)) {
                roots.add(node);
            } else {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> children = (List<Map<String, Object>>) map.get(parentId).get("children");
                children.add(node);
            }
        }
        Comparator<Map<String, Object>> cmp = Comparator.comparing(n -> n.get("sort") == null ? 0 : (Integer) n.get("sort"));
        sort(roots, cmp);
        return roots;
    }

    @SuppressWarnings("unchecked")
    private void sort(List<Map<String, Object>> nodes, Comparator<Map<String, Object>> cmp) {
        nodes.sort(cmp);
        nodes.forEach(n -> sort((List<Map<String, Object>>) n.get("children"), cmp));
    }

    public Long create(SysDept dept) {
        Long count = deptMapper.selectCount(new LambdaQueryWrapper<SysDept>().eq(SysDept::getCode, dept.getCode()));
        if (count > 0) {
            throw new BizException(ErrorCode.DUPLICATE, "部门编码已存在");
        }
        deptMapper.insert(dept);
        return dept.getId();
    }

    public void update(Long id, SysDept dept) {
        SysDept exists = deptMapper.selectById(id);
        if (exists == null) {
            throw BizException.notFound("部门不存在");
        }
        dept.setId(id);
        deptMapper.updateById(dept);
    }

    public void delete(Long id) {
        Long children = deptMapper.selectCount(new LambdaQueryWrapper<SysDept>().eq(SysDept::getParentId, id));
        if (children > 0) {
            throw new BizException(ErrorCode.BIZ_RULE_VIOLATION, "请先删除下级部门");
        }
        Long users = userMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getDeptId, id));
        if (users > 0) {
            throw new BizException(ErrorCode.BIZ_RULE_VIOLATION, "部门下存在用户，不能删除");
        }
        deptMapper.deleteById(id);
    }
}
