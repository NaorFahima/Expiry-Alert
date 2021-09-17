package com.afeka.expiryalert.logic;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Storage implements Serializable {
    private String id;
    private Map<String, String> categoriesColors;
    private Map<String, List<Item>> categories;
    private static final String DEFAULT_COLOR = "#EACDEF";

    public Storage() {
    }

    public Storage(String id) {
        this.id = id;
        categories = new HashMap<>();
        categoriesColors = new HashMap<>();
    }

    public Storage(String id ,Map<String, List<Item>> categories) {
        this.id = id;
        this.categories = categories;
    }


    public Item addItem(String category, String name, String description, int quantity, Date expiryDate, Date reminderDate, boolean isPrivate){
            Item myItem = new Item(category, name, description, quantity, expiryDate, reminderDate, isPrivate);
            addItemToCategory(category,myItem);
            return myItem;
    }

    public boolean addItemToCategory(String name, Item item) {
        boolean checkIfChanged;
        if (item == null) {
            checkIfChanged = false;
        } else if(categories.get(name) == null){
            List<Item> items = new ArrayList<>();
            items.add(item);
            categories.put(name,items);
            categoriesColors.put(name, DEFAULT_COLOR);
            checkIfChanged = true;
        } else {
            for(int i=0; i<categories.get(name).size(); i++) {
                if (categories.get(name).get(i).getItemID().equals(item.getItemID())) {
                    categories.get(name).set(i, item);
                }
            }
            categories.get(name).add(item);
            checkIfChanged = true;
        }
        return checkIfChanged;
    }

    public boolean addCategory(String name, String color) {
        List<Item> items = new ArrayList<>();
        if(categories.get(name) == null) {
            categories.put(name, items);
            categoriesColors.put(name, color);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return "Storage{" +
                "id='" + id + '\'' +
                ", categories=" + categories +
                '}';
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Map<String, List<Item>> getCategories() {
        return categories;
    }

    public void setCategories(Map<String, List<Item>> categories) {
        this.categories = categories;
    }

    public void setItemToCategories(Map<String, List<Item>> categories) {
        this.categories = categories;
    }

    public Map<String, String> getCategoriesColors() {
        return categoriesColors;
    }

    public void setCategoriesColors(Map<String, String> categoriesColors) {
        this.categoriesColors = categoriesColors;
    }
}
