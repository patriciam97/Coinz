package milou.patricia.coinz;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;


public class LoginScreen extends AppCompatActivity {

    private FirebaseAuth mAuth;
    EditText emailInput,passwordInput;
    String email,password;
    Button btnlogin,btnregister ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_screen);
        mAuth = FirebaseAuth.getInstance();
        btnlogin= (Button)findViewById(R.id.loginbtn);
        btnregister= (Button)findViewById(R.id.registerbtn);
        emailInput=findViewById(R.id.emailInput);
        passwordInput=findViewById(R.id.passwordInput);


    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        updateUI(currentUser);
    }
    private void updateUI(FirebaseUser currentUser) {
    }

    public void registerUser (View view){
        Intent i = new Intent(LoginScreen.this, RegisterActivity.class);
        startActivity(i);
    }
    public void loginUser (View view){
        email=emailInput.getText().toString();
        password=passwordInput.getText().toString();
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Toast.makeText(LoginScreen.this, "User Login Successful", Toast.LENGTH_SHORT).show();
                            FirebaseUser user = mAuth.getCurrentUser();
                            updateUI(user);
                            Intent i = new Intent(LoginScreen.this, MainActivity.class);
                            startActivity(i);

                        } else {
                            // If sign in fails, display a message to the user.
                             updateUI(null);
                        }
                    }

                    });
    }
}
