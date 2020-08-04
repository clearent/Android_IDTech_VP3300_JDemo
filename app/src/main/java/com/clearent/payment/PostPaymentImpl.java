package com.clearent.payment;

import com.clearent.Constants;
import com.clearent.idtech.android.PublicOnReceiverListener;
import com.clearent.idtech.android.token.domain.TransactionToken;

public class PostPaymentImpl implements PostPayment {

    public PostPaymentImpl() {
    }

    public PostTransactionRequest createPostTransactionRequest(TransactionToken transactionToken, String amount, String apiKey, String baseUrl) {
        PostTransactionRequest postTransactionRequest = new PostTransactionRequest();
        postTransactionRequest.setTransactionToken(transactionToken);
        postTransactionRequest.setApiKey(apiKey);
        postTransactionRequest.setBaseUrl(baseUrl);
        SaleTransaction saleTransaction;
        if (amount == null || amount.length() == 0) {
            saleTransaction = new SaleTransaction("1.00");
        } else {
            saleTransaction = new SaleTransaction(amount);
        }
        saleTransaction.setCreateToken("true");
        saleTransaction.setCardInquiry("true");
        saleTransaction.setSoftwareType(Constants.SOFTWARE_TYPE);
        saleTransaction.setSoftwareTypeVersion(Constants.SOFTWARE_TYPE_VERSION);
        postTransactionRequest.setSaleTransaction(saleTransaction);
        return postTransactionRequest;
    }

    @Override
    public void doSale(PostTransactionRequest postTransactionRequest, PublicOnReceiverListener publicOnReceiverListener) {
        PostTransactionTaskResponseHandler postTransactionTaskResponseHandler = new PostTransactionTaskResponseHandler(publicOnReceiverListener);
        asyncSale(postTransactionRequest, postTransactionTaskResponseHandler);
    }

    void asyncSale(PostTransactionRequest postTransactionRequest, final PostTransactionTaskResponseHandler postTransactionTaskResponseHandler) {
        PostTransactionTask postTransactionTask = new PostTransactionTask(postTransactionRequest, new PostTransactionTask.AsyncResponse() {
            @Override
            public void processFinish(ClearentTransactionResponse clearentTransactionResponse) {
                postTransactionTaskResponseHandler.handleResponse(clearentTransactionResponse);
            }
        });
        postTransactionTask.execute();
    }


}
