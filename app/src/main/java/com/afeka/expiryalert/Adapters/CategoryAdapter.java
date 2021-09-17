package com.afeka.expiryalert.Adapters;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Color;
import android.text.Html;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.afeka.expiryalert.R;
import com.afeka.expiryalert.logic.Functions;
import com.afeka.expiryalert.logic.Item;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class CategoryAdapter extends  RecyclerView.Adapter<CategoryAdapter.ViewHolder> {

    private Map<String, List<Item>> mCategories;
    private Map<String, String> mCategoriesColors;
    private ArrayList<String> sortedCategories;
    private SparseBooleanArray expandState = new SparseBooleanArray();
    private static RecyclerViewClickListener itemListener;
    private Context context;
    private String lastCategoryUsed;
    private int lastItemPos;

    public interface RecyclerViewClickListener {
        void addItemButtonClicked(String category);
        void itemClicked(int position, String category);
        void editCategoryButtonClicked(String category);
        void deleteCategoryButtonClicked(String category);
    }


    public CategoryAdapter(Context context, Map<String, List<Item>> mCategories, Map<String, String> mCategoriesColors, String lastCategoryUsed, int lastItemPosition,
                           RecyclerViewClickListener listener) {
        this.mCategories = mCategories;
        this.mCategoriesColors = mCategoriesColors;
        this.itemListener = listener;
        this.lastCategoryUsed = lastCategoryUsed;
        this.lastItemPos = lastItemPosition;
        this.sortedCategories = new ArrayList<>();
        this.context = context;
        for (int i = 0; i < mCategories.size(); i++) {
            expandState.append(i, false);
        }
    }

    @NonNull
    @NotNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        View contactView = inflater.inflate(R.layout.category_layout, parent, false);
        ViewHolder viewHolder = new ViewHolder(contactView);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull ViewHolder holder, int position) {
        sortedCategories = Functions.sortCategoriesAlphabetically(mCategories);
        String categoryIndex = sortedCategories.get(position);

        if(lastCategoryUsed != null && !expandState.get(getPositionByKey(sortedCategories,lastCategoryUsed))) {
            expandState.put(getPositionByKey(sortedCategories,lastCategoryUsed), true);
        }

        boolean isExpanded = expandState.get(position);

        holder.expandableLayout.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
        holder.addItemLayout.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
        holder.editCloseLayout.setVisibility(isExpanded ? View.VISIBLE : View.GONE);

        if(lastCategoryUsed != null) {
            holder.llContainer.requestFocus();
        } else {
            holder.llContainer.removeAllViews();
        }


        holder.cardViewCategories.setCardBackgroundColor(Color.parseColor(mCategoriesColors.get(categoryIndex)));

        holder.addItemButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                itemListener.addItemButtonClicked(categoryIndex);
            }
        });

        holder.editCategoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                itemListener.editCategoryButtonClicked(categoryIndex);
            }
        });

        holder.deleteCategoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                itemListener.deleteCategoryButtonClicked(categoryIndex);
            }
        });

        LayoutInflater layoutInflator = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        Collections.sort(mCategories.get(categoryIndex));

        for (int i = 0; i < mCategories.get(categoryIndex).size(); i++) {
            Item item = mCategories.get(categoryIndex).get(i);
            View view = layoutInflator.inflate(R.layout.list_item_layout, null);
            TextView itemName = view.findViewById(R.id.item_name);
            TextView itemExpirationDate = view.findViewById(R.id.item_expiration_date);
            itemName.setText(item.getName());
            int expiration = R.string.item_expires_in;

            String daysReminderColor;
            if(item.getNumberDaysLeft() <= 3) {
                daysReminderColor = "red";
                if(item.getNumberDaysLeft() <= 0) {
                    expiration = R.string.item_expired;
                }
            } else {
                daysReminderColor = "black";
            }

            itemExpirationDate.setText(Html.fromHtml(context.getString(expiration,
                    item.getNumberDaysLeft(), daysReminderColor)));

            holder.llContainer.addView(view);
            int finalI = i;
            view.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            itemListener.itemClicked(finalI, categoryIndex);
                        }
                    });
        }
            holder.buttonLayout.setRotation(expandState.get(position) ? 180f : 0f);
            holder.buttonLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    onClickButton(holder.expandableLayout, holder.buttonLayout, holder.addItemLayout, holder.editCloseLayout, position);
                }
            });

        TextView name = holder.categoryName;
        name.setText(categoryIndex);
        holder.itemsInCategory.append("(" + mCategories.get(categoryIndex).size() + "):");
    }

    @Override
    public int getItemCount() {
        return mCategories.size();
    }

    public static String getMapKeyFromIndex(Map categoryMap, int index) {

        String key = null;
        Map<String, Object> cm = categoryMap;
        int pos = 0;
        for (Map.Entry<String, Object> entry : cm.entrySet()) {
            if (index == pos) {
                key = entry.getKey();
            }
            pos++;
        }
        return key;
    }

    public int getPositionByKey(ArrayList<String> categoryList, String category) {
        int pos = 0;
        for(String key: categoryList) {
            if(key.equals(category))
                return pos;
            pos++;
        }
        return -1;
    }

    private ObjectAnimator createRotateAnimator(final View target, final float from, final float to) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(target, "rotation", from, to);
        animator.setDuration(300);
        animator.setInterpolator(new LinearInterpolator());
        return animator;
    }

    private void onClickButton(final LinearLayout expandableLayout, final RelativeLayout buttonLayout,
                               final LinearLayout addItemLayout, final LinearLayout editDelLayout, final int i) {

        if (expandableLayout.getVisibility() == View.VISIBLE) {
            createRotateAnimator(buttonLayout, 180f, 0f).start();
            expandableLayout.setVisibility(View.GONE);
            editDelLayout.setVisibility(View.GONE);
            addItemLayout.setVisibility(View.GONE);
            expandState.put(i, false);
        } else {
            createRotateAnimator(buttonLayout, 0f, 180f).start();
            expandableLayout.setVisibility(View.VISIBLE);
            editDelLayout.setVisibility(View.VISIBLE);
            addItemLayout.setVisibility(View.VISIBLE);
            expandState.put(i, true);
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        TextView categoryName, itemsInCategory;
        ImageView addItemButton, editCategoryButton, deleteCategoryButton;
        LinearLayout expandableLayout, llContainer, editCloseLayout, addItemLayout;
        RelativeLayout buttonLayout;
        CardView cardViewCategories;

        public ViewHolder(View itemView) {
            super(itemView);

            categoryName = (TextView) itemView.findViewById(R.id.category_name_textview);
            itemsInCategory = itemView.findViewById(R.id.items_in_category_header);
            expandableLayout = itemView.findViewById(R.id.expandableLayout);
            editCloseLayout = itemView.findViewById(R.id.edit_del_category_icons);
            buttonLayout = itemView.findViewById(R.id.dropdown_button);
            llContainer = itemView.findViewById(R.id.llContainer);
            cardViewCategories = itemView.findViewById(R.id.cv);
            addItemLayout = itemView.findViewById(R.id.add_item_layout);
            addItemButton = itemView.findViewById(R.id.add_item_icon);
            editCategoryButton = itemView.findViewById(R.id.edit_category_button);
            deleteCategoryButton = itemView.findViewById(R.id.delete_category_button);
        }

    }

}

