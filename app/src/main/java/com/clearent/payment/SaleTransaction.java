package com.clearent.payment;

import com.google.gson.annotations.SerializedName;

public class SaleTransaction {

    @SerializedName("software-type")
    private String softwareType;

    @SerializedName("software-type-version")
    private String softwareTypeVersion;

    @SerializedName("type")
    private String type = "SALE";

    @SerializedName("amount")
    private String amount;

    @SerializedName("create-token")
    private String createToken;

    @SerializedName("card-inquiry")
    private String cardInquiry;

    @SerializedName("billing")
    private Billing billing;

    public SaleTransaction(String amount) {
        this.amount = amount;
    }

    public String getSoftwareType() {
        return softwareType;
    }

    public void setSoftwareType(String softwareType) {
        this.softwareType = softwareType;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getCreateToken() {
        return createToken;
    }

    public void setCreateToken(String createToken) {
        this.createToken = createToken;
    }

    public String getSoftwareTypeVersion() {
        return softwareTypeVersion;
    }

    public void setSoftwareTypeVersion(String softwareTypeVersion) {
        this.softwareTypeVersion = softwareTypeVersion;
    }

    public Billing getBilling() {
        return billing;
    }

    public void setBilling(Billing billing) {
        this.billing = billing;
    }

    public String getCardInquiry() {
        return cardInquiry;
    }

    public void setCardInquiry(String cardInquiry) {
        this.cardInquiry = cardInquiry;
    }
}
