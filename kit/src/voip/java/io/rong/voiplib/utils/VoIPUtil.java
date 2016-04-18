package io.rong.voiplib.utils;

import android.content.Context;
import android.util.Log;

import org.apache.http.conn.util.InetAddressUtils;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.List;

public class VoIPUtil {

    public static String getLocalIpAddress(Context context) {
        try {
            String ipv4;

            List<NetworkInterface> nilist = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface ni : nilist) {
                if (ni.getName().toLowerCase().contains("usbnet"))
                    continue;
                List<InetAddress> ialist = Collections.list(ni.getInetAddresses());
                for (InetAddress address : ialist) {
                    if (!address.isLoopbackAddress() && InetAddressUtils.isIPv4Address(ipv4 = address.getHostAddress())) {
                        return ipv4;
                    }
                }

            }

        } catch (SocketException ex) {
            Log.d("socket_err", ex.toString());
        }
        return null;
    }

    public static int getPort() {
        DatagramSocket s = null;//为UDP编程中的Socket类,只可以判断UDP占用的端口
        // 测试两个值之间的端口号
        int MINPORT = 10000;
        int MAXPORT = 65000;

        for (; MINPORT < MAXPORT; MINPORT++) {
            try {
                // 第二个为测试本机IP,测试其它机器,则构建一个InetAddress对象
                s = new DatagramSocket(MINPORT, InetAddress.getLocalHost());
                s.close();
                return MINPORT;
            } catch (IOException e) {
//		        	e.printStackTrace();
                // 如果报错就说明报错了,继续测试上面的.
                continue;
            }
        }
        // 如果都在用就返回-1
        return -1;
    }
}
