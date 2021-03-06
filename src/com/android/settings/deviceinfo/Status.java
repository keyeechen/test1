/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.deviceinfo;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.telephony.CellBroadcastMessage;
import android.telephony.PhoneNumberUtils;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.android.internal.telephony.Dsds;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.PhoneStateIntentReceiver;
import com.android.settings.R;
import com.android.settings.Utils;

import java.lang.ref.WeakReference;

/**
 * Display the following information
 * # Phone Number
 * # Network
 * # Roaming
 * # Device Id (IMEI in GSM and MEID in CDMA)
 * # Network type
 * # Operator info (area info cell broadcast for Brazil)
 * # Signal Strength
 * # Battery Strength  : TODO
 * # Uptime
 * # Awake Time
 * # XMPP/buzz/tickle status : TODO
 *
 */
public class Status extends PreferenceActivity {

    private static final String KEY_DATA_STATE = "data_state";
    private static final String KEY_DATA_STATE1 = "data_state1";
    private static final String KEY_DATA_STATE2 = "data_state2";
    private static final String KEY_SERVICE_STATE = "service_state";
    private static final String KEY_SERVICE_STATE1 = "service_state1";
    private static final String KEY_SERVICE_STATE2 = "service_state2";
    private static final String KEY_OPERATOR_NAME = "operator_name";
    private static final String KEY_OPERATOR_NAME1 = "operator_name1";
    private static final String KEY_OPERATOR_NAME2 = "operator_name2";
    private static final String KEY_ROAMING_STATE = "roaming_state";
    private static final String KEY_ROAMING_STATE1 = "roaming_state1";
    private static final String KEY_ROAMING_STATE2 = "roaming_state2";
    private static final String KEY_NETWORK_TYPE = "network_type";
    private static final String KEY_LATEST_AREA_INFO = "latest_area_info";
    private static final String KEY_NETWORK_TYPE1 = "network_type1";
    private static final String KEY_NETWORK_TYPE2 = "network_type2";
    private static final String KEY_PHONE_NUMBER = "number";
    private static final String KEY_PHONE_NUMBER1 = "number1";
    private static final String KEY_PHONE_NUMBER2 = "number2";
    private static final String KEY_IMEI_SV = "imei_sv";
    private static final String KEY_IMEI_SV1 = "imei_sv1";
    private static final String KEY_IMEI_SV2 = "imei_sv2";
    private static final String KEY_IMEI = "imei";
    private static final String KEY_IMEI1 = "imei1";
    private static final String KEY_IMEI2 = "imei2";
    private static final String KEY_PRL_VERSION = "prl_version";
    private static final String KEY_PRL_VERSION1 = "prl_version1";
    private static final String KEY_PRL_VERSION2 = "prl_version2";
    private static final String KEY_MIN_NUMBER = "min_number";
    private static final String KEY_MIN_NUMBER1 = "min_number1";
    private static final String KEY_MIN_NUMBER2 = "min_number2";
    private static final String KEY_MEID_NUMBER = "meid_number";
    private static final String KEY_MEID_NUMBER1 = "meid_number1";
    private static final String KEY_MEID_NUMBER2 = "meid_number2";
    private static final String KEY_SIGNAL_STRENGTH = "signal_strength";
    private static final String KEY_SIGNAL_STRENGTH1 = "signal_strength1";
    private static final String KEY_SIGNAL_STRENGTH2 = "signal_strength2";
    private static final String KEY_BATTERY_STATUS = "battery_status";
    private static final String KEY_BATTERY_LEVEL = "battery_level";
    private static final String KEY_IP_ADDRESS = "wifi_ip_address";
    private static final String KEY_WIFI_MAC_ADDRESS = "wifi_mac_address";
    private static final String KEY_BT_ADDRESS = "bt_address";
    private static final String KEY_SERIAL_NUMBER = "serial_number";
    private static final String KEY_ICC_ID = "icc_id";
    private static final String KEY_ICC_ID1 = "icc_id1";
    private static final String KEY_ICC_ID2 = "icc_id2";
    private static final String KEY_WIMAX_MAC_ADDRESS = "wimax_mac_address";
    private static final String[] PHONE_RELATED_ENTRIES = {
        KEY_DATA_STATE,
        KEY_SERVICE_STATE,
        KEY_OPERATOR_NAME,
        KEY_ROAMING_STATE,
        KEY_NETWORK_TYPE,
        KEY_LATEST_AREA_INFO,
        KEY_PHONE_NUMBER,
        KEY_IMEI,
        KEY_IMEI_SV,
        KEY_PRL_VERSION,
        KEY_MIN_NUMBER,
        KEY_MEID_NUMBER,
        KEY_SIGNAL_STRENGTH,
        KEY_ICC_ID
    };

