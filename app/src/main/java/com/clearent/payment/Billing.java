package com.clearent.payment;


import com.google.gson.annotations.SerializedName;

public class Billing {

    @SerializedName("zip")
    private String zip;

    public String getZip() {
        return zip;
    }

    public void setZip(String zip) {
        this.zip = zip;
    }

}