package com.zxj.udp_playsync.utils;

import java.lang.reflect.Method;
import java.util.ArrayList;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Point;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

public class MonitorInfo {
	private int Width;
	private int Height;
	private int Depth;
	
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
	public MonitorInfo(Context context)
	{

		//Point pos = new Point();
		int w;
		int h;
		
		WindowManager wm = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();
		DisplayMetrics dm = new DisplayMetrics(); 
		display.getRealMetrics(dm);
		
//		AppDebug.Log(MonitorInfo.class, "dm.width="+dm.widthPixels+",dm.height="+dm.heightPixels);
		w = dm.widthPixels;
		int ver = Build.VERSION.SDK_INT;
		if(ver == 13)
		{
			try {   
				Method mt = display.getClass().getMethod("getRealHeight");   
				h = (Integer) mt.invoke(display);   
			} catch (Exception e) { 
//				AppDebug.printStackTrace(e);
				h = dm.heightPixels;
			}  		
		}else if(ver > 13)
		{
			try {   
				Method mt = display.getClass().getMethod("getRawHeight");   
				h = (Integer) mt.invoke(display);   
			} catch (Exception e) { 
//				AppDebug.printStackTrace(e);
				h = dm.heightPixels;
			}  		
		}else
		{
			h = dm.heightPixels;
		}
		
		Width = w;
		Height = h;
		Depth = 32;
//		AppDebug.Log(MonitorInfo.class, "MonitorInfo...Width="+Width+",Height="+Height+",Depth="+Depth);
	}
//	static void setTo(ArrayList<MonitorInfo> arr)
//	{
//		arr.clear();
//		arr.add(new MonitorInfo());
//	}
	
	public int getWidth() {
		return Width;
	}
	public void setWidth(int width) {
		Width = width;
	}
	public int getHeight() {
		return Height;
	}
	public void setHeight(int height) {
		Height = height;
	}
	public int getDepth() {
		return Depth;
	}
	public void setDepth(int depth) {
		Depth = depth;
	}

}
