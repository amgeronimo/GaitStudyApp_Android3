/*
 * Copyright 2015 MbientLab Inc. All rights reserved.
 *
 * IMPORTANT: Your use of this Software is limited to those specific rights
 * granted under the terms of a software license agreement between the user who
 * downloaded the software, his/her employer (which must be your employer) and
 * MbientLab Inc, (the "License").  You may not use this Software unless you
 * agree to abide by the terms of the License which can be found at
 * www.mbientlab.com/terms . The License limits your use, and you acknowledge,
 * that the  Software may not be modified, copied or distributed and can be used
 * solely and exclusively in conjunction with a MbientLab Inc, product.  Other
 * than for the foregoing purpose, you may not use, reproduce, copy, prepare
 * derivative works of, modify, distribute, perform, display or sell this
 * Software and/or its documentation for any purpose.
 *
 * YOU FURTHER ACKNOWLEDGE AND AGREE THAT THE SOFTWARE AND DOCUMENTATION ARE
 * PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESS OR IMPLIED,
 * INCLUDING WITHOUT LIMITATION, ANY WARRANTY OF MERCHANTABILITY, TITLE,
 * NON-INFRINGEMENT AND FITNESS FOR A PARTICULAR PURPOSE. IN NO EVENT SHALL
 * MBIENTLAB OR ITS LICENSORS BE LIABLE OR OBLIGATED UNDER CONTRACT, NEGLIGENCE,
 * STRICT LIABILITY, CONTRIBUTION, BREACH OF WARRANTY, OR OTHER LEGAL EQUITABLE
 * THEORY ANY DIRECT OR INDIRECT DAMAGES OR EXPENSES INCLUDING BUT NOT LIMITED
 * TO ANY INCIDENTAL, SPECIAL, INDIRECT, PUNITIVE OR CONSEQUENTIAL DAMAGES, LOST
 * PROFITS OR LOST DATA, COST OF PROCUREMENT OF SUBSTITUTE GOODS, TECHNOLOGY,
 * SERVICES, OR ANY CLAIMS BY THIRD PARTIES (INCLUDING BUT NOT LIMITED TO ANY
 * DEFENSE THEREOF), OR OTHER SIMILAR COSTS.
 *
 * Should you have any questions regarding your right to use this Software,
 * contact MbientLab Inc, at www.mbientlab.com.
 */

package com.mbientlab.metawear.app;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;


import android.content.Intent;
import android.content.ServiceConnection;
import android.content.Context;
import android.os.Build;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.support.v7.app.AppCompatActivity;

import com.mbientlab.metawear.Route;
import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.android.BtleService;
import com.mbientlab.metawear.app.help.HelpOptionAdapter;
import com.mbientlab.metawear.module.Led;
import com.mbientlab.metawear.module.Led.Color;
import com.mbientlab.metawear.module.Switch;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Locale;

import bolts.Task;


/**
 * Created by etsai on 8/22/2015.
 */
public class HomeFragment extends ModuleFragmentBase {
    private Led ledModule;
    private int switchRouteId = -1;
    public String scfile = "SCfile.txt";


