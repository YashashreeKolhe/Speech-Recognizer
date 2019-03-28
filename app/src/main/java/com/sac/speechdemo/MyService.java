package com.sac.speechdemo;

import android.Manifest.permission;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Handler;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.example.user.speechrecognizationasservice.R;
import com.sac.speech.GoogleVoiceTypingDisabledException;
import com.sac.speech.Speech;
import com.sac.speech.SpeechDelegate;
import com.sac.speech.SpeechRecognitionNotAvailable;
import com.tbruyelle.rxpermissions.RxPermissions;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;

public class MyService extends Service implements SpeechDelegate, Speech.stopDueToDelay {

  public static SpeechDelegate delegate;
  private ServiceCallbacks serviceCallbacks;
  TtsProviderFactory ttsProviderImpl = TtsProviderFactory.getInstance();

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    //TODO do something useful
    if (intent != null && intent.getAction() != null && intent.getAction().equals("SET_CALLBACKS_ON_ACTION")) {
      setCallbacks(null);
    }

    if (intent != null && intent.getAction() != null && intent.getAction().equals("SET_CALLBACKS_OFF_ACTION")) {
      setCallbacks(null);
    }


    if (ttsProviderImpl != null) {
      AudioManager manager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
      ttsProviderImpl.init(this);
      ttsProviderImpl.sendAudioManager(manager);
      ttsProviderImpl.say("You may speak");
    }
    try {
      if (VERSION.SDK_INT >= VERSION_CODES.KITKAT) {
        ((AudioManager) Objects.requireNonNull(
          getSystemService(Context.AUDIO_SERVICE))).setStreamMute(AudioManager.STREAM_SYSTEM, true);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    Speech.init(this);
    delegate = this;
    Speech.getInstance().setListener(this);

    if (Speech.getInstance().isListening()) {
      Speech.getInstance().stopListening();
      muteBeepSoundOfRecorder();
    } else {
      System.setProperty("rx.unsafe-disable", "True");
      RxPermissions.getInstance(this).request(permission.RECORD_AUDIO).subscribe(granted -> {
        if (granted) { // Always true pre-M
          try {
            Speech.getInstance().stopTextToSpeech();
            Speech.getInstance().startListening(null, this);
          } catch (SpeechRecognitionNotAvailable exc) {
            //showSpeechNotSupportedDialog();

          } catch (GoogleVoiceTypingDisabledException exc) {
            //showEnableGoogleVoiceTyping();
          }
        } else {
          Toast.makeText(this, R.string.permission_required, Toast.LENGTH_LONG).show();
        }
      });
      //muteBeepSoundOfRecorder();
    }

    return Service.START_STICKY;
  }

  @Override
  public IBinder onBind(Intent intent) {
    //TODO for communication return IBinder implementation
    return null;
  }

  @Override
  public void onStartOfSpeech() {
  }

  @Override
  public void onSpeechRmsChanged(float value) {

  }

  @Override
  public void onSpeechPartialResults(List<String> results) {
    for (String partial : results) {
      Log.d("Result", partial+"");
    }
  }

  @Override
  public void onSpeechResult(String result) {
    Log.d("Result", result+"");
    if (!TextUtils.isEmpty(result)) {
      Toast.makeText(this, result, Toast.LENGTH_SHORT).show();
      unmuteBeepSoundOfRecorder();
      ttsProviderImpl.say(result);
      parseResult(result);
    }
  }

  @Override
  public void onSpecifiedCommandPronounced(String event) {
    try {
      if (VERSION.SDK_INT >= VERSION_CODES.KITKAT) {
        ((AudioManager) Objects.requireNonNull(
          getSystemService(Context.AUDIO_SERVICE))).setStreamMute(AudioManager.STREAM_SYSTEM, true);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    if (Speech.getInstance().isListening()) {
      muteBeepSoundOfRecorder();
      Speech.getInstance().stopListening();
    } else {
      RxPermissions.getInstance(this).request(permission.RECORD_AUDIO).subscribe(granted -> {
        if (granted) { // Always true pre-M
          try {
            //Speech.getInstance().stopTextToSpeech();
            Speech.getInstance().startListening(null, this);
          } catch (SpeechRecognitionNotAvailable exc) {
            //showSpeechNotSupportedDialog();

          } catch (GoogleVoiceTypingDisabledException exc) {
            //showEnableGoogleVoiceTyping();
          }
        } else {
          Toast.makeText(this, R.string.permission_required, Toast.LENGTH_LONG).show();
        }
      });
      //muteBeepSoundOfRecorder();
    }
  }

  /**
   * Function to remove the beep sound of voice recognizer.
   */

  @SuppressLint("MissingPermission")
  private void parseResult(String result) {
    Log.i("Call_intent", "in Parse Result");
    if(serviceCallbacks != null) {
      serviceCallbacks.performAction(result);
    }
    /*if (result.startsWith("call")) {
      Log.i("Call_intent", "in call");
      Intent notificationIntent = new Intent(Intent.ACTION_CALL);
      notificationIntent.setData(Uri.parse("tel:123456789"));
      notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
      PendingIntent intent = PendingIntent.getActivity(getApplicationContext(), 0,
              notificationIntent, 0);
    }*/
  }

  public void muteBeepSoundOfRecorder() {
    AudioManager amanager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
    if (amanager != null) {
      //amanager.setStreamMute(AudioManager.STREAM_NOTIFICATION, true);
      amanager.setStreamMute(AudioManager.STREAM_ALARM, true);
      amanager.setStreamMute(AudioManager.STREAM_MUSIC, true);
      //amanager.setStreamMute(AudioManager.STREAM_RING, true);
      amanager.setStreamMute(AudioManager.STREAM_SYSTEM, true);
    }
  }

  private void unmuteBeepSoundOfRecorder() {
    AudioManager amanager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
    if (amanager != null) {
      //amanager.setStreamMute(AudioManager.STREAM_NOTIFICATION, false);
      amanager.setStreamMute(AudioManager.STREAM_ALARM, false);
      amanager.setStreamMute(AudioManager.STREAM_MUSIC, false);
      //amanager.setStreamMute(AudioManager.STREAM_RING, false);
      amanager.setStreamMute(AudioManager.STREAM_SYSTEM, false);
    }
  }

  @Override
  public void onTaskRemoved(Intent rootIntent) {
    //Restarting the service if it is removed.
    PendingIntent service =
      PendingIntent.getService(getApplicationContext(), new Random().nextInt(),
        new Intent(getApplicationContext(), MyService.class), PendingIntent.FLAG_ONE_SHOT);

    AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
    assert alarmManager != null;
    alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, 1000, service);
    super.onTaskRemoved(rootIntent);
  }

  public void setCallbacks(ServiceCallbacks callbacks) {
    serviceCallbacks = callbacks;
  }

}