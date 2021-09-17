package com.afeka.expiryalert;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.afeka.expiryalert.logic.DatabaseManager;
import com.afeka.expiryalert.logic.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.Gson;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;


public class MainActivity extends AppCompatActivity{

    private DatabaseManager db;
    private SharedPreferences sp;
    private TextView welcomeHeadline;
    private ImageView settingsButton;
    private FirebaseAuth mAuth;
    FragmentManager fragmentManager;
    Fragment mainFragment, settingsFragment;
    User user;

    public void closeApp (View view) {
        finish() ;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(mAuth.getCurrentUser() != null) {
            setCurrentUserInstance();
            welcomeHeadline = (TextView) findViewById(R.id.welcome_headline);
            welcomeHeadline.setText(user.getUsername());
            mainFragment = MainFragment.newInstance(user, null, 0);
            settingsFragment = SettingsFragment.newInstance(user);
            fragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, mainFragment)
                    .commit();
        } else {
            startActivity(new Intent(this, LoginActivity.class));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createNotificationChannel();
        setContentView(R.layout.activity_main);

        db = new DatabaseManager();
        mAuth = FirebaseAuth.getInstance();

        sp = getSharedPreferences("UserLogged", 0);
        setCurrentUserInstance();

        settingsButton = findViewById(R.id.settings_icon);

        fragmentManager = getSupportFragmentManager();
        mainFragment = MainFragment.newInstance(user, null, 0);
        settingsFragment = SettingsFragment.newInstance(user);

        if(savedInstanceState == null) {
            fragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, mainFragment, "MainFragment")
                    .commit();
        }

        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SettingsFragment sFragment = (SettingsFragment)fragmentManager.findFragmentByTag("SettingsFragment");
                if(sFragment != null) {
                    if (!sFragment.isVisible()) {
                        fragmentManager.beginTransaction()
                                .replace(R.id.fragment_container, settingsFragment, "SettingsFragment")
                                .addToBackStack(null)
                                .commit();
                    }
                } else {
                    fragmentManager.beginTransaction()
                            .replace(R.id.fragment_container, settingsFragment, "SettingsFragment")
                            .addToBackStack(null)
                            .commit();
                }
            }
        });
    }

    public void setCurrentUserInstance() {
        Gson gson = new Gson();
        String userFromPref = sp.getString(mAuth.getCurrentUser().getUid(), "");
        user = gson.fromJson(userFromPref, User.class);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Expiry channel";
            String description = "Expiry Alert";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("notify", name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

}