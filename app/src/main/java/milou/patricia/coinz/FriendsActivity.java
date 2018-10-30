package milou.patricia.coinz;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.StorageReference;

import org.w3c.dom.Text;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class FriendsActivity extends AppCompatActivity {
    private View v;
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private DocumentSnapshot friendrequest;
    private TableLayout table;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends);
        v = getWindow().getDecorView();
        mAuth=FirebaseAuth.getInstance();
        user= mAuth.getCurrentUser();
        table = (TableLayout) findViewById(R.id.table);
        showFriends();
        checkFriendRequests();
    }

    public void showFriends() {
        ShowFriends sf= new ShowFriends(v,null);
        sf.showTable();
//        table.removeAllViews();
//
//        db.collection("Users").document(user.getEmail()).collection("Friends")
//                .orderBy("Email")
//                .get()
//                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
//                    @Override
//                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
//                        if (task.isSuccessful()) {
//                            int index=1;
//                            if(task.getResult().size()>0) {
//                                for (QueryDocumentSnapshot document : task.getResult()) {
//                                    //new row
//                                    TableRow row = new TableRow(FriendsActivity.this);
//                                    if(index % 2==0){
//                                        row.setBackgroundColor(Color.rgb(242, 242, 242));
//                                    }
//                                    index++;
//                                    row.setTextAlignment(TableRow.TEXT_ALIGNMENT_CENTER);
//                                    row.setOrientation(TableRow.HORIZONTAL);
//                                    row.setGravity(Gravity.CENTER_HORIZONTAL);
//                                    row.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
//                                    //set the format of the Text Views
//                                    TextView em= new TextView(FriendsActivity.this);
//                                    em.setText(document.getString("Email"));
//                                    em.setTextSize(17);
//                                    em.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
//                                    //once the user clicks on the row
//                                    row.setOnClickListener(new View.OnClickListener() {
//                                        @Override
//                                        public void onClick(View v) {
//                                            showProf(document.getString("Email"));
//                                            Toast.makeText(FriendsActivity.this, "ok", Toast.LENGTH_SHORT).show();
//                                        }
//                                    });
//
//                                    //add the row in the table
//                                    table.addView(row);
//                                    row.addView(em);
//                                }
//                            }else{
//                                TableRow row = new TableRow(FriendsActivity.this);
//                                TextView tv = new TextView(FriendsActivity.this);
//                                tv.setTextColor(Color.BLACK);
//                                tv.setTextSize(16);
//                                tv.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
//                                tv.setPadding(30,30,30,30);
//                                tv.setText("No friends.");
//                                table.addView(row);
//                                row.addView(tv);
//                            }
//                        } else {
//                            //if an exception occurs
//                            Log.e("Exception",task.getException().getMessage());
//                        }
//                    }
//                });

    }

//    public void showProf(String email){
//        db.collection("Users").document(email).collection("Info")
//                .get()
//                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
//                    @Override
//                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
//                        if (task.isSuccessful()) {
//                           for (QueryDocumentSnapshot document : task.getResult()){
//                               String name= document.getString("Full Name");
//                               String number= document.getString("Telephone Number");
//                               String dob= document.getString("Date of Birth");
//                               profpopup(email,name,dob,number);
//                           }
//                        }else{
//
//                        }
//                    }
//                });
//    }
    public void addAFriend(View view){
        LayoutInflater inflater = FriendsActivity.this.getLayoutInflater();
        View view2= inflater.inflate(R.layout.popup, null);
        TextView title=view2.findViewById(R.id.title);
        title.setText("Find your new friend");
        final EditText input =view2.findViewById(R.id.editTextDialogUserInput);
        Dialog dialog = new AlertDialog.Builder(this).setView(view2)
                // Add action buttons
                .setPositiveButton("Send Friend Request", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String value = input.getText().toString();
                        if (value != user.getEmail()) {
                            db.collection("Users").document(value).collection("Info").document(value)
                                    .get()
                                    .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                        @Override
                                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                            if (task.isSuccessful()) {
                                                DocumentSnapshot document = task.getResult();
                                                if (document.exists()) {
                                                    sendAFriendRequest(value);
                                                    Toast.makeText(FriendsActivity.this, "Friend request has been sent", Toast.LENGTH_SHORT).show();
                                                } else {
                                                    Toast.makeText(FriendsActivity.this, "Email is not valid.", Toast.LENGTH_SHORT).show();
                                                }
                                                dialog.dismiss();
                                            } else {
                                                Log.d("document", "get failed with ", task.getException());
                                            }
                                        }
                                    });
                        }
                    }
                    }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    // Do nothing.
                    dialog.dismiss();
                }
            }).show();
    }

    private void sendAFriendRequest(String email) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
        Date date = new Date();
        String date1 =dateFormat.format(date);
        Map<String, Object> request = new HashMap<>();
        request.put("Sender",user.getEmail());
        request.put("Date",date1);
        db.collection("Users").document(email).collection("Friend Requests").document()
                .set(request);

    }
    private void checkFriendRequests(){
        db.collection("Users").document(user.getEmail()).collection("Friend Requests")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                friendrequest=document;
                                acceptPopup(document.getString("Sender"),document.getString("Date"));

                          }
                        } else {
                            Log.d("document", "get failed with ", task.getException());
                        }
                    }
                });
    }

    private void acceptPopup(String sender,String date) {
        LayoutInflater inflater = FriendsActivity.this.getLayoutInflater();
        View view2= inflater.inflate(R.layout.popup2, null);
        TextView title=view2.findViewById(R.id.title);
        title.setText("New Friend Request");
        TextView info =view2.findViewById(R.id.info);
        info.setText(sender);
        Dialog dialog = new AlertDialog.Builder(FriendsActivity.this).setView(view2)
                // Add action buttons
                .setPositiveButton("Accept", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        Map<String, Object> friend = new HashMap<>();
                        friend.put("Email",sender);
                        friend.put("Friends since",date);

                        db.collection("Users").document(user.getEmail()).collection("Friends").document(sender).set(friend);
                        friend = new HashMap<>();
                        friend.put("Email",user.getEmail());
                        friend.put("Friends since",date);
                        db.collection("Users").document(sender).collection("Friends").document(user.getEmail()).set(friend);

                        friendrequest.getReference().delete();
                        //close dialog and update table
                        dialog.dismiss();
                        showFriends();

                    }
                }).setNegativeButton("Delete", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Delete friend request
               friendrequest.getReference().delete();
                dialog.dismiss();
            }
        }).show();


    }
//    private void profpopup(String email,String name,String dob, String number){
//        AlertDialog.Builder builder = new AlertDialog.Builder(FriendsActivity.this);
//        // Get the layout inflater
//        LayoutInflater inflater = FriendsActivity.this.getLayoutInflater();
//        View view2= inflater.inflate(R.layout.popup2, null);
//        TextView title=view2.findViewById(R.id.title);
//        title.setText(email);
//        TextView info =view2.findViewById(R.id.info);
//        String text= "Full Name: "+name+"\nContact Number: "+number+"\nDate of Birth: "+dob;
//        info.setText(text);
//        info.setTextSize(18);
//        builder.setView(view2).show();
//    }
}
