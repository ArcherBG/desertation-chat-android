package com.example.kkostov.chat.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.kkostov.chat.R;
import com.example.kkostov.chat.models.ClientListItemContent;

import java.util.List;

/**
 * Adapter for displaying all the clients that user is chatting with
 */
public class MyClientsRecyclerViewAdapter extends RecyclerView.Adapter<MyClientsRecyclerViewAdapter.ViewHolder> {

    private static final String TAG = MyClientsRecyclerViewAdapter.class.getSimpleName();
    private List<ClientListItemContent> clients;
    private RecyclerViewListener mListener;
    private Context context;
    private int imageNumber;

    public MyClientsRecyclerViewAdapter(Context context,List<ClientListItemContent> items, RecyclerViewListener listener) {
        clients = items;
        mListener = listener;
        this.context = context;
        imageNumber = 1; // the pic with id 0 is not so good
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_client, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {

        // Set the client object
        holder.clientItem = clients.get(position);

        // Set background image
        int imageNumber = generateNumber();
        holder.ivAvatar.setBackgroundResource(getIdentifier("ic_" + imageNumber));
        holder.ivAvatar.setAdjustViewBounds(true);
        //holder.ivAvatar.setScaleType(ImageView.ScaleType.CENTER_CROP);

        // Set the number of  unread messages
        int count = holder.clientItem.getNewMessagesCount();
        String countText = "(" + count + ")";
        holder.tvNewMessageIndicator.setText(countText);

        // Set email
        holder.tvEmail.setText(holder.clientItem.getEmail());

        //holder.mView.setBackgroundResource(R.mipmap.ic_4);
//        holder.mView.setAlpha(0.5f);


        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mListener) {
                    mListener.onClientClickedInList(holder.clientItem);
                }
            }
        });

        holder.ivDeleteClientHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mListener) {
                    mListener.onDeleteClientClicked(holder.clientItem);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return clients.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public View mView;
        public ImageView ivAvatar;
        public TextView tvNewMessageIndicator;
        public TextView tvEmail;
        public ImageView ivDeleteClientHistory;
        public ClientListItemContent clientItem;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            ivAvatar = (ImageView) view.findViewById(R.id.ivAvatar);
            tvNewMessageIndicator = (TextView) view.findViewById(R.id.tvNewMessageIndicator);
            tvEmail = (TextView) view.findViewById(R.id.tvEmail);
            ivDeleteClientHistory = (ImageView) view.findViewById(R.id.ivDeleteClientHistory);
        }

//        @Override
//        public String toString() {
//            return super.toString() + " '" + tvEmail.getText() + "'";
//        }
    }


    public void updateList(List<ClientListItemContent> clients) {
        this.clients = clients;
        this.notifyDataSetChanged();
    }

    public void swap(List<ClientListItemContent> newClients) {
        clients.clear();
        clients.addAll(newClients);
        notifyDataSetChanged();
        Log.d(TAG, "swap  clients count: " + newClients.size());
    }

    /**
     * Helper methods below     */

    public int getIdentifier( String name)
    {
        return this.context.getResources().getIdentifier(name, "mipmap", context.getPackageName());
    }

    private int generateNumber() {

        if(imageNumber == 4 )
        {
            imageNumber = 0;
        }

        return  imageNumber++;
    }

    public interface RecyclerViewListener {

        void onClientClickedInList(final ClientListItemContent client);

        void onDeleteClientClicked(final ClientListItemContent client);
    }
}
