package tech.akpmakes.basedakp48;

public class BasedAKP48ChatMessage {
    public String channel;
    public String uid;
    public String cid;
    public String text;

    public BasedAKP48ChatMessage(String uid, String cid, String channel, String message) {
        this.uid = uid;
        this.cid = cid;
        this.channel = channel;
        this.text = message;
    }
}
