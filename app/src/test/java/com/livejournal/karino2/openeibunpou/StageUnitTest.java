package com.livejournal.karino2.openeibunpou;

import android.support.annotation.NonNull;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * To work on unit tests, switch the Test Artifact in the Build Variants view.
 */
public class StageUnitTest {
    @Test
    public void sort_isCorrect() throws Exception {
        Stage stage = createStage();

        QuestionRecord r = createQuestionRecord(80);
        stage.addQuestion(r);

        r = createQuestionRecord(60);
        stage.addQuestion(r);

        assertEquals(80, stage.allQuestions.get(0).completion);

        stage.sortAllQuestions();
        assertEquals(60, stage.allQuestions.get(0).completion);
    }

    @NonNull
    public QuestionRecord createQuestionRecord(int completion) {
        QuestionRecord r = new QuestionRecord();
        r.completion = completion;
        return r;
    }

    @NonNull
    public QuestionRecord createQuestionRecord(String subName, int completion) {
        QuestionRecord r = new QuestionRecord();
        r.sub = subName;
        r.completion = completion;
        return r;
    }

    @NonNull
    public Stage createStage() {
        return new Stage("dummy");
    }

    @Test
    public void calcStageCompletion_half (){
        Stage stage = createStage();

        stage.addQuestion(createQuestionRecord(98));
        stage.addQuestion(createQuestionRecord(100));

        assertEquals(50, stage.calcStageCompletion());
    }

    @Test
    public void calcStageCompletion_perfect (){
        Stage stage = createStage();

        stage.addQuestion(createQuestionRecord(100));
        stage.addQuestion(createQuestionRecord(100));
        stage.addQuestion(createQuestionRecord(100));

        assertEquals(100, stage.calcStageCompletion());
    }

    @Test
    public void updateCompletionJson() {
        String expect = "{\"stagen\":\"dummy\",\"stagec\":50,\"comps\":[{\"sub\":\"A-1\",\"comp\":30},{\"sub\":\"B-3\",\"comp\":100}]}";

        Stage stage = createStage();

        stage.addQuestion(createQuestionRecord("A-1", 30));
        stage.addQuestion(createQuestionRecord("B-3", 100));
        // dirty...
        stage.questions.addAll(stage.allQuestions);

        String actual = stage.updateCompletionJson();
        assertEquals(expect, actual);

    }
}