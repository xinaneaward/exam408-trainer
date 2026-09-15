package com.exam408.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.exam408.entity.Question;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface QuestionMapper extends BaseMapper<Question> {

    @Select("SELECT DISTINCT exam_year FROM question ORDER BY exam_year DESC")
    List<Integer> selectDistinctYears();

    @Select("SELECT DISTINCT subject FROM question ORDER BY subject")
    List<String> selectDistinctSubjects();

    @Select("SELECT * FROM question WHERE exam_year = #{year} ORDER BY question_number")
    List<Question> selectByYear(@Param("year") Integer year);

    @Select("SELECT * FROM question WHERE subject = #{subject} AND exam_year IS NOT NULL ORDER BY exam_year DESC, question_number")
    List<Question> selectBySubject(@Param("subject") String subject);

    @Select("SELECT * FROM question WHERE subject = #{subject} AND type = #{type} AND exam_year IS NOT NULL ORDER BY exam_year DESC, question_number")
    List<Question> selectBySubjectAndType(@Param("subject") String subject, @Param("type") String type);

    @Select("SELECT * FROM question WHERE exam_year = #{year} AND subject = #{subject} ORDER BY question_number")
    List<Question> selectByYearAndSubject(@Param("year") Integer year, @Param("subject") String subject);

    @Select("SELECT * FROM question WHERE CONCAT(',', CONCAT(knowledge_tags, ',')) LIKE CONCAT('%,', CONCAT(#{knowledgeTag}, ',%')) OR knowledge_tag = #{knowledgeTag} ORDER BY exam_year DESC, question_number")
    List<Question> selectByKnowledgeTag(@Param("knowledgeTag") String knowledgeTag);

    @Select("SELECT * FROM question WHERE subject = #{subject} AND (CONCAT(',', CONCAT(knowledge_tags, ',')) LIKE CONCAT('%,', CONCAT(#{knowledgeTag}, ',%')) OR knowledge_tag = #{knowledgeTag}) ORDER BY exam_year DESC, question_number")
    List<Question> selectBySubjectAndKnowledgeTag(@Param("subject") String subject, @Param("knowledgeTag") String knowledgeTag);

    @Select("SELECT * FROM question ORDER BY RAND() LIMIT #{limit}")
    List<Question> selectRandom(@Param("limit") Integer limit);

    @Select("SELECT COUNT(*) FROM question WHERE subject = #{subject}")
    Integer countBySubject(@Param("subject") String subject);

    @Select("SELECT knowledge_tags FROM question WHERE subject = #{subject} AND knowledge_tags IS NOT NULL AND knowledge_tags != ''")
    List<String> selectKnowledgeTags(@Param("subject") String subject);

    @Select("SELECT * FROM question WHERE source = 'ai' ORDER BY id DESC")
    List<Question> selectAiQuestions();

    @Select("SELECT * FROM question WHERE source = 'ai' AND subject = #{subject} AND verified = TRUE ORDER BY id DESC")
    List<Question> selectAiQuestionsBySubject(@Param("subject") String subject);

    List<Question> selectByConditions(
            @Param("subject") String subject,
            @Param("knowledgeTag") String knowledgeTag,
            @Param("type") String type,
            @Param("yearStart") Integer yearStart,
            @Param("yearEnd") Integer yearEnd,
            @Param("limit") Integer limit);
}
