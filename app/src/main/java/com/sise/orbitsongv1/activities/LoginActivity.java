package com.sise.orbitsongv1.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.sise.orbitsongv1.R;
import com.sise.orbitsongv1.models.LoginRequest;
import com.sise.orbitsongv1.models.LoginResponse;
import com.sise.orbitsongv1.models.User;
import com.sise.orbitsongv1.services.RetrofitClient;
import com.sise.orbitsongv1.utils.Constants;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private TextInputEditText etUsername, etPassword;
    private Button btnLogin;
    private ProgressBar progressBar;
    private TextView tvRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Ocultar ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        initViews();
        setupListeners();
    }

    private void initViews() {
        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        progressBar = findViewById(R.id.progress_bar);
        tvRegister = findViewById(R.id.tv_register);
    }

    private void setupListeners() {
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptLogin();
            }
        });

        tvRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: Implementar registro
                Toast.makeText(LoginActivity.this, "Registro próximamente", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void attemptLogin() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Validaciones
        if (username.isEmpty()) {
            etUsername.setError("Ingrese su usuario/email");
            etUsername.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            etPassword.setError("Ingrese su contraseña");
            etPassword.requestFocus();
            return;
        }

        // Mostrar loading
        showLoading(true);

        // Crear objeto LoginRequest (en lugar de User)
        LoginRequest loginRequest = new LoginRequest(username, password);

        Log.d(TAG, "Intentando login con usuario: " + username);

        // Hacer petición a la API usando el nuevo endpoint
        RetrofitClient.getInstance().getApiService().login(loginRequest).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                showLoading(false);

                Log.d(TAG, "Respuesta recibida. Código: " + response.code());

                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse loginResponse = response.body();

                    Log.d(TAG, "Login response - Success: " + loginResponse.isSuccess() +
                            ", Message: " + loginResponse.getMessage());

                    if (loginResponse.isSuccess()) {
                        // Login exitoso
                        String token = loginResponse.getToken();
                        User user = loginResponse.getUser();

                        if (token != null && !token.isEmpty()) {
                            saveUserData(token, user);
                            showSuccessMessage("¡Bienvenido!");
                            goToHome();
                        } else {
                            showErrorMessage("Error: Token no recibido");
                        }
                    } else {
                        // Login fallido - mostrar mensaje del servidor
                        String errorMessage = loginResponse.getMessage();
                        if (errorMessage == null || errorMessage.isEmpty()) {
                            errorMessage = "Credenciales incorrectas";
                        }
                        showErrorMessage(errorMessage);
                    }
                } else {
                    // Error en la respuesta
                    Log.e(TAG, "Error en respuesta. Código: " + response.code());
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "Sin detalles";
                        Log.e(TAG, "Error body: " + errorBody);
                    } catch (Exception e) {
                        Log.e(TAG, "Error al leer error body", e);
                    }

                    if (response.code() == 401) {
                        showErrorMessage("Usuario o contraseña incorrectos");
                    } else if (response.code() == 400) {
                        showErrorMessage("Datos de entrada inválidos");
                    } else {
                        showErrorMessage("Error del servidor (Código: " + response.code() + ")");
                    }
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                showLoading(false);

                Log.e(TAG, "Error de conexión", t);

                String errorMessage = "Error de conexión";
                if (t.getMessage() != null) {
                    if (t.getMessage().contains("timeout")) {
                        errorMessage = "Tiempo de espera agotado. Verifica tu conexión.";
                    } else if (t.getMessage().contains("Unable to resolve host")) {
                        errorMessage = "No se puede conectar al servidor. Verifica la URL.";
                    } else if (t.getMessage().contains("Connection refused")) {
                        errorMessage = "Conexión rechazada. ¿Está el servidor ejecutándose?";
                    } else {
                        errorMessage = "Error de red: " + t.getMessage();
                    }
                }

                showErrorMessage(errorMessage);
            }
        });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!show);
        etUsername.setEnabled(!show);
        etPassword.setEnabled(!show);
    }

    private void showErrorMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void showSuccessMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void saveUserData(String token, User user) {
        try {
            SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();

            // Guardar token
            editor.putString(Constants.TOKEN_KEY, token);

            // Guardar datos del usuario si están disponibles
            if (user != null) {
                // Guardar email si existe
                if (user.getEmail() != null) {
                    editor.putString(Constants.USER_EMAIL_KEY, user.getEmail());
                    editor.putString(Constants.USER_KEY, user.getEmail()); // Para compatibilidad
                }

                // Guardar nombre si existe (usar getUsername() si no hay getNombre())
                try {
                    if (user.getUsername() != null) {
                        editor.putString(Constants.USER_NAME_KEY, user.getUsername());
                        // Si no hay email, usar username como user_key
                        if (user.getEmail() == null) {
                            editor.putString(Constants.USER_KEY, user.getUsername());
                        }
                    }
                } catch (Exception e) {
                    Log.d(TAG, "getUsername() no disponible en User model");
                }

                // Guardar ID si existe
                try {
                    if (user.getId() != null) {
                        editor.putLong(Constants.USER_ID_KEY, user.getId());
                    }
                } catch (Exception e) {
                    Log.d(TAG, "getId() no disponible en User model");
                }
            }

            editor.apply();
            Log.d(TAG, "Datos de usuario guardados correctamente");

        } catch (Exception e) {
            Log.e(TAG, "Error al guardar datos del usuario", e);
        }
    }

    private void goToHome() {
        try {
            Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            Log.e(TAG, "Error al navegar a HomeActivity", e);
            showErrorMessage("Error al acceder a la aplicación");
        }
    }
}