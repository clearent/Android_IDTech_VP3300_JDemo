package com.clearent.ui.payment;

import android.app.Application;

import com.clearent.Constants;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

public class PaymentViewModel extends AndroidViewModel {

    private MutableLiveData<String> customerEmailAddress;
    private MutableLiveData<String> readerConnected;

    private MutableLiveData<String> cardNumber;
    private MutableLiveData<String> cardExpirationDate;
    private MutableLiveData<String> cardCVV;
    private MutableLiveData<String> paymentAmount;
    private MutableLiveData<Boolean> isSuccessfulTransaction;

    public PaymentViewModel(Application app) {
        super(app);
        customerEmailAddress = new MutableLiveData<>();
        customerEmailAddress.setValue(Constants.DEFAULT_EMAIl_ADDRESS);
        readerConnected = new MutableLiveData<>();
        cardNumber = new MutableLiveData<>();
        cardExpirationDate = new MutableLiveData<>();
        cardCVV = new MutableLiveData<>();
        isSuccessfulTransaction = new MutableLiveData<>();
        isSuccessfulTransaction.setValue(false);
        paymentAmount = new MutableLiveData<>();
        paymentAmount.setValue("1.00");
    }


    public MutableLiveData<String> getCustomerEmailAddress() {
        return customerEmailAddress;
    }

    public MutableLiveData<String> getCardNumber() {
        return cardNumber;
    }

    public MutableLiveData<String> getReaderConnected() {
        return readerConnected;
    }

    public MutableLiveData<String> getCardExpirationDate() {
        return cardExpirationDate;
    }

    public MutableLiveData<String> getCardCVV() {
        return cardCVV;
    }

    public MutableLiveData<String> getPaymentAmount() {
        return paymentAmount;
    }

   public void setReaderConnected(String message) {
       readerConnected.postValue(message);
   }

    public MutableLiveData<Boolean> isSuccessfulTransaction() {
        return isSuccessfulTransaction;
    }

    public void setSuccessfulTransaction(boolean flag) {
        isSuccessfulTransaction.setValue(flag);
    }
}