package com.clearent.ui.payment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.clearent.Constants;
import com.clearent.payment.R;
import com.clearent.idtech.android.PublicOnReceiverListener;
import com.clearent.idtech.android.domain.CardProcessingResponse;
import com.clearent.idtech.android.domain.ClearentPaymentRequest;
import com.clearent.idtech.android.family.HasManualTokenizingSupport;
import com.clearent.idtech.android.token.domain.TransactionToken;
import com.clearent.payment.CardReaderService;
import com.clearent.payment.CreditCard;
import com.clearent.payment.ManualEntryService;
import com.clearent.payment.PostTransactionRequest;
import com.clearent.payment.ReceiptDetail;
import com.clearent.payment.ReceiptRequest;
import com.clearent.payment.SampleReceipt;
import com.clearent.payment.SampleReceiptImpl;
import com.clearent.payment.SampleTransaction;
import com.clearent.payment.SampleTransactionImpl;
import com.clearent.ui.tools.ConfigureViewModel;
import com.idtechproducts.device.Common;
import com.idtechproducts.device.ErrorCode;
import com.idtechproducts.device.ErrorCodeInfo;
import com.idtechproducts.device.ReaderInfo;
import com.idtechproducts.device.ReaderInfo.DEVICE_TYPE;
import com.idtechproducts.device.ResDataStruct;
import com.idtechproducts.device.StructConfigParameters;
import com.idtechproducts.device.bluetooth.BluetoothLEController;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

public class PaymentFragment extends Fragment implements PublicOnReceiverListener, HasManualTokenizingSupport {

    private PaymentViewModel paymentViewModel;
    private ConfigureViewModel configureViewModel;
    private boolean isBluetoothScanning = false;
    private static final int REQUEST_ENABLE_BT = 1;
    private BluetoothAdapter mBtAdapter = null;
    private AlertDialog transactionAlertDialog;
    private AlertDialog connectionDialog;
    private String info = "";
    private Handler handler = new Handler();
    private Button swipeButton;
    private Button connectButton;

    private int bleRetryCount = 0;
    private boolean isReady = false;
    private boolean btleDeviceRegistered = false;
    private String btleDeviceAddress = null;

    private String settingsApiKey = Constants.API_KEY_FOR_DEMO_ONLY;
    private String settingsPublicKey = Constants.PUBLIC_KEY;
    private Boolean settingsProdEnvironment = false;
    private Boolean settingsBluetoothReader = false;
    private Boolean settingsAudioJackReader = false;
    private Boolean enableContactless = false;
    private Boolean enable2In1Mode = false;
    private Boolean clearContactCache = false;
    private Boolean clearContactlessCache = false;

    private CardReaderService cardReaderService;
    private ManualEntryService manualEntryService;

    private Boolean runningTransaction = false;
    private Boolean connectingToBluetooth = false;
    private Boolean runningManualEntry = false;

    View root = null;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        paymentViewModel =
                ViewModelProviders.of(getActivity()).get(PaymentViewModel.class);
        configureViewModel =
                ViewModelProviders.of(getActivity()).get(ConfigureViewModel.class);
        root = inflater.inflate(R.layout.fragment_payment, container, false);

        observeConfigurationValues(root);

        bindButtons(root);

        updateReaderConnected("Reader Disconnected ❌");

