package com.example.niotcpconnect.utils;


import android.content.Context;
import android.os.Looper;
import android.widget.Toast;

public class Utils {

    public static void ToastUtils(Context context, String msg) {
        if (!isMainThread()) {
            Looper.prepare();
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
            Looper.loop();
        } else Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();

    }

    /**
     * 判断当前线程是不是 主线程
     * @return
     */
    public static boolean isMainThread() {
        return Looper.getMainLooper() == Looper.myLooper();
    }
}
