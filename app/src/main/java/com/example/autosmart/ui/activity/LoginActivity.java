package com.example.autosmart.ui.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.autosmart.R;
import com.example.autosmart.data.db.AppDatabase;
import com.example.autosmart.data.db.UserEntity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.android.gms.common.SignInButton;
import com.google.android.material.snackbar.Snackbar;
import androidx.core.content.ContextCompat;
import android.content.SharedPreferences;


public class LoginActivity extends AppCompatActivity {


    private EditText etEmail, etPassword;
    private Button btnLogin, btnRegister;
    private FirebaseAuth mAuth;
    private SignInButton btnGoogle;
    private TextView tvForgotPassword;

    // Cliente de Google Sign-In y código de solicitud
    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 9001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Inicializar Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Referencias a las vistas
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
        btnGoogle = findViewById(R.id.btnGoogle);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        // Configurar Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                // Asegúrate de que 'default_web_client_id' esté definido en strings.xml
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Configurar el botón de Google
        btnGoogle.setSize(SignInButton.SIZE_WIDE);
        btnGoogle.setColorScheme(SignInButton.COLOR_LIGHT);

        // Listener para Login tradicional
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = etEmail.getText().toString().trim();
                String password = etPassword.getText().toString().trim();
                if (!email.isEmpty() && !password.isEmpty()) {
                    signInUser(email, password);
                } else {
                    showErrorSnackbar("Por favor, llena todos los campos");
                }
            }
        });

        // Listener para Registro
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = etEmail.getText().toString().trim();
                String password = etPassword.getText().toString().trim();
                if (!email.isEmpty() && !password.isEmpty()) {
                    registerUser(email, password);
                } else {
                    showErrorSnackbar("Por favor, llena todos los campos");
                }
            }
        });

        // Listener para Google Sign-In
        btnGoogle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signInWithGoogle();
            }
        });

        tvForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showForgotPasswordDialog();
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        // Si el usuario ya está autenticado, se salta el login
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            updateUI(currentUser);
        }
    }

    // Método para inicio de sesión tradicional con email y contraseña
    private void signInUser(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null && user.isEmailVerified()) {
                                updateUI(user);
                                SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
                                SharedPreferences.Editor editor = prefs.edit();
                                editor.putString("login_method", "email");
                                editor.apply();
                            } else if (user != null) {
                                showResendVerificationSnackbar(user);
                                mAuth.signOut();
                            } else {
                                showErrorSnackbar("Error inesperado. Intenta de nuevo.");
                            }
                        } else {
                            String errorMsg = "Ocurrió un error al iniciar sesión. Intenta de nuevo.";
                            Exception e = task.getException();
                            if (e != null) {
                                String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
                                if (msg.contains("password is invalid") || msg.contains("invalid password")) {
                                    errorMsg = "La contraseña es incorrecta. Intenta de nuevo.";
                                } else if (msg.contains("no user record") || msg.contains("user does not exist") || msg.contains("there is no user record") || msg.contains("has expired")) {
                                    // Comprobar si el correo está registrado con Google
                                    mAuth.fetchSignInMethodsForEmail(email)
                                        .addOnCompleteListener(task2 -> {
                                            if (task2.isSuccessful()) {
                                                boolean hasGoogle = false;
                                                for (String method : task2.getResult().getSignInMethods()) {
                                                    if (method.equals("google.com")) {
                                                        hasGoogle = true;
                                                        break;
                                                    }
                                                }
                                                if (hasGoogle) {
                                                    showErrorSnackbar("Este correo está registrado con Google. Por favor, inicia sesión con el botón de Google.");
                                                } else {
                                                    showErrorSnackbar("El correo no está registrado. Por favor, regístrate.");
                                                }
                                            } else {
                                                showErrorSnackbar("El correo no está registrado. Por favor, regístrate.");
                                            }
                                        });
                                    return;
                                } else if (msg.contains("badly formatted")) {
                                    errorMsg = "El correo no es válido. Revisa el formato.";
                                } else if (msg.contains("email address is already in use")) {
                                    errorMsg = "Ese correo ya fue usado para registrarse con email y contraseña. Por favor, inicia sesión o recupera tu contraseña.";
                                }
                            }
                            showErrorSnackbar(errorMsg);
                        }
                    }
                });
    }

    // Método para registrar un nuevo usuario
    private void registerUser(String email, String password) {
        if (!isPasswordSecure(password)) {
            showErrorSnackbar("La contraseña debe tener al menos 8 caracteres, una mayúscula, una minúscula, un número y un carácter especial.");
            return;
        }
        mAuth.fetchSignInMethodsForEmail(email)
                .addOnCompleteListener(this, new OnCompleteListener<com.google.firebase.auth.SignInMethodQueryResult>() {
                    @Override
                    public void onComplete(@NonNull Task<com.google.firebase.auth.SignInMethodQueryResult> task) {
                        if (task.isSuccessful()) {
                            boolean emailExists = task.getResult().getSignInMethods() != null && !task.getResult().getSignInMethods().isEmpty();
                            if (emailExists) {
                                showErrorSnackbar("El correo ya está registrado. Por favor, inicia sesión o usa otro correo.");
                            } else {
                                // Si no existe, registrar normalmente
                                mAuth.createUserWithEmailAndPassword(email, password)
                                        .addOnCompleteListener(LoginActivity.this, new OnCompleteListener<AuthResult>() {
                                            @Override
                                            public void onComplete(@NonNull Task<AuthResult> task) {
                                                if (task.isSuccessful()) {
                                                    FirebaseUser user = mAuth.getCurrentUser();
                                                    if (user != null) {
                                                        user.sendEmailVerification()
                                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                    @Override
                                                                    public void onComplete(@NonNull Task<Void> task) {
                                                                        if (task.isSuccessful()) {
                                                                            showSuccessSnackbar("Registro exitoso. Revisa tu correo y verifica tu cuenta antes de iniciar sesión.");
                                                                        } else {
                                                                            String msg = task.getException() != null && task.getException().getMessage() != null ? task.getException().getMessage() : "";
                                                                            if (msg.toLowerCase().contains("email address is already in use")) {
                                                                                msg = "El correo electrónico ya está en uso por otra cuenta.";
                                                                            } else if (msg.toLowerCase().contains("password")) {
                                                                                msg = "La contraseña debe tener al menos 8 caracteres, una mayúscula, una minúscula, un número y un carácter especial.";
                                                                            } else {
                                                                                msg = "Error en registro: " + msg;
                                                                            }
                                                                            showErrorSnackbar(msg);
                                                                        }
                                                                    }
                                                                });
                                                        mAuth.signOut(); // Cerrar sesión hasta que verifique
                                                    }
                                                }
                                            }
                                        });
                            }
                        } else {
                            showErrorSnackbar("Error al comprobar el correo: " + (task.getException() != null ? task.getException().getMessage() : ""));
                        }
                    }
                });
    }

    // Validación de contraseña segura
    private boolean isPasswordSecure(String password) {
        if (password == null) return false;
        // Al menos 8 caracteres, una mayúscula, una minúscula, un número y un carácter especial
        return password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&._#-])[A-Za-z\\d@$!%*?&._#-]{8,}$");
    }

    // Iniciar el flujo de Google Sign-In
    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    // Procesar el resultado de Google Sign-In
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task =
                    GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // El inicio de sesión con Google fue exitoso, autentica con Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Toast.makeText(this, "Fallo al iniciar sesión con Google: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Autenticación en Firebase usando el token de Google
    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            updateUI(user);
                            SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putString("login_method", "google");
                            editor.apply();
                        } else {
                            Toast.makeText(LoginActivity.this,
                                    "Fallo en la autenticación con Firebase: " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void updateUI(FirebaseUser user) {
        if (user != null) {
            if (!user.isEmailVerified()) {
                showResendVerificationSnackbar(user);
                mAuth.signOut();
                return;
            }
            SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
            String loginMethod = prefs.getString("login_method", "email");
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            // Cifrar explícitamente nombre y email antes de guardar
            String displayName = user.getDisplayName();
            if (displayName == null || displayName.isEmpty()) {
                displayName = user.getEmail();
            }
            String encryptedName, encryptedEmail;
            try {
                encryptedName = com.example.autosmart.utils.EncryptionUtils.encrypt(displayName);
            } catch (Exception e) {
                encryptedName = displayName;
            }
            try {
                encryptedEmail = com.example.autosmart.utils.EncryptionUtils.encrypt(user.getEmail());
            } catch (Exception e) {
                encryptedEmail = user.getEmail();
            }
            UserEntity userEntity = new UserEntity();
            userEntity.setFirebaseUid(user.getUid());
            userEntity.setName(encryptedName);
            userEntity.setEmail(encryptedEmail);
            userEntity.setNameEncrypted(true);
            userEntity.setEmailEncrypted(true);
            db.userDao().insertUser(userEntity);
            // Guardar en SharedPreferences
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("username", displayName);
            editor.apply();
            // Redirigir al Dashboard
            Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
            startActivity(intent);
            finish();
        }
    }

    private void showForgotPasswordDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_forgot_password, null);
        final com.google.android.material.textfield.TextInputEditText etDialogEmail = dialogView.findViewById(R.id.etDialogEmail);

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Recuperar contraseña")
            .setView(dialogView)
            .setPositiveButton("ENVIAR", (dialog, which) -> {
                String email = etDialogEmail.getText().toString().trim();
                if (!email.isEmpty()) {
                    mAuth.sendPasswordResetEmail(email)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                showSuccessSnackbar("Te hemos enviado un correo para recuperar tu contraseña.");
                            } else {
                                showErrorSnackbar("Error: " + (task.getException() != null ? task.getException().getMessage() : ""));
                            }
                        });
                } else {
                    showErrorSnackbar("Introduce un email válido");
                }
            })
            .setNegativeButton("CANCELAR", null)
            .show();
    }

    private void showErrorSnackbar(String message) {
        Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG);
        snackbar.setBackgroundTint(ContextCompat.getColor(this, R.color.red_500));
        snackbar.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        View snackbarView = snackbar.getView();
        TextView textView = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
        if (textView != null) {
            textView.setMaxLines(5); // Permitir hasta 5 líneas
        }
        snackbar.show();
    }

    private void showSuccessSnackbar(String message) {
        Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG);
        snackbar.setBackgroundTint(ContextCompat.getColor(this, R.color.blue_500));
        snackbar.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        View snackbarView = snackbar.getView();
        TextView textView = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
        if (textView != null) {
            textView.setMaxLines(5); // Permitir hasta 5 líneas
        }
        snackbar.show();
    }

    // Mostrar Snackbar con opción de reenviar correo de verificación
    private void showResendVerificationSnackbar(FirebaseUser user) {
        Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content),
                "Debes verificar tu correo antes de iniciar sesión.", Snackbar.LENGTH_INDEFINITE)
                .setAction("Reenviar", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        resendVerificationEmail(user);
                    }
                });
        snackbar.setBackgroundTint(ContextCompat.getColor(this, R.color.red_500));
        snackbar.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        snackbar.setActionTextColor(ContextCompat.getColor(this, R.color.blue_500));
        snackbar.show();
    }

    // Método para reenviar el correo de verificación
    private void resendVerificationEmail(FirebaseUser user) {
        user.sendEmailVerification()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            showSuccessSnackbar("Correo de verificación reenviado. Revisa tu bandeja de entrada.");
                        } else {
                            showErrorSnackbar("No se pudo reenviar el correo. Intenta de nuevo más tarde.");
                        }
                    }
                });
    }
}
