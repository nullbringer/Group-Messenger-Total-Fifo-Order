package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.TelephonyManager;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
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

    private static AtomicInteger proposalSeqId = new AtomicInteger(0);
    private static AtomicInteger dbSequence = new AtomicInteger(0);

    private static final String KEY_FIELD = "key";
    private static final String VALUE_FIELD = "value";
    private static final String SEPARATOR = "##";

//    private static final Integer REMOTE_PORT [] = { 11108,  11112, 11116, 11120, 11124};

    private static Set<Integer> REMOTE_PORT = new HashSet<Integer>();


    private static TreeMap<Long, HashMap<Integer,Integer>> proposalCounter = new TreeMap<Long, HashMap<Integer, Integer>>();
    private Queue<Messege> messegeQueue = new PriorityQueue<Messege>();

    private static Integer MY_PORT;
    TextView tv;
    private Uri mUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        mUri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger2.provider");
        REMOTE_PORT.addAll(Arrays.asList(new Integer[] {11108,  11112, 11116, 11120, 11124}));



        tv = (TextView) findViewById(R.id.textView1);
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
        MY_PORT = (Integer.parseInt(portStr) * 2);



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

//                    msg = msg  + "\n";

                    editText.setText(""); // This is one way to reset the input box.


                    Messege messege = new Messege(-1, msg,false, MY_PORT, MY_PORT, System.currentTimeMillis());


                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, messege);

                    Log.e(TAG,msg);


                }



            }
        });




//        https://stackoverflow.com/a/10207775

        final Handler handler = new Handler();


        Runnable runnable = new Runnable() {

            @Override
            public void run() {
                try{


                    Messege topMessege = messegeQueue.peek();

                    if(topMessege!=null && topMessege.isDeliverable()){

                        topMessege = messegeQueue.poll();

                        ContentValues mContentValues = new ContentValues();

                        mContentValues.put(KEY_FIELD, dbSequence.getAndIncrement());
                        mContentValues.put(VALUE_FIELD, topMessege.getContent());

                        getContentResolver().insert(mUri, mContentValues);


                        String colorKey = (String) getResources().getText(getResources().getIdentifier("c_"+topMessege.getOrigin(), "string", "edu.buffalo.cse.cse486586.groupmessenger2"));


                        tv.append(Html.fromHtml(dbSequence.get() +": <font color='"+colorKey+"'>"+topMessege.getContent()+ "</color>"));
                        tv.append("\n");



                    }


                }
                catch (Exception e) {
                    Log.e(TAG,"Exception in runnable!!"+e);
                }
                finally{
                    handler.postDelayed(this, 500);
                }
            }
        };

        handler.post(runnable);

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

                    DataInputStream dataInputStream = new DataInputStream(clientSocket.getInputStream());
                    String incomingMessege = dataInputStream.readUTF();


                    publishProgress(incomingMessege);

                    // return acknowledgement to sender

                    DataOutputStream dataOutputStream = new DataOutputStream(clientSocket.getOutputStream());
                    dataOutputStream.writeUTF("ACK");
                    dataOutputStream.flush();






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

//            Log.d(TAG,"msg recieved::::" + strings[0].trim());


            //TODO: create processpacket()

            Messege recievedMessege = new Messege(strings[0], SEPARATOR);



