package com.clearent.ui.payment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.clearent.Constants;
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
import com.clearent.reader.bluetooth.BluetoothScanListener;
import com.clearent.reader.bluetooth.BluetoothLeService;
import com.clearent.reader.bluetooth.BluetoothScanMessage;
import com.clearent.ui.settings.SettingsViewModel;
import com.idtechproducts.device.Common;
import com.idtechproducts.device.ErrorCode;
import com.idtechproducts.device.ErrorCodeInfo;
import com.idtechproducts.device.ReaderInfo;
import com.idtechproducts.device.ReaderInfo.DEVICE_TYPE;
import com.idtechproducts.device.ResDataStruct;
import com.idtechproducts.device.StructConfigParameters;
import com.idtechproducts.device.bluetooth.BluetoothLEController;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

public class PaymentFragment extends Fragment implements PublicOnReceiverListener, HasManualTokenizingSupport, BluetoothScanListener {

    private PaymentViewModel paymentViewModel;
    private SettingsViewModel settingsViewModel;
    private AlertDialog transactionAlertDialog;
    private String info = "";
    private Handler handler = new Handler();
    private Button swipeButton;

    private boolean isReady = false;

    private String settingsApiKey = Constants.API_KEY_FOR_DEMO_ONLY;
    private String settingsPublicKey = Constants.PUBLIC_KEY;
    private Boolean settingsProdEnvironment = false;
    private Boolean settingsBluetoothReader = false;
    private Boolean settingsAudioJackReader = false;
    private Boolean enableContactless = false;
    private Boolean enable2In1Mode = false;
    private Boolean clearContactCache = false;
    private Boolean clearContactlessCache = false;
    private String last5OfBluetoothReader = null;

    private CardReaderService cardReaderService;
    private ManualEntryService manualEntryService;

    private Boolean runningTransaction = false;
    private Boolean runningManualEntry = false;

    private BluetoothLeService bluetoothLeService;

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

        bluetoothLeService = new BluetoothLeService(this, Constants.BLUETOOTH_SCAN_PERIOD);

        updateReaderConnected("Reader Disconnected ❌");

