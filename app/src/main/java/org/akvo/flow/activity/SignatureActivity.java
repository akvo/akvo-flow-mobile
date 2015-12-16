/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.akvo.flow.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.app.Activity;
import android.view.View;
import android.view.Window;

import org.akvo.flow.R;
import org.akvo.flow.ui.view.SignatureView;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.ImageUtil;

public class SignatureActivity extends Activity {
    private static final int SIGNATURE_WIDTH = 320;
    private static final int SIGNATURE_HEIGHT = 240;

    private SignatureView mSignatureView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_signature);
        mSignatureView = (SignatureView)findViewById(R.id.signature);
        findViewById(R.id.clear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clear();
            }
        });
        findViewById(R.id.cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });
        findViewById(R.id.save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mSignatureView.isEmpty()) {
                    save();
                }
            }
        });
    }

    private void clear() {
        mSignatureView.clear();
    }

    private void save() {
        Bitmap bitmap = mSignatureView.getBitmap();
        String data = ImageUtil.encodeBase64(bitmap, SIGNATURE_WIDTH, SIGNATURE_HEIGHT);
        Intent intent = new Intent();
        intent.putExtra(ConstantUtil.SIGNATURE_IMAGE, data);
        setResult(RESULT_OK, intent);
        finish();
    }
}
