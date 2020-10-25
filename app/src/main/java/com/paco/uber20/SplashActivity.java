package com.paco.uber20;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthMethodPickerLayout;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.paco.uber20.Model.driverModel;
import com.paco.uber20.Model.riderModel;
import com.paco.uber20.Utils.UserUtils;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Action;
public class SplashActivity extends AppCompatActivity {
    private final static int LOGIN_REQUEST_CODE = 6666;
    private List<AuthUI.IdpConfig> providers = new ArrayList<>();
    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener listener;
    FirebaseDatabase database;
    DatabaseReference driverInfoRef;
    DatabaseReference riderInfoRef;
    DatabaseReference userRef;
    ImageView logo;
    String driverUid, riderUid;
    TextView company,slogan;
    @Override
    protected void onStart() {
        super.onStart();
        delaySplashScreen();
    }
    @Override
    protected void onStop() {
        if(firebaseAuth !=null && listener !=null)
            firebaseAuth.removeAuthStateListener(listener);
        super.onStop();
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash);
        logo = findViewById(R.id.logo);
        company = findViewById(R.id.company_name);
        slogan = findViewById(R.id.company_slogan);
        init();

    }
    private void init() {
        database = FirebaseDatabase.getInstance();
        driverInfoRef = database.getReference(Common.DRIVER_INFO_REFERENCE);
        riderInfoRef = database.getReference(Common.RIDER_INFO_REFERENCE);
        userRef = database.getReference(Common.USERS_INFO_REFERENCE);
        providers.add(new AuthUI.IdpConfig.GoogleBuilder().build());
        providers.add(new AuthUI.IdpConfig.PhoneBuilder().build());
        firebaseAuth = FirebaseAuth.getInstance();
        listener = myFirebaseAuth->{
            FirebaseUser user = myFirebaseAuth.getCurrentUser();
            if(user !=null) {
                //update Token
                FirebaseInstanceId
                        .getInstance()
                        .getInstanceId().addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(SplashActivity.this, e.getMessage(),
                                Toast.LENGTH_SHORT).show();

                    }
                }).addOnSuccessListener(new OnSuccessListener<InstanceIdResult>() {
                    @Override
                    public void onSuccess(InstanceIdResult instanceIdResult) {
                        Log.d("TOKEN : ",instanceIdResult.getToken());
                        UserUtils.updateToken(SplashActivity.this, instanceIdResult.getToken());
                    }
                });
                checkUserFromFirebase();
            }else {
                showLoginLayout();
            }
        };
    }
    private void checkUserFromFirebase() {
        userRef.child("DriverInfo").child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    Toast.makeText(SplashActivity.this, "User Already Exists!", Toast.LENGTH_SHORT).show();
                    driverModel driver = dataSnapshot.getValue(driverModel.class);
                    gotoDashboard(driver);
                    Toast.makeText(SplashActivity.this, "Rider Dashboard", Toast.LENGTH_SHORT).show();

                }else{
                    chooseUserType();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(SplashActivity.this, "[ERROR] => "+databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        userRef.child("Riders").child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(dataSnapshot.exists()){
                            Toast.makeText(SplashActivity.this, "User Already Exists!", Toast.LENGTH_SHORT).show();
                            riderModel rider = dataSnapshot.getValue(riderModel.class);
                            Toast.makeText(SplashActivity.this, "Rider Dashboard", Toast.LENGTH_SHORT).show();

                        }else{
                            chooseUserType();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Toast.makeText(SplashActivity.this, "[ERROR] => No Rider Exists "+databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });


}

    private void gotoDashboard(driverModel driver) {
        Common.currentDriver = driver;
        startActivity(new Intent(SplashActivity.this, DriverHome.class));
        finish();
    }
    private void gotoUserDashboard(riderModel rider) {
        Common.currentUser = rider;
        startActivity(new Intent(SplashActivity.this, DriverHome.class));
        finish();
    }
    private void chooseUserType() {
        AlertDialog.Builder userTypeBuilder = new AlertDialog.Builder(this);
        View itemView = LayoutInflater.from(this).inflate(R.layout.user_type,null);
        Button btnDriver = itemView.findViewById(R.id.driver);
        Button btnrider = itemView.findViewById(R.id.rider);
        userTypeBuilder.setView(itemView);
        AlertDialog dialog = userTypeBuilder.create();
        dialog.show();
        dialog.setCancelable(false);
        btnDriver.setOnClickListener(v -> {
            Intent intent = new Intent(SplashActivity.this,registerDriver.class);
            startActivity(intent);
            finish();

        });
        btnrider.setOnClickListener(v -> {
            Intent intent = new Intent(SplashActivity.this,registerUser.class);
            startActivity(intent);
            finish();
        });
    }
    private void showLoginLayout() {
        AuthMethodPickerLayout customLayout = new AuthMethodPickerLayout
                .Builder(R.layout.layout_sign_in)
                .setPhoneButtonId(R.id.btn_phone_sign_in)
                .setGoogleButtonId(R.id.btn_google_sign_in)
                .build();
        AuthUI.SignInIntentBuilder builder = AuthUI.getInstance().createSignInIntentBuilder();
        startActivityForResult(builder.setAuthMethodPickerLayout(customLayout)
        .setIsSmartLockEnabled(false)
        .setAvailableProviders(providers)
                .setTheme(R.style.LoginTheme)
        .build(),LOGIN_REQUEST_CODE);
    }
    @SuppressLint("CheckResult")
    private void delaySplashScreen() {
        logo.setVisibility(View.VISIBLE);
        Completable.timer(1,
                TimeUnit.SECONDS,
                AndroidSchedulers.mainThread())
                .subscribe(() -> firebaseAuth.addAuthStateListener(listener));
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == LOGIN_REQUEST_CODE){
            IdpResponse response = IdpResponse.fromResultIntent(data);
            if(resultCode == RESULT_OK){
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            }else{
                Toast.makeText(this, "[ERROR] :"+response.getError().getMessage(), Toast.LENGTH_SHORT).show();
            }

        }
    }
    public void driverKey(){
        driverInfoRef.orderByChild("name").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot d: dataSnapshot.getChildren()) {
                    driverUid = d.child("uid").getValue(String.class);
                    Toast.makeText(SplashActivity.this, "Welcome :"+driverUid, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(SplashActivity.this, "ERROR GETTING DRIVER ID "+ databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();

            }
        });

    }
    public void riverKey(){
        riderInfoRef.orderByChild("name").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot d: dataSnapshot.getChildren()) {
                    riderUid = d.child("uid").getValue(String.class);
                    Toast.makeText(SplashActivity.this, "Welcome :"+riderUid, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(SplashActivity.this, "ERROR GETTING DRIVER ID "+ databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();

            }
        });

    }
}