        return root;
    }

    @Override
    public void onPause(){
        super.onPause();
        releaseSDK();
        updateViewModel();
    }

    private void observeConfigurationValues(final View root) {
        configureViewModel.getApiKey().observe(getActivity(), new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                settingsApiKey = s;
            }
        });
        configureViewModel.getPublicKey().observe(getActivity(), new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                settingsPublicKey = s;
            }
        });

        configureViewModel.getProdEnvironment().observe(getActivity(), new Observer<Integer>() {
            @Override
            public void onChanged(Integer onOff) {
                settingsProdEnvironment = onOff == 0 ? false : true;
            }
        });

        configureViewModel.getBluetoothReader().observe(getActivity(), new Observer<Integer>() {
            @Override
            public void onChanged(Integer onOff) {
                settingsBluetoothReader = onOff == 0 ? false : true;
            }
        });
        configureViewModel.getAudioJackReader().observe(getActivity(), new Observer<Integer>() {
            @Override
            public void onChanged(Integer onOff) {
                settingsAudioJackReader = onOff == 0 ? false : true;
            }
        });
        configureViewModel.getEnableContactless().observe(getActivity(), new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean enabled) {
                enableContactless = enabled;
            }
        });
        configureViewModel.getEnable2In1Mode().observe(getActivity(), new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean enabled) {
                enable2In1Mode = enabled;
            }
        });
        configureViewModel.getEnableContactless().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean enabled) {
                enableContactless = enabled;
            }
        });
        configureViewModel.getEnable2In1Mode().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean enabled) {
                enable2In1Mode = enabled;
            }
        });

        configureViewModel.getClearContactlessConfigurationCache().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean enabled) {
                clearContactlessCache = enabled;
            }
        });

    }

    private void updateReaderConnected(final String message) {
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                if (transactionAlertDialog != null && transactionAlertDialog.isShowing()) {
                    transactionAlertDialog.setMessage("Bluetooth disconnected. Press button on reader.");
                }
                final TextView readerConnectView = root.findViewById(R.id.readerConnected);
                readerConnectView.setText(message);
            }
        });
    }

    private void hideConnectionDialog() {
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                connectionDialog.hide();
            }
        });
    }

    private void bindButtons(View root) {
        swipeButton = (Button) root.findViewById(R.id.btn_swipeCard);
        swipeButton.setOnClickListener(new SwipeButtonListener());

        swipeButton.setEnabled(true);

        connectButton = (Button) root.findViewById(R.id.btn_connect);
        connectButton.setOnClickListener(new ConnectButtonListener());

        connectButton.setEnabled(true);

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
        if(connectingToBluetooth && connectionDialog != null && connectionDialog.isShowing()) {
            updateReaderConnected("Reader Connected \uD83D\uDC9A️");
            hideConnectionDialog();
            connectingToBluetooth = false;
            return;
        }

        if(runningTransaction) {
            return;
        }
        updateReaderConnected("Reader Ready ❤️");
        if (!isReady && transactionAlertDialog != null && transactionAlertDialog.isShowing()) {
            String amount = paymentViewModel.getPaymentAmount().getValue();
            if(amount == null || "".equals(amount)) {
                transactionAlertDialog.hide();
                Toast.makeText(getActivity(), "Amount Required", Toast.LENGTH_LONG).show();
                return;
            } else {
                handler.post(doStartTransaction);
            }
        } else if (!isReady && transactionAlertDialog != null && !transactionAlertDialog.isShowing()) {
            String amount = paymentViewModel.getPaymentAmount().getValue();
            if(amount == null || "".equals(amount)) {
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
        SampleTransaction sampleTransaction = new SampleTransactionImpl();
        PostTransactionRequest postTransactionRequest = sampleTransaction.createPostTransactionRequest(transactionToken, paymentViewModel.getPaymentAmount().getValue(), settingsApiKey);
        sampleTransaction.doSale(postTransactionRequest, this);
    }

    private void showPaymentSuccess(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(message)
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
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
            if(transactionAlertDialog != null && transactionAlertDialog.isShowing()) {
                if(runningManualEntry) {
                    transactionAlertDialog.setMessage("Card tokenized");
                } else {
                    transactionAlertDialog.setMessage("Please remove card");
                }
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
                            transactionAlertDialog.setMessage("Transaction terminated. Look for possible follow up action");
                        }
                    }
                });
                break;
            default:
                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        swipeButton.setEnabled(true);
                        if (transactionAlertDialog != null && transactionAlertDialog.isShowing()) {
                            transactionAlertDialog.setMessage(cardProcessingResponse.getDisplayMessage());
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
                transactionAlertDialog.hide();
            }
        }
    };


    public void initializeReader() {
        if (cardReaderService != null) {
            releaseSDK();
        }
        isReady = false;
        btleDeviceRegistered = false;
        isBluetoothScanning = false;
    }

    private EditText edtBTLE_Name;
    private Dialog dlgBTLE_Name;

    void promptForReaderLast5Digits() {

        if (settingsBluetoothReader) {
            Toast.makeText(getActivity(), "VP3300 Bluetooth (Bluetooth) is selected", Toast.LENGTH_SHORT).show();
            dlgBTLE_Name = new Dialog(getActivity());
            dlgBTLE_Name.setTitle("Enter Name or Address");
            dlgBTLE_Name.setCancelable(false);
            dlgBTLE_Name.setContentView(R.layout.bluetooth_device_dialog);
            Button btnBTLE_Ok = (Button) dlgBTLE_Name.findViewById(R.id.btnSetBTLE_Name_Ok);
            edtBTLE_Name = (EditText) dlgBTLE_Name.findViewById(R.id.edtBTLE_Name);
            String bleId = Constants.DEFAULT_DEVICE_SERIAL_NUMBER_SUFFIX;
            try {
                InputStream inputStream = getActivity().openFileInput("bleId.txt");

                if (inputStream != null) {
                    InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                    BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                    String receiveString = "";
                    StringBuilder stringBuilder = new StringBuilder();

                    while ((receiveString = bufferedReader.readLine()) != null) {
                        stringBuilder.append(receiveString);
                    }

                    inputStream.close();
                    bleId = stringBuilder.toString();
                }
            } catch (FileNotFoundException e) {

            } catch (IOException e) {

            }
            edtBTLE_Name.setText(bleId);
            connectingToBluetooth = true;
            btnBTLE_Ok.setOnClickListener(setBTLE_NameOnClick);
            dlgBTLE_Name.show();
            btleDeviceRegistered = false;
            isBluetoothScanning = false;
        }
    }

    private View.OnClickListener setBTLE_NameOnClick = new View.OnClickListener() {
        public void onClick(View v) {
            dlgBTLE_Name.dismiss();
            Common.setBLEDeviceName(edtBTLE_Name.getText().toString());

            try {
                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(getActivity().openFileOutput("bleId.txt", Context.MODE_PRIVATE));
                outputStreamWriter.write(edtBTLE_Name.getText().toString());
                outputStreamWriter.close();
            } catch (IOException e) {

            }

            if (!isBleSupported(getActivity())) {
                Toast.makeText(getActivity(), "Bluetooth LE is not supported\r\n", Toast.LENGTH_LONG).show();
                return;
            }

            final BluetoothManager bluetoothManager = (BluetoothManager) getActivity().getSystemService(Context.BLUETOOTH_SERVICE);
            mBtAdapter = bluetoothManager.getAdapter();

            if (mBtAdapter == null) {
                Toast.makeText(getActivity(), "Bluetooth LE is not available\r\n", Toast.LENGTH_LONG).show();
                return;
            }

            btleDeviceRegistered = false;
            isBluetoothScanning = false;
            isReady = false;

            displayConnectionPopup();
            if (!mBtAdapter.isEnabled()) {
                Log.i("CLEARENT", "Adapter");
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            } else {
                scanforDevice(true, Constants.BLE_SCAN_TIMEOUT);
            }
        }
    };

    private void displayTransactionPopup() {
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                // infoText.setText(info);
                if (transactionAlertDialog != null) {
                    if(runningManualEntry) {
                        transactionAlertDialog.setTitle("Processing card");
                        transactionAlertDialog.setMessage("");
                    } else {
                        transactionAlertDialog.setTitle("Processing payment");
                        transactionAlertDialog.setMessage("Wait for instructions...");
                    }
                    transactionAlertDialog.show();
                } else {
                    AlertDialog.Builder transactionViewBuilder = new AlertDialog.Builder(getActivity());

                    if(runningManualEntry) {
                        transactionViewBuilder.setTitle("Processing card");
                        transactionViewBuilder.setMessage("");
                    } else {
                        transactionViewBuilder.setTitle("Processing payment");
                        transactionViewBuilder.setMessage("Wait for instructions...");
                    }
                    transactionViewBuilder.setCancelable(false);
                    transactionViewBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int which) {
                            int ret = cardReaderService.device_cancelTransaction();
                            if (ret == ErrorCode.SUCCESS) {
                                //   infoText.setText("Transaction cancelled");
                            } else {
                                //    infoText.setText("Failed to cancel transaction");
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

    private void displayConnectionPopup() {
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                if (connectionDialog != null) {
                    connectionDialog.setTitle("Connecting to Bluetooth Reader");
                    connectionDialog.setMessage("Press button on reader.");
                    connectionDialog.show();
                } else {
                    AlertDialog.Builder connectionViewBuilder = new AlertDialog.Builder(getActivity());

                    connectionViewBuilder.setTitle("Connecting to Bluetooth Reader");
                    connectionViewBuilder.setMessage("Press button on reader.");
                    connectionViewBuilder.setCancelable(false);
                    connectionViewBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int which) {
                           //TODO
                        }
                    });
                    connectionDialog = connectionViewBuilder.create();
                    connectionDialog.show();
                }
            }
        });
    }

    boolean isBleSupported(Context context) {
        return BluetoothAdapter.getDefaultAdapter() != null && context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (cardReaderService.device_getDeviceType() == DEVICE_TYPE.DEVICE_VP3300_BT) {
            if (requestCode == REQUEST_ENABLE_BT) {
                if (resultCode == Activity.RESULT_OK) {
                    Toast.makeText(getActivity(), "Bluetooth has turned on, now searching for device", Toast.LENGTH_SHORT).show();
                    scanforDevice(true, Constants.BLE_SCAN_TIMEOUT);
                } else {
                    Toast.makeText(getActivity(), "Problem in Bluetooth Turning ON", Toast.LENGTH_SHORT).show();
                    handler.post(doEnableButtons);
                }
            }
        }
    }

    private void scanforDevice(final boolean enable, final long timeout) {
        if (cardReaderService.device_getDeviceType() != DEVICE_TYPE.DEVICE_VP3300_BT) {
            return;
        }

        isReady = false;
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        btleDeviceRegistered = false;
        bleRetryCount = 0;
        scanLeDevice(true, timeout);
    }

    private void scanLeDevice(final boolean enable, final long timeout) {
        if (enable) {
            handler.postDelayed(new Runnable() {
                public void run() {
                    info += "\nStopping Scan Time " + new Date().toString() + "\n";
                    mBtAdapter.getBluetoothLeScanner().stopScan(scanCallback);
                    isBluetoothScanning = false;
                    if (!cardReaderService.device_isConnected()) {
                        info += "\nTimed out trying to find bluetooth device";
                        handler.post(doUpdateStatus);
                        btleDeviceRegistered = false;
                        bleRetryCount++;
                        if (bleRetryCount <= Constants.BLE_MAX_SCAN_TRIES) {
                            if (transactionAlertDialog.isShowing()) {
                                transactionAlertDialog.setMessage("Connecting to bluetooth... " + bleRetryCount);
                            }
                            info += "\nTrying again ";
                            handler.post(doUpdateStatus);
                            scanLeDevice(true, timeout);
                        } else {
                            info += "\nFailed to connect to bluetooth device.";
                            handler.post(doUpdateStatus);
                            if (transactionAlertDialog.isShowing()) {
                                transactionAlertDialog.setMessage("Failed to connect to bluetooth. Cancel and Try again");
                            }
                        }
                    }
                }
            }, timeout);

            List<ScanFilter> scanFilters = createScanFilter();
            ScanSettings settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .setReportDelay(0)
                    .build();
            mBtAdapter.getBluetoothLeScanner().startScan(scanFilters, settings, scanCallback);
        } else {
            mBtAdapter.getBluetoothLeScanner().stopScan(scanCallback);
        }
    }

    private List<ScanFilter> createScanFilter() {
        List<ScanFilter> scanFilters = new ArrayList<>();
        ScanFilter scanFilter = null;
        String last5 = Common.getBLEDeviceName();
        if (btleDeviceAddress != null) {
            scanFilter = new ScanFilter.Builder().setDeviceAddress(btleDeviceAddress).build();
        } else if (last5 != null && !"".equals(last5)) {
            scanFilter = new ScanFilter.Builder().setDeviceName("IDTECH-VP3300-" + last5).build();
        } else {
            scanFilter = new ScanFilter.Builder().build();
        }
        scanFilters.add(scanFilter);
        return scanFilters;
    }

    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            String last5 = Common.getBLEDeviceName();
            String searchString = last5 != null && !"".equals(last5) ? "IDTECH-VP3300-" + last5 : "IDTECH";
            if (result == null || result.getDevice() == null || result.getDevice().getName() == null) {
                Log.i("SCAN", "Skipping weirdness ");
            } else if (!btleDeviceRegistered && result.getDevice().getName().contains(searchString)) {
                info += "\nDevice found during scan at Time " + new Date().toString() + ". Pass device to idtech framework to register a listener.\n";
                Log.i("SCAN", "Scan success " + result.getDevice().getName());
                BluetoothLEController.setBluetoothDevice(result.getDevice());
                btleDeviceAddress = result.getDevice().getAddress();
                String storedDeviceSerialNumberOfConfiguredReader = cardReaderService.getStoredDeviceSerialNumberOfConfiguredReader();
                if (storedDeviceSerialNumberOfConfiguredReader.contains(last5)) {
                    Log.i("WATCH", "The device we found during scanning has been configured from this device");
                }
                btleDeviceRegistered = true;
                handler.post(doRegisterListen);
            } else {
                Log.i("SCAN", "Skip " + result.getDevice().getName());
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            System.out.println("BLE// onBatchScanResults");
            for (ScanResult sr : results) {
                Log.i("ScanResult - Results", sr.toString());
                info += "\nScanResult - Results" + sr.toString() + "\n";
                handler.post(doUpdateStatus);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            System.out.println("BLE// onScanFailed");
            Log.e("Scan Failed", "Error Code: " + errorCode);
            info += "\nScan Failed. Error Code: " + errorCode + "\n";
            handler.post(doUpdateStatus);
        }
    };

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
                        transactionAlertDialog.setMessage(lines[0]);
                        String checkTransactionMessage = "Transaction successful. Transaction Id:";
                        String checkReceiptMessage = "Receipt sent successfully";
                        String checkFailedTransactionFailed = "Payment failed";
                        if (lines[0].contains(checkFailedTransactionFailed)) {
                            if (transactionAlertDialog != null && transactionAlertDialog.isShowing()) {
                                transactionAlertDialog.hide();
                            }
                            paymentViewModel.setSuccessfulTransaction(false);
                            showPaymentFailed();
                        } else if (lines[0].contains(checkTransactionMessage)) {
                            if (transactionAlertDialog != null && transactionAlertDialog.isShowing()) {
                                transactionAlertDialog.hide();
                            }
                            paymentViewModel.setSuccessfulTransaction(true);
                            showPaymentSuccess(lines[0]);
                            runSampleReceipt(lines[0]);
                        } else if (lines[0].contains(checkReceiptMessage)) {
                            Toast.makeText(getActivity(), "Sent Receipt", Toast.LENGTH_LONG).show();
                            if (transactionAlertDialog != null && transactionAlertDialog.isShowing()) {
                                transactionAlertDialog.hide();
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

        SampleReceipt sampleReceipt = new SampleReceiptImpl();
        sampleReceipt.doReceipt(receiptRequest, this);
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
                isReady = false;
                if (transactionAlertDialog != null) {
                    transactionAlertDialog.show();
                } else {
                    displayTransactionPopup();
                }
                if (cardReaderService.device_getDeviceType() == DEVICE_TYPE.DEVICE_VP3300_BT) {
                    scanforDevice(true, Constants.BLE_SCAN_TIMEOUT);
                }
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
            byte tags[] = {(byte) 0xDF, (byte) 0xEF, 0x1F, 0x02, 0x01, 0x00};

            String amount = paymentViewModel.getPaymentAmount().getValue();
            ClearentPaymentRequest clearentPaymentRequest = new ClearentPaymentRequest(Double.valueOf(amount), 0.00, 0, 60, null);

            System.out.println(paymentViewModel.getCustomerEmailAddress().getValue());

            clearentPaymentRequest.setEmailAddress(paymentViewModel.getCustomerEmailAddress().getValue());

            int ret = 0;
            if (enable2In1Mode) {
                ret = cardReaderService.emv_startTransaction(clearentPaymentRequest);
            } else {
                ret = cardReaderService.device_startTransaction(clearentPaymentRequest);
            }
            if (ret == ErrorCode.NO_CONFIG) {
                transactionAlertDialog.hide();
                info = "Reader is not configured\n";
                info += "Status: " + cardReaderService.device_getResponseCodeString(ret) + "";
                handler.post(doEnableButtons);
                handler.post(doUpdateStatus);
            } else if (ret == ErrorCode.SUCCESS || ret == ErrorCode.RETURN_CODE_OK_NEXT_COMMAND) {
                runningTransaction = true;
                transactionAlertDialog.setMessage("Insert card or try swipe...");
            } else if (cardReaderService.device_setDeviceType(DEVICE_TYPE.DEVICE_VP3300_AJ)) {
                transactionAlertDialog.setMessage("Failed to start transaction. Check for low battery amber light or reconnect reader");
                info = "Card reader is not connected\n";
                info += "Status: " + cardReaderService.device_getResponseCodeString(ret) + "";
                handler.post(doEnableButtons);
                handler.post(doUpdateStatus);
            } else if (!cardReaderService.device_isConnected() && cardReaderService.device_setDeviceType(DEVICE_TYPE.DEVICE_VP3300_BT)) {
                transactionAlertDialog.setMessage("Card reader is not connecting. Check for low battery amber light or press button to try again");
                info = "Card reader is not connected. Press button,\n";
                info += "Status: " + cardReaderService.device_getResponseCodeString(ret) + "";
                handler.post(doEnableButtons);
                handler.post(doUpdateStatus);
                btleDeviceRegistered = false;
                scanforDevice(true, Constants.BLE_SCAN_TIMEOUT);
            } else {
                transactionAlertDialog.setMessage("Failed to start transaction. Cancel and try again");
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

    public class SwipeButtonListener implements View.OnClickListener {
        public void onClick(View arg0) {

            updateViewModel();

            btleDeviceRegistered = false;
            isBluetoothScanning = false;
            runningManualEntry = false;

            if (cardReaderService == null) {
                initCardReaderService();
            }

            if (isManualCardEntry()) {
                runningManualEntry = true;
                displayTransactionPopup();
                CreditCard creditCard = manualEntryService.createCreditCard(paymentViewModel.getCardNumber().getValue(), paymentViewModel.getCardExpirationDate().getValue(), paymentViewModel.getCardCVV().getValue());
                manualEntryService.createTransactionToken(creditCard);
            } else if (cardReaderService.device_isConnected()) {
                String amount = paymentViewModel.getPaymentAmount().getValue();
                if(amount == null || "".equals(amount)) {
                    Toast.makeText(getActivity(), "Amount Required", Toast.LENGTH_LONG).show();
                } else {
                    handler.post(doSwipeProgressBar);
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

    public class ConnectButtonListener implements View.OnClickListener {
        public void onClick(View arg0) {

            btleDeviceRegistered = false;
            isBluetoothScanning = false;
            connectingToBluetooth = false;

            if (cardReaderService == null) {
                initCardReaderService();
            }

            if (!cardReaderService.device_isConnected()) {
                if (settingsBluetoothReader) {
                    promptForReaderLast5Digits();
                } else if (settingsAudioJackReader) {
                    handler.post(doRegisterListen);
                    cardReaderService.device_configurePeripheralAndConnect();
                }
            } else {
                Toast.makeText(getActivity(), "Reader is connected", Toast.LENGTH_LONG).show();
            }
        }
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
        if(!device_setDeviceTypeResponse) {
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
                    transactionAlertDialog.setMessage("Bluetooth disconnected. Press button on reader.");
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
                transactionAlertDialog.setMessage("Timed out");
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


