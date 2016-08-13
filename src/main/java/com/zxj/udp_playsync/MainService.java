package com.zxj.udp_playsync;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;
import android.os.IBinder;


import com.zxj.udp_playsync.utils.AppDebug;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.Date;

public class MainService extends Service {

    public static boolean isHost = false;
    public static boolean running = false;

    public static final String tag = MainService.class.getSimpleName();
    final public static String ACT_MEDIA_UNMOUNTED = "vikan.action.ACT_MEDIA_UNMOUNTED";
    final public static String ACT_SYNC_RELOAD_PAGE = "vikan.vishow.BC.ACT_SYNC_RELOAD_PAGE";
    final public static String ACT_SENT_SYNC_MC = "vikan.vishow.action.ACT_SENT_SYNC_MC_TEST";
    final public static String ACT_SYNC_PROGRESS = "vikan.vishow.action.ACT_SYNC_PROGRESS";
    final public static String ACT_SENT_SYNC_BC = "vikan.vishow.action.ACT_SENT_SYNC_BC_TEST";


    final public static String ACT_SEND_HOST_PROGRESS = "vikan.vishow.action.ACT_SEND_HOST_PROGRESS";
    final public static String ACT_GET_HOST_PROGRESS = "vikan.vishow.action.ACT_GET_HOST_PROGRESS";

    public static final String SD_CARD = Environment.getExternalStorageDirectory().getAbsolutePath();

    static private Context appContext;

    public static Context getAppContext() {
        return appContext;
    }

    BroadcastReceiver localReceiver = null;

