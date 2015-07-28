package org.akvo.flow.ui.view;

import android.content.Context;
import android.test.AndroidTestCase;

import org.akvo.flow.domain.QuestionGroup;
import org.akvo.flow.event.QuestionInteractionListener;
import org.akvo.flow.event.SurveyListener;
import org.mockito.Mockito;

public class QuestionGroupTabTest extends AndroidTestCase {

    public void testIsLoaded() {
        System.setProperty("dexmaker.dexcache", getContext().getCacheDir().getPath());

        Context c = getContext();
        SurveyListener l = Mockito.mock(SurveyListener.class);
        QuestionInteractionListener qil = Mockito.mock(QuestionInteractionListener.class);
        QuestionGroup qg = Mockito.mock(QuestionGroup.class);

        QuestionGroupTab tab = new QuestionGroupTab(c, qg, l, qil);

        assertEquals(false, tab.isLoaded());
        tab.load();
        assertEquals(true, tab.isLoaded());
    }
}
