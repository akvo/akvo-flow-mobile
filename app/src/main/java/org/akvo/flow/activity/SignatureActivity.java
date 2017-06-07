/*
 *  Copyright (C) 2015 Stichting Akvo (Akvo Foundation)
 *
 *  This file is part of Akvo Flow.
 *
 *  Akvo Flow is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Akvo Flow is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Akvo Flow.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.akvo.flow.activity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.widget.EditText;

import org.akvo.flow.R;
import org.akvo.flow.ui.view.SignatureView;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.ImageUtil;

public class SignatureActivity extends Activity implements SignatureView.SignatureViewListener {

    private static final int SIGNATURE_WIDTH = 320;
    private static final int SIGNATURE_HEIGHT = 240;

    private SignatureView mSignatureView;
    private EditText nameEditText;
    private View sendButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_signature);
        mSignatureView = (SignatureView) findViewById(R.id.signature);
        nameEditText = (EditText) findViewById(R.id.name_edit_text);
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
        sendButton = findViewById(R.id.save);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mSignatureView.isEmpty() && !TextUtils
                        .isEmpty(nameEditText.getText().toString())) {
                    save();
                }
            }
        });
        nameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                    int after) {
                //EMPTY
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //EMPTY
            }

            @Override
            public void afterTextChanged(Editable s) {
                updateSendButton();
            }
        });
        mSignatureView.setListener(this);
    }

    private void updateSendButton() {
        boolean isNameEmpty = TextUtils.isEmpty(nameEditText.getText().toString());
        boolean signatureEmpty = mSignatureView.isEmpty();
        sendButton.setEnabled(!isNameEmpty && !signatureEmpty);
    }

    private void clear() {
        mSignatureView.clear();
        updateSendButton();
    }

    private void save() {
        Bitmap bitmap = mSignatureView.getBitmap();
        String data = ImageUtil.encodeBase64(bitmap, SIGNATURE_WIDTH, SIGNATURE_HEIGHT);
        Intent intent = new Intent();
        intent.putExtra(ConstantUtil.SIGNATURE_IMAGE, data);
        intent.putExtra(ConstantUtil.SIGNATURE_NAME, nameEditText.getText().toString());
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void onSignatureDrawn() {
        updateSendButton();
    }
}
