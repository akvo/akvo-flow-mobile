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

package org.akvo.flow.presentation.legal;

import android.content.Context;
import android.graphics.Typeface;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.akvo.flow.R;
import org.akvo.flow.presentation.legal.views.LicenceItem;
import org.akvo.flow.presentation.legal.views.LicenseDescription;
import org.akvo.flow.presentation.legal.views.LicenseDetail;
import org.akvo.flow.presentation.legal.views.LicenseTitle;
import org.akvo.flow.presentation.legal.views.apache.AndroidMapUtilsItem;
import org.akvo.flow.presentation.legal.views.apache.AndroidOSPItem;
import org.akvo.flow.presentation.legal.views.apache.ButterKnifeItem;
import org.akvo.flow.presentation.legal.views.apache.DaggerItem;
import org.akvo.flow.presentation.legal.views.apache.GsonItem;
import org.akvo.flow.presentation.legal.views.apache.JacksonTimeItem;
import org.akvo.flow.presentation.legal.views.apache.PowerMockItem;
import org.akvo.flow.presentation.legal.views.apache.PrettyTimeItem;
import org.akvo.flow.presentation.legal.views.apache.RxAndroidItem;
import org.akvo.flow.presentation.legal.views.apache.SqlBriteItem;
import org.akvo.flow.presentation.legal.views.apache.TimberItem;
import org.akvo.flow.presentation.legal.views.eclipse.MockitoItem;
import org.akvo.flow.presentation.legal.views.mit.Junit4Item;
import org.akvo.flow.ui.Navigator;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

class LicencesAdapter extends RecyclerView.Adapter<LicencesAdapter.LicenseItemHolder> {

    private static final int VIEW_TYPE_TITLE = 0;
    private static final int VIEW_TYPE_ITEM = 1;
    private static final int VIEW_TYPE_DESCRIPTION = 2;

    @NonNull
    private final List<LicenceItem> licenceItems;

    private final Navigator navigator;

    LicencesAdapter(Context context, Navigator navigator) {
        this.navigator = navigator;
        licenceItems = new ArrayList<>();
        licenceItems.add(new LicenseDescription(context.getString(R.string.licences_description)));
        licenceItems.add(new LicenseTitle(context.getString(R.string.apache_licence_title)));
        licenceItems.add(new AndroidMapUtilsItem());
        licenceItems.add(new AndroidOSPItem());
        licenceItems.add(new ButterKnifeItem());
        licenceItems.add(new DaggerItem());
        licenceItems.add(new GsonItem());
        licenceItems.add(new JacksonTimeItem());
        licenceItems.add(new PowerMockItem());
        licenceItems.add(new PrettyTimeItem());
        licenceItems.add(new RxAndroidItem());
        licenceItems.add(new SqlBriteItem());
        licenceItems.add(new TimberItem());
        licenceItems.add(new LicenseTitle(context.getString(R.string.eclipse_licence_title)));
        licenceItems.add(new Junit4Item());
        licenceItems.add(new LicenseTitle(context.getString(R.string.mit_licence_title)));
        licenceItems.add(new MockitoItem());
    }

    @Override
    public LicenseItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        if (viewType == VIEW_TYPE_TITLE) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.license_title_view_item, parent, false);
            return new LicenseTitleItemHolder(view);
        } else if (viewType == VIEW_TYPE_ITEM) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.license_detail_view_item, parent, false);
            return new LicenseDetailItemHolder(view, navigator);
        } else {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.license_title_view_item, parent, false);
            return new LicenseDescriptionHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(LicenseItemHolder holder, int position) {
        holder.setUpViews(licenceItems.get(position));
    }

    @Override
    public int getItemCount() {
        return licenceItems.size();
    }

    @Override
    public int getItemViewType(int position) {
        LicenceItem licenceItem = licenceItems.get(position);
        if (licenceItem instanceof LicenseTitle) {
            return VIEW_TYPE_TITLE;
        } else if (licenceItem instanceof LicenseDescription) {
            return VIEW_TYPE_DESCRIPTION;
        } else {
            return VIEW_TYPE_ITEM;
        }
    }

    static abstract class LicenseItemHolder<T extends LicenceItem> extends RecyclerView.ViewHolder {

        @BindView(R.id.title_text_view)
        TextView titleView;

        LicenseItemHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        void setUpViews(T item) {
            titleView.setText(item.getTitle());
        }
    }

    static class LicenseTitleItemHolder extends LicenseItemHolder<LicenseTitle> {

        LicenseTitleItemHolder(View itemView) {
            super(itemView);
        }

        void setUpViews(LicenseTitle item) {
            super.setUpViews(item);
            titleView.setTypeface(null, Typeface.BOLD);
        }
    }

    static class LicenseDescriptionHolder extends LicenseItemHolder<LicenseDescription> {

        LicenseDescriptionHolder(View itemView) {
            super(itemView);
        }
    }

    static class LicenseDetailItemHolder extends LicenseItemHolder<LicenseDetail> {

        private final Navigator navigator;

        @BindView(R.id.url_text_view)
        TextView urlView;

        LicenseDetailItemHolder(View itemView, Navigator navigator) {
            super(itemView);
            this.navigator = navigator;
        }

        void setUpViews(LicenseDetail item) {
            super.setUpViews(item);
            urlView.setText(item.getUrl());
        }

        @OnClick(R.id.url_text_view)
        void onLicenseUrlTap() {
            navigator.openUrl(urlView.getContext(), urlView.getText().toString());
        }
    }
}
