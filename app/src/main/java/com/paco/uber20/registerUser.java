package com.paco.uber20;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.paco.uber20.Model.driverModel;
import com.paco.uber20.Model.riderModel;

public class registerUser extends AppCompatActivity {
    EditText name, phone,email,password;
    RadioButton male, female,radioBtn;
    String gender;
    Button cont;
    RadioGroup radio;
    FirebaseDatabase database;
    DatabaseReference riderInfoRef;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register_user);
        name = findViewById(R.id.rdrName);
        phone = findViewById(R.id.rdrPhone);
        email = findViewById(R.id.rdrEmail);
        password = findViewById(R.id.rdrPassword);
        male = findViewById(R.id.rdrMale);
        female = findViewById(R.id.rdrFemale);
        cont = findViewById(R.id.rdrContinue);
        radio = (RadioGroup) findViewById(R.id.gender);
        int radioId = radio.getCheckedRadioButtonId();
        radioBtn = findViewById(radioId);
        if(FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber() !=null
                && !TextUtils.isEmpty(FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber())){
            phone.setText(FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber());
            phone.setEnabled(false);
        }
        if(FirebaseAuth.getInstance().getCurrentUser().getEmail() !=null && !TextUtils.isEmpty(FirebaseAuth.getInstance().getCurrentUser().getEmail())){
            email.setText(FirebaseAuth.getInstance().getCurrentUser().getEmail());
            email.setEnabled(false);
        }
        cont.setOnClickListener(v -> validateUser());
    }
    @SuppressLint("ResourceAsColor")
    private void validateUser() {
        database = FirebaseDatabase.getInstance();
        riderInfoRef = database.getReference(Common.RIDER_INFO_REFERENCE);
        if(male.isChecked()){
            gender = "male";
        }else{
            gender = "female";
        }
        if(TextUtils.isEmpty(name.getText())){
            name.setError("Name should not be empty!");
            name.setTextColor(R.color.chili);
        }else if(TextUtils.isEmpty(phone.getText())){
            phone.setError("phone should not be empty!");
            phone.setTextColor(R.color.chili);
        }else if(TextUtils.isEmpty(email.getText())){
            email.setError("email should not be empty!");
            email.setTextColor(R.color.chili);
        }else if(TextUtils.isEmpty(password.getText())){
            password.setError("password should not be empty!");
            password.setTextColor(R.color.chili);
        }else{
            riderModel rider = new riderModel();
            rider.setName(name.getText().toString());
            rider.setPhone(phone.getText().toString());
            rider.setEmail(email.getText().toString());
            rider.setPassword(password.getText().toString());
            rider.setGender(gender);
            riderInfoRef.child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                    .setValue(rider)
                    .addOnFailureListener(e -> Toast.makeText(registerUser.this, e.getMessage(), Toast.LENGTH_SHORT).show())
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "[SUCCESS] => THANK YOU FOR JOINING US!", Toast.LENGTH_LONG).show();
                        Intent goRiderHome = new Intent(registerUser.this, DriverHome.class);
                        startActivity(goRiderHome);
                        finish();
                    });
        }
    }
    }
