package com.afeka.expiryalert;

import androidx.appcompat.app.AppCompatActivity;
import android.app.ActivityManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;
import com.afeka.expiryalert.logic.DatabaseManager;
import com.afeka.expiryalert.logic.Storage;
import com.afeka.expiryalert.logic.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class StartingActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DatabaseManager db;
    private User currentUser;
    private ProgressBar progressBar;
    private Button goToLoginButton, goToSignUpButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.starting_activity);

        mAuth = FirebaseAuth.getInstance();
        db = new DatabaseManager();
        currentUser = new User();
        
        progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);
        goToLoginButton = findViewById(R.id.loginActivityButton);
        goToSignUpButton = findViewById(R.id.signUpActivityButton);

        if(mAuth.getCurrentUser() != null) {
            progressBar.setVisibility(View.VISIBLE);
            loadUser(mAuth.getCurrentUser());
            goToLoginButton.setEnabled(false);
            goToSignUpButton.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            setTaskDescription(
                    new ActivityManager.TaskDescription(null, R.mipmap.ic_expiryalert_round, 0));
        }


        goToSignUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(StartingActivity.this, RegisterActivity.class));
            }
        });

        goToLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(StartingActivity.this, LoginActivity.class));
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(mAuth.getCurrentUser() != null) {
            progressBar.setVisibility(View.VISIBLE);
            loadUser(mAuth.getCurrentUser());
        }
    }

    public void loadUser(FirebaseUser user) {
        db.loadUser(user.getUid(), new DatabaseManager.FirestoreCallback() {
            @Override
            public void onStorageCallBack(Storage storage) { }
            @Override
            public void onUserCallBack(User user) {
                if(user != null) {
                    currentUser.setId(user.getId());
                    currentUser.setEmail(user.getEmail());
                    currentUser.setUsername(user.getUsername());
                    currentUser.setStorageID(user.getStorageID());
                    currentUser.setSharingList(user.getSharingList());
                    updateUI(true);
                } else {
                    updateUI(false);
                }
            }
        });
    }

    public void updateUI(boolean isLogin) {
        if(!isLogin) {
            Toast.makeText(StartingActivity.this, "There was a problem logging in, please try again!",
                    Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
            goToLoginButton.setEnabled(true);
            goToSignUpButton.setEnabled(true);
            startActivity(new Intent(StartingActivity.this, StartingActivity.class));
        } else {
            Toast.makeText(StartingActivity.this, currentUser.getEmail() + " Login Successfully!",
                    Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
            Intent intent = new Intent(StartingActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    }
}