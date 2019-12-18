package com.clearent.payment;


import android.util.Log;

import com.clearent.idtech.android.PublicOnReceiverListener;

public class PostTransactionTaskResponseHandler {

    private PublicOnReceiverListener publicOnReceiverListener;

    public PostTransactionTaskResponseHandler(PublicOnReceiverListener publicOnReceiverListener) {
        this.publicOnReceiverListener = publicOnReceiverListener;
    }

    public void handleResponse(ClearentTransactionResponse clearentTransactionResponse) {
        if(clearentTransactionResponse == null) {
            publicOnReceiverListener.lcdDisplay(0, new String[]{"Transaction failed"}, 0);
            return;
        }

        try {
            if(clearentTransactionResponse.getClearentErrorTransactionResponse() != null) {
                if(clearentTransactionResponse.getClearentErrorTransactionResponse().getErrorPayload().getClearentTransaction() != null) {
                    publicOnReceiverListener.lcdDisplay(0, new String[]{"Transaction failed: " + clearentTransactionResponse.getClearentErrorTransactionResponse().getErrorPayload().getClearentTransaction().getDisplayMessage()}, 0);
                    Log.i("CLEARENT", "Transaction failed");
                } else {
                    publicOnReceiverListener.lcdDisplay(0, new String[]{"Transaction failed: " + clearentTransactionResponse.getClearentErrorTransactionResponse().getErrorPayload().getClearentErrorResponse().getErrorMessage()}, 0);
                    Log.i("CLEARENT", "Transaction failed");
                }
            } else if(clearentTransactionResponse.getClearentSuccessTransactionResponse() != null) {
                String message = "Transaction successful. Transaction Id:" + clearentTransactionResponse.getClearentSuccessTransactionResponse().getClearentTransactionPayload().getClearentTransaction().getTransactionId();
                 publicOnReceiverListener.lcdDisplay(0, new String[]{message}, 0);
                Log.i("CLEARENT", "Transaction successful");
            } else {
                publicOnReceiverListener.lcdDisplay(0, new String[]{"Transaction failed"}, 0);
            }
        } catch (Exception e) {
            Log.e("CLEARENT","Failed to process clearent transaction", e);
            publicOnReceiverListener.lcdDisplay(0, new String[]{"Transaction failed"}, 0);
        }
    }

}
