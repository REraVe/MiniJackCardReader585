/**
 * Created by REraVe on 30.03.2017.
 */

package rerave.minijackcardreader;

import java.util.ArrayList;
import java.util.HashMap;

public class Employee {

    private String kpp;
    private String fio;

    private static ArrayList<Employee> employeesList = new ArrayList<>();

    public Employee(String kpp, String fio){
        this.kpp = kpp;
        this.fio = fio;

        employeesList.add(this);
    }

    public static ArrayList<Employee> getEmployeesList() {
        return employeesList;
    }

    public static ArrayList<HashMap<String, String>> getEmployeesHashMapList() {
        ArrayList<HashMap<String, String>> employeesHashMapList = new ArrayList<>();

        for (Employee currentEmployee : employeesList) {
            HashMap<String, String> employeesHashMap = new HashMap<>();

            employeesHashMap.put("KPP", currentEmployee.getKpp());
            employeesHashMap.put("FIO", currentEmployee.getFio());

            employeesHashMapList.add(employeesHashMap);
        }

        return employeesHashMapList;
    }

    public static void clearEmployeesList() {
        employeesList.clear();
    }

    public String getKpp() {
        return kpp;
    }

    public String getFio() {
        return fio;
    }

}
