package edu.wisc.cs.scraping_tool;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.StoredCredential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.DataStore;
import com.google.api.client.util.store.FileDataStoreFactory;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;

/**
 * Shared class used by every sample. Contains methods for authorizing a user and caching
 * credentials.
 */
public class Auth {

    /**
     * Define a global instance of the HTTP transport.
     */
    public static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

    /**
     * Define a global instance of the JSON factory.
     */
    public static final JsonFactory JSON_FACTORY = new JacksonFactory();

    /**
     * This is the directory that will be used under the user's home directory where OAuth tokens
     * will be stored.
     */
    private static final String CREDENTIALS_DIRECTORY = ".oauth-credentials";

    /**
     * Authorizes the installed application to access user's protected data.
     *
     * @param scopes list of scopes needed to run youtube upload.
     * @param credentialDatastore name of the credential datastore to cache OAuth tokens
     */
    public static Credential authorize(List<String> scopes, String credentialDatastore)
                    throws IOException {
        // insecure way to store credentials, but it does hide them from simple users
        // if quota becomes an issue, this can get changed to referencing client_secrets.json
        String secrets = "{\"web\":{\"client_id\":\"918023368365-pvr624n2jqh6uvsjskvs3tplib8l4p7i."
                        + "apps.googleusercontent.com\",\"project_id\":\"toru5-music-scraper\",\"au"
                        + "th_uri\":\"https://accounts.google.com/o/oauth2/auth\",\"token_uri\":\"h"
                        + "ttps://www.googleapis.com/oauth2/v3/token\",\"auth_provider_x509_cert_ur"
                        + "l\":\"https://www.googleapis.com/oauth2/v1/certs\",\"client_secret\":\"U"
                        + "lT3BZQgSqDFkbMtm7eLFACS\"}}";

        InputStream secretStream = new ByteArrayInputStream(secrets.getBytes());

        // Load client secrets.
        Reader clientSecretReader = new InputStreamReader(secretStream);
        GoogleClientSecrets clientSecrets =
                        GoogleClientSecrets.load(JSON_FACTORY, clientSecretReader);

        // this is the portion that would be added to use client_secrets.json:

        // Reader clientSecretReader = new InputStreamReader(
        // Auth.class.getResourceAsStream("/client_secrets.json"));
        // GoogleClientSecrets clientSecrets =
        // GoogleClientSecrets.load(JSON_FACTORY, clientSecretReader);

        // this would also be added with use of client_secrets.json:

        // Checks that the defaults have been replaced (Default = "Enter X here").
        // if (clientSecrets.getDetails().getClientId().startsWith("Enter")
        // || clientSecrets.getDetails().getClientSecret().startsWith("Enter ")) {
        // System.out.println(
        // "Enter Client ID and Secret from
        // https://console.developers.google.com/project/_/apiui/credential "
        // + "into src/main/resources/client_secrets.json");
        // System.exit(1);
        // }

        // This creates the credentials datastore at ~/.oauth-credentials/${credentialDatastore}
        FileDataStoreFactory fileDataStoreFactory = new FileDataStoreFactory(
                        new File(System.getProperty("user.home") + "/" + CREDENTIALS_DIRECTORY));
        DataStore<StoredCredential> datastore =
                        fileDataStoreFactory.getDataStore(credentialDatastore);

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT,
                        JSON_FACTORY, clientSecrets, scopes).setCredentialDataStore(datastore)
                                        .build();

        // Build the local server and bind it to port 8080
        LocalServerReceiver localReceiver = new LocalServerReceiver.Builder().setPort(8080).build();

        // Authorize.
        return new AuthorizationCodeInstalledApp(flow, localReceiver).authorize("user");
    }
}
