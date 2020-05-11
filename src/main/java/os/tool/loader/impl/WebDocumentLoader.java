package os.tool.loader.impl;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import os.tool.model.Document;
import os.tool.model.impl.HtmlDocument;
import os.tool.model.impl.JsonDocument;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

public class WebDocumentLoader extends WebDataLoader<Document> {
    private static final Logger logger = LogManager.getLogger(WebDocumentLoader.class);

    @Override
    public Document load(String url) throws IOException {
        logger.info("Loading {}...", url);
        CloseableHttpClient client = null;
        try {
            client = httpClient();
            HttpGet httpGet = new HttpGet(url);
            CloseableHttpResponse response = client.execute(httpGet);
            if(200!=response.getStatusLine().getStatusCode()){
                throw new RuntimeException("Request failed: "+response.getStatusLine());
            }
            String contentType = response.getFirstHeader("Content-Type").getValue();
            InputStream in = response.getEntity().getContent();
            String body = IOUtils.toString(in, StandardCharsets.UTF_8);
            return responseToDocument(contentType, body);
        } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
            throw new IOException(e.getMessage(), e);
        } finally {
            if (client != null) {
                client.close();
            }
        }
    }

    private Document responseToDocument(String contentType, String body) {
        Objects.requireNonNull(contentType, "Error: content type cannot be null");
        if (contentType.contains("text/html")) {
            return new HtmlDocument(body);
        } else if (contentType.contains("application/json")) {
            return new JsonDocument(body);
        } else {
            throw new IllegalArgumentException("Unsupported content type " + contentType);
        }
    }
}
