package com.example.stripeextensiontest;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;
import com.razorpay.Checkout;
import com.razorpay.PaymentData;
import com.razorpay.PaymentResultListener;
import com.razorpay.PaymentResultWithDataListener;
import com.stripe.android.PaymentConfiguration;
import com.stripe.android.paymentsheet.PaymentSheet;
import com.stripe.android.paymentsheet.PaymentSheetResult;

import org.checkerframework.checker.signature.qual.FieldDescriptor;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


public class MainActivity extends AppCompatActivity implements PaymentResultWithDataListener {

    MaterialButton button,loginButton;
    TextInputEditText email, password;
    String emailText = "", passText = "";
    TextView tv;

    FirebaseAuth mAuth;
    FirebaseUser user;
    FirebaseFunctions mFunctions;
    FirebaseFirestore db;
    String orderId="";

    TextView paymentStatus,paymentSubStatus, regStatus,paymentStatusEnding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        button = findViewById(R.id.button);
        loginButton = findViewById(R.id.loginButton);
        email = findViewById(R.id.emailText);
        password = findViewById(R.id.passText);
        tv = findViewById(R.id.textView);
        paymentStatus = findViewById(R.id.textView2);
        paymentSubStatus = findViewById(R.id.textView3);
        regStatus = findViewById(R.id.textView4);
        paymentStatusEnding = findViewById(R.id.textView5);

        mFunctions = FirebaseFunctions.getInstance("asia-south1");
        mAuth = FirebaseAuth.getInstance();
        db=FirebaseFirestore.getInstance();

        Checkout.preload(getApplicationContext());

        if (user != null) {
            tv.setText("Signed in as" + user.getUid());
        }

        email.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                emailText = charSequence.toString().trim();
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        password.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                passText = charSequence.toString().trim();
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        loginButton.setOnClickListener(view -> mAuth.signInWithEmailAndPassword(emailText, passText)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        user = mAuth.getCurrentUser();
                        Toast.makeText(MainActivity.this, "Authentication successful.",
                                Toast.LENGTH_SHORT).show();
                        tv.setText("Signed in as" + user.getUid());

                    } else {
                        Toast.makeText(MainActivity.this, "Authentication failed.",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(MainActivity.this, "Authentication failed.(from onFailuerListener)",
                        Toast.LENGTH_LONG).show()));

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //set wait screen visible

                //razorpay payment start from here

                String userId = "";
                String razorId = "";
                String eventId ="";
                String regId =""; //todo: set all these values

