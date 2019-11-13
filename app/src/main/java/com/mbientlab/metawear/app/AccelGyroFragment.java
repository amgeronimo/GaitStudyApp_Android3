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

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.mbientlab.metawear.AsyncDataProducer;
import com.mbientlab.metawear.Data;
import com.mbientlab.metawear.DataProducer;
import com.mbientlab.metawear.Route;
import com.mbientlab.metawear.Subscriber;
import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.android.BtleService;
import com.mbientlab.metawear.app.help.HelpOption;
import com.mbientlab.metawear.app.help.HelpOptionAdapter;
import com.mbientlab.metawear.builder.RouteBuilder;
import com.mbientlab.metawear.builder.RouteComponent;
import com.mbientlab.metawear.builder.filter.Comparison;
import com.mbientlab.metawear.builder.filter.ThresholdOutput;
import com.mbientlab.metawear.builder.function.Function1;
import com.mbientlab.metawear.data.Acceleration;
import com.mbientlab.metawear.data.AngularVelocity;
import com.mbientlab.metawear.module.Accelerometer;
import com.mbientlab.metawear.module.AccelerometerBosch;
import com.mbientlab.metawear.module.AccelerometerMma8452q;
import com.mbientlab.metawear.module.Debug;
import com.mbientlab.metawear.module.GyroBmi160;
import com.mbientlab.metawear.module.Logging;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import bolts.Continuation;
import bolts.Task;

import static android.provider.AlarmClock.EXTRA_MESSAGE;


/**
 * Created by etsai on 8/19/2015.
 */
public class AccelGyroFragment extends SensorFragment_mod {
    private static final float[] MMA845Q_RANGES = {2.f, 4.f, 8.f}, BOSCH_RANGES = {2.f, 4.f, 8.f, 16.f};
    private static final float INITIAL_RANGE = 2.f, ACC_FREQ = 50.f;
    private Accelerometer accelerometer = null;
    private int accRange = 1;

    private static final float[] AVAILABLE_RANGES = {125.f, 250.f, 500.f, 1000.f, 2000.f};
    private static final float INITIAL_GYR_RANGE = 125.f, GYR_ODR = 25.f;
    private GyroBmi160 gyro = null;
    private int gyroRange = 1;
    private final ArrayList<Float> accDatax = new ArrayList<>(), accDatay = new ArrayList<>(), accDataz = new ArrayList<>(),
            gyroDatax = new ArrayList<>(), gyroDatay = new ArrayList<>(), gyroDataz = new ArrayList<>();
    private final ArrayList<String> gyroTime = new ArrayList<>(), accTime = new ArrayList<>();

    private Handler rHandler;
//    private Logging logging;


    public AccelGyroFragment() {
        super(R.string.navigation_fragment_accelgyro, R.layout.fragment_sensor_mod);
    }


    private final ArrayList<Entry> xAxisData= new ArrayList<>(), yAxisData= new ArrayList<>(), zAxisData= new ArrayList<>();
    private final String dataType = "accel";
    protected float samplePeriod;

//    protected void addChartData(float x0, float x1, float x2, float samplePeriod) {
//        LineData chartData = chart.getData();
//        chartData.addXValue(String.format(Locale.US, "%.2f", sampleCount * samplePeriod));
//        chartData.addEntry(new Entry(x0, sampleCount), 0);
//        chartData.addEntry(new Entry(x1, sampleCount), 1);
//        chartData.addEntry(new Entry(x2, sampleCount), 2);

