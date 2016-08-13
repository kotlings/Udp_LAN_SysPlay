package com.zxj.udp_playsync;


import android.app.Application;
import android.widget.Toast;

public class InitApplication extends Application {

    static InitApplication mInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
    }

    // 全局的Context
    public static InitApplication getmInstance() {
        synchronized (InitApplication.class) {
            if (mInstance == null) {
                mInstance = new InitApplication();
            }
        }
        return mInstance;
    }

    // 全局的shortToast
    public void toastInfo(String str) {

        Toast.makeText(mInstance, str, Toast.LENGTH_SHORT).show();

    }

    // 全局的LongToast
    public void toastLInfo(String str) {

        Toast.makeText(mInstance, str, Toast.LENGTH_LONG).show();

    }

}
