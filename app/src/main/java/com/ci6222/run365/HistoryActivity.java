package com.ci6222.run365;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

public class HistoryActivity extends AppCompatActivity {

    // Database Columns
    // Columns are indexed starting with 0, COL 0 is ID
    private static final int COL1 = 1;
    private static final int COL2 = 2;
    private static final int COL3 = 3;
    private static final int COL4 = 4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        final ListView listView = findViewById(R.id.listView);
        final DbHandler databaseHelper = new DbHandler(this);
        ArrayList<Float> distList = new ArrayList<>();
        ArrayList<String> timeList = new ArrayList<>();
        ArrayList<Float> avgPaceList = new ArrayList<>();
        ArrayList<String> dateList = new ArrayList<>();

        Cursor contents = databaseHelper.getContents();

        if (contents.getCount() == 0) {
            Toast.makeText(this, "No activities saved", Toast.LENGTH_SHORT).show();
        } else {
            while (contents.moveToNext()) {
                distList.add(contents.getFloat(COL1));
                timeList.add(contents.getString(COL2));
                avgPaceList.add(contents.getFloat(COL3));
                dateList.add(contents.getString(COL4));

            }
        }

        final LvAdapter itemAdapter = new LvAdapter(this,
                distList,
                timeList,
                avgPaceList,
                dateList);
        listView.setAdapter(itemAdapter);


        FloatingActionButton DeleteAllButton = findViewById(R.id.deleteallbtn);
        DeleteAllButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDialogue();
//                databaseHelper.deleteAll();
//                listView.setAdapter(null);
//                itemAdapter.notifyDataSetChanged();

            }

            private void showDialogue() {
                final AlertDialog.Builder Dialogue =
                        new AlertDialog.Builder(HistoryActivity.this);
                Dialogue.setIcon(R.drawable.warning);
                Dialogue.setTitle("Warning");
                Dialogue.setMessage("All records will be deleted, continue?");
                Dialogue.setPositiveButton("Confirm",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                databaseHelper.deleteAll();
                                listView.setAdapter(null);
                                itemAdapter.notifyDataSetChanged();
                            }
                        });
                Dialogue.setNegativeButton(android.R.string.cancel, null);
                Dialogue.show();
            }

        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Intent intent = new Intent(this, MapsActivity.class);
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            finish();
            intent.addCategory(Intent.CATEGORY_HOME);
            startActivity(intent);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }


}