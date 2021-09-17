package com.afeka.expiryalert.logic;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.afeka.expiryalert.ItemViewFragment;
import com.afeka.expiryalert.MainFragment;
import com.afeka.expiryalert.R;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import petrov.kristiyan.colorpicker.ColorPicker;

public abstract class Functions {
    public static String defaultCategoryColor = "#EACDEF";
    private static MainFragment mainFragment;
    private static ItemViewFragment itemViewFragment;
    private static Map<String, Integer> dateAndTime;
    public static Item newItem;

    public static boolean isStringOnlyAlphabet(String str) {
        return ((!str.equals(""))
                && (str != null)
                && (str.matches("^[ a-zA-Z]*$")));
    }

    public static boolean isTextOnlyNumbers(String str) {
        return ((!str.equals(""))
                && (str != null)
                && (str.matches("^[0-9]+$")));
    }

    public static Date stringToDate(String aDate, String aFormat) {
        SimpleDateFormat simpledateformat = new SimpleDateFormat(aFormat);
        try {
            Date date = simpledateformat.parse(aDate);
            return date;
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String dateToString(Date date, String aFormat) {
        DateFormat dateFormat = new SimpleDateFormat(aFormat);
        String stringDate = dateFormat.format(date);
        return stringDate;
    }

    public static void syncItemBetweenUsers(DatabaseManager db, User currUser, Item myItem){
            for(int i = 0 ; i < currUser.getSharingList().size();i++) {
                db.loadUser(currUser.getSharingList().get(i).getId(), new DatabaseManager.FirestoreCallback() {
                    @Override
                    public void onStorageCallBack(Storage storage) { }

                    @Override
                    public void onUserCallBack(User user) {
                        db.loadStorage(user.getStorageID(), new DatabaseManager.FirestoreCallback() {
                            @Override
                            public void onStorageCallBack(Storage storage) {
                                storage.addItemToCategory(myItem.getCategory(),myItem);
                                db.saveStorage(storage);
                            }
                            @Override
                            public void onUserCallBack(User user) {  }
                        });
                    }
                });
        }
    }

    public static void syncUsersStorage(DatabaseManager db, User currUser, List<User> currUserSharingList) {
        db.loadStorage(currUser.getStorageID(), new DatabaseManager.FirestoreCallback() {
            @Override
            public void onStorageCallBack(Storage currUserStorage) {
                for(String category: currUserStorage.getCategories().keySet()) {
                    String categoryColor = currUserStorage.getCategoriesColors().get(category);
                    for(Item item: currUserStorage.getCategories().get(category)) {
                        if(!item.isPrivate()) {
                            for (User userToSync: currUserSharingList) {
                                db.loadStorage(userToSync.getStorageID(), new DatabaseManager.FirestoreCallback() {
                                    @Override
                                    public void onStorageCallBack(Storage syncedUserStorage) {
                                        syncedUserStorage.getCategoriesColors().put(category, categoryColor);
                                        syncedUserStorage.addItemToCategory(category, item);
                                        db.saveStorage(syncedUserStorage);
                                    }
                                    @Override
                                    public void onUserCallBack(User user) { }
                                });
                            }
                        }
                    }
                }
            }
            @Override
            public void onUserCallBack(User user) { }
        });
    }

    public static boolean removeUserFromSharingList(User currUser, int position) {
        DatabaseManager db = new DatabaseManager();
        currUser.getSharingList().remove(position);
        db.saveUser(currUser);
        return true;
    }

    public static boolean checkIfItemChanged(Item oldItem, Item newItem) {
        if(!(oldItem.getName().equals(newItem.getName()))||
                !(oldItem.getQuantity() == newItem.getQuantity()) ||
                !(oldItem.getDescription().equals(newItem.getDescription())) ||
                !(oldItem.getExpiryDate().equals(newItem.getExpiryDate())) ||
                !(oldItem.getReminderDate().equals(newItem.getReminderDate())) ||
                !(oldItem.isPrivate() == newItem.isPrivate()))
            return true;
        else
            return false;
    }

    public static boolean checkIfCategoryExists(Storage storage, String category) {
        if(storage.getCategories().get(category) != null)
            return true;
        return false;
    }

    public static ArrayList<String> sortCategoriesAlphabetically(Map<String, List<Item>> storage) {
        ArrayList<String> sortedKeys = new ArrayList<>(storage.keySet());
        Collections.sort(sortedKeys);
        return sortedKeys;
    }

    public static void addEditCategoryDialog(DatabaseManager db, Storage storage, String category,
                                             Activity activity, Context context, String which,
                                                MainFragment mainFragment) {
        LayoutInflater factory = LayoutInflater.from(context);
        final View categoryDialogView = factory.inflate(R.layout.add_edit_category_alert_dialog, null);
        final AlertDialog categoryDialog = new AlertDialog.Builder(context).create();
        categoryDialog.setView(categoryDialogView);
        categoryDialog.setCanceledOnTouchOutside(false);
        categoryDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        TextView header = categoryDialogView.findViewById(R.id.category_dialog_header);
        EditText editText = categoryDialogView.findViewById(R.id.category_name_edit);
        Button color_picker = categoryDialogView.findViewById(R.id.choose_color_button);
        String currentColor = storage.getCategoriesColors().get(category);

        switch (which) {
            case "Add":
                header.setText(R.string.add_category_alert_head);
                break;
            case "Edit":
                header.setText(context.getString(R.string.edit_category_alert_head, category));
                editText.setText(category);
                color_picker.setBackgroundColor(Color.parseColor(currentColor));
                break;
        }

        color_picker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final ColorPicker cp = new ColorPicker(activity);
                cp.setOnFastChooseColorListener(new ColorPicker.OnFastChooseColorListener() {
                    @Override
                    public void setOnFastChooseColorListener(int position, int color) {
                        defaultCategoryColor = "#" + Integer.toHexString(color);
                        int parsedColor = Color.parseColor(defaultCategoryColor);
                        color_picker.setBackgroundColor(parsedColor);
                        cp.dismissDialog();
                    }

                    @Override
                    public void onCancel() {
                        cp.dismissDialog();
                    }
                }).setColumns(6)
                        .show();
            }
        });

