package com.example.kkostov.chat.fragments;


import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.example.kkostov.chat.R;


public class MyDialogFragment extends DialogFragment {

    private DialogFragmentListener mListener;
    private EditText etEmail;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mListener = (DialogFragmentListener) ((Context) getActivity());
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View rootView = inflater.inflate(R.layout.fragment_start_chat_dialog, null, false);
        etEmail = (EditText) rootView.findViewById(R.id.etEmailInput);

        // 1. Instantiate an AlertDialog.Builder with its constructor
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(rootView);

        // 2. Chain together various setter methods to set the dialog characteristics
        builder.setMessage(R.string.dialog_message)
                .setTitle(R.string.dialog_title);

        // Add the buttons
        builder.setPositiveButton(R.string.done, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked OK button
                onPositiveButtonPressed();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
                mListener.OnDialogNegativeButtonClicked();

            }
        });

        // 3. Get the AlertDialog from create()
        AlertDialog dialog = builder.create();
        return dialog;
    }

//    @Override
//    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
//        super.onViewCreated(view, savedInstanceState);
//        etEmail = (EditText) view.findViewById(R.id.etEmailInput);
//    }

    public void onPositiveButtonPressed() {
        String email = etEmail.getText().toString().trim();
        Log.d("MyDialogFragment", "onPositiveButtonPressed email: " + email);

        // Check if email is valid
        if (android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {

            mListener.onDialogPositiveButtonClicked(email);
        } else {

            Toast.makeText((Context) getActivity(), "Email is not valid", Toast.LENGTH_LONG).show();
            mListener.OnDialogNegativeButtonClicked();
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof DialogFragmentListener) {
            mListener = (DialogFragmentListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement DialogFragmentListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /***
     * Callback to inform the activity about the result
     */
    public interface DialogFragmentListener {
        void onDialogPositiveButtonClicked(String email);

        void OnDialogNegativeButtonClicked();
    }
}
