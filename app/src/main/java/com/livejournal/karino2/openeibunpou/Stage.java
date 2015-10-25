package com.livejournal.karino2.openeibunpou;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by karino on 10/25/15.
 */
public class Stage {
    List<QuestionRecord> questions = new ArrayList<>();
    int currentPosition = 0;
    String stageName;

    public Stage(String stageName) {
        this.stageName = stageName;
    }

    public int questionsNum() {
        return questions.size();
    }

    public void addQuestion(QuestionRecord rec) {
        if(rec.getQuestionType() == 99)
            return; // ignore.
        questions.add(rec);
    }

    public void gotoQuestion(int index) {
        currentPosition = index;
    }

    public void gotoNext() {
        currentPosition++;
    }

    public int getCurrentPosition() {
        return currentPosition;
    }

    public QuestionRecord getCurrentQuestion() {
        return questions.get(currentPosition);
    }

    public boolean hasNext() {
        return questions.size()-2 > currentPosition;
    }
}
