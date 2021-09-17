package com.afeka.expiryalert;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.afeka.expiryalert.Adapters.CategoryAdapter;
import com.afeka.expiryalert.logic.DatabaseManager;
import com.afeka.expiryalert.logic.Functions;
import com.afeka.expiryalert.logic.Item;
import com.afeka.expiryalert.logic.Storage;
import com.afeka.expiryalert.logic.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import java.util.Calendar;
import java.util.List;

public class MainFragment extends Fragment implements
        CategoryAdapter.RecyclerViewClickListener,
        SwipeRefreshLayout.OnRefreshListener,
        ItemViewFragment.ItemChangesListener
{

    private DatabaseManager db;
    private RecyclerView rv;
    private TextView addCategoryButton, verifyEmailText, verifyEmailText2;
    private CategoryAdapter adapter;
    private FirebaseAuth mAuth;
    private User user;
    private Storage storage;
    private String lastCategoryUsed;
    private Button verifyEmailButton;
    private int lastItemPosition;
    SwipeRefreshLayout mySwipeRefreshLayout;

    private static final String USER_ARGS = "user";
    private static final String LAST_CATEGORY_USED = "last_c";
    private static final String LAST_ITEM_POS = "last_i";

    public MainFragment() {

    }

    public static MainFragment newInstance(User user, String lastCategory, int lastItem) {
        MainFragment fragment = new MainFragment();
        Bundle args = new Bundle();
        args.putString(LAST_CATEGORY_USED, lastCategory);
        args.putInt(LAST_ITEM_POS, lastItem);
        args.putSerializable(USER_ARGS, user);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            lastCategoryUsed = getArguments().getString(LAST_CATEGORY_USED);
            lastItemPosition = getArguments().getInt(LAST_ITEM_POS);
            user = (User) getArguments().getSerializable(USER_ARGS);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.fragment_main, container, false);

        mAuth = FirebaseAuth.getInstance();
        db = new DatabaseManager();
        rv = view.findViewById(R.id.storageLayout);
        storage = new Storage();

        addCategoryButton = view.findViewById(R.id.add_category_button);
        verifyEmailText = view.findViewById(R.id.emailVerificationText);
        verifyEmailText2 = view.findViewById(R.id.emailVerificationText2);
        verifyEmailButton = view.findViewById(R.id.validateEmailButton);

        mySwipeRefreshLayout = view.findViewById(R.id.swipe_refresh);
        mySwipeRefreshLayout.setOnRefreshListener(this);
        mySwipeRefreshLayout.setColorSchemeResources(R.color.main_purple_color,
                android.R.color.holo_green_dark,
                android.R.color.holo_orange_dark,
                android.R.color.holo_blue_dark);

        if(!mAuth.getCurrentUser().isEmailVerified()) {
            addCategoryButton.setVisibility(View.GONE);
            rv.setVisibility(View.GONE);
            verifyEmailButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mAuth.getCurrentUser().sendEmailVerification()
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task task) {
                                    if (task.isSuccessful()) {
                                Toast.makeText(getContext(),
                                        "Verification email sent to " + mAuth.getCurrentUser().getEmail(),
                                        Toast.LENGTH_SHORT).show();
                                    } else {
                                Toast.makeText(getContext(),
                                        "Failed to send verification email.",
                                        Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                }
            });
        } else {
            verifyEmailText.setVisibility(View.GONE);
            verifyEmailText2.setVisibility(View.GONE);
            verifyEmailButton.setVisibility(View.GONE);
            addCategoryButton.setVisibility(View.VISIBLE);
            rv.setVisibility(View.VISIBLE);
            db.loadStorage(user.getStorageID(), new DatabaseManager.FirestoreCallback() {
                @Override
                public void onStorageCallBack(Storage callbackStorage) {
                    storage.setId(callbackStorage.getId());
                    storage.setCategories(callbackStorage.getCategories());
                    storage.setCategoriesColors(callbackStorage.getCategoriesColors());

                    refresh(view, callbackStorage);
                }

                @Override
                public void onUserCallBack(User user) {
                }
            });

            addCategoryButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Functions.addEditCategoryDialog(db, storage, null,
                            getActivity(), getContext(), "Add", MainFragment.this);
                }
            });
        }
        return view;
    }

    @Override
    public void addItemButtonClicked(String category) {
       Functions.addEditItemDialog(db, user, null, 0, storage, category, getActivity(),
                getContext(),  "Add", MainFragment.this);
    }

    public void createNotificationMessage(int id , String title, int numberDayLeft,int year,int month,int day,int hourOfDay,int minute){
        Intent intent = new Intent(getActivity(),ReminderBroadcast.class);
        intent.putExtra("ID",id);
        intent.putExtra("Title",title);
        intent.putExtra("numDaysLeft",numberDayLeft);
        intent.putExtra("Type", "Create");
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getActivity(),id,intent,PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager) getActivity().getSystemService(Context.ALARM_SERVICE);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(year,month-1,day,hourOfDay,minute); // Note: Months value is MonthNumber-1 (Jan is 0, Feb is 1 and so on).

        alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),pendingIntent);
    }

    public void deleteNotificationMessage(int id) {
        Intent intent = new Intent(getActivity(), ReminderBroadcast.class);
        intent.putExtra("ID",id);
        intent.putExtra("Type", "Delete");
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getActivity(), id, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager alarmManager = (AlarmManager) getActivity().getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC_WAKEUP, Calendar.getInstance().getTimeInMillis(),pendingIntent);
    }

        @Override
    public void itemClicked(int position, String category) {
        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        Fragment itemViewFragment = ItemViewFragment.newInstance(storage.getCategories().get(category).get(position), user, storage, position, MainFragment.this);
        fragmentManager.beginTransaction()
                .replace(R.id.fragment_container, itemViewFragment)
                .addToBackStack("MainFragment")
                .commit();
    }

    @Override
    public void editCategoryButtonClicked(String category) {
        Functions.addEditCategoryDialog(db, storage, category,
                getActivity(), getContext(), "Edit", this);
    }

    @Override
    public void deleteCategoryButtonClicked(String categoryName) {
        LayoutInflater factory = LayoutInflater.from(getContext());
        final View confirmDeleteDialogView = factory.inflate(R.layout.confirm_delete_layout, null);
        final AlertDialog confirmDeleteDialog = new AlertDialog.Builder(getContext()).create();
        confirmDeleteDialog.setView(confirmDeleteDialogView);
        confirmDeleteDialog.setCanceledOnTouchOutside(false);
        confirmDeleteDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        TextView confirmDeleteHeader = confirmDeleteDialogView.findViewById(R.id.confirm_delete_dialog_header);
        confirmDeleteHeader.setText(categoryName);

        List<Item> categoryList = storage.getCategories().get(categoryName);
        if(!categoryList.isEmpty())
            confirmDeleteHeader.append("\n (This category has " + categoryList.size() + " items inside)");

        confirmDeleteDialogView.findViewById(R.id.confirm_delete_dialog_header_btn_save)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                            storage.getCategories().remove(categoryName);
                            storage.getCategoriesColors().remove(categoryName);
                            db.saveStorage(storage);
                            Toast.makeText(view.getContext(), categoryName + " Has been Removed!",
                                    Toast.LENGTH_LONG).show();
                            refresh(getView(), storage);
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

    public void refresh(View view, Storage storage) {
        adapter = new CategoryAdapter(view.getContext(), storage.getCategories(),
                storage.getCategoriesColors(), lastCategoryUsed,
                lastItemPosition, MainFragment.this);
        rv.setAdapter(adapter);
        rv.setLayoutManager(new LinearLayoutManager(view.getContext()));
        if(mySwipeRefreshLayout != null) {
            if (mySwipeRefreshLayout.isRefreshing())
                mySwipeRefreshLayout.setRefreshing(false);
        }
    }

    @Override
    public void onRefresh() {
        mAuth.getCurrentUser().reload();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if(mAuth.getCurrentUser().isEmailVerified()) {
            verifyEmailText.setVisibility(View.GONE);
            verifyEmailText2.setVisibility(View.GONE);
            verifyEmailButton.setVisibility(View.GONE);
            addCategoryButton.setVisibility(View.VISIBLE);
            rv.setVisibility(View.VISIBLE);
            db.loadStorage(user.getStorageID(), new DatabaseManager.FirestoreCallback() {
                @Override
                public void onStorageCallBack(Storage callbackStorage) {
                    storage.setId(callbackStorage.getId());
                    storage.setCategories(callbackStorage.getCategories());

                    refresh(getView(), callbackStorage);
                }

                @Override
                public void onUserCallBack(User user) {
                }
            });
        } else {
            mySwipeRefreshLayout.setRefreshing(false);
        }
    }

    @Override
    public void itemDeleted(String category, int position) {
        if(!category.isEmpty() && position >= 0) {
            deleteNotificationMessage(storage.getCategories().get(category).get(position).getItemNotificationID());
            storage.getCategories().get(category).remove(position);
            db.saveStorage(storage);
        }
    }

}