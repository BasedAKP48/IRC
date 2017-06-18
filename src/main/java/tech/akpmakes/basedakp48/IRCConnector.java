package tech.akpmakes.basedakp48;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseCredentials;
import com.google.firebase.database.*;
import org.kitteh.irc.client.library.Client;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Objects;

public class IRCConnector {
    public static void main(String[] args) {
        FileInputStream serviceAccount = null;
        try {
            serviceAccount = new FileInputStream("C:/basedakp48-firebase-adminsdk.json");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        FirebaseOptions options = null;
        try {
            options = new FirebaseOptions.Builder()
                    .setCredential(FirebaseCredentials.fromCertificate(serviceAccount))
                    .setDatabaseUrl("https://basedakp48.firebaseio.com/")
                    .build();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Connecting to Firebase...");
        FirebaseApp.initializeApp(options);
        FirebaseDatabase database = FirebaseDatabase.getInstance();

        // Get a unique client ID from Firebase.
        DatabaseReference clientRef = database.getReference("clients/").push();
        String cid = clientRef.getKey();
        System.out.println("Got cid "+cid+" from Firebase.");

        System.out.println("Connecting to IRC...");
        Client client = Client.builder().nick("BasedAKP48").serverHost("irc.esper.net").build();
        client.getEventManager().registerEventListener(new IRCListener(cid));
        client.addChannel("#AKP");

        System.out.println("Adding child event listener for "+clientRef.getPath()+".");
        clientRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot snapshot, String previousChildName) {
                BasedAKP48ChatMessage msg = snapshot.getValue(BasedAKP48ChatMessage.class);
                if(msg.msgType.equalsIgnoreCase("chatMessage")) {
                    client.sendMessage(msg.channel, msg.text);
                    System.out.println(msg.channel + " | " + client.getNick() + ": " + msg.text);
                }
                snapshot.getRef().removeValue();
            }

            @Override
            public void onChildChanged(DataSnapshot snapshot, String previousChildName) {}

            @Override
            public void onChildRemoved(DataSnapshot snapshot) {}

            @Override
            public void onChildMoved(DataSnapshot snapshot, String previousChildName) {}

            @Override
            public void onCancelled(DatabaseError error) {
                System.out.println("Database read failed: " + error.getCode());
            }
        });

        String line;
        while ((line = System.console().readLine()) != null) {
            if(Objects.equals(line, "/quit")) {
                System.out.println("Disconnecting...");
                client.shutdown("Goodbye for now!");
                System.out.println("Shutting down...");
                System.exit(0);
            }
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Disconnecting...");
            client.shutdown("Goodbye for now!");
            System.out.println("Shutting down...");
        }));
    }
}
