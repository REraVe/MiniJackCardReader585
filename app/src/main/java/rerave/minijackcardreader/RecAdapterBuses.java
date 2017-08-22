package rerave.minijackcardreader;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

public class RecAdapterBuses extends RecyclerView.Adapter<RecAdapterBuses.ViewHolder> {

    private ArrayList<HashMap<String, String>> mDataMap;

    public RecAdapterBuses(ArrayList<HashMap<String, String>> dataMap) {
        mDataMap = dataMap;
    }

    // класс view holder-а с помощью которого мы получаем ссылку на каждый элемент отдельного пункта списка
    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView tvData, tvRoute, tvTime;

        public ViewHolder(View v) {
            super(v);

            tvData  = (TextView) v.findViewById(R.id.recData);
            tvRoute = (TextView) v.findViewById(R.id.recRoute);
            tvTime  = (TextView) v.findViewById(R.id.recTime);
        }
    }

    // Создает новые views (вызывается layout manager-ом)
    @Override
    public RecAdapterBuses.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.bus_list, parent, false);

        // тут можно программно менять атрибуты лэйаута (size, margins, paddings и др.)
        return new RecAdapterBuses.ViewHolder(view);
    }

    // Заменяет контент отдельного view (вызывается layout manager-ом)
    @Override
    public void onBindViewHolder(RecAdapterBuses.ViewHolder customHolder, int position) {
        customHolder.tvData .setText(mDataMap.get(position).get("DATE"));
        customHolder.tvRoute.setText(mDataMap.get(position).get("ROUTE"));
        customHolder.tvTime .setText(mDataMap.get(position).get("TIME"));
    }

    // Возвращает размер данных (вызывается layout manager-ом)
    @Override
    public int getItemCount() {
        return mDataMap.size();
    }
}