package com.exam408.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.exam408.entity.WrongQuestion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface WrongQuestionMapper extends BaseMapper<WrongQuestion> {

    @Select("SELECT wq.* FROM wrong_question wq INNER JOIN question q ON wq.question_id = q.id WHERE wq.user_id = #{userId} AND q.subject = #{subject} ORDER BY wq.last_wrong_at DESC")
    List<WrongQuestion> selectByUserIdAndSubject(@Param("userId") Long userId, @Param("subject") String subject);

    @Select("SELECT wq.* FROM wrong_question wq INNER JOIN question q ON wq.question_id = q.id WHERE wq.user_id = #{userId} AND q.knowledge_tag = #{tag} ORDER BY wq.last_wrong_at DESC")
    List<WrongQuestion> selectByUserIdAndTag(@Param("userId") Long userId, @Param("tag") String tag);

    @Select("SELECT COUNT(*) FROM wrong_question WHERE user_id = #{userId}")
    Integer countByUserId(@Param("userId") Long userId);

    @Select("SELECT q.subject, COUNT(*) as cnt FROM wrong_question wq LEFT JOIN question q ON wq.question_id = q.id WHERE wq.user_id = #{userId} GROUP BY q.subject")
    List<java.util.Map<String, Object>> countGroupBySubject(@Param("userId") Long userId);
}