                fetchOrderId(userId,razorId,eventId,regId).addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {

                        if(task.isSuccessful()){

                            startRazorPayment(task.getResult(),regId,razorId);
                        }
                        else{
                            Log.i("error",task.getException().getMessage());

                        }

                    }
                });
            }
        });


    }



    private void startRazorPayment(String jsonResponseString, String regId, String razorId) {

        Checkout checkout = new Checkout();
        checkout.setKeyID("//write your key here"); //todo: change to live key

        try {
            JSONObject res = new JSONObject(jsonResponseString);

            JSONObject options = new JSONObject();

            //
            options.put("order_id", res.getString("id"));
            orderId = res.getString("id");

            options.put("currency", res.getString("currency"));
            options.put("amount", res.getString("amount"));
            //


            options.put("name", "Event Name");
            options.put("image", "https://s3.amazonaws.com/rzp-mobile/images/rzp.png");
            options.put("description", "Event Description");
            options.put("theme.color", "#3399cc");
            options.put("prefill.name","Neelesh");
            options.put("prefill.email", "neelesh9924@gmail.com");
            options.put("prefill.contact","+919999999999");

            JSONObject retryObj = new JSONObject();
            retryObj.put("enabled", true);
            retryObj.put("max_count", 0); //made change here

            options.put("retry", retryObj);

            JSONObject notes = new JSONObject();
            notes.put("registrationId", regId);
            notes.put("userId", regId);
            notes.put("eventId", regId); //added this line

            options.put("notes",notes);

            options.put("send_sms_hash", true);
            options.put("readonly.name", true);
            options.put("readonly.contact", true);
            options.put("readonly.email", true);
            options.put("timeout", 600);

            options.put("customer_id",razorId);
            options.put("remember_customer",true);

            checkout.open(MainActivity.this, options);

        } catch (JSONException e) {

            Log.e("errorJSON","Error in starting Razorpay Checkout: "+e);
            e.printStackTrace();

        }

    }

    @Override
    public void onPaymentSuccess(String paymentId, PaymentData paymentData) {

        paymentSuccess(paymentData.getOrderId(),paymentData.getPaymentId(),paymentData.getSignature(),"//current reg id").addOnCompleteListener(new OnCompleteListener<Boolean>() {
            @Override
            public void onComplete(@NonNull Task<Boolean> task) {
                if(task.isSuccessful()){
                    Toast.makeText(MainActivity.this, "Payment Successful", Toast.LENGTH_SHORT).show();
                }
                else{
                    Log.i("error",task.getException().getMessage());
                }

                //completed
            }
        });

        navigateSuccess();

    }

    @Override
    public void onPaymentError(int i, String response, PaymentData paymentData) {


        String errorCode="",errorDescription="",errorToShow="";

        if(i==Checkout.NETWORK_ERROR){
            errorCode = "NETWORK_ERROR";
            errorDescription = "There was a network error, for example, loss of internet connectivity.";
            errorToShow = "Payment failed due to a network error. Please try again.";
        }
        else if(i==Checkout.INVALID_OPTIONS){
            errorCode = "INVALID_OPTIONS";
            errorDescription = "An issue with options passed in checkout.open";
            errorToShow = "Payment failed due to checkout error. Please try again.";
        }
        else if(i==Checkout.PAYMENT_CANCELED){
            errorCode = "PAYMENT_CANCELED";
            errorDescription = "The user canceled the payment.";
            errorToShow = "The payment got failed/cancelled."; //todo made changes here
        }
        else if(i==Checkout.TLS_ERROR){
            errorCode = "TLS_ERROR";
            errorDescription = "The device does not support TLS v1.1 or TLS v1.2.";
            errorToShow = "The device doesn't meet the security norms, Please try again.";
        }
        else{
            errorCode = "UNKNOWN_ERROR";
            errorDescription = "Unknown error: Response code is unknown!";
            errorToShow = "Payment failed due to unknown error. Please try again.";
        }

        //made changes here
        paymentFailure(paymentData.getOrderId(),errorCode,errorDescription,response,"//current reg id").addOnCompleteListener(new OnCompleteListener<Boolean>() {
            @Override
            public void onComplete(@NonNull Task<Boolean> task) {
                if(task.isSuccessful()){
                    Toast.makeText(MainActivity.this, "Payment Failed", Toast.LENGTH_SHORT).show();
                }
                else{
                    Log.i("error",task.getException().getMessage());

                }

                //failed

            }
        });

        navigateFailure(errorToShow);
    }

    private void navigateSuccess() {

        db.collection("registrations").document("wAIVdZcOBTWRrdMeNVRj").addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot doc, @Nullable FirebaseFirestoreException error) {
                if(error!=null){
                    Log.i("error",error.getMessage());
                }
                else{
                    if(doc.exists()){
                        //registration doc exists

                        if(doc.getLong("paymentAuthTimeout") != null){
                            //amount paid
                            Long timeoutInMillisUnix = doc.getLong("paymentAuthTimeout");
                            Long timeoutInMillis = timeoutInMillisUnix*1000;

                            Long authTimeInUnix = doc.getLong("paymentAuthorizedAt");
                            Long authTime = authTimeInUnix*1000;

                            if(doc.getString("registrationStatus") != null){
                                //registrationStatus exists
                                String status = doc.getString("registrationStatus");

                                if(status.equals("successful")){

                                    //navigate to success screen
                                    Toast.makeText(MainActivity.this, "Payment Successful", Toast.LENGTH_SHORT).show();
                                    //todo: navigate to success screen
                                    paymentStatus.setText("Payment Successful");
                                    paymentSubStatus.setText("at "+new Date(authTime));
                                    regStatus.setText("Registration Status: Successful");
                                    paymentStatusEnding.setText("Payment Status: Successful");

                                }
                                else if(status.equals("pending")){

                                    //navigate to pending screen
                                    Toast.makeText(MainActivity.this, "Payment Pending", Toast.LENGTH_SHORT).show();

                                    //todo Show pending screen

                                    //logic to check if payment surpassed timeout or not

                                    Long currentTime = System.currentTimeMillis();

                                    Log.i("currentTime",currentTime.toString());
                                    Log.i("currentTime",timeoutInMillis.toString());

                                    if(currentTime > timeoutInMillis){
                                        //payment timeout
                                        paymentStatus.setText("Payment Failed");
                                        paymentSubStatus.setText("Any amount paid will be refunded within 5-7 working days.");
                                        regStatus.setText("Registration Status: Pending");
                                        paymentStatusEnding.setText("Payment Status: Pending");
                                    }
                                    else{
                                        //payment not timed out
                                        paymentStatus.setText("Payment Pending");
                                        paymentSubStatus.setText("We're processing your payment. Please wait for 12-15 minutes.");
                                        regStatus.setText("Registration Status: Pending");
                                        paymentStatusEnding.setText("Payment Status: Pending");

                                        //add a reminder mail to be sent to the user after 12-15 minutes for final status

                                    }


                                }

                            }
                        }
                        else{

                            //amount not paid
                            Toast.makeText(MainActivity.this, "Payment Failed", Toast.LENGTH_SHORT).show();
                            paymentStatus.setText("Payment Failed");
                            paymentSubStatus.setText("Any amount paid will be refunded within 5-7 working days.");
                            regStatus.setText("Registration Status: Pending");
                            paymentStatusEnding.setText("Payment Status: Failed");

                        }

                    }

                    else{
                        //invalid registration, reg doc doesn't exist
                        Toast.makeText(MainActivity.this, "Invalid registration", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

    }

    private void navigateFailure(String errorToShow) {

        //todo payment failed

        paymentStatus.setText("Payment Failed");
        paymentSubStatus.setText(errorToShow);
        regStatus.setText("Registration Status: Pending");
        paymentStatusEnding.setText("Payment Status: Failed");

    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        super.onPointerCaptureChanged(hasCapture);
    }

    private Task<String> fetchOrderId(String uid, String razorId, String eventId, String regId) {

        Map<String, Object> data = new HashMap<>();
        data.put("uid", uid);
        data.put("razorId", razorId);
        data.put("eventId", eventId);
        data.put("registrationId",regId);

        return mFunctions.getHttpsCallable("fetchOrderId")
                .call(data)
                .continueWith(new Continuation<HttpsCallableResult, String>() {
                    @Override
                    public String then(@NonNull Task<HttpsCallableResult> task) throws Exception {

                        String result = (String) task.getResult().getData();

                        return result;
                    }

                });

    }

    private Task<Boolean> paymentSuccess(String orderId, String paymentId, String signature, String currentRegId) {

        Map<String,String> functionCallData = new HashMap<>();

        functionCallData.put("orderId",orderId);
        functionCallData.put("paymentId",paymentId);
        functionCallData.put("signature",signature);
        functionCallData.put("registrationId",currentRegId);

        return mFunctions.getHttpsCallable("paymentSuccess")
                .call(functionCallData)
                .continueWith(new Continuation<HttpsCallableResult, Boolean>() {
                    @Override
                    public Boolean then(@NonNull Task<HttpsCallableResult> task) throws Exception {

                        return null;
                    }

                });
    }

    private Task<Boolean> paymentFailure(String orderId, String errorCode, String errorDescription, String errorResponse, String currentRegId) {

        Map<String,Object> functionCallData = new HashMap<>();
        functionCallData.put("orderId",orderId);
        functionCallData.put("registrationId",currentRegId);

        functionCallData.put("errorCode",errorCode);
        functionCallData.put("errorDescription",errorDescription);
        functionCallData.put("errorResponse",errorResponse);

        return mFunctions.getHttpsCallable("paymentFailure")
                .call(functionCallData)
                .continueWith(new Continuation<HttpsCallableResult, Boolean>() {
                    @Override
                    public Boolean then(@NonNull Task<HttpsCallableResult> task) throws Exception {

                        return null;
                    }

                });
    }



}