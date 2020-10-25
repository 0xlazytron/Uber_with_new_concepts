package com.paco.uber20;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.paco.uber20.Model.driverModel;

public class registerDriver extends AppCompatActivity {
EditText name, phone, cnic, email,password;
RadioButton male, female,radioBtn;
String gender;
Button cont;
RadioGroup radio;
FirebaseDatabase database;
DatabaseReference driverInfoRef;
private FirebaseAuth firebaseAuth;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register_driver);
        name = findViewById(R.id.drvName);
        phone = findViewById(R.id.drvPhone);
        cnic = findViewById(R.id.drvCnic);
        email = findViewById(R.id.drvEmail);
        password = findViewById(R.id.drvPassword);
        male = findViewById(R.id.drvMale);
        female = findViewById(R.id.drvFemale);
        cont = findViewById(R.id.drvContinue);
        radio = (RadioGroup) findViewById(R.id.gender);
        int radioId = radio.getCheckedRadioButtonId();
        radioBtn = findViewById(radioId);
        if(FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber() !=null && !TextUtils.isEmpty(FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber())){
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
        driverInfoRef = database.getReference(Common.DRIVER_INFO_REFERENCE);
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
        }else if(TextUtils.isEmpty(cnic.getText())){
             cnic.setError("cnic should not be empty!");
             cnic.setTextColor(R.color.chili);
        }else if(TextUtils.isEmpty(email.getText())){
             email.setError("email should not be empty!");
             email.setTextColor(R.color.chili);
        }else if(TextUtils.isEmpty(password.getText())){
             password.setError("password should not be empty!");
             password.setTextColor(R.color.chili);
         }else{
            driverModel driver = new driverModel();
            driver.setName(name.getText().toString());
            driver.setPhone(phone.getText().toString());
            driver.setCnic(cnic.getText().toString());
            driver.setEmail(email.getText().toString());
            driver.setPassword(password.getText().toString());
            driver.setGender(gender);
            driverInfoRef.child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                    .setValue(driver)
                    .addOnFailureListener(e -> Toast.makeText(registerDriver.this, e.getMessage(), Toast.LENGTH_SHORT).show())
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "[SUCCESS] => THANK YOU FOR JOINING US!", Toast.LENGTH_LONG).show();
                        Intent goDriverHome = new Intent(registerDriver.this, DriverHome.class);
                        startActivity(goDriverHome);
                        finish();
                    });
         }
    }
}