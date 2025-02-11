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
import android.annotation.TargetApi;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
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
import android.support.v7.app.ActionBar;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.android.BtleService;
import com.mbientlab.metawear.app.ModuleFragmentBase.FragmentBus;
import com.mbientlab.metawear.module.Debug;
import com.mbientlab.metawear.module.Settings;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import bolts.Capture;
import bolts.Continuation;
import bolts.Task;
import no.nordicsemi.android.dfu.DfuBaseService;
import no.nordicsemi.android.dfu.DfuProgressListener;
import no.nordicsemi.android.dfu.DfuProgressListenerAdapter;
import no.nordicsemi.android.dfu.DfuServiceInitiator;
import no.nordicsemi.android.dfu.DfuServiceListenerHelper;

import static com.mbientlab.metawear.app.ScannerActivity.setConnInterval;

public class NavigationActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, ServiceConnection, FragmentBus, LoaderManager.LoaderCallbacks<Cursor> {
    public final static String EXTRA_BT_DEVICE = "com.mbientlab.metawear.app.NavigationActivity.EXTRA_BT_DEVICE";

    private static final int SELECT_FILE_REQ = 1, PERMISSION_REQUEST_READ_STORAGE = 2, PERMISSION_REQUEST_WRITE_STORAGE = 3, INTERNET_PERMISSION = 4;
    private static final String EXTRA_URI = "uri", FRAGMENT_KEY = "com.mbientlab.metawear.app.NavigationActivity.FRAGMENT_KEY";
    //       DFU_PROGRESS_FRAGMENT_TAG= "com.mbientlab.metawear.app.NavigationActivity.DFU_PROGRESS_FRAGMENT_TAG";
    private final static Map<Integer, Class<? extends ModuleFragmentBase>> FRAGMENT_CLASSES;
//    private final static Map<String, String> EXTENSION_TO_APP_TYPE;

    static {
        Map<Integer, Class<? extends ModuleFragmentBase>> tempMap = new LinkedHashMap<>();
        tempMap.put(R.id.nav_home, HomeFragment.class);
        tempMap.put(R.id.nav_accelgyro, AccelGyroFragment.class);
        tempMap.put(R.id.nav_fallreport, FallReportFragment.class);


        FRAGMENT_CLASSES = Collections.unmodifiableMap(tempMap);
    }

    public static class ReconnectDialogFragment extends DialogFragment implements ServiceConnection {
        private static final String KEY_BLUETOOTH_DEVICE = "com.mbientlab.metawear.app.NavigationActivity.ReconnectDialogFragment.KEY_BLUETOOTH_DEVICE";

        private ProgressDialog reconnectDialog = null;
        private BluetoothDevice btDevice = null;
        private MetaWearBoard currentMwBoard = null;

        public static ReconnectDialogFragment newInstance(BluetoothDevice btDevice) {
            Bundle args = new Bundle();
            args.putParcelable(KEY_BLUETOOTH_DEVICE, btDevice);

            ReconnectDialogFragment newFragment = new ReconnectDialogFragment();
            newFragment.setArguments(args);

            return newFragment;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            btDevice = getArguments().getParcelable(KEY_BLUETOOTH_DEVICE);
            getActivity().getApplicationContext().bindService(new Intent(getActivity(), BtleService.class), this, BIND_AUTO_CREATE);

            reconnectDialog = new ProgressDialog(getActivity());
            reconnectDialog.setTitle(getString(R.string.title_reconnect_attempt));
            reconnectDialog.setMessage(getString(R.string.message_wait));
            reconnectDialog.setCancelable(false);
            reconnectDialog.setCanceledOnTouchOutside(false);
            reconnectDialog.setIndeterminate(true);
            reconnectDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.label_cancel), (dialogInterface, i) -> {
                currentMwBoard.disconnectAsync();
                getActivity().finish();
            });

