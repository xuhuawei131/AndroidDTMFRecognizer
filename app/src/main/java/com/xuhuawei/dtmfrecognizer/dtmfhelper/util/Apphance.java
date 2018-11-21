package com.xuhuawei.dtmfrecognizer.dtmfhelper.util;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.util.Log;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import pl.polidea.apphance.ApphanceCallbackInterface;
import pl.polidea.apphance.ApphanceServiceInterface;
import pl.polidea.apphance.common.DebugInfo;
import pl.polidea.apphance.common.Version;


/**
 * @author xuhuawei
 * @create time 2018-11-21
 */
public class Apphance {
    public static final String ISSUE = "ISSUE";
    public static final String ASSERT = "ASSERT";
    public static final String CRASH = "CRASH";
    public static final String PROBLEM = "PROBLEM";
    public static final String LOG = "LOG";
    public static final String FATAL = "FATAL";
    public static final String ERROR = "ERROR";
    public static final String WARNING = "WARNING";
    public static final String INFO = "INFO";
    public static final String VERBOSE = "VERBOSE";
    public static final String CONDITION = "CONDITION";
    private static final String TAG = pl.polidea.apphance.Apphance.class.getSimpleName();
    private static final String APPHANCE_PACKAGE_NAME = "pl.polidea.apphance";
    private static final String APPHANCE_ACTION = "pl.polidea.apphance.action.START";
    private static final int MSG_EXIT = 1;
    private static Context appContext;
    private static ApphanceServiceConnection serviceConnection = null;

    private Apphance() {
    }

    public static boolean isAvailable() {
        return isServiceConnected();
    }

    public static void start(Context context, String appKey) {
        if(serviceConnection == null) {
            if(!(context instanceof Application)) {
                context = context.getApplicationContext();
            }

            appContext = context;
            Intent i = new Intent();
            i.setAction("pl.polidea.apphance.action.START");
            i.setPackage(context.getPackageName());//为何类库中有这个类还有自定义呢  就是因为这个，隐式启动服务5.0之后必须带包名
            if(appContext.startService(i) != null) {
                ApphanceServiceConnection conn = new ApphanceServiceConnection(context.getPackageName(), appKey);
                appContext.bindService(i, conn, 0);
                serviceConnection = conn;
            }
        }

    }

    public static Intent getExplicitIntent(Context context, Intent implicitIntent) {
        // Retrieve all services that can match the given intent
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> resolveInfo = pm.queryIntentServices(implicitIntent, 0);
        // Make sure only one match was found
        if (resolveInfo == null || resolveInfo.size() != 1) {
            return null;
        }
        // Get component info and create ComponentName
        ResolveInfo serviceInfo = resolveInfo.get(0);
        String packageName = serviceInfo.serviceInfo.packageName;
        String className = serviceInfo.serviceInfo.name;
        ComponentName component = new ComponentName(packageName, className);
        // Create a new intent. Use the old one for extras and such reuse
        Intent explicitIntent = new Intent(implicitIntent);
        // Set the component to be explicit
        explicitIntent.setComponent(component);
        return explicitIntent;
    }

    public static void log(String level, String tag, String message) {
        ArrayList stacktrace = new ArrayList();
        StackTraceElement[] st = (new Throwable()).getStackTrace();
        int len$ = st.length;

        for(int i$ = 0; i$ < len$; ++i$) {
            StackTraceElement ste = st[i$];
            stacktrace.add(ste);
        }

        while(stacktrace.size() > 0 && ((StackTraceElement)stacktrace.get(0)).getClassName().contains("pl.polidea.apphance")) {
            stacktrace.remove(0);
        }

        st = new StackTraceElement[stacktrace.size()];
        log(level, tag, message, DebugInfo.fromStacktrace((StackTraceElement[])stacktrace.toArray(st)));
    }

    public static void log(String level, String tag, String message, DebugInfo debugInfo) {
        ensureServiceConnection();

        try {
            serviceConnection.getInterface().log(serviceConnection.getPackageName(), getCurrentTimestamp(), "DEBUG", level, tag, message, debugInfo);
        } catch (RemoteException var5) {
            var5.printStackTrace();
        }

    }