    public static class MetaBootWarningFragment extends DialogFragment {
        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity()).setTitle(R.string.title_warning)
                    .setPositiveButton(R.string.label_ok, null)
                    .setCancelable(false)
                    .setMessage(R.string.message_metaboot)
                    .create();
        }
    }

    public HomeFragment() {
        super(R.string.navigation_fragment_home);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
        setRetainInstance(true);
        return inflater.inflate(R.layout.fragment_home, container, false);

//
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Button sctmp = (Button) view.findViewById(R.id.study_code_save);
        view.findViewById(R.id.study_code_value).setOnClickListener(view10 -> {
            if (sctmp.isEnabled() == false)
                sctmp.setEnabled(true);
        });

        try {
            String ss = loadSCFile(getContext(),scfile);
            EditText sc = (EditText) view.findViewById(R.id.study_code_value);
            sc.setText(ss);
        } catch (Exception e) {
            throw new RuntimeException("Cannot load sc", e);
        }

        view.findViewById(R.id.study_code_save).setOnClickListener(view9 -> {
            EditText scedt = (EditText) view.findViewById((R.id.study_code_value));
            writeSCFile(scedt.getText().toString(), getContext());
        });

//        view.findViewById(R.id.go_to_fallreport).setOnClickListener(view18 -> {
//            Intent i = new Intent(view.getContext(), FallReportFragment.class);
//            startActivity(i);
        ////Need to use an intent to transition to the navigation activity, possibly with an additional key which points to either the recording or reporting fragments.
//        });

        //        view.findViewById(R.id.go_to_recording).setOnClickListener(view20 -> {
//            Intent i = new Intent(view.getContext(), Fragment.class);
//            startActivity(i);
//        });


        view.findViewById(R.id.led_red_on).setOnClickListener(view1 -> {
            configureChannel(ledModule.editPattern(Color.RED));
            ledModule.play();
        });
        view.findViewById(R.id.led_green_on).setOnClickListener(view12 -> {
            configureChannel(ledModule.editPattern(Color.GREEN));
            ledModule.play();
        });
        view.findViewById(R.id.led_blue_on).setOnClickListener(view13 -> {
            configureChannel(ledModule.editPattern(Color.BLUE));
            ledModule.play();
        });
        view.findViewById(R.id.led_stop).setOnClickListener(view14 -> ledModule.stop(true));

        view.findViewById(R.id.board_battery_level_text).setOnClickListener(v -> mwBoard.readBatteryLevelAsync()
                .continueWith(task -> {
                    ((TextView) view.findViewById(R.id.board_battery_level_value)).setText(String.format(Locale.US, "%d", task.getResult()));
                    return null;
                }, Task.UI_THREAD_EXECUTOR)
        );

//        View current = getActivity().getCurrentFocus();
//       if (current != null) current.clearFocus();

    }

    private void writeSCFile(String data, Context context) {
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput(scfile, Context.MODE_PRIVATE));
            outputStreamWriter.write(data);
            outputStreamWriter.close();
        } catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    public static String loadSCFile(Context context, String scfile) {

        String ret = "";

        try {
            InputStream inputStream = context.openFileInput(scfile);

            if (inputStream != null) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ((receiveString = bufferedReader.readLine()) != null) {
                    stringBuilder.append(receiveString);
                }

                inputStream.close();
                ret = stringBuilder.toString();
            }
        } catch (FileNotFoundException e) {
            Log.e("login activity", "File not found: " + e.toString());
        } catch (IOException e) {
            Log.e("login activity", "Can not read file: " + e.toString());
        }

        return ret;
    }


    private void setupDfuDialog(AlertDialog.Builder builder, int msgResId) {
        builder.setTitle(R.string.title_firmware_update)
                .setPositiveButton(R.string.label_yes, (dialogInterface, i) -> fragBus.initiateDfu(null))
                .setNegativeButton(R.string.label_no, null)
                .setCancelable(false)
                .setMessage(msgResId)
                .show();
    }

    @Override
    protected void boardReady() throws UnsupportedModuleException {
        setupFragment(getView());
    }

    @Override
    protected void fillHelpOptionAdapter(HelpOptionAdapter adapter) {

    }

    @Override
    public void reconnected() {
        setupFragment(getView());
    }

    private void configureChannel(Led.PatternEditor editor) {
        final short PULSE_WIDTH = 1000;
        editor.highIntensity((byte) 31).lowIntensity((byte) 31)
                .highTime((short) (PULSE_WIDTH >> 1)).pulseDuration(PULSE_WIDTH)
                .repeatCount((byte) -1).commit();
    }

    private void setupFragment(final View v) {
        final String METABOOT_WARNING_TAG = "metaboot_warning_tag";

        if (!mwBoard.isConnected()) {
            return;
        }

        if (mwBoard.inMetaBootMode()) {
            if (getFragmentManager().findFragmentByTag(METABOOT_WARNING_TAG) == null) {
                new MetaBootWarningFragment().show(getFragmentManager(), METABOOT_WARNING_TAG);
            }
        } else {
            DialogFragment metabootWarning = (DialogFragment) getFragmentManager().findFragmentByTag(METABOOT_WARNING_TAG);
            if (metabootWarning != null) {
                metabootWarning.dismiss();
            }
        }


        int[] ledResIds = new int[]{R.id.led_stop, R.id.led_red_on, R.id.led_green_on, R.id.led_blue_on};
        if ((ledModule = mwBoard.getModule(Led.class)) != null) {
            for (int id : ledResIds) {
                v.findViewById(id).setEnabled(true);
            }
        } else {
            for (int id : ledResIds) {
                v.findViewById(id).setEnabled(false);
            }
        }
    }
}
