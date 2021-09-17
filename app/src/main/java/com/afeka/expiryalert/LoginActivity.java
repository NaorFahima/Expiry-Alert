package com.afeka.expiryalert;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
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
import com.google.firebase.auth.SignInMethodQueryResult;
import com.google.gson.Gson;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import org.jetbrains.annotations.NotNull;

public class LoginActivity extends AppCompatActivity {

    private SharedPreferences sp;
    private FirebaseAuth mAuth;
    private EditText passEditText, emailEditText;
    private Button loginButton;
    private TextView forgotPassword;
    private ProgressBar progressBar;
    private DatabaseManager db;
    private User loginUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        sp = getSharedPreferences("UserLogged", 0);
        db = new DatabaseManager();
        loginUser = new User();

        passEditText = findViewById(R.id.editTextPassword);
        emailEditText = findViewById(R.id.editTextTextEmailAddress);
        progressBar = findViewById(R.id.progressBar);
        loginButton = findViewById(R.id.loginButton);
        forgotPassword = findViewById(R.id.forgotPassword);

        progressBar.setVisibility(View.GONE);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailEditText.getText().toString().trim();
                String password = passEditText.getText().toString().trim();

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

                progressBar.setVisibility(View.VISIBLE);
                login(email, password);
            }
        });

        forgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LayoutInflater factory = LayoutInflater.from(LoginActivity.this);
                final View forgotPasswordDialogView = factory.inflate(R.layout.forgot_password_dialog, null);
                final AlertDialog forgotPasswordDialog = new AlertDialog.Builder(LoginActivity.this).create();
                forgotPasswordDialog.setView(forgotPasswordDialogView);
                forgotPasswordDialog.setCanceledOnTouchOutside(false);
                forgotPasswordDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

                EditText emailEditText = forgotPasswordDialogView.findViewById(R.id.forgotPasswordEmail);

                forgotPasswordDialogView.findViewById(R.id.forgot_password_dialog_btn_send).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String email = emailEditText.getText().toString();
                        String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";

                        if(!email.isEmpty() && email.matches(emailPattern)) {
                            mAuth.fetchSignInMethodsForEmail(email)
                                    .addOnCompleteListener(new OnCompleteListener<SignInMethodQueryResult>() {
                                        @Override
                                        public void onComplete(@NonNull @NotNull Task<SignInMethodQueryResult> task) {
                                            boolean isEmailDontExist = task.getResult().getSignInMethods().isEmpty();
                                            if(isEmailDontExist) {
                                                emailEditText.setError("No Such Email in the database");
                                                emailEditText.requestFocus();
                                                return;
                                            } else {
                                                mAuth.sendPasswordResetEmail(email)
                                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                            @Override
                                                            public void onComplete(@NonNull @NotNull Task<Void> task) {
                                                                if(task.isSuccessful()) {
                                                                    Toast.makeText(LoginActivity.this, "Password Reset Email Sent!",
                                                                            Toast.LENGTH_LONG).show();
                                                                    forgotPasswordDialog.dismiss();
                                                                } else {
                                                                    Toast.makeText(LoginActivity.this, "There was a problem! Please try again",
                                                                            Toast.LENGTH_LONG).show();
                                                                    forgotPasswordDialog.dismiss();
                                                                }
                                                            }
                                                        });
                                            }
                                        }
                                    });
                        } else {
                            emailEditText.setError("Please provide a valid email");
                            emailEditText.requestFocus();
                            return;
                        }
                    }
                });

                forgotPasswordDialogView.findViewById(R.id.forgot_password_dialog_btn_cancel).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        forgotPasswordDialog.dismiss();
                    }
                });
                forgotPasswordDialog.show();
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

    public void login(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull @NotNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d("USER-CREATE", "createUserWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            loadUser(user);
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w("USER-CREATE", "createUserWithEmail:failure", task.getException());
                            Toast.makeText(LoginActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                            progressBar.setVisibility(View.GONE);
                            updateUI(false);
                        }
                    }
                });
    }

    public void loadUser(FirebaseUser user) {
            db.loadUser(user.getUid(), new DatabaseManager.FirestoreCallback() {
                @Override
                public void onStorageCallBack(Storage storage) { }
                @Override
                public void onUserCallBack(User user) {
                    loginUser.setId(user.getId());
                    loginUser.setEmail(user.getEmail());
                    loginUser.setUsername(user.getUsername());
                    loginUser.setStorageID(user.getStorageID());
                    loginUser.setSharingList(user.getSharingList());
                    updateUI(true);
                }
        });
    }

    public void updateUI(boolean isLogin) {
        if(!isLogin) {
            startActivity(new Intent(this, LoginActivity.class));
        } else {
            Toast.makeText(LoginActivity.this, loginUser.getEmail() + " Login Successfully!",
                    Toast.LENGTH_SHORT).show();
            Gson gson = new Gson();
            String userToSave = gson.toJson(loginUser);
            sp.edit().putString(mAuth.getCurrentUser().getUid(), userToSave).commit();
            progressBar.setVisibility(View.GONE);
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    }
}