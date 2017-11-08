package com.example.niroigensuntharam.elec390application;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.util.ArrayList;

import dmax.dialog.SpotsDialog;

// The AsyncTask is used since the application is doing a web call
public class GetRoomInfoAsync extends AsyncTask<String, Void, Void> {
    private Context mContext;
    private static boolean isConnected = false;
    private String Date;

    public GetRoomInfoAsync(Context context){

        mContext = context;
    }

    @Override
    protected Void doInBackground(String... params) {

        Date = params[0];

        ArrayList<Room> roomsStored = readRooms();
        ArrayList<Application> applicationsStored = readApplications();

        // Clearing all the rooms and available rooms
        MainActivity.Rooms.clear();
        MainActivity.RoomsNowAvailable.clear();
        MainActivity.Applications.clear();

        if ((isConnected && roomsStored == null && applicationsStored == null)
                || !readDate().equals(Date)) {
            try {
                // Looping through all the rooms
                // and getting its information
                for (int i = 0; i < MainActivity.AvailableRooms[0].length; i++) {
                    // Creating an object of the async class
                    //GetRoomInfoAsync getRoomInfoAsync = new GetRoomInfoAsync();

                    // The url will depend on the room number being provided
                    // Executing the async task
                    // Connecting to the website and retrieving the room information
                    Document doc = Jsoup.connect
                            ("https://calendar.encs.concordia.ca/month.php?user=_NUC_LAB_H" +
                                    MainActivity.AvailableRooms[0][i]).get();

                    // Passing in the room number, its capacity and the date
                    Room room = new Room(MainActivity.AvailableRooms[0][i],
                            MainActivity.AvailableRooms[1][i],
                            params[0], doc);

                    // Verifying whether the room is currently available or not
                    Room.VerifyIfAvalaible(room);

                    // Adding the room to the list of rooms
                    MainActivity.Rooms.add(room);
                }

                Document doc1 = Jsoup.connect
                        ("https://aits.encs.concordia.ca/services/software-windows-public-labs").get();

                Element table = doc1.select("table").get(0);
                Elements rows = table.select("tr");

                for (int i = 0; i < rows.size(); i++)
                {
                    Element row = rows.get(i);
                    Elements cols = row.select("td");

                    String applicationName = cols.select("td").get(0).text();

                    String[] rooms = cols.select("td").get(1).text().split(",");

                    Application application = new Application();

                    application.setApplication(applicationName);

                    for (int j = 0; j < rooms.length; j++) {

                        if (rooms.length == 0) {
                            application.AllRooms = true;
                            break;
                        } else if (rooms[j].contains("H")){
                            application.addRoom(rooms[j]);
                        }

                    }

                    MainActivity.Applications.add(application);
                }

                // Connecting to the website and retrieving the room information
                //Document doc = Jsoup.connect(params[0]).get();
            } catch (IOException ex) {
            }
        }

        else{

            for (int i = 0; i < roomsStored.size(); i++) {

                MainActivity.Rooms.add(roomsStored.get(i));

                // Verifying whether the room is currently available or not
                Room.VerifyIfAvalaible(roomsStored.get(i));
            }

            for (int i = 0; i < applicationsStored.size(); i++)
            {
                MainActivity.Applications.add(applicationsStored.get(i));
            }
        }

        return null;
    }

    @Override
    protected void onPreExecute(){
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);

        if (isConnected) {

            if (MainActivity.swipeRefreshLayout.isRefreshing())
                MainActivity.swipeRefreshLayout.setRefreshing(false);

            Toast.makeText(mContext, "Done", Toast.LENGTH_SHORT).show();

            Room.EarliestAvailableTime();

            Room.SortRooms();

            MainActivity.dialog.dismiss();

            //MainActivity.currentRoom = MainActivity.RoomsNowAvailable.get(0);

            MainActivity.myCustomAdapter.notifyDataSetChanged();

            saveRooms();

            saveApplications();

            saveDate();
        }
        else {
            Toast.makeText(mContext, "Not connected to the internet", Toast.LENGTH_SHORT).show();

            MainActivity.swipeRefreshLayout.setRefreshing(false);

            MainActivity.dialog.dismiss();
        }

        //NotificationHelper notificationHelper = new NotificationHelper(mContext);
    }

    @Override
    protected void onProgressUpdate(Void... values) {
        super.onProgressUpdate(values);
    }

    public static void isNetworkAvailable(Context context)
    {
        isConnected = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo() != null;
    }

    private void saveRooms() {

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        Gson gson = new Gson();

        String json = gson.toJson(MainActivity.Rooms);

        editor.putString("Rooms",json);
        editor.apply();
    }

    private void saveDate() {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        Gson gson = new Gson();

        String json = gson.toJson(Date);

        editor.putString("Date",json);
        editor.apply();
    }

    private void saveApplications() {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        Gson gson = new Gson();

        String json = gson.toJson(Date);

        editor.putString("Date",json);
        editor.apply();
    }

    private String readDate() {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        Gson gson = new Gson();
        String json = sharedPrefs.getString("Date", null);
        Type type = new TypeToken<String>() {}.getType();
        String date = gson.fromJson(json, type);

        return date;
    }

    private ArrayList<Application> readApplications(){
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        Gson gson = new Gson();
        String json = sharedPrefs.getString("Applications", null);
        Type type = new TypeToken<ArrayList<Application>>() {}.getType();
        ArrayList<Application> arrayList = gson.fromJson(json, type);

        return arrayList;
    }

    private ArrayList<Room> readRooms()
    {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        Gson gson = new Gson();
        String json = sharedPrefs.getString("Rooms", null);
        Type type = new TypeToken<ArrayList<Room>>() {}.getType();
        ArrayList<Room> arrayList = gson.fromJson(json, type);

        return arrayList;
    }
}