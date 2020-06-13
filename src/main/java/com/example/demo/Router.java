package com.example.demo;

import com.google.api.core.ApiFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.cloud.FirestoreClient;
import com.google.gson.Gson;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Charge;
import com.stripe.model.ChargeCollection;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.param.PaymentIntentCreateParams;
import io.github.cdimascio.dotenv.Dotenv;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ExecutionException;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@RestController
public class Router {

    Firestore db;
    FirebaseAuth auth;
    String id = "";
    String refreshToken = "";

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

    @RequestMapping(value = "/booking/{id}", method = GET)
    public JSONObject booking(@PathVariable("id") String id) throws Exception {
        JSONObject results = new JSONObject();
        DocumentReference docRef = db.collection("bookings").document(id);
        ApiFuture<DocumentSnapshot> booking = docRef.get();
        DocumentSnapshot document = booking.get();
        results.put(document.getId(), document.getData());
        return results;
    }

    @RequestMapping(value = "/book", method = POST)
    public Timestamp book(@RequestParam("firstName") String firstName, @RequestParam("lastName") String lastName, @RequestParam("email") String email, @RequestParam("flightId") String flightId, @RequestParam("seat") int seat, @RequestParam("intentId") String intentId) throws StripeException, ExecutionException, InterruptedException {

        Map<String, Object> docData = new HashMap<>();
        docData.put("firstName", firstName);
        docData.put("lastName", lastName);
        docData.put("email", email);
        docData.put("flightId", flightId);
        docData.put("seat", seat);
        docData.put("orderId", id);
        docData.put("payment_intent_id", intentId);
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
        map.put("payment_intent_id", intent.getId());

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

    @RequestMapping(value = "/allFlights", method = GET)
    public JSONObject allFlights() throws Exception {
        JSONObject allFlights = new JSONObject();
        ApiFuture<QuerySnapshot> query = db.collection("flights").get();
        List<QueryDocumentSnapshot> documents = query.get().getDocuments();
        for (QueryDocumentSnapshot document: documents) {
            allFlights.put(document.getId(), document.getData());
        }
        return allFlights;
    }

    @RequestMapping(value = "/allBookings", method = GET)
    public JSONObject allBookings() throws Exception {
        JSONObject allBookings = new JSONObject();
        ApiFuture<QuerySnapshot> query = db.collection("bookings").get();
        List<QueryDocumentSnapshot> documents = query.get().getDocuments();
        for (QueryDocumentSnapshot document: documents) {
            allBookings.put(document.getId(), document.getData());
        }
        return allBookings;
    }

    @RequestMapping(value = "/login", method = POST)
    public JSONObject login(@RequestParam("email") String email, @RequestParam("password") String password ) throws FirebaseAuthException, IOException {
        Dotenv dotenv = Dotenv.load();

        String url = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=" + dotenv.get("API_KEY");
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpPost httppost = new HttpPost(url);


        List<NameValuePair> params = new ArrayList<NameValuePair>(2);
        params.add(new BasicNameValuePair("email", email));
        params.add(new BasicNameValuePair("password", password));
        params.add(new BasicHeader("returnSecureToken", "true"));
        httppost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));

        HttpResponse response = httpclient.execute(httppost);
        HttpEntity entity = response.getEntity();
        String result = EntityUtils.toString(entity);
        JSONObject convertedObject = new Gson().fromJson(result, JSONObject.class);
        refreshToken = (String) convertedObject.get("refreshToken");

        return convertedObject;
    }

    @RequestMapping(value = "/getToken", method = GET)
    public String token() {
        return refreshToken;
    }


    @RequestMapping(value = "/deleteFlight", method = POST)
    public String deleteFlight(@RequestParam("id") String id) {
        boolean isDeleted = false;

        ApiFuture<WriteResult> writeResult = db.collection("flights").document(id).delete();

        isDeleted = true;

        return id;
    }

    @RequestMapping(value = "/deleteBooking", method = POST)
    public String deleteBooking(@RequestParam("id") String id) throws ExecutionException, InterruptedException, StripeException {
        boolean isDeleted = false;
        Dotenv dotenv = Dotenv.load();


        // Get booking and retrieve flight id and seat
        DocumentReference docRef = db.collection("bookings").document(id);
        ApiFuture<DocumentSnapshot> booking = docRef.get();
        DocumentSnapshot document = booking.get();
        var flightId = document.getString("flightId");
        DocumentReference docRefFlight = db.collection("flights").document(flightId);
        // Add seat back to flight
        ApiFuture<WriteResult> flightAddSeat = docRefFlight.update("availSeats", FieldValue.arrayUnion(document.getLong("seat")));

        // Remove booking
        ApiFuture<WriteResult> writeResult = db.collection("bookings").document(id).delete();


        // Refund user
        Stripe.apiKey = dotenv.get("STRIPE_SECRET_KEY");
        Map<String, Object> params = new HashMap<>();
        params.put(
                "payment_intent", document.getString("payment_intent_id")
        );
        Refund refund = Refund.create(params);

        isDeleted = true;

        return id;
    }

    @RequestMapping(value = "/saveBooking", method = POST)
    public String saveBooking(@RequestParam("id") String id, @RequestParam("flightId") String flightId, @RequestParam("firstName") String firstName, @RequestParam("lastName") String lastName, @RequestParam("email") String email, @RequestParam("seat") int seat, @RequestParam("oldSeat") int oldSeat) throws ExecutionException, InterruptedException {

        String message = "";

        CollectionReference bookingsCol = db.collection("bookings");
        Query checkForSeats = bookingsCol.whereEqualTo("flightId", flightId).whereEqualTo("seat", seat);
        ApiFuture<QuerySnapshot> querySnapshot = checkForSeats.get();
        for (DocumentSnapshot document: querySnapshot.get().getDocuments()) {
             message = "Seat already taken!";
             return message;
        }


        DocumentReference docRef = db.collection("bookings").document(id);
        ApiFuture<WriteResult> booking = docRef.update("firstName", firstName, "lastName", lastName, "email", email, "seat", seat);
        WriteResult result = booking.get();

        DocumentReference docRefFlight = db.collection("flights").document(flightId);

        ApiFuture<WriteResult> flightAddSeat = docRefFlight.update("availSeats", FieldValue.arrayUnion(oldSeat));
        ApiFuture<WriteResult> flightRemoveSeat = docRefFlight.update("availSeats", FieldValue.arrayRemove(seat));



        message = "Changes saved!";
        return message;
    }

    @RequestMapping(value = "/saveFlight", method = POST)
    public String saveFlight(@RequestParam("id") String id, @RequestParam("takeOff") String takeOff, @RequestParam("destination") String destination, @RequestParam("duration") String duration, @RequestParam("departureTime") String departureTime, @RequestParam("departureDate") Date departureDate, @RequestParam("price") int price) throws ExecutionException, InterruptedException {

        String message = "";

        DocumentReference docRef = db.collection("flights").document(id);
        ApiFuture<WriteResult> flight = docRef.update("takeOff", takeOff, "destination", destination, "duration", duration, "departureTime", departureTime, "departureDate", departureDate, "price", price);
        WriteResult result = flight.get();

        message = "Changes saved!";
        return message;
    }
}
