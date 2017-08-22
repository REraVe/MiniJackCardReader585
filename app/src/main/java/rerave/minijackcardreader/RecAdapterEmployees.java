package rerave.minijackcardreader;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

public class RecAdapterEmployees extends RecyclerView.Adapter<RecAdapterEmployees.ViewHolder> {

    private ArrayList<HashMap<String, String>> mDataMap;

    public RecAdapterEmployees(ArrayList<HashMap<String, String>> dataMap) {
        mDataMap = dataMap;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView tvFIO, tvKPP;

        public ViewHolder(View v) {
            super(v);

            tvFIO = (TextView) v.findViewById(R.id.recFIO);
            tvKPP = (TextView) v.findViewById(R.id.recKPP);
        }
    }

    @Override
    public RecAdapterEmployees.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.emp_list, parent, false);

        return new RecAdapterEmployees.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecAdapterEmployees.ViewHolder customHolder, int position) {
        customHolder.tvFIO.setText(mDataMap.get(position).get("FIO"));
        customHolder.tvKPP.setText(mDataMap.get(position).get("KPP"));
    }

    @Override
    public int getItemCount() {
        return mDataMap.size();
    }
}