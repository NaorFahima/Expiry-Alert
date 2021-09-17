package com.afeka.expiryalert;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.afeka.expiryalert.logic.DatabaseManager;
import com.afeka.expiryalert.logic.Functions;
import com.afeka.expiryalert.logic.Item;
import com.afeka.expiryalert.logic.Storage;
import com.afeka.expiryalert.logic.User;
import org.jetbrains.annotations.NotNull;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class ItemViewFragment extends Fragment {

    private static final String STORAGE = "storage";
    private static final String ITEM = "item";
    private static final String ITEM_POSITION = "pos";
    private static final String USER = "user";


    private Item item;
    private User user;
    private Storage storage;
    private int position;
    private TextView itemName, itemCategory, itemQty, itemDesc, itemExpiryDate, itemReminder, itemIsPrivate;
    private ImageView editButton, deleteButton;
    private static ItemViewFragment.ItemChangesListener itemListener;

    public interface ItemChangesListener {
        void itemDeleted(String category, int position);
    }

    public ItemViewFragment(ItemChangesListener mListener) {
        this.itemListener = mListener;
    }

    public static ItemViewFragment newInstance(Item item, User user, Storage storage, int position, ItemChangesListener mListener) {
        ItemViewFragment fragment = new ItemViewFragment(mListener);
        Bundle args = new Bundle();
        args.putSerializable(STORAGE, storage);
        args.putSerializable(USER, user);
        args.putSerializable(ITEM, item);
        args.putInt(ITEM_POSITION, position);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            storage = (Storage) getArguments().getSerializable(STORAGE);
            user = (User) getArguments().getSerializable(USER);
            item = (Item) getArguments().getSerializable(ITEM);
            position = getArguments().getInt(ITEM_POSITION);
        }

        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                backToMainFragment();
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(this, callback);

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
        View view =  inflater.inflate(R.layout.fragment_item_view, container, false);


        itemName = view.findViewById(R.id.item_view_item_name);
        itemCategory = view.findViewById(R.id.item_view_item_category);
        itemQty = view.findViewById(R.id.item_view_item_qty);
        itemDesc = view.findViewById(R.id.item_view_item_desc);
        itemExpiryDate = view.findViewById(R.id.item_view_item_expire_date);
        itemIsPrivate = view.findViewById(R.id.item_view_item_isprivate);
        itemReminder = view.findViewById(R.id.item_view_item_remind_at);
        editButton = view.findViewById(R.id.item_view_edit_button);
        deleteButton = view.findViewById(R.id.item_view_delete_button);

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        SimpleDateFormat sdfFull = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        int expiration = R.string.item_view_expiry_status;

        String daysReminderColor;
        if(item.getNumberDaysLeft() <= 3) {
            daysReminderColor = "red";
            if(item.getNumberDaysLeft() <= 0) {
                expiration = R.string.item_view_expired_status;
            }
        } else {
            daysReminderColor = "black";
        }
        itemName.setText(item.getName());
        itemCategory.setText("(" + item.getCategory() + ")");
        itemQty.setText(Integer.toString(item.getQuantity()));
        itemDesc.setText(item.getDescription());
        itemExpiryDate.setText(sdf.format(item.getExpiryDate()) + "\n");
        itemExpiryDate.append(Html.fromHtml(getString(expiration,
                item.getNumberDaysLeft(), daysReminderColor)));

        itemReminder.setText(sdfFull.format(item.getReminderDate()));

        if(item.isPrivate())
            itemIsPrivate.setText(R.string.item_view_is_private_text);
        else
            itemIsPrivate.setText(R.string.item_view_is_not_private_text);


        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LayoutInflater factory = LayoutInflater.from(getContext());
                final View confirmDeleteDialogView = factory.inflate(R.layout.confirm_delete_layout, null);
                final AlertDialog confirmDeleteDialog = new AlertDialog.Builder(getContext()).create();
                confirmDeleteDialog.setView(confirmDeleteDialogView);
                confirmDeleteDialog.setCanceledOnTouchOutside(false);
                confirmDeleteDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

                TextView confirmDeleteHeader = confirmDeleteDialogView.findViewById(R.id.confirm_delete_dialog_header);
                confirmDeleteHeader.setText(item.getName());

                confirmDeleteDialogView.findViewById(R.id.confirm_delete_dialog_header_btn_save)
                        .setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                itemListener.itemDeleted(item.getCategory(), position);
                                getActivity().onBackPressed();
                                Toast.makeText(view.getContext(), item.getName() + " Has been Removed!",
                                        Toast.LENGTH_LONG).show();
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

        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Functions.addEditItemDialog(new DatabaseManager(), user, item, position, storage, item.getCategory(), getActivity(),
                        getContext(), "Edit", ItemViewFragment.this);
            }
        });
        return view;
    }

    public void updateNotificationMessage(int id , String title , int numberDayLeft,int year,int month,int day,int hourOfDay,int minute){
        Intent intent = new Intent(getActivity(),ReminderBroadcast.class);
        intent.putExtra("ID",id);
        intent.putExtra("Title",title);
        intent.putExtra("numDaysLeft",numberDayLeft);
        intent.putExtra("Type", "Update");
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getActivity(),id,intent,PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager) getActivity().getSystemService(Context.ALARM_SERVICE);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(year,month-1,day,hourOfDay,minute); // Note: Months value is MonthNumber-1 (Jan is 0, Feb is 1 and so on).

        alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),pendingIntent);
    }

    public void backToMainFragment() {
        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        Fragment mainFragment = MainFragment.newInstance(user, item.getCategory(), position);
        fragmentManager.beginTransaction()
                .replace(R.id.fragment_container, mainFragment)
                .addToBackStack("MainFragment")
                .commit();
    }
}