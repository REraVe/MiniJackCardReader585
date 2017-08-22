/**
 * Created by REraVe on 04.03.2017.
 */
package rerave.activities;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;

import rerave.minijackcardreader.AudioDecoder;
import rerave.minijackcardreader.Bus;
import rerave.minijackcardreader.DBHelper;
import rerave.minijackcardreader.Employee;
import rerave.minijackcardreader.MiniJackReader;
import rerave.minijackcardreader.R;
import rerave.minijackcardreader.Route;
import rerave.minijackcardreader.ServiceFunctions;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1;

    private static int defaultAudioVolume;

    private Activity MA_activity;
    private Context MA_ctx;
    private Handler MA_handler;

    private TextView cardNumberTextView;
    private TextView сardOwnerTextView;
    private TextView recordInfoTextView;

    private Button recordButton;
    private Button currentBusButton;
    private Button archiveButton;
    private Button manualEnterButton;
    private ImageView settingsButton;

    private boolean useNewReader = true;

    private DBHelper dbHelper;
    private MiniJackReader miniJackReader;
    private AudioManager audioManager;

    private String enteredText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializePermission();
        initializeActivity();
        initializeHandler();
        initializeAudio();

        dbHelper       = new DBHelper(this);
        miniJackReader = new MiniJackReader(MA_activity, MA_handler);

        loadEmployeesFromBD();

        checkCurrentBus();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        stopListenJack();

        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,  defaultAudioVolume, 0);

        dbHelper.close();
    }

    private void initializeActivity() {
        MA_activity = this;
        MA_ctx      = this;

        recordButton       = (Button) findViewById(R.id.MA_ButtonStartAudioRecord);
        currentBusButton   = (Button) findViewById(R.id.MA_ButtonCurrentBus);
        archiveButton      = (Button) findViewById(R.id.MA_ButtonArchive);
        manualEnterButton  = (Button) findViewById(R.id.MA_ButtonManualEnter);
        settingsButton     = (ImageView) findViewById(R.id.MA_ButtonSettings);
        recordInfoTextView = (TextView) findViewById(R.id.MA_RecordInfo);
        cardNumberTextView = (TextView) findViewById(R.id.MA_TextViewCardNumber);
        сardOwnerTextView  = (TextView) findViewById(R.id.MA_TextViewCardOwner);

        addClickListeners();

        updateButtonView();
        updateTitle();
    }

    private void addClickListeners() {
        settingsButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                // Bus.getCurrentBus().addPassenger(new Employee("3333333333333", "Mister Z")); // Для тестирования

                Intent settingsIntent = new Intent(MA_ctx, SettingsActivity.class);
                startActivity(settingsIntent);

                overridePendingTransition(R.anim.alpha_in, R.anim.alpha_out);
                return true;
            }
        });

        recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Bus.getCurrentBus() == null) {
                    checkCurrentBus();
                    return;
                }

                if (!MiniJackReader.isListenJackStarted()) {
                    startListenJack();
                    Toast.makeText(MA_ctx, "Механизм чтения карт запущен.", Toast.LENGTH_SHORT).show();
                    showButtonRecordStarted();

                    //Bus.getCurrentBus().addPassenger(new Employee("1111111111111", "Mister X")); // Для тестирования
                }
                else {
                    stopListenJack();
                    Toast.makeText(MA_ctx, "Механизм чтения карт остановлен.", Toast.LENGTH_SHORT).show();
                    showButtonRecordStopped();

                    //Bus.getCurrentBus().addPassenger(new Employee("2222222222222", "Mister Y")); // Для тестирования
                }
            }
        });

        currentBusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Bus.getCurrentBus() == null) {
                    checkCurrentBus();
                    return;
                }

                Intent employeeIntent = new Intent(MA_ctx, EmployeeActivity.class);
                employeeIntent.putExtra("currentBus", true);
                startActivity(employeeIntent);

                overridePendingTransition(R.anim.alpha_in, R.anim.alpha_out);
            }
        });

        archiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent archiveIntent = new Intent(MA_ctx, ArchiveActivity.class);
                archiveIntent.putExtra("currentBus", false);
                archiveIntent.putExtra("busKey", 0);
                startActivity(archiveIntent);

                overridePendingTransition(R.anim.alpha_in, R.anim.alpha_out);
            }
        });

        manualEnterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
                alertDialog.setTitle("РУЧНОЙ ВВОД СОТРУДНИКА");
                alertDialog.setMessage("Введите КПП сотрудника");

                final EditText input = new EditText(MainActivity.this);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT);
                input.setLayoutParams(lp);
                alertDialog.setView(input);
