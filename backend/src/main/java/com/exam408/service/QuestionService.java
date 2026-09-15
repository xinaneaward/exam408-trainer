package com.exam408.service;

import com.exam408.entity.Question;
import java.util.List;

public interface QuestionService {
    List<Question> listByYear(Integer year);
    List<Question> listBySubject(String subject);
    List<Question> listBySubjectAndType(String subject, String type);
    List<Question> listByYearAndSubject(Integer year, String subject);
    List<Question> listByKnowledgeTag(String knowledgeTag);
    List<Question> listBySubjectAndKnowledgeTag(String subject, String knowledgeTag);
    List<Question> listRandom(Integer limit);
    Question getById(Long id);
    List<Integer> getYears();
    List<String> getSubjects();
    List<String> getKnowledgeTags(String subject);

    List<Question> listByConditions(String subject, String knowledgeTag, String type, Integer yearStart, Integer yearEnd, Integer limit);
}
