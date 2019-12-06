package com.clearent;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.clearent.idtech.android.domain.CardProcessingResponse;
import com.clearent.idtech.android.domain.ClearentPaymentRequest;
import com.clearent.idtech.android.family.DeviceFactory;
import com.clearent.idtech.android.PublicOnReceiverListener;
import com.clearent.idtech.android.family.HasManualTokenizingSupport;
import com.clearent.idtech.android.family.reader.VP3300;
import com.clearent.idtech.android.token.domain.TransactionToken;
import com.clearent.idtech.android.token.manual.ManualCardTokenizer;
import com.clearent.idtech.android.token.manual.ManualCardTokenizerImpl;
import com.clearent.sample.CreditCard;
import com.clearent.sample.PostTransactionRequest;
import com.clearent.sample.ReceiptDetail;
import com.clearent.sample.ReceiptRequest;
import com.clearent.sample.SaleTransaction;
import com.clearent.sample.SampleReceipt;
import com.clearent.sample.SampleReceiptImpl;
import com.clearent.sample.SampleTransaction;
import com.clearent.sample.SampleTransactionImpl;
import com.idtechproducts.device.Common;
import com.idtechproducts.device.ErrorCode;
import com.idtechproducts.device.ErrorCodeInfo;
import com.idtechproducts.device.ReaderInfo;
import com.idtechproducts.device.ReaderInfo.DEVICE_TYPE;
import com.idtechproducts.device.ResDataStruct;
import com.idtechproducts.device.StructConfigParameters;
import com.idtechproducts.device.audiojack.tools.FirmwareUpdateTool;
import com.idtechproducts.device.audiojack.tools.FirmwareUpdateToolMsg;
import com.idtechproducts.device.bluetooth.BluetoothLEController;
import com.idtechproducts.device.sdkdemo.ApplicationContext3In1;
import com.idtechproducts.device.sdkdemo.ApplicationContextContact;
import com.idtechproducts.device.sdkdemo.R;

