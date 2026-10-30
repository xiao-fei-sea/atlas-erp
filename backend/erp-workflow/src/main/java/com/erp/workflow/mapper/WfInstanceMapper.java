package com.erp.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.workflow.entity.WfInstance;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface WfInstanceMapper extends BaseMapper<WfInstance> {

    @Update("UPDATE wf_instance SET status = #{toStatus}, current_seq = #{currentSeq}, finished_at = #{finishedAt}, "
            + "updated_at = NOW() WHERE id = #{id} AND status = #{fromStatus}")
    int advance(@Param("id") Long id, @Param("fromStatus") String fromStatus,
                @Param("toStatus") String toStatus, @Param("currentSeq") int currentSeq,
                @Param("finishedAt") java.time.LocalDateTime finishedAt);
}
