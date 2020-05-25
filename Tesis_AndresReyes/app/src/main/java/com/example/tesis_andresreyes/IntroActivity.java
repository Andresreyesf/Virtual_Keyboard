package com.example.tesis_andresreyes;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ViewFlipper;

public class IntroActivity extends AppCompatActivity {

    ViewFlipper myView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);
        myView = (ViewFlipper) findViewById(R.id.myViewFlipper);
    }

    public void showInstructions(View view)
    {
        myView.showNext();
    }

    public void showConfig(View view)
    {
        myView.showNext();
    }

    public void launchActivity(View view)
    {
        Intent i = new Intent(this, MainActivity.class);
        startActivity(i);
    }
}
