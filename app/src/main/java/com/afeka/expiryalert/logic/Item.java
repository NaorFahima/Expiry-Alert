package com.afeka.expiryalert.logic;

import java.io.Serializable;
import java.util.Date;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class Item implements Serializable, Comparable<Item>{
    private String itemID;
    private int itemNotificationID;
    private String name;
    private String description;
    private int quantity;
    private Date expiryDate;
    private String category;
    private Date reminderDate;
    private boolean isPrivate;

    public Item() {

    }

    public Item(String category, String name, String description, int quantity, Date expiryDate, Date reminderDate,boolean isPrivate) {
        Random rand = new Random();
        this.itemNotificationID =  rand.nextInt(999999999);
        this.itemID = UUID.randomUUID().toString();
        this.name = name;
        this.description = description;
        this.quantity = quantity;
        this.expiryDate = expiryDate;
        this.reminderDate = reminderDate;
        this.isPrivate = isPrivate;
        this.category = category;
    }

    public long getNumberDaysLeft() {
        Date currentDate = new Date();
        long diff =  expiryDate.getTime() - currentDate.getTime();
        return TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS) + 1;
    }


    @Override
    public int compareTo(Item o) {
        if(getExpiryDate() == null  || o.getExpiryDate() == null) {
            return 0;
        }
        return getExpiryDate().compareTo(o.getExpiryDate());
    }

    @Override
    public String toString() {
        return "Item{" +
                "itemID='" + itemID + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", quantity=" + quantity +
                ", expiryDate=" + expiryDate +
                ", category='" + category + '\'' +
                ", reminderDays=" + reminderDate +
                ", isPrivate=" + isPrivate +
                '}';
    }

    public String getItemID() {
        return itemID;
    }

    public String getCategory() {
        return category;
    }
    public void setCategory(String category) {
        this.category = category;
    }

    public void setItemID(String itemID) {
        this.itemID = itemID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public Date getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(Date expiryDate) {
        this.expiryDate = expiryDate;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public void setPrivate(boolean aPrivate) {
        isPrivate = aPrivate;
    }

    public Date getReminderDate() {
        return reminderDate;
    }

    public void setReminderDate(Date reminderDate) {
        this.reminderDate = reminderDate;
    }

    public int getItemNotificationID() {
        return itemNotificationID;
    }
    public void setItemNotificationID(int itemNotificationID) {
        this.itemNotificationID = itemNotificationID;
    }
}
