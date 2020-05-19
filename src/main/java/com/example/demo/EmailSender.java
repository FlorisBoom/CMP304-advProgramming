package com.example.demo;

import com.sun.mail.smtp.SMTPTransport;
import io.github.cdimascio.dotenv.Dotenv;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Date;
import java.util.Properties;

public class EmailSender {

    static Dotenv dotenv = Dotenv.load();
    private static final String SMTP_SERVER = "smtp.gmail.com";
    private static final String USERNAME = "florisboom1@gmail.com";
    private static final String PASSWORD = dotenv.get("SMTP_PASSWORD");

    private static final String EMAIL_FROM = "florisboom1@gmail.com";
    private static String EMAIL_TO = "";
    private static final String EMAIL_TO_CC = "";

    private static final String EMAIL_SUBJECT = "Order confirmation";
    private static  String EMAIL_TEXT = "";

    public static void main(String email, String id) {

        EMAIL_TO = email;
        EMAIL_TEXT = "Ur confirmation for booking flight: " + id + "/n" + "Use this on the homepage to lookup flight details";

        Properties prop = System.getProperties();
        prop.put("mail.smtp.host", SMTP_SERVER); //optional, defined in SMTPTransport
        prop.put("mail.smtp.auth", "true");
        prop.put("mail.smtp.starttls.enable", "true");
        prop.put("mail.smtp.port", "587"); // default port 25

        Session session = Session.getInstance(prop, null);
        Message msg = new MimeMessage(session);

        try {

            // from
            msg.setFrom(new InternetAddress(EMAIL_FROM));

            // to
            msg.setRecipients(Message.RecipientType.TO,
                    InternetAddress.parse(EMAIL_TO, false));

            // cc
            msg.setRecipients(Message.RecipientType.CC,
                    InternetAddress.parse(EMAIL_TO_CC, false));

            // subject
            msg.setSubject(EMAIL_SUBJECT);

            // content
            msg.setText(EMAIL_TEXT);

            msg.setSentDate(new Date());

            // Get SMTPTransport
            SMTPTransport t = (SMTPTransport) session.getTransport("smtp");

            // connect
            t.connect(SMTP_SERVER, USERNAME, PASSWORD);

            // send
            t.sendMessage(msg, msg.getAllRecipients());

            System.out.println("Response: " + t.getLastServerResponse());

            t.close();

        } catch (MessagingException e) {
            e.printStackTrace();
        }


    }
}
