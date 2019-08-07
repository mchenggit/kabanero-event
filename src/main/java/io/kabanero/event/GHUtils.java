package io.kabanero.event;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.HttpClientBuilder;

import java.net.URL;

public class GHUtils {

    public static String getFile(URL url, String file) {
        try {
            String contentUri = "/repos" + url.getFile() + "/contents/" + file;
            url = new URL(url.getProtocol(), "api." + url.getHost(), url.getPort(), contentUri);
        } catch (Exception e) {
            e.printStackTrace();
        }

        CredentialsProvider provider = new BasicCredentialsProvider();
        UsernamePasswordCredentials gheCreds = new UsernamePasswordCredentials(System.getenv("GHE_USER"), System.getenv("GHE_TOKEN"));
        provider.setCredentials(AuthScope.ANY, gheCreds);

        HttpHost gheHost = new HttpHost("api.github.ibm.com", 443, "https");
        AuthCache authCache = new BasicAuthCache();
        authCache.put(gheHost, new BasicScheme());

        HttpClientContext context = HttpClientContext.create();
        context.setCredentialsProvider(provider);
        context.setAuthCache(authCache);

        HttpClient httpClient = HttpClientBuilder.create().build();

        try {
            HttpGet request = new HttpGet(url.toURI());
            HttpResponse response = httpClient.execute(request, context);
            ResponseHandler<String> handler = new BasicResponseHandler();
            return handler.handleResponse(response);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
