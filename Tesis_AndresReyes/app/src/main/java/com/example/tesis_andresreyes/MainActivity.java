package com.example.tesis_andresreyes;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.neurosky.connection.ConnectionStates;
import com.neurosky.connection.DataType.MindDataType;
import com.neurosky.connection.TgStreamHandler;
import com.neurosky.connection.TgStreamReader;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

public class MainActivity extends AppCompatActivity
{
    final String TAG = "MainActivityTag";
    ////Program variables
    private int currentPhase;
    private char[][] charMatrix = {{'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i'},
                                   {'j', 'k', 'l', 'm', 'n', 'ñ', 'o', 'p', 'q'},
                                   {'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'},
                                   {'B', ' ', 'E', '0', '0', '0', '0', '0', '0'}};
    private int rowIndex;
    private int colIndex;
    private int noBlinkCount;
    private int goodSigCount;
    private String finalWord;
    ////flag variables
    private boolean flagThread;
    private boolean flagRequestBlink;
    private boolean flagGetBlinks;
    private boolean flagGetRawEEG;
    private boolean flagFinishWord;
    private boolean flagStopBackend;
    private boolean flagPoorSig;
    private boolean flagPause;
    private boolean flagInfoVisible;
    ////View variables
    private ImageView keyboard;
    private ImageView imgStart;
    private EditText wordField;
    private ProgressBar progBar;
    private TextView infoText;
    ////Handler and Runnable Variables
    private Handler eegHandler = new Handler();
    Runnable runStartBlink = new Runnable() {
        @Override
        public void run()
        {
            if(!flagStopBackend)
            {
                if(!flagPoorSig)
                {
                    if(flagInfoVisible)
                    {
                        infoText.setVisibility(View.INVISIBLE);
                        flagInfoVisible = false;
                    }
                    imgStart.setVisibility(View.VISIBLE);
                    flagGetBlinks = true;
                    flagRequestBlink = false;
                }
                else
                {
                    if(!flagInfoVisible)
                    {
                        infoText.setVisibility(View.VISIBLE);
                        flagInfoVisible = true;
                    }
                }
            }
        }
    };

