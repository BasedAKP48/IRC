package tech.akpmakes.basedakp48;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseCredentials;
import com.google.firebase.database.*;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import org.kitteh.irc.client.library.Client;

import java.io.*;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Properties;

public class IRCConnector {
    private static Client _client;

    public static void main(String[] args) {
        ArgumentParser parser = ArgumentParsers.newArgumentParser("irc-connector")
                .defaultHelp(true)
                .description("Connect an IRC server to an instance of BasedAKP48.");
        parser.addArgument("-p", "--properties").nargs("*")
                .setDefault("irc-connector.properties")
                .help("Specify properties file to use");
        parser.addArgument("-s", "--serviceAccount").nargs("*")
                .setDefault("serviceAccount.json")
                .help("Specify Firebase service account JSON file to use");
        Namespace ns = null;
        try {
            ns = parser.parseArgs(args);
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            System.exit(1);
        }

        FileInputStream serviceAccount = null;
        try {
            Path saPath = FileSystems.getDefault().getPath(ns.getString("serviceAccount"));
            serviceAccount = new FileInputStream(saPath.toAbsolutePath().toString());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.exit(0);
        }

        FirebaseOptions options = null;
        try {
            options = new FirebaseOptions.Builder()
                    .setCredential(FirebaseCredentials.fromCertificate(serviceAccount))
                    .setDatabaseUrl("https://basedakp48.firebaseio.com/")
                    .build();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(0);
        }

        System.out.println("Connecting to Firebase...");
        FirebaseApp.initializeApp(options);
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference rootRef = database.getReference();

        Properties prop = new Properties();
        Path propPath = FileSystems.getDefault().getPath((ns.getString("properties")));
        File propertiesFile = new File(propPath.toAbsolutePath().toString());
        String cid = null;
        try {
            FileInputStream properties = new FileInputStream(propertiesFile);
            prop.load(properties);
            cid = prop.getProperty("cid");
        } catch (FileNotFoundException e) {
            // No properties file.
            try {
                propertiesFile.createNewFile();
                FileOutputStream out = new FileOutputStream(propertiesFile);
                // Get a unique client ID from Firebase.
                DatabaseReference clientRef = database.getReference("clients/").push();
                cid = clientRef.getKey();
                prop.setProperty("cid", cid);
                prop.store(out, null);
            } catch (IOException e1) {
                e1.printStackTrace();
                System.exit(0);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(0);
        }

        if(cid == null) {
            System.out.println("Something really weird happened, and we couldn't get a client ID. Good luck!");
            System.exit(0);
        }

        DatabaseReference configRef = rootRef.child("config/clients/"+cid);
        String finalCid = cid;
        configRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                BasedAKP48ClientOptions config = snapshot.getValue(BasedAKP48ClientOptions.class);
                if(config == null) { System.out.println("You need to set up your client config at \"config/clients/"+ finalCid +"\"!"); System.exit(0); }
                if(config.nick == null) { config.nick = "BasedAKP48"; }
                if(config.server == null) { System.out.println("No server specified! Goodbye!"); System.exit(0); }
                if(config.channels == null) { System.out.println("No channels specified! Goodbye!"); System.exit(0); }

                if(_client == null) {
                    System.out.println("Connecting to IRC...");
                    _client = createClient(config, finalCid);
                    addClientChildListener(rootRef, finalCid, _client);
                } else {
                    // We should update the config here, I guess.
                    System.out.println("Caught an update to the config. Someday we'll know how to deal with this.");
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                System.out.println("Database read failed: " + error.getCode());
            }
        });

        String line;
        while ((line = System.console().readLine()) != null) {
            if(Objects.equals(line, "/quit")) {
                System.out.println("Disconnecting...");
                _client.shutdown("Goodbye for now!");
                System.out.println("Shutting down...");
                System.exit(0);
            }
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Disconnecting...");
            _client.shutdown("Goodbye for now!");
            System.out.println("Shutting down...");
        }));
    }

    private static Client createClient(BasedAKP48ClientOptions options, String cid) {
        Client.Builder builder = Client.builder();
        builder.nick(options.nick).serverHost(options.server);

        if(options.username != null) {
            builder.user(options.username);
        }

        if(options.password != null) {
            builder.serverPassword(options.password);
        }

        Client client = builder.build();
        client.getEventManager().registerEventListener(new IRCListener(cid));
        for(String channel : options.channels) {
            client.addChannel(channel);
        }

        return client;
    }

    private static void addClientChildListener(DatabaseReference rootRef, String cid, Client client) {
        DatabaseReference clientRef = rootRef.child("clients/"+cid);
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
    }
}
