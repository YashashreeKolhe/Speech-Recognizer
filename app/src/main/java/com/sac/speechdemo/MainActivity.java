package com.sac.speechdemo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog.Builder;
import com.afollestad.materialdialogs.Theme;
import com.example.user.speechrecognizationasservice.R;

public class MainActivity extends AppCompatActivity implements ServiceCallbacks {

  private Button btStartService;
  private TextView tvText;
  //public MyService myService;

  @SuppressLint("SetTextI18n")
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    btStartService = (Button) findViewById(R.id.btStartService);
    tvText = (TextView) findViewById(R.id.tvText);
    //Some devices will not allow background service to work, So we have to enable autoStart for the app.
    //As per now we are not having any way to check autoStart is enable or not,so better to give this in LoginArea,
    //so user will not get this popup again and again until he logout
    enableAutoStart();

    if (checkServiceRunning()) {
      btStartService.setText(getString(R.string.stop_service));
      tvText.setVisibility(View.VISIBLE);
    }

    btStartService.setOnClickListener(v -> {
      if (btStartService.getText().toString().equalsIgnoreCase(getString(R.string.start_service))) {
        Intent i = new Intent(MainActivity.this, MyService.class);

        i.setAction("SET_CALLBACKS_ON_ACTION");
        startService(i);
        btStartService.setText(getString(R.string.stop_service));
        tvText.setVisibility(View.VISIBLE);
      } else {
        Intent i = new Intent(MainActivity.this, MyService.class);
        stopService(i);
        i.setAction("SET_CALLBACKS_OFF_ACTION");
        //myService.setCallbacks(null);
        btStartService.setText(getString(R.string.start_service));
        tvText.setVisibility(View.GONE);
      }
    });
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
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

  public ActivityManager.RunningServiceInfo getServiceInstance() {
    ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
    if (manager != null) {
      for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(
              Integer.MAX_VALUE)) {
        if (getString(R.string.my_service_name).equals(service.service.getClassName())) {
          return service;
        }
      }
    }
    return null;
  }

  @Override
  public void performAction(String stt) {
    Log.i("performAction", "in performAction");
    stopService(new Intent(MainActivity.this, MyService.class));
    //myService.setCallbacks(null);
    btStartService.setText(getString(R.string.start_service));
    tvText.setVisibility(View.GONE);

    if (stt.startsWith("call")) {
      Log.i("performAction", "call");
      Intent callIntent = new Intent(Intent.ACTION_CALL);
      callIntent.setData(Uri.parse("tel:" + "7350045489"));//change the number
      if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
        // TODO: Consider calling
        //    ActivityCompat#requestPermissions
        // here to request the missing permissions, and then overriding
        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
        //                                          int[] grantResults)
        // to handle the case where the user grants the permission. See the documentation
        // for ActivityCompat#requestPermissions for more details.
        return;
      }
      startActivity(callIntent);
    }
  }
}
