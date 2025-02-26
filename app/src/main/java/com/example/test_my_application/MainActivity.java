package com.example.test_my_application;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        EditText num1 = findViewById(R.id.number1);
        EditText num2 = findViewById(R.id.number2);
        Button myButton = findViewById(R.id.button);
        TextView myTextView = findViewById(R.id.textView);
        TextView sumTextView = findViewById(R.id.result);

        // funcție care sterge textul la focus si readauga hint-ul daca e gol
        clearOnFocus(num1, getString(R.string.number1));
        clearOnFocus(num2, getString(R.string.number2));

        myButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myTextView.setText(getString(R.string.success_message));
            }
        });

        // TextWatcher pentru calcul automat al sumei
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                calculateSum(num1, num2, sumTextView);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        num1.addTextChangedListener(textWatcher);
        num2.addTextChangedListener(textWatcher);
    }

    // șterge textul la focus si a readauga hint-ul daca e gol
    private void clearOnFocus(EditText editText, String hintText) {
        editText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    editText.setText("");
                } else {
                    if (editText.getText().toString().isEmpty()) {
                        editText.setHint(hintText);
                    }
                }
            }
        });
    }

    // calculare suma
    private void calculateSum(EditText num1, EditText num2, TextView sumTextView) {
        String num1Text = num1.getText().toString();
        String num2Text = num2.getText().toString();

        if (!num1Text.isEmpty() && !num2Text.isEmpty()) {
            try {
                double number1 = Double.parseDouble(num1Text);
                double number2 = Double.parseDouble(num2Text);
                double sum = number1 + number2;

                sumTextView.setText(String.format(getString(R.string.result), sum));
            } catch (NumberFormatException e) {
                sumTextView.setText(getString(R.string.error_message));
            }
        } else {
            sumTextView.setText(getString(R.string.error_message));
        }
    }
}
