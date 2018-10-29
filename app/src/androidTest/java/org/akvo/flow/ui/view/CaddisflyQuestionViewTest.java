package org.akvo.flow.ui.view;

import android.os.Bundle;
import android.test.AndroidTestCase;

import org.akvo.flow.domain.Question;
import org.akvo.flow.event.SurveyListener;
import org.mockito.Mockito;

public class CaddisflyQuestionViewTest extends AndroidTestCase {

    public static final String TEST_JSON_RESPONSE = "{\"testDate\":\"2017-01-16 18:13\",\"app\":{\"appVersion\":\"Version 1.0.0 Alpha 7.2\",\"language\":\"en\"},\"result\":[{\"value\":\"650\",\"id\":1,\"unit\":\"μS\\/cm\",\"name\":\"Electrical Conductivity\"},{\"value\":\"19.6\",\"id\":2,\"unit\":\"°Celsius\",\"name\":\"Temperature\"}],\"name\":\"Electrical Conductivity\",\"device\":{\"product\":\"WW_zc451cg\",\"os\":\"Android - 4.4.2 (19)\",\"model\":\"ASUS_Z007\",\"language\":\"en\",\"manufacturer\":\"asus\",\"country\":\"US\"},\"uuid\":\"f88237b7-be3d-4fac-bbee-ab328eefcd14\",\"type\":\"caddisfly\",\"user\":{\"backDropDetection\":true,\"language\":\"en\"}}";

    public void testResponse() {
        System.setProperty("dexmaker.dexcache", getContext().getCacheDir().getPath());

        SurveyListener l = Mockito.mock(SurveyListener.class);
        Mockito.when(l.getLanguages()).thenReturn(new String[]{});
        Question q = Mockito.mock(Question.class);
        CaddisflyQuestionView qv = new CaddisflyQuestionView(getContext(), q, l);

        Bundle bundle = new Bundle();
        bundle.putString("response", TEST_JSON_RESPONSE);
        bundle.putString("image", "nonexistent image");

        qv.onQuestionResultReceived(bundle);

        assertEquals(TEST_JSON_RESPONSE, qv.getResponse().getValue());
        assertNull(qv.getResponse().getFilename());
    }
}