//            String strReceived [] = strings[0].trim().split(SEPARATOR);
//
//            int sequence = -1;
//
//            if(strReceived[0]!=null && strReceived[0].length()>0){
//                sequence = Integer.parseInt(strReceived[0]);
//            }
//
//            String content = strReceived[1];
//            boolean isDeliverable = strReceived[2].equals("1")?true:false;
//            int source = Integer.parseInt(strReceived[3]);
//            int origin = Integer.parseInt(strReceived[4]);
//            long originTimestamp = Long.parseLong(strReceived[5]);


            if(!recievedMessege.isDeliverable()){

                // If NOT ready for delivery

                if(recievedMessege.getSequence() == -1) {

                    // If NO sequence found, we need to send proposals to origin

                    recievedMessege.setSequence(proposalSeqId.getAndIncrement());

                    // Add this messege to Priority Queue
                    messegeQueue.add(recievedMessege);


                    new ClientTaskForSpecificTarget().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, recievedMessege);



                } else{

                    //If sequence found, but not ready for delivery. i.e: this is a returned proposal
                    // check source and origin, count proposals, select highest
                    // prepare msg for delivery and multicast to eveyone



                    if(recievedMessege.getOrigin() == MY_PORT){


                        HashMap<Integer, Integer> mp = proposalCounter.get(recievedMessege.getOriginTimestamp());

                        if(mp == null){
                            mp = new HashMap<Integer, Integer>();
                        }

                        mp.put(recievedMessege.getSource(), recievedMessege.getSequence());
                        proposalCounter.put(recievedMessege.getOriginTimestamp(), mp);

                        //TODO: treemap.firstkey() implementation, if time permits
                        if(proposalCounter.get(recievedMessege.getOriginTimestamp()).size() == REMOTE_PORT.size()){

                            //choose highest and let others know to make it depliverable.

                            int highestProposedSequence = 0;

                            for (Integer value : proposalCounter.get(recievedMessege.getOriginTimestamp()).values()) {

                                highestProposedSequence = value>highestProposedSequence?value:highestProposedSequence;
                            }


                            recievedMessege.setSequence(highestProposedSequence);
                            recievedMessege.setDeliverable(true);


                            Log.d(TAG,"AGREED And TRANSMITTED:: " + recievedMessege.toString());

                            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, recievedMessege);

                            proposalCounter.remove(recievedMessege.getOriginTimestamp());


                        }

                    }


                }



            } else {
                /* remove and queue again with agreed sequence id
                ready for delivery */

                recievedMessege.setDeliverable(false);
                messegeQueue.remove(recievedMessege);




                // Add this messege to Priority Queue
                recievedMessege.setDeliverable(true);
                messegeQueue.add(recievedMessege);

                Log.d(TAG,"QUEUED** " + recievedMessege.toString());


            }



            return;
        }
    }


    private class ClientTask extends AsyncTask<Messege, Void, Void> {

        @Override
        protected Void doInBackground(Messege... msgs) {


            Set<Integer> portToRemove = new HashSet<Integer>();

            for (int thisPort: REMOTE_PORT) {

                try {

                    Messege msg = msgs[0].clone();

                    // Create connection and set read timeout

                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            thisPort);

                    socket.setSoTimeout(2000);

                    // Send messege to target port
                    msg.setSource(MY_PORT);
                    String msgToSend = msg.createPacket(SEPARATOR);


                    DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
                    dataOutputStream.writeUTF(msgToSend);
                    dataOutputStream.flush();


                    //Listen for acknowledgement

                    DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());

                    String ackmsg = dataInputStream.readUTF();
                    Log.d(TAG, ackmsg);

                    dataInputStream.close();




                    socket.close();




                } catch (SocketTimeoutException e){
                    Log.e(TAG, "ClientTask SocketTimeoutException");
                    portToRemove.add(thisPort);

                } catch (UnknownHostException e) {
                    Log.e(TAG, "ClientTask UnknownHostException");
                    portToRemove.add(thisPort);

                } catch (IOException e) {
                    Log.e(TAG, "ClientTask socket IOException");
                    portToRemove.add(thisPort);

                } catch (CloneNotSupportedException e){
                    Log.e(TAG, "ClientTask socket CloneNotSupportedException");
                }
            }


            REMOTE_PORT.removeAll(portToRemove);




            return null;
        }
    }

    private class ClientTaskForSpecificTarget extends AsyncTask<Messege, Void, Void> {

        @Override
        protected Void doInBackground(Messege... msgs) {


            Messege msg = null;

            Set<Integer> portToRemove = new HashSet<Integer>();



            try {

                msg = msgs[0].clone();

                // Create connection and set read timeout

                Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                        msg.getOrigin());

                socket.setSoTimeout(2000);

                // Send messege to target port
                msg.setSource(MY_PORT);
                String msgToSend = msg.createPacket(SEPARATOR);


                DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
                dataOutputStream.writeUTF(msgToSend);
                dataOutputStream.flush();


                // Listen for acknowledgement

                DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());

                String ackmsg = dataInputStream.readUTF();
                Log.d(TAG, ackmsg);

                dataInputStream.close();




                socket.close();



            } catch (SocketTimeoutException e){
                Log.e(TAG, "SocketTimeoutException!!!!!!");
                portToRemove.add(msg.getOrigin());

            } catch (UnknownHostException e) {
                Log.e(TAG, "ClientTask UnknownHostException");
                portToRemove.add(msg.getOrigin());

            } catch (IOException e) {
                Log.e(TAG, "ClientTask socket IOException");
                portToRemove.add(msg.getOrigin());

            } catch (CloneNotSupportedException e){
                Log.e(TAG, "ClientTask socket CloneNotSupportedException");
            }


            REMOTE_PORT.removeAll(portToRemove);

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
