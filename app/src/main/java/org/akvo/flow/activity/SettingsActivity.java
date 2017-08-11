/*
 *  Copyright (C) 2010-2017 Stichting Akvo (Akvo Foundation)
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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.SQLException;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.text.Html;
import android.text.TextUtils;
import android.text.method.DigitsKeyListener;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.akvo.flow.R;
import org.akvo.flow.app.FlowApp;
import org.akvo.flow.async.ClearDataAsyncTask;
import org.akvo.flow.data.database.SurveyDbAdapter;
import org.akvo.flow.injector.component.DaggerViewComponent;
import org.akvo.flow.injector.component.ViewComponent;
import org.akvo.flow.service.DataSyncService;
import org.akvo.flow.service.SurveyDownloadService;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.PlatformUtil;
import org.akvo.flow.util.ViewUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

/**
 * Displays the settings menu and handles the user choices
 *
 * @author Christopher Fagiani
 */
public class SettingsActivity extends BackActivity implements AdapterView.OnItemClickListener {

    private static final String LABEL = "label";
    private static final String DESC = "desc";

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        initializeInjector();
        setupToolBar();
        ArrayList<HashMap<String, String>> list = new ArrayList<>();
        Resources resources = getResources();
        list.add(createMap(resources.getString(R.string.prefoptlabel),
                resources.getString(R.string.prefoptdesc)));
        list.add(createMap(resources.getString(R.string.sendoptlabel),
                resources.getString(R.string.sendoptdesc)));
        list.add(createMap(resources.getString(R.string.reloadsurveyslabel),
                resources.getString(R.string.reloadsurveysdesc)));
        list.add(createMap(resources.getString(R.string.downloadsurveylabel),
                resources.getString(R.string.downloadsurveydesc)));
        list.add(createMap(resources.getString(R.string.gpsstatuslabel),
                resources.getString(R.string.gpsstatusdesc)));
        list.add(createMap(resources.getString(R.string.reset_responses),
                resources.getString(R.string.reset_responses_desc)));
        list.add(createMap(resources.getString(R.string.resetall),
                resources.getString(R.string.resetalldesc)));
        list.add(createMap(resources.getString(R.string.checksd),
                resources.getString(R.string.checksddesc)));
        String[] fromKeys = {
                LABEL, DESC
        };
        int[] toIds = {
                R.id.optionLabel, R.id.optionDesc
        };

