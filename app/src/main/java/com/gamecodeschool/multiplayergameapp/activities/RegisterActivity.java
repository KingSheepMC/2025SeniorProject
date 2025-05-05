package com.gamecodeschool.multiplayergameapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.gamecodeschool.multiplayergameapp.R;
import com.gamecodeschool.multiplayergameapp.models.ResponseBody;
import com.gamecodeschool.multiplayergameapp.network.ApiService;
import com.gamecodeschool.multiplayergameapp.network.RetrofitClient;
import com.gamecodeschool.multiplayergameapp.models.RegisterRequest;
import com.gamecodeschool.multiplayergameapp.utils.SharedPrefManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {
    private EditText usernameEditText, passwordEditText, passwordEditText2;
    private Button registerButton, backButton;
    private ApiService apiService;
    private SharedPrefManager prefManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        usernameEditText = findViewById(R.id.etNewUsername);
        passwordEditText = findViewById(R.id.etNewPassword);
        passwordEditText2 = findViewById(R.id.etConfirmPassword);
        registerButton = findViewById(R.id.btnRegister);
        backButton = findViewById(R.id.btnBackToLogin);

        apiService = RetrofitClient.getClient().create(ApiService.class);
        prefManager = SharedPrefManager.getInstance(getApplicationContext());

        registerButton.setOnClickListener(v -> {
            String username = usernameEditText.getText().toString();
            String password = passwordEditText.getText().toString();
            String password2 = passwordEditText2.getText().toString();

            if (!username.isEmpty() && !password.isEmpty()) {
                register(username, password, password2);
            } else {
                Toast.makeText(RegisterActivity.this, "Please enter username and password", Toast.LENGTH_SHORT).show();
            }
        });

        backButton.setOnClickListener(v -> finish());
    }

    private void register(String username, String password, String password2) {
        Call<ResponseBody> call = apiService.register(username, password, password2);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    prefManager.logout();
                    String token = response.body().getToken();
                    prefManager.saveAuthToken(token);
                    prefManager.saveUsername(username);

                    Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    String errorMessage = "Unknown error!";
                    if (response.code() == 400) {
                        errorMessage = "Username or Password are incorrect!";
                    } else if (response.code() == 401) {
                        errorMessage = "Password should be at least 8 char long!";
                    }
                    else if (response.code() == 402) {
                        errorMessage = "Username already taken!";
                    }
                    else if (response.code() == 403) {
                        errorMessage = "Passwords do not match!";
                    }
                    else if (response.code() == 500) {
                        errorMessage = "Node.js server isn't running!";
                    }
                    Toast.makeText(RegisterActivity.this, "Register failed: " + errorMessage, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(RegisterActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

}
