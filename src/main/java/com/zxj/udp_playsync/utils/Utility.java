package com.zxj.udp_playsync.utils;


import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.View.OnClickListener;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class Utility {

    private static Toast toast;

    /**
     * 显示提示信息
     *
     * @param text     提示文字
     * @param duration 持续时间，Toast.LENGTH_SHORT or Toast.LENGTH_LONG or user-define
     */
    public static void ShowToast(Context context, CharSequence text, int duration) {
        if (toast == null) {
            toast = Toast.makeText(context, text, duration);
        } else {
            toast.cancel();
            toast = Toast.makeText(context, text, duration);
            toast.setText(text);
            toast.setDuration(duration);
        }
        toast.show();
    }

    public static void ShowToast(Context context, CharSequence text) {
        ShowToast(context, text, 0);
    }

    /**
     * 显示提示信息
     *
     * @param id       文字id
     * @param duration 持续时间，Toast.LENGTH_SHORT or Toast.LENGTH_LONG or user-define
     */
    public static void ShowToast(Context context, int id, int duration) {
        if (context != null && context.getResources() != null)
            ShowToast(context, context.getResources().getText(id), duration);
    }


    public static void ShowToast(Context context, int id) {
        if (context != null && context.getResources() != null)
            ShowToast(context, context.getResources().getText(id), 0);
    }


    public static boolean isPicture(String filename) {
        String ext = getFileExt(filename);
        return ((";jpg;jpeg;png;bmp;gif;").contains(";" + ext + ";"));
    }

    public static boolean isVideo(String filename) {
        String ext = getFileExt(filename);
        return ((";mp4;avi;rmvb;rm;mkv;ts;mpg;vob;mov;wmv;mpeg;flv;m4v;ogg;ogv;webmv;").contains(";" + ext + ";"));
    }

    public static boolean isAudio(String filename) {
        String ext = getFileExt(filename);
        return ((";wav;mp3;").contains(";" + ext + ";"));
    }

    public static String getFileExt(String filename)    //不含.号
    {
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }




}
