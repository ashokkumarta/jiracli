/**
 * Copyright 2016 Pascal
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.pascalgn.jiracli.context;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.Charset;
import java.security.AccessControlException;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.SSLContext;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScheme;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.pascalgn.jiracli.util.Consumer;
import com.github.pascalgn.jiracli.util.Credentials;
import com.github.pascalgn.jiracli.util.Function;
import com.github.pascalgn.jiracli.util.IOUtils;
import com.github.pascalgn.jiracli.util.StringUtils;
import com.github.pascalgn.jiracli.util.Supplier;

class HttpClient implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpClient.class);

    private static final int MAX_ERROR_LENGTH = 500;

    private static final Function<Reader, String> TO_STRING;
    private static final SSLConnectionSocketFactory SSL_SOCKET_FACTORY;

    static {
        TO_STRING = new Function<Reader, String>() {
            @Override
            public String apply(Reader reader) {
                return IOUtils.toString(reader);
            }
        };

        SSLContext sslcontext;
        try {
            sslcontext = SSLContexts.custom().loadTrustMaterial(null, new TrustSelfSignedStrategy()).build();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
        SSL_SOCKET_FACTORY = new SSLConnectionSocketFactory(sslcontext, NoopHostnameVerifier.INSTANCE);
    }

    private final Map<String, Credentials> credentials;
    private final Supplier<String> baseUrl;
    private final CloseableHttpClient httpClient;
    private final HttpClientContext httpClientContext;

    public HttpClient(Supplier<String> baseUrl, Function<String, Credentials> credentials) {
        this.credentials = new HashMap<String, Credentials>();
        this.baseUrl = baseUrl;
        this.httpClient = createHttpClient(credentials);
        this.httpClientContext = createHttpClientContext(credentials);
    }

    private static CloseableHttpClient createHttpClient(final Function<String, Credentials> credentials) {
        HttpClientBuilder httpClientBuilder = HttpClients.custom();
        httpClientBuilder.setSSLSocketFactory(SSL_SOCKET_FACTORY);
        return httpClientBuilder.build();
    }

    private HttpClientContext createHttpClientContext(final Function<String, Credentials> credentials) {
        HttpClientContext context = HttpClientContext.create();

        CredentialsProvider credentialsProvider = new CredentialsProvider() {
            @Override
            public void setCredentials(AuthScope authscope, org.apache.http.auth.Credentials credentials) {
            }

            @Override
            public org.apache.http.auth.Credentials getCredentials(AuthScope authscope) {
                String baseUrl = getBaseUrl();
                Credentials c = HttpClient.this.credentials.get(baseUrl);
                if (c == null) {
                    c = credentials.apply(authscope.getOrigin().toURI());
                    if (c == null) {
                        throw new IllegalStateException("No credentials provided!");
                    }
                    HttpClient.this.credentials.put(baseUrl, c);
                }
                if (c == Credentials.getAnonymous()) {
                    return null;
                } else {
                    return new UsernamePasswordCredentials(c.getUsername(), new String(c.getPassword()));
                }
            }

            @Override
            public void clear() {
            }
        };

        final AuthScheme authScheme = new BasicScheme();
        AuthCache authCache = new AuthCache() {
            @Override
            public void remove(HttpHost host) {
            }

            @Override
            public void put(HttpHost host, AuthScheme authScheme) {
            }

            @Override
            public AuthScheme get(HttpHost host) {
                return authScheme;
            }

            @Override
            public void clear() {
            }
        };

        context.setCredentialsProvider(credentialsProvider);
        context.setAuthCache(authCache);

        return context;
    }

    public String getBaseUrl() {
        String url = baseUrl.get();
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalStateException("No base URL provided!");
        }
        if (url.endsWith("/") || !url.equals(url.trim())) {
            throw new IllegalStateException("Invalid base URL: " + url);
        }
        return url;
    }

    public String get(String path) {
        return get(path, TO_STRING);
    }

    public <T> T get(String path, Function<Reader, T> function) {
        return execute(new HttpGet(getUrl(path)), function);
    }

    public void get(final URI uri, final Consumer<InputStream> consumer) {
        execute0(new HttpGet(uri), true, new Function<HttpEntity, Void>() {
            @Override
            public Void apply(HttpEntity entity) {
                if (entity == null) {
                    throw new IllegalStateException("No response!");
                } else {
                    try (InputStream input = entity.getContent()) {
                        consumer.accept(input);
                    } catch (IOException e) {
                        throw new IllegalStateException("Could not read response for URL: " + uri, e);
                    }
                }
                return null;
            }
        });
    }

    public String post(String path, String body) {
        return post(path, body, TO_STRING);
    }

    public <T> T post(String path, String body, Function<Reader, T> function) {
        HttpPost request = new HttpPost(getUrl(path));
        request.setEntity(new StringEntity(body, ContentType.APPLICATION_JSON));
        return execute(request, function);
    }

    public String put(String path, String body) {
        return put(path, body, TO_STRING);
    }

    public <T> T put(String path, String body, Function<Reader, T> function) {
        HttpPut request = new HttpPut(getUrl(path));
        request.setEntity(new StringEntity(body, ContentType.APPLICATION_JSON));
        return execute(request, function);
    }

    private String getUrl(String path) {
        if (!path.startsWith("/")) {
            throw new IllegalArgumentException("Invalid path: " + path);
        }
        return getBaseUrl() + path;
    }

    private <T> T execute(final HttpUriRequest request, final Function<Reader, T> function) {
        return execute0(request, true, new Function<HttpEntity, T>() {
            @Override
            public T apply(HttpEntity entity) {
                return (entity == null ? null : readResponse(request.getURI(), entity, function));
            }
        });
    }

    private <T> T execute0(HttpUriRequest request, boolean retry, Function<HttpEntity, T> function) {
        LOGGER.debug("Calling URL: {} [{}]", request.getURI(), request.getMethod());

        HttpResponse response;
        try {
            response = httpClient.execute(request, httpClientContext);
        } catch (IOException e) {
            throw new IllegalStateException("Could not call URL: " + request.getURI());
        }

        LOGGER.debug("Response received ({})", response.getStatusLine().toString().trim());

        HttpEntity entity = response.getEntity();
        try {
            int statusCode = response.getStatusLine().getStatusCode();
            if (isSuccess(statusCode)) {
                return function.apply(entity);
            } else {
                if (statusCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    resetAuthentication();
                    if (retry) {
                        return execute0(request, false, function);
                    } else {
                        throw new AccessControlException("Unauthorized [401]: " + request.getURI());
                    }
                } else if (statusCode == HttpURLConnection.HTTP_FORBIDDEN) {
                    resetAuthentication();
                    checkAccountLocked(response);
                    if (retry) {
                        return execute0(request, false, function);
                    } else {
                        throw new AccessControlException("Forbidden [403]: " + request.getURI());
                    }
                } else if (statusCode == HttpURLConnection.HTTP_NOT_FOUND) {
                    throw new NoSuchElementException("Not found [404]: " + request.getURI());
                } else {
                    String status = response.getStatusLine().toString().trim();
                    String message;
                    if (entity == null) {
                        message = status;
                    } else {
                        String error = readErrorResponse(request.getURI(), entity);
                        message = status + (error.isEmpty() ? "" : ": " + error);
                    }
                    if (statusCode == HttpURLConnection.HTTP_NOT_FOUND) {
                        throw new NoSuchElementException(message);
                    } else {
                        throw new IllegalStateException(message);
                    }
                }
            }
        } finally {
            EntityUtils.consumeQuietly(entity);
        }
    }

    private static boolean isSuccess(int statusCode) {
        return statusCode >= 200 && statusCode <= 299;
    }

    private static <T> T readResponse(URI uri, HttpEntity entity, Function<Reader, T> function) {
        try (InputStream input = entity.getContent()) {
            try (Reader reader = new InputStreamReader(input, getEncoding(entity))) {
                return function.apply(reader);
            }
        } catch (RuntimeException e) {
            throw new IllegalStateException("Could not read response for URL: " + uri, e);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read response for URL: " + uri, e);
        }
    }

    private static Charset getEncoding(HttpEntity entity) {
        if (entity.getContentEncoding() != null) {
            String value = entity.getContentEncoding().getValue();
            if (value != null) {
                try {
                    return Charset.forName(value);
                } catch (RuntimeException e) {
                    // use the default charset!
                    LOGGER.debug("Unsupported charset: {}", value, e);
                }
            }
        }
        return Charset.defaultCharset();
    }

    private void resetAuthentication() {
        String url = getBaseUrl();
        credentials.remove(url);
        AuthState authState = httpClientContext.getTargetAuthState();
        if (authState != null) {
            authState.reset();
        }
    }

    private static void checkAccountLocked(HttpResponse response) {
        Header header = response.getLastHeader("X-Authentication-Denied-Reason");
        if (header != null) {
            String info = Objects.toString(header.getValue(), "").trim();
            throw new AccessControlException("Your account seems to be locked" + (info.isEmpty() ? "" : ": " + info));
        }
    }

    private static String readErrorResponse(URI uri, HttpEntity entity) {
        String error;
        try (InputStream input = entity.getContent()) {
            try (Reader reader = new InputStreamReader(maybeDecompress(input), getEncoding(entity))) {
                error = IOUtils.toString(reader, MAX_ERROR_LENGTH + 1);
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        error = StringUtils.shorten(error, MAX_ERROR_LENGTH);
        return error.trim();
    }

    private static InputStream maybeDecompress(InputStream input) throws IOException {
        // Due to a bug, Jira sometimes returns double-compressed responses. See JRA-37608
        BufferedInputStream buffered = new BufferedInputStream(input, 2);
        buffered.mark(2);
        int[] buf = new int[2];
        buf[0] = buffered.read();
        buf[1] = buffered.read();
        buffered.reset();
        int header = (buf[1] << 8) | buf[0];
        if (header == GZIPInputStream.GZIP_MAGIC) {
            return new GZIPInputStream(buffered);
        } else {
            return buffered;
        }
    }

    @Override
    public void close() {
        try {
            httpClient.close();
        } catch (IOException e) {
            LOGGER.warn("Failed to close HTTP client!", e);
        }
    }
}