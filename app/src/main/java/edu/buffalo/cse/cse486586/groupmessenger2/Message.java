package edu.buffalo.cse.cse486586.groupmessenger2;

import java.util.Objects;

public class Message implements Comparable<Message>, Cloneable{

    private int sequence;
    private String content;
    private boolean isDeliverable;
    private int source;
    private int origin;
    private long originTimestamp;

    public Message(int sequence, String content, boolean isDeliverable, int source, int origin, long originTimestamp) {
        this.sequence = sequence;
        this.content = content;
        this.isDeliverable = isDeliverable;
        this.source = source;
        this.origin = origin;
        this.originTimestamp = originTimestamp;
    }

    public Message(String packet, String separator) {


        String strReceived [] = packet.trim().split(separator);

        this.sequence = -1;

        if(strReceived[0]!=null && strReceived[0].length()>0){
            this.sequence = Integer.parseInt(strReceived[0]);
        }

        this.content = strReceived[1];
        this.isDeliverable = strReceived[2].equals("1")?true:false;
        this.source = Integer.parseInt(strReceived[3]);
        this.origin = Integer.parseInt(strReceived[4]);
        this.originTimestamp = Long.parseLong(strReceived[5]);


    }

    public int getSequence() {
        return sequence;
    }

    public void setSequence(int sequence) {
        this.sequence = sequence;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean isDeliverable() {
        return isDeliverable;
    }

    public void setDeliverable(boolean deliverable) {
        isDeliverable = deliverable;
    }

    public int getSource() {
        return source;
    }

    public void setSource(int source) {
        this.source = source;
    }

    public int getOrigin() {
        return origin;
    }

    public void setOrigin(int origin) {
        this.origin = origin;
    }

    public long getOriginTimestamp() {
        return originTimestamp;
    }

    public void setOriginTimestamp(long originTimestamp) {
        this.originTimestamp = originTimestamp;
    }

    @Override
    public int compareTo(Message another) {

        int result = Integer.compare(this.sequence, another.sequence);

        if(result == 0){
            result = Integer.compare(this.source, another.source);
        }

        return result;
    }

    @Override
    public String toString() {
        return "Message{" +
                "sequence=" + sequence +
                ", content='" + content + '\'' +
                ", isDeliverable=" + isDeliverable +
                ", source=" + source +
                ", origin=" + origin +
                ", originTimestamp=" + originTimestamp +
                '}';
    }

    public String createPacket(String separator){

        String deliveryStatus = isDeliverable()?"1":"0";

        return String.valueOf(getSequence()) + separator + getContent() +
                separator + deliveryStatus + separator + String.valueOf(getSource()) +
                separator + String.valueOf(getOrigin() + separator + String.valueOf(getOriginTimestamp()));

    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Message message = (Message) o;
        return isDeliverable == message.isDeliverable &&
                origin == message.origin &&
                originTimestamp == message.originTimestamp &&
                Objects.equals(content, message.content);
    }

    public Message clone() throws CloneNotSupportedException
    {
        return (Message) super.clone();
    }


}
