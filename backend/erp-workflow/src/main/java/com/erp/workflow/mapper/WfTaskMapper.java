package com.erp.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.workflow.dto.TaskView;
import com.erp.workflow.entity.WfTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface WfTaskMapper extends BaseMapper<WfTask> {

    @Update("UPDATE wf_task SET status = #{toStatus}, opinion = #{opinion}, acted_by = #{actedBy}, "
            + "acted_at = NOW(), updated_at = NOW() WHERE id = #{id} AND status = 'PENDING'")
    int act(@Param("id") Long id, @Param("toStatus") String toStatus,
            @Param("opinion") String opinion, @Param("actedBy") Long actedBy);

    @Update("UPDATE wf_task SET status = 'CANCELLED', updated_at = NOW() WHERE instance_id = #{instanceId} AND status = 'PENDING'")
    int cancelPending(@Param("instanceId") Long instanceId);

    @Update("UPDATE wf_task SET status = 'CANCELLED', updated_at = NOW() "
            + "WHERE instance_id = #{instanceId} AND node_seq = #{nodeSeq} AND status = 'PENDING'")
    int cancelPendingAtNode(@Param("instanceId") Long instanceId, @Param("nodeSeq") Integer nodeSeq);

    @Select("SELECT COUNT(*) FROM wf_task WHERE instance_id = #{instanceId} "
            + "AND node_seq = #{nodeSeq} AND status = 'PENDING'")
    int countPendingAtNode(@Param("instanceId") Long instanceId, @Param("nodeSeq") Integer nodeSeq);

    @Update("UPDATE wf_task SET origin_approver_id = COALESCE(origin_approver_id, approver_id), "
            + "approver_type = 'USER', approver_id = #{targetUserId}, transferred_by = #{operatorId}, "
            + "transferred_at = NOW(), updated_at = NOW() WHERE id = #{id} AND status = 'PENDING'")
    int transfer(@Param("id") Long id, @Param("targetUserId") Long targetUserId,
                 @Param("operatorId") Long operatorId);

    @Update("UPDATE wf_task SET overdue = 1, updated_at = NOW() "
            + "WHERE status = 'PENDING' AND overdue = 0 AND due_at IS NOT NULL AND due_at < NOW()")
    int markOverdue();

    @Select("""
            <script>
            SELECT t.id, t.instance_id, t.node_seq, t.node_name, t.sign_type, t.approver_type, t.approver_id,
                   t.approver_role, t.status, t.opinion, t.due_at, t.overdue, t.created_at,
                   i.biz_type, i.biz_no, i.amount, i.submitter_id, u.real_name AS submitter_name
              FROM wf_task t
              JOIN wf_instance i ON i.id = t.instance_id
              LEFT JOIN sys_user u ON u.id = i.submitter_id
             WHERE t.status = #{status}
             <if test="userId != null and (roles == null or roles.isEmpty())">
               AND t.approver_type = 'USER' AND t.approver_id = #{userId}
             </if>
             <if test="roles != null and !roles.isEmpty()">
               AND ((t.approver_type = 'ROLE' AND t.approver_role IN
                 <foreach collection="roles" item="role" open="(" separator="," close=")">#{role}</foreach>)
                 <if test="userId != null">OR (t.approver_type = 'USER' AND t.approver_id = #{userId})</if>)
             </if>
             ORDER BY t.id DESC
            </script>
            """)
    List<TaskView> selectTasks(@Param("status") String status, @Param("userId") Long userId,
                               @Param("roles") List<String> roles);

    @Select("SELECT COUNT(*) FROM wf_task WHERE status = 'PENDING' AND instance_id = #{instanceId}")
    int countPending(@Param("instanceId") Long instanceId);
}
