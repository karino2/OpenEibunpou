package com.livejournal.karino2.openeibunpou;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by karino on 10/25/15.
 */
public class Stage {
    List<QuestionRecord> allQuestions = new ArrayList<>();
    List<QuestionRecord> questions = new ArrayList<>();
    int currentPosition = 0;
    String stageName;

    final int questionSize = 5;

    public Stage(String stageName) {
        this.stageName = stageName;
    }

    public void addQuestion(QuestionRecord rec) {
        if(rec.getQuestionType() == 99)
            return; // ignore.
        allQuestions.add(rec);
    }

    public void setupNewStage() {
        sortAllQuestions();
        questions.clear();
        questions.addAll(allQuestions.subList(0, questionSize));
        Collections.shuffle(questions);
    }


    void sortAllQuestions() {
        Collections.sort(allQuestions, new Comparator<QuestionRecord>() {
            @Override
            public int compare(QuestionRecord lhs, QuestionRecord rhs) {
                // asc
                return (lhs.getCompletion() - rhs.getCompletion());
            }
        });
    }

    public String updateCompletionJson() {
        StringBuilder builder = new StringBuilder();

        builder.append("{\"stagen\":\"");
        builder.append(stageName);
        builder.append("\",\"stagec\":");
        builder.append(calcStageCompletion());
        builder.append(",\"comps\":[");
        boolean firstTime = true;
        for(QuestionRecord q : questions) {

            if(firstTime) {
                firstTime = false;
            } else {
                builder.append(",");
            }
            builder.append("{\"sub\":\"");
            builder.append(q.getSubName());
            builder.append("\",\"comp\":");
            builder.append(q.getCompletion());
            builder.append("}");
        }
        builder.append("]}");
        return builder.toString();
    }

    public int calcStageCompletion() {
        int hundredCount = 0;
        for(QuestionRecord r : allQuestions) {
            if(r.getCompletion() == 100)
                hundredCount++;
        }

        return hundredCount*100/allQuestions.size();
    }


    public void gotoNext() {
        currentPosition++;
    }

    public int getCurrentPosition() {
        return currentPosition;
    }

    public int getQuestionNum() { return questions.size(); }

    public QuestionRecord getCurrentQuestion() {
        return questions.get(currentPosition);
    }

    public boolean hasNext() {
        return questions.size()-1 > currentPosition;
    }
}
