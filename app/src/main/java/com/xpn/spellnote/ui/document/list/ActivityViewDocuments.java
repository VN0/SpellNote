package com.xpn.spellnote.ui.document.list;

import android.content.Intent;
import androidx.databinding.DataBindingUtil;
import android.os.Bundle;
import androidx.annotation.NonNull;
import com.google.android.material.navigation.NavigationView;
import androidx.fragment.app.FragmentManager;
import androidx.core.view.GravityCompat;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.Gravity;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.perf.metrics.AddTrace;
import com.xpn.spellnote.R;
import com.xpn.spellnote.databinding.ActivityViewDocumentsBinding;
import com.xpn.spellnote.ui.about.ActivityAbout;
import com.xpn.spellnote.ui.ads.RemoveAdsBilling;
import com.xpn.spellnote.ui.dictionary.ActivitySelectLanguages;
import com.xpn.spellnote.ui.document.list.archive.FragmentViewArchive;
import com.xpn.spellnote.ui.document.list.documents.FragmentViewDocumentList;
import com.xpn.spellnote.ui.document.list.trash.FragmentViewTrash;
import com.xpn.spellnote.util.CacheUtil;
import com.xpn.spellnote.util.Util;

import timber.log.Timber;


public class ActivityViewDocuments extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, RemoveAdsBilling.ViewContract, BaseFragmentDocumentList.DocumentContract {


    private static final String NAVIGATION_DRAWER_FIRST_LAUNCH_TAG = "nav_first";
    private static final String SAVED_STATE_FRAGMENT_TAG = "curr_f";
    private ActivityViewDocumentsBinding binding;
    private FirebaseAnalytics analytics;
    BaseFragmentDocumentList documentFragment = null;
    private RemoveAdsBilling billing;


    @Override
    @AddTrace(name = "onCreateViewDocuments")
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate( savedInstanceState );
        binding = DataBindingUtil.setContentView(this, R.layout.activity_view_documents);

        /// set-up remove-ads
        billing = new RemoveAdsBilling(this, this, getString(R.string.license_key), getString(R.string.remove_ads_id));

        /// set up toolbar and navigation-toggle
        setSupportActionBar(binding.toolbar);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, binding.drawer, binding.toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        binding.drawer.addDrawerListener(toggle);
        toggle.syncState();

        /// set up navigation drawer
        binding.navigation.setNavigationItemSelectedListener(this);

        /// show the latest chosen fragment
        Integer navigationId = savedInstanceState == null ? R.id.nav_documents : savedInstanceState.getInt(SAVED_STATE_FRAGMENT_TAG);
        onNavigationItemSelected( binding.navigation.getMenu().findItem(navigationId));

        /// show remove-ads button only if it's not purchased yet
        binding.navigation.getMenu().findItem(R.id.nav_remove_ads).setVisible(!billing.areAdsRemoved());

        /// open drawer on first launch
        if(CacheUtil.getCache(this, NAVIGATION_DRAWER_FIRST_LAUNCH_TAG, true)) {
            binding.drawer.openDrawer(Gravity.START, true);
            CacheUtil.setCache(this, NAVIGATION_DRAWER_FIRST_LAUNCH_TAG, false );
        }

        /// initialize analytics
        analytics = FirebaseAnalytics.getInstance(this);
    }

    @Override
    protected void onDestroy() {
        billing.release();
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Integer currentFragmentTag = Integer.parseInt(getSupportFragmentManager().findFragmentById( R.id.list_of_documents ).getTag());
        outState.putInt( SAVED_STATE_FRAGMENT_TAG, currentFragmentTag );
    }


    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if( id == R.id.nav_documents )          showFragment( id, new FragmentViewDocumentList() );
        else if( id == R.id.nav_archive )       showFragment( id, new FragmentViewArchive() );
        else if( id == R.id.nav_trash )         showFragment( id, new FragmentViewTrash() );
        else if( id == R.id.nav_dictionaries)   startActivity( new Intent( this, ActivitySelectLanguages.class ) ) ;
        else if( id == R.id.nav_remove_ads)     purchaseRemoveAds();
        else if( id == R.id.nav_feedback )      Util.sendFeedback( this );
        else if( id == R.id.nav_rate )          Util.openAppInPlayStore( this );
        else if( id == R.id.nav_about )         startActivity( new Intent( this, ActivityAbout.class ) );

        /// close the drawer
        binding.drawer.closeDrawer(GravityCompat.START, true);
        return true;
    }


    public void showFragment( Integer navigationId, BaseFragmentDocumentList documentFragment ) {

        /// use navigation id as fragment tag, show category in toolbar
        String fragmentTag = navigationId.toString();
        binding.navigation.setCheckedItem(navigationId);

        // get fragment manager, Make sure the current transaction finishes first
        FragmentManager fm = getSupportFragmentManager();
        fm.executePendingTransactions();

        // Don't make new transaction if it's already present
        if( fm.findFragmentByTag( fragmentTag ) != null ) {
            this.documentFragment = (BaseFragmentDocumentList) fm.findFragmentById( R.id.list_of_documents );
        }
        else {
            this.documentFragment = documentFragment;
            fm.beginTransaction().replace(R.id.list_of_documents, documentFragment, fragmentTag).commit();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (!billing.handleActivityResult(requestCode, resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void purchaseRemoveAds() {
        billing.purchase();
        analytics.logEvent("remove_ads_click", null);
    }

    @Override
    public void onAdsRemoved() {
        binding.navigation.getMenu().findItem(R.id.nav_remove_ads).setVisible(false);
        Toast.makeText(this, getString(R.string.advertisement_thank_you_message), Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPurchaseError(Throwable error) {
        Timber.e(error);
        Toast.makeText(this, getString(R.string.advertisement_purchase_failure), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void setTitle(String title) {
        binding.toolbar.setTitle(title);
    }
}
