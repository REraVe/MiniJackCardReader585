/**
 * Created by REraVe on 24.05.2017.
 */

package rerave.minijackcardreader;

import android.text.Html;
import android.text.Spanned;

import java.util.ArrayList;

public class Route {

    private static final Spanned ROUTE_ARROW = Html.fromHtml("\u27A0");

    public static final String ROUTE_TIME_0750 = "07:50";
    public static final String ROUTE_TIME_1710 = "17:10";

    public static final String ROUTE_NAME_GR_TO_OFFICE = "Гражданский пр. " + ROUTE_ARROW + " Офис";
    public static final String ROUTE_NAME_KM_TO_OFFICE = "Комендантский пр. " + ROUTE_ARROW + " Офис";
    public static final String ROUTE_NAME_OFFICE_TO_GR = "Офис " + ROUTE_ARROW + " Гражданский пр.";
    public static final String ROUTE_NAME_OFFICE_TO_KM = "Офис " + ROUTE_ARROW + " Комендантский пр.";

    private static ArrayList<String> routesTimeList = new ArrayList<>();
    private static ArrayList<String> routesNameList = new ArrayList<>();

    private String time;
    private String name;
    private String routeKey;

    static {
        routesTimeList.add(ROUTE_TIME_0750);
        routesTimeList.add(ROUTE_TIME_1710);

        routesNameList.add(ROUTE_NAME_GR_TO_OFFICE);
        routesNameList.add(ROUTE_NAME_KM_TO_OFFICE);
        routesNameList.add(ROUTE_NAME_OFFICE_TO_GR);
        routesNameList.add(ROUTE_NAME_OFFICE_TO_KM);
    }

    public Route (int time, int name) {
        this(routesTimeList.get(time), routesNameList.get(name));
    }

    public Route (String time, String name) {
        this.time     = time;
        this.name     = name;
        this.routeKey = generateRouteKey(time, name);
    }

    public static String[] getRoutesTimeList() {
        return routesTimeList.toArray(new String[routesTimeList.size()]);
    }

    public static String[] getRoutesNameList() {
        return routesNameList.toArray(new String[routesNameList.size()]);
    }

    private String generateRouteKey(String time, String name) {
        return Integer.toString(routesTimeList.indexOf(time) + 1) + Integer.toString(routesNameList.indexOf(name) + 1);
    }

    public String getTime() {
        return this.time;
    }

    public String getName() {
        return this.name;
    }

    public String getRouteKey() {
        return this.routeKey;
    }
}
