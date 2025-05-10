package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class SignupActivity extends AppCompatActivity {

    EditText signupName, signupEmail, signupUsername, signupPassword;
    TextView loginRedirectText;
    Button signupButton;
    FirebaseDatabase database;
    DatabaseReference reference;

    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        signupName = findViewById(R.id.signup_name);
        signupEmail = findViewById(R.id.signup_email);
        signupUsername = findViewById(R.id.signup_username);
        signupPassword = findViewById(R.id.signup_password);
        signupButton = findViewById(R.id.signup_button);
        loginRedirectText = findViewById(R.id.loginRedirectText);

        // Inițializează DatabaseHelper
        dbHelper = DatabaseHelper.getInstance(this);

        signupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (validateFields()) {
                    registerUser();
                }
            }
        });

        loginRedirectText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });
    }

    private boolean validateFields() {
        String name = signupName.getText().toString().trim();
        String email = signupEmail.getText().toString().trim();
        String username = signupUsername.getText().toString().trim();
        String password = signupPassword.getText().toString().trim();

        if (name.isEmpty()) {
            signupName.setError("Name is required");
            return false;
        }

        if (email.isEmpty()) {
            signupEmail.setError("Email is required");
            return false;
        }

        if (username.isEmpty()) {
            signupUsername.setError("Username is required");
            return false;
        }

        if (password.isEmpty()) {
            signupPassword.setError("Password is required");
            return false;
        }

        if (password.length() < 6) {
            signupPassword.setError("Password must be at least 6 characters");
            return false;
        }

        return true;
    }

    private void registerUser() {
        database = FirebaseDatabase.getInstance();
        reference = database.getReference("users");

        String name = signupName.getText().toString().trim();
        String email = signupEmail.getText().toString().trim();
        String username = signupUsername.getText().toString().trim();
        String password = signupPassword.getText().toString().trim();

        /*// Verifică dacă utilizatorul există deja
        if (dbHelper.checkUserExists(username)) {
            signupUsername.setError("Username already exists");
            return;
        }
*/
        HelperClass helperClass = new HelperClass(name, email, username, password);

        // Salvează în Firebase
        reference.child(username).setValue(helperClass)
                .addOnSuccessListener(aVoid -> {
                    // Salvează în SQLite pentru cache
                    dbHelper.insertOrUpdateUser(helperClass, true); // true = from Firebase

                    Toast.makeText(SignupActivity.this, "You have signed up successfully!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    // Dacă Firebase eșuează, salvează doar în SQLite
                    dbHelper.insertOrUpdateUser(helperClass, false); // false = local

                    Toast.makeText(SignupActivity.this, "Signed up offline. Will sync when online.", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                });
    }
}