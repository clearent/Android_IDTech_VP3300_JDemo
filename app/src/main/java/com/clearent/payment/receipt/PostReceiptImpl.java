package com.clearent.payment.receipt;

import com.clearent.idtech.android.PublicOnReceiverListener;

public class PostReceiptImpl implements PostReceipt {

    public PostReceiptImpl() {
    }

    @Override
    public void doReceipt(ReceiptRequest receiptRequest, PublicOnReceiverListener publicOnReceiverListener) {
        PostReceiptTaskResponseHandler postReceiptTaskResponseHandler = new PostReceiptTaskResponseHandler(publicOnReceiverListener);
        asyncSale(receiptRequest, postReceiptTaskResponseHandler);
    }

    void asyncSale(ReceiptRequest receiptRequest, final PostReceiptTaskResponseHandler postReceiptTaskResponseHandler) {
        PostReceiptTask postReceiptTask = new PostReceiptTask(receiptRequest, new PostReceiptTask.AsyncResponse() {
            @Override
            public void processFinish(ClearentReceiptResponse clearentReceiptResponse) {
                postReceiptTaskResponseHandler.handleResponse(clearentReceiptResponse);
            }
        });
        postReceiptTask.execute();
    }


}
