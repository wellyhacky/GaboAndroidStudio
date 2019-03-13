package com.example.gmailapp;

import android.accounts.Account;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener {
    Button myButton;
    Button signOutButton;
    TextView myText;
    ListView myList;
    private GoogleApiClient googleApiClient;

    private static final int REQ_CODE = 9001;
    private static final int REQUEST_AUTH = 9002;

    String[] SCOPES = { //cut this down
            GmailScopes.GMAIL_LABELS,
            GmailScopes.GMAIL_MODIFY,
            GmailScopes.GMAIL_READONLY,
            GmailScopes.MAIL_GOOGLE_COM
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        myButton = (Button) findViewById(R.id.button);
        signOutButton = (Button) findViewById(R.id.SignOut);
        myText = (TextView) findViewById(R.id.TextBoxThing);
        myList = (ListView) findViewById(R.id.Emails);

        GoogleSignInOptions signInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build();
        googleApiClient = new GoogleApiClient.Builder(this).enableAutoManage(this, this).addApi(Auth.GOOGLE_SIGN_IN_API, signInOptions).build();

        myButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SignIn();
            }
        });
        signOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SignOut();
            }
        });
    }
    private void SignIn(){
        Intent intent = Auth.GoogleSignInApi.getSignInIntent(googleApiClient);
        startActivityForResult(intent, REQ_CODE);
    }
    private void SignOut () {
        Auth.GoogleSignInApi.signOut(googleApiClient).setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(@NonNull Status status) {

            }
        });
    }
    private void handleResult (GoogleSignInResult result){
        if (result.isSuccess()){
            GoogleSignInAccount account = result.getSignInAccount();
            myText.setText(account.getDisplayName());
            new GetContactsTask(account.getAccount()).execute();
        } else {
            myText.setText("Didn't work at all");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_CODE){
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleResult(result);
        } else if (requestCode == REQUEST_AUTH){
            SignIn();
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    /** Global instance of the HTTP transport. */
    private static HttpTransport HTTP_TRANSPORT = AndroidHttp.newCompatibleTransport();
    /** Global instance of the JSON factory. */
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();


    private class GetContactsTask extends AsyncTask<Void, Void, List<Message>> {
        Account mAccount;
        public GetContactsTask(Account account) {
            mAccount = account;
        }

        @Override
        protected List<Message> doInBackground(Void... params) {
            List<Message> result = new ArrayList<Message>();
            try {
                GoogleAccountCredential credential =
                        GoogleAccountCredential.usingOAuth2(
                                getApplicationContext(),
                                Arrays.asList(SCOPES)
                        );
                credential.setSelectedAccount(mAccount);
                Gmail service = new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                        .setApplicationName("REST API sample")
                        .build();
                //Do stuff

                ListMessagesResponse response = service.users().messages().list("me").execute();
                while (response.getMessages() != null) {
                    result.addAll(response.getMessages());
                    if (response.getNextPageToken() != null) {
                        String pageToken = response.getNextPageToken();
                        response = service.users().messages().list("me").setPageToken(pageToken).execute();
                    } else {
                        break;
                    }
                }
                for (Message message : result) {
                    Message actualMessage = service.users().messages().get("me", message.getId()).execute();
                    List<MessagePart> parts = actualMessage.getPayload().getParts();
                    for (MessagePart part : parts) {
                        if (part.getFilename() != null && part.getFilename().length() > 0) {
                            String filename = part.getFilename();
                            System.out.println(filename);
                        }
                    }

                }

            } catch (UserRecoverableAuthIOException e) {
                startActivityForResult(e.getIntent(), REQUEST_AUTH);
                // Other non-recoverable exceptions.
            } catch (Exception e){
                System.out.println(e);

            }

            return result;
        }
    }

}
