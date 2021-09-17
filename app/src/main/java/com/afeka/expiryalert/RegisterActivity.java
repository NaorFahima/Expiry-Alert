package com.afeka.expiryalert;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.afeka.expiryalert.logic.DatabaseManager;
import com.afeka.expiryalert.logic.Storage;
import com.afeka.expiryalert.logic.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.Gson;
import org.jetbrains.annotations.NotNull;
import java.util.UUID;

public class RegisterActivity extends AppCompatActivity {

    private SharedPreferences sp;
    private FirebaseAuth mAuth;
    private EditText passEditText, emailEditText, nameEditText;
    private Button signupButton;
    private ProgressBar progressBar;
    private DatabaseManager db;
    private User signUpUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        sp = getSharedPreferences("UserLogged", 0);
        mAuth = FirebaseAuth.getInstance();
        db = new DatabaseManager();
        signUpUser = new User();

        passEditText = findViewById(R.id.editTextPassword);
        emailEditText = findViewById(R.id.editTextTextEmailAddress);
        nameEditText = findViewById(R.id.editTextName);
        progressBar = findViewById(R.id.progressBar);
        signupButton = findViewById(R.id.signupButton);

        progressBar.setVisibility(View.GONE);

        signupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = nameEditText.getText().toString().trim();
                String email = emailEditText.getText().toString().trim();
                String password = passEditText.getText().toString().trim();

                if(name.isEmpty() || name.length() < 2) {
                    nameEditText.setError("Name must contain at least 2 characters");
                    nameEditText.requestFocus();
                    return;
                }

                String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";

                if (!email.matches(emailPattern) || email.isEmpty())
                {
                    emailEditText.setError("Please fill a valid email address");
                    emailEditText.requestFocus();
                    return;
                }

                if(password.isEmpty()) {
                    passEditText.setError("Please fill a password");
                    passEditText.requestFocus();
                    return;
                }
                if(password.length() < 6) {
                    passEditText.setError("Password must contain at least 6 characters");
                    passEditText.requestFocus();
                    return;
                }
                progressBar.setVisibility(View.VISIBLE);
                createAccount(name, email, password);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(mAuth.getCurrentUser() != null) {
            loadUser(mAuth.getCurrentUser());
        }
    }

    public void createAccount(String name, String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull @NotNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d("USER-CREATE", "createUserWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            String storageUniqueID = UUID.randomUUID().toString();
                            Storage storage = new Storage(storageUniqueID);
                            db.saveStorage(storage);
                            User userToAdd = new User(user.getUid(), name, user.getEmail(), storageUniqueID);
                            db.saveUser(userToAdd);
                            loadUser(user);
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w("USER-CREATE", "createUserWithEmail:failure", task.getException());
                            Toast.makeText(RegisterActivity.this, task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                            updateUI(null);
                        }
                    }
                });
    }

    public void loadUser(FirebaseUser user) {
        if(user != null) {
            db.loadUser(user.getUid(), new DatabaseManager.FirestoreCallback() {
                @Override
                public void onStorageCallBack(Storage storage) { }

                @Override
                public void onUserCallBack(User user) {
                    signUpUser.setId(user.getId());
                    signUpUser.setEmail(user.getEmail());
                    signUpUser.setUsername(user.getUsername());
                    signUpUser.setStorageID(user.getStorageID());
                    signUpUser.setSharingList(user.getSharingList());
                    updateUI(signUpUser);
                }
            });
        } else {
            updateUI(null);
        }
    }

    public void updateUI(User user) {
        if(user != null) {
            Toast.makeText(RegisterActivity.this, user.getEmail() + " Created Successfully!",
                    Toast.LENGTH_SHORT).show();
            Gson gson = new Gson();
            String userToSave = gson.toJson(signUpUser);
            sp.edit().putString(mAuth.getCurrentUser().getUid(), userToSave).commit();
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        } else {
            progressBar.setVisibility(View.GONE);
            startActivity(new Intent(this, RegisterActivity.class));
        }
    }
}