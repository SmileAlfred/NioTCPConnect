package com.example.niotcpconnect;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TCPServerService extends Service {
    private static final String TAG = "测试：" + TCPServerService.class.getSimpleName();

    public final static int SERVER_PORT = 9999;         // 跟客户端绝定的端口

    private TCPServer mTCPServer;
    private ThreadPoolExecutor mConnectThreadPool;     // 总的连接线程池
    private Handler mHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        init();
    }

    IBinder mBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        public TCPServerService getServerInstance() {
            return TCPServerService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unInitTcpServer();
    }

    private void init() {
        mConnectThreadPool = new ThreadPoolExecutor(
                1,
                1,
                0,
                TimeUnit.MILLISECONDS,
                new SynchronousQueue<Runnable>(),
                new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r, "server_thread_pool");
                    }
                },
                new RejectedExecutionHandler() {
                    @Override
                    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                        Log.i(TAG, "已启动连接，请免重复操作");
                    }
                }
        );
    }

    /**
     * 初始化TCP服务
     */
    private void initTcpServer() {
        mConnectThreadPool.execute(new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void run() {
                mTCPServer = new TCPServer(TCPServerService.this, mHandler);
                mTCPServer.init();
            }
        });
    }

    public TCPServer getmTCPServer() {
        return mTCPServer;
    }

    public void setHandler(Handler handler) {
        mHandler = handler;
        if(null == mTCPServer )initTcpServer();
    }

    /**
     * 反初始化TCP服务
     */
    private void unInitTcpServer() {
        mTCPServer.close();
    }
}
