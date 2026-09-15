package com.exam408.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam408.entity.Question;
import com.exam408.mapper.QuestionMapper;
import com.exam408.service.QuestionService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class QuestionServiceImpl implements QuestionService {

    @Resource
    private QuestionMapper questionMapper;

    @Override
    public List<Question> listByYear(Integer year) {
        return questionMapper.selectByYear(year);
    }

    @Override
    public List<Question> listBySubject(String subject) {
        return questionMapper.selectBySubject(subject);
    }

    @Override
    public List<Question> listBySubjectAndType(String subject, String type) {
        return questionMapper.selectBySubjectAndType(subject, type);
    }

    @Override
    public List<Question> listByYearAndSubject(Integer year, String subject) {
        return questionMapper.selectByYearAndSubject(year, subject);
    }

    @Override
    public List<Question> listByKnowledgeTag(String knowledgeTag) {
        return questionMapper.selectByKnowledgeTag(knowledgeTag);
    }

    @Override
    public List<Question> listBySubjectAndKnowledgeTag(String subject, String knowledgeTag) {
        return questionMapper.selectBySubjectAndKnowledgeTag(subject, knowledgeTag);
    }

    @Override
    public List<Question> listRandom(Integer limit) {
        return questionMapper.selectRandom(limit != null ? limit : 40);
    }

    @Override
    public Question getById(Long id) {
        return questionMapper.selectById(id);
    }

    @Override
    public List<Integer> getYears() {
        return questionMapper.selectDistinctYears();
    }

    @Override
    public List<String> getSubjects() {
        return questionMapper.selectDistinctSubjects();
    }

    @Override
    public List<String> getKnowledgeTags(String subject) {
        List<String> raw = questionMapper.selectKnowledgeTags(subject);
        java.util.Set<String> uniqueTags = new java.util.TreeSet<>();
        for (String tagStr : raw) {
            if (tagStr != null && !tagStr.isEmpty()) {
                for (String t : tagStr.split(",")) {
                    String trimmed = t.trim();
                    if (!trimmed.isEmpty()) {
                        uniqueTags.add(trimmed);
                    }
                }
            }
        }
        return new java.util.ArrayList<>(uniqueTags);
    }

    @Override
    public List<Question> listByConditions(String subject, String knowledgeTag, String type, Integer yearStart, Integer yearEnd, Integer limit) {
        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<>();
        
        if (subject != null && !subject.isEmpty()) {
            wrapper.eq(Question::getSubject, subject);
        }
        if (knowledgeTag != null && !knowledgeTag.isEmpty()) {
            final String tag = knowledgeTag.trim();
            wrapper.and(w -> w.apply("CONCAT(',', CONCAT(knowledge_tags, ',')) LIKE CONCAT('%,', CONCAT({0}, ',%'))", tag)
                    .or().eq(Question::getKnowledgeTag, tag));
        }
        if (type != null && !type.isEmpty()) {
            wrapper.eq(Question::getType, type);
        }
        if (yearStart != null) {
            wrapper.ge(Question::getExamYear, yearStart);
        }
        if (yearEnd != null) {
            wrapper.le(Question::getExamYear, yearEnd);
        }
        
        wrapper.orderByDesc(Question::getExamYear)
               .orderByAsc(Question::getQuestionNumber);
        
        if (limit != null) {
            wrapper.last("LIMIT " + limit);
        }
        
        List<Question> questions = questionMapper.selectList(wrapper);
        return removeDuplicates(questions);
    }

    private List<Question> removeDuplicates(List<Question> questions) {
        if (questions == null || questions.isEmpty()) {
            return questions;
        }
        Set<Long> seenIds = new HashSet<>();
        List<Question> result = new ArrayList<>();
        for (Question q : questions) {
            if (q.getId() != null && !seenIds.contains(q.getId())) {
                seenIds.add(q.getId());
                result.add(q);
            }
        }
        return result;
    }
}
