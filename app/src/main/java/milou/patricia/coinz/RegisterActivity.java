package milou.patricia.coinz;

import android.content.Intent;
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

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.concurrent.ExecutionException;


public class RegisterActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    EditText emailET,passwordET ;
    String email,password;
    Button btnregister;
    private FirebaseFirestore firestore;
    private DocumentReference firestoreUsers;
    private FirebaseUser user;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        emailET =findViewById(R.id.emailInput);
        passwordET = findViewById(R.id.passwordInput);
        mAuth = FirebaseAuth.getInstance();
        btnregister= (Button)findViewById(R.id.registerbtn);
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

    private void registerUser() throws ExecutionException, InterruptedException {
        ValidateFields();
        // if(checkIfUserExists()==false) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            user=mAuth.getCurrentUser();
                            // Sign in success, update UI with the signed-in user's information
                            Toast.makeText(RegisterActivity.this, "User Registered Succesful", Toast.LENGTH_SHORT).show();
                            Intent i = new Intent(RegisterActivity.this, EditProfile.class);
                           // i.putExtra("user",user); //pass the user to the Profile activity
                            startActivity(i);
                            finish();
                        }else{
                            Toast.makeText(RegisterActivity.this, "An error has occured please try again.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
//        }else{
//            Toast.makeText(RegisterActivity.this, "User is already registered", Toast.LENGTH_SHORT).show();
//        }
    }
    public void clickRegister(View view) throws ExecutionException, InterruptedException {

        email=emailET.getText().toString();
        password=passwordET.getText().toString();
        registerUser();
    }
}

