package com.erp.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.system.entity.SysIdSequence;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysIdSequenceMapper extends BaseMapper<SysIdSequence> {

    @Select("SELECT * FROM sys_id_sequence WHERE seq_code = #{code} FOR UPDATE")
    SysIdSequence selectForUpdate(@Param("code") String code);
}
