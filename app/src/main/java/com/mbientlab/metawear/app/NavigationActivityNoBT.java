/*
 * Copyright 2014-2015 MbientLab Inc. All rights reserved.
 *
 * IMPORTANT: Your use of this Software is limited to those specific rights granted under the terms of a software
 * license agreement between the user who downloaded the software, his/her employer (which must be your
 * employer) and MbientLab Inc, (the "License").  You may not use this Software unless you agree to abide by the
 * terms of the License which can be found at www.mbientlab.com/terms.  The License limits your use, and you
 * acknowledge, that the Software may be modified, copied, and distributed when used in conjunction with an
 * MbientLab Inc, product.  Other than for the foregoing purpose, you may not use, reproduce, copy, prepare
 * derivative works of, modify, distribute, perform, display or sell this Software and/or its documentation for any
 * purpose.
 *
 * YOU FURTHER ACKNOWLEDGE AND AGREE THAT THE SOFTWARE AND DOCUMENTATION ARE PROVIDED "AS IS" WITHOUT WARRANTY
 * OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING WITHOUT LIMITATION, ANY WARRANTY OF MERCHANTABILITY, TITLE,
 * NON-INFRINGEMENT AND FITNESS FOR A PARTICULAR PURPOSE. IN NO EVENT SHALL MBIENTLAB OR ITS LICENSORS BE LIABLE OR
 * OBLIGATED UNDER CONTRACT, NEGLIGENCE, STRICT LIABILITY, CONTRIBUTION, BREACH OF WARRANTY, OR OTHER LEGAL EQUITABLE
 * THEORY ANY DIRECT OR INDIRECT DAMAGES OR EXPENSES INCLUDING BUT NOT LIMITED TO ANY INCIDENTAL, SPECIAL, INDIRECT,
 * PUNITIVE OR CONSEQUENTIAL DAMAGES, LOST PROFITS OR LOST DATA, COST OF PROCUREMENT OF SUBSTITUTE GOODS, TECHNOLOGY,
 * SERVICES, OR ANY CLAIMS BY THIRD PARTIES (INCLUDING BUT NOT LIMITED TO ANY DEFENSE THEREOF), OR OTHER SIMILAR COSTS.
 *
 * Should you have any questions regarding your right to use this Software, contact MbientLab via email:
 * hello@mbientlab.com.
 */

package com.mbientlab.metawear.app;

import android.Manifest;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.android.BtleService;
import com.mbientlab.metawear.app.ModuleFragmentBase.FragmentBus;
import com.mbientlab.metawear.module.Debug;
import com.mbientlab.metawear.module.Settings;

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import bolts.Continuation;

import static com.mbientlab.metawear.app.ScannerActivity.setConnInterval;

public class NavigationActivityNoBT extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final int SELECT_FILE_REQ = 1, PERMISSION_REQUEST_READ_STORAGE = 2, PERMISSION_REQUEST_WRITE_STORAGE = 3, INTERNET_PERMISSION = 4;
    private static final String EXTRA_URI = "uri", FRAGMENT_KEY = "com.mbientlab.metawear.app.NavigationActivityNoBT.FRAGMENT_KEY";
    //       DFU_PROGRESS_FRAGMENT_TAG= "com.mbientlab.metawear.app.NavigationActivity.DFU_PROGRESS_FRAGMENT_TAG";
    private final static Map<Integer, Class<? extends Fragment>> FRAGMENT_CLASSES;
//    private final static Map<String, String> EXTENSION_TO_APP_TYPE;

    static {
        Map<Integer, Class<? extends Fragment>> tempMap = new LinkedHashMap<>();
//        tempMap.put(R.id.nav_home, HomeFragment.class);
//        tempMap.put(R.id.nav_accelgyro, AccelGyroFragment.class);
        tempMap.put(R.id.nav_fallreport_nobt, FallReportFragment_nobt.class);

        FRAGMENT_CLASSES = Collections.unmodifiableMap(tempMap);
    }


    private final Handler taskScheduler = new Handler();
    private Fragment currentFragment = null;
    private String fileName;




    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation_nobt);

        checkPermission(Manifest.permission.INTERNET,
                INTERNET_PERMISSION);
        checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE,
                PERMISSION_REQUEST_READ_STORAGE);
        checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                PERMISSION_REQUEST_WRITE_STORAGE);


        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);



        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view_nobt);
        navigationView.setNavigationItemSelectedListener(this);

        if (savedInstanceState == null) {
            onNavigationItemSelected(navigationView.getMenu().findItem(R.id.nav_fallreport_nobt));
        } else {
            currentFragment = getSupportFragmentManager().getFragment(savedInstanceState, FRAGMENT_KEY);
        }


    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (currentFragment != null) {
            getSupportFragmentManager().putFragment(outState, FRAGMENT_KEY, currentFragment);
        }
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        if (resultCode != RESULT_OK)
            return;

        switch (requestCode) {
            case SELECT_FILE_REQ:
                // and read new one
//                final Uri uri = data.getData();
//                /*
//                 * The URI returned from application may be in 'file' or 'content' schema.
//                 * 'File' schema allows us to create a File object and read details from if directly.
//                 *
//                 * Data from 'Content' schema must be read by Content Provider. To do that we are using a Loader.
//                 */
//                if (uri.getScheme().equals("file")) {
//                    // the direct path to the file has been returned
//                    initiateDfu(new File(uri.getPath()));
//                } else if (uri.getScheme().equals("content")) {
//                    fileStreamUri = uri;
//
//                    // file name and size must be obtained from Content Provider
//                    final Bundle bundle = new Bundle();
//                    bundle.putParcelable(EXTRA_URI, uri);
//                    getSupportLoaderManager().restartLoader(0, bundle, this);
//                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
           // super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.navigation_nobt, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id) {
            case R.id.action_back:
//                onBackPressed();
                Intent NavIntent = new Intent(NavigationActivityNoBT.this, ScannerActivity.class);
                startActivityForResult(NavIntent, RESULT_OK);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        // update the main content by replacing fragments
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        if (currentFragment != null) {
            transaction.detach(currentFragment);
        }

        String fragmentTag = FRAGMENT_CLASSES.get(id).getCanonicalName();
        currentFragment = fragmentManager.findFragmentByTag(fragmentTag);

        if (currentFragment == null) {
            try {
                currentFragment = FRAGMENT_CLASSES.get(id).getConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Cannot instantiate fragment", e);
            }

            transaction.add(R.id.container, currentFragment, fragmentTag);
        }

        transaction.attach(currentFragment).commit();

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setTitle(item.getTitle());
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }







    // Function to check and request permission.
    public void checkPermission(String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(NavigationActivityNoBT.this, permission)
                == PackageManager.PERMISSION_DENIED) {

            // Requesting the permission
            ActivityCompat.requestPermissions(NavigationActivityNoBT.this,
                    new String[]{permission},
                    requestCode);
        } else {
            Toast.makeText(NavigationActivityNoBT.this,
                    "Internet permission already granted",
                    Toast.LENGTH_SHORT)
                    .show();
        }
    }


}