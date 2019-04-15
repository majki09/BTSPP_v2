package com.btsoft.majki.btspp_v2;

/*
 * - w programie w Atmedze dopisać do odpowiedzi UARTowych pierwszy token jako oznaczenie pytania np:
 * rtc,1131,080115
 * pwm,1023,1023,0,56,1
 * - w programie w Atmedze dopisac obsluge roku w RTC
 */

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.Format;
import java.util.Calendar;
import java.util.StringTokenizer;
import java.util.UUID;

//import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
//import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
//import android.widget.ProgressBar;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;


class PWM {
    public static short red = 0;
    public static short green = 0;
    public static short blue = 0;
    public static byte jasnosc = 0;
    public static byte stan = 0;
};

class RTC {
    public static byte godz = 0;
    public static byte minuta = 0;
    public static byte sekunda = 0;
    public static short rok = 2000;
    public static byte mies = 1;
    public static byte dzien = 1;
};

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 1;
    //	private static String address	= "D0:17:6A:31:70:4B";		//GT-I9100
//	private static String address	= "50:46:5D:15:14:86";		//Nexus 7
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");        //SPP
    //	private static final UUID MY_UUID	= UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");		//Secure
//	private static final UUID MY_UUID	= UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");		//Insecure
//	static Boolean BTConnected	=	false;
    static int bytes = 0;
    private static String TAG = "BT SPP";
    private static String address = "00:12:6F:28:30:3A";        //LEDwizard
