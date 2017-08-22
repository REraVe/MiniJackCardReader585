/**
 * Created by REraVe on 30.03.2017.
 */
package rerave.minijackcardreader;

import android.os.Handler;
import android.os.Message;
import android.text.Html;

import org.json.JSONArray;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.TimeZone;

import javax.net.ssl.HttpsURLConnection;

public class ServiceFunctions {

    public final static int HANDLER_MESSAGE_TYPE_CARD_READ_RESULT        = 0;
    public final static int HANDLER_MESSAGE_TYPE_HTTPS_QUERY_RESULT      = 1;
    public final static int HANDLER_MESSAGE_TYPE_CURRENT_BUS_UPDATED     = 2;
    public final static int HANDLER_MESSAGE_TYPE_EMPLOYEES_TABLE_UPDATED = 3;

    public static int normalSilenceLevel = 700;

    public static void downloadEmployeesFromPortal(Handler handler, int messageType) {
        try {
            String inputLine;

            URL url = new URL("https://zolotoy.ru/xxxxxxx.php"); // Адрес скрыт, чтобы не нарушать коммерческую тайну организации

            HttpsURLConnection httpsConnection = (HttpsURLConnection) url.openConnection();
            httpsConnection.setRequestMethod("GET");

            BufferedReader reader  = new BufferedReader(new InputStreamReader(httpsConnection.getInputStream()));
            StringBuilder sBuilder = new StringBuilder();

            while ((inputLine = reader.readLine()) != null) {
                sBuilder.append(inputLine);
            }

            reader.close();

            ArrayList<HashMap<String, String>> valuesFromXML = ServiceFunctions.getValuesFromXML(sBuilder.toString());

            Employee.clearEmployeesList();

            for (HashMap<String, String> currentHashMap : valuesFromXML) {
                new Employee(currentHashMap.get("KPP"), currentHashMap.get("FIO"));
            }

            ServiceFunctions.sendHandlerMessage(handler, messageType, null);
        }
        catch (Exception e) {
            System.err.println("Ошибка https соединения: " + e);
        }
    }

    public static void uploadPassengersToPortal(ArrayList<HashMap<String, String>> busesList, ArrayList<HashMap<String, String>> passengersList) {
        try {
            JSONObject jsonToPortal       = new JSONObject();

            JSONArray jsonBusesArray      = new JSONArray();
            JSONArray jsonPassengersArray = new JSONArray();

            for (HashMap<String, String> currentBus : busesList) {
                JSONObject jsonBus = new JSONObject();

                for (HashMap.Entry<String, String> pair : currentBus.entrySet()) {
                    String pairKey   = pair.getKey();
                    String pairValue = pair.getValue();

                    if (pairKey.equals("ROUTE"))
                        pairValue = pairValue.replace(Html.fromHtml("\u27A0"), ">");

                    jsonBus.put(pairKey, pairValue);
                }
                
                jsonBusesArray.put(jsonBus);
            }

            jsonToPortal.put("buses", jsonBusesArray);

            for (HashMap<String, String> currentPassenger : passengersList) {
                JSONObject jsonPassenger = new JSONObject();

                for (HashMap.Entry<String, String> pair : currentPassenger.entrySet()) {
                    jsonPassenger.put(pair.getKey(), pair.getValue());
                }

                jsonPassengersArray.put(jsonPassenger);
            }

            jsonToPortal.put("passengers", jsonPassengersArray);

            String myUrl  = "https://zolotoy.ru/xxxxxxx.php"; // Адрес скрыт, чтобы не нарушать коммерческую тайну организации
            String params = "d=" + jsonToPortal.toString();

            sendDataToPortal(myUrl, params);
        }
        catch (Exception e) {
            System.err.println("Ошибка https соединения: " + e);
        }
    }

