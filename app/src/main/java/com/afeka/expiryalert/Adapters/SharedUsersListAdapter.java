package com.afeka.expiryalert.Adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.afeka.expiryalert.R;
import com.afeka.expiryalert.logic.Functions;
import com.afeka.expiryalert.logic.User;

import java.util.List;

public class SharedUsersListAdapter extends ArrayAdapter<User> {

    private Context mContext;
    private int resource;
    private User user;
    private List<User> userList;
    private static SharedUsersListAdapter.SharedUserListClickListener listListener;


    public interface SharedUserListClickListener {
        void isChanged();
    }

    public SharedUsersListAdapter(@NonNull Context context, int resource, @NonNull User userFromOut, SharedUserListClickListener listener) {
        super(context, resource, userFromOut.getSharingList());

        this.mContext = context;
        this.resource = resource;
        this.userList = userFromOut.getSharingList();
        this.listListener = listener;
        user = userFromOut;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(resource, null);

        TextView usernameTextview = view.findViewById(R.id.userlist_email);
        TextView emailTextview = view.findViewById(R.id.userlist_token);
        ImageView removeButton = view.findViewById(R.id.userlist_remove_button);

        usernameTextview.setText(userList.get(position).getUsername());
        emailTextview.setText("(" + userList.get(position).getEmail() + ")");

        removeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view1) {
                LayoutInflater factory = LayoutInflater.from(getContext());
                final View confirmDeleteDialogView = factory.inflate(R.layout.confirm_delete_layout, null);
                final AlertDialog confirmDeleteDialog = new AlertDialog.Builder(getContext()).create();
                confirmDeleteDialog.setView(confirmDeleteDialogView);
                confirmDeleteDialog.setCanceledOnTouchOutside(false);
                confirmDeleteDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

                TextView confirmDeleteHeader = confirmDeleteDialogView.findViewById(R.id.confirm_delete_dialog_header);
                confirmDeleteHeader.setText(userList.get(position).getEmail());

                confirmDeleteDialogView.findViewById(R.id.confirm_delete_dialog_header_btn_save)
                        .setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                String userRemoved = userList.get(position).getEmail();
                                boolean isRemoved = Functions.removeUserFromSharingList(user, position);
                                if (isRemoved) {
                                    Toast.makeText(view.getContext(), userRemoved + " Has been Removed!",
                                            Toast.LENGTH_LONG).show();
                                    listListener.isChanged();
                                }
                                confirmDeleteDialog.dismiss();
                            }
                        });
                confirmDeleteDialogView.findViewById(R.id.confirm_delete_dialog_header_btn_cancel).
                        setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                confirmDeleteDialog.dismiss();
                            }
                        });
                confirmDeleteDialog.show();
            }
        });
        return view;
    }
}
