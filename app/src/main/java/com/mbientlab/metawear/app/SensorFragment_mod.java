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

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.FileProvider;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.YAxis;
import com.mbientlab.metawear.Route;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;

/**
 * Created by etsai on 8/19/2015.
 */
public abstract class SensorFragment_mod extends ModuleFragmentBase{

    protected int sampleCount;
    protected long prevUpdate = -1;

    protected Route streamRoute = null;
    protected Route streamRoute2  = null;
    private byte globalLayoutListenerCounter= 0;
    public int layoutId;

    protected final ArrayList<String> chartXValues= new ArrayList<>();
    protected LineChart chart;
    private final Handler chartHandler= new Handler();


    protected SensorFragment_mod(int sensorResId, int layoutId) {
        super(sensorResId);
        this.layoutId= layoutId;
    }

    protected void updateChart() {
        long current = Calendar.getInstance().getTimeInMillis();
        if (prevUpdate == -1 || (current - prevUpdate) >= 33) {
            chartHandler.post(() -> {
                chart.getData().notifyDataChanged();
                chart.notifyDataSetChanged();

                moveViewToLast();
            });

            prevUpdate = current;
        }
    }
    private void moveViewToLast() {
        chart.setVisibleXRangeMinimum(120);
        chart.setVisibleXRangeMaximum(120);
        chart.moveViewToX(Math.max(0f, chartXValues.size() - 1));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setRetainInstance(true);

        View v= inflater.inflate(layoutId, container, false);
        final View scrollView = v.findViewById(R.id.scrollView);
        if (scrollView != null) {
            globalLayoutListenerCounter= 1;
            scrollView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    LineChart.LayoutParams params = chart.getLayoutParams();
                    params.height = scrollView.getHeight();
                    chart.setLayoutParams(params);

                    globalLayoutListenerCounter--;
                    if (globalLayoutListenerCounter < 0) {
                        scrollView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                }
            });
        }

        return v;
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        chart = (LineChart) view.findViewById(R.id.data_chart);

        initializeChart();
       resetData(view, true);
        chart.invalidate();
        chart.setDescription(null);


        Button startButton= (Button) view.findViewById(R.id.layout_two_button_left);
        startButton.setOnClickListener(view1 -> setup(view));
        startButton.setText(R.string.label_start_sample);
        //startButton.setEnabled(false);


        Button cancelButton= (Button) view.findViewById(R.id.layout_two_button_right);
        cancelButton.setText(R.string.label_cancel);
        //saveButton.setEnabled(false);
        cancelButton.setOnClickListener(view12 -> {
            clean(view, false);
        });
    }

    protected void refreshChart(View view, boolean clearData) {
        chart.resetTracking();
        chart.clear();
        resetData(view,clearData);
        chart.invalidate();
        chart.fitScreen();
    }

    protected void initializeChart() {
        ///< configure axis settings
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setStartAtZero(false);
        leftAxis.setAxisMaxValue(5);
        leftAxis.setAxisMinValue(-5);
        chart.getAxisRight().setEnabled(false);
    }


    protected abstract void setup(View view);
    protected abstract void clean(View view, boolean SendEmail);
    protected abstract String saveData(String sc);
    protected abstract void resetData(View view, boolean clearData);
}