        return root;
    }

    @Override
    public void onPause() {
        super.onPause();
        releaseSDK();
        updateViewModel();
    }

    private void observeConfigurationValues(final View root) {
        settingsViewModel.getApiKey().observe(getActivity(), new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                settingsApiKey = s;
            }
        });
        settingsViewModel.getPublicKey().observe(getActivity(), new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                settingsPublicKey = s;
            }
        });

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
                Common.setBLEDeviceName(s);
            }
        });

    }

    private void updateReaderConnected(final String message) {
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                if (transactionAlertDialog != null && transactionAlertDialog.isShowing()) {
                    addPopupMessage(transactionAlertDialog, "Bluetooth disconnected. Press button on reader.");
                }
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

    private void updateViewModel() {
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

    @Override
    public void isReady() {
//        if (cardReaderService.device_isConnected()) {
//            updateReaderConnected("Reader Connected \uD83D\uDC9A️");
//            if(reconnectingReader) {
//                handler.post(doStartTransaction);
//            }
//            return;
//        }

        if (runningTransaction) {
            return;
        }

        updateReaderConnected("Reader Ready ❤️");
        if (!isReady && transactionAlertDialog != null && transactionAlertDialog.isShowing()) {
            String amount = paymentViewModel.getPaymentAmount().getValue();
            if (amount == null || "".equals(amount)) {
                closePopup();
                Toast.makeText(getActivity(), "Amount Required", Toast.LENGTH_LONG).show();
                return;
            } else {
                handler.post(doStartTransaction);
            }
        } else if (!isReady && transactionAlertDialog != null && !transactionAlertDialog.isShowing()) {
            String amount = paymentViewModel.getPaymentAmount().getValue();
            if (amount == null || "".equals(amount)) {
                Toast.makeText(getActivity(), "Amount Required", Toast.LENGTH_LONG).show();
            } else {
                transactionAlertDialog.show();
                handler.post(doStartTransaction);
            }
        }

        isReady = true;

    }

    @Override
    public void successfulTransactionToken(final TransactionToken transactionToken) {
        handler.post(doSuccessUpdates);
        PostPayment postPayment = new PostPaymentImpl();
        PostTransactionRequest postTransactionRequest = postPayment.createPostTransactionRequest(transactionToken, paymentViewModel.getPaymentAmount().getValue(), settingsApiKey);
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
                } else {
                    addPopupMessage(transactionAlertDialog, "Please remove card");
                }
                addPopupMessage(transactionAlertDialog, "Running transaction");
            }
            handler.post(doUpdateStatus);
            swipeButton.setEnabled(true);
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
                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        swipeButton.setEnabled(true);
                        if (transactionAlertDialog != null && transactionAlertDialog.isShowing()) {
                            addPopupMessage(transactionAlertDialog, cardProcessingResponse.getDisplayMessage());
                        }
                    }
                });
        }
    }

    @Override
    public void handleManualEntryError(String message) {
        info += "\nFailed to get a transaction token from a manually entered card. Error - " + message;
        handler.post(doUpdateStatus);
        swipeButton.setEnabled(true);
    }

    @Override
    public void handleConfigurationErrors(String message) {
        info += "\nThe reader failed to configure. Error - " + message;
        handler.post(doUpdateStatus);
        handler.post(disablePopupWhenConfigurationFails);
    }

    private Runnable disablePopupWhenConfigurationFails = new Runnable() {
        public void run() {
            if (transactionAlertDialog != null && transactionAlertDialog.isShowing()) {
                closePopup();
            }
        }
    };


    public void initializeReader() {
        if (cardReaderService != null) {
            releaseSDK();
        }
        isReady = false;
    }

    private void displayTransactionPopup() {
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                if (transactionAlertDialog != null) {
                    if (runningManualEntry) {
                        transactionAlertDialog.setTitle("Processing card");
                        addPopupMessage(transactionAlertDialog, "");
                    } else {
                        transactionAlertDialog.setTitle("Processing payment");
                        addPopupMessage(transactionAlertDialog, "Wait for instructions...");
                    }
                    transactionAlertDialog.show();
                } else {
                    AlertDialog.Builder transactionViewBuilder = new AlertDialog.Builder(getActivity());

                    if (runningManualEntry) {
                        transactionViewBuilder.setTitle("Processing card");
                        addPopupMessage(transactionAlertDialog, "");
                    } else {
                        transactionViewBuilder.setTitle("Processing payment");
                        addPopupMessage(transactionAlertDialog, "Wait for instructions...");
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
                if(!cardReaderService.device_isConnected()) {
                    addPopupMessage(transactionAlertDialog,"Press button on reader");
                }
            }
        });
    }

    private void closePopup() {
        if (transactionAlertDialog != null) {
            transactionAlertDialog.hide();
            TextView textView = (TextView) transactionAlertDialog.findViewById(R.id.popupMessages);
            if (textView != null) {
                textView.setText("");
            }
        }
    }

    private void addPopupMessage(AlertDialog alertDialog, String message) {
        if (alertDialog != null && alertDialog.isShowing()) {
            TextView textView = (TextView) alertDialog.findViewById(R.id.popupMessages);
            if (textView == null) {
                return;
            }
            textView.append(message + "\n");
        }
    }

    public void lcdDisplay(int mode, final String[] lines, int timeout) {
        if (lines != null && lines.length > 0) {
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    if (lines[0].contains("TIME OUT")) {
                        handler.post(doEnableButtons);
                    }
                    info += "\n";
                    Log.i("WATCH1", lines[0]);
                    info += lines[0] + "\n";
                    handler.post(doUpdateStatus);
                    if (transactionAlertDialog != null && transactionAlertDialog.isShowing()) {
                        addPopupMessage(transactionAlertDialog, lines[0]);
                        String checkTransactionMessage = "Transaction successful. Transaction Id:";
                        String checkReceiptMessage = "PostReceipt sent successfully";
                        String checkFailedTransactionFailed = "Transaction failed";
                        if (lines[0].contains(checkFailedTransactionFailed)) {
                            if (transactionAlertDialog != null && transactionAlertDialog.isShowing()) {
                                closePopup();
                            }
                            paymentViewModel.setSuccessfulTransaction(false);
                            showPaymentFailed();
                        } else if (lines[0].contains(checkTransactionMessage)) {
                            if (transactionAlertDialog != null && transactionAlertDialog.isShowing()) {
                                closePopup();
                            }
                            paymentViewModel.setSuccessfulTransaction(true);
                            showPaymentSuccess(lines[0]);
                            runSampleReceipt(lines[0]);
                        } else if (lines[0].contains(checkReceiptMessage)) {
                            Toast.makeText(getActivity(), "Sent PostReceipt", Toast.LENGTH_LONG).show();
                            if (transactionAlertDialog != null && transactionAlertDialog.isShowing()) {
                                closePopup();
                            }
                        }
                    }
                }
            });
        }
    }

    private void runSampleReceipt(String line) {
        String[] parts = line.split(":");
        ReceiptRequest receiptRequest = new ReceiptRequest();
        receiptRequest.setApiKey(settingsApiKey);
        String baseUrl = Constants.BASE_URL;
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

    public void lcdDisplay(int mode, String[] lines, int timeout, byte[] languageCode, byte messageId) {
        //Clearent runs with a terminal major configuration of 5C, so no prompts. We should be able to
        //monitor all messaging using the other lcdDisplay method as well as the response and error handlers.
    }

    private int dialogId = 0;  //authenticate_dialog: 0 complete_emv_dialog: 1 language selection: 2 menu_display: 3

    public void timerDelayRemoveDialog(long time, final Dialog d) {
        new Handler().postDelayed(new Runnable() {
            public void run() {
                if (d.isShowing()) {
                    d.dismiss();
                    switch (dialogId) {
                        case 0:
                            info = "EMV Transaction Declined.  Authentication Time Out.\n";
                            break;
                        case 1:
                            info = "EMV Transaction Declined.  Complete EMV Time Out.\n";
                            break;
                        case 2:
                            info = "EMV Transaction Language Selection Time Out.\n";
                            break;
                        case 3:
                            info = "EMV Transaction Menu Selection Time Out.\n";
                            break;
                    }
                    handler.post(doUpdateStatus);
                    ResDataStruct resData = new ResDataStruct();
                    cardReaderService.emv_cancelTransaction(resData);
                    swipeButton.setEnabled(true);
                }
            }
        }, time);
    }

    public void releaseSDK() {
        if (cardReaderService != null) {
            cardReaderService.unregisterListen();
            cardReaderService.release();
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                //do nothing
            }
        }
    }

    private Runnable doUpdateStatus = new Runnable() {
        public void run() {
            //   infoText.setText(info);
        }
    };

    private Runnable doEnableButtons = new Runnable() {
        public void run() {
            swipeButton.setEnabled(true);
        }
    };

    private Runnable doSwipeProgressBar = new Runnable() {
        public void run() {
            runningTransaction = false;
            if (cardReaderService.device_isConnected()) {
                if (transactionAlertDialog != null && !transactionAlertDialog.isShowing()) {
                    transactionAlertDialog.show();
                } else {
                    displayTransactionPopup();
                }
                handler.post(doStartTransaction);
            } else {
                Toast.makeText(getActivity(), "Connect Reader First", Toast.LENGTH_LONG).show();
            }
        }
    };

    private Runnable doRegisterListen = new Runnable() {
        public void run() {
            cardReaderService.registerListen();
        }
    };

    private Runnable doStartTransaction = new Runnable() {
        @Override
        public void run() {
            String amount = paymentViewModel.getPaymentAmount().getValue();
            ClearentPaymentRequest clearentPaymentRequest = new ClearentPaymentRequest(Double.valueOf(amount), 0.00, 0, 60, null);
            clearentPaymentRequest.setEmailAddress(paymentViewModel.getCustomerEmailAddress().getValue());

            int ret = 0;
            if (enable2In1Mode) {
                ret = cardReaderService.emv_startTransaction(clearentPaymentRequest);
            } else {
                ret = cardReaderService.device_startTransaction(clearentPaymentRequest);
            }
            if (ret == ErrorCode.NO_CONFIG) {
                addPopupMessage(transactionAlertDialog, "Reader is not configured");
                handler.post(doEnableButtons);
                handler.post(doUpdateStatus);
            } else if (ret == ErrorCode.SUCCESS || ret == ErrorCode.RETURN_CODE_OK_NEXT_COMMAND) {
                runningTransaction = true;
                addPopupMessage(transactionAlertDialog, "Please tap, insert, or swipe...");
            } else if (cardReaderService.device_setDeviceType(DEVICE_TYPE.DEVICE_VP3300_AJ)) {
                addPopupMessage(transactionAlertDialog, "Failed to start transaction. Check for low battery amber light or reconnect reader");
                handler.post(doEnableButtons);
                handler.post(doUpdateStatus);
            } else if (!cardReaderService.device_isConnected() && cardReaderService.device_setDeviceType(DEVICE_TYPE.DEVICE_VP3300_BT)) {
                addPopupMessage(transactionAlertDialog, "Card reader not connected. Press button on reader and try again");
                handler.post(doEnableButtons);
            } else {
                addPopupMessage(transactionAlertDialog, "Failed to start transaction. Cancel and try again");
                info = "cannot swipe/tap card\n";
                info += "Status: " + cardReaderService.device_getResponseCodeString(ret) + "";
                handler.post(doEnableButtons);
                handler.post(doUpdateStatus);
            }
        }
    };


    @Override
    public String getPaymentsBaseUrl() {
        return Constants.BASE_URL;
    }

    @Override
    public String getPaymentsPublicKey() {
        return Constants.PUBLIC_KEY;
    }

    @Override
    public void handle(BluetoothDevice bluetoothDevice) {
        BluetoothLEController.setBluetoothDevice(bluetoothDevice);
        handler.post(doRegisterListen);
    }

    @Override
    public void handle(BluetoothScanMessage bluetoothScanMessage) {
        if(!BluetoothScanMessage.SCAN_STOPPED.equals(bluetoothScanMessage)) {
            addPopupMessage(transactionAlertDialog, bluetoothScanMessage.getDisplayMessage());
        }
    }

    public class SwipeButtonListener implements View.OnClickListener {
        public void onClick(View arg0) {

            updateViewModel();
            runningManualEntry = false;

            if (cardReaderService == null) {
                initCardReaderService();
            }

            closePopup();

            if (isManualCardEntry()) {
                runningManualEntry = true;
                displayTransactionPopup();
                CreditCard creditCard = manualEntryService.createCreditCard(paymentViewModel.getCardNumber().getValue(), paymentViewModel.getCardExpirationDate().getValue(), paymentViewModel.getCardCVV().getValue());
                manualEntryService.createTransactionToken(creditCard);
            } else if (cardReaderService.device_isConnected()) {
                String amount = paymentViewModel.getPaymentAmount().getValue();
                if (amount == null || "".equals(amount)) {
                    Toast.makeText(getActivity(), "Amount Required", Toast.LENGTH_LONG).show();
                } else {
                    handler.post(doSwipeProgressBar);
                }
            } else if (!cardReaderService.device_isConnected()) {
                displayTransactionPopup();
                if (settingsBluetoothReader) {
                    bluetoothLeService.scan(Constants.BLUETOOTH_READER_PREFIX + "-" + last5OfBluetoothReader);
                    Toast.makeText(getActivity(), "Connecting Bluetooth Reader Ending In " + last5OfBluetoothReader, Toast.LENGTH_LONG).show();
                } else if (settingsAudioJackReader) {
                    handler.post(doRegisterListen);
                    cardReaderService.device_configurePeripheralAndConnect();
                    Toast.makeText(getActivity(), "Connecting Audio Jack Reader", Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(getActivity(), "Connect Reader First", Toast.LENGTH_LONG).show();
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
        }
        String baseUrl = Constants.BASE_URL;
        if (settingsProdEnvironment) {
            baseUrl = Constants.PROD_BASE_URL;
        }

        cardReaderService = new CardReaderService(device_type, this, getContext(), baseUrl, settingsPublicKey, true);

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
        System.out.println("device connected");
    }

    public void deviceDisconnected() {
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                if (transactionAlertDialog != null && transactionAlertDialog.isShowing()) {
                    addPopupMessage(transactionAlertDialog, "Bluetooth disconnected. Press button on reader.");
                }
            }
        });
        updateReaderConnected("Reader Disconnected ❌");

        isReady = false;
    }

    public void timeout(int errorCode) {
        info += ErrorCodeInfo.getErrorCodeDescription(errorCode);
        handler.post(showTimeout);
    }

    private Runnable showTimeout = new Runnable() {
        public void run() {
            if (transactionAlertDialog != null && transactionAlertDialog.isShowing()) {
                addPopupMessage(transactionAlertDialog, "Timed out");
            }
        }
    };

    public void ICCNotifyInfo(byte[] dataNotify, String strMessage) {
        if (strMessage != null && strMessage.length() > 0) {
            String strHexResp = Common.getHexStringFromBytes(dataNotify);

            info += "ICC Notification Info: " + strMessage + "\n" + "Resp: " + strHexResp;
            handler.post(doUpdateStatus);
        }
    }

    public void msgToConnectDevice() {
        updateReaderConnected("Press Button ⚠️");
    }

    public void msgAudioVolumeAdjustFailed() {
        info += "SDK could not adjust volume...";
        handler.post(doUpdateStatus);
    }


    public void onReceiveMsgChallengeResult(int returnCode, byte[] data) {
        // Not called for UniPay Firmware update
    }


    public void LoadXMLConfigFailureInfo(int index, String strMessage) {
        info += "XML loading error...";
        handler.post(doUpdateStatus);
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

    public void msgBatteryLow() {
        Toast.makeText(getActivity(), "LOW BATTERY", Toast.LENGTH_LONG).show();
    }
}


