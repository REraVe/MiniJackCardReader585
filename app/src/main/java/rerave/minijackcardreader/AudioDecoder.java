/**
 * Created by REraVe on 04.03.2017.
 */
package rerave.minijackcardreader;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.BitSet;

public class AudioDecoder {

    private static boolean scanSignal = false;

    private Activity activity;
    private Handler handler;

    private AudioRecord audioRecord;

    private int audioSource       = MediaRecorder.AudioSource.MIC;  // Откуда ведётся запись. В нашем случае это микрофон (mini jack)
    private int sampleRateInHz    = 22050;                          // Частота дискретизации в герцах. Документация утверждает, что 44100Гц поддерживается всеми устройствами
    private int channelConfig     = AudioFormat.CHANNEL_IN_MONO;    // Конфигурация каналов. Может быть CHANNEL_IN_MONO или CHANNEL_IN_STEREO. Моно работает везде
    private int audioFormat       = AudioFormat.ENCODING_PCM_16BIT; // Формат входных данных, более известный как кодек. Может быть ENCODING_PCM_16BIT или ENCODING_PCM_8BIT
    private int bufferSizeInBytes = AudioRecord.getMinBufferSize(this.sampleRateInHz, this.channelConfig, this.audioFormat) * 2;  // Размер внутреннего буфера, в который записываются данные

    private int normalSilenceLevel = ServiceFunctions.normalSilenceLevel;
    private int minSilenceLevel    = normalSilenceLevel;
    private double minLevelCoeff   = 0.5d;

    private boolean isRecording;
    private byte[] audioBytes;


    public AudioDecoder(Activity activity, Handler handler) {
        this.activity = activity;
        this.handler  = handler;
    }

    public static boolean isScanSignal() {
        return AudioDecoder.scanSignal;
    }

    public static void setScanSignal(boolean scanSignal) {
        AudioDecoder.scanSignal = scanSignal;
    }

