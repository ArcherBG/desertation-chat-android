package com.example.kkostov.chat.fragments;

import android.app.Fragment;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.example.kkostov.chat.R;
import com.example.kkostov.chat.adapters.MyClientsRecyclerViewAdapter;
import com.example.kkostov.chat.adapters.MyClientsRecyclerViewAdapter.RecyclerViewListener;
import com.example.kkostov.chat.data.ClientsContract.ClientsEntry;
import com.example.kkostov.chat.data.ClientsDataProvider;
import com.example.kkostov.chat.data.HistoryContract.HistoryEntry;
import com.example.kkostov.chat.models.ClientListItemContent;
import com.example.kkostov.chat.sharedpreferences.SessionManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Fragment responsible for showing all clients user has previously chat with.
 */
public class ClientsFragment extends Fragment {

    private static final String TAG = ClientsFragment.class.getSimpleName();
    private static final String ARG_COLUMN_COUNT = "column-count";

    // Define a projection that specifies which columns from the database
    // you will actually use after this query.
    private static final String[] projection = {
            ClientsEntry._ID,
            ClientsEntry.COLUMN_CLIENT_ID,
            ClientsEntry.COLUMN_EMAIL
    };

    private final long MY_CLIENT_ID = SessionManager.getInstance(getActivity()).getClientId();

    private int mColumnCount = 2;  // default column count
    private LinearLayout llNoContacts;
    private RecyclerView recyclerView;
    private MyClientsRecyclerViewAdapter adapter;


    @SuppressWarnings("unused")
    public static ClientsFragment newInstance(int columnCount) {
        ClientsFragment fragment = new ClientsFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_COLUMN_COUNT, columnCount);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mColumnCount = getArguments().getInt(ARG_COLUMN_COUNT);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_clients, container, false);

        llNoContacts = (LinearLayout) view.findViewById(R.id.llNoContacts);

        // Set the adapter
