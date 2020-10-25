package com.paco.uber20;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.Menu;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.util.GAuthToken;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.paco.uber20.Utils.UserUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DriverHome extends AppCompatActivity {
    private static final int IMAGE_PICK = 7172;
    private AppBarConfiguration mAppBarConfiguration;
    private DrawerLayout drawer;
    private NavigationView navigationView;
    NavController navController;
    private AlertDialog waitingDialog;
    private StorageReference storageReference;
    private Uri ImageUri;
    ImageView img_avatar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.driver_home);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
       drawer = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home)
                .setDrawerLayout(drawer)
                .build();
        navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
        init();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == IMAGE_PICK && resultCode == Activity.RESULT_OK){
            if(data != null && data.getData()!=null){
                ImageUri = data.getData();
                img_avatar.setImageURI(ImageUri);
                showDialogUpload();

            }
        }
    }

    private void showDialogUpload() {
        AlertDialog.Builder builder = new AlertDialog.Builder(DriverHome.this);
        builder.setTitle("Change Avatar");
        builder.setMessage("Do You Really Want to Change Avatar")
                .setNegativeButton("CANCEL", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("UPLOAD", (dialog, which) -> {
                   if(ImageUri!=null){
                       waitingDialog.setMessage("uploading.....");
                       waitingDialog.show();
                       String unique_name = FirebaseAuth.getInstance().getCurrentUser().getUid();
                       StorageReference avatarFolder = storageReference.child("avatars/"+unique_name);
                       avatarFolder.putFile(ImageUri)
                               .addOnFailureListener(e -> Snackbar.make(drawer,e.getMessage(),Snackbar.LENGTH_SHORT).show())
                               .addOnCompleteListener(task -> {
                                   if(task.isSuccessful()){
                                       avatarFolder.getDownloadUrl().addOnSuccessListener(uri -> {
                                           Map<String,Object>updateData = new HashMap<>();
                                           updateData.put("avatar",uri.toString());
                                           UserUtils.updatesUser(drawer,updateData);
                                       });
                                   }
                                   waitingDialog.dismiss();
                               }).addOnProgressListener(taskSnapshot -> {
                                double progress = (100.0 *taskSnapshot.
                                        getBytesTransferred()/taskSnapshot.
                                        getTotalByteCount());
                                waitingDialog.setMessage(new StringBuilder("uploading:").append(progress).append("%"));
                               });
                   }
                }).setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dialog1 -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                    .setTextColor(getResources().getColor(R.color.black ));
        });
        dialog.show();
    }

    private void init() {
 waitingDialog = new AlertDialog.Builder(this)
         .setCancelable(false)
         .setMessage("Uploding....")
         .create();
 storageReference = FirebaseStorage.getInstance().getReference();

        navigationView.setNavigationItemSelectedListener(item -> {
            if(item.getItemId()==R.id.nav_sign_out){
                AlertDialog.Builder builder = new AlertDialog.Builder(DriverHome.this);
                builder.setTitle("Sign Out");
                builder.setMessage("Do You Really Want to Sign Out?")
                        .setNegativeButton("CANCEL", (dialog, which) -> dialog.dismiss())
                        .setPositiveButton("SIGN OUT", (dialog, which) -> {
                            FirebaseAuth.getInstance().signOut();
                            Intent signout = new Intent(DriverHome.this,SplashActivity.class);
//                                    signout.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(signout);
                            finish();
                        }).setCancelable(false);
                AlertDialog dialog = builder.create();
                dialog.setOnShowListener(dialog1 -> {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                            .setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                            .setTextColor(getResources().getColor(R.color.black ));
                });
                dialog.show();
            }
            return true;
        });
        //setting Data For User
        View headerView = navigationView.getHeaderView(0);
        TextView txt_name = (TextView)headerView.findViewById(R.id.txt_name);
        TextView txt_phone = (TextView)headerView.findViewById(R.id.txt_phone);
        TextView txt_star = (TextView)headerView.findViewById(R.id.txt_star);
         img_avatar = (ImageView)headerView.findViewById(R.id.img_avatar);

        txt_name.setText(Common.buildWelcomeMessage());
        txt_phone.setText(Common.currentDriver!=null ? Common.currentDriver.getPhone() : " ");
        txt_star.setText(Common.currentDriver!=null ? String.valueOf(Common.currentDriver.getRating()): "0.0");
        img_avatar.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(intent, IMAGE_PICK);
        });
        if(Common.currentDriver != null && Common.currentDriver.getAvatar()!=null
                && !TextUtils.isEmpty(Common.currentDriver.getAvatar())){
            Glide.with(this)
                    .load(Common.currentDriver.getAvatar())
                    .into(img_avatar);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.driver_home, menu);
        return true;
    }
    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }
}