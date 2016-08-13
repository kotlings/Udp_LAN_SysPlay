package com.zxj.udp_playsync;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnSeekCompleteListener;
import android.media.MediaPlayer.OnTimedTextListener;
import android.media.MediaPlayer.OnVideoSizeChangedListener;
import android.media.TimedText;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.zxj.udp_playsync.utils.AppDebug;
import com.zxj.udp_playsync.utils.MonitorInfo;
import com.zxj.udp_playsync.utils.Utility;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class PlayDirActivity extends Activity
        implements SurfaceHolder.Callback, OnCompletionListener, OnErrorListener, OnInfoListener, OnPreparedListener,
        OnSeekCompleteListener, OnVideoSizeChangedListener, OnClickListener, OnTimedTextListener {

    public static final String tag = PlayDirActivity.class.getSimpleName();
    private String datadir = "";
    private String lastplay = "";
    private boolean touchenable = true;
    private boolean loopplay = false;
    private int position = 0;
    private int duration = 0;
    private int play_position = 0;
    private String video_filepath = "";
    private int fileindex;

    private SurfaceHolder holder;
    private MediaPlayer player;

    private List<Map<String, Object>> itemList;

    private ImageView imageView;
    private SurfaceView surfaceView;

    private View layoutAction;
    private SeekBar seekBar;
    private TextView textViewTime;
    private Button buttonPrev;
    private Button buttonPlay;
    private Button buttonNext;

    private boolean pause = false;
    private int currdelay = 8;

    final static private int playnext = 100;
    final static private int PLAYER_SERVER_DIED = 101;
    final static private int HideActionBar = 102;
    final static private int UpdateSeekBar = 103;

    Timer timer;
    int playType = 0;

    final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case playnext:
                    position++;
                    play_position = 0;
//				buttonPrev.setEnabled(position > 0);
//				buttonNext.setEnabled(position < itemList.size()-1);
                    if (!loopplay && position >= itemList.size())
                        finish();
                    else {
                        if (MainService.isHost) {
                            SendHostProGress(position, 0);
                        }
                        PlayData();
                    }
                    break;
                case PLAYER_SERVER_DIED:
                    handler.removeMessages(PLAYER_SERVER_DIED);
//				AppDebug.Log(WebViewActivity.class, "MediaPlayer...PLAYER_SERVER_DIED...vpath="+video_filepath);
                    if (!("").equals(video_filepath)) {
                        if (player == null)
                            initPlayer();
                        prepareVideo(video_filepath);
                    }
                    break;
                case HideActionBar:
                    layoutAction.setVisibility(View.GONE);
                    handler.removeMessages(UpdateSeekBar);
                    break;
                case UpdateSeekBar:
                    handler.removeMessages(UpdateSeekBar);
         /**/
                    updateSeekUI(false);
                    handler.sendEmptyMessageDelayed(UpdateSeekBar, 500);
                    break;
                default:
                    break;
            }
        }
    };


    BroadcastReceiver localReceiver = null;
    private final class PlayDirBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(MainService.ACT_MEDIA_UNMOUNTED)) {
                Utility.ShowToast(context, "--弹出U盘--");
                handler.postDelayed(new Runnable() {

                    @Override
                    public void run() {

                        File fpath = new File(datadir);
                        if (!fpath.exists() || fpath.isDirectory()) {
                            System.exit(0);
                            Intent i = InitApplication.getmInstance().getPackageManager()
                                    .getLaunchIntentForPackage(InitApplication.getmInstance().getPackageName());
                            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            InitApplication.getmInstance().startActivity(i);
                        }
                    }
                }, 1000);
            } else if (action.equals(MainService.ACT_SYNC_PROGRESS)) {

                fileindex = intent.getIntExtra("fileindex", 0);
                position = fileindex;
                play_position = intent.getIntExtra("position", 0);
//                InitApplication.getmInstance().toastInfo("-同步主机进度..." + fileindex + "," + play_position);
                PlayData();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
//		AppDebug.Log(PlayDirActivity.class, "[lifecycle]onCreate...");
        super.onCreate(savedInstanceState);
        //////////////隐藏导航栏
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        if (getActionBar() != null)
            getActionBar().hide();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(0x00000008);
        //////////////<<<

        if (timer == null)
            timer = new Timer();

        setContentView(R.layout.activity_playdir);

        imageView = (ImageView) findViewById(R.id.imageView);
        surfaceView = (SurfaceView) findViewById(R.id.surfaceView);

        MonitorInfo mi = new MonitorInfo(PlayDirActivity.this);
        int ww = mi.getWidth();
        int wh = mi.getHeight();
        RelativeLayout.LayoutParams params =
                new RelativeLayout.LayoutParams(ww, wh);

        params.leftMargin = 0;
        params.topMargin = 0;

        surfaceView.setLayoutParams(params);
        surfaceView.getHolder().setFixedSize(ww, wh);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(MainService.ACT_MEDIA_UNMOUNTED);
        intentFilter.addAction(MainService.ACT_SYNC_PROGRESS);
        intentFilter.addAction(Intent.ACTION_TIME_TICK);


        localReceiver = new PlayDirBroadcastReceiver();
        registerReceiver(localReceiver, intentFilter);

        layoutAction = (View) findViewById(R.id.layoutAction);
        layoutAction.setVisibility(View.GONE);
        buttonPrev = (Button) findViewById(R.id.buttonPrev);
        buttonPrev.setOnClickListener(this);
        buttonPlay = (Button) findViewById(R.id.buttonPlay);
        buttonPlay.setOnClickListener(this);
        buttonNext = (Button) findViewById(R.id.buttonNext);
        buttonNext.setOnClickListener(this);
        textViewTime = (TextView) findViewById(R.id.textViewTime);
        seekBar = (SeekBar) findViewById(R.id.seekBar);
        seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {

                if (fromUser)
                    player.seekTo(progress);
                updateSeekUI(true);

                AppDebug.Log("1111", "onProgressChanged.....progress:" + progress + ",getCurrentPosition:" + player.getCurrentPosition());
            }
        });

        position = 0;
        Intent intent = this.getIntent();
        if (intent != null) {
            datadir = intent.getStringExtra("datadir");
            touchenable = intent.getBooleanExtra("touchenable", true);
            loopplay = intent.getBooleanExtra("loopplay", false);
            lastplay = intent.getStringExtra("lastplay");
            if (lastplay != null && !lastplay.isEmpty()) {
//				AppDebug.Log(PlayDirActivity.class, "lastplay="+lastplay);
                String list[] = lastplay.split(",");
                if (list.length > 1)
                    position = Integer.valueOf(list[1]);
                if (list.length > 2) {
                    duration = Integer.valueOf(list[2]);
                    play_position = duration;
                }
            }
        }

        itemList = new ArrayList<>();
        initDirData();
    }

    //发送主机进度
    long lastsynctime = 0;

    public void SendHostProGress(final int index, final int position) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                int AutoSyncGroup = 1;
                String cmd = "22#autosyncplay#" + AutoSyncGroup + "#" + index + "#" + position;
                MainService.MulticastClientSend(9998, cmd);
                lastsynctime = System.currentTimeMillis();

                AppDebug.Log("ctr", "MulticastClientSend..." + cmd);
            }
        }).start();
    }


    @Override
    public void onDestroy() {
//		AppDebug.Log(PlayDirActivity.class, "[lifecycle]onDestroy...");
        releasePlayer();

        if (localReceiver != null) {
            unregisterReceiver(localReceiver);
            localReceiver = null;
        }
        super.onDestroy();
    }

    @Override
    protected void onStop() {
		AppDebug.Log(PlayDirActivity.class, "[lifecycle]onStop...");
        super.onStop();
    }

    @Override
    protected void onPause() {
		AppDebug.Log(PlayDirActivity.class, "[lifecycle]onPause...");
        if (player != null) {
            play_position = player.getCurrentPosition();
            //player.pause();
            releasePlayer();
        } else
            play_position = 0;
        handler.removeMessages(playnext);

        if (!datadir.isEmpty()) {
            SimpleDateFormat dtfmt = new SimpleDateFormat("dd/MM HH:mm:ss");
            String timestr = dtfmt.format(new Date());
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
		AppDebug.Log(PlayDirActivity.class, "[lifecycle]onResume...");
        super.onResume();

        PlayData();

        if (MainService.isHost) {

            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (!("").equals(video_filepath) && lastsynctime + 10 * 1000 < System.currentTimeMillis()) {
                        int pp = 0;
                        if (player != null)
                            pp = player.getCurrentPosition();
                        SendHostProGress(position, pp);
                    }
                }
            }, 0, 10 * 1000);
        }
    }

    private void PlayData() {
        if (itemList.size() > 0) {
            position %= itemList.size();
            AppDebug.Log(tag, "---------position:" + position);
            buttonPrev.setEnabled(position > 0);
            buttonNext.setEnabled(position < itemList.size() - 1);
//			AppDebug.Log(PlayDirActivity.class, "PlayData...position:"+position);
            Map<String, Object> obj = itemList.get(position);
            if (obj != null) {
                buttonPlay.setText(getString(R.string.pause));
                Object od = obj.get("delay");
//              currdelay = (od == null) ? 0 : (Integer) od;
                currdelay = (od == null) ? 0 : Integer.parseInt(od.toString());
//                L.i(tag, "---------currdelay:" + currdelay);

                String filename = (String) obj.get("filename");
//                L.i(tag, "---------filename:" + filename);
                String filepath = datadir + "/" + filename;
                handler.removeMessages(playnext);
                if (Utility.isPicture(filename)) {

                    //TODO 在此处设置播放时间
                    video_filepath = "";
                    imageView.setImageURI(Uri.fromFile(new File(filepath)));
                    imageView.setVisibility(View.VISIBLE);
                    surfaceView.setVisibility(View.GONE);
                    handler.sendEmptyMessageDelayed(playnext, (currdelay <= 0) ? 8000 : currdelay * 1000);
                    if (player != null)
                        releasePlayer();
                    seekBar.setVisibility(View.GONE);
                    textViewTime.setVisibility(View.GONE);
                } else if (Utility.isVideo(filename)) {
                    if (player == null)
                        initPlayer();

                    surfaceView.setVisibility(View.VISIBLE);
                    imageView.setVisibility(View.GONE);

                    if (!filepath.equals(video_filepath)) {
                        video_filepath = filepath;
//					AppDebug.Log(PlayDirActivity.class, "MediaPlayer...player="+player);
                        prepareVideo(video_filepath);
                    } else {
                        player.seekTo(play_position);
                    }
                    seekBar.setVisibility(View.VISIBLE);
                    textViewTime.setVisibility(View.VISIBLE);

                    //如果为0则全部播完
                    if (currdelay > 0) {
                        handler.sendEmptyMessageDelayed(playnext, currdelay * 1000);
                    }
                } else {
                    video_filepath = "";
//					imageView.setVisibility(View.GONE);
//					surfaceView.setVisibility(View.GONE);
                    Utility.ShowToast(this, getString(R.string.notsupport) + " [" + filename + "]");
                    handler.sendEmptyMessage(playnext);
                }
            }
        }
    }

    private void prepareVideo(String filepath) {
        if (player != null) {
            try {
//				AppDebug.Log(PlayDirActivity.class, "MediaPlayer.setDataSource...vpath="+filepath);
                //play_position = 0;
                player.reset();
                player.setDataSource(filepath);
                player.prepare();
            } catch (IllegalArgumentException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                //releasePlayer();
            } catch (IllegalStateException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                //releasePlayer();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                //releasePlayer();
            }
        }
    }

    private void initDirData() {
        itemList.clear();
        if (datadir == null || datadir.isEmpty())
            return;

        File dir = new File(datadir);
        if (!dir.exists()) {
//    		AppDebug.Log(DataPlayActivity.class, "initDirData...!dir.exists:");
            return;
        }
        dirPlay(dir);
    }

    //目录播放
    private void dirPlay(File dir) {
        playType = 0;
        File[] list = dir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isFile();
            }
        });

        List<String> dlist = new ArrayList<String>();
        for (File f : list) {
            if (f.isFile() && (Utility.isPicture(f.getName()) || Utility.isVideo(f.getName()) || Utility.isAudio(f.getName())))
                dlist.add(f.getName());
        }
        Collections.sort(dlist, String.CASE_INSENSITIVE_ORDER);
        for (String dd : dlist) {
            Map<String, Object> mapo = new HashMap<String, Object>();
            mapo.put("filename", dd);
            itemList.add(mapo);
        }

    }

    protected void initPlayer() {
//		AppDebug.Log(PlayDirActivity.class, "MediaPlayer.initPlayer...player="+player);
        if (player == null) {
//			AppDebug.Log(PlayDirActivity.class, "MediaPlayer.new MediaPlayer()...");
            //下面开始实例化MediaPlayer对象
            player = new MediaPlayer();
            player.setOnCompletionListener(this);
            player.setOnErrorListener(this);
            player.setOnInfoListener(this);
            player.setOnPreparedListener(this);
            player.setOnSeekCompleteListener(this);
            player.setOnVideoSizeChangedListener(this);
            player.setOnTimedTextListener(this);
        }
//		AppDebug.Log(PlayDirActivity.class, "MediaPlayer.surfaceView...");
        if (holder == null) {
            holder = surfaceView.getHolder();
            holder.addCallback(this);
        }

//		AppDebug.Log(tag, "MediaPlayer.surfaceView.setVisibility...surfaceView="+surfaceView);
        if (surfaceView != null) {
            try {
                surfaceView.setVisibility(View.VISIBLE);
            } catch (Exception e) {
//				AppDebug.printStackTrace(e);
            }
        }
//		AppDebug.Log(tag, "MediaPlayer.surfaceView.setVisibility...end");
    }

    protected void releasePlayer() {
//		AppDebug.Log(PlayDirActivity.class, "MediaPlayer.releasePlayer...surfaceView="+surfaceView);
        //play_position = 0;
        if (player != null) {
//	        if (player.isPlaying()) {
//			try{
//	        	player.stop();
//			} catch (IllegalStateException e) {
//			}
//	        }
            player.reset(); // Might not be necessary, since release() is called right after, but it doesn't seem to hurt/cause issues
            player.release();
            player = null;
            //System.gc();
        }
        if (surfaceView != null)
            surfaceView.setVisibility(View.GONE);
    }

    //////////////////////////////////////////////
    @Override
    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {
        // TODO Auto-generated method stub
        //updateSeekUI();
    }

    @Override
    public void onPrepared(MediaPlayer p) {
        if (play_position > 0)
            player.seekTo(play_position);

        MonitorInfo mi = new MonitorInfo(PlayDirActivity.this);
        int ww = mi.getWidth();
        int wh = mi.getHeight();
        int vw = player.getVideoWidth();
        int vh = player.getVideoHeight();
        int rw = ww;
        int rh = wh;
        if (vh > 0) {
            rw = (vw * wh) / vh;
            rh = wh;
            if (rw > ww) {
                rw = ww;
                rh = (vh * ww) / vw;
            }
        }

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(rw, rh);

        params.leftMargin = (ww - rw) / 2;
        params.topMargin = (wh - rh) / 2;

        surfaceView.setLayoutParams(params);
        surfaceView.getHolder().setFixedSize(rw, rh);

        seekBar.setMax(player.getDuration());
        updateSeekUI(false);

        p.start();
    }

    private void updateSeekUI(boolean onlyText) {
        if (player == null)
            return;
        int dd = player.getDuration() - player.getCurrentPosition();
        if (dd < 0) dd = 0;
        int h = dd / 1000 / 60 / 60;
        int m = (dd - h * 60 * 60 * 1000) / 1000 / 60;
        int s = (dd - h * 60 * 60 * 1000 - m * 1000 * 60) / 1000;
        textViewTime.setText(String.format("%02d:%02d:%02d", h, m, s));
        if (!onlyText)
            seekBar.setProgress(player.getCurrentPosition());
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
//		AppDebug.Log(PlayDirActivity.class, "MediaPlayer.onError...what="+what+",extra="+extra);
        if (MediaPlayer.MEDIA_ERROR_SERVER_DIED == what) {
            play_position = player.getCurrentPosition();
//			try{
//	        	player.stop();
//			} catch (IllegalStateException e) {
//			}
            //player.reset();
            player.release();
            player = null;
            handler.sendEmptyMessageDelayed(PLAYER_SERVER_DIED, 2000);
            //handler.sendEmptyMessageDelayed(RELOAD_DEFAULT_PAGE, 10000);
        } else if (MediaPlayer.MEDIA_ERROR_UNSUPPORTED == what) {
            play_position = 0;
            handler.sendEmptyMessage(playnext);
        } else if (MediaPlayer.MEDIA_ERROR_TIMED_OUT != what) {
            //releasePlayer();
        }
        return false;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        // TODO Auto-generated method stub
//		AppDebug.Log(PlayDirActivity.class, "MediaPlayer.onCompletion..");
        video_filepath = "";
        play_position = 0;
        handler.sendEmptyMessage(playnext);
        //releasePlayer();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
        // TODO Auto-generated method stub

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
//		AppDebug.Log(PlayDirActivity.class, "MediaPlayer.surfaceCreated...");
        // TODO Auto-generated method stub
        if (player != null) {
            try {
//				player.prepare();
                player.setDisplay(holder);
                if (Build.VERSION.SDK_INT >= 16) {
                    player.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT);
                    //player.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
                }
            } catch (RuntimeException e) {

//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
            }
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.buttonPrev:
                position--;
                if (position < 0) position = 0;
                play_position = 0;
//			buttonPrev.setEnabled(position > 0);
//			buttonNext.setEnabled(position < itemList.size()-1);
                //if (MainService.isHost)
            {
                SendHostProGress(position, 0);
            }
            PlayData();
            break;
            case R.id.buttonPlay:
                pause = !pause;
                if (pause) {
                    if (player != null)
                        player.pause();
                    else
                        handler.removeMessages(playnext);
                    buttonPlay.setText(getString(R.string.play));
                } else {
                    if (player != null)
                        player.start();
                    else
                        handler.sendEmptyMessageDelayed(playnext, (currdelay <= 0) ? 8000 : currdelay * 1000);
                    buttonPlay.setText(getString(R.string.pause));
                }
                break;
            case R.id.buttonNext:
                position++;
                if (position >= itemList.size()) position = itemList.size() - 1;
                play_position = 0;
//			buttonPrev.setEnabled(position > 0);
//			buttonNext.setEnabled(position < itemList.size()-1);
                //if (MainService.isHost)
            {
                SendHostProGress(position, 0);
            }
            PlayData();
            break;
        }

    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            if (touchenable) {
                if (layoutAction.getVisibility() != View.VISIBLE) {
                    layoutAction.setVisibility(View.VISIBLE);
                    updateSeekUI(false);
                    handler.sendEmptyMessageDelayed(UpdateSeekBar, 500);
                }
                handler.removeMessages(HideActionBar);
                handler.sendEmptyMessageDelayed(HideActionBar, 5 * 1000);
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public void onTimedText(MediaPlayer arg0, TimedText arg1) {
        // TODO Auto-generated method stub

    }
}
