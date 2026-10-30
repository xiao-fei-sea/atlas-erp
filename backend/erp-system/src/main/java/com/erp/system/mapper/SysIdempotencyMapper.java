package com.erp.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.system.entity.SysIdempotency;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysIdempotencyMapper extends BaseMapper<SysIdempotency> {
}
