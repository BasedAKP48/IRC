package tech.akpmakes.basedakp48;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseCredentials;
import org.kitteh.irc.client.library.Client;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

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

        FirebaseApp.initializeApp(options);

        Client client = Client.builder().nick("BasedAKP48").serverHost("irc.esper.net").build();
        client.getEventManager().registerEventListener(new IRCListener());
        client.addChannel("#AKP");
    }
}
