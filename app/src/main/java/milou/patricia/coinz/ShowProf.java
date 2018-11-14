package milou.patricia.coinz;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import timber.log.Timber;

public class ShowProf extends AppCompatActivity {
    private ImageView image;
    private TextView name,email,number,dob;
    private FirebaseAuth mAuth;
    private StorageReference mStorageRef;
    private FirebaseUser user;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_prof);
        //get current user
        mAuth=FirebaseAuth.getInstance();
        mStorageRef = FirebaseStorage.getInstance().getReference();
        user= mAuth.getCurrentUser();
        image=findViewById(R.id.profile);
        //get his profile
        getImage();
        //get his other details
        getUser();
        name =findViewById(R.id.nameval);
        email =findViewById(R.id.emailval);
        number =findViewById(R.id.numberval);
        dob =findViewById(R.id.dobval);
    }

    /**
     * If the user wants to change his password.
     * @param view Current View
     */
    public void forgotPassword(View view){
        //send password reset email
        mAuth.sendPasswordResetEmail(user.getEmail());
        Toast.makeText(ShowProf.this, "Check your inbox for a password reset email", Toast.LENGTH_SHORT).show();
    }

    /**
     * This starts another Activity, where the user can edit his information.
     * @param view Current view
     */
    public void EditUser(View view) {
        Intent i = new Intent(ShowProf.this, EditProfile.class);
        startActivity(i);
        this.finish();
    }

    /**
     * Get user's profile
     */
    public void getImage() {
        //Path to image
        StorageReference profRef = mStorageRef.child("images").child(user.getEmail()).child("profile.jpg");
        final long ONE_MEGABYTE = 1024 * 1024 *5;
        profRef.getBytes(ONE_MEGABYTE).addOnSuccessListener(bytes -> {
            Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            //set the image at the image view
            image.setImageBitmap(bmp);
        }).addOnFailureListener(exception -> {
        });
        ProgressBar pgsBar = findViewById(R.id.progressBar);
        pgsBar.setVisibility(View.GONE);

    }

    /**
     * Get personal infromation
     */
    public void getUser() {
        DocumentReference docRef = db.collection("Users").document(user.getEmail()).collection("Info").document(user.getEmail());
        docRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    Timber.d("DocumentSnapshot data: %s", document.getData());
                    //set his information at the variosu text views
                    name.setText(document.getString("Full Name"));
                    email.setText(document.getId());
                    number.setText(document.getString("Telephone Number"));
                    dob.setText(document.getString("Date of Birth"));
                } else {
                    Timber.d("No such document");
                }
            } else {
                Timber.d(task.getException(), "get failed with ");
            }
        });
    }
}
