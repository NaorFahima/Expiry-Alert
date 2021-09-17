package com.afeka.expiryalert;

import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.afeka.expiryalert.Adapters.SharedUsersListAdapter;
import com.afeka.expiryalert.logic.DatabaseManager;
import com.afeka.expiryalert.logic.Functions;
import com.afeka.expiryalert.logic.Storage;
import com.afeka.expiryalert.logic.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.Gson;
import com.tomergoldst.tooltips.ToolTip;
import com.tomergoldst.tooltips.ToolTipsManager;
import org.jetbrains.annotations.NotNull;

public class SettingsFragment extends Fragment implements SharedUsersListAdapter.SharedUserListClickListener, ToolTipsManager.TipListener {

    private SharedPreferences sp;
    private FirebaseAuth mAuth;
    private RelativeLayout settingsRelativeLayout;
    private TextView emailText, emailVerifiedText, tokenText, userTokenHead, userListEmpty, userListHeadLine;
    private ImageView addUserButton, sharedUsersListHelpButton;
    private Button logoutButton, changePasswordButton, changeUsernameButton;
    private ListView sharedUsersListView;
    private SharedUsersListAdapter adapter;
    private ToolTipsManager toolTipsManager;
    private User user;
    private DatabaseManager db;
    private boolean clicked = false;

    private static final String USER_ARGS = "param1";

    public SettingsFragment() {
    }

