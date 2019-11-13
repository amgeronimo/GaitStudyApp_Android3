package com.mbientlab.metawear.app;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

public class GMailSender {

    final String emailPort = "587";// gmail's smtp port
    final String smtpAuth = "true";
    final String starttls = "true";
    final String emailHost = "smtp.gmail.com";


    String fromEmail;
    String fromPassword;
    List<String> toEmailList;
    String emailSubject;
    String emailBody;
    String filedate;
    String sc;

    Properties emailProperties;
    Session mailSession;
    MimeMessage emailMessage;

    public GMailSender() {

    }
// HERE AS AN INPUT TO GMAILSENDER WILL NEED TO INCLUDE THE PATH TO FILE ATTACHMENT SO THAT IT IS NOT HARD-CODED.
    public GMailSender(String fromEmail, String fromPassword,
                       List<String> toEmailList, String emailSubject, String emailBody, String filedate, String sc) {
        this.fromEmail = fromEmail;
        this.fromPassword = fromPassword;
        this.toEmailList = toEmailList;
        this.emailSubject = emailSubject;
        this.emailBody = emailBody;
        this.filedate = filedate;
        this.sc = sc;
        this.

                emailProperties = System.getProperties();
        emailProperties.put("mail.smtp.port", emailPort);
        emailProperties.put("mail.smtp.auth", smtpAuth);
        emailProperties.put("mail.smtp.starttls.enable", starttls);
        Log.i("GMail", "Mail server properties set.");
    }


    public MimeMessage createEmailMessage() throws AddressException,
            MessagingException, UnsupportedEncodingException {

        mailSession = Session.getDefaultInstance(emailProperties, null);
        emailMessage = new MimeMessage(mailSession);
        emailMessage.setFrom(new InternetAddress(fromEmail, fromEmail));
        for (String toEmail : toEmailList) {
            Log.i("GMail", "toEmail: " + toEmail);
            emailMessage.addRecipient(Message.RecipientType.TO,
                    new InternetAddress(toEmail));
        }
        emailMessage.setSubject(emailSubject);

        MimeMultipart MPbody = new MimeMultipart();
        //First part of the email -- email body
        MimeBodyPart messagepart1 = new MimeBodyPart();
        messagepart1.setText("metawear data");

        //Second part -- ACC file attachment
        MimeBodyPart messagepart2 = new MimeBodyPart();
        File root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        String filenmA = sc + "_ACC_" + filedate;
        DataSource source = new FileDataSource(root + "/" + filenmA);
        DataHandler handler = new DataHandler(source);


        File file = new File(root, "/" + filenmA);
        if (!file.exists()) {
            Log.e("SendMailTask", "Acc File does not exist");
            return null;
        }
        if (!file.canRead()) {
            Log.e("SendMailTask","Acc File cannot be read");
            return null;
        }

        messagepart2.setDataHandler(handler);
        messagepart2.setFileName(filenmA);

        //Third part -- GYRO file attachment
        MimeBodyPart messagepart3 = new MimeBodyPart();
        String filenmG = sc + "_GYR_" + filedate;
        DataSource source2 = new FileDataSource(root + "/" + filenmG);
        DataHandler handler2 = new DataHandler(source2);


        File file2 = new File(root, "/" + filenmG);
        if (!file2.exists()) {
            Log.e("SendMailTask", "Gyro File does not exist");
            return null;
        }
        if (!file2.canRead()) {
            Log.e("SendMailTask","Gyro File cannot be read");
            return null;
        }

        messagepart3.setDataHandler(handler2);
        messagepart3.setFileName(filenmG);


        MPbody.addBodyPart(messagepart1);
        MPbody.addBodyPart(messagepart2);
        MPbody.addBodyPart(messagepart3);


        emailMessage.setContent(MPbody, "text/csv");// for a csv email?
        //emailMessage.setContent(MPbody, "text/html");// for a html email
        // emailMessage.setText(emailBody);// for a text email
        Log.i("GMail", "Email Message created.");
        return emailMessage;
    }

    public void sendEmail() throws AddressException, MessagingException {

        Transport transport = mailSession.getTransport("smtp");
        transport.connect(emailHost, fromEmail, fromPassword);
        Log.i("GMail", "allrecipients: " + emailMessage.getAllRecipients());
        transport.sendMessage(emailMessage, emailMessage.getAllRecipients());
        transport.close();
        Log.i("GMail", "Email sent successfully.");
    }

}