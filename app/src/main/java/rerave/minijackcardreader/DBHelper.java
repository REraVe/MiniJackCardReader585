/**
 * Created by REraVe on 22.05.2017.
 */
package rerave.minijackcardreader;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;

public class DBHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "Passengers.db";

    private static final String TABLE_NAME_TEMP       = "TEMP";
    private static final String TABLE_NAME_BUSES      = "BUSES";
    private static final String TABLE_NAME_PASSENGERS = "PASSENGERS";
    private static final String TABLE_NAME_EMPLOYEES  = "EMPLOYEES";

    private static final String COLUMN_NAME_NULLABLE = null;

    private static final String COLUMN_NAME_BUS_KEY    = "BUS_KEY";
    private static final String COLUMN_NAME_DATE       = "DATE";
    private static final String COLUMN_NAME_ROUTE_TIME = "TIME";
    private static final String COLUMN_NAME_ROUTE_NAME = "ROUTE";
    private static final String COLUMN_NAME_KPP        = "KPP";
    private static final String COLUMN_NAME_FIO        = "FIO";

    private static final String TEXT_TYPE = " TEXT";
    private static final String COMMA_SEP = ",";

    private static final String SQL_CREATE_TABLE_TEMP =
            "CREATE TABLE " + TABLE_NAME_TEMP + " (" +
                    "ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                    COLUMN_NAME_BUS_KEY + TEXT_TYPE + COMMA_SEP +
                    COLUMN_NAME_KPP + TEXT_TYPE + COMMA_SEP +
                    COLUMN_NAME_FIO + TEXT_TYPE +
                    ")";

    private static final String SQL_CREATE_TABLE_BUSES =
            "CREATE TABLE " + TABLE_NAME_BUSES + " (" +
                    "ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                    COLUMN_NAME_BUS_KEY + TEXT_TYPE + COMMA_SEP +
                    COLUMN_NAME_DATE + TEXT_TYPE + COMMA_SEP +
                    COLUMN_NAME_ROUTE_TIME + TEXT_TYPE + COMMA_SEP +
                    COLUMN_NAME_ROUTE_NAME + TEXT_TYPE +
                    ")";

    private static final String SQL_CREATE_TABLE_PASSENGERS =
            "CREATE TABLE " + TABLE_NAME_PASSENGERS + " (" +
                    "ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                    COLUMN_NAME_BUS_KEY + TEXT_TYPE + COMMA_SEP +
                    COLUMN_NAME_KPP + TEXT_TYPE + COMMA_SEP +
                    COLUMN_NAME_FIO + TEXT_TYPE +
                    ")";

    private static final String SQL_CREATE_TABLE_EMPLOYEES =
            "CREATE TABLE " + TABLE_NAME_EMPLOYEES + " (" +
                    "ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                    COLUMN_NAME_KPP + TEXT_TYPE + COMMA_SEP +
                    COLUMN_NAME_FIO + TEXT_TYPE +
                    ")";

    private static final String SQL_DELETE_TABLE_TEMP =
            "DROP TABLE IF EXISTS " + TABLE_NAME_TEMP;

    private static final String SQL_DELETE_TABLE_BUSES =
            "DROP TABLE IF EXISTS " + TABLE_NAME_BUSES;

    private static final String SQL_DELETE_TABLE_PASSENGERS =
            "DROP TABLE IF EXISTS " + TABLE_NAME_PASSENGERS;

    private static final String SQL_DELETE_TABLE_EMPLOYEES =
            "DROP TABLE IF EXISTS " + TABLE_NAME_EMPLOYEES;

    private final String LOG_TAG = "DB_Logs";

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_TABLE_TEMP);
        Log.d(LOG_TAG, TABLE_NAME_TEMP + " table created");

        db.execSQL(SQL_CREATE_TABLE_BUSES);
        Log.d(LOG_TAG, TABLE_NAME_BUSES + " table created");

        db.execSQL(SQL_CREATE_TABLE_PASSENGERS);
        Log.d(LOG_TAG, TABLE_NAME_PASSENGERS + " table created");

        db.execSQL(SQL_CREATE_TABLE_EMPLOYEES);
        Log.d(LOG_TAG, TABLE_NAME_EMPLOYEES + " table created");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public void addNewRecordIntoDB(Bus bus) {
        if (bus == null)
            return;

        addRowIntoBusesTable(bus);

        for (Employee currentEmployee : bus.getEmployeesList()) {
            addRowIntoPassengersTable(bus, currentEmployee);
        }
    }

    public void addNewEmployeesIntoDB() {
        for (Employee employee : Employee.getEmployeesList()) {
            addRowIntoEmployeesTable(employee);
        }
    }

    private void addRowIntoBusesTable(Bus bus) {
        HashMap<String, String> valuesMap = new HashMap<>();

        valuesMap.put(COLUMN_NAME_BUS_KEY,    bus.getBusKey());
        valuesMap.put(COLUMN_NAME_DATE,       bus.getDate());
        valuesMap.put(COLUMN_NAME_ROUTE_TIME, bus.getRoute().getTime());
        valuesMap.put(COLUMN_NAME_ROUTE_NAME, bus.getRoute().getName());

        addRowIntoTable(TABLE_NAME_BUSES, valuesMap);
    }

    private void addRowIntoPassengersTable(Bus bus, Employee employee) {
        HashMap<String, String> valuesMap = new HashMap<>();

        valuesMap.put(COLUMN_NAME_BUS_KEY, bus.getBusKey());
        valuesMap.put(COLUMN_NAME_KPP, employee.getKpp());
        valuesMap.put(COLUMN_NAME_FIO, employee.getFio());

        addRowIntoTable(TABLE_NAME_PASSENGERS, valuesMap);
    }

    private void addRowIntoEmployeesTable(Employee employee) {
        HashMap<String, String> valuesMap = new HashMap<>();

        valuesMap.put(COLUMN_NAME_KPP, employee.getKpp());
        valuesMap.put(COLUMN_NAME_FIO, employee.getFio());

        addRowIntoTable(TABLE_NAME_EMPLOYEES, valuesMap);
    }

    public void updateTempDB(Bus bus) {
        if (bus == null)
            return;

        clearTempTable();

        SQLiteDatabase db = this.getWritableDatabase();

        for (Employee currentEmployee : bus.getEmployeesList()) {
            ContentValues values = new ContentValues();

            values.put(COLUMN_NAME_BUS_KEY, bus.getBusKey());
            values.put(COLUMN_NAME_KPP,     currentEmployee.getKpp());
            values.put(COLUMN_NAME_FIO,     currentEmployee.getFio());

            long newRowId = db.insert(TABLE_NAME_TEMP, COLUMN_NAME_NULLABLE, values);
            Log.d(LOG_TAG, "row inserted into " + TABLE_NAME_TEMP + " table, ID = " + newRowId);
        }
    }

    private void addRowIntoTable(String tableName, HashMap<String, String> valuesMap) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        for (HashMap.Entry<String, String> pair : valuesMap.entrySet()) {
            values.put(pair.getKey(), pair.getValue());
        }

        long newRowId = db.insert(tableName, COLUMN_NAME_NULLABLE, values);
        Log.d(LOG_TAG, "row inserted into " + tableName + " table, ID = " + newRowId);
    }

    public ArrayList<HashMap<String, String>> selectRecordsFromTable(String tableName, String conditions) {
        ArrayList<HashMap<String, String>> arrayList = new ArrayList<>();

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(tableName, null, conditions, null, null, null, null);

        if (cursor.moveToFirst()) {

            do {
                HashMap<String, String> hashMap = new HashMap<>();

                for (int i = 0; i < cursor.getColumnCount(); i++) {
                    hashMap.put(cursor.getColumnName(i), cursor.getString(i));
                }

                arrayList.add(hashMap);

            } while (cursor.moveToNext());

            Log.d(LOG_TAG, "select " + cursor.getCount() + " rows from " + tableName + " table");
        }
        else
            Log.d(LOG_TAG, "0 rows");

        cursor.close();

        return arrayList;
    }

    public void clearTable(String tableName) {
        SQLiteDatabase db = this.getWritableDatabase();

        int clearCount = db.delete(tableName, null, null);
        Log.d(LOG_TAG, "deleted " + clearCount + " rows from " + tableName + " table");
    }

    public ArrayList<HashMap<String, String>> selectRecordsFromTempTable() {
        return selectRecordsFromTable(TABLE_NAME_TEMP, null);
    }

    public ArrayList<HashMap<String, String>> selectRecordsFromBusesTable() {
        return selectRecordsFromTable(TABLE_NAME_BUSES, null);
    }

    public ArrayList<HashMap<String, String>> selectRecordsFromEmployeesTable() {
        return selectRecordsFromTable(TABLE_NAME_EMPLOYEES, null);
    }

    public ArrayList<HashMap<String, String>> selectRecordsFromPassengersTable(String busKey) {
        return selectRecordsFromTable(TABLE_NAME_PASSENGERS, busKey);
    }

    public ArrayList<HashMap<String, String>> selectRecordsFromPassengersTable() {
        return selectRecordsFromPassengersTable(null);
    }

    public void clearTempTable() {
        clearTable(TABLE_NAME_TEMP);
    }

    public void clearBusesTable() {
        clearTable(TABLE_NAME_BUSES);
    }

    public void clearPassengersTables() {
        clearTable(TABLE_NAME_PASSENGERS);
    }

    public void clearEmployeesTables() {
        clearTable(TABLE_NAME_EMPLOYEES);
    }

}