    public static SettingsFragment newInstance(User user) {
        SettingsFragment fragment = new SettingsFragment();
        Bundle args = new Bundle();
        args.putSerializable(USER_ARGS, user);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            user = (User) getArguments().getSerializable(USER_ARGS);
        }
    }

    @Override
    public void onAttach(@NonNull @NotNull Context context) {
        super.onAttach(context);
        FragmentTransaction ft = getParentFragmentManager().beginTransaction();
        if (Build.VERSION.SDK_INT >= 26) {
            ft.setReorderingAllowed(false);
        }
        ft.detach(this).attach(this).commit();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view =  inflater.inflate(R.layout.fragment_settings, container, false);

        mAuth = FirebaseAuth.getInstance();

        db = new DatabaseManager();
        sp = getActivity().getSharedPreferences("UserLogged", 0);

        settingsRelativeLayout = view.findViewById(R.id.settings_main_relative_layout);
        logoutButton = view.findViewById(R.id.logout_button);
        changePasswordButton = view.findViewById(R.id.change_password_button);
        changeUsernameButton = view.findViewById(R.id.change_username_button);
        emailText = view.findViewById(R.id.settings_email_text);
        emailVerifiedText = view.findViewById(R.id.settings_email_verified_text);
        tokenText = view.findViewById(R.id.user_token_text);
        addUserButton = view.findViewById(R.id.add_shared_user_button);
        userTokenHead = view.findViewById(R.id.settings_token_head);
        userListEmpty = view.findViewById(R.id.user_shared_list_if_empty);
        userListHeadLine = view.findViewById(R.id.user_shared_list_headline);
        sharedUsersListView = view.findViewById(R.id.sharedUsers_list);
        sharedUsersListHelpButton = view.findViewById(R.id.sharedUsers_list_help_button);

        toolTipsManager = new ToolTipsManager(this);

        String emailVerified;
        if(mAuth.getCurrentUser().isEmailVerified())
            emailVerified = "Verified";
        else
            emailVerified = "Not Verified";

        emailText.append(" " + user.getEmail());
        emailVerifiedText.setText("(" + emailVerified + ")");
        tokenText.setText(user.getId());

        tokenText.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                ClipboardManager clipboard = (ClipboardManager) view.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                clipboard.setText(tokenText.getText());
                Toast.makeText(view.getContext(), "Token Copied",
                        Toast.LENGTH_LONG).show();
                return true;
            }
        });

        sharedUsersListHelpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!clicked) {
                    int position = ToolTip.POSITION_ABOVE;
                    int align = ToolTip.ALIGN_CENTER;
                    displayTooltip(position, align);
                    clicked = true;
                } else {
                    clicked = false;
                    toolTipsManager.dismissAll();
                }
            }
        });

        if(user.getSharingList() != null && !user.getSharingList().isEmpty()) {
            userListEmpty.setVisibility(View.GONE);
            SharedUsersListAdapter adapter =
                    new  SharedUsersListAdapter(getContext(), R.layout.sharedusers_list_layout, user, SettingsFragment.this);
            sharedUsersListView.setAdapter(adapter);
        } else {
            userListEmpty.setVisibility(View.VISIBLE);
            sharedUsersListView.setVisibility(View.GONE);
        }

        addUserButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LayoutInflater factory = LayoutInflater.from(getContext());
                final View addUserToSharingListDialogView = factory.inflate(R.layout.add_user_share_list_dialog, null);
                final AlertDialog addUserToSharingListDialog = new AlertDialog.Builder(getContext()).create();
                addUserToSharingListDialog.setView(addUserToSharingListDialogView);
                addUserToSharingListDialog.setCanceledOnTouchOutside(false);
                addUserToSharingListDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

                EditText userTokenEditText = addUserToSharingListDialogView.findViewById(R.id.add_user_to_sharing_list_dialog_edittext);

                addUserToSharingListDialogView.findViewById(R.id.add_user_to_sharing_list_dialog_btn_save).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String userToken = userTokenEditText.getText().toString();
                        if(!userToken.isEmpty()) {
                            db.loadUser(userToken, new DatabaseManager.FirestoreCallback() {
                                @Override
                                public void onStorageCallBack(Storage storage) { }

                                @Override
                                public void onUserCallBack(User userToAdd) {
                                    if(userToAdd == null) {
                                        userTokenEditText.setError("No Such User");
                                        userTokenEditText.requestFocus();
                                        return;
                                    } else {
                                        if(user.getId().equals(userToAdd.getId())) {
                                            userTokenEditText.setError("Can't add yourself");
                                            userTokenEditText.requestFocus();
                                            return;
                                        } else {
                                            for (User listUser: user.getSharingList()) {
                                                if(listUser.getId().equals(userToAdd.getId())) {
                                                    userTokenEditText.setError("User already in the list");
                                                    userTokenEditText.requestFocus();
                                                    return;
                                                }
                                            }
                                        }
                                            user.getSharingList().add(userToAdd);
                                            db.saveUser(user);
                                            saveUserToPref(user);
                                            Toast.makeText(view.getContext(), userToAdd.getEmail() + " Added!",
                                                    Toast.LENGTH_LONG).show();
                                            refresh();
                                            Functions.syncUsersStorage(db, user, user.getSharingList());
                                            addUserToSharingListDialog.dismiss();
                                        }
                                }
                            });
                        } else {
                            userTokenEditText.setError("Please insert valid token");
                            userTokenEditText.requestFocus();
                            return;
                        }
                    }
                });
                addUserToSharingListDialogView.findViewById(R.id.add_user_to_sharing_list_dialog_btn_cancel).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        addUserToSharingListDialog.dismiss();
                    }
                });
                addUserToSharingListDialog.show();
            }
        });

        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sp.edit().clear().apply();
                mAuth.signOut();
                Toast.makeText(view.getContext(), "Logout Successfully!",
                        Toast.LENGTH_LONG).show();
                startActivity(new Intent(getActivity(), StartingActivity.class));
            }
        });

        changePasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseUser loggedUser = mAuth.getCurrentUser();

                LayoutInflater factory = LayoutInflater.from(getContext());
                final View changePasswordDialogView = factory.inflate(R.layout.change_password_dialog, null);
                final AlertDialog changePasswordDialog = new AlertDialog.Builder(getContext()).create();
                changePasswordDialog.setView(changePasswordDialogView);
                changePasswordDialog.setCanceledOnTouchOutside(false);
                changePasswordDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

                EditText oldPasswordEditText = changePasswordDialogView.findViewById(R.id.change_password_dialog_oldpass_edit);
                EditText newPasswordEditText = changePasswordDialogView.findViewById(R.id.change_password_dialog_newpass_edit);
                EditText newPasswordAgainEditText = changePasswordDialogView.findViewById(R.id.change_password_dialog_newpass_again_edit);

                changePasswordDialogView.findViewById(R.id.change_password_dialog_btn_save).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        String userEmail = loggedUser.getEmail();
                        String oldPass = oldPasswordEditText.getText().toString();
                        String newPass = newPasswordEditText.getText().toString();
                        String newPassAgain = newPasswordAgainEditText.getText().toString();

                        if (oldPass.isEmpty()) {
                            oldPasswordEditText.setError("Must fill password");
                            oldPasswordEditText.requestFocus();
                            return;
                        }
                        if(newPass.isEmpty()) {
                            newPasswordEditText.setError("Must fill password");
                            newPasswordEditText.requestFocus();
                            return;
                        }
                        if(newPass.length() < 6) {
                            newPasswordEditText.setError("Must be at least 6 chars");
                            newPasswordEditText.requestFocus();
                            return;
                        }

                        if(!newPassAgain.equals(newPass)) {
                            newPasswordAgainEditText.setError("Passwords doesn't match!");
                            newPasswordAgainEditText.requestFocus();
                            return;
                        }

                        AuthCredential credential = EmailAuthProvider.getCredential(userEmail, oldPass);
                        loggedUser.reauthenticate(credential).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    loggedUser.updatePassword(newPass).addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                Toast.makeText(getContext(), "Password Changed Successfully", Toast.LENGTH_SHORT).show();
                                                changePasswordDialog.dismiss();
                                            } else {
                                                Toast.makeText(getContext(), "Something Went Wrong", Toast.LENGTH_SHORT).show();
                                                changePasswordDialog.dismiss();
                                            }

                                        }
                                    });
                                } else {
                                    oldPasswordEditText.setError("Password Incorrect");
                                    oldPasswordEditText.requestFocus();
                                    return;
                                }
                            }
                        });
                    }
                });

                changePasswordDialogView.findViewById(R.id.change_password_dialog_btn_cancel).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        changePasswordDialog.dismiss();
                    }
                });

                changePasswordDialog.show();
            }
        });

        changeUsernameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LayoutInflater factory = LayoutInflater.from(getContext());
                final View editUsernameDialogView = factory.inflate(R.layout.edit_username_alert_dialog, null);
                final AlertDialog editUsernameDialog = new AlertDialog.Builder(getContext()).create();
                editUsernameDialog.setView(editUsernameDialogView);
                editUsernameDialog.setCanceledOnTouchOutside(false);
                editUsernameDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

                TextView editUsernameHeader = editUsernameDialogView.findViewById(R.id.edit_username_dialog_header);
                editUsernameHeader.append(user.getUsername());

                EditText usernameEditText = editUsernameDialogView.findViewById(R.id.edit_username_edittext);
                usernameEditText.setText(user.getUsername());

                editUsernameDialogView.findViewById(R.id.btn_save).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String usernameText = usernameEditText.getText().toString();
                        if(usernameText.isEmpty()) {
                            usernameEditText.setError("Name Can't be Empty!");
                            usernameEditText.requestFocus();
                            return;
                        }
                        if(usernameText.equals(user.getUsername())) {
                            editUsernameDialog.dismiss();
                            return;
                        }

                        user.setUsername(usernameText);
                        db.saveUser(user);
                        db.loadUser(mAuth.getCurrentUser().getUid(), new DatabaseManager.FirestoreCallback() {
                            @Override
                            public void onStorageCallBack(Storage storage) { }
                            @Override
                            public void onUserCallBack(User userCallBack) {
                                user.setUsername(userCallBack.getUsername());
                                Gson gson = new Gson();
                                String userToSave = gson.toJson(user);
                                sp.edit().putString(mAuth.getCurrentUser().getUid(), userToSave).commit();
                                startActivity(new Intent(getActivity(), MainActivity.class));
                                editUsernameDialog.dismiss();
                                Toast.makeText(getContext(), "username has Changed!",
                                        Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                });
                editUsernameDialogView.findViewById(R.id.btn_cancel).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        editUsernameDialog.dismiss();
                    }
                });
                editUsernameDialog.show();
            }
        });

        return view;
    }

    public void refresh() {
        if(user.getSharingList().isEmpty()) {
            userListEmpty.setVisibility(View.VISIBLE);
            sharedUsersListView.setVisibility(View.GONE);
        }   else {
            userListEmpty.setVisibility(View.GONE);
            sharedUsersListView.setVisibility(View.VISIBLE);
            adapter = new SharedUsersListAdapter(getContext(), R.layout.sharedusers_list_layout, user, SettingsFragment.this);
            sharedUsersListView.setAdapter(adapter);
        }
    }

    public void saveUserToPref(User userToSave) {
        Gson gson = new Gson();
        String userJson = gson.toJson(user);
        sp.edit().putString(user.getId(), userJson).commit();
    }

    @Override
    public void isChanged() {
        refresh();
        saveUserToPref(user);
    }

    private void displayTooltip(int position, int align) {
        Spanned helpMessage = Html.fromHtml(getString(R.string.user_sharing_list_tooltip_text,
                "black","red"));
        toolTipsManager.findAndDismiss(sharedUsersListHelpButton);
        ToolTip.Builder builder = new ToolTip.Builder(getContext(), sharedUsersListHelpButton, settingsRelativeLayout, helpMessage, position);
        builder.setTextAppearance(R.style.TooltipTextAppearance);
        builder.setAlign(align);
        builder.setBackgroundColor(Color.GRAY);
        toolTipsManager.show(builder.build());

    }

    @Override
    public void onTipDismissed(View view, int anchorViewId, boolean byUser) {
        toolTipsManager.dismissAll();
    }
}
