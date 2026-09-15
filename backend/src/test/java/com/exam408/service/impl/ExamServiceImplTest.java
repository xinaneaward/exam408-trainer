package com.exam408.service.impl;

import com.exam408.dto.ExamResultVO;
import com.exam408.dto.SubmitAnswerRequest;
import com.exam408.entity.ExamRecord;
import com.exam408.entity.Question;
import com.exam408.entity.UserAnswer;
import com.exam408.entity.WrongQuestion;
import com.exam408.mapper.ExamRecordMapper;
import com.exam408.mapper.QuestionMapper;
import com.exam408.mapper.UserAnswerMapper;
import com.exam408.mapper.WrongQuestionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExamServiceImplTest {

    @Mock private ExamRecordMapper examRecordMapper;
    @Mock private UserAnswerMapper userAnswerMapper;
    @Mock private QuestionMapper questionMapper;
    @Mock private WrongQuestionMapper wrongQuestionMapper;

    private ExamServiceImpl service;

    private Question singleChoice(String answer) {
        Question q = new Question();
        q.setId(1L);
        q.setType("单选");
        q.setSubject("数据结构");
        q.setAnswer(answer);
        q.setKnowledgeTag("线性表");
        return q;
    }

    private Question essay() {
        Question q = new Question();
        q.setId(2L);
        q.setType("综合应用");
        q.setSubject("数据结构");
        q.setAnswer("参考解析");
        q.setKnowledgeTag("线性表");
        return q;
    }

    private SubmitAnswerRequest.AnswerItem item(Long id, String answer) {
        SubmitAnswerRequest.AnswerItem i = new SubmitAnswerRequest.AnswerItem();
        i.setQuestionId(id);
        i.setUserAnswer(answer);
        return i;
    }

    @BeforeEach
    void setUp() {
        service = new ExamServiceImpl();
        ReflectionTestUtils.setField(service, "examRecordMapper", examRecordMapper);
        ReflectionTestUtils.setField(service, "userAnswerMapper", userAnswerMapper);
        ReflectionTestUtils.setField(service, "questionMapper", questionMapper);
        ReflectionTestUtils.setField(service, "wrongQuestionMapper", wrongQuestionMapper);
    }

    @Test
    void singleChoiceCorrectScores2AndSaves() {
        when(questionMapper.selectById(1L)).thenReturn(singleChoice("B"));
        SubmitAnswerRequest req = new SubmitAnswerRequest();
        req.setMode("year");
        req.setAnswers(Arrays.asList(item(1L, "B")));

        ExamResultVO vo = service.submitAnswers(10L, req);

        assertNotNull(vo);
        assertEquals(1, vo.getCorrectCount());
        assertEquals(0, new BigDecimal("2").compareTo(vo.getScore()));
        assertEquals(1, vo.getQuestions().size());
        assertTrue(vo.getQuestions().get(0).getIsCorrect());
        assertEquals(Integer.valueOf(2), vo.getQuestions().get(0).getScore());

        ArgumentCaptor<UserAnswer> uaCaptor = ArgumentCaptor.forClass(UserAnswer.class);
        verify(userAnswerMapper, times(1)).insert(uaCaptor.capture());
        assertEquals(Long.valueOf(10), uaCaptor.getValue().getUserId());
        assertTrue(uaCaptor.getValue().getIsCorrect());
        assertEquals(Integer.valueOf(2), uaCaptor.getValue().getScore());
        // 未触发错题收录
        verify(wrongQuestionMapper, never()).selectOne(any());
    }

    @Test
    void singleChoiceWrongCollectsAndResetsReviewStage() {
        when(questionMapper.selectById(1L)).thenReturn(singleChoice("B"));

        WrongQuestion existing = new WrongQuestion();
        existing.setId(99L);
        existing.setWrongCount(2);
        existing.setReviewStage(3);
        existing.setIsReviewed(true);
        when(wrongQuestionMapper.selectOne(any())).thenReturn(existing);

        SubmitAnswerRequest req = new SubmitAnswerRequest();
        req.setMode("year");
        req.setAnswers(Arrays.asList(item(1L, "C")));

        ExamResultVO vo = service.submitAnswers(10L, req);

        assertEquals(0, vo.getCorrectCount());
        assertEquals(0, new BigDecimal("0").compareTo(vo.getScore()));
        assertFalse(vo.getQuestions().get(0).getIsCorrect());
        assertEquals(Integer.valueOf(0), vo.getQuestions().get(0).getScore());

        // 明细照常保存（错了也要记录）
        verify(userAnswerMapper, times(1)).insert(any(UserAnswer.class));

        // 错题：次数+1、阶段重置、待复习
        ArgumentCaptor<WrongQuestion> wqCaptor = ArgumentCaptor.forClass(WrongQuestion.class);
        verify(wrongQuestionMapper, times(1)).updateById(wqCaptor.capture());
        WrongQuestion updated = wqCaptor.getValue();
        assertEquals(Integer.valueOf(3), updated.getWrongCount());
        assertEquals(Integer.valueOf(0), updated.getReviewStage());
        assertNull(updated.getNextReviewAt());
        assertFalse(updated.getIsReviewed());
    }

    @Test
    void singleChoiceWrongInsertsNewWrongQuestion() {
        when(questionMapper.selectById(1L)).thenReturn(singleChoice("B"));
        when(wrongQuestionMapper.selectOne(any())).thenReturn(null);

        SubmitAnswerRequest req = new SubmitAnswerRequest();
        req.setMode("year");
        req.setAnswers(Arrays.asList(item(1L, "A")));

        service.submitAnswers(10L, req);

        ArgumentCaptor<WrongQuestion> wqCaptor = ArgumentCaptor.forClass(WrongQuestion.class);
        verify(wrongQuestionMapper, times(1)).insert(wqCaptor.capture());
        WrongQuestion inserted = wqCaptor.getValue();
        assertEquals(Long.valueOf(1), inserted.getQuestionId());
        assertEquals(Integer.valueOf(1), inserted.getWrongCount());
        assertEquals(Integer.valueOf(0), inserted.getReviewStage());
        assertNull(inserted.getNextReviewAt());
    }

    @Test
    void essayIsNotScoredNotSavedNotCollected() {
        when(questionMapper.selectById(2L)).thenReturn(essay());
        SubmitAnswerRequest req = new SubmitAnswerRequest();
        req.setMode("year");
        req.setAnswers(Arrays.asList(item(2L, "我的作答无所谓")));

        ExamResultVO vo = service.submitAnswers(10L, req);

        assertEquals(0, vo.getCorrectCount());
        assertEquals(0, new BigDecimal("0").compareTo(vo.getScore()));
        assertEquals(1, vo.getQuestions().size());
        assertFalse(vo.getQuestions().get(0).getIsCorrect());
        assertEquals(Integer.valueOf(0), vo.getQuestions().get(0).getScore());

        // 综合应用题不保存明细、不收录错题
        verify(userAnswerMapper, never()).insert(any());
        verify(wrongQuestionMapper, never()).selectOne(any());
    }

    @Test
    void mixedSubmissionScoresOnlySingleChoiceAndSavesRecord() {
        when(questionMapper.selectById(1L)).thenReturn(singleChoice("B"));
        when(questionMapper.selectById(3L)).thenReturn(singleChoice("A"));
        when(questionMapper.selectById(2L)).thenReturn(essay());
        when(wrongQuestionMapper.selectOne(any())).thenReturn(null);

        SubmitAnswerRequest req = new SubmitAnswerRequest();
        req.setMode("year");
        req.setAnswers(Arrays.asList(
                item(1L, "B"),
                item(3L, "C"),
                item(2L, "")
        ));

        ExamResultVO vo = service.submitAnswers(10L, req);

        // 单选对1道=2分，错1道0分，应用题不计分
        assertEquals(1, vo.getCorrectCount());
        assertEquals(0, new BigDecimal("2").compareTo(vo.getScore()));
        assertEquals(3, vo.getQuestions().size());

        // 记录：总题数=提交题数，分数=2
        ArgumentCaptor<ExamRecord> recCaptor = ArgumentCaptor.forClass(ExamRecord.class);
        verify(examRecordMapper, times(1)).insert(recCaptor.capture());
        assertEquals(Integer.valueOf(3), recCaptor.getValue().getTotalQuestions());
        assertEquals(Integer.valueOf(1), recCaptor.getValue().getCorrectCount());

        // 只保存了2道单选明细，1道错单选进错题本
        verify(userAnswerMapper, times(2)).insert(any(UserAnswer.class));
        verify(wrongQuestionMapper, times(1)).insert(any(WrongQuestion.class));
    }

    @Test
    void saveSingleAnswerSkipsEssay() {
        when(questionMapper.selectById(2L)).thenReturn(essay());
        SubmitAnswerRequest req = new SubmitAnswerRequest();
        req.setMode("practice");
        req.setAnswers(Arrays.asList(item(2L, "随便写")));

        service.saveSingleAnswer(10L, req);

        verify(userAnswerMapper, never()).selectOne(any());
        verify(userAnswerMapper, never()).insert(any());
        verify(userAnswerMapper, never()).updateById(any());
        verify(wrongQuestionMapper, never()).selectOne(any());
    }

    @Test
    void saveSingleAnswerSavesSingleChoiceAndCollectsWrong() {
        when(questionMapper.selectById(1L)).thenReturn(singleChoice("B"));
        when(userAnswerMapper.selectOne(any())).thenReturn(null);
        when(wrongQuestionMapper.selectOne(any())).thenReturn(null);

        SubmitAnswerRequest req = new SubmitAnswerRequest();
        req.setMode("practice");
        req.setAnswers(Arrays.asList(item(1L, "A")));

        service.saveSingleAnswer(10L, req);

        ArgumentCaptor<UserAnswer> uaCaptor = ArgumentCaptor.forClass(UserAnswer.class);
        verify(userAnswerMapper, times(1)).insert(uaCaptor.capture());
        assertFalse(uaCaptor.getValue().getIsCorrect());
        assertEquals(Integer.valueOf(0), uaCaptor.getValue().getScore());

        ArgumentCaptor<WrongQuestion> wqCaptor = ArgumentCaptor.forClass(WrongQuestion.class);
        verify(wrongQuestionMapper, times(1)).insert(wqCaptor.capture());
        assertEquals(Integer.valueOf(1), wqCaptor.getValue().getWrongCount());
    }
}