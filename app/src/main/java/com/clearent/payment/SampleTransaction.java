package com.clearent.payment;

import com.clearent.idtech.android.PublicOnReceiverListener;
import com.clearent.idtech.android.token.domain.TransactionToken;

public interface SampleTransaction {
    PostTransactionRequest createPostTransactionRequest(TransactionToken transactionToken, String amount, String apiKey);
    void doSale(PostTransactionRequest postTransactionRequest, PublicOnReceiverListener publicOnReceiverListener);
}