    static final String CB_AREA_INFO_RECEIVED_ACTION =
            "android.cellbroadcastreceiver.CB_AREA_INFO_RECEIVED";

    static final String GET_LATEST_CB_AREA_INFO_ACTION =
            "android.cellbroadcastreceiver.GET_LATEST_CB_AREA_INFO";

    // Require the sender to have this permission to prevent third-party spoofing.
    static final String CB_AREA_INFO_SENDER_PERMISSION =
            "android.permission.RECEIVE_EMERGENCY_BROADCAST";

    private static final String[] PHONE_RELATED_ENTRIES1 = {
        KEY_DATA_STATE1,
        KEY_SERVICE_STATE1,
        KEY_OPERATOR_NAME1,
        KEY_ROAMING_STATE1,
        KEY_NETWORK_TYPE1,
        KEY_PHONE_NUMBER1,
        KEY_IMEI1,
        KEY_IMEI_SV1,
        KEY_PRL_VERSION1,
        KEY_MIN_NUMBER1,
        KEY_MEID_NUMBER1,
        KEY_SIGNAL_STRENGTH1,
        KEY_ICC_ID1
    };

    private static final String[] PHONE_RELATED_ENTRIES2 = {
        KEY_DATA_STATE2,
        KEY_SERVICE_STATE2,
        KEY_OPERATOR_NAME2,
        KEY_ROAMING_STATE2,
        KEY_NETWORK_TYPE2,
        KEY_PHONE_NUMBER2,
        KEY_IMEI2,
        KEY_IMEI_SV2,
        KEY_PRL_VERSION2,
        KEY_MIN_NUMBER2,
        KEY_MEID_NUMBER2,
        KEY_SIGNAL_STRENGTH2,
        KEY_ICC_ID2
    };

    private static final int EVENT_SIGNAL_STRENGTH_CHANGED = 200;
    private static final int EVENT_SERVICE_STATE_CHANGED = 300;

    private static final int EVENT_UPDATE_STATS = 500;

    private TelephonyManager mTelephonyManager;
    private Phone mPhone = null;
    private Phone mPhone2 = null;
    private ServiceState mCurrentServiceState;
    private ServiceState mCurrentServiceState2;
    private SignalStrength mCurrentSignalStrength;
    private SignalStrength mCurrentSignalStrength2;
    private Resources mRes;
    private Preference mSignalStrength;
    private Preference mSignalStrength2;
    private Preference mUptime;
    private boolean mShowLatestAreaInfo;

    private String sUnknown;

    private Preference mBatteryStatus;
    private Preference mBatteryLevel;

    private Handler mHandler;

    private static class MyHandler extends Handler {
        private WeakReference<Status> mStatus;

        public MyHandler(Status activity) {
            mStatus = new WeakReference<Status>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            Status status = mStatus.get();
            if (status == null) {
                return;
            }

            switch (msg.what) {
                case EVENT_UPDATE_STATS:
                    status.updateTimes();
                    sendEmptyMessageDelayed(EVENT_UPDATE_STATS, 1000);
                    break;
            }
        }
    }

