package com.zxj.udp_playsync.utils;

import android.util.Log;

import com.zxj.udp_playsync.BuildConfig;

public class AppDebug {
	@SuppressWarnings("rawtypes")
	public static void Log(Class cls, String msg)
	{
		String tag = "";
		if(cls != null)
			tag = cls.getSimpleName();
		Log(tag, msg);
	}
	public static void Log(String tag, String msg)
	{
		//发布版本必须关闭打印提调试
		if(BuildConfig.DEBUG)
		{
			Log.i(tag, msg);
		}
	}
	public static void printStackTrace(Exception e)
	{
		//发布版本必须关闭打印提调试
		if(e != null) e.printStackTrace();
	}
	public static void printError(Error e)
	{
		//发布版本必须关闭打印提调试
		if(e != null) e.printStackTrace();
	}
}
