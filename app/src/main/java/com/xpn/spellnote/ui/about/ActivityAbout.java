package com.xpn.spellnote.ui.about;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.oss.licenses.OssLicensesMenuActivity;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.xpn.spellnote.R;
import com.xpn.spellnote.databinding.ActivityAboutBinding;


public class ActivityAbout extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityAboutBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_about);

        /// set-up analytics
        FirebaseAnalytics.getInstance(this);

        /// set-up toolbar
        setSupportActionBar(binding.toolbar);
        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        binding.aboutAuthor.setMovementMethod( LinkMovementMethod.getInstance() );
        binding.aboutApp.setMovementMethod( LinkMovementMethod.getInstance() );
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_about, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if( id == android.R.id.home )
            finish();
        if( id == R.id.action_open_source_licenses )
            startActivity( new Intent(this, OssLicensesMenuActivity.class) );

        return super.onOptionsItemSelected(item);
    }
}
