package com.clearent.ui.tools;

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
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;


import com.clearent.Constants;
import com.clearent.idtech.android.PublicOnReceiverListener;
import com.clearent.idtech.android.domain.CardProcessingResponse;
import com.clearent.idtech.android.token.domain.TransactionToken;
import com.clearent.payment.CardReaderService;
import com.clearent.payment.R;
import com.idtechproducts.device.Common;
import com.idtechproducts.device.ReaderInfo;
import com.idtechproducts.device.StructConfigParameters;
import com.idtechproducts.device.bluetooth.BluetoothLEController;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

public class ConfigureFragment extends Fragment implements PublicOnReceiverListener {

    private ConfigureViewModel configureViewModel;
    private Button configureReaderButton;

    private AlertDialog configurationDialog;

    private boolean isBluetoothScanning = false;
    private static final int REQUEST_ENABLE_BT = 1;
    private BluetoothAdapter mBtAdapter = null;
    private Handler handler = new Handler();

    private int bleRetryCount = 0;
    private boolean isReady = false;
    private boolean btleDeviceRegistered = false;
    private String btleDeviceAddress = null;

    private EditText edtBTLE_Name;
    private Dialog dlgBTLE_Name;

    private String settingsApiKey = null;
    private String settingsPublicKey = null;
    private Boolean settingsProdEnvironment = false;
    private Boolean settingsBluetoothReader = false;
    private Boolean settingsAudioJackReader = false;
    private Boolean enableContactless = false;
    private Boolean enable2In1Mode = false;
    private Boolean clearContactCache = false;
    private Boolean clearContactlessCache = false;

    private View root;

    private CardReaderService cardReaderService;
    private boolean okayToConfigure = false;
    private boolean configuring = false;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        configureViewModel =
                ViewModelProviders.of(getActivity()).get(ConfigureViewModel.class);
        root = inflater.inflate(R.layout.fragment_configure, container, false);

        observeConfigurationValues(root);
        bindButtons(root);

        final TextView apiKeytextView = root.findViewById(R.id.settings_apikey);
        apiKeytextView.setText(Constants.API_KEY_FOR_DEMO_ONLY);

        final TextView publicKeyTextView = root.findViewById(R.id.settings_publickey);
        publicKeyTextView.setText(Constants.PUBLIC_KEY);

        updateReaderConnected("Reader Disconnected ❌");