        categoryDialogView.findViewById(R.id.btn_save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String categoryName = editText.getText().toString();
                String currentColor = storage.getCategoriesColors().get(category);
                if(currentColor == null) {
                    currentColor = defaultCategoryColor;
                }
                if (!categoryName.isEmpty()) {
                    if (!Functions.checkIfCategoryExists(storage, categoryName) || !currentColor.equals(defaultCategoryColor)) {
                        if (which.equals("Add")) {
                            storage.addCategory(categoryName, defaultCategoryColor);
                        } else if (which.equals("Edit")) {
                            storage.getCategoriesColors().remove(category);
                            List<Item> categoryList = storage.getCategories().remove(category);
                            storage.getCategories().put(categoryName, categoryList);
                            storage.getCategoriesColors().put(categoryName, defaultCategoryColor);

                            for (Item item : storage.getCategories().get(categoryName)) {
                                item.setCategory(categoryName);
                            }
                        }
                        db.saveStorage(storage);

                        mainFragment.refresh(mainFragment.getView(), storage);
                    } else {
                        editText.setError("Category already Exists");
                        editText.requestFocus();
                        return;
                    }
                } else {
                    editText.setError("Name Can't be Empty!");
                    editText.requestFocus();
                    return;
                }
                categoryDialog.dismiss();
            }
        });
        categoryDialogView.findViewById(R.id.btn_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                categoryDialog.dismiss();
            }
        });
        categoryDialog.show();
    }

    public static void addEditItemDialog(DatabaseManager db, User user, Item item, int position,
                                         Storage storage, String category, Activity activity,
                                         Context context, String which, Fragment fragment) {

        LayoutInflater factory = LayoutInflater.from(context);
        final View itemDialogView = factory.inflate(R.layout.add_edit_item_to_category_popup_layout, null);
        final AlertDialog itemDialog = new AlertDialog.Builder(context).create();
        itemDialog.setView(itemDialogView);
        itemDialog.setCanceledOnTouchOutside(false);
        itemDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));


        TextView itemDialogHead = itemDialogView.findViewById(R.id.add_item_dialog_header);
        itemDialogHead.setText(context.getString(R.string.item_popup_headline, category));
        EditText itemName_editText = itemDialogView.findViewById(R.id.item_name_edit);
        EditText itemDesc_editText = itemDialogView.findViewById(R.id.item_desc_edit);
        EditText itemQty_editText = itemDialogView.findViewById(R.id.quantity_edittext);
        EditText itemExpiryDate_editText =  itemDialogView.findViewById(R.id.item_expire_date);
        EditText itemReminderDate_editText =  itemDialogView.findViewById(R.id.add_item_reminder_expire_date);
        CheckBox isPrivate_checkbox = itemDialogView.findViewById(R.id.checkbox_isPrivate);


        final Calendar myCalendar = Calendar.getInstance();
        Date current_date = myCalendar.getTime();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        SimpleDateFormat sdfFull = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        itemExpiryDate_editText.setText(sdf.format(current_date));
        itemReminderDate_editText.setText(sdfFull.format(current_date));

        switch (which) {
            case "Add":
                itemExpiryDate_editText.setText(sdf.format(current_date));
                isPrivate_checkbox.setChecked(true);
                mainFragment = (MainFragment)fragment;
                break;
            case "Edit":
                itemName_editText.setText(item.getName());
                itemDesc_editText.setText(item.getDescription());
                itemQty_editText.setText(Integer.toString(item.getQuantity()));
                isPrivate_checkbox.setChecked(item.isPrivate());
                itemExpiryDate_editText.setText(sdf.format(item.getExpiryDate()));
                itemReminderDate_editText.setText(sdfFull.format(item.getReminderDate()));

                itemViewFragment = (ItemViewFragment)fragment;
                break;
        }

        DatePickerDialog.OnDateSetListener date = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear,
                                  int dayOfMonth) {
                    myCalendar.set(Calendar.YEAR, year);
                    myCalendar.set(Calendar.MONTH, monthOfYear);
                    myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    itemExpiryDate_editText.setText(sdf.format(myCalendar.getTime()));
            }
        };

        itemExpiryDate_editText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                new DatePickerDialog(context, date, myCalendar
                        .get(Calendar.YEAR), myCalendar.get(Calendar.MONTH),
                        myCalendar.get(Calendar.DAY_OF_MONTH)).show();
            }
        });

        final Calendar myDateTimeCalendar = Calendar.getInstance();

        DatePickerDialog.OnDateSetListener dateTime = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear,
                                  int dayOfMonth) {
                // TODO Auto-generated method stub
                myDateTimeCalendar.set(Calendar.YEAR, year);
                myDateTimeCalendar.set(Calendar.MONTH, monthOfYear);
                myDateTimeCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                showHourPicker(activity, itemReminderDate_editText, myDateTimeCalendar);
            }
        };

        itemReminderDate_editText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                new DatePickerDialog(context, dateTime, myDateTimeCalendar
                        .get(Calendar.YEAR), myDateTimeCalendar.get(Calendar.MONTH),
                        myDateTimeCalendar.get(Calendar.DAY_OF_MONTH)).show();
            }
        });

        itemDialogView.findViewById(R.id.add_item_btn_save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String itemName = itemName_editText.getText().toString();
                String itemDesc = itemDesc_editText.getText().toString();
                String itemQty = itemQty_editText.getText().toString();
                String itemExpiryDate = itemExpiryDate_editText.getText().toString();
                String itemExpiryReminder = itemReminderDate_editText.getText().toString();
                boolean isPrivate = isPrivate_checkbox.isChecked();

                if (itemName.isEmpty() || itemName.length() < 2) {
                    itemName_editText.setError("Must contain at least 2 characters");
                    itemName_editText.requestFocus();
                    return;
                }

                if (!Functions.isTextOnlyNumbers(itemQty)) {
                    itemQty_editText.setError("Must be Numbers");
                    itemQty_editText.requestFocus();
                    return;
                }

                Date itemExpiryDateToSet = Functions.stringToDate(itemExpiryDate, "dd/MM/yyyy");
                Date itemReminderToSet = Functions.stringToDate(itemExpiryReminder, "dd/MM/yyyy HH:mm");
                getDateAndHour(itemExpiryReminder);

                Calendar expiryCalendar = Calendar.getInstance();
                Calendar reminderCalendar = Calendar.getInstance();
                Calendar currentCalendadr = Calendar.getInstance();
                expiryCalendar.setTime(itemExpiryDateToSet);
                currentCalendadr.setTime(current_date);

                if (itemExpiryDateToSet.before(current_date)) {
                    if(expiryCalendar.get(Calendar.YEAR) == currentCalendadr.get(Calendar.YEAR)
                            && expiryCalendar.get(Calendar.MONTH) == currentCalendadr.get(Calendar.MONTH)
                            && expiryCalendar.get(Calendar.DAY_OF_MONTH) == currentCalendadr.get(Calendar.DAY_OF_MONTH)) {
                    } else {
                        Toast.makeText(context, "Expiry Date can't be in the past",
                                Toast.LENGTH_SHORT).show();
                        itemExpiryDate_editText.requestFocus();
                        return;
                    }
                }

                reminderCalendar.setTime(itemReminderToSet);
                if (itemReminderToSet.before(current_date)) {
                    Toast.makeText(context, "Can't set reminder in the past",
                            Toast.LENGTH_SHORT).show();
                    itemReminderDate_editText.requestFocus();
                    return;
                }

                if(itemReminderToSet.after(itemExpiryDateToSet)) {
                    if((reminderCalendar.get(Calendar.YEAR) == currentCalendadr.get(Calendar.YEAR)
                            && reminderCalendar.get(Calendar.MONTH) == currentCalendadr.get(Calendar.MONTH)
                            && reminderCalendar.get(Calendar.DAY_OF_MONTH) == currentCalendadr.get(Calendar.DAY_OF_MONTH))
                    && (reminderCalendar.getTimeInMillis() > currentCalendadr.getTimeInMillis())) {
                    } else {
                        Toast.makeText(context, "Can't set reminder after expiry date",
                                Toast.LENGTH_SHORT).show();
                        itemReminderDate_editText.requestFocus();
                        return;
                    }
                }

                newItem = new Item(category,
                        itemName,
                        itemDesc,
                        Integer.parseInt(itemQty),
                        itemExpiryDateToSet,
                        itemReminderToSet,
                        isPrivate);

                getDateAndHour(itemExpiryReminder);


                if (newItem != null) {
                    if (which.equals("Edit")) {
                        newItem.setItemNotificationID(item.getItemNotificationID());
                        if (Functions.checkIfItemChanged(item, newItem)) {
                            storage.getCategories().get(category).set(position, newItem);
                            Toast.makeText(context, newItem.getName() + " has Changed!",
                                    Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(context, "Nothing has Changed!",
                                    Toast.LENGTH_LONG).show();
                        }
                    } else if (which.equals("Add")) {
                        storage.addItemToCategory(category, newItem);
                        Toast.makeText(context, newItem.getName() + " has Added!",
                                Toast.LENGTH_LONG).show();
                    }
                    db.saveStorage(storage);
                    if (!newItem.isPrivate())
                        syncItemBetweenUsers(db, user, newItem);

                    int numDaysLeft = (int)newItem.getNumberDaysLeft();
                    if (which.equals("Edit")) {
                        itemViewFragment.backToMainFragment();
                        itemViewFragment.updateNotificationMessage(newItem.getItemNotificationID(),newItem.getName(), numDaysLeft, dateAndTime.get("year"),dateAndTime.get("month"),dateAndTime.get("day"),dateAndTime.get("hour"),dateAndTime.get("minutes"));
                    } else if(which.equals("Add")) {
                        mainFragment.refresh(mainFragment.getView(), storage);
                        mainFragment.createNotificationMessage(newItem.getItemNotificationID(),newItem.getName(), numDaysLeft, dateAndTime.get("year"),dateAndTime.get("month"),dateAndTime.get("day"),dateAndTime.get("hour"),dateAndTime.get("minutes"));
                    }
                }
                itemDialog.dismiss();
            }
        });


        itemDialogView.findViewById(R.id.add_item_btn_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                itemDialog.dismiss();
            }
        });
        itemDialog.show();
    }

    public static void showHourPicker(Activity activity, EditText editText, Calendar myCalendar) {
        int hour = myCalendar.get(Calendar.HOUR_OF_DAY);
        int minute = myCalendar.get(Calendar.MINUTE);
        SimpleDateFormat sdfFull = new SimpleDateFormat("dd/MM/yyyy HH:mm");

        TimePickerDialog.OnTimeSetListener myTimeListener = new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                if (view.isShown()) {
                    myCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    myCalendar.set(Calendar.MINUTE, minute);
                    editText.setText(sdfFull.format(myCalendar.getTime()));
                }
            }
        };
        TimePickerDialog timePickerDialog = new TimePickerDialog(activity, android.R.style.Theme_Holo_Light_Dialog_NoActionBar, myTimeListener, hour, minute, true);
        timePickerDialog.setTitle("Choose hour:");
        timePickerDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        timePickerDialog.show();
    }

    public static void getDateAndHour(String dateFormat){
        dateAndTime = new HashMap<String,Integer>();
        String[] arrOfStr = dateFormat.split(" ");
        String date = arrOfStr[0];
        String time = arrOfStr[1];
        arrOfStr = date.split("/");
        dateAndTime.put("day",Integer.parseInt(arrOfStr[0]));
        dateAndTime.put("month",Integer.parseInt(arrOfStr[1]));
        dateAndTime.put("year",Integer.parseInt(arrOfStr[2]));
        arrOfStr = time.split(":");
        dateAndTime.put("hour",Integer.parseInt(arrOfStr[0]));
        dateAndTime.put("minutes",Integer.parseInt(arrOfStr[1]));
    }

}
