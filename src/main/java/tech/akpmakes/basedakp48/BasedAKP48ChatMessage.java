package tech.akpmakes.basedakp48;

import java.util.Date;

public class BasedAKP48ChatMessage {
    public String msgType;
    public String channel;
    public String uid;
    public String cid;
    public String text;
    public long timeReceived;

    public BasedAKP48ChatMessage(){}

    public BasedAKP48ChatMessage(String uid, String cid, String channel, String message) {
        this.uid = uid;
        this.cid = cid;
        this.channel = channel;
        this.text = message;
        this.msgType = "chatMessage";
        this.timeReceived = new Date().getTime();
    }
}
