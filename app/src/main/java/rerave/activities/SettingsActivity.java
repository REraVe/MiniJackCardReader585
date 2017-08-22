package rerave.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;

import rerave.minijackcardreader.DBHelper;
import rerave.minijackcardreader.R;
import rerave.minijackcardreader.ServiceFunctions;

public class SettingsActivity extends AppCompatActivity {

    private Activity SA_activity;
    private Handler SA_handler;

    private EditText silenceLevelEditText;

    private DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        initializeActivity();

        initializeHandler();
    }

    @Override
    public void onBackPressed() {
        overridePendingTransition(R.anim.alpha_in, R.anim.alpha_out);
        finish();
    }

    private void initializeActivity() {
        SA_activity = this;

        dbHelper = new DBHelper(this);

        silenceLevelEditText = (EditText) findViewById(R.id.SA_EditTextSilenceLevel);
        silenceLevelEditText.setText(ServiceFunctions.normalSilenceLevel + "");

        this.setTitle("Настройки");
    }

    private void initializeHandler() {
        SA_handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == ServiceFunctions.HANDLER_MESSAGE_TYPE_HTTPS_QUERY_RESULT) {
                    Toast.makeText(SA_activity, "Данные по владельцам КПП загружены с Портала!", Toast.LENGTH_SHORT).show();

                    updateEmployeesInDB();
                }
                else if (msg.what == ServiceFunctions.HANDLER_MESSAGE_TYPE_EMPLOYEES_TABLE_UPDATED)
                    Toast.makeText(SA_activity, "Данные по владельцам КПП обновлены в базе данных!", Toast.LENGTH_SHORT).show();
            }
        };
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.SA_ButtonDownloadKPPFromPortal:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        ServiceFunctions.downloadEmployeesFromPortal(SA_handler, ServiceFunctions.HANDLER_MESSAGE_TYPE_HTTPS_QUERY_RESULT);
                    }
                }).start();
                break;

            case R.id.SA_ButtonOpenStaffersList:
                Intent employeeIntent = new Intent(this, EmployeeActivity.class);
                employeeIntent.putExtra("currentBus", false);
                startActivity(employeeIntent);

                overridePendingTransition(R.anim.alpha_in, R.anim.alpha_out);
                finish();
                break;

            case R.id.SA_ButtonUploadInfoToPortal:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        ArrayList<HashMap<String,String>> busesList      = dbHelper.selectRecordsFromBusesTable();
                        ArrayList<HashMap<String,String>> passengersList = dbHelper.selectRecordsFromPassengersTable();

                        ServiceFunctions.uploadPassengersToPortal(busesList, passengersList);
                    }
                }).start();
                break;

            case R.id.SA_ButtonWriteSilenceLevel:
                ServiceFunctions.normalSilenceLevel = Integer.parseInt(silenceLevelEditText.getText().toString());
                silenceLevelEditText.clearFocus();
                break;
        }
    }

    private void updateEmployeesInDB() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                dbHelper.addNewEmployeesIntoDB();

                ServiceFunctions.sendHandlerMessage(SA_handler, ServiceFunctions.HANDLER_MESSAGE_TYPE_EMPLOYEES_TABLE_UPDATED, null);
            }
        }).start();
    }

}
