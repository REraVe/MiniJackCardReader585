package rerave.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.HashMap;

import rerave.minijackcardreader.DBHelper;
import rerave.minijackcardreader.R;
import rerave.minijackcardreader.RecAdapterBuses;
import rerave.minijackcardreader.RecAdapterEmployees;

public class ArchiveActivity extends AppCompatActivity {

    private Context AA_ctx;
    private View ChildView;

    private int RecyclerViewItemPosition;
    private boolean isCurrentBus = false;
    private String busKey = "0";

    private ArrayList<HashMap<String, String>> busesMap;

    private DBHelper dbHelper = new DBHelper(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_archive);

        getExtra();
        initializeActivity();
    }

    private void initializeActivity() {
        AA_ctx = this;

        if (!isCurrentBus) {
            showBuses();
        }
        else {
            showClickedBus();
        }
    }

    private void getExtra() {
        isCurrentBus = getIntent().getExtras().getBoolean("currentBus");
        busKey       = getIntent().getExtras().getString("busKey");

        //Toast.makeText(this, "BusKey: " + busKey, Toast.LENGTH_SHORT).show();
    }

    private void showBuses() {
        RecyclerView listView = (RecyclerView) findViewById(R.id.AR_rec_busList);

        busesMap = dbHelper.selectRecordsFromBusesTable();

        RecAdapterBuses adapter = new RecAdapterBuses(busesMap);
        listView.setAdapter(adapter);

        listView.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            GestureDetector gestureDetector = new GestureDetector(AA_ctx, new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onSingleTapUp(MotionEvent motionEvent) {
                    return true;
                }
            });

            @Override
            public boolean onInterceptTouchEvent(RecyclerView Recyclerview, MotionEvent motionEvent) {
                ChildView = Recyclerview.findChildViewUnder(motionEvent.getX(), motionEvent.getY());

                if (ChildView != null && gestureDetector.onTouchEvent(motionEvent)) {
                    RecyclerViewItemPosition = Recyclerview.getChildAdapterPosition(ChildView);

                    //Toast.makeText(AA_ctx, "Item: " + RecyclerViewItemPosition, Toast.LENGTH_SHORT).show();

                    Intent archiveIntent = new Intent(AA_ctx, ArchiveActivity.class);
                    archiveIntent.putExtra("currentBus", true);
                    archiveIntent.putExtra("busKey",     busesMap.get(RecyclerViewItemPosition).get("BUS_KEY"));

                    startActivity(archiveIntent);
                    overridePendingTransition(R.anim.alpha_in, R.anim.alpha_out);
                }
                return false;
            }

            @Override
            public void onTouchEvent(RecyclerView Recyclerview, MotionEvent motionEvent) {}

            @Override
            public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {}
        });
    }

    private void showClickedBus() {
        RecyclerView listView = (RecyclerView) findViewById(R.id.AR_rec_busList);

        ArrayList<HashMap<String, String>> passengersMap = dbHelper.selectRecordsFromPassengersTable(("BUS_KEY = " + busKey));

        RecAdapterEmployees adapter = new RecAdapterEmployees(passengersMap);
        listView.setAdapter(adapter);
    }
}
