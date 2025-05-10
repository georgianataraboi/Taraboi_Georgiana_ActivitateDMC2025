package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends AppCompatActivity {

    EditText loginUsername, loginPassword;
    Button loginButton;
    TextView signupRedirectText;

    private DatabaseHelper dbHelper;
    private boolean isOnline = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        loginUsername = findViewById(R.id.login_username);
        loginPassword = findViewById(R.id.login_password);
        signupRedirectText = findViewById(R.id.signupRedirectText);
        loginButton = findViewById(R.id.login_button);

        // Inițializează DatabaseHelper
        dbHelper = DatabaseHelper.getInstance(this);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!validateUsername() | !validatePassword()){
                    // Validation failed
                } else {
                    checkUser();
                }
            }
        });

        signupRedirectText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
                startActivity(intent);
            }
        });
    }

    public Boolean validateUsername(){
        String val = loginUsername.getText().toString();
        if (val.isEmpty()){
            loginUsername.setError("Username cannot be empty");
            return false;
        } else {
            loginUsername.setError(null);
            return true;
        }
    }

    public Boolean validatePassword(){
        String val = loginPassword.getText().toString();
        if (val.isEmpty()){
            loginPassword.setError("Password cannot be empty");
            return false;
        } else {
            loginPassword.setError(null);
            return true;
        }
    }

    public void checkUser(){
        String userUsername = loginUsername.getText().toString().trim();
        String userPassword = loginPassword.getText().toString().trim();

        // Încearcă autentificarea prin Firebase mai întâi
        checkUserInFirebase(userUsername, userPassword);
    }

    private void checkUserInFirebase(String userUsername, String userPassword) {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("users");
        Query checkUserDatabase = reference.orderByChild("username").equalTo(userUsername);

        checkUserDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()){
                    loginUsername.setError(null);
                    String passwordFromDB = snapshot.child(userUsername).child("password").getValue(String.class);

                    if (passwordFromDB != null && passwordFromDB.equals(userPassword)){
                        loginUsername.setError(null);

                        // Obține datele utilizatorului
                        String nameFromDB = snapshot.child(userUsername).child("name").getValue(String.class);
                        String emailFromDB = snapshot.child(userUsername).child("email").getValue(String.class);
                        String usernameFromDB = snapshot.child(userUsername).child("username").getValue(String.class);

                        // Salvează utilizatorul în SQLite pentru accesul offline
                        HelperClass user = new HelperClass(nameFromDB, emailFromDB, usernameFromDB, passwordFromDB);
                        dbHelper.insertOrUpdateUser(user, true); // true = from Firebase

                        // Continuă la MainActivity
                        proceedToMainActivity(nameFromDB, emailFromDB, usernameFromDB, passwordFromDB);
                    } else {
                        loginPassword.setError("Invalid Credentials");
                        loginPassword.requestFocus();
                    }
                } else {
                    // User not found in Firebase, check SQLite (offline mode)
                    checkUserInSQLite(userUsername, userPassword);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Error connecting to Firebase, try SQLite
                Toast.makeText(LoginActivity.this, "Network error, trying offline mode...", Toast.LENGTH_SHORT).show();
                checkUserInSQLite(userUsername, userPassword);
            }
        });
    }

    private void checkUserInSQLite(String userUsername, String userPassword) {
        // Verifică în SQLite pentru modul offline
        if (dbHelper.validateUser(userUsername, userPassword)) {
            HelperClass user = dbHelper.getUserByUsername(userUsername);

            if (user != null) {
                Toast.makeText(this, "Logged in offline mode", Toast.LENGTH_SHORT).show();
                proceedToMainActivity(user.getName(), user.getEmail(), user.getUsername(), user.getPassword());
            } else {
                loginUsername.setError("User not found");
                loginUsername.requestFocus();
            }
        } else {
            loginPassword.setError("Invalid Credentials");
            loginPassword.requestFocus();
        }
    }

    private void proceedToMainActivity(String name, String email, String username, String password) {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);

        intent.putExtra("name", name);
        intent.putExtra("email", email);
        intent.putExtra("username", username);
        intent.putExtra("password", password);

        startActivity(intent);
        finish(); // Închide LoginActivity
    }
}