    private static void sendDataToPortal(String myUrl, String params) throws IOException {
        String resultString = null;
        byte[] data = null;

        URL url = new URL(myUrl);
        HttpsURLConnection httpsConnection = (HttpsURLConnection) url.openConnection();

        httpsConnection.setRequestMethod("POST");
        httpsConnection.setDoOutput(true);
        httpsConnection.setDoInput(true);
        httpsConnection.setRequestProperty("Content-Length", "" + Integer.toString(params.getBytes().length));

        OutputStream os = httpsConnection.getOutputStream();
        data = params.getBytes("UTF-8");
        os.write(data);
        data = null;

        httpsConnection.connect();

        int responseCode = httpsConnection.getResponseCode();
        InputStream is   = httpsConnection.getInputStream();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        int bytesRead;
        byte[] buffer = new byte[8192];

        while ((bytesRead = is.read(buffer)) != -1) {
            baos.write(buffer, 0, bytesRead);
        }

        data = baos.toByteArray();
        resultString = new String(data, "UTF-8");
    }

    public static ArrayList<HashMap<String, String>> getValuesFromXML(String xmlText) {
        String tegName = "";
        String tegText = "";

        int i     = 0;
        int lasti = 0;

        HashMap<String, String> tegsHashMap = new HashMap<>();
        ArrayList<HashMap<String, String>> tegsHashMapList = new ArrayList<>();

        try {
            XmlPullParser xpp = XmlPullParserFactory.newInstance().newPullParser();
            xpp.setInput(new StringReader(xmlText));

            while (xpp.getEventType() != XmlPullParser.END_DOCUMENT) {
                switch (xpp.getEventType()) {
                    case XmlPullParser.START_DOCUMENT:
                        // Начало документа
                        break;

                    case XmlPullParser.START_TAG:
                        // Начало тэга
                        tegName = xpp.getName().trim();

                        lasti = i;
                        i++;
                        break;

                    case XmlPullParser.END_TAG:
                        // Конец тега (обнуляем временные переменные)
                        tegName = "";
                        tegText = "";

                        i--;
                        break;

                    case XmlPullParser.TEXT:
                        // Содержимое тега
                        tegText = xpp.getText().trim();
                        break;

                    default:
                        break;
                }

                if (!(tegName.equals("") || tegText.equals(""))) {
                    tegsHashMap.put(tegName, tegText);
                }

                if (i < lasti) {
                    tegsHashMapList.add(tegsHashMap);
                    tegsHashMap = new HashMap<>();
                    lasti = i;
                }

                xpp.next();
            }
        } catch (Throwable e) {
            System.err.println("Ошибка при загрузке XML-документа: " + e.toString());
        }

        return tegsHashMapList;
    }

    public static void sendHandlerMessage(Handler handler, int messageType, Object handlerResult) {
        Message msg = Message.obtain();
        msg.what = messageType;

        if (handlerResult != null) {
            msg.obj = handlerResult;
        }

        handler.sendMessage(msg);
    }

    public static String getCurrentDate() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(TimeZone.getTimeZone("Europe/Moscow"));

        StringBuilder sb = new StringBuilder();
        sb.append(calendar.get(Calendar.YEAR));

        if (calendar.get(Calendar.MONTH) + 1 < 10) {
            sb.append("0");
        }
        sb.append(calendar.get(Calendar.MONTH) + 1);

        if (calendar.get(Calendar.DAY_OF_MONTH) < 10) {
            sb.append("0");
        }
        sb.append(calendar.get(Calendar.DAY_OF_MONTH));

        if (calendar.get(Calendar.HOUR_OF_DAY) < 10) {
            sb.append("0");
        }
        sb.append(calendar.get(Calendar.HOUR_OF_DAY));

        if (calendar.get(Calendar.MINUTE) < 10) {
            sb.append("0");
        }
        sb.append(calendar.get(Calendar.MINUTE));

        if (calendar.get(Calendar.SECOND) < 10) {
            sb.append("0");
        }
        sb.append(calendar.get(Calendar.SECOND));

        return sb.toString();
    }

    public static String getFormatedDate(String date) {
        StringBuilder sb = new StringBuilder();

        sb.append(date.substring(6, 8));
        sb.append("-");
        sb.append(date.substring(4, 6));
        sb.append("-");
        sb.append(date.substring(0, 4));
        sb.append(" ");
        sb.append(date.substring(8, 10));
        sb.append(":");
        sb.append(date.substring(10, 12));
        sb.append(":");
        sb.append(date.substring(12, 14));

        return sb.toString();
    }

}
