package com.example.demo;

import com.google.api.core.ApiFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import com.stripe.Stripe;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import io.github.cdimascio.dotenv.Dotenv;
import org.json.simple.JSONObject;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

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

    @RequestMapping(value = "/flight/{id}", method = GET)
    public JSONObject flights(@PathVariable("id") String id) throws Exception {
        JSONObject results = new JSONObject();
        DocumentReference docRef = db.collection("flights").document(id);
        ApiFuture<DocumentSnapshot> flight = docRef.get();
        DocumentSnapshot document = flight.get();
        results.put(document.getId(), document.getData());
        return results;
    }

    @RequestMapping(value = "/book", method = POST)
    public String book(@RequestParam("firstName") String firstName, @RequestParam("lastName") String lastName, @RequestParam("email") String email, @RequestParam("flightId") String flightId, @RequestParam("seat") String seat) {
        Dotenv dotenv = Dotenv.load();


        Stripe.apiKey = dotenv.get("STRIPE_SECRET_KEY");

        PaymentIntentCreateParams params =
                PaymentIntentCreateParams.builder()
                        .setCurrency("eur")
                        .setAmount(1099L)
                        // Verify your integration in this guide by including this parameter
                        .putMetadata("integration_check", "accept_a_payment")
                        .build();

        PaymentIntent intent = PaymentIntent.create(params);


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
