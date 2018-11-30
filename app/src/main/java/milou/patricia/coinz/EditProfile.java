package milou.patricia.coinz;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
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

import java.io.IOException;

import java.util.HashMap;
import java.util.Map;

import timber.log.Timber;

public class EditProfile extends AppCompatActivity {
    //Firebase objects
    private FirebaseAuth mAuth;
    private StorageReference mStorageRef;
    private FirebaseUser user;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    //other
    private ImageView image;
    private Bitmap mBitmap;
    private EditText nameET,numberET,dobET;
    private TextView emaillbl;
    ProgressBar pgsBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);
        //get current user
        mAuth=FirebaseAuth.getInstance();
        mStorageRef = FirebaseStorage.getInstance().getReference();
        user= mAuth.getCurrentUser();
        image=findViewById(R.id.profile);
        nameET =findViewById(R.id.nameval);
        emaillbl =findViewById(R.id.emailval);
        numberET =findViewById(R.id.numberval);
        dobET =findViewById(R.id.dobval);
        /* get user's image */
        getImage();
        /* get user's details */
        getUser();
    }

    public void getUser(){
        DocumentReference docRef = db.collection("Users").document(user.getEmail()).collection("Info").document(user.getEmail());
        docRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    Timber.d("DocumentSnapshot data: %s", document.getData());
                    nameET.setText(document.getString("Full Name"));
                    emaillbl.setText(document.getId());
                    numberET.setText(document.getString("Telephone Number"));
                    dobET.setText(document.getString("Date of Birth"));
                } else {
                    Timber.d("No such document");
                    emaillbl.setText(user.getEmail());
                }
            } else {
                Timber.d(task.getException(), "get failed with ");
            }
        });
    }

    /**
     * This function get the user's image stored in Firebase
     */
    public void getImage() {
        StorageReference profRef = mStorageRef.child("images").child(user.getEmail()).child("profile.jpg");
        final long ONE_MEGABYTE = 1024 * 1024 *5;
        profRef.getBytes(ONE_MEGABYTE).addOnSuccessListener(bytes -> {
            Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            //load image in Image view
            image.setImageBitmap(bmp);
        }).addOnFailureListener(exception -> Timber.i(exception, "Failed to load"));
        pgsBar = findViewById(R.id.progressBar);
        pgsBar.setVisibility(View.GONE);

    }

    /**
     * This function saves the updated information of the User in the database
     * @param view Current View
     */
    public void SaveUser(View view) {
        //get updated information
        String name = nameET.getText().toString();
        String email = user.getEmail();
        String number = numberET.getText().toString();
        String dob = dobET.getText().toString();
        //create a new map object
        Map<String, Object> user = new HashMap<>();
        user.put("Full Name", name);
        user.put("Telephone Number", number);
        user.put("Date of Birth", dob);
        //update user
        db.collection("Users").document(email).collection("Info").document(email)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    Timber.d("DocumentSnapshot successfully updated.");
                    Toast.makeText(EditProfile.this, "Information Saved", Toast.LENGTH_SHORT).show();
                    Intent i = new Intent(EditProfile.this, MainActivity.class);
                    //go back to the showProf Activity
                    startActivity(i);
                    finish();
                })
                .addOnFailureListener(e -> Timber.tag("User").w(e, "Error updating document"));
    }

    /**
     * This function sends a password reset email to the user's email address
     * @param view Current view
     */
    public void forgotPassword(View view){
        mAuth.sendPasswordResetEmail(user.getEmail());
        Toast.makeText(EditProfile.this, "Check your inbox for a password reset email", Toast.LENGTH_SHORT).show();

    }

    /**
     * This function allows the user to select a new profile and update it in Firebase.
     * @param view Current View
     */
    public void uploadPhoto(View view){
        Intent photoPickerIntent = new Intent(Intent.ACTION_GET_CONTENT);
        photoPickerIntent.setType("image/*");
        startActivityForResult(photoPickerIntent, 1);

    }
    //once a photo has been selected
    protected void onActivityResult(int requestCode, int resultCode, Intent data){

        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK)
        {
            Uri chosenImageUri = data.getData();
            String name=data.toString();
            Timber.i(name);

            mBitmap = null;
            try {
                mBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), chosenImageUri);
            } catch (IOException e) {
                e.printStackTrace();
            }
            //update Firebase
            StorageReference imageRef = mStorageRef.child("images/"+user.getEmail()+"/profile.jpg");

            imageRef.putFile(chosenImageUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        // Get a URL to the uploaded content
                        taskSnapshot.getUploadSessionUri();
                        image.setImageBitmap(mBitmap);
                        Toast.makeText(EditProfile.this, "Uploaded", Toast.LENGTH_SHORT).show();
                        Timber.i("Uploaded");
                    })
                    .addOnFailureListener(exception -> {
                        Toast.makeText(EditProfile.this, "Failed,try again.", Toast.LENGTH_SHORT).show();
                        Timber.i(exception, "Not Uploaded");
                    });

        }
    }

}
