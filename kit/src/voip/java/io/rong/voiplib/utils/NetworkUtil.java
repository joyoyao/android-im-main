package io.rong.voiplib.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.util.Log;
import android.widget.Toast;

public class NetworkUtil {


    public static void isConnectivityAvailable(Context context, DoEventByConnectivityState doEvent) {
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connManager.getActiveNetworkInfo();
        if (null == networkInfo) {
            Toast.makeText(context, "当前的网络连接不可用", Toast.LENGTH_SHORT).show();
        } else {
            boolean available = networkInfo.isAvailable();
            if (available) {
                State state = connManager.getNetworkInfo(
                        ConnectivityManager.TYPE_WIFI).getState();
                if (State.CONNECTED == state) {
                    Log.d("通知", "=========WIFI网络已连接================");
                    if (doEvent != null) doEvent.doCall();
                    return;
                }

                state = connManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)
                        .getState();
                if (State.CONNECTED == state) {
                    Log.i("通知", "===========GPRS网络已连接================");
                    if (doEvent != null) doEvent.doWarnCall();
                }
            } else {
                Log.i("通知", "当前的网络连接不可用");
                Toast.makeText(context, "当前的网络连接不可用", Toast.LENGTH_SHORT).show();
            }
        }
        // // 跳转到无线网络设置界面
        // startActivity(new
        // Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS));
        // // 跳转到无限wifi网络设置界面
        // startActivity(new
        // Intent(android.provider.Settings.ACTION_WIFI_SETTINGS));
    }

    public interface DoEventByConnectivityState {
        public void doCall();

        public void doWarnCall();
    }
}
