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
