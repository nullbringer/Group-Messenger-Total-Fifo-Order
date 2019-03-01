package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TabHost;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 * 
 * @author Amlan
 *
 */
public class GroupMessengerActivity extends Activity {

    private static final String TAG = GroupMessengerActivity.class.getName();
    static final int SERVER_PORT = 10000;
    private static AtomicInteger clientSeqId = new AtomicInteger(0);

    private static final String KEY_FIELD = "key";
    private static final String VALUE_FIELD = "value";
    private static final String REMOTE_PORT [] = { "11108",  "11112", "11116", "11120", "11124"};
    private static final String SEPARATOR = "##";

    private Queue<Messege> messegeQueue = new PriorityQueue<Messege>();

    private Uri mUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        mUri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger2.provider");



        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        
        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));

        final EditText editText = (EditText) findViewById(R.id.editText1);

        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));


        try {

            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);

        } catch (IOException e) {

            Log.e(TAG, "Can't create a ServerSocket");
            return;
        }

        findViewById(R.id.button4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                String msg = editText.getText().toString();

                if(msg!= null && msg.length()>0){

                    msg = msg  + "\n";

                    editText.setText(""); // This is one way to reset the input box.


                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);

                    Log.e(TAG,msg);


                }



            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }


    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];

            /* infinite while loop to accept multiple messeges */

            while (true) {

                try {


                    Socket clientSocket = serverSocket.accept();

                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));


                    String incomingMessege = bufferedReader.readLine();

                    if (incomingMessege != null) {

                        publishProgress(incomingMessege);
                    }

                    clientSocket.close();


                } catch (IOException e) {
                    Log.e(TAG, "Client Connection failed");
                }
            }


        }

        protected void onProgressUpdate(String...strings) {
            /*
             * The following code displays what is received in doInBackground().
             */

            Log.d(TAG,"msg recieved::::" + strings[0].trim());


//            msg format:    sequence##content##isDeliverable##source

            String strReceived [] = strings[0].trim().split(SEPARATOR);

            int sequence;

            if(strReceived[0]!=null && strReceived[0].length()>0){
                sequence = Integer.parseInt(strReceived[0]);
            }

            String content = strReceived[1];
            boolean isDeliverable = strReceived[2].equals("1")?true:false;
            int source = Integer.parseInt(strReceived[3]);

            Messege msg;

            if(!isDeliverable){
                // create proposal

                sequence = clientSeqId.getAndIncrement();
                msg = new Messege(sequence, content, isDeliverable, source);

                //TODO: multicast to source


            } else {
                /* remove and queue again with agreed sequence id
                ready for delivery */

                msg = new Messege(sequence, content, false, source);
                messegeQueue.remove();

                msg = new Messege(sequence, content, isDeliverable, source);


            }

            messegeQueue.add(msg);










//
//            ContentValues mContentValues = new ContentValues();
//
//            mContentValues.put(KEY_FIELD, sourceSequence);
//            mContentValues.put(VALUE_FIELD, msgReceieved);
//
//            getContentResolver().insert(mUri, mContentValues);








//
//            Cursor resultCursor = getContentResolver().query(mUri, null, key, null, null);
//
//            if (resultCursor == null) {
//                Log.e(TAG, "Result null");
//            }
//
//            int keyIndex = resultCursor.getColumnIndex(KEY_FIELD);
//            int valueIndex = resultCursor.getColumnIndex(VALUE_FIELD);
//            if (keyIndex == -1 || valueIndex == -1) {
//                Log.e(TAG, "Wrong columns");
//                resultCursor.close();
//            }
//
//            resultCursor.moveToFirst();
//
//            if (!(resultCursor.isFirst() && resultCursor.isLast())) {
//                Log.e(TAG, "Wrong number of rows");
//                resultCursor.close();
//            }
//
//
//            String returnValue = resultCursor.getString(valueIndex);
//
//
//            tv.append(returnValue+ "\n");
//
//
//            resultCursor.close();


            return;
        }
    }


    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {

            String myPort = msgs[1];

            try {

                for (int z=0; z<5; z++) {

                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(REMOTE_PORT[z]));

//                    msg format:    sequence##content##isDeliverable##source


                    String msgToSend = "" + SEPARATOR + msgs[0] +
                            SEPARATOR + "0" + myPort;


                    DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());

                    dataOutputStream.writeBytes(msgToSend);
                    dataOutputStream.flush();

                    socket.close();
                    dataOutputStream.close();
                }


            } catch (UnknownHostException e) {
                Log.e(TAG, "ClientTask UnknownHostException");
            } catch (IOException e) {
                Log.e(TAG, "ClientTask socket IOException");
            }

            return null;
        }
    }

    private class ClientTaskForSpecificTarget extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {

            String myPort = msgs[1];

            try {

                String currentSequenceId = String.valueOf(clientSeqId.getAndIncrement());
                for (int z=0; z<5; z++) {

                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(REMOTE_PORT[z]));


                    String msgToSend = myPort + "::" + currentSequenceId + "::" + msgs[0];

                    DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());

                    dataOutputStream.writeBytes(msgToSend);
                    dataOutputStream.flush();

                    socket.close();
                    dataOutputStream.close();
                }


            } catch (UnknownHostException e) {
                Log.e(TAG, "ClientTask UnknownHostException");
            } catch (IOException e) {
                Log.e(TAG, "ClientTask socket IOException");
            }

            return null;
        }
    }

    private static Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }

}
