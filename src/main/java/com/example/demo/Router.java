package com.example.demo;

import com.google.api.core.ApiFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import io.github.cdimascio.dotenv.Dotenv;
import org.json.simple.JSONObject;
import org.springframework.http.MediaType;
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
import java.util.concurrent.ExecutionException;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@RestController
public class Router {

    Firestore db;
    String id = "";

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
    public Timestamp book(@RequestParam("firstName") String firstName, @RequestParam("lastName") String lastName, @RequestParam("email") String email, @RequestParam("flightId") String flightId, @RequestParam("seat") String seat) throws StripeException, ExecutionException, InterruptedException {

        Map<String, Object> docData = new HashMap<>();
        docData.put("firstName", firstName);
        docData.put("lastName", lastName);
        docData.put("email", email);
        docData.put("flightId", flightId);
        docData.put("seat", seat);
        docData.put("orderId", id);
        ApiFuture<WriteResult> booking = db.collection("bookings").document().set(docData);

        ApiFuture<WriteResult> removeSeat = db.collection("flights").document(flightId).update("availSeats", FieldValue.arrayRemove(seat));

        EmailSender.main(email, id);

        return booking.get().getUpdateTime();
    }

    @RequestMapping(value = "/secret/{email}", produces = MediaType.APPLICATION_JSON_VALUE, method = GET)
    public Map<String, String> secret(@PathVariable String email) throws StripeException {
        Dotenv dotenv = Dotenv.load();

        DocumentReference ref = db.collection("bookings").document();
        id = ref.getId();

        Stripe.apiKey = dotenv.get("STRIPE_SECRET_KEY");
        PaymentIntentCreateParams params =
                PaymentIntentCreateParams.builder()
                        .setCurrency("eur")
                        .setAmount(5000L)
                        .setDescription(id)
                        .setReceiptEmail(email)
                        .putMetadata("integration_check", "accept_a_payment")
                        .build();
        PaymentIntent intent = PaymentIntent.create(params);

        Map<String, String> map = new HashMap();
        map.put("client_secret", intent.getClientSecret());

        return map;
    }


    @RequestMapping(value = "/flightByOrder/{orderNumber}", method = GET)
    public JSONObject flightByOrder(@PathVariable String orderNumber) throws ExecutionException, InterruptedException {
        JSONObject flightDetails = new JSONObject();
        String flightId = "";
        ApiFuture<QuerySnapshot> querySnapshot = db.collection("bookings").whereEqualTo("orderId", orderNumber).get();
        List<QueryDocumentSnapshot> documents = querySnapshot.get().getDocuments();
        for (DocumentSnapshot document : documents) {
            flightDetails.put(0, document.getData());
            flightId = document.getString("flightId");
            DocumentReference docRef = db.collection("flights").document(flightId);
            ApiFuture<DocumentSnapshot> flight = docRef.get();
            DocumentSnapshot document2 = flight.get();
            flightDetails.put(1, document2.getData());
        }

        return flightDetails;
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