    ////Bluetooth variables
    private BluetoothAdapter mBluetoothAdapter;
    ///////////////////////////
    /////TensorFlow model Variables
    private int numBlinks;
    ////////////////////////////
    ////NeuroSky Variables
    private short[] rawEEG = {0}; //short -> signed 16-bits int (-32768 to 32767)
    private int rawEEG_index = 0;
    private int numSamples;
    private TgStreamReader tgStreamReader;
    private TgStreamHandler tgStreamHandler = new TgStreamHandler() {

        @Override
        public void onStatesChanged(int connectionStates)
        {
            // TODO Auto-generated method stub
            Log.d(TAG, "connectionStates change to: " + connectionStates);
            switch (connectionStates) {
                case ConnectionStates.STATE_CONNECTING:
                    // Do something when connecting
                    break;
                case ConnectionStates.STATE_CONNECTED:
                    // Do something when connected
                    tgStreamReader.start();
                    eegHandler.post(new Runnable() {
                        @Override
                        public void run()
                        {
                            startWordSelection();
                        }
                    });
                    break;
                case ConnectionStates.STATE_WORKING:
                    // Do something when working
                    break;
                case ConnectionStates.STATE_GET_DATA_TIME_OUT:
                    // Do something when getting data timeout
                    showToast("Fallo en conexion de la diadema!!");
                    /*
                    if (tgStreamReader != null && tgStreamReader.isBTConnected()) {
                        tgStreamReader.stop();
                        tgStreamReader.close();
                    }*/
                    break;
                case ConnectionStates.STATE_STOPPED:
                    // Do something when stopped
                    // We have to call tgStreamReader.stop() and tgStreamReader.close() much more than
                    // tgStreamReader.connectAndstart(), because we have to prepare for that.
                    break;
                case ConnectionStates.STATE_DISCONNECTED:
                    // Do something when disconnected
                    break;
                case ConnectionStates.STATE_ERROR:
                    // Do something when you get error message
                    break;
                case ConnectionStates.STATE_FAILED:
                    // Do something when you get failed message
                    // It always happens when open the BluetoothSocket error or timeout
                    // Maybe the device is not working normal.
                    // Maybe you have to try again
                    break;
            }
        }

        @Override
        public void onRecordFail(int flag) {
            // You can handle the record error message here
            Log.e(TAG,"onRecordFail: " +flag);

        }

        @Override
        public void onChecksumFail(byte[] payload, int length, int checksum) {
            // You can handle the bad packets here.
        }

        @Override
        public void onDataReceived(int datatype, int data, Object obj) {
            // You can handle the received data here
            switch (datatype)
            {
                case MindDataType.CODE_ATTENTION:
                    //short attValue[] = {(short)data};
                    break;
                case MindDataType.CODE_MEDITATION:
                    //short medValue[] = {(short)data};
                    break;
                case MindDataType.CODE_POOR_SIGNAL:
                    //long startTime = System.nanoTime();
                    if((short)data > 0)
                    {
                        goodSigCount = 0;
                        flagPoorSig = true;
                    }
                    else
                    {
                        if(flagPoorSig)
                        {
                            goodSigCount++;
                            if(goodSigCount > 2)
                            {
                                flagPoorSig = false;
                            }
                        }
                    }
                    if(flagRequestBlink)
                    {
                        eegHandler.postDelayed(runStartBlink,200);
                    }
                   /* long endTime = System.nanoTime();
                    long duration = (endTime - startTime);
                    Log.i("Time_POORSIG(Nanosec): ", String.valueOf(duration));*/
                    break;
                case MindDataType.CODE_RAW:
                    if(flagGetRawEEG)
                    {
                        rawEEG[rawEEG_index] = (short)data;
                        rawEEG_index++;
                        if (rawEEG_index == 1024)
                        {
                            //Log.d("Finish recording", "rawEEG COMPLETED!");
                            rawEEG_index = 0;
                            flagGetRawEEG = false;
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    };
    ////////////////////////////

    Thread blink_thread = new Thread() {
        @Override
        public void run()
        {
            float[] inputEEG = new float[numSamples];
            ////Variables of tensorflow
            TensorFlowInferenceInterface tfmodel = new TensorFlowInferenceInterface(getAssets(), "file:///android_asset/fin1(5C)f_model.pb");
            float[] blinkOutput = new float[5];
            long[] input_shape = new long[]{1,numSamples,1};
            String outputNode = "output_node0";
            String[] outputNodes = {outputNode};
            boolean enableStats = false;
            ///////////////////////////////////////////
            while(flagThread)
            {
                if(flagGetBlinks)
                {
                    ////EEG Section
                    rawEEG_index = 0;
                    flagGetRawEEG = true;
                    //Thread.sleep(3000); //wait 3 seconds
                    while(flagGetRawEEG)
                    {
                        //wait until rawEEG array is full.
                    }
                    eegHandler.post(new Runnable() {
                        @Override
                        public void run()
                        {
                            imgStart.setVisibility(View.INVISIBLE);
                        }
                    });
                    ///////////////
                    ////Tensorflow section
                    // loading new input
                    for (int i = 0; i < numSamples; ++i)
                    {
                        inputEEG[i]= rawEEG[i];
                    }
                    tfmodel.feed("lstm_7_input",inputEEG,input_shape); // INPUT_SHAPE is an long[] of expected shape, input is a float[] with the input data
                    tfmodel.run(outputNodes, enableStats);
                    tfmodel.fetch(outputNode, blinkOutput); // blink_output is a float[] (size of num_classes)
                    numBlinks = findMaxIndex(blinkOutput);
                    //showToast("Number of blinks: " + String.valueOf(numBlinks));
                    ///////////////////////
                    flagGetBlinks = false;
                    eegHandler.post(new Runnable() {
                        @Override
                        public void run()
                        {
                            processBlinkResult();
                        }
                    });
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ////Variables initialization
        numSamples = 1024;
        numBlinks = 4;
        rawEEG = new short[numSamples];
        //Flag Variables!
        flagThread = true;
        flagRequestBlink = false;
        flagGetBlinks = false;
        flagGetRawEEG = false;
        flagFinishWord = false;
        flagStopBackend = false;
        flagPause = false;
        flagPoorSig = false;
        flagInfoVisible = false;
        ////word building cycle
        currentPhase = 1;
        rowIndex = 0;
        colIndex = 0;
        noBlinkCount = 0;
        goodSigCount = 0;
        finalWord = "";
        ////
        keyboard = findViewById(R.id.i_keyboard);
        imgStart = findViewById(R.id.i_square);
        wordField = findViewById(R.id.t_word);
        progBar = findViewById(R.id.prgbar);
        infoText = findViewById(R.id.t_info);

        keyboard.setImageResource(R.drawable.letrasf1);
        wordField.setVisibility(View.VISIBLE);
        keyboard.setVisibility(View.VISIBLE);
        imgStart.setVisibility(View.INVISIBLE);
        progBar.setVisibility(View.INVISIBLE);
        infoText.setVisibility(View.INVISIBLE);
        ////
        try
        {
            //Make sure that Bluetooth is ON
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled())
            {
                showToast("Por favor encienda el Bluetooth en su celular y reinicie la app");
                finish();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            showToast("No fue posible conectarse con el dispositivo");
            Log.i(TAG, "error:" + e.getMessage());
            return;
        }
        tgStreamReader = new TgStreamReader(mBluetoothAdapter,tgStreamHandler);
        if(tgStreamReader!=null && tgStreamReader.isBTConnected())
        {
            tgStreamReader.stop();
            tgStreamReader.close();
            tgStreamReader = null;
        }
        tgStreamReader.connect();
        blink_thread.start();
        //prgress bar visible wait connection
        infoText.setText(R.string.text_conecting);
        infoText.setVisibility(View.VISIBLE);
        progBar.setVisibility(View.VISIBLE);
    }
    public void startWordSelection()
    {
        progBar.setVisibility(View.INVISIBLE);
        infoText.setVisibility(View.INVISIBLE);
        infoText.setText(R.string.text_poorsig);
        showToast("Conexion Realizada!! Preparate!");
        wordField.setText(finalWord);
        wordField.setSelection(wordField.getText().length());
        Handler startWordHandler = new Handler();
        startWordHandler.postDelayed(new Runnable() {
            @Override
            public void run()
            {
                imgStart.setVisibility(View.VISIBLE);
                flagGetBlinks = true;
            }
        }, 4000);
    }
    public void processBlinkResult()
    {
        switch (numBlinks)
        {
            case 0:
                noBlinkCount++;
                switch (currentPhase)
                {
                    case 1:
                        if(noBlinkCount == 2)
                        {
                            noBlinkCount = 0;
                            rowIndex = 3;
                            colIndex = 0;
                            //update keyboard (phase)
                            keyboard.setImageResource(R.drawable.letrasf3_10);
                            //update phase
                            currentPhase+=2;
                        }
                        break;
                    case 2://go to phase 1
                        if(noBlinkCount == 2)
                        {
                            noBlinkCount = 0;
                            //update keyboard (phase)
                            keyboard.setImageResource(R.drawable.letrasf1);
                            //update phase
                            currentPhase--;
                        }
                        break;
                    case 3:
                        if(noBlinkCount == 2)
                        {
                            if(rowIndex == 3)
                            {
                                //go back to phase 1
                                showToast("de vuelta a fase 1");
                                //update keyboard (phase)
                                keyboard.setImageResource(R.drawable.letrasf1);
                                //update phase
                                currentPhase-=2;
                            }
                            else
                            {
                                //update keyboard (phase)
                                if(rowIndex == 0)
                                {
                                    keyboard.setImageResource(R.drawable.letrasf2_1);
                                }
                                else if(rowIndex == 1)
                                {
                                    keyboard.setImageResource(R.drawable.letrasf2_2);
                                }
                                else
                                {
                                    keyboard.setImageResource(R.drawable.letrasf2_3);
                                }
                                currentPhase--;
                            }
                            noBlinkCount = 0;
                        }
                        break;
                    default:
                        break;
                }
                break;
            case 1:
                noBlinkCount = 0;
                switch (currentPhase)
                {
                    case 1:
                        rowIndex = 0;
                        //update keyboard (phase)
                        keyboard.setImageResource(R.drawable.letrasf2_1);
                        //update phase
                        currentPhase++;
                        break;
                    case 2:
                        colIndex = 0;//Column baseIndex
                        //update keyboard (phase)
                        if(rowIndex == 0)
                        {
                            keyboard.setImageResource(R.drawable.letrasf3_1);
                        }
                        else if(rowIndex == 1)
                        {
                            keyboard.setImageResource(R.drawable.letrasf3_4);
                        }
                        else
                        {
                            keyboard.setImageResource(R.drawable.letrasf3_7);
                        }
                        //update phase
                        currentPhase++;
                        break;
                    case 3:
                        //Column Index stays in 0
                        //execute ACTION
                        if(rowIndex == 3)
                        {
                            //borrar caracter
                            if(finalWord.length()>0)
                            {
                                finalWord = finalWord.substring(0,finalWord.length()-1);
                            }
                        }
                        else
                        {
                            //seleccionar letra
                            finalWord = finalWord + charMatrix[rowIndex][colIndex];
                        }
                        //update word
                        wordField.setText(finalWord);
                        //update keyboard (phase)
                        keyboard.setImageResource(R.drawable.letrasf1);
                        //update phase(go back to phase 1)
                        currentPhase = 1;
                        break;
                    default:

                        break;
                }
                break;
            case 2:
                noBlinkCount = 0;
                switch (currentPhase)
                {
                    case 1:
                        rowIndex = 1;
                        //update keyboard (phase)
                        keyboard.setImageResource(R.drawable.letrasf2_2);
                        //update phase
                        currentPhase++;
                        break;
                    case 2:
                        colIndex = 3;//Column baseIndex
                        //update keyboard (phase)
                        if(rowIndex == 0)
                        {
                            keyboard.setImageResource(R.drawable.letrasf3_2);
                        }
                        else if(rowIndex == 1)
                        {
                            keyboard.setImageResource(R.drawable.letrasf3_5);
                        }
                        else
                        {
                            keyboard.setImageResource(R.drawable.letrasf3_8);
                        }
                        //update phase
                        currentPhase++;
                        break;
                    case 3:
                        colIndex += 1;//Column Index
                        //update word
                        finalWord = finalWord + charMatrix[rowIndex][colIndex];
                        wordField.setText(finalWord);
                        //update keyboard (phase)
                        keyboard.setImageResource(R.drawable.letrasf1);
                        //update phase(go back to phase 1)
                        currentPhase = 1;
                        break;
                    default:
                        //asda
                        break;
                }
                break;
            case 3:
                noBlinkCount = 0;
                switch (currentPhase)
                {
                    case 1:
                        rowIndex = 2;
                        //update keyboard (phase)
                        keyboard.setImageResource(R.drawable.letrasf2_3);
                        //update phase
                        currentPhase++;
                        break;
                    case 2:
                        colIndex = 6;//Column baseIndex
                        //update keyboard (phase)
                        if(rowIndex == 0)
                        {
                            keyboard.setImageResource(R.drawable.letrasf3_3);
                        }
                        else if(rowIndex == 1)
                        {
                            keyboard.setImageResource(R.drawable.letrasf3_6);
                        }
                        else
                        {
                            keyboard.setImageResource(R.drawable.letrasf3_9);
                        }
                        //update phase
                        currentPhase++;
                        break;
                    case 3:
                        //execute ACTION
                        if(rowIndex == 3)
                        {
                            //terminar palabra/frase
                            flagFinishWord = true;
                        }
                        else
                        {
                            colIndex += 2;//Column Index
                            //seleccionar letra
                            finalWord = finalWord + charMatrix[rowIndex][colIndex];
                        }
                        //update word
                        wordField.setText(finalWord);
                        //update keyboard (phase)
                        keyboard.setImageResource(R.drawable.letrasf1);
                        //update phase(go back to phase 1)
                        currentPhase = 1;
                        break;
                    default:
                        //nothing
                        break;
                }
                break;
            default:
                noBlinkCount = 0;
                showToast("No entendí tu acción, intentalo de nuevo");
                break;
        }
        wordField.setSelection(wordField.getText().length());
        if(!flagFinishWord)
        {
            if(!flagPause)
            {
                flagRequestBlink = true;
            }
        }
        else
        {
            //Go to ResultsActivity
            goNextActivity();
        }
    }
    public void goNextActivity()
    {
        Button b = findViewById(R.id.b_stop);
        b.performClick();
        Intent i = new Intent(this, ResultsActivity.class);
        i.putExtra("speechText",finalWord);
        startActivity(i);
    }
    public void stopBackendProcess(View view)
    {
        flagStopBackend = true;
        flagThread = false;
        flagRequestBlink = false;
        flagGetBlinks = false;
        flagGetRawEEG = false;
        flagFinishWord = true;
        if (tgStreamReader != null && tgStreamReader.isBTConnected())
        {
            // Prepare for connecting
            tgStreamReader.stop();
            tgStreamReader.close();
            showToast("Neurosky ha finalizado exitosamente");
        }
        tgStreamReader = null;
        if(!blink_thread.isAlive())
        {
            showToast("Proceso finalizado exitosamente, puede cerrar la app");
        }
    }
    public int findMaxIndex(float [] arr)
    {
        float max = arr[0];
        int maxIdx = 0;
        for(int i = 1; i < arr.length; i++)
        {
            if(arr[i] > max)
            {
                max = arr[i];
                maxIdx = i;
            }
        }
        return maxIdx;
    }
    public void showToast(final String msg)
    {
        MainActivity.this.runOnUiThread(new Runnable()
        {
            public void run()
            {
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
