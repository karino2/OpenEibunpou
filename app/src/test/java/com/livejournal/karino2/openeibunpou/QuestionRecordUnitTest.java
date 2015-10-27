package com.livejournal.karino2.openeibunpou;

import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Created by _ on 2015/10/27.
 */
public class QuestionRecordUnitTest {
    @Test
    public void answerCorrectUpdateCompletion_firstTime() {
        QuestionRecord r = new QuestionRecord();

        r.answerCorrectUpdateCompletion();
        assertEquals(100, r.completion);
    }

    @Test
    public void answerCorrectUpdateCompletion_fromZero() {
        QuestionRecord r = new QuestionRecord();

        r.completion = 0;
        r.answerCorrectUpdateCompletion();
        assertTrue(r.completion < 50);
        assertNotEquals(0, r.completion);
    }

    @Test
    public void answerCorrectUpdateCompletion_fromHundred_Same() {
        QuestionRecord r = new QuestionRecord();

        r.completion = 100;
        r.answerCorrectUpdateCompletion();
        assertEquals(100, r.completion);
    }

    @Test
    public void answerWrongUpdateCompletion_firstTime() {
        QuestionRecord r = new QuestionRecord();

        r.answerWrongUpdateCompletion();
        assertEquals(0, r.completion);
    }

    @Test
    public void answerWrongUpdateCompletion_fromHundred() {
        QuestionRecord r = new QuestionRecord();

        r.completion = 100;
        r.answerWrongUpdateCompletion();
        assertNotEquals(0, r.completion);
        assertNotEquals(100, r.completion);
    }

    @Test
    public void answerWrongUpdateCompletion_fromZero_same() {
        QuestionRecord r = new QuestionRecord();

        r.completion = 0;
        r.answerWrongUpdateCompletion();
        assertEquals(0, r.completion);
    }


}
