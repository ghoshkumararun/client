package org.msf.records.diagnostics;

import android.app.Application;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;

import org.apache.http.HttpResponse;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.msf.records.model.Concepts;
import org.msf.records.net.OpenMrsConnectionDetails;
import org.msf.records.utils.Logger;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A {@link org.msf.records.diagnostics.HealthCheck} that checks whether the
 * Buendia API server is up and responding to HTTP requests at the URL in the
 * "OpenMRS root URL" preference setting.
 */
public class BuendiaApiHealthCheck extends HealthCheck {

    private static final Logger LOG = Logger.create();

    private static final int CHECK_FREQUENCY_MS = 20000;

    // Retrieving a concept should be quick and ensures that the module is both running and has
    // database access.
    private static final String HEALTH_CHECK_ENDPOINT =
            "/concept/" + Concepts.GENERAL_CONDITION_UUID;

    private final Object mLock = new Object();

    private final OpenMrsConnectionDetails mConnectionDetails;

    private HandlerThread mHandlerThread;
    private Handler mHandler;
    private BuendiaModuleHealthCheckRunnable mRunnable;

    BuendiaApiHealthCheck(
            Application application,
            OpenMrsConnectionDetails connectionDetails) {
        super(application);

        mConnectionDetails = connectionDetails;
    }

    @Override
    protected void startImpl() {
        synchronized (mLock) {
            if (mHandlerThread == null) {
                mHandlerThread = new HandlerThread("Buendia API Health Check");
                mHandlerThread.start();
                mHandler = new Handler(mHandlerThread.getLooper());
            }

            if (mRunnable == null) {
                mRunnable = new BuendiaModuleHealthCheckRunnable(mHandler);
            }

            if (!mRunnable.isRunning.getAndSet(true)) {
                mHandler.post(mRunnable);
            }
        }
    }

    @Override
    protected void stopImpl() {
        synchronized (mLock) {
            if (mRunnable != null) {
                mRunnable.isRunning.set(false);
                mRunnable = null;
            }

            if (mHandlerThread != null) {
                mHandlerThread.quit();
                mHandlerThread = null;
            }

            mHandler = null;
        }
    }

    private class BuendiaModuleHealthCheckRunnable implements Runnable {
        public final AtomicBoolean isRunning;

        private final Handler mHandler;

        public BuendiaModuleHealthCheckRunnable(Handler handler) {
            isRunning = new AtomicBoolean(false);
            mHandler = handler;
        }

        @Override
        public void run() {
            if (!isRunning.get()) {
                return;
            }

            try {
                String uriString = mConnectionDetails.getBuendiaApiUrl() + HEALTH_CHECK_ENDPOINT;
                Uri uri = Uri.parse(uriString);
                if (uri.getHost() == null) {
                    LOG.w("The configured OpenMRS API URL '%1$s' is invalid.", uriString);
                    reportIssue(HealthIssue.SERVER_CONFIGURATION_INVALID);
                    return;
                }

                try {
                    HttpClient httpClient = new DefaultHttpClient();
                    HttpGet httpGet = new HttpGet(uri.toString());
                    httpGet.addHeader(BasicScheme.authenticate(
                            new UsernamePasswordCredentials(
                                    mConnectionDetails.getUserName(),
                                    mConnectionDetails.getPassword()),
                            "UTF-8", false));

                    HttpResponse httpResponse = httpClient.execute(httpGet);
                    if (httpResponse.getStatusLine().getStatusCode() != HttpURLConnection.HTTP_OK) {
                        LOG.w(
                                "The OpenMRS URL '%1$s' returned unexpected error code: %2$s",
                                uriString,
                                httpResponse.getStatusLine().getStatusCode());
                        switch (httpResponse.getStatusLine().getStatusCode()) {
                            case HttpURLConnection.HTTP_INTERNAL_ERROR:
                                reportIssue(HealthIssue.SERVER_INTERNAL_ISSUE);
                                break;
                            case HttpURLConnection.HTTP_FORBIDDEN:
                            case HttpURLConnection.HTTP_UNAUTHORIZED:
                                reportIssue(HealthIssue.SERVER_AUTHENTICATION_ISSUE);
                                break;
                            case HttpURLConnection.HTTP_NOT_FOUND:
                            default:
                                reportIssue(HealthIssue.SERVER_NOT_RESPONDING);
                                break;
                        }
                        return;
                    }
                } catch (UnknownHostException e) {
                    reportIssue(HealthIssue.SERVER_HOST_UNREACHABLE);
                } catch (IOException e) {
                    LOG.w(
                            "Could not perform OpenMRS health check using URL '%1$s'.",
                            uriString);
                }

                resolveAllIssues();
            } finally {
                mHandler.postDelayed(this, CHECK_FREQUENCY_MS);
            }
        }
    }
}