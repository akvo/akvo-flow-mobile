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
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;

import org.akvo.flow.R;
import org.akvo.flow.domain.response.value.Signature;
import org.akvo.flow.ui.view.signature.SignatureView;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.FileUtil;
import org.akvo.flow.util.ImageUtil;
import org.akvo.flow.util.image.GlideImageLoader;
import org.akvo.flow.util.image.ImageLoader;
import org.akvo.flow.util.image.ImageLoaderListener;

import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class SignatureActivity extends Activity implements SignatureView.SignatureViewListener {

    private static final int SIGNATURE_WIDTH = 320;
    private static final int SIGNATURE_HEIGHT = 240;

    @BindView(R.id.signature)
    SignatureView mSignatureView;

    @BindView(R.id.name_edit_text)
    EditText nameEditText;

    @BindView(R.id.save)
    Button saveButton;

    private String questionId;
    private String name;
    private String datapointId;

    private ImageLoader imageLoader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_signature);
        ButterKnife.bind(this);
        questionId = getIntent().getStringExtra(ConstantUtil.SIGNATURE_QUESTION_ID_EXTRA);
        datapointId = getIntent().getStringExtra(ConstantUtil.SIGNATURE_DATAPOINT_ID_EXTRA);
        name = getIntent().getStringExtra(ConstantUtil.SIGNATURE_NAME_EXTRA);
        imageLoader = new GlideImageLoader(this);
        setSignatureView();
        setNameEditText();
    }

    private void setNameEditText() {
        if (!TextUtils.isEmpty(name)) {
            nameEditText.setText(name);
        }
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
    }

    private void setSignatureView() {
        mSignatureView.setListener(this);
        mSignatureView.getViewTreeObserver()
                .addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        mSignatureView.getViewTreeObserver()
                                .removeGlobalOnLayoutListener(this);
                        File originalSignatureImage = getOriginalSignatureImage();
                        if (originalSignatureImage.exists()) {
                            imageLoader.loadFromFile(originalSignatureImage,
                                    new ImageLoaderListener() {
                                        @Override
                                        public void onImageReady(Bitmap bitmap) {
                                            if (bitmap != null) {
                                                mSignatureView.setBitmap(bitmap);
                                                mSignatureView.invalidate();
                                            }
                                        }
                                    });
                        }
                    }
                });

    }

    @NonNull
    private File getOriginalSignatureImage() {
        return new File(FileUtil.getFilesDir(FileUtil.FileType.TMP),
                "sign_" + questionId + "_" + datapointId + "_original.png");
    }

    private void updateSendButton() {
        boolean isNameEmpty = TextUtils.isEmpty(nameEditText.getText().toString());
        boolean signatureEmpty = mSignatureView.isEmpty();
        saveButton.setEnabled(!isNameEmpty && !signatureEmpty);
    }

    @OnClick(R.id.cancel)
    void onCancelTap() {
        setResult(RESULT_CANCELED);
        finish();
    }

    @OnClick(R.id.clear)
    void onClearTap() {
        mSignatureView.clear();
        updateSendButton();
    }

    @OnClick(R.id.save)
    void onSaveButtonTap() {
        if (!mSignatureView.isEmpty() && !TextUtils
                .isEmpty(nameEditText.getText().toString())) {
            saveButton.setText(R.string.saving);
            saveButton.setEnabled(false);
            Bitmap bitmap = mSignatureView.getBitmap();
            //TODO: this should be done on separate thread
            String data = ImageUtil.encodeBase64(bitmap, SIGNATURE_WIDTH, SIGNATURE_HEIGHT);
            ImageUtil.saveImage(bitmap, getOriginalSignatureImage().getAbsolutePath(), 100);
            Intent intent = new Intent();
            Signature signature = new Signature();
            signature.setName(nameEditText.getText().toString());
            signature.setImage(data);
            intent.putExtra(ConstantUtil.SIGNATURE_EXTRA, signature);
            setResult(RESULT_OK, intent);
            finish();
        }
    }

    @Override
    public void onSignatureDrawn() {
        updateSendButton();
    }
}
