package milou.patricia.coinz;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import timber.log.Timber;


public class RegisterActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private EditText emailET,passwordET ;
    private String email,password;
    private Boolean exists=false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        emailET =findViewById(R.id.emailInput);
        passwordET = findViewById(R.id.passwordInput);
        mAuth = FirebaseAuth.getInstance();
    }

    /**
     * Validate that the inputs are in the right format
     */
    private boolean ValidateFields(){
        boolean wait=false;
        Toast.makeText(RegisterActivity.this, "Validating", Toast.LENGTH_SHORT).show();

        if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            emailET.setError("Username is required");
            emailET.requestFocus();
            wait=true;
        }
        if(password.isEmpty()){
            passwordET.setError("Username is required");
            passwordET.requestFocus();
            wait=true;
        }
        //Password has to be more than 6 characters
        if (password.length()<6){
            passwordET.setError("Minimum length of password is 6");
            passwordET.requestFocus();
            wait=true;
        }
        return wait;
    }

    /**
     * Register the user, by creating a user on Firebase.
     */
    private void registerUser() {
        boolean res=ValidateFields();
        if(!checkIfUserExists() && !res) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        mAuth.getCurrentUser();
                        // Sign in success, update UI with the signed-in user's information
                        Toast.makeText(RegisterActivity.this, "User Registered Succesful", Toast.LENGTH_SHORT).show();
                        Intent i = new Intent(RegisterActivity.this, EditProfile.class);
                       // i.putExtra("user",user); //pass the user to the Profile activity
                        startActivity(i);
                        finish();
                        Timber.i("Successful Registration");
                    }else{
                        Toast.makeText(RegisterActivity.this, "An error has occured please try again.", Toast.LENGTH_SHORT).show();
                    }
                });
        }else{
            Toast.makeText(RegisterActivity.this, "User is already registered", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean checkIfUserExists() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Users")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        //find all coins that have have Depodited date as today
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            if (document.getId().equals(email)) {
                                exists = true;
                            }
                        }
                    }
                });
                    return exists;
    }

    /**
     * Register button action
     * @param view Current View
     */
    public void clickRegister(View view) {

        email=emailET.getText().toString();
        password=passwordET.getText().toString();
        registerUser();
    }
}

