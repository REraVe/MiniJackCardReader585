/**
 * Created by REraVe on 07.05.2017.
 */

package rerave.minijackcardreader;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.os.Handler;

import com.acs.audiojack.AesTrackData;
import com.acs.audiojack.AudioJackReader;
import com.acs.audiojack.DukptReceiver;
import com.acs.audiojack.DukptTrackData;
import com.acs.audiojack.Track1Data;
import com.acs.audiojack.Track2Data;
import com.acs.audiojack.TrackData;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.util.Locale;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class MiniJackReader {

    private static final String DEFAULT_MASTER_KEY_STRING = "00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00";
    private static final String DEFAULT_AES_KEY_STRING = "4E 61 74 68 61 6E 2E 4C 69 20 54 65 64 64 79 20";
    private static final String DEFAULT_IKSN_STRING = "FF FF 98 76 54 32 10 E0 00 00";
    private static final String DEFAULT_IPEK_STRING = "6A C2 92 FA A1 31 5B 4D 85 8A B3 A3 D7 D5 93 3A";

    private static boolean listenJackStarted = false;

    private Activity activity;
    private Handler handler;

    private AudioManager mAudioManager;
    private AudioJackReader mReader;
    private DukptReceiver mDukptReceiver = new DukptReceiver();

    private byte[] mMasterKey = new byte[16];
    private byte[] mAesKey    = new byte[16];
    private byte[] mIksn      = new byte[10];
    private byte[] mIpek      = new byte[16];

    private int mSwipeCount;

    private String mTrackDataSwipeCountPreference;
    private String mTrackDataBatteryStatusPreference;
    private String mTrackDataKeySerialNumberPreference;
    private String mTrackDataTrack1MacPreference;
    private String mTrackDataTrack2MacPreference;

    private String mTrack1Jis2DataPreference;
    private String mTrack1PrimaryAccountNumberPreference;
    private String mTrack1NamePreference;
    private String mTrack1ExpirationDatePreference;
    private String mTrack1ServiceCodePreference;
    private String mTrack1DiscretionaryDataPreference;

    private String mTrack2PrimaryAccountNumberPreference;
    private String mTrack2ExpirationDatePreference;
    private String mTrack2ServiceCodePreference;
    private String mTrack2DiscretionaryDataPreference;

    public MiniJackReader(Activity activity, Handler handler) {
        this.activity = activity;
        this.handler  = handler;

        mAudioManager = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
        mReader = new AudioJackReader(mAudioManager);

        initializeAudioJackReader();
    }

    private void initializeAudioJackReader() {
        mSwipeCount = 0;

        /* Load the master key. */
        String masterKeyString = DEFAULT_MASTER_KEY_STRING;
        toByteArray(masterKeyString, mMasterKey);

        masterKeyString = toHexString(mMasterKey);

        /* Load the AES key. */
        String aesKeyString = DEFAULT_AES_KEY_STRING;
        toByteArray(aesKeyString, mAesKey);

        aesKeyString = toHexString(mAesKey);

        /* Load the IKSN. */
        String iksnString = DEFAULT_IKSN_STRING;
        toByteArray(iksnString, mIksn);

        iksnString = toHexString(mIksn);

        /* Load the IPEK. */
        String ipekString = DEFAULT_IPEK_STRING;
        toByteArray(ipekString, mIpek);

        ipekString = toHexString(mIpek);

        /* Set the reset complete callback. */
        mReader.setOnResetCompleteListener(new OnResetCompleteListener());

        /* Set the firmware version callback. */
        mReader.setOnFirmwareVersionAvailableListener(new OnFirmwareVersionAvailableListener());

        /* Set the track data notification callback. */
        mReader.setOnTrackDataNotificationListener(new OnTrackDataNotificationListener());

        /* Set the track data callback. */
        mReader.setOnTrackDataAvailableListener(new OnTrackDataAvailableListener());

        /* Set the raw data callback. */
        mReader.setOnRawDataAvailableListener(new OnRawDataAvailableListener());

        /* Set the key serial number. */
        mDukptReceiver.setKeySerialNumber(mIksn);

        /* Load the initial key. */
        mDukptReceiver.loadInitialKey(mIpek);
    }

    public static boolean isListenJackStarted() {
        return MiniJackReader.listenJackStarted;
    }

    public static void setListenJackStarted(boolean listenJackStarted) {
        MiniJackReader.listenJackStarted = listenJackStarted;
    }

    public void startListenJack() {
        MiniJackReader.setListenJackStarted(true);

        /* Start the reader service */
        mReader.start();

        /* Reset the reader. */
        mReader.reset();
    }

    public void stopListenJack() {
        /* Stop the reader service */
        mReader.stop();

        MiniJackReader.setListenJackStarted(false);
    }

    /** Set the reset complete callback. */
    private class OnResetCompleteListener implements AudioJackReader.OnResetCompleteListener{
        @Override
        public void onResetComplete(AudioJackReader reader) {

        }
    }

    /** Set the firmware version callback. */
    private class OnFirmwareVersionAvailableListener implements AudioJackReader.OnFirmwareVersionAvailableListener {
        @Override
        public void onFirmwareVersionAvailable(AudioJackReader reader, String firmwareVersion) {

        }
    }

     /** Set the track data notification callback. */
    private class OnTrackDataNotificationListener implements AudioJackReader.OnTrackDataNotificationListener {
        @Override
        public void onTrackDataNotification(AudioJackReader reader) {

        }
    }

    /** Set the raw data callback. */
    private class OnRawDataAvailableListener implements AudioJackReader.OnRawDataAvailableListener {
        private String mHexString;

        @Override
        public void onRawDataAvailable(AudioJackReader reader, byte[] rawData) {
            mHexString = toHexString(rawData) + (reader.verifyData(rawData) ? " (Checksum OK)" : " (Checksum Error)");
        }
    }

    /** Set the track data callback. */
    private class OnTrackDataAvailableListener implements AudioJackReader.OnTrackDataAvailableListener {
        private Track1Data mTrack1Data;
        private Track2Data mTrack2Data;
        private Track1Data mTrack1MaskedData;
        private Track2Data mTrack2MaskedData;
        private String mTrack1MacString;
        private String mTrack2MacString;
        private String mBatteryStatusString;
        private String mKeySerialNumberString;
        private int mErrorId;

        @Override
        public void onTrackDataAvailable(AudioJackReader reader, TrackData trackData) {
            mTrack1Data       = new Track1Data();
            mTrack2Data       = new Track2Data();
            mTrack1MaskedData = new Track1Data();
            mTrack2MaskedData = new Track2Data();
            mTrack1MacString  = "";
            mTrack2MacString  = "";

            mBatteryStatusString   = toBatteryStatusString(trackData.getBatteryStatus());
            mKeySerialNumberString = "";

            if ((trackData.getTrack1ErrorCode() != TrackData.TRACK_ERROR_SUCCESS) && (trackData.getTrack2ErrorCode() != TrackData.TRACK_ERROR_SUCCESS)) {
                mErrorId = R.string.message_track_data_error_corrupted;
            }
            else if (trackData.getTrack1ErrorCode() != TrackData.TRACK_ERROR_SUCCESS) {
                mErrorId = R.string.message_track1_data_error_corrupted;
            }
            else if (trackData.getTrack2ErrorCode() != TrackData.TRACK_ERROR_SUCCESS) {
                mErrorId = R.string.message_track2_data_error_corrupted;
            }

            /* Show the track error. */
            if ((trackData.getTrack1ErrorCode() != TrackData.TRACK_ERROR_SUCCESS) || (trackData.getTrack2ErrorCode() != TrackData.TRACK_ERROR_SUCCESS)) {
                System.err.println("Ошибка: " + mErrorId);
            }

            /* Show the track data. */
            if (trackData instanceof AesTrackData) {
                showAesTrackData((AesTrackData) trackData);
            }
            else if (trackData instanceof DukptTrackData) {
                showDukptTrackData((DukptTrackData) trackData);
            }
        }

        /** Shows the AES track data. */
        private void showAesTrackData(AesTrackData trackData) {
            byte[] decryptedTrackData = null;

            /* Decrypt the track data. */
            try {
                decryptedTrackData = aesDecrypt(mAesKey, trackData.getTrackData());
            }
            catch (GeneralSecurityException e) {
                mErrorId = R.string.message_track_data_error_decrypted;
                System.err.println("Ошибка: " + mErrorId);

                /* Show the track data. */
                showTrackData();
                return;
            }

            /* Verify the track data. */
            if (!mReader.verifyData(decryptedTrackData)) {
                mErrorId = R.string.message_track_data_error_checksum;
                System.err.println("Ошибка: " + mErrorId);

                /* Show the track data. */
                showTrackData();
                return;
            }

            /* Decode the track data. */
            mTrack1Data.fromByteArray(decryptedTrackData, 0, trackData.getTrack1Length());
            mTrack2Data.fromByteArray(decryptedTrackData, 79, trackData.getTrack2Length());

            /* Show the track data. */
            showTrackData();
        }

        /**Shows the DUKPT track data. */
        private void showDukptTrackData(DukptTrackData trackData) {
            int ec  = 0;
            int ec2 = 0;
            byte[] track1Data = null;
            byte[] track2Data = null;
            String track1DataString = null;
            String track2DataString = null;
            byte[] key     = null;
            byte[] dek     = null;
            byte[] macKey  = null;
            byte[] dek3des = null;

            mKeySerialNumberString = toHexString(trackData.getKeySerialNumber());
            mTrack1MacString       = toHexString(trackData.getTrack1Mac());
            mTrack2MacString       = toHexString(trackData.getTrack2Mac());

            mTrack1MaskedData.fromString(trackData.getTrack1MaskedData());
            mTrack2MaskedData.fromString(trackData.getTrack2MaskedData());

            /* Compare the key serial number. */
            if (!DukptReceiver.compareKeySerialNumber(mIksn, trackData.getKeySerialNumber())) {
                mErrorId = R.string.message_track_data_error_ksn;
                System.err.println("Ошибка: " + mErrorId);

                /* Show the track data. */
                showTrackData();
                return;
            }

            /* Get the encryption counter from KSN. */
            ec = DukptReceiver.getEncryptionCounter(trackData.getKeySerialNumber());

            /* Get the encryption counter from DUKPT receiver. */
            ec2 = mDukptReceiver.getEncryptionCounter();

            /* Load the initial key if the encryption counter from KSN is less
             * than the encryption counter from DUKPT receiver. */
            if (ec < ec2) {
                mDukptReceiver.loadInitialKey(mIpek);
                ec2 = mDukptReceiver.getEncryptionCounter();
            }

            /*Synchronize the key if the encryption counter from KSN is greater
             * than the encryption counter from DUKPT receiver. */
            while (ec > ec2) {
                mDukptReceiver.getKey();
                ec2 = mDukptReceiver.getEncryptionCounter();
            }

            if (ec != ec2) {
                mErrorId = R.string.message_track_data_error_ec;
                System.err.println("Ошибка: " + mErrorId);

                /* Show the track data. */
                showTrackData();
                return;
            }

            key = mDukptReceiver.getKey();

            if (key == null) {
                System.err.println("Ошибка: The maximum encryption count had been reached.");

                /* Show the track data. */
                showTrackData();
                return;
            }

            dek = DukptReceiver.generateDataEncryptionRequestKey(key);
            macKey = DukptReceiver.generateMacRequestKey(key);
            dek3des = new byte[24];

            /* Generate 3DES key (K1 = K3) */
            System.arraycopy(dek, 0, dek3des, 0, dek.length);
            System.arraycopy(dek, 0, dek3des, 16, 8);

            try {
                if (trackData.getTrack1Data() != null) {
                    /* Decrypt the track 1 data. */
                    track1Data = tripleDesDecrypt(dek3des, trackData.getTrack1Data());

                    /* Generate the MAC for track 1 data. */
                    mTrack1MacString += " (" + toHexString(DukptReceiver.generateMac(macKey, track1Data)) + ")";

                    /* Get the track 1 data as string. */
                    track1DataString = new String(track1Data, 1, trackData.getTrack1Length(), "US-ASCII");

                    /* Divide the track 1 data into fields. */
                    mTrack1Data.fromString(track1DataString);
                }

                if (trackData.getTrack2Data() != null) {

                    /* Decrypt the track 2 data. */
                    track2Data = tripleDesDecrypt(dek3des,
                            trackData.getTrack2Data());

                    /* Generate the MAC for track 2 data. */
                    mTrack2MacString += " ("
                            + toHexString(DukptReceiver.generateMac(macKey,
                            track2Data)) + ")";

                    /* Get the track 2 data as string. */
                    track2DataString = new String(track2Data, 1, trackData.getTrack2Length(), "US-ASCII");

                    /* Divide the track 2 data into fields. */
                    mTrack2Data.fromString(track2DataString);
                }
            }
            catch (GeneralSecurityException e) {
                mErrorId = R.string.message_track_data_error_decrypted;
                System.err.println("Ошибка: " + mErrorId);
            }
            catch (UnsupportedEncodingException e) {
            }

            /* Show the track data. */
            showTrackData();
        }

        /** Shows the track data. */
        private void showTrackData() {
            /* Increment the swipe count. */
            mSwipeCount++;

            mTrackDataSwipeCountPreference        = Integer.toString(mSwipeCount);
            mTrackDataBatteryStatusPreference     = mBatteryStatusString;
            mTrackDataKeySerialNumberPreference   = mKeySerialNumberString;
            mTrackDataTrack1MacPreference         = mTrack1MacString;
            mTrackDataTrack2MacPreference         = mTrack2MacString;

            mTrack1Jis2DataPreference             = mTrack1Data.getJis2Data();
            mTrack1PrimaryAccountNumberPreference = concatString(mTrack1Data.getPrimaryAccountNumber(), mTrack1MaskedData.getPrimaryAccountNumber());
            mTrack1NamePreference                 = concatString(mTrack1Data.getName(), mTrack1MaskedData.getName());
            mTrack1ExpirationDatePreference       = concatString(mTrack1Data.getExpirationDate(), mTrack1MaskedData.getExpirationDate());
            mTrack1ServiceCodePreference          = concatString(mTrack1Data.getServiceCode(), mTrack1MaskedData.getServiceCode());
            mTrack1DiscretionaryDataPreference    = concatString(mTrack1Data.getDiscretionaryData(), mTrack1MaskedData.getDiscretionaryData());

            mTrack2PrimaryAccountNumberPreference = concatString(mTrack2Data.getPrimaryAccountNumber(), mTrack2MaskedData.getPrimaryAccountNumber());
            mTrack2ExpirationDatePreference       = concatString(mTrack2Data.getExpirationDate(), mTrack2MaskedData.getExpirationDate());
            mTrack2ServiceCodePreference          = concatString(mTrack2Data.getServiceCode(), mTrack2MaskedData.getServiceCode());
            mTrack2DiscretionaryDataPreference    = concatString(mTrack2Data.getDiscretionaryData(), mTrack2MaskedData.getDiscretionaryData());

            ServiceFunctions.sendHandlerMessage(handler, ServiceFunctions.HANDLER_MESSAGE_TYPE_CARD_READ_RESULT, mTrack2PrimaryAccountNumberPreference);
        }

        /** Concatenates two strings with carriage return. */
        private String concatString(String string1, String string2) {
            String ret = string1;

            if ((string1.length() > 0) && (string2.length() > 0)) {
                ret += "\n";
            }

            ret += string2;

            return ret;
        }
    }

    /** Converts the battery status to string. */
    private String toBatteryStatusString(int batteryStatus) {
        String batteryStatusString = null;

        switch (batteryStatus) {
            case TrackData.BATTERY_STATUS_LOW:
                batteryStatusString = "Low";
                break;

            case TrackData.BATTERY_STATUS_FULL:
                batteryStatusString = "Full";
                break;

            default:
                batteryStatusString = "Unknown";
                break;
        }

        return batteryStatusString;
    }

    /** Decrypts the data using AES. */
    private byte[] aesDecrypt(byte key[], byte[] input) throws GeneralSecurityException {
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
        IvParameterSpec ivParameterSpec = new IvParameterSpec(new byte[16]);

        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);

        return cipher.doFinal(input);
    }

    /** Converts the HEX string to byte array. */
    private int toByteArray(String hexString, byte[] byteArray) {
        char c = 0;
        boolean first = true;
        int length = 0;
        int value = 0;
        int i = 0;

        for (i = 0; i < hexString.length(); i++) {
            c = hexString.charAt(i);

            if ((c >= '0') && (c <= '9')) {
                value = c - '0';
            }
            else if ((c >= 'A') && (c <= 'F')) {
                value = c - 'A' + 10;
            }
            else if ((c >= 'a') && (c <= 'f')) {
                value = c - 'a' + 10;
            }
            else {
                value = -1;
            }

            if (value >= 0) {
                if (first) {
                    byteArray[length] = (byte) (value << 4);
                }
                else {
                    byteArray[length] |= value;
                    length++;
                }
                first = !first;
            }

            if (length >= byteArray.length) {
                break;
            }
        }

        return length;
    }

    /**Converts the HEX string to byte array. */
    private byte[] toByteArray(String hexString) {
        byte[] byteArray = null;
        int count = 0;
        char c = 0;
        int i = 0;

        boolean first = true;
        int length = 0;
        int value = 0;

        // Count number of hex characters
        for (i = 0; i < hexString.length(); i++) {
            c = hexString.charAt(i);

            if (c >= '0' && c <= '9' || c >= 'A' && c <= 'F' || c >= 'a' && c <= 'f') {
                count++;
            }
        }

        byteArray = new byte[(count + 1) / 2];

        for (i = 0; i < hexString.length(); i++) {
            c = hexString.charAt(i);

            if (c >= '0' && c <= '9') {
                value = c - '0';
            }
            else if (c >= 'A' && c <= 'F') {
                value = c - 'A' + 10;
            }
            else if (c >= 'a' && c <= 'f') {
                value = c - 'a' + 10;
            }
            else {
                value = -1;
            }

            if (value >= 0) {
                if (first) {
                    byteArray[length] = (byte) (value << 4);
                }
                else {
                    byteArray[length] |= value;
                    length++;
                }
                first = !first;
            }
        }

        return byteArray;
    }

    /** Converts the byte array to HEX string. */
    private String toHexString(byte[] buffer) {
        String bufferString = "";

        if (buffer != null) {
            for (int i = 0; i < buffer.length; i++) {

                String hexChar = Integer.toHexString(buffer[i] & 0xFF);

                if (hexChar.length() == 1) {
                    hexChar = "0" + hexChar;
                }

                bufferString += hexChar.toUpperCase(Locale.US) + " ";
            }
        }

        return bufferString;
    }

    /** Decrypts the data using Triple DES. */
    private byte[] tripleDesDecrypt(byte[] key, byte[] input) throws GeneralSecurityException {
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "DESede");
        Cipher cipher = Cipher.getInstance("DESede/CBC/NoPadding");
        IvParameterSpec ivParameterSpec = new IvParameterSpec(new byte[8]);

        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);
        return cipher.doFinal(input);
    }
}
