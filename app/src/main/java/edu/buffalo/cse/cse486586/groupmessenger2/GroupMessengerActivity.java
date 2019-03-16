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
import java.util.Iterator;
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
    private static final int READ_TIMEOUT_RANGE = 1500;
    private static final int DELIVERY_INTERVAL = 4500;

    private static AtomicInteger proposalSeqId = new AtomicInteger(0);
    private static AtomicInteger dbSequence = new AtomicInteger(0);

    private static final String KEY_FIELD = "key";
    private static final String VALUE_FIELD = "value";
    private static final String SEPARATOR = "##";
    private static final String PING_VALUE = "ping!";
    private static final String ACK_VALUE = "ACK";

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


                    editText.setText("");

                    /* Construct the Messege for first time */

                    Messege messege = new Messege(-1, msg,false, MY_PORT, MY_PORT, System.currentTimeMillis());


                    /* Ask for sequence proposal from the alive nodes in the network */

                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, messege);

                    Log.e(TAG,msg);


                }



            }
        });




        /*
        * Clearing the delivery queue in defined interval
        * https://stackoverflow.com/a/10207775
        *
        * */


        final Handler handler = new Handler();

        Runnable runnable = new Runnable() {

            @Override
            public void run() {
                try{

                    makeDelivery();


                }
                catch (Exception e) {
                    Log.e(TAG,"Exception in runnable!!"+e);
                }
                finally{
                    handler.postDelayed(this, DELIVERY_INTERVAL);
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





                    if(incomingMessege!=null && incomingMessege.equals(PING_VALUE)){

                        /* If it's a ping! to cross-check life, return AcK */

                        returnStandardAcknoldegement(clientSocket);


                    } else  {

                        Messege recievedMessege = new Messege(incomingMessege, SEPARATOR);


                        if(recievedMessege.getSequence() == -1) {


                            /* If NO sequence found, we need to send proposals
                             * to the origin node
                             * */

                            recievedMessege.setSequence(proposalSeqId.getAndIncrement());
                            recievedMessege.setSource(MY_PORT);

                            /* Add messege with proposed sequence to Priority Queue */

                            messegeQueue.add(recievedMessege);

                            Log.d(TAG,"Proposed** " + recievedMessege.toString());

                            /* Send back the proposal through channel as Ack */

                            DataOutputStream dataOutputStream = new DataOutputStream(clientSocket.getOutputStream());
                            dataOutputStream.writeUTF(recievedMessege.createPacket(SEPARATOR));
                            dataOutputStream.flush();

                            dataOutputStream.close();

                        } else{

                            /* Decision for a sequence found
                            * Update the delivery queue with the decision
                            * */

                            publishProgress(recievedMessege);

                            /* Return standard Ack */
                            returnStandardAcknoldegement(clientSocket);

                        }
                    }


                    clientSocket.close();


                } catch (IOException e) {
                    Log.e(TAG, "Client Connection failed");
                }
            }


        }

        protected void onProgressUpdate(Messege...msgs) {


            try {

                Messege messegeToAdd = msgs[0].clone();
                Messege messegeToremove = msgs[0].clone();


                if(messegeToAdd.isDeliverable()){

                    /* Update proposal Sequence larger than all observed agreed priorities */

                    if(messegeToAdd.getSequence()>=proposalSeqId.get()){
                        proposalSeqId.set(messegeToAdd.getSequence() + 1);
                    }


                    /* Add the ready messege to the queue for delivery */
                    messegeQueue.add(messegeToAdd);


                    /* remove the old instance from queue */

                    messegeToremove.setDeliverable(false);
                    messegeQueue.remove(messegeToremove);

                    Log.d(TAG,"QUEUED** " + messegeToAdd.toString());


                }

            } catch (CloneNotSupportedException e) {
                Log.d(TAG,"CloneNotSupportedException in queueing!!");
            }


            return;
        }
    }


    private class ClientTask extends AsyncTask<Messege, Void, Void> {

        @Override
        protected Void doInBackground(Messege... msgs) {


            Set<Integer> portList = new HashSet<Integer>();
            portList.addAll(REMOTE_PORT);

            /*
            * Send messeges to alive nodes one by one
            * */


            for (int thisPort: portList) {

                try {

                    Messege msg = msgs[0].clone();

                    Socket socket = connectAndwriteMessege(thisPort, msg);
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
                }
            }


            /* make a decision from proposals collected from alive nodes */

            makeDecisionOnSequence(msgs[0].getOriginTimestamp());



            return null;
        }
    }

    /* Writes standard ACK in open channel */

    private void returnStandardAcknoldegement(Socket clientSocket) throws IOException{

        DataOutputStream dataOutputStream = new DataOutputStream(clientSocket.getOutputStream());
        dataOutputStream.writeUTF(ACK_VALUE);
        dataOutputStream.flush();

        dataOutputStream.close();

    }

    /*
    * Establish connecton to another node and write send a Messege Object
    * */

    private Socket connectAndwriteMessege(int thisPort, Messege msg) throws IOException {

        Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                thisPort);

        socket.setSoTimeout(READ_TIMEOUT_RANGE);


        String msgToSend = msg.createPacket(SEPARATOR);

        DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
        dataOutputStream.writeUTF(msgToSend);
        dataOutputStream.flush();

        return socket;

    }

    /*
     * Establish connecton to another node and write send a String
     * */

    private Socket connectAndwriteMessege(int thisPort, String msg) throws IOException {

        Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                thisPort);

        socket.setSoTimeout(READ_TIMEOUT_RANGE);


        String msgToSend = msg;

        DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
        dataOutputStream.writeUTF(msgToSend);
        dataOutputStream.flush();

        return socket;

    }


    /*
     * Wait for Ack from other nodes!
     * If recieved proposals buffer them for later evaluation
     * */

    private void readAckAndClose(Socket socket) throws IOException{

        DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());

        String reply = dataInputStream.readUTF();

        if(reply.equals(ACK_VALUE)){

            /* Standard ACK recieved. */

        } else {

            /*
            * Recieved proposals!
            * Store it in buffer to evaluate later
            * */

            Messege repliedMsg = new Messege(reply, SEPARATOR);


            HashMap<Integer, Messege> mp = proposalCounter.get(repliedMsg.getOriginTimestamp());

            if(mp == null){
                mp = new HashMap<Integer, Messege>();
            }

            mp.put(repliedMsg.getSource(), repliedMsg);
            proposalCounter.put(repliedMsg.getOriginTimestamp(), mp);



        }

        dataInputStream.close();

    }

    /*
     * Evaluate all the proposals and make a decision on sequence number for a messege
     * */

    private void makeDecisionOnSequence(long originTimestamp){


        HashMap<Integer,Messege> headCounter = proposalCounter.get(originTimestamp);


        if(headCounter!= null && headCounter.size() >= REMOTE_PORT.size()){


            /* Choose the highest sequence number from the buffer */

            int highestProposedSequence = 0;

            Messege decision =null;

            boolean isDeliverable = true;

            try {

                /* Make sure only proposals from alive nodes are evaluated.
                * Evaluated in sorted manner, node with highest address (port)
                * should get priority
                * */

                Iterator<Integer> value = REMOTE_PORT.iterator();
                while(value.hasNext()){

                    Integer port = value.next();

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

                /* broadcast decision to all the nodes */

                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, decision);

                proposalCounter.remove(decision.getOriginTimestamp());

            }

        }


    }


    /*
     * Periodically checks the delivery queue for deliverable messeges
     * */

    private void makeDelivery(){


        while (!messegeQueue.isEmpty() ) {


            Messege peekedMessege = messegeQueue.peek();



            if(BANNED_PORT.contains(peekedMessege.getOrigin()) && !peekedMessege.isDeliverable()){

                /*
                 * If non-deliverable head is from a dead node, remove them
                 */

                messegeQueue.poll();
                continue;

            }

            if(peekedMessege.isDeliverable()){

                /* If the head is ready for delivery,
                * Store it and show it to user
                * */

                Messege topMessege =  messegeQueue.poll();

                int finalSeq = dbSequence.getAndIncrement();

                ContentValues mContentValues = new ContentValues();

                mContentValues.put(KEY_FIELD, finalSeq);
                mContentValues.put(VALUE_FIELD, topMessege.getContent());

                getContentResolver().insert(mUri, mContentValues);


                String colorKey = (String) getResources().getText(getResources().getIdentifier("c_"+topMessege.getOrigin(), "string", "edu.buffalo.cse.cse486586.groupmessenger2"));


                tv.append(Html.fromHtml(finalSeq+ "*"+topMessege.getSequence() +":"+ topMessege.getSource() +":" +": <font color='"+colorKey+"'>"+topMessege.getContent()+ "</color>"));
                tv.append("\n");




            } else{

                /* If head is not deliverable, ping the origin node to check if it is still alive */

                new ReconfirmLife().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, String.valueOf(peekedMessege.getOrigin()));

                // exit and try again later
                break;
            }
        }


    }

    /*
    * Sends a string to a destination.
    * Used to verify if the target node is still alive.
    * */

    private class ReconfirmLife extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... ports) {

            int thisPort = Integer.parseInt(ports[0]);

                try {

                    Socket socket = connectAndwriteMessege(thisPort, PING_VALUE);
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