            return reconnectDialog;
        }



        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            currentMwBoard = ((BtleService.LocalBinder) service).getMetaWearBoard(btDevice);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    }

    private final String RECONNECT_DIALOG_TAG = "reconnect_dialog_tag";
    private final Handler taskScheduler = new Handler();
    private BluetoothDevice btDevice;
    private MetaWearBoard mwBoard;
    private Fragment currentFragment = null;
    private Uri fileStreamUri;
    private String fileName;

    private Continuation<Void, Void> reconnectResult = task -> {
        ((DialogFragment) getSupportFragmentManager().findFragmentByTag(RECONNECT_DIALOG_TAG)).dismiss();

        if (task.isCancelled()) {
            finish();
        } else {
            setConnInterval(mwBoard.getModule(Settings.class));
            ((ModuleFragmentBase) currentFragment).reconnected();
        }

        return null;
    };

    private void attemptReconnect() {
        attemptReconnect(0);
    }

    private void attemptReconnect(long delay) {
        ReconnectDialogFragment dialogFragment = ReconnectDialogFragment.newInstance(btDevice);
        dialogFragment.show(getSupportFragmentManager(), RECONNECT_DIALOG_TAG);

        if (delay != 0) {
            taskScheduler.postDelayed(() -> ScannerActivity.reconnect(mwBoard).continueWith(reconnectResult), delay);
        } else {
            ScannerActivity.reconnect(mwBoard).continueWith(reconnectResult);
        }
    }

    @Override
    public BluetoothDevice getBtDevice() {
        return btDevice;
    }

    @Override
    public void resetConnectionStateHandler(long delay) {
        attemptReconnect(delay);
    }

    @Override
    public void initiateDfu(final Object path) {
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        getApplicationContext().unbindService(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);

        checkPermission(Manifest.permission.INTERNET,
                INTERNET_PERMISSION);
        checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE,
                PERMISSION_REQUEST_READ_STORAGE);
        checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                PERMISSION_REQUEST_WRITE_STORAGE);


        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(view -> ((ModuleFragmentBase) currentFragment).showHelpDialog());

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        if (savedInstanceState == null) {
            onNavigationItemSelected(navigationView.getMenu().findItem(R.id.nav_home));
        } else {
            currentFragment = getSupportFragmentManager().getFragment(savedInstanceState, FRAGMENT_KEY);
        }

        btDevice = getIntent().getParcelableExtra(EXTRA_BT_DEVICE);
        getApplicationContext().bindService(new Intent(this, BtleService.class), this, BIND_AUTO_CREATE);

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

        fileStreamUri = null;
        switch (requestCode) {
            case SELECT_FILE_REQ:
                // and read new one
                final Uri uri = data.getData();
                /*
                 * The URI returned from application may be in 'file' or 'content' schema.
                 * 'File' schema allows us to create a File object and read details from if directly.
                 *
                 * Data from 'Content' schema must be read by Content Provider. To do that we are using a Loader.
                 */
                if (uri.getScheme().equals("file")) {
                    // the direct path to the file has been returned
                    initiateDfu(new File(uri.getPath()));
                } else if (uri.getScheme().equals("content")) {
                    fileStreamUri = uri;

                    // file name and size must be obtained from Content Provider
                    final Bundle bundle = new Bundle();
                    bundle.putParcelable(EXTRA_URI, uri);
                    getSupportLoaderManager().restartLoader(0, bundle, this);
                }
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
            mwBoard.disconnectAsync();
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.navigation, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case R.id.action_reset:
                if (!mwBoard.inMetaBootMode()) {
                    mwBoard.getModule(Debug.class).resetAsync()
                            .continueWith(ignored -> {
                                attemptReconnect(0);
                                return null;
                            });
                    Snackbar.make(findViewById(R.id.drawer_layout), R.string.message_soft_reset, Snackbar.LENGTH_LONG).show();
                } else {
                    Snackbar.make(findViewById(R.id.drawer_layout), R.string.message_no_soft_reset, Snackbar.LENGTH_LONG).show();
                }
                return true;
            case R.id.action_disconnect:
                if (!mwBoard.inMetaBootMode()) {
                    Settings.BleConnectionParametersEditor editor = mwBoard.getModule(Settings.class).editBleConnParams();
                    if (editor != null) {
                        editor.maxConnectionInterval(125f)
                                .commit();
                    }
                    mwBoard.getModule(Debug.class).disconnectAsync();
                } else {
                    mwBoard.disconnectAsync();
                }

                finish();
                return true;
//            case R.id.action_manual_dfu:
//                if (checkLocationPermission()) {
//                    startContentSelectionIntent();
//                }
//                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();


//If Home or Perform a Recording Fragment, follow the stuff below

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

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mwBoard = ((BtleService.LocalBinder) service).getMetaWearBoard(btDevice);
        mwBoard.onUnexpectedDisconnect(status -> attemptReconnect());
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        final Uri uri = args.getParcelable(EXTRA_URI);
        /*
         * Some apps, f.e. Google Drive allow to select file that is not on the device. There is no "_data" column handled by that provider. Let's try to obtain all columns and than check
         * which columns are present.
         */
        //final String[] projection = new String[] { MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.SIZE, MediaStore.MediaColumns.DATA };
        return new CursorLoader(this, uri, null /*all columns, instead of projection*/, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data.moveToNext()) {
            /*
             * Here we have to check the column indexes by name as we have requested for all. The order may be different.
             */
            fileName = data.getString(data.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)/* 0 DISPLAY_NAME */);
            //final int fileSize = data.getInt(data.getColumnIndex(MediaStore.MediaColumns.SIZE) /* 1 SIZE */);

            final int dataIndex = data.getColumnIndex(MediaStore.MediaColumns.DATA);
            if (dataIndex != -1) {
                initiateDfu(new File(data.getString(dataIndex /*2 DATA */)));
            } else {
                initiateDfu(fileStreamUri);
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    /**
     * Code for content selection adapted from the nRF Toolbox app by Nordic Semiconductor
     * https://play.google.com/store/apps/details?id=no.nordicsemi.android.nrftoolbox&hl=en
     */
    private void startContentSelectionIntent() {
        final Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*")
                .addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, SELECT_FILE_REQ);
    }




    // Function to check and request permission.
    public void checkPermission(String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(NavigationActivity.this, permission)
                == PackageManager.PERMISSION_DENIED) {

            // Requesting the permission
            ActivityCompat.requestPermissions(NavigationActivity.this,
                    new String[]{permission},
                    requestCode);
        } else {
            Toast.makeText(NavigationActivity.this,
                    "Internet permission already granted",
                    Toast.LENGTH_SHORT)
                    .show();
        }
    }


}