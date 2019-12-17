package com.clearent.payment;

import com.clearent.idtech.android.PublicOnReceiverListener;

public interface SampleReceipt {
    void doReceipt(ReceiptRequest receiptRequest, PublicOnReceiverListener publicOnReceiverListener);
}