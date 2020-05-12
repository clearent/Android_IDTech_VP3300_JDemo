package com.clearent.ui.payment;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.clearent.Constants;
import com.clearent.idtech.android.domain.ClearentFeedback;
import com.clearent.idtech.android.domain.ClearentResponse;
import com.clearent.idtech.android.domain.FeedbackType;
import com.clearent.idtech.android.domain.ReaderInterfaceMode;
import com.clearent.idtech.android.domain.connection.AudioJack;
import com.clearent.idtech.android.domain.connection.Bluetooth;
import com.clearent.idtech.android.domain.connection.BluetoothSearchType;
import com.clearent.payment.R;
import com.clearent.idtech.android.PublicOnReceiverListener;
import com.clearent.idtech.android.domain.CardProcessingResponse;
import com.clearent.idtech.android.domain.ClearentPaymentRequest;
import com.clearent.idtech.android.family.HasManualTokenizingSupport;
import com.clearent.idtech.android.token.domain.TransactionToken;
import com.clearent.reader.CardReaderService;
import com.clearent.payment.CreditCard;
import com.clearent.payment.manual.ManualEntryService;
import com.clearent.payment.PostTransactionRequest;
import com.clearent.payment.receipt.ReceiptDetail;
import com.clearent.payment.receipt.ReceiptRequest;
import com.clearent.payment.receipt.PostReceipt;
import com.clearent.payment.receipt.PostReceiptImpl;
import com.clearent.payment.PostPayment;
import com.clearent.payment.PostPaymentImpl;
import com.clearent.ui.settings.SettingsViewModel;
import com.idtechproducts.device.Common;
import com.idtechproducts.device.ReaderInfo;
import com.idtechproducts.device.ReaderInfo.DEVICE_TYPE;
import com.idtechproducts.device.StructConfigParameters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class PaymentFragment extends Fragment implements PublicOnReceiverListener, HasManualTokenizingSupport {

    private PaymentViewModel paymentViewModel;
    private SettingsViewModel settingsViewModel;
    private AlertDialog transactionAlertDialog;
    private Handler handler = new Handler();
    private Button swipeButton;

    private Boolean settingsProdEnvironment = false;
    private Boolean settingsBluetoothReader = false;
    private Boolean settingsBluetoothReaderUsb = false;
    private Boolean settingsAudioJackReader = false;
    private Boolean enableContactless = false;
    private Boolean enable2In1Mode = false;
    private Boolean clearContactCache = false;
    private Boolean clearContactlessCache = false;
    private String last5OfBluetoothReader = null;

    private CardReaderService cardReaderService;
    private ManualEntryService manualEntryService;

    private Boolean runningManualEntry = false;

    private Boolean runningPayment = false;

    View root = null;
    ViewGroup viewGroup;
    LayoutInflater layoutInflater;



    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        this.layoutInflater = inflater;
        this.viewGroup = container;
        paymentViewModel =
                ViewModelProviders.of(getActivity()).get(PaymentViewModel.class);
        settingsViewModel =
                ViewModelProviders.of(getActivity()).get(SettingsViewModel.class);
        root = inflater.inflate(R.layout.fragment_payment, container, false);

        observeConfigurationValues(root);

        bindButtons(root);

        updateReaderConnected("Reader Disconnected ❌");

        return root;
    }

    @Override
    public void onPause() {
        super.onPause();
        releaseSDK();
        updateModelFromView();
    }

    private void observeConfigurationValues(final View root) {

        settingsViewModel.getProdEnvironment().observe(getActivity(), new Observer<Integer>() {
            @Override
            public void onChanged(Integer onOff) {
                settingsProdEnvironment = onOff == 0 ? false : true;
            }
        });

        settingsViewModel.getBluetoothReader().observe(getActivity(), new Observer<Integer>() {
            @Override
            public void onChanged(Integer onOff) {
                settingsBluetoothReader = onOff == 0 ? false : true;
            }
        });
        settingsViewModel.getBluetoothReaderUsb().observe(getActivity(), new Observer<Integer>() {
            @Override
            public void onChanged(Integer onOff) {
                settingsBluetoothReaderUsb = onOff == 0 ? false : true;
            }
        });
        settingsViewModel.getAudioJackReader().observe(getActivity(), new Observer<Integer>() {
            @Override
            public void onChanged(Integer onOff) {
                settingsAudioJackReader = onOff == 0 ? false : true;
            }
        });
        settingsViewModel.getEnableContactless().observe(getActivity(), new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean enabled) {
                enableContactless = enabled;
            }
        });
        settingsViewModel.getEnable2In1Mode().observe(getActivity(), new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean enabled) {
                enable2In1Mode = enabled;
            }
        });
        settingsViewModel.getEnableContactless().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean enabled) {
                enableContactless = enabled;
            }
        });
        settingsViewModel.getEnable2In1Mode().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean enabled) {
                enable2In1Mode = enabled;
            }
        });

        settingsViewModel.getClearContactConfigurationCache().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean enabled) {
                clearContactCache = enabled;
            }
        });

        settingsViewModel.getClearContactlessConfigurationCache().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean enabled) {
                clearContactlessCache = enabled;
            }
        });

        settingsViewModel.getLast5OfBluetoothReader().observe(this, new Observer<String>() {
            @Override
            public void onChanged(String s) {
                last5OfBluetoothReader = s;
               // Common.setBLEDeviceName(s);
            }
        });

    }

    private void updateReaderConnected(final String message) {
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                final TextView readerConnectView = root.findViewById(R.id.readerConnected);
                readerConnectView.setText(message);
            }
        });
    }

    private void bindButtons(View root) {
        swipeButton = (Button) root.findViewById(R.id.btn_swipeCard);
        swipeButton.setOnClickListener(new SwipeButtonListener());

        swipeButton.setEnabled(true);

    }

    private void updateModelFromView() {
        final TextView textAmountView = root.findViewById(R.id.textAmount);
        paymentViewModel.getPaymentAmount().setValue(textAmountView.getText().toString());

        final TextView textCustomerEmailAddressView = root.findViewById(R.id.customerEmailAddress);
        paymentViewModel.getCustomerEmailAddress().setValue(textCustomerEmailAddressView.getText().toString());

        final TextView textCardNumberView = root.findViewById(R.id.textCard);
        paymentViewModel.getCardNumber().setValue(textCardNumberView.getText().toString());

        final TextView textExpirationDateView = root.findViewById(R.id.textExpirationDate);
        paymentViewModel.getCardExpirationDate().setValue(textExpirationDateView.getText().toString());

        final TextView textCVVView = root.findViewById(R.id.textCsc);
        paymentViewModel.getCardCVV().setValue(textCVVView.getText().toString());

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {

        if (cardReaderService != null) {
            cardReaderService.unregisterListen();
        }
        if (manualEntryService == null) {
            manualEntryService = ManualEntryService.getInstance(this);
        }

        initializeReader();
        super.onActivityCreated(savedInstanceState);
    }


    @Override
    public void onDestroy() {
        if (cardReaderService != null) {
            cardReaderService.unregisterListen();
        }

        super.onDestroy();
    }

    //Version 2 isReady is deprecated
    @Deprecated
    @Override
    public void isReady() {
        //still supported but we are moving away from its usage to align our design with the one used in the iOS framework.

    }

    @Override
    public void successfulTransactionToken(final TransactionToken transactionToken) {
        runningPayment = false;
        Log.d("WATCH", "thread in successfulTransactionToken" + Thread.currentThread().getName());
        handler.post(doSuccessUpdates);
        PostPayment postPayment = new PostPaymentImpl();

        String apiKey = Constants.SB_API_KEY;
        if (settingsProdEnvironment) {
            apiKey = Constants.PROD_API_KEY;
        }

        String baseUrl = Constants.SB_BASE_URL;
        if (settingsProdEnvironment) {
            baseUrl = Constants.PROD_BASE_URL;
        }

        PostTransactionRequest postTransactionRequest = postPayment.createPostTransactionRequest(transactionToken, paymentViewModel.getPaymentAmount().getValue(), apiKey, baseUrl);
        postPayment.doSale(postTransactionRequest, this);
    }

    private void showPaymentSuccess(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(message)
                .setCancelable(false)
                .setPositiveButton("\uD83D\uDCB3 OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //do things
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();

    }

    private void showPaymentFailed() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage("Payment Failed")
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //do things
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();

    }


    private Runnable doSuccessUpdates = new Runnable() {
        public void run() {
            if (transactionAlertDialog != null && transactionAlertDialog.isShowing()) {
                if (runningManualEntry) {
                    addPopupMessage(transactionAlertDialog, "Card tokenized");
                } else if(runningPayment) {
                    addPopupMessage(transactionAlertDialog, "Running transaction");
                }
            }
        }
    };

    @Override
    public void handleCardProcessingResponse(final CardProcessingResponse cardProcessingResponse) {

        switch (cardProcessingResponse) {
            case TERMINATE:
                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        if (transactionAlertDialog != null && transactionAlertDialog.isShowing()) {
                            addPopupMessage(transactionAlertDialog, "Transaction terminated. Look for possible follow up action");
                        }
                    }
                });
                break;
            default:
        }
    }

    @Override
    public void handleManualEntryError(String message) {
        swipeButton.setEnabled(true);
    }

    @Override
    public void handleConfigurationErrors(String message) {
        handler.post(disablePopupWhenConfigurationFails);
    }

    private Runnable disablePopupWhenConfigurationFails = new Runnable() {
        public void run() {
            closePopup();
        }
    };


    public void initializeReader() {
        if (cardReaderService != null) {
            releaseSDK();
        }
    }

    private void displayTransactionPopup() {
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                if (transactionAlertDialog != null) {
                    TextView textView = (TextView) transactionAlertDialog.findViewById(R.id.popupMessages);
                    if (textView != null) {
                        textView.setText("");
                    }
                    if (runningManualEntry) {
                        transactionAlertDialog.setTitle("Processing card");
                        addPopupMessage(transactionAlertDialog, "");
                    } else {
                        transactionAlertDialog.setTitle("Processing payment");
                    }
                    transactionAlertDialog.show();
                } else {
                    AlertDialog.Builder transactionViewBuilder = new AlertDialog.Builder(getActivity());

                    if (runningManualEntry) {
                        transactionViewBuilder.setTitle("Processing card");
                        addPopupMessage(transactionAlertDialog, "");
                    } else {
                        transactionViewBuilder.setTitle("Processing payment");
                    }

                    View view = layoutInflater.inflate(R.layout.frame_swipe, viewGroup, false);
                    transactionViewBuilder.setView(view);

                    transactionViewBuilder.setCancelable(false);
                    transactionViewBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int which) {
                            cardReaderService.device_cancelTransaction();
                            TextView textView = (TextView) transactionAlertDialog.findViewById(R.id.popupMessages);
                            if (textView != null) {
                                textView.setText("");
                            }
                            handler.post(doEnableButtons);
                        }
                    });
                    transactionAlertDialog = transactionViewBuilder.create();
                    transactionAlertDialog.show();
                }
            }
        });
    }

    private void closePopup() {

        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                if (transactionAlertDialog != null) {
                    TextView textView = (TextView) transactionAlertDialog.findViewById(R.id.popupMessages);
                    if (textView != null) {
                        textView.setText("");
                    }
                    transactionAlertDialog.hide();
                }
            }
        });

    }

    private void addPopupMessage(final AlertDialog alertDialog, final String message) {

        if (transactionAlertDialog != null) {
            TextView textView = (TextView) alertDialog.findViewById(R.id.popupMessages);
            if (textView == null) {
                return;
            }
            textView.append(message + "\n");
        } else {
            Log.d("FAIL","dialog not initialized");
        }

    }

    //TODO For the demo, keeping this in place to preserve the sample transaction and receipt logic
    public void lcdDisplay(int mode, final String[] lines, int timeout) {
        if (lines != null && lines.length > 0) {
            // Log.i("CLEARENTLCD", lines[0]);
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    if (transactionAlertDialog != null && transactionAlertDialog.isShowing()) {
                        String checkTransactionMessage = "Transaction successful. Transaction Id:";
                        String checkReceiptMessage = "PostReceipt sent successfully";
                        String checkFailedTransactionFailed = "Transaction failed";
                        if (lines[0].contains(checkFailedTransactionFailed)) {
                            closePopup();
                            paymentViewModel.setSuccessfulTransaction(false);
                            showPaymentFailed();
                        } else if (lines[0].contains(checkTransactionMessage)) {

                            closePopup();
                            paymentViewModel.setSuccessfulTransaction(true);
                            showPaymentSuccess(lines[0]);
                            runSampleReceipt(lines[0]);
                        } else if (lines[0].contains(checkReceiptMessage)) {

                            Toast.makeText(getActivity(), "Sent PostReceipt", Toast.LENGTH_LONG).show();
                            closePopup();
                        }
                    }
                }
            });
        }
    }

    private void runSampleReceipt(String line) {
        String[] parts = line.split(":");
        ReceiptRequest receiptRequest = new ReceiptRequest();

        if (settingsProdEnvironment) {
            receiptRequest.setApiKey(Constants.PROD_API_KEY);
        } else {
            receiptRequest.setApiKey(Constants.SB_API_KEY);
        }

        String baseUrl = Constants.SB_BASE_URL;
        if (settingsProdEnvironment) {
            baseUrl = Constants.PROD_BASE_URL;
        }
        receiptRequest.setBaseUrl(baseUrl);
        ReceiptDetail receiptDetail = new ReceiptDetail();
        System.out.println(paymentViewModel.getCustomerEmailAddress().getValue());
        receiptDetail.setEmailAddress(paymentViewModel.getCustomerEmailAddress().getValue());
        receiptDetail.setTransactionId(parts[1]);
        receiptRequest.setReceiptDetail(receiptDetail);

        PostReceipt postReceipt = new PostReceiptImpl();
        postReceipt.doReceipt(receiptRequest, this);
    }

    @Deprecated
    public void lcdDisplay(int mode, final String[] lines, int timeout, byte[] languageCode, byte messageId) {
        //Clearent runs with a terminal major configuration of 5C, so no prompts. We should be able to
        //monitor all messaging using the other lcdDisplay method as well as the response and error handlers.
        //  Log.i("CLEARENTLCD2", lines[0]);
    }


    public void releaseSDK() {
        if (cardReaderService != null) {
            try {
                cardReaderService.unregisterListen();
                cardReaderService.release();
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                //do nothing
            }
        }
    }

    private Runnable doEnableButtons = new Runnable() {
        public void run() {
            swipeButton.setEnabled(true);
        }
    };


    ClearentResponse startTransaction() {

        runningPayment = true;
        String amount = paymentViewModel.getPaymentAmount().getValue();

        ClearentPaymentRequest clearentPaymentRequest = new ClearentPaymentRequest(Double.valueOf(amount), 0.00, 0, 60, null);

        clearentPaymentRequest.setEmailAddress(paymentViewModel.getCustomerEmailAddress().getValue());


        ReaderInterfaceMode readerInterfaceMode;
        boolean reader2In1Mode = settingsViewModel.getEnable2In1Mode().getValue();
        if (reader2In1Mode) {
            readerInterfaceMode = ReaderInterfaceMode.CLEARENT_READER_INTERFACE_2_IN_1;
        } else {
            readerInterfaceMode = ReaderInterfaceMode.CLEARENT_READER_INTERFACE_3_IN_1;
        }

        if (settingsAudioJackReader) {
            AudioJack audiojack = new AudioJack(readerInterfaceMode);
            return cardReaderService.startTransaction(clearentPaymentRequest, audiojack);
        } else {
            return cardReaderService.startTransaction(clearentPaymentRequest, getBluetooth(readerInterfaceMode));
        }

    }

    private Bluetooth getBluetooth(ReaderInterfaceMode readerInterfaceMode) {

        Bluetooth bluetooth;
        boolean connectToFirstFound = settingsViewModel.getConnectToFirstBluetooth().getValue();
        boolean searchBluetooth = settingsViewModel.getSearchBluetooth().getValue();
        String bluetoothFriendlyName = settingsViewModel.getBluetoothFriendlyName().getValue();
        String last5 = settingsViewModel.getLast5OfBluetoothReader().getValue();

        if(connectToFirstFound) {
            bluetooth = new Bluetooth(readerInterfaceMode, BluetoothSearchType.CONNECT_TO_FIRST_FOUND);
        } else if(searchBluetooth){
            bluetooth = new Bluetooth(readerInterfaceMode,BluetoothSearchType.SEARCH_ONLY);
        } else if(bluetoothFriendlyName != null) {
            bluetooth = new Bluetooth(readerInterfaceMode,BluetoothSearchType.FRIENDLY_NAME, bluetoothFriendlyName);
        } else if(last5 != null) {
            bluetooth = new Bluetooth(readerInterfaceMode,BluetoothSearchType.LAST_5_OF_DEVICE_SERIAL_NUMBER, last5);
        } else {
            bluetooth = new Bluetooth(readerInterfaceMode,BluetoothSearchType.CONNECT_TO_FIRST_FOUND);
        }
        return bluetooth;

    }

    @Override
    public String getPaymentsBaseUrl() {
        if (settingsProdEnvironment) {
            return Constants.PROD_BASE_URL;
        }
        return Constants.SB_BASE_URL;
    }

    @Override
    public String getPaymentsPublicKey() {
        if (settingsProdEnvironment) {
            return Constants.PROD_PUBLIC_KEY;
        }
        return Constants.SB_PUBLIC_KEY;
    }

    public class SwipeButtonListener implements View.OnClickListener {

        public void onClick(View arg0) {

            updateModelFromView();
            runningManualEntry = false;
            runningPayment = false;

            if (cardReaderService == null) {
                initCardReaderService();
            }

            String amount = paymentViewModel.getPaymentAmount().getValue();

            if (amount == null || "".equals(amount)) {
                Toast.makeText(getActivity(), "Amount Required", Toast.LENGTH_LONG).show();
            } else if (isManualCardEntry()) {
                runningManualEntry = true;
                displayTransactionPopup();
                CreditCard creditCard = manualEntryService.createCreditCard(paymentViewModel.getCardNumber().getValue(), paymentViewModel.getCardExpirationDate().getValue(), paymentViewModel.getCardCVV().getValue());
                manualEntryService.createTransactionToken(creditCard);
            } else {
                displayTransactionPopup();
                if (settingsBluetoothReader || settingsBluetoothReaderUsb) {
                    Toast.makeText(getActivity(), "Connecting Bluetooth Reader Ending In " + last5OfBluetoothReader, Toast.LENGTH_LONG).show();
                } else if (settingsAudioJackReader) {
                    Toast.makeText(getActivity(), "Connecting Audio Jack Reader", Toast.LENGTH_LONG).show();
                }

                startTransaction();

            }

        }
    }

    private boolean isManualCardEntry() {
        if ((paymentViewModel.getCardNumber().getValue() != null && !"".equals(paymentViewModel.getCardNumber().getValue()))) {
            return true;
        }
        return false;
    }

    private void initCardReaderService() {

        ReaderInfo.DEVICE_TYPE device_type = ReaderInfo.DEVICE_TYPE.DEVICE_VP3300_BT;

        if (settingsAudioJackReader) {
            device_type = ReaderInfo.DEVICE_TYPE.DEVICE_VP3300_AJ;
        } else if (settingsBluetoothReaderUsb) {
            device_type = DEVICE_TYPE.DEVICE_VP3300_BT_USB;
        }

        String baseUrl = Constants.SB_BASE_URL;
        String publicKey = Constants.SB_PUBLIC_KEY;
        if (settingsProdEnvironment) {
            baseUrl = Constants.PROD_BASE_URL;
            publicKey = Constants.PROD_PUBLIC_KEY;
        }

        cardReaderService = new CardReaderService(device_type, this, getContext(), baseUrl, publicKey, true);

        boolean device_setDeviceTypeResponse = cardReaderService.device_setDeviceType(device_type);
        if (!device_setDeviceTypeResponse) {
            Toast.makeText(getActivity(), "Issue setting device type", Toast.LENGTH_LONG).show();
        }
        cardReaderService.setContactlessConfiguration(false);
        cardReaderService.setContactless(enableContactless);
        cardReaderService.setAutoConfiguration(false);

        cardReaderService.addRemoteLogRequest("Android_IDTech_VP3300_JDemo", "Initialized the VP3300 For Payments");
    }

    public void deviceConnected() {
        Log.d("WATCH", "thread in deviceConnected" + Thread.currentThread().getName());
        updateReaderConnected("Reader Ready ❤️");
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                if(transactionAlertDialog != null && runningPayment) {
                    transactionAlertDialog.show();
                }
            }
        });

    }

    public void deviceDisconnected() {
        runningPayment = false;
        Log.d("WATCH", "thread in deviceDisconnected" + Thread.currentThread().getName());
        updateReaderConnected("Reader Disconnected ❌");
    }

    public void timeout(int errorCode) {
        handler.post(showTimeout);
    }

    private Runnable showTimeout = new Runnable() {
        public void run() {

            addPopupMessage(transactionAlertDialog, "Timed out");

        }
    };

    public void ICCNotifyInfo(byte[] dataNotify, String strMessage) {
        //demo does not use this
    }

    public void msgToConnectDevice() {
        updateReaderConnected("Press Button ⚠️");
    }

    public void msgAudioVolumeAdjustFailed() {
        //demo does not use this
    }


    public void onReceiveMsgChallengeResult(int returnCode, byte[] data) {
        // Not called for UniPay Firmware update
    }


    public void LoadXMLConfigFailureInfo(int index, String strMessage) {
        //demo does not use this
    }

    public void dataInOutMonitor(byte[] data, boolean isIncoming) {
        //monitor for debugging and support purposes only.
    }

    @Override
    public void autoConfigProgress(int i) {
        Log.i("WATCH", "autoConfigProgress");
    }

    @Override
    public void autoConfigCompleted(StructConfigParameters structConfigParameters) {
        Log.i("WATCH", "autoConfigCompleted");
    }

    @Override
    public void deviceConfigured() {
        Log.i("WATCH", "device is configured");

    }

    @Override
    public void bluetoothDevices(List<BluetoothDevice> list) {
        //TODO do something with bluetooth devices
    }

    @Override
    public void feedback(final ClearentFeedback clearentFeedback) {
        Log.d("WATCH", "thread in feedback" + Thread.currentThread().getName());
        getActivity().runOnUiThread(new Runnable() {
            public void run() {

        if(clearentFeedback == null) {
            Log.i("CLEARENTFEEDBACK", "null");
        } else if (transactionAlertDialog != null && transactionAlertDialog.isShowing()) {
            if (clearentFeedback.getMessage().contains("TIME OUT")) {
                handler.post(doEnableButtons);
            }
            addFeedbackToPopup(transactionAlertDialog, clearentFeedback);
        } else {
            Log.d("FAIL"," dialog not initialized");
        }
            }
        });
    }

    private void addFeedbackToPopup(final AlertDialog alertDialog, final ClearentFeedback clearentFeedback) {

        Handler handler = new Handler(Looper.getMainLooper());
        Log.d("THREADING", handler.getLooper().getThread().getName());
        handler.post(new Runnable() {
            public void run() {
                if (alertDialog != null && alertDialog.isShowing()) {
                    TextView textView = (TextView) alertDialog.findViewById(R.id.popupMessages);
                    if (textView == null) {
                        return;
                    }
                    if (clearentFeedback.getFeedbackType() == FeedbackType.FEEDBACK_USER_ACTION) {
                        textView.append(clearentFeedback.getMessage() + "\n");
                    } else if (clearentFeedback.getFeedbackType() == FeedbackType.FEEDBACK_INFO) {
                        textView.append("    " + clearentFeedback.getMessage() + "\n");
                    } else if (clearentFeedback.getFeedbackType() == FeedbackType.FEEDBACK_ERROR) {
                        textView.append(clearentFeedback.getMessage() + "\n");
                    } else {
                        textView.append("" + clearentFeedback.getMessage() + "\n");
                    }
                }
            }
        });

    }


    public void msgBatteryLow() {
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                if (transactionAlertDialog != null && transactionAlertDialog.isShowing()) {
                    addPopupMessage(transactionAlertDialog, "BATTERY IS LOW");
                }
            }
        });
        Toast.makeText(getActivity(), "LOW BATTERY", Toast.LENGTH_LONG).show();
    }

}


