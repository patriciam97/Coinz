package milou.patricia.coinz;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.BoringLayout;
import android.text.Editable;
import android.text.InputType;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import timber.log.Timber;


public class LoginScreen extends AppCompatActivity {
    //firebase objects
    private FirebaseAuth mAuth;
    private EditText emailInput,passwordInput;
    private String email,password;
    private CheckBox checkBox ;
    private ImageView eye;
    private boolean see=false;
    public FirebaseUser user;
    private static final String PREFS_NAME = "preferences";
    private static final String PREF_EMAIL = "Username";
    private static final String PREF_PASSWORD = "Password";
    private static final String PREF_SAVE = "Save";
    protected String PasswordValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_screen);
        mAuth = FirebaseAuth.getInstance();
        eye=findViewById(R.id.eye);
        emailInput=findViewById(R.id.emailInput);
        passwordInput=findViewById(R.id.passwordInput);
        checkBox= findViewById(R.id.rememberme);
        //checkBox.setChecked(true); //by default set it to true

    }

    /**
     * This function controls if the passord will be visible or now.
     * @param view Current View
     */
    public void passwordbtn (View view){
        if(see) { //normal text form
            hidepassword();
        }else{ //if password is hidden
            showpassword();
        }
    }
    /*
    This function makes the password text visible.
     */
    public void showpassword(){
        //change it
        passwordInput.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
        see=true;
        int imageResource = getResources().getIdentifier("@drawable/eye", null, getPackageName());
        Drawable res = getResources().getDrawable(imageResource);
        eye.setImageDrawable(res);
        Timber.tag("Password Edit Text").v("Changed");
    }

    /**
     * This function makes the password text back to hidden.
     */
    public void hidepassword(){
        //change it
        passwordInput.setTransformationMethod(PasswordTransformationMethod.getInstance());
        see=false;
        int imageResource = getResources().getIdentifier("@drawable/mascara", null, getPackageName());
        Drawable res = getResources().getDrawable(imageResource);
        eye.setImageDrawable(res);
        Timber.tag("Password Edit Text").v("Changed");
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

    /**
     * This function starts the Register Acitivy where someone can create an account.
     * @param view Current View
     */
    public void registerUser (View view){
        Intent i = new Intent(LoginScreen.this, RegisterActivity.class);
        startActivity(i);
        finish();
    }

    /**
     * This function logins the user.
     * @param view Current View
     */
    public void loginUser (View view){
        //get email and password input from the user
        email=emailInput.getText().toString();
        password=passwordInput.getText().toString();
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign in success, update UI with the signed-in user's information
                        Toast.makeText(LoginScreen.this, "User Login Successful", Toast.LENGTH_SHORT).show();
                        user = mAuth.getCurrentUser();
                        updateUI(user);
                        Intent i = new Intent(LoginScreen.this, MainActivity.class);
                        startActivity(i);
                        finish();
                        Timber.i("Success");

                    } else {
                        // If sign in fails, display a message to the user.
                        updateUI(null);
                        Toast.makeText(LoginScreen.this, "An error has occured please try again.", Toast.LENGTH_SHORT).show();
                        Timber.i("Failure");
                    }
                });
    }

    /**
     * This function is run if the user had forgot his password.
     * In this case, a dialog will inflate where the user has to type his email.
     * A reset password email is then sent to that email address.
     * @param view Current View
     */
    public void forgotPassword(View view){
        AlertDialog.Builder builder = new AlertDialog.Builder(LoginScreen.this);
        // Get the layout inflater
        LayoutInflater inflater = LoginScreen.this.getLayoutInflater();
        @SuppressLint("InflateParams") View view2= inflater.inflate(R.layout.popup, null);
        final EditText input =view2.findViewById(R.id.editTextDialogUserInput);
        builder.setView(view2)
                // Add action buttons
                .setPositiveButton("Continue", (dialog, whichButton) -> {
                    //get email address
                    Editable value =input.getText();
                    //send email
                    mAuth.sendPasswordResetEmail(value.toString());
                    Toast.makeText(LoginScreen.this, "Check your inbox for a password reset email", Toast.LENGTH_SHORT).show();
                    Timber.i("Passrod Reset Email Send.");
                }).setNegativeButton("Cancel", (dialog, whichButton) -> {
                    // Do nothing.
                }).show();
    }

    /**
     * The following information is stored in the application.
     * @param email Email of user
     * @param password Password of user
     * @param save Boolean value which indicates if the application should remember his credentials.
     */
    private void savePreferences(String email, String password, Boolean save) {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME,Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();

        editor.putString(PREF_EMAIL, email);
        editor.putString(PREF_PASSWORD, password);
        editor.putString(PREF_SAVE, save.toString());
        editor.apply();
    }

    /**
     * This functions loads the above variables.
     */
    private void loadPreferences() {

        SharedPreferences settings = getSharedPreferences(PREFS_NAME,Context.MODE_PRIVATE);
        String emailValue = settings.getString(PREF_EMAIL, "");
        PasswordValue = settings.getString(PREF_PASSWORD,"");
        String savedValue = settings.getString(PREF_SAVE,"false");
        emailInput.setText(emailValue);
        passwordInput.setText(PasswordValue);
        if(savedValue=="true"){
            checkBox.setChecked(true);
        }else if(savedValue=="false"){
            checkBox.setChecked(false);
        }
    }
    @Override
    public void onPause() {
        super.onPause();
        if (checkBox.isChecked()) {
            savePreferences(email,password,true);
        }else{
            savePreferences("","",false);
        }
    }
    @Override
    public void onResume() {
        super.onResume();
        loadPreferences();
    }


}
