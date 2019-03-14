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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
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
import java.util.TreeSet;
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

    private static TreeSet<Integer> REMOTE_PORT = new TreeSet<Integer>();
    private static TreeSet<Integer> BANNED_PORT = new TreeSet<Integer>();


    private static TreeMap<Long, HashMap<Integer,Messege>> proposalCounter = new TreeMap<Long, HashMap<Integer, Messege>>();
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

                    //TODO: implement without runnable

                    makeDelivery();


                }
                catch (Exception e) {
                    Log.e(TAG,"Exception in runnable!!"+e);
                }
                finally{
                    handler.postDelayed(this, 4500);
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


    private class ServerTask extends AsyncTask<ServerSocket, Messege, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];

            /* infinite while loop to accept multiple messeges */

            while (true) {

                try {


                    Socket clientSocket = serverSocket.accept();

                    DataInputStream dataInputStream = new DataInputStream(clientSocket.getInputStream());
                    String incomingMessege = dataInputStream.readUTF();


                    Messege recievedMessege = new Messege(incomingMessege, SEPARATOR);



                    // If no sequence, propose sequence number

                    if(recievedMessege.getSequence() == -1) {

                        // If NO sequence found, we need to send proposals to origin

                        recievedMessege.setSequence(proposalSeqId.getAndIncrement());
                        recievedMessege.setSource(MY_PORT);

                        // Add this messege to Priority Queue
                        messegeQueue.add(recievedMessege);

                        Log.d(TAG,"Proposed** " + recievedMessege.toString());


                        DataOutputStream dataOutputStream = new DataOutputStream(clientSocket.getOutputStream());
                        dataOutputStream.writeUTF(recievedMessege.createPacket(SEPARATOR));
                        dataOutputStream.flush();


                    } else{

                        publishProgress(recievedMessege);

                        DataOutputStream dataOutputStream = new DataOutputStream(clientSocket.getOutputStream());
                        dataOutputStream.writeUTF("ACK");
                        dataOutputStream.flush();


                    }


                    clientSocket.close();



                } catch (IOException e) {
                    Log.e(TAG, "Client Connection failed");
                }
            }


        }

        protected void onProgressUpdate(Messege...msgs) {
            /*
             * The following code displays what is received in doInBackground().
             */


            try {

                Messege messegeToAdd = msgs[0].clone();
                Messege messegeToremove = msgs[0].clone();


                if(messegeToAdd.isDeliverable()){

                    // update proposal sequence if less

                    if(messegeToAdd.getSequence()>=proposalSeqId.get()){
                        proposalSeqId.set(messegeToAdd.getSequence() + 1);
                    }


                    // Add this messege to Priority Queue
                    messegeQueue.add(messegeToAdd);



                    /* remove the old instance */

                    messegeToremove.setDeliverable(false);
                    messegeQueue.remove(messegeToremove);

                    Log.d(TAG,"QUEUED** " + messegeToAdd.toString());







                }

            } catch (CloneNotSupportedException e) {
                Log.d(TAG,"CloneNotSupportedException in queueing!!");
            }


            //makeDelivery();




            return;
        }
    }


    private class ClientTask extends AsyncTask<Messege, Void, Void> {

        @Override
        protected Void doInBackground(Messege... msgs) {


            Set<Integer> portList = new HashSet<Integer>();
            portList.addAll(REMOTE_PORT);



            for (int thisPort: portList) {

                try {

                    Messege msg = msgs[0].clone();

                    Socket socket = connectionAndwriteMessege(thisPort, msg);
                    readAckAndClose(socket);

                    socket.close();



                } catch (SocketTimeoutException e){
                    Log.e(TAG, "ClientTask SocketTimeoutException");
                    REMOTE_PORT.remove(new Integer(thisPort));
                    BANNED_PORT.add(new Integer(thisPort));

                } catch (UnknownHostException e) {
                    Log.e(TAG, "ClientTask UnknownHostException");
                    REMOTE_PORT.remove(new Integer(thisPort));
                    BANNED_PORT.add(new Integer(thisPort));

                } catch (IOException e) {
                    Log.e(TAG, "ClientTask socket IOException: "+thisPort);
                    REMOTE_PORT.remove(new Integer(thisPort));
                    BANNED_PORT.add(new Integer(thisPort));

                } catch (CloneNotSupportedException e){
                    Log.e(TAG, "ClientTask socket CloneNotSupportedException");
                } finally {

                    // check if ready to deliver

                }
            }



            return null;
        }
    }

    private Socket connectionAndwriteMessege(int thisPort, Messege msg) throws IOException {

        Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                thisPort);

        socket.setSoTimeout(1000);

        msg.setSource(MY_PORT);
        String msgToSend = msg.createPacket(SEPARATOR);

        DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
        dataOutputStream.writeUTF(msgToSend);
        dataOutputStream.flush();

        return socket;

    }


    private void readAckAndClose(Socket socket) throws IOException{

        DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());

        String reply = dataInputStream.readUTF();

        if(reply.equals("ACK")){

            // all good

        } else {
            // recieved proposal

            Messege repliedMsg = new Messege(reply, SEPARATOR);


            HashMap<Integer, Messege> mp = proposalCounter.get(repliedMsg.getOriginTimestamp());

            if(mp == null){
                mp = new HashMap<Integer, Messege>();
            }

            mp.put(repliedMsg.getSource(), repliedMsg);
            proposalCounter.put(repliedMsg.getOriginTimestamp(), mp);


            makeDecisionOnSequence(repliedMsg.getOriginTimestamp());







        }

        dataInputStream.close();

    }

    private void makeDecisionOnSequence(long originTimestamp){


        // add proposal checking


        HashMap<Integer,Messege> headCounter = proposalCounter.get(originTimestamp);


        if(headCounter!= null && headCounter.size() >= REMOTE_PORT.size()){




            //choose highest and let others know to make it depliverable.

            int highestProposedSequence = 0;

            Messege decision =null;

            boolean isDeliverable = true;

            try {

                for(int port:REMOTE_PORT){

                    if(headCounter.get(port).getSequence()>=highestProposedSequence){

                        decision = headCounter.get(port);
                        highestProposedSequence = headCounter.get(port).getSequence();

                    }

                }
            } catch (Exception e) {
                isDeliverable = false;
            }

            if(isDeliverable){
                decision.setDeliverable(true);


                Log.d(TAG,"AGREED And TRANSMITTED:: " + decision.toString());

//                                try {
//                    Thread.sleep(1000);
//
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }

                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, decision);

                proposalCounter.remove(decision.getOriginTimestamp());

            }

        }


    }


    private void makeDelivery(){


        while (!messegeQueue.isEmpty() ) {




            Messege peekedMessege = messegeQueue.peek();

            //remove head if the origin is failed node.
            if(BANNED_PORT.contains(peekedMessege.getOrigin())){
                messegeQueue.poll();
                continue;

            }

            if(peekedMessege.isDeliverable()){


                Messege topMessege =  messegeQueue.poll();

                int finalSeq = dbSequence.getAndIncrement();

                ContentValues mContentValues = new ContentValues();

                mContentValues.put(KEY_FIELD, finalSeq);
                mContentValues.put(VALUE_FIELD, topMessege.getContent());

                getContentResolver().insert(mUri, mContentValues);


                String colorKey = (String) getResources().getText(getResources().getIdentifier("c_"+topMessege.getOrigin(), "string", "edu.buffalo.cse.cse486586.groupmessenger2"));


                tv.append(Html.fromHtml(finalSeq+ "*"+topMessege.getSequence() +":"+ topMessege.getOrigin() +":" +": <font color='"+colorKey+"'>"+topMessege.getContent()+ "</color>"));
                tv.append("\n");




            } else{
                //if head is not deliverable, exit
                break;
            }
        }


    }


    private static Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }




}
