package com.sac.speechdemo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.telephony.PhoneStateListener;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog.Builder;
import com.afollestad.materialdialogs.Theme;
import com.example.user.speechrecognizationasservice.R;

import java.util.HashSet;

/**
 * Created by dell on 18/3/19.
 */

public class MainActivityAlternate extends AppCompatActivity implements ServiceCallbacks{
    private Button btStartService;
    private TextView tvText;
    boolean mBounded;
    MyServiceAlternate mService;
    Intent callIntent;
    String aNameFromContacts[];
    String aNumberFromContacts[];
    TtsProviderFactory ttsProviderImpl = TtsProviderFactory.getInstance();
    MainActivityAlternate myContext;

    private final int OBJECT_DETECTION_ACTIVITY = 1;

    @SuppressLint("SetTextI18n")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        myContext = this;

        btStartService = (Button) findViewById(R.id.btStartService);
        tvText = (TextView) findViewById(R.id.tvText);

        if (ttsProviderImpl != null) {
            ttsProviderImpl.init(this);
        }
        //Some devices will not allow background service to work, So we have to enable autoStart for the app.
        //As per now we are not having any way to check autoStart is enable or not,so better to give this in LoginArea,
        //so user will not get this popup again and again until he logout

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            Log.i("check", "in if");
            requestForCallPermission(new String[]{Manifest.permission.READ_CONTACTS});
        }else {
            Log.i("check", "in else");
            if(aNameFromContacts == null) {
                requestForCallPermission(new String[]{Manifest.permission.READ_CONTACTS});
                /*Cursor contacts = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
                aNameFromContacts = new String[contacts.getCount()];
                aNumberFromContacts = new String[contacts.getCount()];
                int j = 0;
                if (contacts != null) {
                    try {
                        HashSet<String> normalizedNumbersAlreadyFound = new HashSet<>();
                        int indexOfNormalizedNumber = contacts.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER);
                        int indexOfDisplayName = contacts.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                        int indexOfDisplayNumber = contacts.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);

                        while (contacts.moveToNext()) {
                            String normalizedNumber = contacts.getString(indexOfNormalizedNumber);
                            if (normalizedNumbersAlreadyFound.add(normalizedNumber)) {
                                String displayName = contacts.getString(indexOfDisplayName);
                                aNameFromContacts[j] = displayName;
                                String displayNumber = contacts.getString(indexOfDisplayNumber);
                                aNumberFromContacts[j] = displayNumber;
                                j++;
                                //haven't seen this number yet: do something with this contact!
                            } else {
                                //don't do anything with this contact because we've already found this number
                            }
                        }
                    } finally {
                        contacts.close();
                    }
                }*/

                //contacts.close();
            }
        }
        if(aNameFromContacts != null) {
            for (int i = 0; i < aNameFromContacts.length; i++)
                Log.i("contacts", aNameFromContacts[i]);
        }

        enableAutoStart();

        if (checkServiceRunning()) {
            btStartService.setText(getString(R.string.stop_service));
            tvText.setVisibility(View.VISIBLE);
        }

        btStartService.setOnClickListener(v -> {
            if (btStartService.getText().toString().equalsIgnoreCase(getString(R.string.start_service))) {
                //onStart();

                Intent mIntent = new Intent(this, MyServiceAlternate.class);
                startService(mIntent);
                bindService(mIntent, mConnection, BIND_AUTO_CREATE);

                btStartService.setText(getString(R.string.stop_service));
                tvText.setVisibility(View.VISIBLE);
            } else {
                if (mBounded) {
                    unbindService(mConnection);
                    mBounded = false;
                }
                Intent intent = new Intent(MainActivityAlternate.this,
                        MyServiceAlternate.class);
                stopService(intent);
                btStartService.setText(getString(R.string.start_service));
                tvText.setVisibility(View.GONE);
            }
        });
    }

    private void enableAutoStart() {
        for (Intent intent : Constants.AUTO_START_INTENTS) {
            if (getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null) {
                new Builder(this).title(R.string.enable_autostart)
                        .content(R.string.ask_permission)
                        .theme(Theme.LIGHT)
                        .positiveText(getString(R.string.allow))
                        .onPositive((dialog, which) -> {
                            try {
                                for (Intent intent1 : Constants.AUTO_START_INTENTS)
                                    if (getPackageManager().resolveActivity(intent1, PackageManager.MATCH_DEFAULT_ONLY)
                                            != null) {
                                        startActivity(intent1);
                                        break;
                                    }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        })
                        .show();
                break;
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        /*Log.i("MainActivity", "OnStart");
        Intent mIntent = new Intent(this, MyServiceAlternate.class);
        startService(mIntent);
        bindService(mIntent, mConnection, BIND_AUTO_CREATE);*/
    };

    ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            Toast.makeText(MainActivityAlternate.this, "Service is disconnected", Toast.LENGTH_SHORT).show();
            mBounded = false;
            mService.setCallbacks(null);
            mService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Toast.makeText(MainActivityAlternate.this, "Service is connected", Toast.LENGTH_SHORT).show();
            mBounded = true;
            MyServiceAlternate.LocalBinder mLocalBinder = (MyServiceAlternate.LocalBinder)service;
            mService = mLocalBinder.getServerInstance();
            mService.setCallbacks(MainActivityAlternate.this);
        }
    };

    @Override
    protected void onStop() {
        super.onStop();
        if (mBounded) {
            unbindService(mConnection);
            mBounded = false;
        }
    };

    public boolean checkServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        if (manager != null) {
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(
                    Integer.MAX_VALUE)) {
                if (getString(R.string.my_service_name).equals(service.service.getClassName())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void performAction(String result) {
        Log.i("performAction", "in performAction");

        if (result.startsWith("call")) {
            Log.i("performAction", "call");

            btStartService.setText(getString(R.string.start_service));
            tvText.setVisibility(View.GONE);
            if (mBounded) {
                unbindService(mConnection);
                mBounded = false;
            }

            PhoneCallListener phoneListener = new PhoneCallListener();
            TelephonyManager telephonyManager = (TelephonyManager) this
                    .getSystemService(Context.TELEPHONY_SERVICE);
            telephonyManager.listen(phoneListener,
                    PhoneStateListener.LISTEN_CALL_STATE);

            Intent callIntent = new Intent(Intent.ACTION_CALL);
            this.callIntent = callIntent;
            callIntent.setData(Uri.parse("tel:" + "7350045489"));//change the number
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                Log.i("performAction", "error");
                requestForCallPermission(new String[]{Manifest.permission.CALL_PHONE});
            }
            Log.i("performAction", "after call");
            startActivity(callIntent);
        }else if(result.startsWith("open") || result.startsWith("launch")) {
            if(result.toLowerCase().replaceAll(" ", "").equals("opentfdetect")) {
                btStartService.setText(getString(R.string.stop_service));
                tvText.setVisibility(View.VISIBLE);
                findViewById(R.id.btStartService).performClick();
                //Intent serviceIntent = new Intent(MainActivityAlternate.this,
                //        MyServiceAlternate.class);
                //stopService(serviceIntent);
                PackageManager pm = this.getPackageManager();
                Intent intent = pm.getLaunchIntentForPackage("org.tensorflow.demo");
                if (intent != null) {
                    this.startActivityForResult(intent, OBJECT_DETECTION_ACTIVITY);
                }
            }else {
                //Intent intent = new Intent(MainActivityAlternate.this,
                 //       MyServiceAlternate.class);
                //stopService(intent);
                btStartService.setText(getString(R.string.stop_service));
                tvText.setVisibility(View.VISIBLE);
                findViewById(R.id.btStartService).performClick();
                ttsProviderImpl.say("Cannot find application. Please say it again.");
                //onStart();
                /*Intent mIntent = new Intent(this, MyServiceAlternate.class);
                startService(mIntent);
                bindService(mIntent, mConnection, BIND_AUTO_CREATE);*/
                findViewById(R.id.btStartService).performClick();
            }
        }
    }

    public void requestForCallPermission(String[] permissions)
    {

        if (ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.CALL_PHONE))
        {
        }
        else {

            ActivityCompat.requestPermissions(this, permissions,1);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults)
    {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0) {
                    for (int i = 0; i < permissions.length; i++) {
                        if (permissions[i].equals(Manifest.permission.READ_CONTACTS)) {
                            if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                                Log.i("permission_granted", "granted");
                                Cursor contacts = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
                                aNameFromContacts = new String[contacts.getCount()];
                                aNumberFromContacts = new String[contacts.getCount()];
                                int j = 0;

                                if (contacts != null) {
                                    try {
                                        HashSet<String> normalizedNumbersAlreadyFound = new HashSet<>();
                                        int indexOfNormalizedNumber = contacts.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER);
                                        int indexOfDisplayName = contacts.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                                        int indexOfDisplayNumber = contacts.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);

                                        while (contacts.moveToNext()) {
                                            String normalizedNumber = contacts.getString(indexOfNormalizedNumber);
                                            if (normalizedNumbersAlreadyFound.add(normalizedNumber)) {
                                                String displayName = contacts.getString(indexOfDisplayName);
                                                aNameFromContacts[j] = displayName;
                                                String displayNumber = contacts.getString(indexOfDisplayNumber);
                                                aNumberFromContacts[j] = displayNumber;
                                                j++;
                                                //haven't seen this number yet: do something with this contact!
                                            } else {
                                                //don't do anything with this contact because we've already found this number
                                            }
                                        }
                                    } finally {
                                        contacts.close();
                                    }
                                }

                                /*int nameFieldColumnIndex = contacts.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
                                int numberFieldColumnIndex = contacts.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);

                                while (contacts.moveToNext()) {

                                    String contactName = contacts.getString(nameFieldColumnIndex);
                                    aNameFromContacts[j] = contactName;

                                    String number = contacts.getString(numberFieldColumnIndex);
                                    aNumberFromContacts[j] = number;
                                    j++;
                                }

                                contacts.close();*/
                            }
                        } else if(permissions[i].equals(Manifest.permission.CALL_PHONE)) {
                            if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                                startActivity(callIntent);
                            }
                        }
                    }
                }
                break;
        }
    }

    private class PhoneCallListener extends PhoneStateListener {

        private boolean isPhoneCalling = false;

        String LOG_TAG = "LOGGING 123";

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {

            if (TelephonyManager.CALL_STATE_RINGING == state) {
                // phone ringing
                Log.i(LOG_TAG, "RINGING, number: " + incomingNumber);
            }

            if (TelephonyManager.CALL_STATE_OFFHOOK == state) {
                // active
                Log.i(LOG_TAG, "OFFHOOK");

                isPhoneCalling = true;
            }

            if (TelephonyManager.CALL_STATE_IDLE == state) {
                // run when class initial and phone call ended, need detect flag
                // from CALL_STATE_OFFHOOK
                Log.i(LOG_TAG, "IDLE");

                if (isPhoneCalling) {

                    Log.i(LOG_TAG, "restart app");

                    // restart app
                    //onStart();

                    /*Intent mIntent = new Intent(myContext, MyServiceAlternate.class);
                    startService(mIntent);
                    bindService(mIntent, mConnection, BIND_AUTO_CREATE);
                    mBounded = true;*/

                    btStartService.setText(getString(R.string.stop_service));
                    tvText.setVisibility(View.VISIBLE);


                    isPhoneCalling = false;
                }

            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case (OBJECT_DETECTION_ACTIVITY) : {
                if (resultCode == Activity.RESULT_OK) {
                    Log.i("ActivityResult", "TF Detect returned result");
                    // TODO Extract the data returned from the child Activity.
                    Intent mIntent = new Intent(myContext, MyServiceAlternate.class);
                    startService(mIntent);
                    bindService(mIntent, mConnection, BIND_AUTO_CREATE);
                    mBounded = true;

                    btStartService.setText(getString(R.string.start_service));
                    tvText.setVisibility(View.INVISIBLE);
                    findViewById(R.id.btStartService).performClick();
                }
                break;
            }
        }
    }
}
