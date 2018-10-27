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

public class ShowProf extends AppCompatActivity {
    private ImageView image;
    private Bitmap mBitmap;
    private TextView name,email,number,dob;
    private FirebaseAuth mAuth;
    private StorageReference mStorageRef;
    private FirebaseUser user;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private ArrayList<String> pathArray = new ArrayList<String>();
    private ProgressBar pgsBar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_prof);
        mAuth=FirebaseAuth.getInstance();
        mStorageRef = FirebaseStorage.getInstance().getReference();
        user= mAuth.getCurrentUser();
        image=findViewById(R.id.profile);
        getImage();
        getUser();
        name =findViewById(R.id.nameval);
        email =findViewById(R.id.emailval);
        number =findViewById(R.id.numberval);
        dob =findViewById(R.id.dobval);
    }
    public void forgotPassword(View view){
        mAuth.sendPasswordResetEmail(user.getEmail());
        Toast.makeText(ShowProf.this, "Check your inbox for a password reset email", Toast.LENGTH_SHORT).show();
    }

    public void EditUser(View view) {
        Intent i = new Intent(ShowProf.this, EditProfile.class);
        startActivity(i);
    }

    public void getImage() {
         StorageReference profRef = mStorageRef.child("images").child(user.getEmail()).child("profile.jpg");
        final long ONE_MEGABYTE = 1024 * 1024 *5;
        profRef.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
            @Override
            public void onSuccess(byte[] bytes) {
                Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                image.setImageBitmap(bmp);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
            }
        });
        pgsBar = (ProgressBar)findViewById(R.id.progressBar);
        pgsBar.setVisibility(View.GONE);

    }

    public void getUser() {
        DocumentReference docRef = db.collection("Users").document(user.getEmail()).collection("Info").document(user.getEmail());
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        Log.d("document", "DocumentSnapshot data: " + document.getData());
                        name.setText(document.getString("Full Name"));
                        email.setText(document.getId());
                        number.setText(document.getString("Telephone Number"));
                        dob.setText(document.getString("Date of Birth"));
                    } else {
                        Log.d("document", "No such document");
                    }
                } else {
                    Log.d("document", "get failed with ", task.getException());
                }
            }
        });
    }
}
