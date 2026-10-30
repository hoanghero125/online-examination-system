package com.group2.oes.dao.memory;

import com.group2.oes.dao.QuestionDao;
import com.group2.oes.model.Exam;
import com.group2.oes.model.Subject;
import com.group2.oes.model.question.AnswerOption;
import com.group2.oes.model.question.Question;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

class MemoryQuestionDao implements QuestionDao {

    private final InMemoryDatabase db;

    MemoryQuestionDao(InMemoryDatabase db) {
        this.db = db;
    }

    @Override
    public Question insert(Question q) {
        synchronized (db) {
            q.assignId(db.nextId());
            for (AnswerOption o : q.getOptions()) {
                o.assignId(db.nextId());
            }
            db.questions.put(q.getQuestionId(), q);
            return q;
        }
    }

    @Override
    public Optional<Question> findById(int questionId) {
        synchronized (db) {
            return Optional.ofNullable(db.questions.get(questionId));
        }
    }

    @Override
    public Map<Integer, Question> findByIds(Collection<Integer> ids) {
        synchronized (db) {
            Map<Integer, Question> m = new LinkedHashMap<>();
            for (int id : ids) {
                Question q = db.questions.get(id);
                if (q != null) {
                    m.put(id, q);
                }
            }
            return m;
        }
    }

    @Override
    public List<Question> findBySubject(Subject subject) {
        synchronized (db) {
            return db.questions.values().stream().filter(q -> q.getSubject().equals(subject)).toList();
        }
    }

    @Override
    public void update(Question q) {
        synchronized (db) {
            for (AnswerOption o : q.getOptions()) {
                o.assignId(db.nextId());
            }
            db.questions.put(q.getQuestionId(), q);
        }
    }

    @Override
    public void delete(int questionId) {
        synchronized (db) {
            db.questions.remove(questionId);
        }
    }

    @Override
    public boolean isUsedInLockedExam(int questionId) {
        synchronized (db) {
            return db.exams.values().stream()
                    .filter(Exam::isLocked)
                    .flatMap(e -> e.getItems().stream())
                    .anyMatch(it -> it.getQuestion().getQuestionId() == questionId);
        }
    }

    @Override
    public boolean isUsedInAnyExam(int questionId) {
        synchronized (db) {
            return db.exams.values().stream()
                    .flatMap(e -> e.getItems().stream())
                    .anyMatch(it -> it.getQuestion().getQuestionId() == questionId);
        }
    }
}
