package org.akvo.flow.activity;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.app.Activity;
import android.view.View;

import org.akvo.flow.R;
import org.akvo.flow.ui.view.SignatureView;

public class SignatureActivity extends Activity {
    private SignatureView mSignatureView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signature);
        mSignatureView = (SignatureView)findViewById(R.id.signature);
        findViewById(R.id.clear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clear();
            }
        });
        findViewById(R.id.save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                save();
            }
        });
    }

    private void clear() {
        mSignatureView.clear();
    }

    private void save() {
        Bitmap bitmap = mSignatureView.getBitmap();
    }
}
