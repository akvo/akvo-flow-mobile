package com.gallatinsystems.survey.device.activity;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.gallatinsystems.survey.device.R;
import com.gallatinsystems.survey.device.async.loader.SurveyedLocaleLoader;
import com.gallatinsystems.survey.device.dao.SurveyDbAdapter;
import com.gallatinsystems.survey.device.domain.SurveyGroup;
import com.gallatinsystems.survey.device.domain.SurveyedLocale;


public class SurveyedLocaleListActivity extends ActionBarActivity implements LoaderCallbacks<Cursor> {
    public static final String TAG = SurveyedLocaleListActivity.class.getSimpleName();
    public static final String EXTRA_SURVEY_GROUP_ID = "survey_group_id";
    
    private int mSurveyGroupId;
    
    private ListView mListView;
    private CursorAdapter mAdapter;
    
    private SurveyDbAdapter mDatabase;
    
    // Loader id
    private static final int ID_SURVEYED_LOCALE_LIST = 0;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.surveyed_locale_list_activity);
        
        mSurveyGroupId = getIntent().getIntExtra(EXTRA_SURVEY_GROUP_ID, SurveyGroup.ID_NONE);
        
        mDatabase = new SurveyDbAdapter(this);
        mDatabase.open();
        mAdapter = new SurveyedLocaleListAdapter(this);
        mListView = (ListView) findViewById(R.id.listview);
        mListView.setAdapter(mAdapter);
        
        display();
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        mDatabase.close();
    }
    
    private void display() {
        getSupportLoaderManager().restartLoader(ID_SURVEYED_LOCALE_LIST, null, this);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.surveyed_locale_list_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.new_record:
                SurveyDbAdapter db = new SurveyDbAdapter(this).open();
                db.createSurveyedLocale(mSurveyGroupId);
                db.close();
                display();
                return true;
            case R.id.map_results:
                // TODO
                Toast.makeText(this, "Not implemented yet", Toast.LENGTH_SHORT).show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    class SurveyedLocaleListAdapter extends CursorAdapter {

        public SurveyedLocaleListAdapter(Context context) {
            super(context, null, false);
        }

        @Override
        public void bindView(View view, Context context, Cursor c) {
            TextView tv = (TextView) view.findViewById(R.id.text1);
            final SurveyedLocale surveyedLocale = SurveyDbAdapter.getSurveyedLocale(c);
            tv.setText(surveyedLocale.getId());
        }

        @Override
        public View newView(Context context, Cursor c, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(SurveyedLocaleListActivity.this);
            return inflater.inflate(R.layout.surveyed_locale_item, null);
        }
        
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case ID_SURVEYED_LOCALE_LIST:
                return new SurveyedLocaleLoader(this, mDatabase, mSurveyGroupId);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (cursor == null) {
            Log.e(TAG, "onFinished() - Loader returned no data");
            return;
        }
        
        switch (loader.getId()) {
            case ID_SURVEYED_LOCALE_LIST:
                mAdapter.changeCursor(cursor);
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }
}