    public static void problem() {
        try {
            serviceConnection.getInterface().reportProblem(serviceConnection.getPackageName(), getCurrentTimestamp(), (DebugInfo)null);
        } catch (RemoteException var1) {
            var1.printStackTrace();
        }

    }

    public static void end() {
        if(serviceConnection != null) {
            appContext.unbindService(serviceConnection);
            serviceConnection = null;
        }

    }

    private static boolean isServiceConnected() {
        return serviceConnection != null && serviceConnection.isConnected();
    }

    private static void ensureServiceConnection() {
        if(!isServiceConnected()) {
            throw new IllegalStateException("apphance service not connected; call start() first.");
        }
    }

    private static Version getApplicationVersion(Context context) {
        try {
            PackageInfo e = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return new Version(e.versionCode, e.versionName != null?e.versionName:"v" + Integer.toString(e.versionCode));
        } catch (PackageManager.NameNotFoundException var2) {
            return new Version(0, "(Unknown)");
        }
    }

    private static double getCurrentTimestamp() {
        return (double)(new Date()).getTime() / 1000.0D;
    }

    private static class ApphanceServiceConnection implements ServiceConnection {
        private final String packageName;
        private final String appKey;
        private ApphanceServiceInterface serviceInterface = null;
        private ApphanceCallbackInterface callback = new ApphanceCallbackInterface.Stub() {
            public void requestExit() {
                Apphance.ApphanceServiceConnection.this.exitHandler.sendEmptyMessage(1);
            }
        };
        private Handler exitHandler = new Handler() {
            public void handleMessage(Message msg) {
                if(msg.what == 1) {
                    try {
                        Thread.sleep(350L);
                    } catch (InterruptedException var3) {
                        ;
                    }

                    System.exit(1);
                }

            }
        };

        public String getPackageName() {
            return this.packageName;
        }

        public String getAppKey() {
            return this.appKey;
        }

        public boolean isConnected() {
            return this.serviceInterface != null;
        }

        public ApphanceServiceInterface getInterface() {
            return this.serviceInterface;
        }

        public ApphanceServiceConnection(String packageName, String appKey) {
            this.packageName = packageName;
            this.appKey = appKey;
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i(TAG, "Application " + this.getPackageName() + " connected to service.");
            this.serviceInterface = ApphanceServiceInterface.Stub.asInterface(service);

            try {
                this.serviceInterface.login(this.packageName, Process.myPid(), getCurrentTimestamp(), this.appKey, getApplicationVersion(appContext), this.callback);
                Thread.setDefaultUncaughtExceptionHandler(new ApphanceServiceConnection.ApphanceUncaughtExceptionHandler());
            } catch (RemoteException var4) {
                var4.printStackTrace();
            }

        }

        public void onServiceDisconnected(ComponentName name) {
            Log.i(TAG, "Application " + this.getPackageName() + " disconnected from service.");
            this.serviceInterface = null;
        }

        private static class ApphanceUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
            private ApphanceUncaughtExceptionHandler() {
            }

            public void uncaughtException(Thread thread, Throwable ex) {
                boolean isAssert = ex instanceof AssertionError;
                String message = isAssert?ex.getMessage():ex.getClass().getName() + " -> " + ex.getMessage();
                Log.e(TAG, "apphance intercepted uncaught exception: " + message);
                ex.printStackTrace();
                if(isServiceConnected()) {
                    Log.i(TAG, "Reporting it to apphance.");

                    try {
                        serviceConnection.getInterface().issue(serviceConnection.getPackageName(), getCurrentTimestamp(), isAssert?"ASSERT":"CRASH", message, DebugInfo.fromThrowable(ex));
                    } catch (RemoteException var6) {
                        var6.printStackTrace();
                    }

                    appContext.unbindService(serviceConnection);
                }

                System.exit(1);
            }
        }
    }
}