    private boolean initializeRecording() {
        // Инициализируем объект audioRecord
        this.audioRecord = new AudioRecord(audioSource, sampleRateInHz, channelConfig, audioFormat, bufferSizeInBytes);

        // Проверяем, удалось ли инициализировать объект audioRecord
        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            printLogMessage("Ошибка инициализации объекта AudioRecord!", true);
            return false;
        }
        else
            printLogMessage("Объект AudioRecord инициализирован.", false);
        return true;
    }

    private void startRecording() {
        // Начинаем запись аудио сигнала
        try {
            this.audioRecord.startRecording();
            this.isRecording = true;
        }
        catch(IllegalStateException e) {
            printLogMessage("Ошибка запуска записи: " + e, true);
        }
    }

    private void stopRecording() {
        // Останавливаем запись аудио сигнала
        try {
            this.audioRecord.stop();
        }
        catch(IllegalStateException e) {
            printLogMessage("Ошибка остановки записи: " + e, true);
        }
        finally {
            // освобождаем ресурсы
            this.audioRecord.release();
            this.audioRecord = null;
            this.isRecording = false;
        }
    }

    public void scanInputSignal() {
        short[] audioData = new short[this.bufferSizeInBytes];

        int audioDataReadResult = 0;
        boolean endOfSilence    = false;

        if (!initializeRecording())
            return;

        startRecording();

        while (this.isRecording && !endOfSilence && isScanSignal()) {
            int found = 0;
            boolean silenceNow;

            // Считываем партию аудио данных с помощью объекта audioRecord (в данном случае входящий сигнал с мини джека)
            audioDataReadResult = this.audioRecord.read(audioData, 0, this.bufferSizeInBytes);

            if (audioDataReadResult == AudioRecord.ERROR_INVALID_OPERATION || audioDataReadResult == AudioRecord.ERROR_BAD_VALUE) {
                printLogMessage("Ошибка считывания звукового сигнала", true);
                return;
            }

            // Проверяем, был ли входящий сигнал в считанных аудио данных
            for (int i = 0; i < audioDataReadResult; i++) {
                silenceNow = (Math.abs(audioData[i]) < this.normalSilenceLevel);

                if (!silenceNow) {
                    if (++found > 5) {
                        endOfSilence = true;
                        break;
                    }
                }
            }
        }

        if (endOfSilence) {
            try {
                // Появился сигнал, начинаем его записывать в память
                recordData(audioData, audioDataReadResult);
                printLogMessage("Карта считана!!!", false);
            }
            catch (IOException e) {
                printLogMessage("Ошибка записи звукового сигнала: " + e, true);
            }
        }

        stopRecording();
    }

    private void recordData(short[] initialBuffer, int initialBufferSize) throws IOException {
        short[] audioData = new short[this.bufferSizeInBytes];

        int audioDataReadResult   = 0;
        int silenceAtEndThreshold = this.sampleRateInHz;
        int silentSamples         = 0;
        int maxSamples            = this.sampleRateInHz * 10;
        int totalSamples          = 0;
        int nonSilentAtEndFound   = 0;
        boolean endOfSignal       = false;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos       = new DataOutputStream(new BufferedOutputStream(baos));

        for (int i = 0; i < initialBufferSize; i++) {
            dos.writeShort(initialBuffer[i]);
        }

        while (!endOfSignal) {
            boolean silenceNow;

            if (!this.isRecording || totalSamples >= maxSamples) {
                break;
            }

            // Считываем партию аудио данных с помощью объекта audioRecord (в данном случае входящий сигнал с мини джека)
            audioDataReadResult = this.audioRecord.read(audioData, 0, this.bufferSizeInBytes);

            if (audioDataReadResult == AudioRecord.ERROR_INVALID_OPERATION || audioDataReadResult == AudioRecord.ERROR_BAD_VALUE) {
                printLogMessage("Ошибка считывания звукового сигнала", true);
                return;
            }

            for (int i = 0; i < audioDataReadResult; i++) {
                silenceNow = Math.abs(audioData[i]) < this.normalSilenceLevel;
                dos.writeShort(audioData[i]);

                if (silenceNow) {
                    nonSilentAtEndFound = 0;

                    if (++silentSamples > silenceAtEndThreshold)
                        endOfSignal = true;

                } else {
                    if (++nonSilentAtEndFound > 5)
                        silentSamples = 0;
                }
                totalSamples++;
            }
        }

        dos.close();

        if (this.isRecording)
            this.audioBytes = baos.toByteArray();
            processData(this.audioBytes);

    }

    private void processData(byte[] bytes) throws IOException {
        this.minSilenceLevel = getMinLevel(bytes, this.minLevelCoeff);

        BitSet bits = decodeToBitSet(bytes);

        String result = decodeToASCII(bits);
        if (result.equals("BadRead"))
            result = decodeToASCII(reverse(bits));

        System.err.println("Результат:  " + result);

        ServiceFunctions.sendHandlerMessage(this.handler, ServiceFunctions.HANDLER_MESSAGE_TYPE_CARD_READ_RESULT, result);
    }

    private int getMinLevel(byte[] bytes, double coeff) throws IOException {
        short lastval = (short) 0;
        int peakcount = 0;
        int peaksum   = 0;
        int peaktemp  = 0;
        boolean hitmin = false;

        DataInputStream dis = new DataInputStream(new BufferedInputStream(new ByteArrayInputStream(this.audioBytes)));

        while (dis.available() > 0) {
            short val = dis.readShort();

            if (val > (short) 0 && lastval <= (short) 0) {
                peaktemp = 0;
                hitmin = false;
            } else if (val < (short) 0 && lastval >= (short) 0 && hitmin) {
                peaksum += peaktemp;
                peakcount++;
            }

            if (val > (short) 0 && lastval > val && lastval > this.normalSilenceLevel && val > peaktemp) {
                hitmin = true;
                peaktemp = val;
            }
            lastval = val;
        }

        if (peakcount <= 0)
            return this.normalSilenceLevel;

        return (int) Math.floor(((double) (peaksum / peakcount)) * coeff);
    }

    private BitSet decodeToBitSet(byte[] bytes) throws IOException {
        System.err.println("bytes length: " + bytes.length);

        BitSet result = new BitSet();
        DataInputStream dis = new DataInputStream(new BufferedInputStream(new ByteArrayInputStream(bytes)));

        int i                 = 0;
        int lasti             = 0;
        int first             = 0;
        int lastSign          = -1;
        int oneInterval       = -1;
        int discardCount      = 0;
        int resultBitCount    = 0;
        int expectedParityBit = 1;

        boolean needHalfOne = false;

        while (dis.available() > 0) {
            short dp = dis.readShort();

            if (dp * lastSign < 0 && Math.abs(dp) > this.minSilenceLevel) {
                if (first != 0) {
                    if (discardCount >= 1) {
                        int sinceLast = i - lasti;

                        if (oneInterval != -1) {
                            if (!isOne(sinceLast, oneInterval)) {
                                oneInterval = sinceLast / 2;

                                if (needHalfOne)
                                    break;

                                result.set(resultBitCount, false);
                                resultBitCount++;
                            } else {
                                oneInterval = sinceLast;

                                if (needHalfOne) {
                                    expectedParityBit = 1 - expectedParityBit;
                                    result.set(resultBitCount, true);
                                    resultBitCount++;
                                    needHalfOne = false;
                                } else {
                                    needHalfOne = true;
                                }
                            }
                        } else {
                            oneInterval = sinceLast / 2;
                        }
                    } else {
                        discardCount++;
                    }
                } else {
                    first = i;
                    System.err.println("set first to: " + first);
                }
                lasti = i;
                lastSign *= -1;
            }
            i++;
        }

        dis.close();
        System.err.println("row binary: " + dumpString(result));

        return result;
    }

    private String decodeToASCII(BitSet bits) {

        int firstBit = bits.nextSetBit(0);

        if (firstBit < 0) {
            System.err.println("Не найден первый бит");
            return "BadRead";
        }

        System.err.println("Позиция первого бита: " + firstBit);

        int sentinel = 0;
        int exp = 0;
        int i = firstBit;

        while (i < firstBit + 4) {
            if (bits.get(i))
                sentinel += 1 << exp;

            exp++;
            i++;
        }

        System.err.println("sentinel value for 4 bit: " + sentinel);
        if (sentinel == 11) {
            return decodeToASCII(bits, firstBit, 4, 48);
        }

        while (i < firstBit + 6) {
            if (bits.get(i))
                sentinel += 1 << exp;

            exp++;
            i++;
        }

        System.err.println("sentinel value for 6 bit: " + sentinel);
        if (sentinel == 5) {
            return decodeToASCII(bits, firstBit, 6, 32);
        }

        System.err.println("could not match sentinel value to either 11 or 5 magic values");
        return "BadRead";
    }

    private String decodeToASCII(BitSet bits, int beginIndex, int bitsPerChar, int baseChar) {

        StringBuilder sb = new StringBuilder();

        int i = beginIndex;
        int charCount = 0;
        int size = bits.size();

        boolean sentinelFound = false;

        while (i < size && !sentinelFound) {
            int letterVal = 0;
            boolean expectedParity = true;
            int exp = 0;
            int nextCharIndex = i + bitsPerChar;

            while (i < nextCharIndex) {
                if (bits.get(i)) {
                    letterVal += 1 << exp;
                    expectedParity = !expectedParity;
                }
                exp++;
                i++;
            }

            char letter = decode(letterVal, baseChar);
            sb.append(letter);

            if (bits.get(i) != expectedParity) {
                System.err.println("bad char index: " + charCount);
            }

            i++;
            charCount++;

            if (letter == '?') {
                sentinelFound = true;
            }
        }

        return sb.toString();
    }

    private BitSet reverse(BitSet bits) {
        int size = bits.size();
        BitSet toReturn = new BitSet(size);

        for (int i = 0; i < size; i++) {
            toReturn.set(i, bits.get((size - 1) - i));
        }
        return toReturn;
    }

    private char decode(int input, int baseChar) {
        return (char) (input + baseChar);
    }

    private boolean isOne(int actualInterval, int oneInterval) {
        return Math.abs(actualInterval - oneInterval) < Math.abs(actualInterval - oneInterval * 2);
    }

    private String dumpString(BitSet bits) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < bits.size(); i++) {
            if (bits.get(i))
                sb.append("1");
            else
                sb.append("0");
        }
        return sb.toString();
    }

    private void printLogMessage(String message, boolean toast) {
        System.err.println(message);

        if (toast)
            Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
    }

}
