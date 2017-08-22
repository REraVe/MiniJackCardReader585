package rerave.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;

import rerave.minijackcardreader.Bus;
import rerave.minijackcardreader.DBHelper;
import rerave.minijackcardreader.Employee;
import rerave.minijackcardreader.R;
import rerave.minijackcardreader.ServiceFunctions;

public class EmployeeActivity extends AppCompatActivity {

    private static SimpleAdapter adapter;

    private DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee);

        ArrayList<HashMap<String, String>> employeesHashMapList;

        if ((getIntent().getExtras().getBoolean("currentBus", false)))
            employeesHashMapList = Bus.getCurrentBus().getEmployeesHashMapList();
        else
            employeesHashMapList = Employee.getEmployeesHashMapList();

        adapter = new SimpleAdapter(this, employeesHashMapList, R.layout.adapter_sa,
                new String[] {"KPP", "FIO"},
                new int[] {R.id.SA_adapter_KPP, R.id.SA_adapter_FIO});

        ListView employeesListView = (ListView) findViewById(R.id.SA_ListViewEmployees);
        employeesListView.setAdapter(adapter);

        // создаем объект для создания и управления версиями БД
        dbHelper = new DBHelper(this);
    }

    @Override
    public void onBackPressed() {
        overridePendingTransition(R.anim.alpha_in, R.anim.alpha_out);
        finish();
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.buttonSQL_add:
                saveBusIntoBD();
                Toast.makeText(this, "Данные успешно сохранены.", Toast.LENGTH_SHORT).show();
                break;

            case R.id.buttonSQL_read:
                ArrayList<HashMap<String,String>> busesList      = dbHelper.selectRecordsFromBusesTable();
                ArrayList<HashMap<String,String>> passengersList = dbHelper.selectRecordsFromPassengersTable();

                ServiceFunctions.uploadPassengersToPortal(busesList, passengersList);
                break;

            case R.id.buttonSQL_clear:
                dbHelper.clearBusesTable();
                dbHelper.clearPassengersTables();
                break;
        }
        dbHelper.close();
    }

    private void saveBusIntoBD() {
        dbHelper.addNewRecordIntoDB(Bus.getCurrentBus());
        dbHelper.clearTempTable();

        Bus.clearCurrentBus();

        overridePendingTransition(R.anim.alpha_in, R.anim.alpha_out);
        finish();
    }
}