    protected void add1xChartData(float x0, float samplePeriod) {
        LineData chartData = chart.getData();
        chartData.addXValue(String.format(Locale.US, "%.2f", sampleCount * samplePeriod));
            chartData.addEntry(new Entry(x0, sampleCount), 0);
        sampleCount++;
        updateChart();
    }
    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        try {
            String ss = HomeFragment.loadSCFile(getContext(),"SCfile.txt");
            TextView sc = (TextView) view.findViewById(R.id.studyCode);
            sc.setText(ss);
        } catch (Exception e) {
            throw new RuntimeException("Cannot load sc", e);
        }
    }

    @Override
    protected void boardReady() throws UnsupportedModuleException {
//        logging = mwBoard.getModule(Logging.class);
        accelerometer = mwBoard.getModuleOrThrow(Accelerometer.class);
        gyro = mwBoard.getModuleOrThrow(GyroBmi160.class);

    }

    @Override
    protected void fillHelpOptionAdapter(HelpOptionAdapter adapter) {
    }

    @Override
    protected void setup(View view) {





        refreshChart(view,true);

        TextView studycode = (TextView) view.findViewById(R.id.studyCode);
//        String sc = studycode.getText().toString();
        studycode.setEnabled(false);
        Button startButton = (Button) view.findViewById(R.id.layout_two_button_left);
        startButton.setEnabled(false);
        Button cancelButton = (Button) view.findViewById(R.id.layout_two_button_right);
        cancelButton.setEnabled(true);
        SeekBar rectimer = (SeekBar) view.findViewById(R.id.rec_length);
        rectimer.setEnabled(false);
        RadioGroup asstgrp = (RadioGroup) getView().findViewById(R.id.AssistanceRadioGroup);
        for (int i = 0; i < asstgrp.getChildCount(); i++) {
            asstgrp.getChildAt(i).setEnabled(false);
        }


        startStreaming(view);

    }


    @Override
    protected void clean(View view, boolean SendEmail) {
        stopStreaming();
//        logging.stop();
        if (SendEmail==true) {
            TextView studycode = (TextView) view.findViewById(R.id.studyCode);
            RadioGroup asstgrp = (RadioGroup) getView().findViewById(R.id.AssistanceRadioGroup);
//            for (int i = 0; i < asstgrp.getChildCount(); i++) {
//                if (asstgrp.getChildAt(i).isPressed())
//                    ac = i;
//            }

            int radioButtonID = asstgrp.getCheckedRadioButtonId();
            View radioButton = asstgrp.findViewById(radioButtonID);
            int idx = asstgrp.indexOfChild(radioButton);
            String sc = String.format(Locale.US, "%s_%d", studycode.getText().toString(), idx);

            //String sc = studycode.getText().toString();
            saveData(sc);
        }
resetData(view, true);
        mwBoard.tearDown();
    }

    void startStreaming(View view) {

        Accelerometer.ConfigEditor<?> editor = accelerometer.configure();
        editor.odr(ACC_FREQ);
        editor.range(BOSCH_RANGES[accRange]);
        editor.commit();
        samplePeriod= 1 / accelerometer.getOdr();


        final AsyncDataProducer producer = accelerometer.packedAcceleration() == null ?
                accelerometer.packedAcceleration() :
                accelerometer.acceleration();
        producer.addRouteAsync(source -> source.stream((data, env) -> {
            final Acceleration value = data.value(Acceleration.class);
            //addChartData(value.x(), value.y(), value.z(), samplePeriod);
            accDatax.add(value.x());
            accDatay.add(value.y());
            accDataz.add(value.z());
            accTime.add(data.formattedTimestamp());
            add1xChartData(value.x(), samplePeriod);
        })).continueWith(task -> {
            streamRoute = task.getResult();
            producer.start();
            accelerometer.start();
            return null;
        });

        gyro = mwBoard.getModule(GyroBmi160.class);
        GyroBmi160.Range[] values = GyroBmi160.Range.values();
        gyro.configure()
                .odr(GyroBmi160.OutputDataRate.ODR_50_HZ)
                .range(values[gyroRange])
                .commit();

        //final float period = 1 / GYR_ODR;


        final AsyncDataProducer producer2 = gyro.packedAngularVelocity() == null ?
                gyro.packedAngularVelocity() :
                gyro.angularVelocity();
        producer2.addRouteAsync(source -> source.stream((data, env) -> {
            final AngularVelocity value2 = data.value(AngularVelocity.class);
            //addChartData(value2.x(), value2.y(), value2.z(), period);
            gyroDatax.add(value2.x());
            gyroDatay.add(value2.y());
            gyroDataz.add(value2.z());
            gyroTime.add(data.formattedTimestamp());
//Log.i("gyro",String.format("Gyro: %s",value.toString()));
        })).continueWith(task -> {
//            logging2.start(true);
            streamRoute2 = task.getResult();
            producer2.start();
            gyro.start();


            ProgressBar pbar = (ProgressBar) view.findViewById(R.id.progressBar);
            SeekBar sbar = (SeekBar) view.findViewById(R.id.rec_length);
            long rectimer = (sbar.getProgress() + 1)*60*1000; //Remove the /10
            rHandler = new Handler();
            rHandler.postDelayed(() -> pbar.setProgress(10), (rectimer) / 10);
            rHandler.postDelayed(() -> pbar.setProgress(20), 2 * (rectimer) / 10);
            rHandler.postDelayed(() -> pbar.setProgress(30), 3 * (rectimer) / 10);
            rHandler.postDelayed(() -> pbar.setProgress(40), 4 * (rectimer) / 10);
            rHandler.postDelayed(() -> pbar.setProgress(50), 5 * (rectimer) / 10);
            rHandler.postDelayed(() -> pbar.setProgress(60), 6 * (rectimer) / 10);
            rHandler.postDelayed(() -> pbar.setProgress(70), 7 * (rectimer) / 10);
            rHandler.postDelayed(() -> pbar.setProgress(80), 8 * (rectimer) / 10);
            rHandler.postDelayed(() -> pbar.setProgress(90), 9 * (rectimer) / 10);
            rHandler.postDelayed(() -> pbar.setProgress(100), (rectimer));
            rHandler.postDelayed(() -> clean(view,true), rectimer);

            return null;
        });

    }

    void stopStreaming() {



        if (accelerometer != null) {
            accelerometer.stop();
            accelerometer.acceleration().stop();
        }

        if (gyro != null) {
            gyro.stop();
            gyro.angularVelocity().stop();
        }
        rHandler.removeCallbacksAndMessages(null);

    }


    @Override
    protected String saveData(String sc) {


        final String accCSV_HEADER = String.format("time,x-accel,y-accel,z-accel%n");
        final Calendar currtime = Calendar.getInstance();
        final String filedate = String.format(Locale.US, "%tY%<tm%<td-%<tH%<tM%<tS%<tL.csv", currtime);


        final String accfilename = String.format(Locale.US, "%s_%s_%s", sc, "ACC", filedate);
        final File accpath = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS), accfilename);
        try {
            FileOutputStream fos = new FileOutputStream(accpath, false);
            fos.write(accCSV_HEADER.getBytes());
            for (int i = 0; i < accTime.size(); i++)
                fos.write(String.format(Locale.US, "%s,%.3f,%.3f,%.3f%n", accTime.get(i),
                        accDatax.get(i),
                        accDatay.get(i),
                        accDataz.get(i)).getBytes());
            fos.close();
            //return accfilename;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }


        final String gyroCSV_HEADER = String.format("time,x-gyro,y-gyro,z-gyro%n");
        final String gyrofilename = String.format(Locale.US, "%s_%s_%s", sc, "GYR", filedate);
        final File gyropath = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS), gyrofilename);
        try {
            FileOutputStream fosg = new FileOutputStream(gyropath, false);
            fosg.write(gyroCSV_HEADER.getBytes());
            for (int i = 0; i < gyroTime.size(); i++)
                fosg.write(String.format(Locale.US, "%s,%.3f,%.3f,%.3f%n", gyroTime.get(i),
                        gyroDatax.get(i),
                        gyroDatay.get(i),
                        gyroDataz.get(i)).getBytes());
            fosg.close();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }


        if (filedate != null) {
            String fromEmail = "gaitals01@gmail.com";
            String fromPassword = "time2walk";
            String toEmails = "Study_D.gy7u89itfm8rlml0@u.box.com";
            List<String> toEmailList = Arrays.asList(toEmails
                    .split("\\s*,\\s*"));
            Log.i("SendMailActivity", "To List: " + toEmailList);
            String emailSubject = sc + " data";
            String emailBody = "";
            Activity curractivity = this.getActivity();
            new SendMailTask(curractivity).execute(fromEmail,
                    fromPassword, toEmailList, emailSubject, emailBody, filedate, sc);
            Log.i("SentEmail", "email was sent!");
        }
        return filedate;
    }

    @Override
    protected void resetData(View view, boolean clearData) {
        if (clearData) {
            accTime.clear();
            accDatax.clear();
            accDatay.clear();
            accDataz.clear();
            gyroTime.clear();
            gyroDatax.clear();
            gyroDatay.clear();
            gyroDataz.clear();

            sampleCount = 0;
            chartXValues.clear();
            xAxisData.clear();
            yAxisData.clear();
            zAxisData.clear();
            ArrayList<LineDataSet> spinAxisData= new ArrayList<>();
            spinAxisData.add(new LineDataSet(xAxisData, "x-" + dataType));
            spinAxisData.get(0).setColor(Color.RED);
            spinAxisData.get(0).setDrawCircles(false);

            spinAxisData.add(new LineDataSet(yAxisData, "y-" + dataType));
            spinAxisData.get(1).setColor(Color.GREEN);
            spinAxisData.get(1).setDrawCircles(false);

            spinAxisData.add(new LineDataSet(zAxisData, "z-" + dataType));
            spinAxisData.get(2).setColor(Color.BLUE);
            spinAxisData.get(2).setDrawCircles(false);

            LineData data= new LineData(chartXValues);
            for(LineDataSet set: spinAxisData) {
                data.addDataSet(set);
            }
            data.setDrawValues(false);
            chart.setData(data);

            if (streamRoute != null) {
                streamRoute.remove();
                streamRoute = null;
            }
            if (streamRoute2 != null) {
                streamRoute2.remove();
                streamRoute2 = null;
            }

            TextView studycode = (TextView) getView().findViewById(R.id.studyCode);
            studycode.setEnabled(true);
            Button startButton = (Button) getView().findViewById(R.id.layout_two_button_left);
            startButton.setEnabled(true);
            Button cancelButton = (Button) getView().findViewById(R.id.layout_two_button_right);
            cancelButton.setEnabled(false);
            SeekBar rectimer = (SeekBar) getView().findViewById(R.id.rec_length);
            rectimer.setEnabled(true);
            ProgressBar pbar = (ProgressBar) getView().findViewById(R.id.progressBar);
            pbar.setProgress(0);
            RadioGroup asstgrp = (RadioGroup) getView().findViewById(R.id.AssistanceRadioGroup);
            for (int i = 0; i < asstgrp.getChildCount(); i++) {
                asstgrp.getChildAt(i).setEnabled(true);
            }

        }

    }

}