    private final class LocalReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            AppDebug.Log(tag, action);
            if (action.equals(MainService.ACT_SENT_SYNC_MC)) {
                SendSyncMC();
            } else if (action.equals(MainService.ACT_SENT_SYNC_BC)) {
                //TODO 广播形式
                SendSyncBC();
            }
        }
    }


    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        appContext = this.getApplicationContext();
        running = true;

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_TIME_TICK);
        intentFilter.addAction(MainService.ACT_SENT_SYNC_MC);
        intentFilter.addAction(MainService.ACT_SENT_SYNC_BC);
        localReceiver = new LocalReceiver();
        registerReceiver(localReceiver, intentFilter);
        new Thread(autosyncplayMcRunnable).start();
        new Thread(autosyncplayBcRunnable).start();
    }

    private  Runnable autosyncplayBcRunnable=new Runnable() {
        @Override
        public void run() {
            DatagramServerStart(9999);
        }
    };

    private Runnable autosyncplayMcRunnable = new Runnable() {
        @Override
        public void run() {
            MulticastServerStart(9998);

        }
    };

    public static void SendSyncBC() {
        String AutoSyncInterval = "1"; //自动同步播放
        if (AutoSyncInterval == null || AutoSyncInterval.equals(""))
            AutoSyncInterval = "10";
        int asi = Integer.valueOf(AutoSyncInterval);
        if (asi < 1)
            asi = 1;
        final int final_asi = asi;
        Calendar calender = Calendar.getInstance();
        calender.setTime(new Date(System.currentTimeMillis()));
        int curMin = calender.get(Calendar.MINUTE);
        if ((curMin % asi) == 0) {
            new Thread(new Runnable() {
                @Override
                public void run() {

                    String AutoSyncGroup = "1";   //自动同步播放广播组，默认为1
                    if (AutoSyncGroup == null || AutoSyncGroup.equals(""))
                        AutoSyncGroup = "1";
                    DatagramClientSend(9999, "11#autosyncplay#" + AutoSyncGroup + "#" + System.currentTimeMillis() + "#" + final_asi + "#" + SD_CARD);
                }
            }).start();
        }

    }

    //发送者
    private void SendSyncMC() {
        //自动同步播放广播，设置为1则为同步主机，其他从机这个值应配置为0。
        //从机可配置AutoSyncGroup为广播组，默认为1
        String AutoSyncInterval = "1"; //自动同步播放
        if (AutoSyncInterval == null || AutoSyncInterval.equals(""))
            AutoSyncInterval = "10";
        int asi = Integer.valueOf(AutoSyncInterval);
        if (asi < 1)
            asi = 1;
        final int final_asi = asi;
        Calendar calender = Calendar.getInstance();
        calender.setTime(new Date(System.currentTimeMillis()));
        int curMin = calender.get(Calendar.MINUTE);
        if ((curMin % asi) == 0) {
            new Thread(new Runnable() {
                @Override
                public void run() {
//                    String AutoSyncGroup = AppConfig.getCustomConfigItem("AutoSyncGroup","");   //自动同步播放广播组，默认为1
                    String AutoSyncGroup = "1";   //自动同步播放广播组，默认为1
                    if (AutoSyncGroup == null || AutoSyncGroup.equals(""))
                        AutoSyncGroup = "1";
                    MulticastClientSend(9998, "11#autosyncplay#" + AutoSyncGroup + "#" + System.currentTimeMillis() + "#" + final_asi + "#" + SD_CARD);
                }
            }).start();
        }
    }

    //UDP广播形式发送
    public static void DatagramClientSend(int port, String cmd) {
        String host = "255.255.255.255";//广播地址
        DatagramSocket multiSocket;

        try {
            InetAddress adds = InetAddress.getByName(host);
            AppDebug.Log(tag, "发送广播信息：" + cmd);
            multiSocket = new DatagramSocket();

            byte[] sendMSG = cmd.getBytes();
            DatagramPacket dp = new DatagramPacket(sendMSG,
                    sendMSG.length, adds, port);
            multiSocket.send(dp);
            multiSocket.close();
        } catch (UnknownHostException e) {
            AppDebug.Log(tag, "发送广播信息...UnknownHostException"+e.getMessage());
            e.printStackTrace();
        } catch (SocketException e) {
            AppDebug.Log(tag, "发送广播信息...SocketException"+e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            AppDebug.Log(tag, "发送广播信息...IOException"+e.getMessage());
            e.printStackTrace();
        }
    }

    public static boolean MulticastClientSend(int port, String cmd) {
        String destAddressStr = "224.1.2.3";
        int destPort = port;
        int TTL = 4;
        boolean ret = false;

        MulticastSocket multiSocket;
        try {
            InetAddress destAddress = InetAddress.getByName(destAddressStr);

            if (!destAddress.isMulticastAddress()) {// 检测该地址是否是多播地址
                return false;
            }
            AppDebug.Log(tag, "发送组播信息：" + cmd);

            multiSocket = new MulticastSocket();

            multiSocket.setTimeToLive(TTL);


            byte[] sendMSG = cmd.getBytes();

            DatagramPacket dp = new DatagramPacket(sendMSG, sendMSG.length, destAddress, destPort);

            multiSocket.send(dp);

            multiSocket.close();
            ret = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ret;
    }

    //UDP广播形式
    public void DatagramServerStart(int localPort) {
        int RECEIVE_LENGTH = 1024;
        try {
            DatagramSocket receiveDatagram = new DatagramSocket(localPort);
            DatagramPacket dp = new DatagramPacket(new byte[RECEIVE_LENGTH], RECEIVE_LENGTH);
            while (running) {
                receiveDatagram.receive(dp);
                String data = (new String(dp.getData())).substring(0, dp.getLength());
                String AutoSyncGroup = "1";   //自动同步播放广播组，默认为1
                if (AutoSyncGroup == null || AutoSyncGroup.equals(""))
                    AutoSyncGroup = "1";
                AppDebug.Log(tag, "收到广播信息[" + dp.getLength() + "]：" + data);
                doDealData(data, AutoSyncGroup,"DatagramServerStart");
            }
            receiveDatagram.close();
        } catch (SocketException e) {
            AppDebug.Log(tag, "收到广播信息...SocketException"+e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            AppDebug.Log(tag, "收到广播信息..IOException"+e.getMessage());
            e.printStackTrace();
        }
    }

    private void doDealData(String data, String AutoSyncGroup,String type) {
        if (data.startsWith("11#autosyncplay#" + AutoSyncGroup + "#")) {
            String[] darr = data.split("#");
            String asi = "";
            if (darr.length >= 5)
                asi = darr[4];
            String dir = "";
            if (darr.length >= 6)
                dir = darr[5];
            AppDebug.Log(tag, type+"...AutoSyncGroup=" + AutoSyncGroup + ",asi=" + asi);
            Intent intent = new Intent();
            intent.setAction(MainService.ACT_SYNC_RELOAD_PAGE);
            intent.putExtra("AutoSyncInterval", asi);
            intent.putExtra("dir", dir);

            MainService.getAppContext().sendBroadcast(intent);

        } else if (data.startsWith("22#autosyncplay#" + AutoSyncGroup + "#")) {

            String[] darr = data.split("#");
            int fileindex = 0;

            if (darr.length >= 4)
                fileindex = Integer.parseInt(darr[3]);
            int position = 0;
            if (darr.length >= 5)
                position = Integer.parseInt(darr[4]);

            AppDebug.Log(tag, type+"...fileindex=" + fileindex + ",position=" + position);

            Intent intent = new Intent();
            intent.setAction(MainService.ACT_SYNC_PROGRESS);
            intent.putExtra("fileindex", fileindex);
            intent.putExtra("position", position);

            MainService.getAppContext().sendBroadcast(intent);
        }
    }

    public void MulticastServerStart(int localPort) {
        String multicastHost = "224.1.2.3";
        int RECEIVE_LENGTH = 1024;
        InetAddress receiveAddress;
        try {
            receiveAddress = InetAddress.getByName(multicastHost);
            int port = localPort;
            MulticastSocket receiveMulticast = new MulticastSocket(port);
            receiveMulticast.joinGroup(receiveAddress);
            DatagramPacket dp = new DatagramPacket(new byte[RECEIVE_LENGTH], RECEIVE_LENGTH);
            while (running) {
                receiveMulticast.receive(dp);
                String data = (new String(dp.getData())).substring(0, dp.getLength());
                String AutoSyncGroup = "1";   //自动同步播放广播组，默认为1

                if (AutoSyncGroup == null || AutoSyncGroup.equals(""))
                    AutoSyncGroup = "1";
                AppDebug.Log(tag, "收到组播信息[" + dp.getLength() + "]：" + data);
                doDealData(data, AutoSyncGroup,"MulticastServerStart");
            }
            receiveMulticast.close();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (localReceiver != null) {
            unregisterReceiver(localReceiver);
            localReceiver = null;
        }
    }

}