//        if (view instanceof RecyclerView) {
        Context context = view.getContext();
        recyclerView = (RecyclerView) view.findViewById(R.id.list);
        if (mColumnCount <= 1) {
            recyclerView.setLayoutManager(new LinearLayoutManager(context));
        } else {
            recyclerView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
        }

        List<ClientListItemContent> clients = new ArrayList<>();
        adapter = new MyClientsRecyclerViewAdapter((Context) getActivity(), clients, (RecyclerViewListener) getActivity());
        recyclerView.setAdapter(adapter);

        getClientsFromDbViaHandler();


        return view;
    }

    private String findLastMessageTimestamp(long senderId, long receiverId) {

        //  WHERE (senderId = 5 and  receiverId = 1) OR    ( senderId = 1 and receiverId = 5)
        String selection = "( " + HistoryEntry.COLUMN_SENDER_ID + " = ? " + " AND " +
                HistoryEntry.COLUMN_RECEIVER_ID + " = ? " +
                " ) OR ( " +
                HistoryEntry.COLUMN_SENDER_ID + " = ? " + " AND " +
                HistoryEntry.COLUMN_RECEIVER_ID + " = ? " +
                " )";

        String selectionArgs[] = {Long.toString(senderId), Long.toString(receiverId), Long.toString(receiverId), Long.toString(senderId)};
        String sortOrder = HistoryEntry.COLUMN_TIMESTAMP + " DESC";

        // Get all messages in descending order of send time
        Cursor cursor = ((Context) getActivity()).getContentResolver().query(
                HistoryEntry.CONTENT_URI,
                new String[]{HistoryEntry.COLUMN_TIMESTAMP},
                selection,
                selectionArgs,
                sortOrder
        );

        // Get the latest time
        String timestamp = null;
        if (cursor != null && cursor.moveToFirst()) {
            timestamp = cursor.getString(cursor.getColumnIndexOrThrow(HistoryEntry.COLUMN_TIMESTAMP));
        }

        // Close the cursor
        if (cursor != null) {
            cursor.close();
        }

        return timestamp;
    }

    /**
     * Find the number of messages that are received from given client,
     * but are not read by the user of this app.
     */
    private int findCountOfUnreadMessages(long clientId) {
        int count = 0;

        // Filter results WHERE "id" = '5'
        String selection = HistoryEntry.COLUMN_SENDER_ID + " = ?" + " AND " +
                HistoryEntry.COLUMN_IS_MESSAGE_OUTGOING + " = ?" + " AND " +
                HistoryEntry.COLUMN_IS_MESSAGE_READ_BY_ME + " = ? ";
        String[] selectionArgs = {Long.toString(clientId) // sender id
                , "0"  // false
                , "0"}; // false

        Cursor cursor = ((Context) getActivity()).getContentResolver().query(
                HistoryEntry.CONTENT_URI,
                new String[]{HistoryEntry._ID},  // projection
                selection, // selection
                selectionArgs, // selectionArgs
                null  // sort order
        );


        // Get value and close cursor
        if (cursor != null) {
            count = cursor.getCount();
            cursor.close();
        }
        return count;
    }

    public void getClientsFromDbViaHandler() {

        // Query the db on worker thread
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                // Clear the previously stored records
                // clients.clear();
                List<ClientListItemContent> list = new ArrayList<>();

                ClientsDataProvider clientsDataProvider = new ClientsDataProvider((Context) getActivity());

                // How you want the results sorted in the resulting Cursor
                String sortOrder = ClientsEntry._ID + " ASC";

                Cursor cursor = clientsDataProvider.query(projection, null, null, sortOrder);
                // Iterate through elements and store them in list
                if (cursor.moveToFirst()) {
                    while (!cursor.isAfterLast()) {
                        long clientId = cursor.getLong(cursor.getColumnIndexOrThrow(ClientsEntry.COLUMN_CLIENT_ID));
                        String email = cursor.getString(cursor.getColumnIndexOrThrow(ClientsEntry.COLUMN_EMAIL));
                        list.add(new ClientListItemContent(clientId, email));
                        cursor.moveToNext();
                    }
                }

                // Add last message timestamp later to order by it
                for (ClientListItemContent client : list) {
                    String lastMsgTimestamp = findLastMessageTimestamp(client.getClientId(), MY_CLIENT_ID);
                    client.setLastMessageTimestamp(lastMsgTimestamp);
                    Log.d(TAG, "last message timestamp: " + lastMsgTimestamp);
                }

                // Set the number of unread messages from every client
                for (ClientListItemContent client : list) {
                    int unreadMsgCount = findCountOfUnreadMessages(client.getClientId());
                    client.setNewMessagesCount(unreadMsgCount);
                }

                // Sort the array by last time of message send or received
                Collections.sort(list, new Comparator<ClientListItemContent>() {
                    @Override
                    public int compare(ClientListItemContent firstObj, ClientListItemContent secondObj) {

                        // Handle if one or all values are null
                        if (firstObj.getLastMessageTimestamp() == null) {
                            return 1;
                        }
                        if (secondObj.getLastMessageTimestamp() == null) {
                            return -1;
                        } else if ((firstObj.getLastMessageTimestamp() == null) &&
                                (secondObj.getLastMessageTimestamp() == null)) {
                            return 0;
                        }

                        int result = firstObj.getLastMessageTimestamp().compareTo(secondObj.getLastMessageTimestamp());
                        return result;
                    }
                });

                // Reverse the list so the client with latest message activity to become first
                Collections.reverse(list);

                // Change UI accordingly
                if (list.size() == 0) {
                    //Toast.makeText(getActivity(), "No contacts to show", Toast.LENGTH_LONG).show();
                    llNoContacts.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    llNoContacts.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }

                //  final List<ClientListItemContent> copy = new ArrayList<ClientListItemContent>(list);
                // Update the list to show the records while still in handler
                ClientsFragment.this.updateUI(list);
            }
        });

    }

    private void updateUI(final List<ClientListItemContent> list) {

        if (adapter != null) {
            adapter.swap(list);
        }
    }

}