//	static byte[] buffer = new byte[64];  // buffer store for the stream
    final Handler mHandler = new Handler();
    volatile protected Object mResults = null;
    volatile String text = null;
    Button buttonSendRtc, buttonOnf, buttonPWM, buttonSendText;
    EditText EditText1, editTextSent, editTextReceived, editTextIRSeconds, editTextRed;
    StringBuilder ReceivedText = new StringBuilder();
    StringBuilder SentText = new StringBuilder();
    final Runnable mUpdateResults = new Runnable() {
        public void run() {
            updateResultsInUI();
        }
    };
    SeekBar seekBarRed;
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private OutputStream outStream = null;
    private InputStream inStream = null;
    private String message;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        buttonSendRtc = (Button) findViewById(R.id.buttonSendRtc);
        buttonOnf = (Button) findViewById(R.id.buttonOnf);
        buttonSendText = (Button) findViewById(R.id.buttonSendText);
        buttonPWM = (Button) findViewById(R.id.buttonPwm);
        EditText1 = (EditText) findViewById(R.id.editText1);
        editTextSent = (EditText) findViewById(R.id.editTextSent);
        editTextReceived = (EditText) findViewById(R.id.editTextReceived);
        editTextIRSeconds = (EditText) findViewById(R.id.editTextIRSeconds);
        editTextRed = (EditText) findViewById(R.id.editTextRed);
        seekBarRed = (SeekBar) findViewById(R.id.seekBarRed);

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        checkBTstate();

        buttonSendRtc.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                sendData("rtc\r\n");
            }
        });

        buttonOnf.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                sendData("onf\r\n");
            }
        });

        buttonPWM.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                sendData("sta\r\n");
            }
        });

        buttonSendText.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String data = new String();
                data = EditText1.getText().toString() + "\r\n";
                sendData(data);
            }
        });

        seekBarRed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                int red = (int) seekBarRed.getProgress()*1023/100;
                editTextRed.setText(String.valueOf(red));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

    }

    public void onClickSet(View view)
    {

//        for (int i=1; i>=3;i++) {
        StringBuilder m = new StringBuilder();
        m.append("pwm");
        m.append(String.valueOf(1));
//        m.append(("0000"+editTextRed.getText()).substring(editTextRed.getText().length()));
        m.append(String.format("%04d", Integer.parseInt(editTextRed.getText().toString())));
        m.append("\r\n");

        sendData(m.toString());
//        }
    }

    public void onClickSyncTime(View view)
    {
        StringBuilder m = new StringBuilder();
        m.append("tim");
        m.append(String.valueOf(String.format("%02d", Calendar.getInstance().getTime().getHours())));
        m.append(String.valueOf(String.format("%02d", Calendar.getInstance().getTime().getMinutes())));
        m.append("\r\n");

        sendData(m.toString());
    }

    public void onClickSetIRTime(View view)
	{
		if (!editTextIRSeconds.getText().toString().equals("")) {
			StringBuilder m=new StringBuilder();
			m.append("irt");
			m.append(editTextIRSeconds.getText());
			m.append("\r\n");

			sendData(m.toString());
		}
	}

    public void updateResultsInUI() {
//        String ReceivedText;

//		buttonGet.setText("OK!");
//        TextView1.setText(text);
        ReceivedText.append(text + "\r\n");

        editTextReceived.setText(ReceivedText);

        String typWiadomosci = parseFirstToken(text);
        if (typWiadomosci.equalsIgnoreCase("rtc"))
            parseRTC(text);
        if (typWiadomosci.equalsIgnoreCase("pwm"))
            parsePWM(text);
    }

    @Override
    public void onResume() {
        super.onResume();

        Toast toast = Toast.makeText(getBaseContext(), "Proszę czekać na połączenie.", Toast.LENGTH_SHORT);
        toast.show();

        checkBTstate();
        Log.d(TAG, "BT: onResume, attemping to connect a client");

        BluetoothDevice btDev = btAdapter.getRemoteDevice(address);

        try {
            btSocket = btDev.createRfcommSocketToServiceRecord(MY_UUID);
        } catch (IOException e) {
            Log.d(TAG, "BT: onResume, failed to create socket: " + e.getMessage());
            finish();
        }

        btAdapter.cancelDiscovery();

        Log.d(TAG, "BT: Connecting to a client");
        try {
            btSocket.connect();
//			if(btSocket.isConnected())
//			{
            Log.d(TAG, "BT: Connection established and data link opened");
            toast = Toast.makeText(getBaseContext(), "Połączono pomyślnie!", Toast.LENGTH_SHORT);
            toast.show();
            new Receiver().start();                //start nowego thread'u odbierającego dane przez BT
//			}
//			else
//			{
//				Log.d(TAG, "BT: Connection NOT established!");
//				toast	= Toast.makeText(getBaseContext(), "Brak połączenia :(", Toast.LENGTH_LONG);
//				toast.show();
//			}
        } catch (IOException e) {
            try {
                btSocket.close();
            } catch (IOException e2) {
                Log.d(TAG, "BT: onResume() and unable to close socket during connection failure" + e2.getMessage());
                finish();
            }
        }

        Log.d(TAG, "BT: Socket creating");
        try {
            inStream = btSocket.getInputStream();
            outStream = btSocket.getOutputStream();
        } catch (IOException e) {
            Log.d(TAG, "onResume() and output stream creation failed:" + e.getMessage());
        }
    }

    public void onPause() {
        super.onPause();

//		try {
//			btSocket.close();
//		} catch (IOException e) {
//			Log.d(TAG, "In onPause() and failed to close socket." + e.getMessage());
//		}
        disconnect();
        return;
    }

    private void disconnect() {
//        if (btSocket != null) {
//    		if (outStream != null)
//    		{
//    			try
//	    		{
////	    			inStream.reset();
//	    			outStream.flush();
//	    		}
//	    		catch (IOException e1)
//	    		{
//	    			Log.e(TAG, "BT: Failed to flush outStream: " + e1.getMessage());
//	    		}
//    		}
//
//    		if (inStream != null)
//    		{
//    			try
//	    		{
////	    			inStream.reset();
//	    			inStream.reset();
////	    			inStream.close();
////	    			outStream.flush();
//	    		}
//	    		catch (IOException e1)
//	    		{
//	    			Log.e(TAG, "BT: Failed to reset inStream: " + e1.getMessage());
//	    		}
//    		}

            try {
                btSocket.close();
                btSocket = null;
            } catch (IOException e) {
                Log.e(TAG, "BT: Failed to close socket");
            }
//        }
    }

    private void checkBTstate() {
        if (btAdapter.isEnabled()) {
            Log.d(TAG, "BT: Bluetooth enabled");
        } else {
            Log.d(TAG, "BT: Bluetooth enabling");
            Intent enableBTintent = new Intent(btAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBTintent, REQUEST_ENABLE_BT);
        }
    }

    private String parseFirstToken(String message) {
        StringTokenizer tokens = new StringTokenizer(message, ",");
        String typ = tokens.nextToken();

        return typ;
    }

    private void parseRTC(String message) {
        int time = 0;
        int date = 0;

        StringTokenizer tokens = new StringTokenizer(message, ",");
        tokens.nextToken();        //po to, by ominąć token z typem wiadomości
        String pierwszy = tokens.nextToken();
        String drugi = tokens.nextToken();
//		String trzeci	= tokens.nextToken();
        try {
            time = Integer.parseInt(pierwszy);
            date = Integer.parseInt(drugi);
//			PWM.blue	= (short) Integer.parseInt(trzeci);
        } catch (NumberFormatException e) {
            Log.d(TAG, "Nie można sparsować PWM: " + e.getMessage());
        }

        RTC.godz = (byte) (time / 10000);
        RTC.minuta = (byte) ((time % 10000) / 100);
        RTC.sekunda = (byte) (time % 100);

        RTC.dzien = (byte) (date / 10000);
        RTC.mies = (byte) ((date % 10000) / 100);
        RTC.rok = (short) (2000 + date % 100);
    }

    private void parsePWM(String message)
    {
        SeekBar brightness = (SeekBar) findViewById(R.id.seekBarBrightness);
        SeekBar seekBarRed = (SeekBar) findViewById(R.id.seekBarRed);
        EditText editBrightness = (EditText) findViewById(R.id.editTextBrightness);
        EditText editRed = (EditText) findViewById(R.id.editTextRed);
        EditText editGreen = (EditText) findViewById(R.id.editTextGreen);
        EditText editBlue = (EditText) findViewById(R.id.editTextBlue);
        StringTokenizer tokens = new StringTokenizer(message, ",");

        tokens.nextToken();        //po to, by ominąć token z typem wiadomości
        String red = tokens.nextToken();
        String green = tokens.nextToken();
        String blue = tokens.nextToken();
        String jasnosc = tokens.nextToken();
        String stan = tokens.nextToken();
        try {
            PWM.red = (short) Integer.parseInt(red);
            PWM.green = (short) Integer.parseInt(green);
            PWM.blue = (short) Integer.parseInt(blue);
            PWM.jasnosc = (byte) Integer.parseInt(jasnosc);
            PWM.stan = (byte) Integer.parseInt(stan);
        } catch (NumberFormatException e) {
            Log.d(TAG, "Nie można sparsować PWM: " + e.getMessage());
        }

        brightness.setProgress(Integer.parseInt(jasnosc));
        seekBarRed.setProgress((int) (Float.parseFloat(red) / 1023 * 100));
        editBrightness.setText(jasnosc);
        editRed.setText(red);
        editGreen.setText(green);
        editBlue.setText(blue);
    }

    private void sendData(String message) {
        byte[] msgBuffer = message.getBytes();

        SentText.append(message);
        editTextSent.setText(SentText);
        editTextSent.scrollTo(0,10);
        Log.d(TAG, "BT: Sending String: " + message);

        try {
            outStream.write(msgBuffer);
        } catch (IOException e) {
            //String msg	= "In onResume() and an exception occured during write to buffer: " + e.getMessage();
            String msg = "An exception occured during write to buffer: " + e.getMessage();
            Toast toast = Toast.makeText(getBaseContext(), msg, Toast.LENGTH_LONG);
            toast.show();
        }
    }

    private class Receiver extends Thread {
        @Override
        public void run() {
//    		byte[] buffer	=	new byte[1024];
//    		int bytes;
//    		String text	=	null;

            while (true) {
                try {
//    				bytes	=	inStream.read(buffer);
//    				ByteBuffer b	=	ByteBuffer.wrap(buffer);

                    BufferedReader r = new BufferedReader(new InputStreamReader(inStream));
                    StringBuilder total = new StringBuilder();
                    String line;
                    // ORYGINAL :
//    				while ((line = r.readLine()) != null)
//    				{
//    					total.append(line);
//    				}
                    // zmienione, bo byly problemy z wykryciem nulla
                    line = r.readLine();
                    total.append(line);
                    text = line.toString();

//    				mResults	=	text;
                    mHandler.post(mUpdateResults);

//    				if(text.equalsIgnoreCase("abc"))
//    				{
//    					TextView1.setText(text);
//    					buttonGet.setText("OK!");
//    				}

//    	    		String text	= new String(buffer, "UTF-8");
                    Log.d(TAG, "Collected data: " + text);
                } catch (IOException e2) {
                    Log.e(TAG, "Error przy odbieraniu");
//    				disconnect();
                    return;
                }
            }
        }
    }
}
