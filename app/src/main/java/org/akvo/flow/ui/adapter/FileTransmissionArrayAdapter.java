/*
 *  Copyright (C) 2010-2015 Stichting Akvo (Akvo Foundation)
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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.akvo.flow.R;
import org.akvo.flow.domain.FileTransmission;
import org.akvo.flow.data.database.TransmissionStatus;

/**
 * Adapter that converts FileTransmission objects for display in a list view.
 * 
 * @author Christopher Fagiani
 */
public class FileTransmissionArrayAdapter extends ArrayAdapter<FileTransmission> {
    private DateFormat dateFormat;
    private int layoutId;

    public FileTransmissionArrayAdapter(Context context, int resourceId,
            List<FileTransmission> objects) {
        super(context, resourceId, objects);
        layoutId = resourceId;
        dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    }

    private void bindView(View view, FileTransmission trans) {
        ImageView imageView = (ImageView) view.findViewById(R.id.statusicon);
        TextView tv = (TextView)view.findViewById(R.id.statustext);

        switch (trans.getStatus()) {
            case TransmissionStatus.QUEUED:
                tv.setText(R.string.status_queued);
                imageView.setImageResource(R.drawable.queued_icn);
                break;
            case TransmissionStatus.IN_PROGRESS:
                tv.setText(R.string.status_in_progress);
                imageView.setImageResource(R.drawable.blueuparrow);
                break;
            case TransmissionStatus.SYNCED:
                tv.setText(R.string.status_synced);
                imageView.setImageResource(R.drawable.checkmark);
                break;
            case TransmissionStatus.FAILED:
                tv.setText(R.string.status_failed);
                imageView.setImageResource(R.drawable.red_cross);
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

    public View getView(int position, View convertView, ViewGroup parent) {
        Context ctx = getContext();
        LayoutInflater inflater = (LayoutInflater) ctx
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(layoutId, null);
        bindView(view, getItem(position));
        return view;
    }

}