//                alertDialog.setIcon(R.drawable.key);

                alertDialog.setPositiveButton("Ввод",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                enteredText = input.getText().toString() + "?<";
                                processResultNew(enteredText);
                            }
                        });

                alertDialog.setNegativeButton("Отмена",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });

                alertDialog.show();
            }

        });

    }

    private void initializeHandler() {
        MA_handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case ServiceFunctions.HANDLER_MESSAGE_TYPE_CARD_READ_RESULT:
                        if (useNewReader) {
                            processResultNew(msg.obj);
                        } else {
                            processResultOld(msg.obj);
                        }

                        break;

                    case ServiceFunctions.HANDLER_MESSAGE_TYPE_HTTPS_QUERY_RESULT:
                        Toast.makeText(MA_ctx, "Данные по владельцам КПП обновлены!", Toast.LENGTH_SHORT).show();
                        break;

                    case ServiceFunctions.HANDLER_MESSAGE_TYPE_CURRENT_BUS_UPDATED:
                        updateTitle();
                        break;
                }
            }
        };
    }

    private void initializeAudio() {
        audioManager = (AudioManager) MA_activity.getSystemService(Context.AUDIO_SERVICE);
        defaultAudioVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,  audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);
    }

    private void updateButtonView() {
        if (!MiniJackReader.isListenJackStarted()) {
            showButtonRecordStopped();
        } else {
            showButtonRecordStarted();
        }
    }

    private void showButtonRecordStarted() {
        recordButton.setText(getResources().getString(R.string.stop_audio_record));
        recordButton.setBackground(getResources().getDrawable(R.drawable.button_shape_red));

        recordInfoTextView.setText(getResources().getString(R.string.recordStarted));
    }

    private void showButtonRecordStopped() {
        recordButton.setText(getResources().getString(R.string.start_audio_record));
        recordButton.setBackground(getResources().getDrawable(R.drawable.button_shape_green));

        recordInfoTextView.setText(getResources().getString(R.string.recordStopped));
    }

    private void loadEmployeesFromBD() {
        ArrayList<HashMap<String, String>> employeesList = dbHelper.selectRecordsFromEmployeesTable();

        for (HashMap<String, String> currentHashMap : employeesList) {
            new Employee(currentHashMap.get("KPP"), currentHashMap.get("FIO"));
        }
    }

    private void checkCurrentBus() {
        if (Bus.getCurrentBus() == null) {
            ArrayList<HashMap<String, String >> tempList = dbHelper.selectRecordsFromTempTable();

            if (tempList.size() > 0) {
                loadBusFromTempList(tempList);
            }
            else {
                selectBus();
            }
        }
    }

    private void loadBusFromTempList(ArrayList<HashMap<String, String >> tempList) {
        String busKey = tempList.get(0).get("BUS_KEY");

        int routeTimeIndex = Integer.parseInt(busKey.substring(0, 1));
        int routeNameIndex = Integer.parseInt(busKey.substring(1, 2));

        Bus.setCurrentBus(new Bus(new Route(routeTimeIndex, routeNameIndex)), MA_handler);

        for (HashMap<String, String> currentHashMap : tempList) {
            Bus.getCurrentBus().addPassengerOnBus(new Employee(currentHashMap.get("KPP"), currentHashMap.get("FIO")));
        }
    }

    private void selectBus() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this).setCancelable(false);
        builder.setTitle("Выберите маршрут автобуса:");

        final String[] routesNameArray = Route.getRoutesNameList();

        builder.setItems(routesNameArray, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();

                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this).setCancelable(false);
                builder.setTitle("Выберите время отправления:");

                final String selectedRouteName = routesNameArray[which];
                final String[] routesTimeArray = Route.getRoutesTimeList();

                builder.setItems(routesTimeArray, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which1) {
                        dialog.dismiss();

                        Bus.setCurrentBus(new Bus(new Route(routesTimeArray[which1], selectedRouteName)), MA_handler);
                    }
                }).show();
            }
        }).show();
    }

    private void updateTitle() {
        if (Bus.getCurrentBus() == null) {
            this.setTitle("585 Золотой");
        }
        else {
            Route currentRoute = Bus.getCurrentBus().getRoute();
            this.setTitle(currentRoute.getName() + " " + Html.fromHtml("\u231A") + " " + currentRoute.getTime());
        }
    }

    private void startListenJack() {
        if (useNewReader) {
            if (!MiniJackReader.isListenJackStarted()) {
                listenJackNewReader();
            }
        } else {
            if (!AudioDecoder.isScanSignal()) {
                listenJackOldReader();
            }
        }
    }

    private void stopListenJack() {
        if (useNewReader) {
            if (MiniJackReader.isListenJackStarted()) {
                miniJackReader.stopListenJack();
            }
        } else {
            if (AudioDecoder.isScanSignal()) {
                AudioDecoder.setScanSignal(false);
            }
        }
    }

    private void listenJackNewReader() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (!MiniJackReader.isListenJackStarted()) {
                        miniJackReader.startListenJack();
                    }
                } catch (Exception e) {
                    System.err.println("Ошибка чтения данных: " + e);
                }
            }
        }).start();
    }

    private void listenJackOldReader() {
        AudioDecoder.setScanSignal(true);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (AudioDecoder.isScanSignal()) {
                        AudioDecoder audioDecoder = new AudioDecoder(MA_activity, MA_handler);
                        audioDecoder.scanInputSignal();
                    }
                } catch (Exception e) {
                    System.err.println("Ошибка аудио потока: " + e);
                }
            }
        }).start();
    }

    private void processResultNew(Object readedCardNumberObject) {
        String ownerName        = "";
        String readedCardNumber = "";
        String lastSymbols;

        boolean cardValid = false;

        if (readedCardNumberObject instanceof String) {
            readedCardNumber = (String) readedCardNumberObject;

            if (!readedCardNumber.equals("")) {
                lastSymbols = readedCardNumber.substring(readedCardNumber.length() - 2, readedCardNumber.length() - 1);

                if (lastSymbols.equals("?")) {
                    readedCardNumber = readedCardNumber.substring(0, readedCardNumber.length() - 2);
                    cardValid = true;
                }
            }
        }

        if (cardValid) {
            for (Employee currentEmployee : Employee.getEmployeesList()) {
                if (readedCardNumber.equals(currentEmployee.getKpp())) {
                    Bus.getCurrentBus().addPassengerOnBus(currentEmployee);
                    ownerName = currentEmployee.getFio();
                    break;
                }
            }

            if (ownerName.equals("")) {
                ownerName = "Владелец неизвестен";
                Bus.getCurrentBus().addPassengerOnBus(new Employee(readedCardNumber, ownerName));
            }

            cardNumberTextView.setText(readedCardNumber);
            сardOwnerTextView .setText(ownerName);
            cardNumberTextView.setVisibility(View.VISIBLE);
            сardOwnerTextView .setVisibility(View.VISIBLE);
        }
        else {
            Toast.makeText(this, "Номер " + readedCardNumber + " в базе данных не найден!", Toast.LENGTH_SHORT).show();

            cardNumberTextView.setText("");
            сardOwnerTextView .setText("");
            cardNumberTextView.setVisibility(View.INVISIBLE);
            сardOwnerTextView .setVisibility(View.INVISIBLE);
        }
    }

    private void processResultOld(Object lastReadCardNumberObject) {
        String firstSymbol, lastSymbol;
        String ownerName = "";
        String lastReadCardNumber;

        boolean cardValid;

        if (lastReadCardNumberObject instanceof String) {
            lastReadCardNumber = (String) lastReadCardNumberObject;
        }
        else {
            lastReadCardNumber = "";
        }

        firstSymbol = lastReadCardNumber.substring(0, 1);
        lastSymbol = lastReadCardNumber.substring(lastReadCardNumber.length() - 1, lastReadCardNumber.length());

        if (firstSymbol.equals(";") && lastSymbol.equals("?")) {
            lastReadCardNumber = lastReadCardNumber.substring(1, lastReadCardNumber.length() - 1);
            cardValid = true;
        } else {
            cardValid = false;
        }

        Toast.makeText(this, "Результат: " + lastReadCardNumber, Toast.LENGTH_SHORT).show();

        if (cardValid) {
            for (Employee currentEmployee : Employee.getEmployeesList()) {
                if (lastReadCardNumber.equals(currentEmployee.getKpp())) {
                    Bus.getCurrentBus().addPassengerOnBus(currentEmployee);
                    ownerName = currentEmployee.getFio();
                    break;
                }
            }

            if (ownerName.equals("")) {
                ownerName = "Владелец неизвестен";
                Bus.getCurrentBus().addPassengerOnBus(new Employee(lastReadCardNumber, ownerName));
            }

            dbHelper.updateTempDB(Bus.getCurrentBus());
            dbHelper.close();

            cardNumberTextView.setText(lastReadCardNumber);
            сardOwnerTextView .setText(ownerName);
            cardNumberTextView.setVisibility(View.VISIBLE);
            сardOwnerTextView .setVisibility(View.VISIBLE);
        }
        else {
            cardNumberTextView.setText("");
            сardOwnerTextView .setText("");
            cardNumberTextView.setVisibility(View.INVISIBLE);
            сardOwnerTextView .setVisibility(View.INVISIBLE);
        }
    }

    private void initializePermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkPermission()) {
                // Code for above or equal 23 API Oriented Device
                // Create a common Method for both
            } else {
                requestPermission();
            }
        } else {
            // Code for Below 23 API Oriented Device
            // Create a common Method for both
        }
    }

    private boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);

        return result == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
            Toast.makeText(this,
                    "Record Audio permission allows us to read card. Please allow this permission in App Settings.",
                    Toast.LENGTH_LONG).show();
        }
        else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    Log.e("value", "Permission Granted, Now you can use mini jack.");
                else
                    Log.e("value", "Permission Denied, You cannot use mini jack.");
                break;
        }
    }
}

