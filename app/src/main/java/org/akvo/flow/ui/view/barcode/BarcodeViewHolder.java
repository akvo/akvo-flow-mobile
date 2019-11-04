/*
 * Copyright (C) 2017,2019 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.ui.view.barcode;

import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import org.akvo.flow.R;

class BarcodeViewHolder extends RecyclerView.ViewHolder {

    private final EditText barcodeEdit;
    private final TextView positionTextView;
    private final ImageButton deleteButton;
    private final RemoveButtonListener removeButtonListener;

    BarcodeViewHolder(View itemView, RemoveButtonListener removeButtonListener) {
        super(itemView);
        this.barcodeEdit = (EditText) itemView.findViewById(R.id.barcode_input);
        this.positionTextView = (TextView) itemView.findViewById(R.id.order);
        this.deleteButton = (ImageButton) itemView.findViewById(R.id.delete);
        this.removeButtonListener = removeButtonListener;
    }

    void setUpViews(String text, final int position) {
        barcodeEdit.setText(text);
        positionTextView.setText(position + 1 + "");
        if (removeButtonListener != null) {
            deleteButton.setVisibility(View.VISIBLE);
            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (removeButtonListener != null) {
                        removeButtonListener.onQuestionRemoveTap(position);
                    }
                }
            });
        } else {
            deleteButton.setVisibility(View.GONE);
        }
    }
}