        return root;
    }

    @Override
    public void onPause() {
        super.onPause();
        updateViewModel();
    }

    private void bindButtons(View root) {
        configureReaderButton = (Button) root.findViewById(R.id.settings_configure_reader_button);
        configureReaderButton.setOnClickListener(new ConfigureReaderButtonListener());
        configureReaderButton.setEnabled(true);
    }

    private void observeConfigurationValues(View root) {
        configureViewModel.getApiKey().observe(this, new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                settingsApiKey = s;
            }
        });
        configureViewModel.getPublicKey().observe(this, new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                settingsPublicKey = s;
            }
        });

        configureViewModel.getProdEnvironment().observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer onOff) {
                settingsProdEnvironment = onOff == 0 ? false : true;
            }
        });

        configureViewModel.getBluetoothReader().observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer onOff) {
                settingsBluetoothReader = onOff == 0 ? false : true;
            }
        });
        configureViewModel.getAudioJackReader().observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer onOff) {
                settingsAudioJackReader = onOff == 0 ? false : true;
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
        configureViewModel.getClearContactConfigurationCache().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean enabled) {
                clearContactCache = enabled;
            }
        });
        configureViewModel.getClearContactlessConfigurationCache().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean enabled) {
                clearContactlessCache = enabled;
            }
        });
    }

    @Override
    public void isReady() {
        if(!configuring) {
            if (okayToConfigure) {
                Toast.makeText(getActivity(), "\uD83D\uDED1 Applying Configuration \uD83D\uDED1️", Toast.LENGTH_LONG).show();
                applyConfiguration();
            } else {
                Toast.makeText(getActivity(), "Configuration Not Applied", Toast.LENGTH_LONG).show();
            }
        } else {
            if (configurationDialog != null && configurationDialog.isShowing()) {
                configurationDialog.hide();
            }
            Toast.makeText(getActivity(), "\uD83D\uDC28 \uD83D\uDC28 \uD83D\uDC28 \uD83D\uDC28 \uD83D\uDC28 Configuration Applied \uD83D\uDC28 \uD83D\uDC28 \uD83D\uDC28 \uD83D\uDC28 \uD83D\uDC28", Toast.LENGTH_LONG).show();
        }

    }

    @Override
    public void successfulTransactionToken(TransactionToken transactionToken) {
        System.out.println("here");
        //does not apply to configuration
    }

    @Override
    public void handleCardProcessingResponse(CardProcessingResponse cardProcessingResponse) {
        System.out.println("here");
        //does not apply to configuration
    }

    @Override
    public void handleConfigurationErrors(String message) {
        Toast.makeText(getActivity(), "Configuration Failed \uD83D\uDC4E", Toast.LENGTH_LONG).show();
    }

    @Override
    public void lcdDisplay(int mode, String[] lines, int timeout) {
        System.out.println("lcdDisplay");
         //TODO
    }

    @Override
    public void lcdDisplay(int mode, String[] lines, int timeout, byte[] languageCode, byte messageId) {
        System.out.println("here");

    }

    @Override
    public void deviceConnected() {
        updateReaderConnected("Reader Connected \uD83D\uDC9A️");
    }

    @Override
    public void deviceDisconnected() {
        updateReaderConnected("Reader Disconnected ❌");
    }

    @Override
    public void timeout(int errorCode) {
        Toast.makeText(getActivity(), "Configuration Timed out \uD83D\uDC4E", Toast.LENGTH_LONG).show();
    }

    @Override
    public void ICCNotifyInfo(byte[] dataNotify, String strMessage) {
        System.out.println("here");
//does not apply to configuration
    }

    @Override
    public void msgBatteryLow() {
        Toast.makeText(getActivity(), "LOW BATTERY \uD83D\uDC4E", Toast.LENGTH_LONG).show();
    }

    @Override
    public void LoadXMLConfigFailureInfo(int index, String message) {
        Toast.makeText(getActivity(), "Loading of xml configuration file failed \uD83D\uDC4E", Toast.LENGTH_LONG).show();
    }

    @Override
    public void msgToConnectDevice() {
        updateReaderConnected("Press Button ⚠️");
    }

    @Override
    public void msgAudioVolumeAdjustFailed() {
        Toast.makeText(getActivity(), "Volume Adjust failed \uD83D\uDC4E", Toast.LENGTH_LONG).show();
    }

    @Override
    public void dataInOutMonitor(byte[] data, boolean isIncoming) {
        System.out.println("here");
//ignore
    }

    @Override
    public void autoConfigProgress(int i) {
        System.out.println("here");
//TODO
    }

    @Override
    public void autoConfigCompleted(StructConfigParameters structConfigParameters) {
        Toast.makeText(getActivity(), "Peripheral configuration completed", Toast.LENGTH_LONG).show();
    }

    @Override
    public void deviceConfigured() {
        System.out.println("here");
//TODO
    }

    public class ConfigureReaderButtonListener implements View.OnClickListener {
        public void onClick(View arg0) {

            configuring = false;
            updateViewModel();

            if(cardReaderService == null) {
                initCardReaderService();
            }

            if(!configurable()) {
                return;
            }

            if(cardReaderService.device_isConnected() && okayToConfigure){
                applyConfiguration();
            } else if(!cardReaderService.device_isConnected()){
                okayToConfigure = false;
                if(!isBluetoothReaderConfigured()) {
                    Toast.makeText(getActivity(), "Plug In Audio Jack", Toast.LENGTH_SHORT).show();
                    okayToConfigure = true;
                    cardReaderService.registerListen();
                    cardReaderService.device_configurePeripheralAndConnect();
                } else {
                    promptForReaderLast5Digits();
                }
            }
        }
    }

    private void updateViewModel() {

        final TextView apiKeytextView = root.findViewById(R.id.settings_apikey);
        configureViewModel.getApiKey().setValue(apiKeytextView.getText().toString());

        final TextView publicKeyTextView = root.findViewById(R.id.settings_publickey);
        configureViewModel.getPublicKey().setValue(publicKeyTextView.getText().toString());

        final RadioButton prodEnvRadioButton = root.findViewById(R.id.settings_prod_env);
        configureViewModel.getProdEnvironment().setValue(prodEnvRadioButton.isChecked()?1:0);

        final RadioButton sandBoxEnvRadioButton = root.findViewById(R.id.settings_sandbox_env);
        configureViewModel.getSandboxEnvironment().setValue(sandBoxEnvRadioButton.isChecked()?1:0);

        final RadioButton audioJackRadioButton = root.findViewById(R.id.settings_audiojack_reader);
        configureViewModel.getAudioJackReader().setValue(audioJackRadioButton.isChecked()?1:0);

        final RadioButton radioButton = root.findViewById(R.id.settings_bluetooth_reader);
        configureViewModel.getBluetoothReader().setValue(radioButton.isChecked()?1:0);

        final CheckBox enableContactlessCheckBox = root.findViewById(R.id.enableContactless);
        configureViewModel.getEnableContactless().setValue(enableContactlessCheckBox.isChecked());

        final CheckBox enableContactlessConfigurationCheckBox = root.findViewById(R.id.checkboxContactlessConfigure);
        configureViewModel.getConfigureContactless().setValue(enableContactlessConfigurationCheckBox.isChecked());

        final CheckBox enableContactConfigurationCheckBox = root.findViewById(R.id.checkboxAutoConfigure);
        configureViewModel.getConfigureContact().setValue(enableContactConfigurationCheckBox.isChecked());

        final CheckBox clearContactlessCacheCheckbox = root.findViewById(R.id.clearContactlessCache);
        configureViewModel.getClearContactlessConfigurationCache().setValue(clearContactlessCacheCheckbox.isChecked());

        final CheckBox clearContactCacheCheckbox = root.findViewById(R.id.clearReaderCache);
        configureViewModel.getClearContactConfigurationCache().setValue(clearContactCacheCheckbox.isChecked());

        final CheckBox enable2In1ModeCheckbox = root.findViewById(R.id.enable2In1Mode);
        configureViewModel.getEnable2In1Mode().setValue(enable2In1ModeCheckbox.isChecked());

    }

    void applyConfiguration() {
        if(configurable()) {
            cardReaderService.setContactlessConfiguration(configureViewModel.getConfigureContactless().getValue());
            cardReaderService.setContactless(configureViewModel.getEnableContactless().getValue());
            cardReaderService.setAutoConfiguration(configureViewModel.getConfigureContact().getValue());

            if (configureViewModel.getClearContactConfigurationCache().getValue()) {
                cardReaderService.setReaderConfiguredSharedPreference(configureViewModel.getClearContactConfigurationCache().getValue());
            }
            if (configureViewModel.getClearContactlessConfigurationCache().getValue()) {
                cardReaderService.setReaderContactlessConfiguredSharedPreference(configureViewModel.getClearContactlessConfigurationCache().getValue());
            }
            configuring = true;
            cardReaderService.applyClearentConfiguration();
        } else {
            if (configurationDialog.isShowing()) {
                configurationDialog.hide();
            }
        }
    }

    private boolean configurable() {
        if(configureViewModel.getConfigureContact().getValue() || configureViewModel.getConfigureContactless().getValue()) {
            return true;
        }
        Toast.makeText(getActivity(), "Configuration not enabled", Toast.LENGTH_SHORT).show();
        return false;
    }

    private void scanforDevice(final boolean enable, final long timeout) {
        if (!isBluetoothReaderConfigured()) {
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
                    mBtAdapter.getBluetoothLeScanner().stopScan(scanCallback);
                    isBluetoothScanning = false;
                    if (!cardReaderService.device_isConnected()) {

                        //info += "\nTimed out trying to find bluetooth device";
                        //show error
                        //handler.post(doUpdateStatus);

                        btleDeviceRegistered = false;
                        bleRetryCount++;
                        if (bleRetryCount <= Constants.BLE_MAX_SCAN_TRIES) {
                            if (configurationDialog.isShowing()) {
                                configurationDialog.setMessage("Connecting to bluetooth... " + bleRetryCount);
                            }

//                            info += "\nTrying again ";
//                            handler.post(doUpdateStatus);
                            //show error
                            scanLeDevice(true, timeout);
                        } else {
//                            info += "\nFailed to connect to bluetooth device.";
//                            handler.post(doUpdateStatus);
                            //TODO show error
                            if (configurationDialog.isShowing()) {
                                configurationDialog.setMessage("Failed to connect to bluetooth. Cancel and Try again");
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
                //info += "\nDevice found during scan at Time " + new Date().toString() + ". Pass device to idtech framework to register a listener.\n";
                //show device was found
                Log.i("SCAN", "Scan success " + result.getDevice().getName());
                BluetoothLEController.setBluetoothDevice(result.getDevice());
                btleDeviceAddress = result.getDevice().getAddress();
                btleDeviceRegistered = true;
                okayToConfigure = true;
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
//                info += "\nScanResult - Results" + sr.toString() + "\n";
//                handler.post(doUpdateStatus);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            System.out.println("BLE// onScanFailed");
            Log.e("Scan Failed", "Error Code: " + errorCode);
//            info += "\nScan Failed. Error Code: " + errorCode + "\n";
//            handler.post(doUpdateStatus);
        }
    };

    private Runnable doRegisterListen = new Runnable() {
        public void run() {
            cardReaderService.registerListen();
        }
    };

    void promptForReaderLast5Digits() {

        Integer audioJackReader = configureViewModel.getAudioJackReader().getValue();
        boolean audioJackReaderEnabled = audioJackReader == 0 ? false : true;

        if (!audioJackReaderEnabled) {
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

            btnBTLE_Ok.setOnClickListener(setBTLE_NameOnClick);
            dlgBTLE_Name.show();
            btleDeviceRegistered = false;
            isBluetoothScanning = false;
        } else if (audioJackReaderEnabled) {
            handler.post(doRegisterListen);
            cardReaderService.device_configurePeripheralAndConnect();
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

            displayConfigurationPopup();
            if (!mBtAdapter.isEnabled()) {
                Log.i("CLEARENT", "Adapter");
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            } else {
                scanforDevice(true, Constants.BLE_SCAN_TIMEOUT);
            }
        }
    };

    private void displayConfigurationPopup() {
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                if (configurationDialog != null) {
                    configurationDialog.setTitle("Configuring Reader.");
                    configurationDialog.setMessage("Do not cancel, disconnect, or switch apps...");
                    configurationDialog.show();
                } else {
                    AlertDialog.Builder configurationViewBuilder = new AlertDialog.Builder(getActivity());

                    configurationViewBuilder.setTitle("Configuring Reader.");
                    configurationViewBuilder.setMessage("Do not cancel, disconnect, or switch apps...");
                    configurationViewBuilder.setCancelable(false);
                    configurationViewBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int which) {
                            Toast.makeText(getActivity(), "Configuration cancelled", Toast.LENGTH_SHORT).show();
                        }
                    });
                    configurationDialog = configurationViewBuilder.create();
                    configurationDialog.show();
                }
            }
        });
    }

    boolean isBleSupported(Context context) {
        return BluetoothAdapter.getDefaultAdapter() != null && context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (isBluetoothReaderConfigured()) {
            if (requestCode == REQUEST_ENABLE_BT) {
                if (resultCode == Activity.RESULT_OK) {
                    Toast.makeText(getActivity(), "Bluetooth has turned on, now searching for device", Toast.LENGTH_SHORT).show();
                    scanforDevice(true, Constants.BLE_SCAN_TIMEOUT);
                } else {
                    Toast.makeText(getActivity(), "Problem in Bluetooth Turning ON", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private boolean isBluetoothReaderConfigured() {
        Integer audioJackReader = configureViewModel.getAudioJackReader().getValue();
        boolean audioJackReaderEnabled = audioJackReader == 0 ? false : true;
        return !audioJackReaderEnabled;
    }

    private void initCardReaderService() {

        ReaderInfo.DEVICE_TYPE device_type = ReaderInfo.DEVICE_TYPE.DEVICE_VP3300_BT;

        if(!isBluetoothReaderConfigured()) {
            device_type = ReaderInfo.DEVICE_TYPE.DEVICE_VP3300_AJ;
        }

        String baseUrl = Constants.BASE_URL;
        Integer prodEnvironment = configureViewModel.getProdEnvironment().getValue();
        boolean prodEnvironmentEnabled = prodEnvironment == 0 ? false : true;
        if(prodEnvironmentEnabled) {
            baseUrl = Constants.PROD_BASE_URL;

        }
        String publicKey = configureViewModel.getPublicKey().getValue();

        cardReaderService = new CardReaderService(device_type, this, getContext(), baseUrl, publicKey, true);

        boolean device_setDeviceTypeResponse = cardReaderService.device_setDeviceType(device_type);
        if(!device_setDeviceTypeResponse) {
            Toast.makeText(getActivity(), "Issue setting device type", Toast.LENGTH_LONG).show();
        }
        cardReaderService.setContactlessConfiguration(false);
        cardReaderService.setContactless(configureViewModel.getEnableContactless().getValue());
        cardReaderService.setAutoConfiguration(false);

        cardReaderService.addRemoteLogRequest("Android_IDTech_VP3300_JDemo", "Initialized the VP3300 For Configuration");
    }

    private void updateReaderConnected(final String message) {
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                final TextView readerConnectView = root.findViewById(R.id.readerConnected);
                readerConnectView.setText(message);
            }
        });
    }

}