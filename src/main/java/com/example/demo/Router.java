package com.example.demo;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import com.google.protobuf.Api;
import org.json.simple.JSONObject;
import org.springframework.web.bind.annotation.*;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

import javax.validation.constraints.Email;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.web.bind.annotation.RequestMethod.*;

@RestController
public class Router {

    Firestore db;

    public Router() throws IOException {
        InputStream serviceAccount = new FileInputStream("cmp304-advprogramming-firebase-adminsdk-eaken-75845526b6.json");
        GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccount);
        FirebaseOptions options = new FirebaseOptions.Builder()
                .setCredentials(credentials)
                .build();
        FirebaseApp.initializeApp(options);
        db = FirestoreClient.getFirestore();
    }


    @RequestMapping(value = "/flights/{date}/{airport}", method = GET)
    public JSONObject flights(@PathVariable("date") Date date, @PathVariable("airport") String airport) throws Exception {
        JSONObject results = new JSONObject();
        ApiFuture<QuerySnapshot> flights = db.collection("flights").whereEqualTo("takeOff", airport).whereEqualTo("departureDate", date).get();
        List<QueryDocumentSnapshot> documents = flights.get().getDocuments();
        for (QueryDocumentSnapshot document : documents) {
            results.put(document.getId(), document.getData());
        }
        return results;
    }

    @RequestMapping(value = "/book", method = POST)
    public String book(@RequestParam("firstName") String firstName, @RequestParam("lastName") String lastName, @RequestParam("email") String email, @RequestParam("flightId") String flightId, @RequestParam("seat") String seat) {
        Map<String, Object> docData = new HashMap<>();
        docData.put("firstName", firstName);
        docData.put("lastName", lastName);
        docData.put("email", email);
        docData.put("flightId", flightId);
        docData.put("seat", seat);
        ApiFuture<WriteResult> booking = db.collection("bookings").document().set(docData);


        return "test";
    }


    @RequestMapping(value = "/test", method = GET)
    public JSONObject test() throws Exception {
        JSONObject results = new JSONObject();
        ApiFuture<QuerySnapshot> flights = db.collection("flights").whereEqualTo("takeOff", "Newcastle").whereEqualTo("departureDate", "2020-02-29").get();
        List<QueryDocumentSnapshot> documents = flights.get().getDocuments();
        for (QueryDocumentSnapshot document : documents) {
            results.put(document.getId(), document.getData());
        }
        return results;
    }
}
