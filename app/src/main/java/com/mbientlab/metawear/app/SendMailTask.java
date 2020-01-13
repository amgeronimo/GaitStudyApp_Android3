package com.mbientlab.metawear.app;


import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

public class SendMailTask extends AsyncTask<Object,Object,Boolean> {


    public interface MailDelegate {
        void MailResult(Boolean output);
    }

    private ProgressDialog statusDialog;
    private Activity sendMailActivity;
    public MailDelegate delegate;

    public SendMailTask(Activity activity, MailDelegate delegate) {
        sendMailActivity=activity;
        this.delegate = delegate;
    }



    protected void onPreExecute() {
        statusDialog = new ProgressDialog(sendMailActivity);
        statusDialog.setMessage("Getting ready...");
        statusDialog.setIndeterminate(false);
        statusDialog.setCancelable(false);
        statusDialog.show();
    }

    @Override
    protected Boolean doInBackground(Object... args) {
        try {
            Log.i("SendMailTask", "About to instantiate GMail...");
            publishProgress("Processing input....");
            GMailSender androidEmail = new GMailSender(args[0].toString(),
                    args[1].toString(), (List) args[2], args[3].toString(),
                    args[4].toString(), args[5].toString(), args[6].toString());
            publishProgress("Preparing mail message....");
            androidEmail.createEmailMessage();
            publishProgress("Sending email....");
            androidEmail.sendEmail();
            publishProgress("Email Sent.");
            Log.i("SendMailTask", "Mail Sent.");
            return Boolean.TRUE;
        } catch (Exception e) {
            publishProgress(e.getMessage());
            Log.e("SendMailTask", e.getMessage(), e);
            return Boolean.FALSE;
        }
    }

    @Override
    public void onProgressUpdate(Object... values) {
        statusDialog.setMessage(values[0].toString());
    }

    @Override
    public void onPostExecute(Boolean result) {
        statusDialog.dismiss();
        delegate.MailResult(result);
    }


}