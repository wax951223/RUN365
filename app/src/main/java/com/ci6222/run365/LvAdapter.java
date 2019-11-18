package com.ci6222.run365;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

public class LvAdapter extends BaseAdapter {

    private LayoutInflater inflater;
    private ArrayList<Float> distList;
    private ArrayList<String> timeList;
    private ArrayList<Float> avgPaceList;
    private ArrayList<String> dateList;
    private ArrayList<Integer> indexlist;


    LvAdapter(Context context, ArrayList<Float> dist, ArrayList<String> time,
              ArrayList<Float> avgPace, ArrayList<String> date) {
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        distList = dist;
        timeList = time;
        avgPaceList = avgPace;
        dateList = date;

    }

    @Override
    public int getCount() {
        return distList.size();
    }

    @Override
    public Object getItem(int i) {
        return distList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {

        View v = inflater.inflate(R.layout.listview_detail, null);
        TextView distDetail = v.findViewById(R.id.distDetailTV);
        TextView timeDetail = v.findViewById(R.id.timeDetailTV);
        TextView avgPaceDetail = v.findViewById(R.id.avgPaceDetailTV);
        TextView dateDetail = v.findViewById(R.id.dateDetailTV);
        // Displaying list in reverse chronological order (list.size() - 1 - i)
        String dist = String.format(Locale.getDefault(), "Distance: %.2f km",
                distList.get(distList.size() - 1 - i));
        String time = "Time: " + timeList.get(timeList.size() - 1 - i);
        String avgP = String.format(Locale.getDefault(), "Pace: %s /km",
                convertDecimalToMins(avgPaceList.get(avgPaceList.size() - 1 - i)));
        String date = dateList.get(dateList.size() - 1 - i);

        distDetail.setText(dist);
        timeDetail.setText(time);
        avgPaceDetail.setText(avgP);
        dateDetail.setText(date);
        return v;
    }

    private String convertDecimalToMins(float decimal) {
        int mins = (int) Math.floor(decimal);
        double fractional = decimal - mins;
        int secs = (int) Math.round(fractional * 60);
        return String.format(Locale.getDefault(), "%d:%02d", mins, secs);
    }

}
