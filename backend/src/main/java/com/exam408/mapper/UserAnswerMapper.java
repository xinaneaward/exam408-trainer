package com.exam408.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.exam408.entity.UserAnswer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface UserAnswerMapper extends BaseMapper<UserAnswer> {

    @Select("SELECT ua.*, q.subject FROM user_answer ua LEFT JOIN question q ON ua.question_id = q.id WHERE ua.exam_record_id = #{recordId}")
    List<UserAnswer> selectByExamRecordId(@Param("recordId") Long recordId);

    @Select("SELECT COUNT(DISTINCT question_id) FROM user_answer WHERE user_id = #{userId} AND is_correct = 1")
    Integer countCorrectByUserId(@Param("userId") Long userId);

    @Select("SELECT COUNT(DISTINCT question_id) FROM user_answer WHERE user_id = #{userId}")
    Integer countTotalByUserId(@Param("userId") Long userId);
}
