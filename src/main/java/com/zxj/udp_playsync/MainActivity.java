package com.zxj.udp_playsync;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.zxj.udp_playsync.utils.AppDebug;

import java.io.File;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    public static final String tag = MainActivity.class.getSimpleName();

    LocalBroadcastReceiver localReceiver = null;

    class LocalBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(MainService.ACT_SYNC_RELOAD_PAGE)) {
                String dir = intent.getStringExtra("dir");
                if (dir != null) {
                    goUsbPlay(dir);
                } else {
                    InitApplication.getmInstance().toastInfo("请检查是否有AutoPlayDir目录---2");
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!MainService.running) {
            Intent intent = new Intent(getApplicationContext(), MainService.class);
            startService(intent);
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(MainService.ACT_SYNC_RELOAD_PAGE);
        localReceiver = new LocalBroadcastReceiver();
        registerReceiver(localReceiver, filter);
        initView();
    }

    private void initView() {
        Button statrButton = (Button) findViewById(R.id.btn_start);
        statrButton.setOnClickListener(this);

        Button statrBC = (Button) findViewById(R.id.btn_udpda);
        statrBC.setOnClickListener(this);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();

        if (localReceiver != null) {
            unregisterReceiver(localReceiver);
            localReceiver = null;
        }
    }


    private void goUsbPlay(String path) {

        String datadir = path + "/AutoPlayDir/";
        File fpath = new File(datadir);
        AppDebug.Log(tag, fpath.getAbsolutePath());
        if (fpath.exists() && fpath.isDirectory()) {
            Intent intent = new Intent(MainActivity.this, PlayDirActivity.class);
            boolean touchenable = true;
            boolean loopplay = true;
            String lastplay = "";
            intent.putExtra("datadir", datadir);
            intent.putExtra("touchenable", touchenable);
            intent.putExtra("loopplay", loopplay);
            intent.putExtra("lastplay", lastplay);
            startActivity(intent);
        } else {
            InitApplication.getmInstance().toastInfo("请检查U盘是否有AutoPlayDir目录--1");
        }
    }


    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.btn_start: {

                Intent intent = new Intent(MainService.ACT_SENT_SYNC_MC);
                sendBroadcast(intent);
                MainService.isHost = true;

                break;
            }
            case R.id.btn_udpda: {

                Intent intent = new Intent(MainService.ACT_SENT_SYNC_BC);
                sendBroadcast(intent);
                MainService.isHost = true;

                break;
            }
            default:
                break;
        }

    }
}
