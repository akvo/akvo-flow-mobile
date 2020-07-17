/*
 * Copyright (C) 2017,2018 Stichting Akvo (Akvo Foundation)
 *
 * This file is part of Akvo Flow.
 *
 * Akvo Flow is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Akvo Flow is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Akvo Flow.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.akvo.flow.presentation.signature;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.akvo.flow.R;
import org.akvo.flow.app.FlowApp;
import org.akvo.flow.injector.component.ApplicationComponent;
import org.akvo.flow.injector.component.DaggerViewComponent;
import org.akvo.flow.injector.component.ViewComponent;
import org.akvo.flow.ui.view.signature.SignatureDrawView;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.ViewUtil;
import org.akvo.flow.util.image.GlideImageLoader;
import org.akvo.flow.util.image.ImageLoader;

import java.io.File;

import javax.inject.Inject;

import androidx.appcompat.app.AppCompatActivity;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTextChanged;

import static org.akvo.flow.R.id.signature;

public class SignatureActivity extends AppCompatActivity
        implements SignatureDrawView.SignatureViewListener,
        SignatureView {

    @BindView(signature)
    SignatureDrawView mSignatureDrawView;

    @BindView(R.id.name_edit_text)
    EditText nameEditText;

    @BindView(R.id.save)
    Button saveButton;

    @Inject
    SignaturePresenter presenter;

    private ImageLoader imageLoader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signature);
        ButterKnife.bind(this);
        initializeInjector();
        String questionId = getIntent().getStringExtra(ConstantUtil.SIGNATURE_QUESTION_ID_EXTRA);
        String datapointId = getIntent().getStringExtra(ConstantUtil.SIGNATURE_DATAPOINT_ID_EXTRA);
        String name = getIntent().getStringExtra(ConstantUtil.SIGNATURE_NAME_EXTRA);
        presenter.setView(this);
        presenter.setExtras(questionId, datapointId, name);
        imageLoader = new GlideImageLoader(this);
        setSignatureDrawView();
    }

    private void initializeInjector() {
        ViewComponent viewComponent = DaggerViewComponent.builder()
                .applicationComponent(getApplicationComponent()).build();
        viewComponent.inject(this);
    }

    /**
     * Get the Main Application component for dependency injection.
     *
     * @return {@link ApplicationComponent}
     */
    private ApplicationComponent getApplicationComponent() {
        return ((FlowApp) getApplication()).getApplicationComponent();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        presenter.destroy();
    }

    private void setSignatureDrawView() {
        mSignatureDrawView.setListener(this);
        mSignatureDrawView.getViewTreeObserver()
                .addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        ViewUtil.removeLayoutListener(mSignatureDrawView.getViewTreeObserver(),
                                this);
                        File originalSignatureImage = presenter.getOriginalSignatureFile();
                        if (originalSignatureImage.exists()) {
                            imageLoader.loadFromFile(originalSignatureImage,
                                    bitmap -> runOnUiThread(() -> {
                                        if (bitmap != null) {
                                            mSignatureDrawView.setBitmap(bitmap);
                                            mSignatureDrawView.invalidate();
                                            onViewContentChanged();
                                        }
                                    }));
                        }
                    }
                });
    }

    @OnTextChanged(value = R.id.name_edit_text,
            callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    void onNameTextChanged() {
        onViewContentChanged();
    }

    private void onViewContentChanged() {
        String name = nameEditText.getText().toString();
        boolean emptySignature = mSignatureDrawView.isEmpty();
        presenter.onViewContentChanged(name, emptySignature);
    }

    @OnClick(R.id.cancel)
    void onCancelTap() {
        setResult(RESULT_CANCELED);
        finish();
    }

    @OnClick(R.id.clear)
    void onClearTap() {
        mSignatureDrawView.clear();
        onViewContentChanged();
    }

    @OnClick(R.id.save)
    void onSaveButtonTap() {
        String name = nameEditText.getText().toString();
        boolean emptySignature = mSignatureDrawView.isEmpty();
        Bitmap bitmap = mSignatureDrawView.getBitmap();
        presenter.onSaveButtonTap(name, emptySignature, bitmap);
    }

    @Override
    public void showSaving() {
        saveButton.setText(R.string.saving);
        disableSaveButton();
    }

    @Override
    public void finishWithResultOK(String signatureName) {
        Intent intent = new Intent();
        intent.putExtra(ConstantUtil.SIGNATURE_NAME_EXTRA, signatureName);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void onSignatureDrawn() {
        onViewContentChanged();
    }

    @Override
    public void enableSaveButton() {
        saveButton.setEnabled(true);
    }

    @Override
    public void disableSaveButton() {
        saveButton.setEnabled(false);
    }

    @Override
    public void setNameText(String name) {
        nameEditText.setText(name);
        nameEditText.setSelection(nameEditText.getText().length());
    }

    @Override
    public void hideSaving() {
        saveButton.setText(R.string.savebutton);
        enableSaveButton();
    }

    @Override
    public void displayErrorSavingImage() {
        Toast.makeText(getApplicationContext(), R.string.error_saving_signature, Toast.LENGTH_LONG)
                .show();
    }
}
