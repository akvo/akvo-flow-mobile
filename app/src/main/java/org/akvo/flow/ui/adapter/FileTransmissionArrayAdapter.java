/*
 *  Copyright (C) 2010-2019 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.ui.adapter;

import android.content.Context;
import androidx.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.akvo.flow.R;
import org.akvo.flow.database.TransmissionStatus;
import org.akvo.flow.domain.FileTransmission;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * Adapter that converts FileTransmission objects for display in a list view.
 *
 * @author Christopher Fagiani
 */
public class FileTransmissionArrayAdapter extends ArrayAdapter<FileTransmission> {
    private final DateFormat dateFormat;
    private final int layoutId;

    public FileTransmissionArrayAdapter(Context context, int resourceId,
            List<FileTransmission> objects) {
        super(context, resourceId, objects);
        layoutId = resourceId;
        dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    }

    private void bindView(View view, FileTransmission trans) {
        TextView tv = (TextView) view.findViewById(R.id.statustext);
        switch (trans.getStatus()) {
            case TransmissionStatus.QUEUED:
                tv.setText(R.string.status_queued);
                tv.setCompoundDrawablesWithIntrinsicBounds(R.drawable.status_queued, 0, 0, 0);
                break;
            case TransmissionStatus.IN_PROGRESS:
                tv.setText(R.string.status_in_progress);
                tv.setCompoundDrawablesWithIntrinsicBounds(R.drawable.status_progress, 0, 0, 0);
                break;
            case TransmissionStatus.SYNCED:
                tv.setText(R.string.status_uploaded);
                tv.setCompoundDrawablesWithIntrinsicBounds(R.drawable.status_synced, 0, 0, 0);
                break;
            case TransmissionStatus.FAILED:
                tv.setText(R.string.status_failed);
                tv.setCompoundDrawablesWithIntrinsicBounds(R.drawable.status_failed, 0, 0, 0);
                break;
            default:
                break;
        }

        String startDate = getContext().getString(R.string.transstartdate);
        String endDate = getContext().getString(R.string.transenddate);

        if (trans.getStartDate() != null) {
            startDate += " " + dateFormat.format(trans.getStartDate());
        }
        if (trans.getEndDate() != null) {
            endDate += " " + dateFormat.format(trans.getEndDate());
        }

        ((TextView) view.findViewById(R.id.startdate)).setText(startDate);
        ((TextView) view.findViewById(R.id.enddate)).setText(endDate);
        ((TextView) view.findViewById(R.id.filename)).setText(trans.getFileName());
    }

    @NonNull
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        View view;
        if (convertView == null) {
            Context ctx = getContext();
            LayoutInflater inflater = (LayoutInflater) ctx
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(layoutId, null);
        } else {
            view = convertView;
        }
        bindView(view, getItem(position));
        return view;
    }
}
