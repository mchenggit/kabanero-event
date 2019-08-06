package io.kabanero.event;
import io.kubernetes.client.ApiClient;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String args[] ) {
        try {
            logger.info("Starting io.kabanero.event");


            ApiClient apiClient = KubeUtils.getApiClient();

            // disable host name  verification for HTTSURLConnection calls
            HostnameVerifier trustAllHosts = new  HostnameVerifier() {
                public boolean verify(String host, SSLSession session) { return true; }
            };
            HttpsURLConnection.setDefaultHostnameVerifier(trustAllHosts);

           // TODO: Remove 
           TektonUtils.test(apiClient);

           for (; ; ) {
               // TODO: remove after we put in an infinite loop to retrieve and process messages
               Thread.currentThread().sleep(60000);
           }

        }  catch(Throwable t) {
            logger.error("Error in WASController", t);
        }
        // If we get here, something is wrong. Should loop forever
        System.exit(1);
    }
}
