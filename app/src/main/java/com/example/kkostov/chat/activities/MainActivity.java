package com.example.kkostov.chat.activities;

import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.kkostov.chat.R;
import com.example.kkostov.chat.adapters.MyClientsRecyclerViewAdapter.RecyclerViewListener;
import com.example.kkostov.chat.broadcastreceivers.NetworkChangeReceiver;
import com.example.kkostov.chat.data.ClientsDataProvider;
import com.example.kkostov.chat.data.HistoryContract.HistoryEntry;
import com.example.kkostov.chat.fragments.ChatFragment;
import com.example.kkostov.chat.fragments.ChatFragment.MainFragmentListener;
import com.example.kkostov.chat.fragments.ClientsFragment;
import com.example.kkostov.chat.fragments.MyDialogFragment;
import com.example.kkostov.chat.fragments.MyDialogFragment.DialogFragmentListener;
import com.example.kkostov.chat.interfaces.ErrorOccurredCallback;
import com.example.kkostov.chat.interfaces.LoginHelperCallback;
import com.example.kkostov.chat.interfaces.MainActivityHelperCallback;
import com.example.kkostov.chat.models.ClientListItemContent;
import com.example.kkostov.chat.services.SocketService;
import com.example.kkostov.chat.sharedpreferences.SessionManager;
import com.example.kkostov.chat.utilities.TimeUtility;

import static com.example.kkostov.chat.data.ClientsContract.ClientsEntry.COLUMN_CLIENT_ID;

