package com.sac.speechdemo;

import android.content.Context;
import android.media.AudioManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import java.util.Locale;

/**
 * Created by dell on 17/3/19.
 */

public class TtsProviderImpl extends TtsProviderFactory implements TextToSpeech.OnInitListener {
    private TextToSpeech tts;
    private AudioManager amanager;

    public void sendAudioManager(AudioManager a) {
        this.amanager = a;
    }

    public void init(Context context) {
        if (tts == null) {
            tts = new TextToSpeech(context, this);
        }
        Log.i("Init", "in init");
    }

    @Override
    public void say(String sayThis) {
        tts.speak(sayThis, TextToSpeech.QUEUE_FLUSH, null);
    }

    @Override
    public void onInit(int status) {
        Log.i("OnInit", "in oninit");
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onDone(String utteranceId) {
                Log.i("Utterance", "TTS complete");
            }

            @Override
            public void onError(String utteranceId) {
                Log.e("Utterance", "TTS error");
            }

            @Override
            public void onStart(String utteranceId) {
                Log.i("Utterance", "TTS start");
            }

        });
        Locale loc = new Locale("de", "", "");
        if (tts.isLanguageAvailable(loc) >= TextToSpeech.LANG_AVAILABLE) {
            tts.setLanguage(loc);
        }
    }

    /*public void onUtteranceCompleted(String utteranceId)  {
        Log.i("Utterance", "outside amanager");
        if (amanager != null) {
            Log.i("Utterance", "in amanager");
            amanager.setStreamMute(AudioManager.STREAM_NOTIFICATION, true);
            amanager.setStreamMute(AudioManager.STREAM_ALARM, true);
            amanager.setStreamMute(AudioManager.STREAM_MUSIC, true);
            amanager.setStreamMute(AudioManager.STREAM_RING, true);
            amanager.setStreamMute(AudioManager.STREAM_SYSTEM, true);
        }
    }*/

    public void shutdown() {
        tts.shutdown();
    }
}
