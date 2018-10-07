package milou.patricia.coinz;

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class RegisterActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    EditText emailET,passwordET ;
    String email,password;
    Button btnregister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        emailET =findViewById(R.id.emailInput);
        passwordET = findViewById(R.id.passwordInput);
        mAuth = FirebaseAuth.getInstance();
        btnregister= (Button)findViewById(R.id.registerbtn);

    }
    private void registerUser(){
        ValidateFields();
        Toast.makeText(RegisterActivity.this, "Fields ok", Toast.LENGTH_SHORT).show();
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            // Sign in success, update UI with the signed-in user's information
                            Toast.makeText(RegisterActivity.this, "User Registered Succesful", Toast.LENGTH_SHORT).show();

                        }
                    }
                });
    }
    private void ValidateFields(){
        Toast.makeText(RegisterActivity.this, "Validating", Toast.LENGTH_SHORT).show();

        if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            emailET.setError("Username is required");
            emailET.requestFocus();
        }
        if(password.isEmpty()){
            passwordET.setError("Username is required");
            passwordET.requestFocus();
        }
        if (password.length()<6){
            passwordET.setError("Minimum length of password is 6");
            passwordET.requestFocus();
        }
    }

     public void clickRegister(View view){

         email=emailET.getText().toString();
         password=passwordET.getText().toString();
         registerUser();
     }
}
