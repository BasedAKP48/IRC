package tech.akpmakes.basedakp48;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import net.engio.mbassy.listener.Handler;
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent;

public class IRCListener {
    private final FirebaseDatabase database = FirebaseDatabase.getInstance();
    private final DatabaseReference ref;
    private final String cid;

    public IRCListener(String cid) {
        this.ref = database.getReference("");
        this.cid = cid;
    }

    @Handler
    public void onMessage(ChannelMessageEvent event) {
        DatabaseReference msgRef = this.ref.child("inbound_raw_messages").push();
        BasedAKP48ChatMessage msg = new BasedAKP48ChatMessage(event.getActor().getName(), this.cid, event.getChannel().getName(), event.getMessage());
        msgRef.setValue(msg);

        System.out.println(event.getChannel().getName() + " | " + event.getActor().getMessagingName() + ": " + event.getMessage());
    }
}
