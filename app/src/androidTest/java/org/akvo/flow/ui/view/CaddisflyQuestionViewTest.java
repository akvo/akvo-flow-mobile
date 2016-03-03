package org.akvo.flow.ui.view;

import android.os.Bundle;
import android.test.AndroidTestCase;

import org.akvo.flow.domain.Question;
import org.akvo.flow.event.SurveyListener;
import org.mockito.Mockito;

public class CaddisflyQuestionViewTest extends AndroidTestCase {

    public void testResponse() {
        System.setProperty("dexmaker.dexcache", getContext().getCacheDir().getPath());

        SurveyListener l = Mockito.mock(SurveyListener.class);
        Mockito.when(l.getLanguages()).thenReturn(new String[]{});
        Question q = Mockito.mock(Question.class);
        CaddisflyQuestionView qv = new CaddisflyQuestionView(getContext(), q, l);

        Bundle bundle = new Bundle();
        bundle.putString("response", "response value");
        bundle.putString("image", "nonexistent image");

        qv.questionComplete(bundle);

        assertEquals("response value", qv.getResponse().getValue());
        assertNull(qv.getResponse().getFilename());
    }
}