        ListView lv = (ListView) findViewById(android.R.id.list);
        lv.setAdapter(new SettingsAdapter(this, list, R.layout.settingsdetail, fromKeys, toIds));
        lv.setOnItemClickListener(this);
    }

    private void initializeInjector() {
        ViewComponent viewComponent = DaggerViewComponent.builder()
                .applicationComponent(getApplicationComponent()).build();
        viewComponent.inject(this);
    }

    /**
     * creates data structure for use in list adapter
     */
    private HashMap<String, String> createMap(String label, String desc) {
        HashMap<String, String> map = new HashMap<>();
        map.put(LABEL, label);
        map.put(DESC, desc);
        return map;
    }

    /**
     * when an item is clicked, use the label value to determine what option it
     * was and then handle that type of action
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        TextView label = (TextView) view.findViewById(R.id.optionLabel);
        if (label != null) {
            String val = label.getText().toString();
            Resources resources = getResources();
            if (resources.getString(R.string.prefoptlabel).equals(val)) {
                onPreferencesOptionTap();
            } else if (resources.getString(R.string.gpsstatuslabel).equals(val)) {
                onGpsStatusOptionTap();
            } else if (resources.getString(R.string.reloadsurveyslabel).equals(val)) {
                onReloadAllSurveysOptionTap();
            } else if (resources.getString(R.string.downloadsurveylabel).equals(val)) {
                onDownloadFormOptionTap();
            } else if (resources.getString(R.string.reset_responses).equals(val)) {
                onDeleteCollectedDataOptionTap();
            } else if (resources.getString(R.string.resetall).equals(val)) {
                onDeleteEverythingOptionTap();
            } else if (resources.getString(R.string.checksd).equals(val)) {
                onCheckSdCardStateOptionTap(resources);
            } else if (resources.getString(R.string.sendoptlabel).equals(val)) {
                onSyncDataOptionTap(view);
            }
        }
    }

    private void onSyncDataOptionTap(View view) {
        Intent i = new Intent(view.getContext(), DataSyncService.class);
        getApplicationContext().startService(i);
        finish();
    }

    private void onCheckSdCardStateOptionTap(Resources resources) {
        String state = Environment.getExternalStorageState();
        StringBuilder builder = new StringBuilder();
        if (state == null || !Environment.MEDIA_MOUNTED.equals(state)) {
            builder.append("<b>").append(resources.getString(R.string.sdmissing))
                    .append("</b><br>");
        } else {
            builder.append(resources.getString(R.string.sdmounted)).append("<br>");
            File f = Environment.getExternalStorageDirectory();
            if (f != null) {
                // normally, we could just do f.getFreeSpace() but that
                // would tie us to later versions of Android. So for
                // maximum compatibility, just use StatFS
                StatFs fs = new StatFs(f.getAbsolutePath());
                // We first cast the blocks and size values to float, to avoid an
                // integer overflow scenario. Ideally we should use getFreeBlocksLong()
                // instead, but it's only available in API level 18+
                long fb = fs.getFreeBlocks();
                long bs = fs.getBlockSize();
                long space = fb * bs;
                builder.append(resources.getString(R.string.sdcardspace))
                        .append(String.format(" %.2f", (double) space / (double) (1024 * 1024)));
            }
        }
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle(R.string.checksd);
        String text = Html.fromHtml(builder.toString()).toString();
        dialog.setMessage(text);
        dialog.setPositiveButton(R.string.okbutton, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        dialog.show();
    }

    private void onDeleteEverythingOptionTap() {
        ViewUtil.showAdminAuthDialog(this, new ViewUtil.AdminAuthDialogListener() {
            @Override
            public void onAuthenticated() {
                deleteData(false);
            }
        });
    }

    private void onDeleteCollectedDataOptionTap() {
        ViewUtil.showAdminAuthDialog(this, new ViewUtil.AdminAuthDialogListener() {
            @Override
            public void onAuthenticated() {
                deleteData(true);
            }
        });
    }

    private void onDownloadFormOptionTap() {
        ViewUtil.showAdminAuthDialog(this, new ViewUtil.AdminAuthDialogListener() {

            @Override
            public void onAuthenticated() {
                AlertDialog.Builder inputDialog = new AlertDialog.Builder(SettingsActivity.this);
                inputDialog.setTitle(R.string.downloadsurveylabel);
                inputDialog.setMessage(R.string.downloadsurveyinstr);

                // Set an EditText view to get user input
                final EditText input = new EditText(SettingsActivity.this);

                input.setKeyListener(new DigitsKeyListener(false, false));
                inputDialog.setView(input);
                inputDialog.setPositiveButton(R.string.okbutton, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String surveyId = input.getText().toString().trim();
                        if (!TextUtils.isEmpty(surveyId)) {
                            Intent i = new Intent(SettingsActivity.this,
                                    SurveyDownloadService.class);
                            i.putExtra(SurveyDownloadService.EXTRA_SURVEY_ID, surveyId);
                            SettingsActivity.this.startService(i);
                        }
                    }
                });

                inputDialog.setNegativeButton(R.string.cancelbutton, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Canceled.
                    }
                });

                inputDialog.show();
            }
        });
    }

    private void onReloadAllSurveysOptionTap() {
        ViewUtil.showAdminAuthDialog(this, new ViewUtil.AdminAuthDialogListener() {
            @Override
            public void onAuthenticated() {
                AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);
                builder.setTitle(R.string.conftitle);
                builder.setMessage(R.string.reloadconftext);
                builder.setPositiveButton(R.string.okbutton, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Context c = SettingsActivity.this;
                        Intent i = new Intent(c, SurveyDownloadService.class);
                        i.putExtra(SurveyDownloadService.EXTRA_DELETE_SURVEYS, true);
                        c.startService(i);
                    }
                });
                builder.setNegativeButton(R.string.cancelbutton,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
                builder.show();
            }
        });
    }

    private void onGpsStatusOptionTap() {
        Intent intent = getPackageManager()
                .getLaunchIntentForPackage(ConstantUtil.GPS_STATUS_PACKAGE);
        if (intent != null) {
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            startActivity(intent);
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.nogpsstatus);
            builder.setPositiveButton(R.string.okbutton, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                }
            });
            builder.show();
        }
    }

    private void onPreferencesOptionTap() {
        Intent i = new Intent(this, PreferencesActivity.class);
        startActivity(i);
    }

    private boolean unsentData() throws SQLException {
        SurveyDbAdapter db = new SurveyDbAdapter(this);
        try {
            db.open();
            return db.getUnsyncedTransmissions().size() > 0;
        } finally {
            db.close();
        }
    }

    /**
     * Permanently deletes data from the device. If unsubmitted data is found on
     * the database, the user will be prompted with a message to confirm the
     * operation.
     *
     * @param responsesOnly Flag to specify a partial deletion (user generated
     *                      data).
     */
    private void deleteData(final boolean responsesOnly) throws SQLException {
        try {
            int messageId;
            if (unsentData()) {
                messageId = R.string.unsentdatawarning;
            } else if (responsesOnly) {
                messageId = R.string.delete_responses_warning;
            } else {
                messageId = R.string.deletealldatawarning;
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(messageId)
                    .setCancelable(true)
                    .setPositiveButton(R.string.okbutton, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            if (!responsesOnly) {
                                // Delete everything implies logging the current user out (if any)
                                FlowApp.getApp().setUser(null);
                            }
                            new ClearDataAsyncTask(SettingsActivity.this).execute(responsesOnly);
                        }
                    })
                    .setNegativeButton(R.string.cancelbutton,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            });
            builder.show();
        } catch (SQLException e) {
            Timber.e(e, e.getMessage());
            Toast.makeText(this, R.string.clear_data_error, Toast.LENGTH_SHORT).show();
        }
    }

    private static class SettingsAdapter extends SimpleAdapter {

        public SettingsAdapter(Context context, List<? extends Map<String, ?>> data, int resource,
                String[] from,
                int[] to) {
            super(context, data, resource, from, to);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);

            // Alternate background
            int attr = position % 2 == 0 ? R.attr.listitem_bg1 : R.attr.listitem_bg2;
            final int res = PlatformUtil.getResource(view.getContext(), attr);
            view.setBackgroundResource(res);

            return view;
        }
    }
}