    private BroadcastReceiver mBatteryInfoReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                mBatteryLevel.setSummary(Utils.getBatteryPercentage(intent));
                mBatteryStatus.setSummary(Utils.getBatteryStatus(getResources(), intent));
            }
        }
    };

    private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onServiceStateChanged(ServiceState serviceState) {
            mCurrentServiceState = serviceState;
            updateServiceState(serviceState);
        }

        @Override
        public void onDataConnectionStateChanged(int state) {
            updateDataState();
            updateNetworkType();
        }

        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            mCurrentSignalStrength = signalStrength;
            updateSignalStrength();
        }
    };

    private PhoneStateListener mPhoneStateListener2 = new PhoneStateListener() {
        @Override
        public void onServiceStateChanged(ServiceState serviceState) {
            mCurrentServiceState2 = serviceState;
            updateServiceState2(serviceState);
        }

        @Override
        public void onDataConnectionStateChanged(int state) {
            updateDataState2();
            updateNetworkType2();
        }

        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            mCurrentSignalStrength2 = signalStrength;
            updateSignalStrength2();
        }
    };

    private BroadcastReceiver mAreaInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (CB_AREA_INFO_RECEIVED_ACTION.equals(action)) {
                Bundle extras = intent.getExtras();
                if (extras == null) {
                    return;
                }
                CellBroadcastMessage cbMessage = (CellBroadcastMessage) extras.get("message");
                if (cbMessage != null && cbMessage.getServiceCategory() == 50) {
                    String latestAreaInfo = cbMessage.getMessageBody();
                    updateAreaInfo(latestAreaInfo);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mHandler = new MyHandler(this);

        mTelephonyManager = (TelephonyManager)getSystemService(TELEPHONY_SERVICE);

        addPreferencesFromResource(R.xml.device_info_status);
        mBatteryLevel = findPreference(KEY_BATTERY_LEVEL);
        mBatteryStatus = findPreference(KEY_BATTERY_STATUS);

        mRes = getResources();
        sUnknown = mRes.getString(R.string.device_info_default);
        if (UserHandle.myUserId() == UserHandle.USER_OWNER) {
            if (Dsds.isDualSimSolution()) {
                mPhone = PhoneFactory.getPhone(PhoneConstants.SimId.SIM1);
                mPhone2 = PhoneFactory.getPhone(PhoneConstants.SimId.SIM2);
            } else {
                mPhone = PhoneFactory.getDefaultPhone();
                mPhone2 = null;
            }
        }
        mUptime = findPreference("up_time");

        if (mPhone == null || Utils.isWifiOnly(getApplicationContext())) {
            for (String key : PHONE_RELATED_ENTRIES) {
                removePreferenceFromScreen(key);
            }
            for (String key : PHONE_RELATED_ENTRIES1) {
                removePreferenceFromScreen(key);
            }
            for (String key : PHONE_RELATED_ENTRIES2) {
                removePreferenceFromScreen(key);
            }
        } else if (Dsds.isDualSimSolution()) {
            for (String key : PHONE_RELATED_ENTRIES) {
                removePreferenceFromScreen(key);
            }

            // Note - missing in zaku build, be careful later...
            mSignalStrength = findPreference(KEY_SIGNAL_STRENGTH1);
            mSignalStrength2 = findPreference(KEY_SIGNAL_STRENGTH2);
            // NOTE "imei" is the "Device ID" since it represents
            //  the IMEI in GSM and the MEID in CDMA
            if (mPhone.getPhoneName().equals("CDMA")) {
                setSummaryText(KEY_MEID_NUMBER1, mPhone.getMeid());
                setSummaryText(KEY_MIN_NUMBER1, mPhone.getCdmaMin());
                if (getResources().getBoolean(R.bool.config_msid_enable)) {
                    findPreference(KEY_MIN_NUMBER1).setTitle(R.string.status_msid_number);
                }
                setSummaryText(KEY_PRL_VERSION1, mPhone.getCdmaPrlVersion());
                removePreferenceFromScreen(KEY_IMEI_SV1);

                if (mPhone.getLteOnCdmaMode() == PhoneConstants.LTE_ON_CDMA_TRUE) {
                    // Show ICC ID and IMEI for LTE device
                    setSummaryText(KEY_ICC_ID1, mPhone.getIccSerialNumber());
                    setSummaryText(KEY_IMEI1, mPhone.getImei());
                } else {
                    // device is not GSM/UMTS, do not display GSM/UMTS features
                    // check Null in case no specified preference in overlay xml
                    removePreferenceFromScreen(KEY_IMEI1);
                    removePreferenceFromScreen(KEY_ICC_ID1);
                }
            } else {
                setSummaryText(KEY_IMEI1, mPhone.getDeviceId());

                setSummaryText(KEY_IMEI_SV1,
                        ((TelephonyManager) getSystemService(TELEPHONY_SERVICE))
                            .getDeviceSoftwareVersion());

                // device is not CDMA, do not display CDMA features
                // check Null in case no specified preference in overlay xml
                removePreferenceFromScreen(KEY_PRL_VERSION1);
                removePreferenceFromScreen(KEY_MEID_NUMBER1);
                removePreferenceFromScreen(KEY_MIN_NUMBER1);
                removePreferenceFromScreen(KEY_ICC_ID1);
            }

            String rawNumber = mPhone.getLine1Number();  // may be null or empty
            String formattedNumber = null;
            if (!TextUtils.isEmpty(rawNumber)) {
                formattedNumber = PhoneNumberUtils.formatNumber(rawNumber);
            }
            // If formattedNumber is null or empty, it'll display as "Unknown".
            setSummaryText(KEY_PHONE_NUMBER1, formattedNumber);

            if (mPhone2.getPhoneName().equals("CDMA")) {
                setSummaryText(KEY_MEID_NUMBER2, mPhone2.getMeid());
                setSummaryText(KEY_MIN_NUMBER2, mPhone2.getCdmaMin());
                if (getResources().getBoolean(R.bool.config_msid_enable)) {
                    findPreference(KEY_MIN_NUMBER2).setTitle(R.string.status_msid_number);
                }
                setSummaryText(KEY_PRL_VERSION2, mPhone2.getCdmaPrlVersion());
                removePreferenceFromScreen(KEY_IMEI_SV2);

                if (mPhone2.getLteOnCdmaMode() == PhoneConstants.LTE_ON_CDMA_TRUE) {
                    // Show ICC ID and IMEI for LTE device
                    setSummaryText(KEY_ICC_ID2, mPhone2.getIccSerialNumber());
                    setSummaryText(KEY_IMEI2, mPhone2.getImei());
                } else {
                    // device is not GSM/UMTS, do not display GSM/UMTS features
                    // check Null in case no specified preference in overlay xml
                    removePreferenceFromScreen(KEY_IMEI2);
                    removePreferenceFromScreen(KEY_ICC_ID2);
                }
            } else {
                setSummaryText(KEY_IMEI2, mPhone2.getDeviceId());

                setSummaryText(KEY_IMEI_SV2,
                        ((TelephonyManager) getSystemService(TELEPHONY_SERVICE))
                            .getDeviceSoftwareVersion());

                // device is not CDMA, do not display CDMA features
                // check Null in case no specified preference in overlay xml
                removePreferenceFromScreen(KEY_PRL_VERSION2);
                removePreferenceFromScreen(KEY_MEID_NUMBER2);
                removePreferenceFromScreen(KEY_MIN_NUMBER2);
                removePreferenceFromScreen(KEY_ICC_ID2);
            }

            String rawNumber2 = mPhone2.getLine1Number();  // may be null or empty
            String formattedNumber2 = null;
            if (!TextUtils.isEmpty(rawNumber2)) {
                formattedNumber2 = PhoneNumberUtils.formatNumber(rawNumber2);
            }
            // If formattedNumber is null or empty, it'll display as "Unknown".
            setSummaryText(KEY_PHONE_NUMBER2, formattedNumber2);
        } else {
            for (String key : PHONE_RELATED_ENTRIES1) {
                removePreferenceFromScreen(key);
            }
            for (String key : PHONE_RELATED_ENTRIES2) {
                removePreferenceFromScreen(key);
            }

            // Note - missing in zaku build, be careful later...
            mSignalStrength = findPreference(KEY_SIGNAL_STRENGTH);

            // NOTE "imei" is the "Device ID" since it represents
            //  the IMEI in GSM and the MEID in CDMA
            if (mPhone.getPhoneName().equals("CDMA")) {
                setSummaryText(KEY_MEID_NUMBER, mPhone.getMeid());
                setSummaryText(KEY_MIN_NUMBER, mPhone.getCdmaMin());
                if (getResources().getBoolean(R.bool.config_msid_enable)) {
                    findPreference(KEY_MIN_NUMBER).setTitle(R.string.status_msid_number);
                }
                setSummaryText(KEY_PRL_VERSION, mPhone.getCdmaPrlVersion());
                removePreferenceFromScreen(KEY_IMEI_SV);

                if (mPhone.getLteOnCdmaMode() == PhoneConstants.LTE_ON_CDMA_TRUE) {
                    // Show ICC ID and IMEI for LTE device
                    setSummaryText(KEY_ICC_ID, mPhone.getIccSerialNumber());
                    setSummaryText(KEY_IMEI, mPhone.getImei());
                } else {
                    // device is not GSM/UMTS, do not display GSM/UMTS features
                    // check Null in case no specified preference in overlay xml
                    removePreferenceFromScreen(KEY_IMEI);
                    removePreferenceFromScreen(KEY_ICC_ID);
                }
            } else {
                setSummaryText(KEY_IMEI, mPhone.getDeviceId());

                setSummaryText(KEY_IMEI_SV,
                        ((TelephonyManager) getSystemService(TELEPHONY_SERVICE))
                            .getDeviceSoftwareVersion());

                // device is not CDMA, do not display CDMA features
                // check Null in case no specified preference in overlay xml
                removePreferenceFromScreen(KEY_PRL_VERSION);
                removePreferenceFromScreen(KEY_MEID_NUMBER);
                removePreferenceFromScreen(KEY_MIN_NUMBER);
                removePreferenceFromScreen(KEY_ICC_ID);

                // only show area info when SIM country is Brazil
                if ("br".equals(mTelephonyManager.getSimCountryIso())) {
                    mShowLatestAreaInfo = true;
                }
            }

            String rawNumber = mPhone.getLine1Number();  // may be null or empty
            String formattedNumber = null;
            if (!TextUtils.isEmpty(rawNumber)) {
                formattedNumber = PhoneNumberUtils.formatNumber(rawNumber);
            }
            // If formattedNumber is null or empty, it'll display as "Unknown".
            setSummaryText(KEY_PHONE_NUMBER, formattedNumber);

            if (!mShowLatestAreaInfo) {
                removePreferenceFromScreen(KEY_LATEST_AREA_INFO);
            }
        }

        setWimaxStatus();
        setWifiStatus();
        setBtStatus();
        setIpAddressStatus();

        String serial = Build.SERIAL;
        if (serial != null && !serial.equals("")) {
            setSummaryText(KEY_SERIAL_NUMBER, serial);
        } else {
            removePreferenceFromScreen(KEY_SERIAL_NUMBER);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mPhone != null && !Utils.isWifiOnly(getApplicationContext())) {
            if (mShowLatestAreaInfo) {
                registerReceiver(mAreaInfoReceiver, new IntentFilter(CB_AREA_INFO_RECEIVED_ACTION),
                        CB_AREA_INFO_SENDER_PERMISSION, null);
                // Ask CellBroadcastReceiver to broadcast the latest area info received
                Intent getLatestIntent = new Intent(GET_LATEST_CB_AREA_INFO_ACTION);
                sendBroadcastAsUser(getLatestIntent, UserHandle.ALL,
                        CB_AREA_INFO_SENDER_PERMISSION);
            }
            if (Dsds.isDualSimSolution()) {
                updateSignalStrength();
                updateServiceState(mPhone.getServiceState());
                updateDataState();
                updateSignalStrength2();
                updateServiceState2(mPhone2.getServiceState());
                updateDataState2();

                mTelephonyManager.listenDs(TelephonyManager.SIM1, mPhoneStateListener,
                          PhoneStateListener.LISTEN_DATA_CONNECTION_STATE |
                          PhoneStateListener.LISTEN_SERVICE_STATE |
                          PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
                mTelephonyManager.listenDs(TelephonyManager.SIM2, mPhoneStateListener2,
                          PhoneStateListener.LISTEN_DATA_CONNECTION_STATE |
                          PhoneStateListener.LISTEN_SERVICE_STATE |
                          PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
            } else {
                updateSignalStrength();
                updateServiceState(mPhone.getServiceState());
                updateDataState();

                mTelephonyManager.listen(mPhoneStateListener,
                          PhoneStateListener.LISTEN_DATA_CONNECTION_STATE |
                          PhoneStateListener.LISTEN_SERVICE_STATE |
                          PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
            }
        }
        registerReceiver(mBatteryInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        mHandler.sendEmptyMessage(EVENT_UPDATE_STATS);
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mPhone != null && !Utils.isWifiOnly(getApplicationContext())) {
            if (Dsds.isDualSimSolution()) {
                mTelephonyManager.listenDs(TelephonyManager.SIM1, mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
                mTelephonyManager.listenDs(TelephonyManager.SIM2, mPhoneStateListener2, PhoneStateListener.LISTEN_NONE);
            } else {
                mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
            }
        }
        if (mShowLatestAreaInfo) {
            unregisterReceiver(mAreaInfoReceiver);
        }
        unregisterReceiver(mBatteryInfoReceiver);
        mHandler.removeMessages(EVENT_UPDATE_STATS);
    }

    /**
     * Removes the specified preference, if it exists.
     * @param key the key for the Preference item
     */
    private void removePreferenceFromScreen(String key) {
        Preference pref = findPreference(key);
        if (pref != null) {
            getPreferenceScreen().removePreference(pref);
        }
    }

    /**
     * @param preference The key for the Preference item
     * @param property The system property to fetch
     * @param alt The default value, if the property doesn't exist
     */
    private void setSummary(String preference, String property, String alt) {
        try {
            findPreference(preference).setSummary(
                    SystemProperties.get(property, alt));
        } catch (RuntimeException e) {

        }
    }

    private void setSummaryText(String preference, String text) {
            if (TextUtils.isEmpty(text)) {
               text = sUnknown;
             }
             // some preferences may be missing
             if (findPreference(preference) != null) {
                 findPreference(preference).setSummary(text);
             }
    }

    private void updateNetworkType() {
        // Whether EDGE, UMTS, etc...
        if (Dsds.isDualSimSolution()) {
            String networktype = null;
            if (TelephonyManager.NETWORK_TYPE_UNKNOWN !=
                    mTelephonyManager.getNetworkTypeDs(TelephonyManager.SIM1)) {
                networktype = mTelephonyManager.getNetworkTypeNameDs(TelephonyManager.SIM1);
            }
            setSummaryText(KEY_NETWORK_TYPE1, networktype);
        } else {
            String networktype = null;
            if (TelephonyManager.NETWORK_TYPE_UNKNOWN != mTelephonyManager.getNetworkType()) {
                networktype = mTelephonyManager.getNetworkTypeName();
            }
            setSummaryText(KEY_NETWORK_TYPE, networktype);
        }
    }

    private void updateNetworkType2() {
        // Whether EDGE, UMTS, etc...
        String networktype = null;
        if (TelephonyManager.NETWORK_TYPE_UNKNOWN !=
                mTelephonyManager.getNetworkTypeDs(TelephonyManager.SIM2)) {
            networktype = mTelephonyManager.getNetworkTypeNameDs(TelephonyManager.SIM2);
        }
        setSummaryText(KEY_NETWORK_TYPE2, networktype);
    }

    private void updateDataState() {
        int state = mTelephonyManager.getDataStateDs(TelephonyManager.SIM1);
        String display = mRes.getString(R.string.radioInfo_unknown);

        switch (state) {
            case TelephonyManager.DATA_CONNECTED:
                display = mRes.getString(R.string.radioInfo_data_connected);
                break;
            case TelephonyManager.DATA_SUSPENDED:
                display = mRes.getString(R.string.radioInfo_data_suspended);
                break;
            case TelephonyManager.DATA_CONNECTING:
                display = mRes.getString(R.string.radioInfo_data_connecting);
                break;
            case TelephonyManager.DATA_DISCONNECTED:
                display = mRes.getString(R.string.radioInfo_data_disconnected);
                break;
        }

        if (Dsds.isDualSimSolution()) {
            setSummaryText(KEY_DATA_STATE1, display);
        } else {
            setSummaryText(KEY_DATA_STATE, display);
        }
    }

    private void updateDataState2() {
        int state = mTelephonyManager.getDataStateDs(TelephonyManager.SIM2);
        String display = mRes.getString(R.string.radioInfo_unknown);

        switch (state) {
            case TelephonyManager.DATA_CONNECTED:
                display = mRes.getString(R.string.radioInfo_data_connected);
                break;
            case TelephonyManager.DATA_SUSPENDED:
                display = mRes.getString(R.string.radioInfo_data_suspended);
                break;
            case TelephonyManager.DATA_CONNECTING:
                display = mRes.getString(R.string.radioInfo_data_connecting);
                break;
            case TelephonyManager.DATA_DISCONNECTED:
                display = mRes.getString(R.string.radioInfo_data_disconnected);
                break;
        }

        setSummaryText(KEY_DATA_STATE2, display);
    }

    private void updateServiceState(ServiceState serviceState) {
        int state = serviceState.getState();
        String display = mRes.getString(R.string.radioInfo_unknown);

        switch (state) {
            case ServiceState.STATE_IN_SERVICE:
                display = mRes.getString(R.string.radioInfo_service_in);
                break;
            case ServiceState.STATE_OUT_OF_SERVICE:
            case ServiceState.STATE_EMERGENCY_ONLY:
                display = mRes.getString(R.string.radioInfo_service_out);
                break;
            case ServiceState.STATE_POWER_OFF:
                display = mRes.getString(R.string.radioInfo_service_off);
                break;
        }

        if (Dsds.isDualSimSolution()) {
            setSummaryText(KEY_SERVICE_STATE1, display);

            if (serviceState.getRoaming()) {
                setSummaryText(KEY_ROAMING_STATE1, mRes.getString(R.string.radioInfo_roaming_in));
            } else {
                setSummaryText(KEY_ROAMING_STATE1, mRes.getString(R.string.radioInfo_roaming_not));
            }
            setSummaryText(KEY_OPERATOR_NAME1, serviceState.getOperatorAlphaLong());
        } else {
            setSummaryText(KEY_SERVICE_STATE, display);

            if (serviceState.getRoaming()) {
                setSummaryText(KEY_ROAMING_STATE, mRes.getString(R.string.radioInfo_roaming_in));
            } else {
                setSummaryText(KEY_ROAMING_STATE, mRes.getString(R.string.radioInfo_roaming_not));
            }
            setSummaryText(KEY_OPERATOR_NAME, serviceState.getOperatorAlphaLong());
        }
    }

    private void updateServiceState2(ServiceState serviceState) {
        int state = serviceState.getState();
        String display = mRes.getString(R.string.radioInfo_unknown);

        switch (state) {
            case ServiceState.STATE_IN_SERVICE:
                display = mRes.getString(R.string.radioInfo_service_in);
                break;
            case ServiceState.STATE_OUT_OF_SERVICE:
            case ServiceState.STATE_EMERGENCY_ONLY:
                display = mRes.getString(R.string.radioInfo_service_out);
                break;
            case ServiceState.STATE_POWER_OFF:
                display = mRes.getString(R.string.radioInfo_service_off);
                break;
        }

        setSummaryText(KEY_SERVICE_STATE2, display);

        if (serviceState.getRoaming()) {
            setSummaryText(KEY_ROAMING_STATE2, mRes.getString(R.string.radioInfo_roaming_in));
        } else {
            setSummaryText(KEY_ROAMING_STATE2, mRes.getString(R.string.radioInfo_roaming_not));
        }
        setSummaryText(KEY_OPERATOR_NAME2, serviceState.getOperatorAlphaLong());
    }

    private void updateAreaInfo(String areaInfo) {
        if (areaInfo != null) {
            setSummaryText(KEY_LATEST_AREA_INFO, areaInfo);
        }
    }

    void updateSignalStrength() {
        // not loaded in some versions of the code (e.g., zaku)
        if (mSignalStrength != null) {
            int state = mCurrentServiceState == null?ServiceState.STATE_POWER_OFF:mCurrentServiceState.getState();
            Resources r = getResources();

            if ((ServiceState.STATE_OUT_OF_SERVICE == state) ||
                    (ServiceState.STATE_POWER_OFF == state)) {
                mSignalStrength.setSummary("0");
            }

            int signalDbm = mCurrentSignalStrength == null?0:mCurrentSignalStrength.getDbm();

            if (-1 == signalDbm) signalDbm = 0;

            int signalAsu = mCurrentSignalStrength == null?0:mCurrentSignalStrength.getAsuLevel();

            if (-1 == signalAsu) signalAsu = 0;

            mSignalStrength.setSummary(String.valueOf(signalDbm) + " "
                        + r.getString(R.string.radioInfo_display_dbm) + "   "
                        + String.valueOf(signalAsu) + " "
                        + r.getString(R.string.radioInfo_display_asu));
        }
    }

    void updateSignalStrength2() {
        // not loaded in some versions of the code (e.g., zaku)
        if (mSignalStrength2 != null) {
            int state = mCurrentServiceState2 == null?ServiceState.STATE_POWER_OFF:mCurrentServiceState2.getState();
            Resources r = getResources();

            if ((ServiceState.STATE_OUT_OF_SERVICE == state) ||
                    (ServiceState.STATE_POWER_OFF == state)) {
                mSignalStrength2.setSummary("0");
            }

            int signalDbm = mCurrentSignalStrength2 == null?0:mCurrentSignalStrength2.getDbm();

            if (-1 == signalDbm) signalDbm = 0;

            int signalAsu = mCurrentSignalStrength2 == null?0:mCurrentSignalStrength2.getAsuLevel();

            if (-1 == signalAsu) signalAsu = 0;

            mSignalStrength2.setSummary(String.valueOf(signalDbm) + " "
                        + r.getString(R.string.radioInfo_display_dbm) + "   "
                        + String.valueOf(signalAsu) + " "
                        + r.getString(R.string.radioInfo_display_asu));
        }
    }

    private void setWimaxStatus() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getNetworkInfo(ConnectivityManager.TYPE_WIMAX);

        if (ni == null) {
            PreferenceScreen root = getPreferenceScreen();
            Preference ps = (Preference) findPreference(KEY_WIMAX_MAC_ADDRESS);
            if (ps != null) root.removePreference(ps);
        } else {
            Preference wimaxMacAddressPref = findPreference(KEY_WIMAX_MAC_ADDRESS);
            String macAddress = SystemProperties.get("net.wimax.mac.address",
                    getString(R.string.status_unavailable));
            wimaxMacAddressPref.setSummary(macAddress);
        }
    }
    private void setWifiStatus() {
        WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();

        Preference wifiMacAddressPref = findPreference(KEY_WIFI_MAC_ADDRESS);

        String macAddress = wifiInfo == null ? null : wifiInfo.getMacAddress();
        wifiMacAddressPref.setSummary(!TextUtils.isEmpty(macAddress) ? macAddress
                : getString(R.string.status_unavailable));
    }

    private void setIpAddressStatus() {
        Preference ipAddressPref = findPreference(KEY_IP_ADDRESS);
        String ipAddress = Utils.getDefaultIpAddresses(this);
        if (ipAddress != null) {
            ipAddressPref.setSummary(ipAddress);
        } else {
            ipAddressPref.setSummary(getString(R.string.status_unavailable));
        }
    }

    private void setBtStatus() {
        BluetoothAdapter bluetooth = BluetoothAdapter.getDefaultAdapter();
        Preference btAddressPref = findPreference(KEY_BT_ADDRESS);

        if (bluetooth == null) {
            // device not BT capable
            getPreferenceScreen().removePreference(btAddressPref);
        } else {
            String address = bluetooth.isEnabled() ? bluetooth.getAddress() : null;
            btAddressPref.setSummary(!TextUtils.isEmpty(address) ? address
                    : getString(R.string.status_unavailable));
        }
    }

    void updateTimes() {
        long at = SystemClock.uptimeMillis() / 1000;
        long ut = SystemClock.elapsedRealtime() / 1000;

        if (ut == 0) {
            ut = 1;
        }

        mUptime.setSummary(convert(ut));
    }

    private String pad(int n) {
        if (n >= 10) {
            return String.valueOf(n);
        } else {
            return "0" + String.valueOf(n);
        }
    }

    private String convert(long t) {
        int s = (int)(t % 60);
        int m = (int)((t / 60) % 60);
        int h = (int)((t / 3600));

        return h + ":" + pad(m) + ":" + pad(s);
    }
}
