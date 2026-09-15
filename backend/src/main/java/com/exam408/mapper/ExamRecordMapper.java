package com.exam408.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.exam408.entity.ExamRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface ExamRecordMapper extends BaseMapper<ExamRecord> {

    @Select("SELECT * FROM exam_record WHERE user_id = #{userId} ORDER BY created_at DESC")
    List<ExamRecord> selectByUserId(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM exam_record WHERE user_id = #{userId}")
    Integer countByUserId(@Param("userId") Long userId);

    @Select("SELECT IFNULL(AVG(correct_count * 100.0 / NULLIF(total_questions, 0)), 0) FROM exam_record WHERE user_id = #{userId}")
    Double selectAvgAccuracyByUserId(@Param("userId") Long userId);
}
