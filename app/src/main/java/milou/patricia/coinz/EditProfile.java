package milou.patricia.coinz;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
    private String name,email,number,dob;
    private ProgressBar pgsBar;

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
        //get user's image
        getImage();
        //get user's details
        getUser();
    }

    public void getUser(){
        DocumentReference docRef = db.collection("Users").document(user.getEmail()).collection("Info").document(user.getEmail());
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        Log.d("document", "DocumentSnapshot data: " + document.getData());
                        nameET.setText(document.getString("Full Name"));
                        emaillbl.setText(document.getId());
                        numberET.setText(document.getString("Telephone Number"));
                        dobET.setText(document.getString("Date of Birth"));
                    } else {
                        Log.d("document", "No such document");
                        emaillbl.setText(user.getEmail());
                    }
                } else {
                    Log.d("document", "get failed with ", task.getException());
                }
            }
        });
    }

    /**
     * This function get the user's image stored in Firebase
     */
    public void getImage() {
        StorageReference profRef = mStorageRef.child("images").child(user.getEmail()).child("profile.jpg");
        final long ONE_MEGABYTE = 1024 * 1024 *5;
        profRef.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
            @Override
            public void onSuccess(byte[] bytes) {
                Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                //load image in Imageview
                image.setImageBitmap(bmp);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Log.i("Photo","Failed to load",exception);
            }
        });
        pgsBar = (ProgressBar)findViewById(R.id.progressBar);
        pgsBar.setVisibility(View.GONE);

    }

    /**
     * This function saves the updated information of the User in the database
     * @param view
     */
    public void SaveUser(View view) {
        //get updated information
        name=nameET.getText().toString();
        email=user.getEmail();
        number=numberET.getText().toString();
        dob=dobET.getText().toString();
        //create a new map object
        Map<String, Object> user = new HashMap<>();
        user.put("Full Name", name);
        user.put("Telephone Number", number);
        user.put("Date of Birth", dob);
        //update user
        db.collection("Users").document(email).collection("Info").document(email)
                .set(user)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d("User", "DocumentSnapshot successfully updated.");
                        Toast.makeText(EditProfile.this, "Information Saved", Toast.LENGTH_SHORT).show();
                        Intent i = new Intent(EditProfile.this, ShowProf.class);
                        //go back to the showProf Activity
                        startActivity(i);
                        finish();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("User", "Error updating document", e);
                    }
                });
    }

    /**
     * This function sends a password reset email to the user's email address
     * @param view
     */
    public void forgotPassword(View view){
        mAuth.sendPasswordResetEmail(user.getEmail());
        Toast.makeText(EditProfile.this, "Check your inbox for a password reset email", Toast.LENGTH_SHORT).show();

    }

    /**
     * This function allows the user to select a new profile and update it in Firebase.
     * @param view
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
            Log.i("file",name);

            mBitmap = null;
            try {
                mBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), chosenImageUri);
            } catch (IOException e) {
                e.printStackTrace();
            }
            Uri file =chosenImageUri;
            //update Firebase
            StorageReference imageRef = mStorageRef.child("images/"+user.getEmail()+"/profile.jpg");

            imageRef.putFile(file)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            // Get a URL to the uploaded content
                            Uri downloadUrl = taskSnapshot.getUploadSessionUri();
                            image.setImageBitmap(mBitmap);
                            Toast.makeText(EditProfile.this, "Uploaded", Toast.LENGTH_SHORT).show();
                            Log.i("User's Photo","Uploaded");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            Toast.makeText(EditProfile.this, "Failed,try again.", Toast.LENGTH_SHORT).show();
                            Log.i("User's Photo","Not Uploaded",exception);
                        }
                    });

        }
    }

}
