package net.bnfour.phone2web2tg_forwarder;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.telephony.TelephonyManager;
import android.text.Html;
import android.util.Log;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;


import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.READ_PHONE_NUMBERS;
import static android.Manifest.permission.READ_PHONE_STATE;
import static android.Manifest.permission.RECEIVE_SMS;
import static android.Manifest.permission.READ_SMS;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.preference.PreferenceManager.getDefaultSharedPreferences;

public class MainPreferencesActivity extends Activity {

    public static String mPhoneNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        SharedPreferences prefs = getDefaultSharedPreferences(this);

        prefs.edit().clear().apply();

        PreferenceManager.setDefaultValues(this, R.xml.preferences, true);

        if (ActivityCompat.checkSelfPermission(this, RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, READ_PHONE_NUMBERS) ==
                        PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            TelephonyManager tMgr = (TelephonyManager)   this.getSystemService(Context.TELEPHONY_SERVICE);
            mPhoneNumber = tMgr.getLine1Number();
            //textView.setText(mPhoneNumber);
            return;
        } else {
            requestPermission();
        }

//        getFragmentManager().beginTransaction()
//        .replace(android.R.id.content, new SettingsFragment())
//        .commit();




        checkForUpdates();




    }
    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{RECEIVE_SMS, READ_SMS, READ_PHONE_STATE, READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE}, 100);
        }
    }
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 100:
                TelephonyManager tMgr = (TelephonyManager)  this.getSystemService(Context.TELEPHONY_SERVICE);
                if (ActivityCompat.checkSelfPermission(this, RECEIVE_SMS) !=
                        PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                        READ_PHONE_NUMBERS) != PackageManager.PERMISSION_GRANTED  &&
                        ActivityCompat.checkSelfPermission(this, READ_PHONE_STATE) !=      PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                mPhoneNumber = tMgr.getLine1Number();
                //textView.setText(mPhoneNumber);
                break;
        }
    }


    private void checkForUpdates() {

        final String[] last_version_raw = new String[1];
        final String[] api_url_raw = new String[1];

        @SuppressLint("StaticFieldLeak") AsyncTask aTask = new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] params) {

                XMLParser parser = new XMLParser();
                String xml = parser.getXmlFromUrl( "https://bankapi-dev.winners888.com/api/sms_version/" ); // getting XML

                Log.e("XML", xml);

                Document doc = null;

                try{
                    doc = parser.getDomElement(xml); // getting DOM element
                }catch( DOMException e ){

                }

                if( doc != null ){
                    NodeList nl = doc.getElementsByTagName( "data" );

                    for (int i = 0; i < nl.getLength(); i++) {
                        HashMap<String, String> map = new HashMap<String, String>();
                        Element e = (Element) nl.item(i);

                        last_version_raw[0] = parser.getValue(e, "lastver");
                        api_url_raw[0] = parser.getValue(e, "api_url");

                    }
                }

                return null;
            }

            @Override
            protected void onPostExecute(Object o) {
                super.onPostExecute(o);

                int appversion = BuildConfig.VERSION_CODE;
                int last_version = Integer.parseInt(last_version_raw[0]);
                String api_url = api_url_raw[0];

                SharedPreferences prefs = getDefaultSharedPreferences(MainPreferencesActivity.this);

                prefs.edit().putString("api_url", api_url).apply();

                //Toast.makeText(getApplicationContext(), String.valueOf( long_appversion ) + "/" + String.valueOf( long_last_version ), Toast.LENGTH_LONG).show();

                if( appversion < last_version ){

                    String dialogstr = Html.fromHtml( "<font color='#ffffff'><b>New version available</b></font>" ).toString();

                    AlertDialog.Builder customBuilder = new AlertDialog.Builder( MainPreferencesActivity.this );

                    customBuilder.setMessage( dialogstr );
                    customBuilder.setNegativeButton( Html.fromHtml("<font color='#ffffff'>Cancel</font>") , (arg0, arg1) -> {});
                    customBuilder.setPositiveButton( Html.fromHtml("<font color='#ffffff'>OK</font>") , (arg0, arg1) -> Update( api_url + "sms_app/app-release.apk" ));
                    AlertDialog dialogUpd = customBuilder.create();
                    dialogUpd.show();

                }

            }

        };

        aTask.execute();

    }

    public void Update(final String apkurl){

        
        //ProgressDialog dialog = ProgressDialog.show(this, "", "Download Update, Please wait...", true);
        final ProgressDialog dialog = new ProgressDialog( this );
        dialog.setIndeterminate( false );
        dialog.setMessage( "Downloading Update" );
        dialog.setCancelable( false );
        dialog.setMax( 5 );
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        //dialog.setIndeterminateDrawable( getResources().getDrawable( R.drawable.menuhilight ) );
        //dialog.setProgressDrawable( getResources().getDrawable(R.drawable.) );
        dialog.show();

        //dialog = ProgressDialog.show(this, "", "Download Update, Please wait...", true);

        @SuppressLint("StaticFieldLeak") AsyncTask updateTask = new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] objects) {

                try {
                    URL url = new URL(apkurl);

                    Log.e("APK URL", url.toString());
                    HttpURLConnection c = (HttpURLConnection) url.openConnection();
                    c.setRequestMethod("GET");
                    c.setDoOutput(false);
                    c.connect();

                    File file = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "winners888");

                    file.mkdirs();
                    File outputFile = new File(file, "app.apk");
                    FileOutputStream fos = new FileOutputStream(outputFile);

                    InputStream is = c.getInputStream();

                    byte[] buffer = new byte[1024];
                    int len1 = 0;
                    while ((len1 = is.read(buffer)) != -1) {

                        // Get length of file in bytes
                        long fileSizeInBytes = outputFile.length();
                        // Convert the bytes to Kilobytes (1 KB = 1024 Bytes)
                        long fileSizeInKB = fileSizeInBytes / 1024;
                        // Convert the KB to MegaBytes (1 MB = 1024 KBytes)
                        long fileSizeInMB = fileSizeInKB / 1024;

                        dialog.setProgress((int) fileSizeInMB);

                        fos.write(buffer, 0, len1);
                    }
                    fos.close();
                    is.close();//till here, it works fine - .apk is download to my sdcard in download file

                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), "Update error!", Toast.LENGTH_LONG).show();
                }

                return null;
            }

            @Override
            protected void onPostExecute(Object o) {
                super.onPostExecute(o);

                File directory = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
                File file = new File(directory, "winners888/app.apk");

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    Uri apkUri = FileProvider.getUriForFile(MainPreferencesActivity.this, BuildConfig.APPLICATION_ID + ".fileprovider", file);
                    Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
                    intent.setData(apkUri);
                    intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(intent);
                } else {
                    Uri apkUri = Uri.fromFile(file);
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }

                finish();

            }
        };

        updateTask.execute();

    }

}

