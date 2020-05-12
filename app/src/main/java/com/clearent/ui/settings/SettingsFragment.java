package com.clearent.ui.settings;

import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


import com.clearent.Constants;
import com.clearent.idtech.android.PublicOnReceiverListener;
import com.clearent.idtech.android.domain.CardProcessingResponse;
import com.clearent.idtech.android.domain.ClearentFeedback;
import com.clearent.idtech.android.domain.ReaderInterfaceMode;
import com.clearent.idtech.android.domain.connection.AudioJack;
import com.clearent.idtech.android.domain.connection.Bluetooth;
import com.clearent.idtech.android.domain.connection.BluetoothSearchType;
import com.clearent.idtech.android.token.domain.TransactionToken;
import com.clearent.reader.CardReaderService;
import com.clearent.payment.R;
import com.clearent.util.LocalCache;
import com.idtechproducts.device.ReaderInfo;
import com.idtechproducts.device.StructConfigParameters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import java.util.ArrayList;
import java.util.List;

public class SettingsFragment extends Fragment implements PublicOnReceiverListener, AdapterView.OnItemSelectedListener {

    private SettingsViewModel settingsViewModel;
    private Button configureReaderButton;
    private Button selectBluetoothDeviceButton;
    private Button unPairBluetoothDeviceButton;

    private AlertDialog configurationDialog;

    private Handler handler = new Handler();

    private boolean isReady = false;

    private String bluetoothReaderLast5 = null;
    private String settingsApiKey = null;
    private String settingsPublicKey = null;
    private Boolean settingsProdEnvironment = false;
    private Boolean settingsBluetoothReader = false;
    private Boolean settingsBluetoothReaderUsb = false;
    private Boolean settingsAudioJackReader = false;
    private Boolean enableContactless = false;
    private Boolean enable2In1Mode = false;
    private Boolean clearContactCache = false;
    private Boolean clearContactlessCache = false;

    View root = null;
    ViewGroup viewGroup;
    LayoutInflater layoutInflater;

    private CardReaderService cardReaderService;
    private boolean okayToConfigure = false;
    private boolean configuring = false;
    private String currentFirmwareVersion;
    private String currentDeviceSerialNumber;
    private Spinner spinner;
    private TextView spinnerText;

    private Bluetooth bluetooth;

    ArrayList<String> scannedBluetoothDevices = new ArrayList<>();
    ArrayAdapter<String> spinnerArrayAdapter;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        this.layoutInflater = inflater;
        this.viewGroup = container;
        settingsViewModel =
                ViewModelProviders.of(getActivity()).get(SettingsViewModel.class);
        root = inflater.inflate(R.layout.fragment_configure, container, false);

        syncLocalCache(root);
        bindButtons(root);

        updateViewWithModel();


        spinner = (Spinner) root.findViewById(R.id.bluetooth_spinner);

        spinnerArrayAdapter = new ArrayAdapter<String>
                (getActivity().getApplicationContext(), android.R.layout.simple_spinner_dropdown_item,
                        scannedBluetoothDevices);
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setPrompt("Select Device");
        spinnerText = (TextView) root.findViewById(R.id.spinner_text);
        spinner.setVisibility(View.INVISIBLE);
        spinnerText.setVisibility(View.INVISIBLE);

        //spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinnerArrayAdapter);
        spinner.setOnItemSelectedListener(this);

        updateReaderConnected("Reader Disconnected ❌");