import android.annotation.SuppressLint;
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
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class PaymentFragment extends ActionBarActivity {

    private SdkDemoFragment mainView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo);

        if (savedInstanceState == null) {
            mainView = new SdkDemoFragment();
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, mainView).commit();
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public void onBackPressed() {
    }

    // Inflate the menu items to the action bar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_switch_reader_type:
                mainView.openReaderSelectDialog();
                break;
            case R.id.action_exit_app:
                mainView.releaseSDK();
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("ValidFragment")
    public class SdkDemoFragment extends Fragment implements PublicOnReceiverListener, HasManualTokenizingSupport, FirmwareUpdateToolMsg {
        private final long BLE_ScanTimeout = 10000; //in milliseconds
        private final long BLE_Max_Scan_Retries = 3; //in milliseconds

        private boolean isBluetoothScanning = false;

        private VP3300 device;
        private FirmwareUpdateTool fwTool;
        private ManualCardTokenizer manualCardTokenizer;
        private static final int REQUEST_ENABLE_BT = 1;
        private long totalEMVTime;
        private boolean calcLRC = true;

        private BluetoothAdapter mBtAdapter = null;

        private TextView status;
        private TextView infoText;
        private EditText textAmount;
        private EditText textCard;
        private EditText textExpirationDate;
        private EditText textCsc;

        private View rootView;
        private LayoutInflater layoutInflater;
        private ViewGroup viewGroup;
        private AlertDialog transactionAlertDialog;

        private String lastTransactionToken;
        private String info = "";
        private String detail = "";
        private Handler handler = new Handler();

        private StructConfigParameters config = null;
        private EditText edtSelection;

        private boolean isFwInitDone = false;

        private Button swipeButton;
        private Button manualButton;
        private final int emvTimeout = 60;

        private int bleRetryCount = 0;

        private boolean isReady = false;

        private ApplicationContextContact applicationContextContact;
        private ApplicationContext3In1 applicationContext3In1;

        private boolean applyClearentConfiguration = false;

        private boolean btleDeviceRegistered = false;
        private String btleDeviceAddress = null;


        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            layoutInflater = inflater;
            viewGroup = container;
            rootView = inflater.inflate(R.layout.fragment_main, container, false);

            status = (TextView) rootView.findViewById(R.id.status_text);
            status.setText("Disconnected");

            infoText = (TextView) rootView.findViewById(R.id.text_area_top);
            infoText.setVerticalScrollBarEnabled(true);

            swipeButton = (Button) rootView.findViewById(R.id.btn_swipeCard);
            swipeButton.setOnClickListener(new SwipeButtonListener());

            manualButton = (Button) rootView.findViewById(R.id.btn_manualCard);
            manualButton.setOnClickListener(new ManualButtonListener());
            manualButton.setEnabled(true);

            swipeButton.setEnabled(true);

            return rootView;
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {

            if (device != null) {
                device.unregisterListen();
            }

            initializeReader();
            openReaderSelectDialog();

            super.onActivityCreated(savedInstanceState);
        }


        @Override
        public void onDestroy() {
            if (device != null) {
                device.unregisterListen();
            }

            super.onDestroy();
        }

        @Override
        public void isReady() {
            applyClearentConfiguration = false;
            info += "\nCard reader is ready.\n";
            handler.post(doUpdateStatus);

            if (!isReady && transactionAlertDialog != null && transactionAlertDialog.isShowing()) {
                handler.post(doStartTransaction);
            } else if (!isReady && transactionAlertDialog != null && !transactionAlertDialog.isShowing()) {
                transactionAlertDialog.show();
                handler.post(doStartTransaction);
            }

            isReady = true;

        }

        @Override
        public void successfulTransactionToken(final TransactionToken transactionToken) {
            lastTransactionToken = transactionToken.getTransactionToken();
            handler.post(doSuccessUpdates);
            runSampleTransaction(transactionToken);
        }

        private Runnable doSuccessUpdates = new Runnable() {
            public void run() {
                info += "Please remove card\n";
                transactionAlertDialog.setMessage("Please remove card");
                info += "Card is now represented by a transaction token: " + lastTransactionToken + "\n";
                handler.post(doUpdateStatus);
                manualButton.setEnabled(true);
                swipeButton.setEnabled(true);
            }
        };

        @Override
        public void handleCardProcessingResponse(CardProcessingResponse cardProcessingResponse) {
            switch (cardProcessingResponse) {
                case TERMINATE:
                    getActivity().runOnUiThread(new Runnable() {
                        public void run() {
                            swipeButton.setEnabled(true);
                            if (transactionAlertDialog != null && transactionAlertDialog.isShowing()) {
                                transactionAlertDialog.hide();
                            }
                        }
                    });
                    break;
                case USE_MAGSTRIPE:
                    getActivity().runOnUiThread(new Runnable() {
                        public void run() {
                            if (transactionAlertDialog != null && transactionAlertDialog.isShowing()) {
                                transactionAlertDialog.setMessage("USE MAGSTRIPE");
                            }
                        }
                    });
                    break;
                case USE_CHIP_READER:
                    info += "USE CHIP READER\n";
                    handler.post(doUpdateStatus);
                    getActivity().runOnUiThread(new Runnable() {
                        public void run() {
                            swipeButton.setEnabled(true);
                            if (transactionAlertDialog != null && transactionAlertDialog.isShowing()) {
                                transactionAlertDialog.hide();
                            }
                        }
                    });
                    break;
                case REMOVE_CARD_AND_TRY_SWIPE:
                    info += "Remove card\n";
                    handler.post(doUpdateStatus);
                    break;
                case NONTECHNICAL_FALLBACK_SWIPE_CARD:
                    info += "Please swipe card\n";
                    handler.post(doUpdateStatus);
                    break;
                case READER_NOT_CONFIGURED:
                    info += "Reader is not configured and will not allow transaction processing\n";
                    handler.post(doUpdateStatus);
                    break;
                default:
                    info += "Card processing error: " + cardProcessingResponse.getDisplayMessage() + "\n";
                    handler.post(doUpdateStatus);
                    getActivity().runOnUiThread(new Runnable() {
                        public void run() {
                            swipeButton.setEnabled(true);
                            if (transactionAlertDialog != null && transactionAlertDialog.isShowing()) {
                                transactionAlertDialog.hide();
                            }
                        }
                    });
            }
        }

        @Override
        public void handleManualEntryError(String message) {
            info += "\nFailed to get a transaction token from a manually entered card. Error - " + message;
            handler.post(doUpdateStatus);
            manualButton.setEnabled(true);
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

        private void runSampleTransaction(TransactionToken transactionToken) {
            SampleTransaction sampleTransaction = new SampleTransactionImpl();
            PostTransactionRequest postTransactionRequest = new PostTransactionRequest();
            postTransactionRequest.setTransactionToken(transactionToken);
            postTransactionRequest.setApiKey(Constants.API_KEY_FOR_DEMO_ONLY);
            postTransactionRequest.setBaseUrl(Constants.BASE_URL);
            SaleTransaction saleTransaction;
            if (textAmount == null || textAmount.getText().toString() == null || textAmount.getText().toString().length() == 0) {
                saleTransaction = new SaleTransaction("1.00");
            } else {
                saleTransaction = new SaleTransaction(textAmount.getText().toString());
            }
            saleTransaction.setSoftwareType(Constants.SOFTWARE_TYPE);
            saleTransaction.setSoftwareTypeVersion(Constants.SOFTWARE_TYPE_VERSION);
            postTransactionRequest.setSaleTransaction(saleTransaction);
            sampleTransaction.doSale(postTransactionRequest, this);
        }

        public void initializeReader() {

            if (device != null) {
                releaseSDK();
            }
            isReady = false;
            applyClearentConfiguration = false;
            btleDeviceRegistered = false;
            isBluetoothScanning = false;
            manualCardTokenizer = new ManualCardTokenizerImpl(this);

            //Contact example
            //Gather the context needed to get a device object representing the card reader.
            applicationContextContact = new ApplicationContextContact(ReaderInfo.DEVICE_TYPE.DEVICE_VP3300_AJ, this, getActivity(), getPaymentsBaseUrl(), getPaymentsPublicKey(), null);
            //demoApplicationContext = new DemoApplicationContext(DEVICE_TYPE.DEVICE_VP3300_USB, this, getActivity(), getPaymentsBaseUrl(), getPaymentsPublicKey(), null);
            device = DeviceFactory.getVP3300(applicationContextContact);

            //Enabling Contactless example
            //Gather the context needed to get a device object representing the card reader.
            //applicationContext3In1 = new ApplicationContext3In1(ReaderInfo.DEVICE_TYPE.DEVICE_VP3300_AJ, this, getActivity(), getPaymentsBaseUrl(), getPaymentsPublicKey(), null);
            //device = DeviceFactory.getVP3300(applicationContext3In1);

            //Enable verbose logging only when instructed to by support.
            device.log_setVerboseLoggingEnable(true);
            device.log_setSaveLogEnable(true);

            fwTool = new FirmwareUpdateTool(this, getActivity());

            Toast.makeText(getActivity(), "get started", Toast.LENGTH_LONG).show();
            displaySdkInfo();

            device.addRemoteLogRequest("Android_IDTech_VP3300_Demo", "Initialized the VP3300");
        }

        private EditText edtBTLE_Name;
        private Dialog dlgBTLE_Name;

        void openReaderSelectDialog() {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());


            if (device.device_getDeviceType() == DEVICE_TYPE.DEVICE_VP3300_BT && device.device_isConnected()) {
                device.unregisterListen();
            }

            device.setAutoConfiguration(false);
            View checkBoxView = View.inflate(getActivity(), R.layout.auto_configure_checkbox, null);
            CheckBox checkBox = (CheckBox) checkBoxView.findViewById(R.id.checkboxAutoConfigure);
            checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    device.setAutoConfiguration(isChecked ? true : false);
                }
            });

            CheckBox clearReaderCacheCheckBox = (CheckBox) checkBoxView.findViewById(R.id.clearReaderCache);
            clearReaderCacheCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    device.setReaderConfiguredSharedPreference(isChecked ? false : true);
                }
            });

            device.setContactlessConfiguration(false);
            CheckBox checkBox2 = (CheckBox) checkBoxView.findViewById(R.id.checkboxContactlessConfigure);
            checkBox2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    device.setContactlessConfiguration(isChecked ? true : false);
                }
            });
            CheckBox clearContactlessCacheCheckBox = (CheckBox) checkBoxView.findViewById(R.id.clearContactlessCache);
            clearContactlessCacheCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    device.setReaderContactlessConfiguredSharedPreference(isChecked ? false : true);
                }
            });

            device.setContactless(false);
            CheckBox enableContactlessCacheCheckBox = (CheckBox) checkBoxView.findViewById(R.id.enableContactless);
            enableContactlessCacheCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    device.setContactless(isChecked ? true : false);
                }
            });

            builder.setTitle("Select a device:");
            builder.setView(checkBoxView);
            builder.setCancelable(false);
            builder.setItems(R.array.reader_type, new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {

                        case 0:
                            if (device.device_setDeviceType(DEVICE_TYPE.DEVICE_VP3300_AJ)) {
                                Toast.makeText(getActivity(), "VP3300 Audio Jack (Audio Jack) is selected", Toast.LENGTH_SHORT).show();
                                applyClearentConfiguration = false;
                                btleDeviceRegistered = false;
                                isBluetoothScanning = false;
                            } else {
                                Toast.makeText(getActivity(), "Failed. Please disconnect first.", Toast.LENGTH_SHORT).show();
                            }
                            break;
                        case 1:
                            if (device.device_setDeviceType(DEVICE_TYPE.DEVICE_VP3300_BT)) {
                                Toast.makeText(getActivity(), "VP3300 Bluetooth (Bluetooth) is selected", Toast.LENGTH_SHORT).show();
                                dlgBTLE_Name = new Dialog(getActivity());
                                dlgBTLE_Name.setTitle("Enter Name or Address");
                                dlgBTLE_Name.setCancelable(false);
                                dlgBTLE_Name.setContentView(R.layout.btle_device_name_dialog);
                                Button btnBTLE_Ok = (Button) dlgBTLE_Name.findViewById(R.id.btnSetBTLE_Name_Ok);
                                edtBTLE_Name = (EditText) dlgBTLE_Name.findViewById(R.id.edtBTLE_Name);
                                String bleId = "03826";
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

                                btnBTLE_Ok.setOnClickListener(setBTLE_NameOnClick);
                                dlgBTLE_Name.show();
                                btleDeviceRegistered = false;
                                isBluetoothScanning = false;
                            } else
                                Toast.makeText(getActivity(), "Failed. Please disconnect first.", Toast.LENGTH_SHORT).show();
                            break;
                    }

                    if (device.device_getDeviceType() == DEVICE_TYPE.DEVICE_VP3300_USB) {
                        handler.post(doEnableButtons);
                        handler.post(doRegisterListen);
                    } else if (device.device_getDeviceType() != DEVICE_TYPE.DEVICE_VP3300_BT) {
                        handler.post(doRegisterListen);
                        device.device_configurePeripheralAndConnect();
                    }
                }
            });

            builder.create().show();
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

                device.setIDT_Device(fwTool);

                if (!isBleSupported(getActivity())) {
                    Toast.makeText(getActivity(), "Bluetooth LE is not supported\r\n", Toast.LENGTH_LONG).show();
                    return;
                }

                final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
                mBtAdapter = bluetoothManager.getAdapter();

                if (mBtAdapter == null) {
                    Toast.makeText(getActivity(), "Bluetooth LE is not available\r\n", Toast.LENGTH_LONG).show();
                    return;
                }

                btleDeviceRegistered = false;
                isBluetoothScanning = false;
                isReady = false;

                displayTransactionPopup();
                if (!mBtAdapter.isEnabled()) {
                    Log.i("CLEARENT", "Adapter");
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                } else {
                    scanforDevice(true, BLE_ScanTimeout);
                }
            }
        };

        private void displayTransactionPopup() {
            System.out.println("displayTransactionPopup!!!!!");
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    infoText.setText(info);
                    if (transactionAlertDialog != null) {
                        transactionAlertDialog.setTitle("Processing payment");
                        transactionAlertDialog.setMessage("Wait for instructions...");
                        transactionAlertDialog.show();
                    } else {
                        AlertDialog.Builder transactionViewBuilder = new AlertDialog.Builder(getActivity());

                        transactionViewBuilder.setTitle("Processing payment");
                        transactionViewBuilder.setMessage("Wait for instructions...");
                        transactionViewBuilder.setView(layoutInflater.inflate(R.layout.frame_swipe, viewGroup, false));
                        transactionViewBuilder.setCancelable(false);
                        transactionViewBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int which) {
                                int ret = device.device_cancelTransaction();
                                if (ret == ErrorCode.SUCCESS) {
                                    infoText.setText("Transaction cancelled");
                                } else {
                                    infoText.setText("Failed to cancel transaction");
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

        boolean isBleSupported(Context context) {
            return BluetoothAdapter.getDefaultAdapter() != null && context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
        }

        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (device.device_getDeviceType() == DEVICE_TYPE.DEVICE_VP3300_BT) {
                if (requestCode == REQUEST_ENABLE_BT) {
                    if (resultCode == Activity.RESULT_OK) {
                        Toast.makeText(getActivity(), "Bluetooth has turned on, now searching for device", Toast.LENGTH_SHORT).show();
                        scanforDevice(true, BLE_ScanTimeout);
                    } else {
                        Toast.makeText(getActivity(), "Problem in Bluetooth Turning ON", Toast.LENGTH_SHORT).show();
                        handler.post(doEnableButtons);
                    }
                }
            }
        }

        private void scanforDevice(final boolean enable, final long timeout) {
            if (device.device_getDeviceType() != DEVICE_TYPE.DEVICE_VP3300_BT) {
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
                        if (!device.device_isConnected()) {
                            info += "\nTimed out trying to find bluetooth device";
                            handler.post(doUpdateStatus);
                            btleDeviceRegistered = false;
                            bleRetryCount++;
                            if (bleRetryCount <= BLE_Max_Scan_Retries) {
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
                    String storedDeviceSerialNumberOfConfiguredReader = device.getStoredDeviceSerialNumberOfConfiguredReader();
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
                            String checkTransactionMessage = "Sample Transaction successful. Transaction Id:";
                            String checkReceiptMessage = "Sample receipt sent successfully";
                            String checkFailedTransactionFailed = "Sample transaction failed";
                            if (lines[0].contains(checkFailedTransactionFailed)) {
                                if (transactionAlertDialog != null && transactionAlertDialog.isShowing()) {
                                    transactionAlertDialog.hide();
                                }
                            } else if (lines[0].contains(checkTransactionMessage)) {
                                if (transactionAlertDialog != null && transactionAlertDialog.isShowing()) {
                                    transactionAlertDialog.hide();
                                }
                                runSampleReceipt(lines[0]);
                            } else if (lines[0].contains(checkReceiptMessage)) {
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
            receiptRequest.setApiKey(Constants.API_KEY_FOR_DEMO_ONLY);
            receiptRequest.setBaseUrl(getPaymentsBaseUrl());
            ReceiptDetail receiptDetail = new ReceiptDetail();
            receiptDetail.setEmailAddress("dhigginbotham@clearent.com,lwalden@clearent.com");
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
                        device.emv_cancelTransaction(resData);
                        swipeButton.setEnabled(true);
                    }
                }
            }, time);
        }

        public void releaseSDK() {
            if (device != null) {
                device.unregisterListen();
                device.release();
            }
        }

        public void displaySdkInfo() {
            info += "Manufacturer: " + android.os.Build.MANUFACTURER + "\n" +
                    "Model: " + android.os.Build.MODEL + "\n" +
                    "OS Version: " + android.os.Build.VERSION.RELEASE + " \n" +
                    "SDK Version: \n" + device.config_getSDKVersion() + "\n";

            detail = "";

            handler.post(doUpdateStatus);
        }

        private Runnable doUpdateStatus = new Runnable() {
            public void run() {
                infoText.setText(info);
            }
        };

        private Runnable doEnableButtons = new Runnable() {
            public void run() {
                swipeButton.setEnabled(true);
            }
        };

        private Runnable doSwipeProgressBar = new Runnable() {
            public void run() {
                if (device.device_isConnected()) {
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
                    if (device.device_getDeviceType() == DEVICE_TYPE.DEVICE_VP3300_BT) {
                        scanforDevice(true, BLE_ScanTimeout);
                    }
                }
            }
        };

        private Runnable doRegisterListen = new Runnable() {
            public void run() {
                device.registerListen();
            }
        };

        private Runnable doStartTransaction = new Runnable() {
            @Override
            public void run() {
                byte tags[] = {(byte) 0xDF, (byte) 0xEF, 0x1F, 0x02, 0x01, 0x00};

                ClearentPaymentRequest clearentPaymentRequest = new ClearentPaymentRequest(1.00, 0.00, 0, 60, null);
                clearentPaymentRequest.setEmailAddress("dhigginbotham@clearent.com");

                int ret = device.device_startTransaction(clearentPaymentRequest);
                if (ret == ErrorCode.NO_CONFIG) {
                    transactionAlertDialog.hide();
                    info = "Reader is not configured\n";
                    info += "Status: " + device.device_getResponseCodeString(ret) + "";
                    handler.post(doEnableButtons);
                    handler.post(doUpdateStatus);
                } else if (ret == ErrorCode.SUCCESS || ret == ErrorCode.RETURN_CODE_OK_NEXT_COMMAND) {
                    transactionAlertDialog.setMessage("Insert card or try swipe...");
                } else if (device.device_setDeviceType(DEVICE_TYPE.DEVICE_VP3300_AJ)) {
                    transactionAlertDialog.setMessage("Failed to start transaction. Check for low battery amber light or reconnect reader");
                    info = "Card reader is not connected\n";
                    info += "Status: " + device.device_getResponseCodeString(ret) + "";
                    handler.post(doEnableButtons);
                    handler.post(doUpdateStatus);
                } else if (!device.device_isConnected() && device.device_setDeviceType(DEVICE_TYPE.DEVICE_VP3300_BT)) {
                    transactionAlertDialog.setMessage("Card reader is not connecting. Check for low battery amber light or press button to try again");
                    info = "Card reader is not connected. Press button,\n";
                    info += "Status: " + device.device_getResponseCodeString(ret) + "";
                    handler.post(doEnableButtons);
                    handler.post(doUpdateStatus);
                    btleDeviceRegistered = false;
                    scanforDevice(true, BLE_ScanTimeout);
                } else {
                    transactionAlertDialog.setMessage("Failed to start transaction. Cancel and try again");
                    info = "cannot swipe/tap card\n";
                    info += "Status: " + device.device_getResponseCodeString(ret) + "";
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

        public class SwipeButtonListener implements OnClickListener {
            public void onClick(View arg0) {
                int ret;
                totalEMVTime = System.currentTimeMillis();
                textAmount = (EditText) findViewById(R.id.textAmount);
                handler.post(doSwipeProgressBar);
            }
        }

        private class ManualButtonListener implements OnClickListener {
            public void onClick(View arg0) {
                textCard = (EditText) findViewById(R.id.textCard);
                textExpirationDate = (EditText) findViewById(R.id.textExpirationDate);
                textCsc = (EditText) findViewById(R.id.textCsc);

                CreditCard creditCard = new CreditCard();
                creditCard.setCard(textCard.getText().toString());
                creditCard.setExpirationDateMMYY(textExpirationDate.getText().toString());
                creditCard.setCsc(textCsc.getText().toString());

                manualCardTokenizer.createTransactionToken(creditCard);

                manualButton.setEnabled(false);
                swipeButton.setEnabled(false);

                info = "Manual tokenization requested\n";
                detail = "";
                handler.post(doUpdateStatus);
            }
        }

        public void deviceConnected() {
            if (applyClearentConfiguration) {
                long waitBeforeAttemptingConfiguration = 100;
                if (isBluetoothScanning) {
                    waitBeforeAttemptingConfiguration = 8000;
                }
                handler.postDelayed(new Runnable() {
                    public void run() {
                        if (applyClearentConfiguration) {
                            device.applyClearentConfiguration();
                        }
                    }
                }, waitBeforeAttemptingConfiguration);
            }

            System.out.println(device.getStoredDeviceSerialNumberOfConfiguredReader());
        }

        public void deviceDisconnected() {
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    if (transactionAlertDialog != null && transactionAlertDialog.isShowing()) {
                        transactionAlertDialog.setMessage("Bluetooth disconnected. Press button on reader.");
                    }
                }
            });
            if (!Common.getBootLoaderMode()) {
                info += "Reader is disconnected";
                detail = "";
                handler.post(doUpdateStatus);
            }
            isReady = false;
        }

        public void timeout(int errorCode) {
            info += ErrorCodeInfo.getErrorCodeDescription(errorCode);
            detail = "";
            handler.post(showTimeout);
        }

        private Runnable showTimeout = new Runnable() {
            public void run() {
                if (transactionAlertDialog != null && transactionAlertDialog.isShowing()) {
                    transactionAlertDialog.setMessage("Timed out");
                }
                infoText.setText(info);
            }
        };

        public void ICCNotifyInfo(byte[] dataNotify, String strMessage) {
            if (strMessage != null && strMessage.length() > 0) {
                String strHexResp = Common.getHexStringFromBytes(dataNotify);

                info += "ICC Notification Info: " + strMessage + "\n" + "Resp: " + strHexResp;
                detail = "";
                handler.post(doUpdateStatus);
            }
        }

        public void msgToConnectDevice() {
            info += "\nConnect device...\n";
            detail = "";
            handler.post(doUpdateStatus);
        }

        public void msgAudioVolumeAdjustFailed() {
            info += "SDK could not adjust volume...";
            detail = "";
            handler.post(doUpdateStatus);
        }

        @Override
        public void onReceiveMsgUpdateFirmwareProgress(int i) {
            //do nothing
        }

        @Override
        public void onReceiveMsgUpdateFirmwareResult(int i) {
            //do nothing
        }

        public void onReceiveMsgChallengeResult(int returnCode, byte[] data) {
            // Not called for UniPay Firmware update
        }


        public void LoadXMLConfigFailureInfo(int index, String strMessage) {
            info += "XML loading error...";
            detail = "";
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
            Log.i("WATCH", "msgBatteryLow");
        }
    }

}
