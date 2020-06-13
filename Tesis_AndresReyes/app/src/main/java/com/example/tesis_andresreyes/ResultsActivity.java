package com.example.tesis_andresreyes;

import android.content.Intent;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import java.util.Locale;

public class ResultsActivity extends AppCompatActivity {

    private TextView wordText;
    private boolean flagSpeak;

    private TextToSpeech speechMachine;
    private String finalWord;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);
        wordText = findViewById(R.id.t_word);
        flagSpeak = true;
        ////Receive message by launcher activity(Main)
        Intent i = getIntent();
        finalWord = i.getStringExtra("speechText");
        /////
        wordText.setText(finalWord);

        speechMachine = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status)
            {
                Locale locSpanish = new Locale("spa", "COL");
                speechMachine.setLanguage(locSpanish);
                if (status == TextToSpeech.SUCCESS)
                {
                    ///here the machine can start to speak
                    speechMachine.speak(finalWord,TextToSpeech.QUEUE_ADD, null, null);
                }
                else
                {
                    Handler speakHandler = new Handler();
                    speakHandler.postDelayed(new Runnable() {
                        @Override
                        public void run()
                        {
                            speechMachine.speak(finalWord,TextToSpeech.QUEUE_ADD, null, null);
                        }
                    },3000);
                }
            }
        });
    }
    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        if(flagSpeak)
        {
            speechMachine.speak(finalWord,TextToSpeech.QUEUE_ADD, null, null);
            flagSpeak = false;
        }
        return true;
    }

    public void exitApp(View view)
    {
        if(speechMachine != null)
        {
            speechMachine.stop();
            speechMachine.shutdown();
        }
        Intent replyIntent = new Intent();
        setResult(RESULT_OK,replyIntent);
        finish();
    }
}
