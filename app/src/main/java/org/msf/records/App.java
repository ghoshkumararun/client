package org.msf.records;

import android.app.Application;

import net.sqlcipher.database.SQLiteDatabase;

import org.msf.records.diagnostics.HealthMonitor;
import org.msf.records.net.OpenMrsConnectionDetails;
import org.msf.records.net.Server;
import org.msf.records.user.UserManager;
import org.msf.records.utils.ActivityHierarchyServer;
import org.odk.collect.android.application.Collect;

import javax.inject.Inject;

import dagger.ObjectGraph;

/**
 * An {@link Application} the represents the Android Client.
 */
public class App extends Application {

    private ObjectGraph mObjectGraph;

    /**
     * The current instance of the application.
     */
    private static App sInstance;

    private static UserManager sUserManager;

    private static Server sServer;

    private static HealthMonitor sHealthMonitor;

    private static OpenMrsConnectionDetails sConnectionDetails;

    @Inject ActivityHierarchyServer mActivityHierarchyServer;
    @Inject UserManager mUserManager;
    @Inject OpenMrsConnectionDetails mOpenMrsConnectionDetails;
    @Inject Server mServer;
    @Inject HealthMonitor mHealthMonitor;

    @Override
    public void onCreate() {
        Collect.onCreate(this);
        super.onCreate();

        initializeSqlCipher();

        buildObjectGraphAndInject();

        registerActivityLifecycleCallbacks(mActivityHierarchyServer);

        synchronized (App.class) {
            sInstance = this;
            sUserManager = mUserManager; // TODO(dxchen): Remove when Daggered.
            sConnectionDetails = mOpenMrsConnectionDetails; // TODO(dxchen): Remove when Daggered.
            sServer = mServer; // TODO(dxchen): Remove when Daggered.
            sHealthMonitor = mHealthMonitor;
        }

        mHealthMonitor.start();
    }

    private void initializeSqlCipher() {
        SQLiteDatabase.loadLibs(this);
    }

    public void buildObjectGraphAndInject() {
        mObjectGraph = ObjectGraph.create(Modules.list(this));
        mObjectGraph.inject(this);
    }

    public void inject(Object obj) {
        mObjectGraph.inject(obj);
    }

    public static synchronized App getInstance() {
        return sInstance;
    }

    public static synchronized UserManager getUserManager() {
        return sUserManager;
    }

    public static synchronized Server getServer() {
        return sServer;
    }

    public static synchronized HealthMonitor getHealthMonitor() {
        return sHealthMonitor;
    }

    public static synchronized OpenMrsConnectionDetails getConnectionDetails() {
        return sConnectionDetails;
    }
}
