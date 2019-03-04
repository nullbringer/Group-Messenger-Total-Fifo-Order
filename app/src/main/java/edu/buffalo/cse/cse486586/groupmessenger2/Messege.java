package edu.buffalo.cse.cse486586.groupmessenger2;

import java.util.Objects;

public class Messege implements Comparable<Messege>{

    private int sequence;
    private String content;
    private boolean isDeliverable;
    private int source;
    private int origin;
// TODO: can timestamp work to implement equals without sequence? and FIFO ordering
//    private int timestamp;

    public Messege(int sequence, String content, boolean isDeliverable, int source, int origin) {
        this.sequence = sequence;
        this.content = content;
        this.isDeliverable = isDeliverable;
        this.source = source;
        this.origin = origin;
    }

    public Messege(Messege msg) {

        this(msg.getSequence(), msg.getContent(), msg.isDeliverable(), msg.getSource(), msg.getOrigin());
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

    @Override
    public int compareTo(Messege another) {

        //TODO: check priority: higher or lower


        int result = Integer.compare(this.sequence, another.sequence);

        if(result == 0){
            result = Integer.compare(this.source, another.source);
        }

        return result;
    }

    @Override
    public String toString() {
        return "Messege{" +
                "sequence=" + sequence +
                ", content='" + content + '\'' +
                ", isDeliverable=" + isDeliverable +
                ", source=" + source +
                ", origin=" + origin +
                '}';
    }


    public String createPacket(String separator){


        String deliveryStatus = isDeliverable()?"1":"0";

        return String.valueOf(getSequence()) + separator + getContent() +
                separator + deliveryStatus + separator + String.valueOf(getSource()) +
                separator + String.valueOf(getOrigin());

    }

    @Override
    public boolean equals(Object o) {

        //TODO: removing sequence comparison, need to look for a better method, may be timestamp

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Messege messege = (Messege) o;
        return isDeliverable == messege.isDeliverable &&
                source == messege.source &&
                origin == messege.origin &&
                Objects.equals(content, messege.content);
    }


}