public class MainActivity extends AppCompatActivity implements MainFragmentListener,
        DialogFragmentListener, MainActivityHelperCallback, RecyclerViewListener, ErrorOccurredCallback {

    public static final String INTENT_MESSAGE_RESTART_SERVICE = "intent_message_restart";

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final long SUPPORT_ID = 1;
    private static int COLUMN_COUNT = 2;
    private SocketService mService;
    private boolean mBound = false;
    private String receiverEmail = null;

    private TextView tvNoService;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        NetworkChangeReceiver receiver = new NetworkChangeReceiver();
        receiver.setListener(MainActivity.this);

        tvNoService = (TextView) MainActivity.this.findViewById(R.id.tvNoService);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);


        // Bind to SocketService
        Intent intent = new Intent(this, SocketService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        FragmentTransaction ft = getFragmentManager().beginTransaction();
        SessionManager sessionManager = SessionManager.getInstance(getApplicationContext());

        // Depending weather the client is admin load the appropriate fragment
        if (sessionManager.isAdminModeEnabled()) {
            ClientsFragment clientsFragment = ClientsFragment.newInstance(COLUMN_COUNT);
            ft.addToBackStack(ClientsFragment.class.getSimpleName())
                    .add(R.id.flMainActivityFrame, clientsFragment, ClientsFragment.class.getSimpleName()).commit();
        } else {

            ChatFragment chatFragment = ChatFragment.newInstance(SUPPORT_ID);
            ft.addToBackStack(ChatFragment.class.getSimpleName())
                    .add(R.id.flMainActivityFrame, chatFragment, ChatFragment.class.getSimpleName()).commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity_main in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_new_chat) {
            // Open dialog for user to enter receivers email
            askUserForReceiverEmail();

            // Show spinner until server calback returns the receiverId
            findViewById(R.id.loadingPanel).setVisibility(View.VISIBLE);
        }
        if (id == R.id.action_signout) {
            // Remove stored user data
            SessionManager.getInstance(getApplicationContext()).removeAllData();

            Intent intent = new Intent(this, LoginActivity.class);
            intent.putExtra(INTENT_MESSAGE_RESTART_SERVICE, true);
            startActivity(intent);

            finish();
        }

        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }

        return super.onOptionsItemSelected(item);
    }


    private void askUserForReceiverEmail() {
        MyDialogFragment newFragment = new MyDialogFragment();
        newFragment.show(getFragmentManager(), "dialog");
    }

    @Override
    public void onBackPressed() {
        int index = getFragmentManager().getBackStackEntryCount() - 1;
        FragmentManager.BackStackEntry backEntry = getFragmentManager().getBackStackEntryAt(index);
        String tag = backEntry.getName();

        Log.d(TAG, "onBackPressed fragment count: " + index + " tag: " + tag);
        if (index == 0) {
            // No fragment to remove so close the app
            finish();
        } else {
            getFragmentManager().popBackStack();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: will tell the service to stop");
       // mService.forceCloseSocket();

        // Unbind from the service
        if (mBound) {

            unbindService(mConnection);
            mBound = false;
        }
    }

    @Override
    public void hasConnection(boolean hasConnection) {

        Log.d(TAG, "hasConnection: " + hasConnection);

        if (tvNoService != null) {
            this.showNoServiceUI(!hasConnection);
        }
    }

    public void showNoServiceUI(final boolean show) {
        Log.d(TAG, "showNoServiceUI: " + show);

        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (show) {
                    tvNoService.setText(R.string.no_service);
                    tvNoService.setVisibility(View.VISIBLE);
                } else {
                    tvNoService.setVisibility(View.GONE);
                }
            }
        });

    }


    private void markMessagesAsRead(long senderId) {

        ContentValues contentValues = new ContentValues();
        contentValues.put(HistoryEntry.COLUMN_IS_MESSAGE_READ_BY_ME, 1);   // 1 = true

        String selection = HistoryEntry.COLUMN_SENDER_ID + " = ? ";
        String selectionArgs[] = {String.valueOf(senderId)};


        int rowsUpdated = getApplicationContext().getContentResolver().update(
                HistoryEntry.CONTENT_URI,
                contentValues,
                selection,
                selectionArgs);

    }

    @Deprecated
    private boolean hasForIntenetConnection() {
        ConnectivityManager cm =
                (ConnectivityManager) ((Context) this).getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();

        return isConnected;
    }


    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to SocketService, cast the IBinder and get LocalService instance
            SocketService.LocalBinder binder = (SocketService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;

            // Set the listener to receive callback from service
            mService.setMainActivityCallback((MainActivityHelperCallback) MainActivity.this);
            mService.setErrorOccuredCallback((ErrorOccurredCallback) MainActivity.this);

        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    /*  Callback methods are implemented  below  */

    @Override
    public void sendMessageButtonClicked(String text, final long receiverId) {
        long myId = SessionManager.getInstance(getApplicationContext()).getClientId();

        // Prepare data to  save in db
        ContentValues contentValues = new ContentValues();
        contentValues.put(HistoryEntry.COLUMN_SENDER_ID, myId);            // set my id for later to show in chat fragment the chat history
        contentValues.put(HistoryEntry.COLUMN_RECEIVER_ID, receiverId);
        contentValues.put(HistoryEntry.COLUMN_MESSAGE, text);
        contentValues.put(HistoryEntry.COLUMN_IS_MESSAGE_OUTGOING, 1); // We set to true because we are sending to server
        contentValues.put(HistoryEntry.COLUMN_IS_MESSAGE_RECEIVED, 0); // At the moment we assume that is has not been send
        contentValues.put(HistoryEntry.COLUMN_IS_MESSAGE_READ_BY_ME, 1); // This is always true because we are the ones who wrote it
        contentValues.put(HistoryEntry.COLUMN_TIMESTAMP, TimeUtility.getCurrentTimeStamp());  // Time we send the message

        Log.d(TAG, "sendMessageButtonClicked Will save text and isOutgoung: " + text + " --- " + contentValues.getAsInteger(HistoryEntry.COLUMN_IS_MESSAGE_OUTGOING));

        // Save message in db
        Uri newRecordUri = getContentResolver().insert(HistoryEntry.CONTENT_URI, contentValues);
        long newRecordId = ContentUris.parseId(newRecordUri);
        Log.d(TAG, "sendMessageButtonClicked: inserted : " + newRecordId);
        mService.sendWhisperMessageToServer(text, receiverId, newRecordId);
    }

    @Override
    public void markMessagesWithClientAsRead(long receiverId) {
        MainActivity.this.markMessagesAsRead(receiverId);
    }

    @Override
    public void sendMessageAgain(final String message, final long receiverId, final int messageId) {

        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage(R.string.send_again)
                .setPositiveButton(R.string.send, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        mService.sendWhisperMessageToServer(message, receiverId, messageId);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //  Remove  loading spinner
                        MainActivity.this.findViewById(R.id.loadingPanel).setVisibility(View.GONE);
                        dialog.dismiss();
                    }
                })
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        //  Remove  loading spinner
                        MainActivity.this.findViewById(R.id.loadingPanel).setVisibility(View.GONE);
                        dialog.dismiss();
                    }
                });
        Dialog dialog = builder.create();
        dialog.show();
    }

    /*
    Callback method from DialogFragmentListener
     */
    @Override
    public void onDialogPositiveButtonClicked(String email) {

        // Ask server to give you the receivers id
        mService.findClientInfo(0, email, MainActivity.this);

        // When user adds new email contact return him to ClientsFragment
        Fragment f = MainActivity.this.getFragmentManager().findFragmentById(R.id.flMainActivityFrame);
        if (f instanceof ChatFragment) {
            onBackPressed();
        }
    }

    @Override
    public void OnDialogNegativeButtonClicked() {
        Log.d(TAG, "OnDialogNegativeButtonClicked: ");
        //  Remove  loading spinner
        MainActivity.this.findViewById(R.id.loadingPanel).setVisibility(View.GONE);
    }

    @Override
    public void findClientResponse(final long clientId) {

        // Because this was called from another thread, run on UI thread explicitly.
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Validate that the client exist
                if (clientId <= 0) {
                    Toast.makeText(MainActivity.this, "There is no user with such email", Toast.LENGTH_LONG).show();
                    // Remove loading spinner
                    findViewById(R.id.loadingPanel).setVisibility(View.GONE);
                    return;
                }

                // Remove loading spinner
                findViewById(R.id.loadingPanel).setVisibility(View.GONE);

                updateClientsFragmentUI();

//                // Open new chat fragment
//                FragmentTransaction ft = getFragmentManager().beginTransaction();
//                ChatFragment chatFragment = ChatFragment.newInstance(clientId);
//                ft.addToBackStack(ChatFragment.class.getSimpleName())
//                        .replace(R.id.flMainActivityFrame, chatFragment, ChatFragment.class.getSimpleName()).commit();
            }
        });

    }

    /**
     * Update the clients and the number of their messages that are unread by the user of this app.
     */
    @Override
    public void updateClientsFragmentUI() {
        //Find ClientsFragment and tell it to requery the db.
        final Fragment fragment = getFragmentManager().findFragmentByTag(ClientsFragment.class.getSimpleName());
        if (fragment instanceof ClientsFragment) {
            // Because the method invocation originated from the service, explicitly tell to run on UI thread
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ((ClientsFragment) fragment).getClientsFromDbViaHandler();
                }
            });
        }
    }

    @Override
    public void onClientClickedInList(ClientListItemContent client) {
        //   Toast.makeText(MainActivity.this, "clicked on client in list", Toast.LENGTH_SHORT).show();

        ChatFragment chatFragment = ChatFragment.newInstance(client.getClientId());
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.addToBackStack(ChatFragment.class.getSimpleName())
                .replace(R.id.flMainActivityFrame, chatFragment, ChatFragment.class.getSimpleName()).commit();

        // Mark messages ar read when the chat fragment is opened.
        markMessagesAsRead(client.getClientId());
    }


    /**
     * Show dialog  and delete if positive
     */
    @Override
    public void onDeleteClientClicked(final ClientListItemContent client) {


        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage(R.string.delete_client)
                .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        String userIdAsString = String.valueOf(SessionManager.getInstance(MainActivity.this).getClientId());

                        // Delete from messages table
                        String historySelection = "( " + HistoryEntry.COLUMN_RECEIVER_ID + " = ? AND " + HistoryEntry.COLUMN_SENDER_ID +  " = ? ) OR (" +
                                 HistoryEntry.COLUMN_RECEIVER_ID + " = ? AND " + HistoryEntry.COLUMN_SENDER_ID +  " = ? )";
                        String historySelectionArgs[] = {String.valueOf(client.getClientId()), userIdAsString, userIdAsString,  String.valueOf(client.getClientId())};
                        getContentResolver().delete(HistoryEntry.CONTENT_URI, historySelection, historySelectionArgs);

                        // Delete from client table
                        ClientsDataProvider clientsDataProvider = new ClientsDataProvider(MainActivity.this);
                        String selection = COLUMN_CLIENT_ID + " = ?";
                        String selectionArgs[] = {String.valueOf(client.getClientId())};
                        clientsDataProvider.delete(selection, selectionArgs);

                        updateClientsFragmentUI();
                    }
                })
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        //  Remove  loading spinner
                        MainActivity.this.findViewById(R.id.loadingPanel).setVisibility(View.GONE);
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
        Dialog dialog = builder.create();
        dialog.show();
    }


}