        return root;
    }

    private void updateViewWithModel() {
        final TextView last5View = root.findViewById(R.id.settings_last_five_of_reader);
        last5View.setText(settingsViewModel.getLast5OfBluetoothReader().getValue());

        final RadioButton prodEnvRadioButton = root.findViewById(R.id.settings_prod_env);
        prodEnvRadioButton.setChecked(settingsViewModel.getProdEnvironment().getValue() == 1 ? true : false);

        final RadioButton sandBoxEnvRadioButton = root.findViewById(R.id.settings_sandbox_env);
        sandBoxEnvRadioButton.setChecked(settingsViewModel.getSandboxEnvironment().getValue() == 1 ? true : false);

        final RadioButton audioJackRadioButton = root.findViewById(R.id.settings_audiojack_reader);
        audioJackRadioButton.setChecked(settingsViewModel.getAudioJackReader().getValue() == 1 ? true : false);

        final RadioButton radioButton = root.findViewById(R.id.settings_bluetooth_reader);
        radioButton.setChecked(settingsViewModel.getBluetoothReader().getValue() == 1 ? true : false);

        final RadioButton radioButtonBluetoothUSB = root.findViewById(R.id.settings_bluetooth_reader_usb);
        radioButtonBluetoothUSB.setChecked(settingsViewModel.getBluetoothReaderUsb().getValue() == 1 ? true : false);

        final CheckBox enableContactlessCheckBox = root.findViewById(R.id.enableContactless);
        enableContactlessCheckBox.setChecked(settingsViewModel.getEnableContactless().getValue());

        final CheckBox enableContactlessConfigurationCheckBox = root.findViewById(R.id.checkboxContactlessConfigure);
        enableContactlessConfigurationCheckBox.setChecked(settingsViewModel.getConfigureContactless().getValue());

        final CheckBox enableContactConfigurationCheckBox = root.findViewById(R.id.checkboxAutoConfigure);
        enableContactConfigurationCheckBox.setChecked(settingsViewModel.getConfigureContact().getValue());


        final CheckBox clearContactlessCacheCheckbox = root.findViewById(R.id.clearContactlessCache);
        clearContactlessCacheCheckbox.setChecked(settingsViewModel.getClearContactlessConfigurationCache().getValue());


        final CheckBox clearContactCacheCheckbox = root.findViewById(R.id.clearReaderCache);
        clearContactCacheCheckbox.setChecked(settingsViewModel.getClearContactConfigurationCache().getValue());

        final CheckBox enable2In1ModeCheckbox = root.findViewById(R.id.enable2In1Mode);
        enable2In1ModeCheckbox.setChecked(settingsViewModel.getEnable2In1Mode().getValue());

        final CheckBox searchBlueboothCheckbox = root.findViewById(R.id.searchBluetooth);
        searchBlueboothCheckbox.setChecked(settingsViewModel.getSearchBluetooth().getValue());

        final CheckBox connectToFirstFoundCheckbox = root.findViewById(R.id.connectToFirstBluetooth);
        connectToFirstFoundCheckbox.setChecked(settingsViewModel.getConnectToFirstBluetooth().getValue());
    }

    public void onItemSelected(AdapterView<?> parent, View view,
                               int pos, long id) {

        String selectedBluetoothDevice = (String) parent.getItemAtPosition(pos);
        TextView textView = (TextView) root.findViewById(R.id.settings_bluetooth_friendlyname);
        if (textView != null) {
            textView.setText(selectedBluetoothDevice);
        }

    }

    public void onNothingSelected(AdapterView<?> parent) {
        // Another interface callback
    }

    @Override
    public void onPause() {
        super.onPause();
        releaseSDK();
        updateModelFromView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        releaseSDK();
        updateModelFromView();
    }

    @Override
    public void onResume() {
        super.onResume();

        updateViewWithModel();
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

    private void bindButtons(View root) {
        configureReaderButton = (Button) root.findViewById(R.id.settings_configure_reader_button);
        configureReaderButton.setOnClickListener(new ConfigureReaderButtonListener());
        configureReaderButton.setEnabled(true);

        selectBluetoothDeviceButton = (Button) root.findViewById(R.id.settings_select_bluetooth_button);
        selectBluetoothDeviceButton.setOnClickListener(new SelectBluetoothReaderButtonListener());
        selectBluetoothDeviceButton.setEnabled(true);

        unPairBluetoothDeviceButton = (Button) root.findViewById(R.id.settings_disconnect_bluetooth_button);
        unPairBluetoothDeviceButton.setOnClickListener(new UnpairBluetoothReaderButtonListener());
        unPairBluetoothDeviceButton.setEnabled(true);

        final RadioGroup radioGroup = (RadioGroup) root.findViewById(R.id.settings_readers);
        final RadioButton audioJackReaderButton = root.findViewById(R.id.settings_audiojack_reader);
        final RadioButton bluetoothReaderButton = root.findViewById(R.id.settings_bluetooth_reader);
        final RadioButton bluetoothReaderUsbButton = root.findViewById(R.id.settings_bluetooth_reader_usb);
        final TextView bluetoothFriendlyView = root.findViewById(R.id.settings_bluetooth_friendlyname);
        final TextView last5View = root.findViewById(R.id.settings_last_five_of_reader);
        final CheckBox searchBluetoothCheckBox = root.findViewById(R.id.searchBluetooth);
        final CheckBox connectFirstFoundCheckBox = root.findViewById(R.id.connectToFirstBluetooth);
        final TextView selectBluetoothReaderButton = root.findViewById(R.id.settings_select_bluetooth_button);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (audioJackReaderButton.isChecked()) {
                    selectBluetoothReaderButton.setEnabled(false);
                    last5View.setEnabled(false);
                    searchBluetoothCheckBox.setEnabled(false);
                    bluetoothFriendlyView.setEnabled(false);
                    connectFirstFoundCheckBox.setEnabled(false);
                } else if (bluetoothReaderButton.isChecked()) {
                    selectBluetoothReaderButton.setEnabled(true);
                    last5View.setEnabled(true);
                    bluetoothFriendlyView.setEnabled(true);
                    searchBluetoothCheckBox.setEnabled(true);
                    connectFirstFoundCheckBox.setEnabled(true);
                } else if (bluetoothReaderUsbButton.isChecked()) {
                    selectBluetoothReaderButton.setEnabled(false);
                    last5View.setEnabled(true);
                    bluetoothFriendlyView.setEnabled(true);
                    searchBluetoothCheckBox.setEnabled(true);
                    connectFirstFoundCheckBox.setEnabled(true);
                }
            }
        });
    }

    private void syncLocalCache(final View root) {

        final TextView last5View = root.findViewById(R.id.settings_last_five_of_reader);
        final TextView bluetoothFriendlyView = root.findViewById(R.id.settings_bluetooth_friendlyname);

        settingsViewModel.getProdEnvironment().observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer onOff) {
                settingsProdEnvironment = onOff == 0 ? false : true;
                LocalCache.setProdValue(getActivity().getApplicationContext(), onOff);
            }
        });

        settingsViewModel.getBluetoothReader().observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer onOff) {
                settingsBluetoothReader = onOff == 0 ? false : true;
                LocalCache.setBluetoothReaderValue(getActivity().getApplicationContext(), onOff);
            }
        });
        settingsViewModel.getBluetoothReaderUsb().observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer onOff) {
                settingsBluetoothReaderUsb = onOff == 0 ? false : true;
                LocalCache.setBluetoothReaderUsbValue(getActivity().getApplicationContext(), onOff);
            }
        });
        settingsViewModel.getAudioJackReader().observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer onOff) {
                settingsAudioJackReader = onOff == 0 ? false : true;
                LocalCache.setAudioJackValue(getActivity().getApplicationContext(), onOff);

            }
        });
        settingsViewModel.getEnableContactless().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean enabled) {
                enableContactless = enabled;
                LocalCache.setEnableContactlessValue(getActivity().getApplicationContext(), enabled);
            }
        });
        settingsViewModel.getEnable2In1Mode().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean enabled) {
                enable2In1Mode = enabled;
                LocalCache.setEnable2InModeValue(getActivity().getApplicationContext(), enabled);
            }
        });
        settingsViewModel.getClearContactConfigurationCache().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean enabled) {
                clearContactCache = enabled;
                LocalCache.setClearContactConfigValue(getActivity().getApplicationContext(), enabled);
            }
        });
        settingsViewModel.getClearContactlessConfigurationCache().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean enabled) {
                clearContactlessCache = enabled;
                LocalCache.setClearContactlessConfigValue(getActivity().getApplicationContext(), enabled);
            }
        });

        settingsViewModel.getLast5OfBluetoothReader().observe(this, new Observer<String>() {
            @Override
            public void onChanged(String s) {
                last5View.setText(s);
                LocalCache.setSelectedBluetoothDeviceLast5(getActivity().getApplicationContext(), s);
            }
        });

        settingsViewModel.getBluetoothFriendlyName().observe(this, new Observer<String>() {
            @Override
            public void onChanged(String s) {
                bluetoothFriendlyView.setText(s);
                LocalCache.setSelectedUsedBluetoothFriendlyName(getActivity().getApplicationContext(), s);
            }
        });

        settingsViewModel.getSearchBluetooth().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean enabled) {
                LocalCache.setSearchBluetoothValue(getActivity().getApplicationContext(), enabled);
            }
        });

        settingsViewModel.getConnectToFirstBluetooth().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean enabled) {
                LocalCache.setConnectToFirstBluetoothFoundValue(getActivity().getApplicationContext(), enabled);
            }
        });

    }

    @Override
    public void isReady() {
       //deprecated

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

        if (configurationDialog != null && configurationDialog.isShowing()) {
            addPopupMessage(configurationDialog, message);
        }

        Toast.makeText(getActivity(), "Configuration Failed \uD83D\uDC4E", Toast.LENGTH_LONG).show();
    }

    @Override
    public void lcdDisplay(int mode, final String[] lines, int timeout) {
        if (lines != null && lines.length > 0) {
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    if (configurationDialog != null && configurationDialog.isShowing()) {
                        addPopupMessage(configurationDialog, lines[0]);
                    }
                }
            });
        }
    }

    private void closePopup() {
        if (configurationDialog != null) {
            configurationDialog.hide();
            TextView textView = (TextView) configurationDialog.findViewById(R.id.popupMessages);
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


    @Override
    public void lcdDisplay(int mode, String[] lines, int timeout, byte[] languageCode, byte messageId) {
        System.out.println("here");

    }

    @Override
    public void deviceConnected() {

        if (configuring && configurationDialog != null && configurationDialog.isShowing()) {
            closePopup();
            cardReaderService.addRemoteLogRequest(Constants.getSoftwareTypeAndVersion(), "Configuration applied to reader " + cardReaderService.getStoredDeviceSerialNumberOfConfiguredReader());
            Toast.makeText(getActivity(), "\uD83D\uDC28 \uD83D\uDC28 \uD83D\uDC28 \uD83D\uDC28 \uD83D\uDC28 Configuration Applied \uD83D\uDC28 \uD83D\uDC28 \uD83D\uDC28 \uD83D\uDC28 \uD83D\uDC28", Toast.LENGTH_LONG).show();
        }

        currentFirmwareVersion = cardReaderService.getFirmware();
        currentDeviceSerialNumber = cardReaderService.getDeviceSerialNumber();
        if(currentDeviceSerialNumber != null) {
            updateReaderConnected(currentDeviceSerialNumber + " Connected \uD83D\uDC9A️");
        } else {
            updateReaderConnected("Reader Connected \uD83D\uDC9A️");
        }

    }

    @Override
    public void deviceDisconnected() {
        currentFirmwareVersion = "";
        currentDeviceSerialNumber = "";
        updateReaderConnected("Reader Disconnected ❌");
    }

    @Override
    public void timeout(int errorCode) {
        Toast.makeText(getActivity(), "Configuration Timed out \uD83D\uDC4E", Toast.LENGTH_LONG).show();
    }

    @Override
    public void ICCNotifyInfo(byte[] dataNotify, String strMessage) {
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
        //Only grab this info if IDTech requires logging to research an issue.
    }

    @Override
    public void autoConfigProgress(int i) {
        //needed ?
    }

    @Override
    public void autoConfigCompleted(StructConfigParameters structConfigParameters) {
        Toast.makeText(getActivity(), "Peripheral configuration completed", Toast.LENGTH_LONG).show();
    }

    @Override
    public void deviceConfigured() {
        System.out.println("here");
    }

    @Override
    public void bluetoothDevices(List<BluetoothDevice> list) {

        if(list != null && list.size() > 0) {
            scannedBluetoothDevices.clear();
            spinner.setVisibility(View.VISIBLE);
            spinnerText.setVisibility(View.VISIBLE);
            for(BluetoothDevice bluetoothDevice:list) {
                scannedBluetoothDevices.add(bluetoothDevice.getName());
            }
            spinnerArrayAdapter.notifyDataSetChanged();
        }

    }

    @Override
    public void feedback(ClearentFeedback clearentFeedback) {
        //TODO sendfeedback
    }


    public class ConfigureReaderButtonListener implements View.OnClickListener {
        public void onClick(View arg0) {

            configuring = false;
            updateModelFromView();

            if (cardReaderService == null) {
                initCardReaderService();
            }

            if (!configurable()) {
                return;
            }

            displayConfigurationPopup();

            if (!cardReaderService.device_isConnected()) {
                Toast.makeText(getActivity(), "Reader not connected ", Toast.LENGTH_LONG).show();
            } else if (cardReaderService.device_isConnected()) {
                applyConfiguration();
            }
        }
    }

    public class SelectBluetoothReaderButtonListener implements View.OnClickListener {
        public void onClick(View arg0) {

            if (cardReaderService == null) {
                initCardReaderService();
            }

            updateModelFromView();//why?
            Toast.makeText(getActivity(), "Press button on reader(s)", Toast.LENGTH_LONG).show();

            if (settingsAudioJackReader) {
                AudioJack audiojack = new AudioJack(ReaderInterfaceMode.CLEARENT_READER_INTERFACE_3_IN_1);
                cardReaderService.startConnection(audiojack);
            } else {
                cardReaderService.startConnection(getBluetooth());
            }


        }
    }

    public class UnpairBluetoothReaderButtonListener implements View.OnClickListener {
        public void onClick(View arg0) {
            cardReaderService.unpairBluetooth();
        }
    }


    private Bluetooth getBluetooth() {
        ReaderInterfaceMode readerInterfaceMode = ReaderInterfaceMode.CLEARENT_READER_INTERFACE_3_IN_1;
        boolean searchBluetooth = settingsViewModel.getSearchBluetooth().getValue();
        String bluetoothFriendlyName = settingsViewModel.getBluetoothFriendlyName().getValue();
        String last5 = settingsViewModel.getLast5OfBluetoothReader().getValue();

        if(searchBluetooth) {
            bluetooth = new Bluetooth(readerInterfaceMode,BluetoothSearchType.SEARCH_ONLY);
        } else if(bluetoothFriendlyName != null && !"".equals(bluetoothFriendlyName)) {
            bluetooth = new Bluetooth(readerInterfaceMode,BluetoothSearchType.FRIENDLY_NAME, bluetoothFriendlyName);
        } else if(last5 != null && !"".equals(last5)) {
            bluetooth = new Bluetooth(readerInterfaceMode,BluetoothSearchType.LAST_5_OF_DEVICE_SERIAL_NUMBER, last5);
        } else {
            bluetooth = new Bluetooth(readerInterfaceMode,BluetoothSearchType.CONNECT_TO_FIRST_FOUND);
        }
        return bluetooth;

    }

    private void updateModelFromView() {

        final RadioButton prodEnvRadioButton = root.findViewById(R.id.settings_prod_env);
        settingsViewModel.getProdEnvironment().setValue(prodEnvRadioButton.isChecked() ? 1 : 0);

        final RadioButton sandBoxEnvRadioButton = root.findViewById(R.id.settings_sandbox_env);
        settingsViewModel.getSandboxEnvironment().setValue(sandBoxEnvRadioButton.isChecked() ? 1 : 0);

        final RadioButton audioJackRadioButton = root.findViewById(R.id.settings_audiojack_reader);
        settingsViewModel.getAudioJackReader().setValue(audioJackRadioButton.isChecked() ? 1 : 0);

        final RadioButton radioButton = root.findViewById(R.id.settings_bluetooth_reader);
        settingsViewModel.getBluetoothReader().setValue(radioButton.isChecked() ? 1 : 0);

        final RadioButton radioButtonusb = root.findViewById(R.id.settings_bluetooth_reader_usb);
        settingsViewModel.getBluetoothReaderUsb().setValue(radioButtonusb.isChecked() ? 1 : 0);

        final CheckBox enableContactlessCheckBox = root.findViewById(R.id.enableContactless);
        settingsViewModel.getEnableContactless().setValue(enableContactlessCheckBox.isChecked());

        final CheckBox enableContactlessConfigurationCheckBox = root.findViewById(R.id.checkboxContactlessConfigure);
        settingsViewModel.getConfigureContactless().setValue(enableContactlessConfigurationCheckBox.isChecked());

        final CheckBox enableContactConfigurationCheckBox = root.findViewById(R.id.checkboxAutoConfigure);
        settingsViewModel.getConfigureContact().setValue(enableContactConfigurationCheckBox.isChecked());

        final CheckBox clearContactlessCacheCheckbox = root.findViewById(R.id.clearContactlessCache);
        settingsViewModel.getClearContactlessConfigurationCache().setValue(clearContactlessCacheCheckbox.isChecked());

        final CheckBox clearContactCacheCheckbox = root.findViewById(R.id.clearReaderCache);
        settingsViewModel.getClearContactConfigurationCache().setValue(clearContactCacheCheckbox.isChecked());

        final CheckBox enable2In1ModeCheckbox = root.findViewById(R.id.enable2In1Mode);
        settingsViewModel.getEnable2In1Mode().setValue(enable2In1ModeCheckbox.isChecked());

        final TextView last5View = root.findViewById(R.id.settings_last_five_of_reader);
        settingsViewModel.getLast5OfBluetoothReader().setValue(last5View.getText().toString());

        final TextView bluetoothFriendlyName = root.findViewById(R.id.settings_bluetooth_friendlyname);
        settingsViewModel.getBluetoothFriendlyName().setValue(bluetoothFriendlyName.getText().toString());

        final CheckBox searchBluetoothCheckbox = root.findViewById(R.id.searchBluetooth);
        settingsViewModel.getSearchBluetooth().setValue(searchBluetoothCheckbox.isChecked());

        final CheckBox firstConnectFoundCheckbox = root.findViewById(R.id.connectToFirstBluetooth);
        settingsViewModel.getConnectToFirstBluetooth().setValue(firstConnectFoundCheckbox.isChecked());

    }

    void applyConfiguration() {
        if (configurable()) {
            cardReaderService.setContactlessConfiguration(settingsViewModel.getConfigureContactless().getValue());
            cardReaderService.setContactless(settingsViewModel.getEnableContactless().getValue());
            cardReaderService.setAutoConfiguration(settingsViewModel.getConfigureContact().getValue());

            if (settingsViewModel.getClearContactConfigurationCache().getValue()) {
                cardReaderService.setReaderConfiguredSharedPreference(settingsViewModel.getClearContactConfigurationCache().getValue());
            }
            if (settingsViewModel.getClearContactlessConfigurationCache().getValue()) {
                cardReaderService.setReaderContactlessConfiguredSharedPreference(settingsViewModel.getClearContactlessConfigurationCache().getValue());
            }
            configuring = true;

            cardReaderService.addRemoteLogRequest(Constants.getSoftwareTypeAndVersion(), settingsViewModel.toString());

            cardReaderService.applyClearentConfiguration();
        } else {
            if (configurationDialog.isShowing()) {
                closePopup();
            }
        }
    }

    private boolean configurable() {
        if (settingsViewModel.getConfigureContact().getValue() || settingsViewModel.getConfigureContactless().getValue()) {
            return true;
        }
        return false;
    }


    private Runnable doRegisterListen = new Runnable() {
        public void run() {
            cardReaderService.registerListen();
        }
    };

    private void displayConfigurationPopup() {
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                if (configurationDialog != null) {
                    configurationDialog.setTitle("Configuring Reader");
                    addPopupMessage(configurationDialog, "Do not cancel, disconnect, or switch apps...");
                    configurationDialog.show();
                } else {
                    AlertDialog.Builder configurationViewBuilder = new AlertDialog.Builder(getActivity());

                    configurationViewBuilder.setTitle("Configuring Reader");

                    View view = layoutInflater.inflate(R.layout.frame_swipe, viewGroup, false);
                    configurationViewBuilder.setView(view);

                    configurationViewBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int which) {
                            cardReaderService.device_cancelTransaction();
                            TextView textView = (TextView) configurationDialog.findViewById(R.id.popupMessages);
                            if (textView != null) {
                                textView.setText("");
                            }
                            Toast.makeText(getActivity(), "Configuration cancelled", Toast.LENGTH_SHORT).show();
                        }
                    });

                    configurationViewBuilder.setCancelable(false);

                    configurationDialog = configurationViewBuilder.create();

                    addPopupMessage(configurationDialog, "Do not cancel, disconnect, or switch apps...");

                    configurationDialog.show();
                }
            }
        });
    }

    private boolean isBluetoothReaderConfigured() {
        Integer audioJackReader = settingsViewModel.getAudioJackReader().getValue();
        boolean audioJackReaderEnabled = audioJackReader == 0 ? false : true;
        return !audioJackReaderEnabled;
    }

    private void initCardReaderService() {

        ReaderInfo.DEVICE_TYPE device_type = ReaderInfo.DEVICE_TYPE.DEVICE_VP3300_BT;

        if (!isBluetoothReaderConfigured()) {
            device_type = ReaderInfo.DEVICE_TYPE.DEVICE_VP3300_AJ;
        }

        String baseUrl = Constants.SB_BASE_URL;
        String publicKey = Constants.SB_PUBLIC_KEY;
        Integer prodEnvironment = settingsViewModel.getProdEnvironment().getValue();
        boolean prodEnvironmentEnabled = prodEnvironment == 0 ? false : true;
        if (prodEnvironmentEnabled) {
            baseUrl = Constants.PROD_BASE_URL;
            publicKey = Constants.PROD_PUBLIC_KEY;
        }

        cardReaderService = new CardReaderService(device_type, this, getContext(), baseUrl, publicKey, true);

        boolean device_setDeviceTypeResponse = cardReaderService.device_setDeviceType(device_type);
        if (!device_setDeviceTypeResponse) {
            Toast.makeText(getActivity(), "Issue setting device type", Toast.LENGTH_LONG).show();
        }
        cardReaderService.setContactlessConfiguration(false);
        cardReaderService.setContactless(settingsViewModel.getEnableContactless().getValue());
        cardReaderService.setAutoConfiguration(false);

        cardReaderService.addRemoteLogRequest("Android_IDTech_VP3300_JDemo", "Initialized the VP3300 For Configuration");
    }

    private void updateReaderConnected(final String message) {
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                final TextView readerConnectView = root.findViewById(R.id.readerConnected);
                readerConnectView.setText(message);
                final TextView firmwareView = root.findViewById(R.id.firmware);
                firmwareView.setText(currentFirmwareVersion);
            }
        });
    }

}