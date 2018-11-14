package milou.patricia.coinz;

import android.annotation.SuppressLint;
import android.app.Dialog;

import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import timber.log.Timber;

public class FriendsActivity extends AppCompatActivity {
    private View v;
    private FirebaseUser user;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private DocumentSnapshot friendrequest;
    private ShowFriends sf;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends);
        //get current view
        v = getWindow().getDecorView();
        //get current user
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        user= mAuth.getCurrentUser();
        //show friends
        showFriends();
        //check for friend requests
        checkFriendRequests();
    }

    public void showFriends() {
        sf= new ShowFriends(v,null);
        sf.showTable();
    }

    /**
     * This function is run when the user wants to add a new friend.
     * @param view Current view
     */
    @SuppressLint("SetTextI18n")
    public void addAFriend(View view){
        LayoutInflater inflater = FriendsActivity.this.getLayoutInflater();
        @SuppressLint("InflateParams") View view2= inflater.inflate(R.layout.popup, null);
        TextView title=view2.findViewById(R.id.title);
        title.setText("Find your new friend");
        final EditText input =view2.findViewById(R.id.editTextDialogUserInput);
        //show a dialog where the user will have to input an email addess
        Dialog dialog = new AlertDialog.Builder(this).setView(view2)
                // Add action buttons
                .setPositiveButton("Send Friend Request", (dialog12, whichButton) -> {
                    String value = input.getText().toString();
                    //check that the email address entered is not the same as the user's email address(Adding himself)
                    if (!user.getEmail().equals(value)&& !sf.friends.contains(value)) {
                        db.collection("Users").document(value).collection("Info").document(value)
                                .get()
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        //if such a user exists
                                        DocumentSnapshot document = task.getResult();
                                        if (document.exists()) {
                                            //sent the request
                                            sendAFriendRequest(value);
                                        } else {
                                            Toast.makeText(FriendsActivity.this, "Email is not valid.", Toast.LENGTH_SHORT).show();
                                        }
                                        dialog12.dismiss();
                                    } else {
                                        Timber.d(task.getException(), "get failed with ");
                                    }
                                });
                    }else{
                        if(user.getEmail().equals(value)) {
                            Toast.makeText(FriendsActivity.this, "You cannot add yourself.", Toast.LENGTH_SHORT).show();
                        }else if (sf.friends.contains(value)){
                            Toast.makeText(FriendsActivity.this, "You are already friends with "+value, Toast.LENGTH_SHORT).show();
                        }
                    }
                }).setNegativeButton("Cancel", (dialog1, whichButton) -> {
                        // Do nothing.
                        dialog1.dismiss();
                    }).show();
    }

    /**
     * This function sents the friend request
     * @param email Email address of the selected user
     */
    private void sendAFriendRequest(String email) {
        @SuppressLint("SimpleDateFormat") DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
        Date date = new Date();
        String date1 =dateFormat.format(date);
        //new mapping object
        //represents a friend request
        Map<String, Object> request = new HashMap<>();
        request.put("Sender",user.getEmail());
        request.put("Date",date1);
        db.collection("Users").document(email).collection("Friend Requests").document()
                .set(request);
        //once the friend request has been sent, toast a message
        Toast.makeText(FriendsActivity.this, "Friend request has been sent", Toast.LENGTH_SHORT).show();

    }

    /**
     * This function checks if the user received any friend requests
     */
    private void checkFriendRequests(){
        db.collection("Users").document(user.getEmail()).collection("Friend Requests")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // if friend requests exist
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            friendrequest=document;
                            //show a popup
                            acceptPopup(document.getString("Sender"),document.getString("Date"));

                      }
                    } else {
                        Timber.d(task.getException(), "get failed with ");
                    }
                });
    }

    /**
     * This function shows  the friend requests to the user, and allows him/her to either accept them or delete them.
     * @param sender Sender's email address
     * @param date Date that the friend request has been sent
     */
    @SuppressLint("SetTextI18n")
    private void acceptPopup(String sender, String date) {
        LayoutInflater inflater = FriendsActivity.this.getLayoutInflater();
        View view2= inflater.inflate(R.layout.popup2, null);
        TextView title=view2.findViewById(R.id.title);
        title.setText("New Friend Request");
        TextView info =view2.findViewById(R.id.info);
        info.setText(sender);
        Dialog dialog = new AlertDialog.Builder(FriendsActivity.this).setView(view2)
                // Add action buttons
                .setPositiveButton("Accept", (dialog12, whichButton) -> {
                    //friend object
                    Map<String, Object> friend = new HashMap<>();
                    friend.put("Email",sender);
                    friend.put("Friends since",date);

                    db.collection("Users").document(user.getEmail()).collection("Friends").document(sender).set(friend);
                    //second friend object
                    friend = new HashMap<>();
                    friend.put("Email",user.getEmail());
                    friend.put("Friends since",date);
                    db.collection("Users").document(sender).collection("Friends").document(user.getEmail()).set(friend);

                    friendrequest.getReference().delete();
                    //close dialog and update table
                    dialog12.dismiss();
                    showFriends();

                }).setNegativeButton("Delete", (dialog1, whichButton) -> {
                    // Delete friend request
                   friendrequest.getReference().delete();
                    dialog1.dismiss();
                }).show();


    }

}
