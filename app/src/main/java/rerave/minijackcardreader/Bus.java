/**
 * Created by REraVe on 01.04.2017.
 */

package rerave.minijackcardreader;

import android.os.Handler;

import java.util.ArrayList;
import java.util.HashMap;

public class Bus {

    private static Bus currentBus;

    private Route route;
    private String date;
    private String busKey;

    private ArrayList<Employee> employeesList = new ArrayList<>();

    public Bus(Route route) {
        this.route  = route;
        this.date   = ServiceFunctions.getCurrentDate();
        this.busKey = generateBusKey(route, this.date);
    }

    public static Bus getCurrentBus() {
        return Bus.currentBus;
    }

    public static void setCurrentBus(Bus currentBus, Handler handler) {
        Bus.currentBus = currentBus;

        ServiceFunctions.sendHandlerMessage(handler, ServiceFunctions.HANDLER_MESSAGE_TYPE_CURRENT_BUS_UPDATED, null);
    }

    public static void clearCurrentBus() {
        Bus.currentBus = null;
    }

    private String generateBusKey(Route route, String date) {
        return route.getRouteKey() + date;
    }

    public Route getRoute() {
        return this.route;
    }

    public String getDate() {
        return this.date;
    }

    public String getBusKey() {
        return this.busKey;
    }

    public void addPassengerOnBus(Employee employee) {
        if (!this.getEmployeesList().contains(employee))
            this.addPassenger(employee);
    }

    public ArrayList<Employee> getEmployeesList() {
        return this.employeesList;
    }

    public void addPassenger(Employee employee) {
        this.employeesList.add(employee);
    }

    public ArrayList<HashMap<String, String>> getEmployeesHashMapList() {
        ArrayList<HashMap<String, String>> employeesHashMapList = new ArrayList<>();

        for (Employee currentEmployee : this.employeesList) {
            HashMap<String, String> employeesHashMap = new HashMap<>();

            employeesHashMap.put("KPP", currentEmployee.getKpp());
            employeesHashMap.put("FIO", currentEmployee.getFio());

            employeesHashMapList.add(employeesHashMap);
        }
        return employeesHashMapList;
    }
}
