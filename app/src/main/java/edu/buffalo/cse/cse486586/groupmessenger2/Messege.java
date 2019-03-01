package edu.buffalo.cse.cse486586.groupmessenger2;

import java.util.Objects;

public class Messege implements Comparable<Messege>{

    int sequence;
    String content;
    boolean isDeliverable;
    int source;

    public Messege(int sequence, String content, boolean isDeliverable, int source) {
        this.sequence = sequence;
        this.content = content;
        this.isDeliverable = isDeliverable;
        this.source = source;
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

    @Override
    public int compareTo(Messege another) {

        int result = another.sequence-another.sequence;

        if(result == 0){
            result = this.source - another.source;
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
                '}';
    }


    @Override
    public boolean equals(Object o) {

        //TODO: removing sequence comparison, need to look for a better method

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Messege messege = (Messege) o;
        return isDeliverable == messege.isDeliverable &&
                source == messege.source &&
                Objects.equals(content, messege.content);
    }


}
