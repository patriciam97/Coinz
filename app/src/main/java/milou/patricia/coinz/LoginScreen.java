package milou.patricia.coinz;

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


public class LoginScreen extends AppCompatActivity {

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
    private String EmailValue,PasswordValue,SavedValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_screen);
        mAuth = FirebaseAuth.getInstance();
        eye=findViewById(R.id.eye);
        emailInput=findViewById(R.id.emailInput);
        passwordInput=findViewById(R.id.passwordInput);
        checkBox= (CheckBox)findViewById(R.id.rememberme);
        checkBox.setChecked(true); //by default set it to true

    }
    public void passwordbtn (View view){
        if(see==true) {//normal text form
            hidepassword();
        }else{
            showpassword();
        }
    }
    public void showpassword(){
        Log.v("see before", "" + see);
        //passwordInput.setInputType(InputType.TYPE_TEXT_VARIATION_NORMAL);
        passwordInput.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
        see=true;
        int imageResource = getResources().getIdentifier("@drawable/eye", null, getPackageName());
        Drawable res = getResources().getDrawable(imageResource);
        eye.setImageDrawable(res);
        Log.v("see after", "" + see);
    }
    public void hidepassword(){
        Log.v("see before", "" + see);
       // passwordInput.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
        passwordInput.setTransformationMethod(PasswordTransformationMethod.getInstance());
        see=false;
        int imageResource = getResources().getIdentifier("@drawable/mascara", null, getPackageName());
        Drawable res = getResources().getDrawable(imageResource);
        eye.setImageDrawable(res);
        Log.v("see after", "" + see);
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
                            user = mAuth.getCurrentUser();
                            updateUI(user);
                            Intent i = new Intent(LoginScreen.this, MainActivity.class);
                            startActivity(i);
                            finish();

                        } else {
                            // If sign in fails, display a message to the user.
                            updateUI(null);
                            Toast.makeText(LoginScreen.this, "An error has occured please try again.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    });
    }

    public void forgotPassword(View view){
        AlertDialog.Builder builder = new AlertDialog.Builder(LoginScreen.this);
        // Get the layout inflater
        LayoutInflater inflater = LoginScreen.this.getLayoutInflater();
        View view2= inflater.inflate(R.layout.popup, null);
        final EditText input =view2.findViewById(R.id.editTextDialogUserInput);
        builder.setView(view2)
                // Add action buttons
                .setPositiveButton("Continue", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Editable value =input.getText();
                        mAuth.sendPasswordResetEmail(value.toString());
                        Toast.makeText(LoginScreen.this, "Check your inbox for a password reset email", Toast.LENGTH_SHORT).show();
                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Do nothing.
            }
        }).show();
    }

    private void savePreferences(String email, String password, Boolean save) {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME,Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();

        editor.putString(PREF_EMAIL, email);
        editor.putString(PREF_PASSWORD, password);
        editor.putString(PREF_SAVE,save.toString());
        editor.commit();
    }
    private void loadPreferences() {

        SharedPreferences settings = getSharedPreferences(PREFS_NAME,Context.MODE_PRIVATE);
        EmailValue = settings.getString(PREF_EMAIL,"");
        PasswordValue = settings.getString(PREF_PASSWORD,"");
        SavedValue = settings.getString(PREF_SAVE,"");
        emailInput.setText(EmailValue);
        passwordInput.setText(PasswordValue);
        if(SavedValue=="true"){
            checkBox.setChecked(true);
        }else{
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
