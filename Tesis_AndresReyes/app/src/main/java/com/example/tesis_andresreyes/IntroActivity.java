package com.example.tesis_andresreyes;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ViewFlipper;

public class IntroActivity extends AppCompatActivity {

    ViewFlipper myView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);
        myView = (ViewFlipper) findViewById(R.id.myViewFlipper);
        TextView instrucText = findViewById(R.id.t_instructions);
        instrucText.setMovementMethod(new ScrollingMovementMethod());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) //Function to handle the result the next activity and receive the data back
    {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 1)
        {
            if (resultCode == RESULT_OK)
            {
                finish();
                Log.d("Finish: ","Closing App!");
                System.exit(0);
            }
        }
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
        startActivityForResult(i,1);
    }
}
