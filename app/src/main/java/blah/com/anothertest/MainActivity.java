package blah.com.anothertest;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import android.widget.Button;
import android.widget.TextView;
import android.widget.EditText;
import android.view.View.OnClickListener;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;

import java.io.IOException;



public class MainActivity extends Activity {

    Bluetooth bt;
    private Button send,connect;
    private Context context;
    private EditText input;
    private EditText input2;
    private EditText input3;
    private TextView receiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bt = new Bluetooth(getApplicationContext(), this);
        context = this.getApplicationContext();
        input = (EditText) findViewById(R.id.editInp);
        input2 = (EditText) findViewById(R.id.editInp2);
        input3 = (EditText) findViewById(R.id.editInp3);

        //try to connect with a device
        tryConnect();
        //when we click the send button, send all of our data
        clickSend();
    }



    //if the send button is clicked, send a string to the bluetooth module
    public void clickSend(){

        final Button send = (Button) findViewById(R.id.sendmsg);
        send.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View view) {

                Toast.makeText(getApplicationContext(), "sending data", Toast.LENGTH_SHORT).show();
                sendAll();
            }

        });
    }

    //when given a string, send it as a series of characters to the bluetooth module
    public void sendAll(){

        //for now, just send one number
        int x = translateInput(input.getText().toString());
        int y = translateInput(input2.getText().toString());
        int z = translateInput(input2.getText().toString());
        try {
            bt.sendData((byte)'s');
            bt.sendData((byte)x);
            bt.sendData((byte)y);
            bt.sendData((byte)z);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    //translate input from the user into a number
    public int translateInput(String raw){

        int num;
        try{
            num = Integer.parseInt(raw);
            return num;
        }catch(Exception e){
            Toast.makeText(getApplicationContext(), "invalid input", Toast.LENGTH_SHORT).show();
            return 0;
        }
    }


    //set up a connection with the target device and ensure that the user won't crash everything
    public void tryConnect(){

        /* try to find a bluetooth device on startup
        * if it cannot be found, pull up bt settings, ask user to pair
        * then when connect is clicked, connect to bluetooth device */
        bt.findBT();
        final Button connect = (Button) findViewById(R.id.connection);
        final Button send = (Button) findViewById(R.id.sendmsg);
        final TextView receiver = (TextView) findViewById(R.id.recdata);
        final TextView temp = (TextView) findViewById(R.id.settemp);
        final EditText editor = (EditText) findViewById(R.id.editInp);
        final EditText editor2 = (EditText) findViewById(R.id.editInp2);
        final EditText editor3 = (EditText) findViewById(R.id.editInp3);
        //to avoid crashes, start off with invisible send/receive stuff
        send.setVisibility(View.GONE);
        receiver.setVisibility(View.GONE);
        editor.setVisibility(View.GONE);
        editor2.setVisibility(View.GONE);
        editor3.setVisibility(View.GONE);
        temp.setVisibility(View.GONE);

        //listen for a click
        connect.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View view) {

                //try one again to find the device
                if (bt.findBT() == -1) {
                    Toast.makeText(getApplicationContext(), "Cannot connect to device", Toast.LENGTH_LONG).show();
                    return;
                }

                // Attempt open a Bluetooth connection
                try {
                    bt.openBT();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // Start listening for Bluetooth data from the Teensy
                bt.beginListenForData();

                //make send button visible if connection went well
                send.setVisibility(View.VISIBLE);
                receiver.setVisibility(View.VISIBLE);
                temp.setVisibility(View.VISIBLE);
                editor.setVisibility(View.VISIBLE);
                editor2.setVisibility(View.VISIBLE);
                editor3.setVisibility(View.VISIBLE);
            }
        });
    }




    //receiving data from the bluetooth module
    public void receiveData(byte[] data) {

        final TextView receiver = (TextView) findViewById(R.id.recdata);
        // If the received data exists and is does not equal zero
        if(data.length > 0 && data[0] != 0) {
            int dataInt = data[0];

            try {
                bt.sendData((byte)'l');
                receiver.setText("Received: " + dataInt);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}