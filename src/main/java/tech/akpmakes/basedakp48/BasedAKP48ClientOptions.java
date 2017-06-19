package tech.akpmakes.basedakp48;

import java.util.ArrayList;

public class BasedAKP48ClientOptions {
    public String nick;
    public String server;
    public ArrayList<String> channels;

    public BasedAKP48ClientOptions(){}

    public BasedAKP48ClientOptions(String nick, String server, ArrayList<String> channels) {
        this.nick = nick;
        this.server = server;
        this.channels = channels;
    }
}
