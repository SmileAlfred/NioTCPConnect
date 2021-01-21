package com.example.niotcpconnect;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TCPClient {
    private static final String TAG = "测试：" + TCPClient.class.getSimpleName();

    private String mSendMsg;
    private String mClientName;     // 客户端命名
    private Selector mSelector;
    private SocketChannel mSocketChannel;

    private ThreadPoolExecutor mConnectThreadPool;  // 消息连接和接收的线程池
    private Context mContext;
    private Handler mHandler;


    public TCPClient(Context context, Handler handler, String clientName) {
        this.mContext = context;
        this.mHandler = handler;

        init(clientName);
    }

    /**
     * 基本初始化
     *
     * @param clientName
     */
    private void init(String clientName) {
        mClientName = clientName;
        mConnectThreadPool = new ThreadPoolExecutor(
                1,
                1,
                0,
                TimeUnit.MILLISECONDS,
                new SynchronousQueue<Runnable>(),
                new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r, "client_connection_thread_pool");
                    }
                },
                new RejectedExecutionHandler() {
                    @Override
                    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                        Log.i(TAG, mClientName + " 已启动连接，请免重复操作");

                        //更新UI：服务器界面显示【客户端返回的消息：】
                        Message mMessage= new Message();
                        mMessage.obj = mClientName + " 已启动连接，请免重复操作";
                        mMessage.what = MainActivity.LOG;
                        mHandler.sendMessage(mMessage);
                    }
                }
        );
    }

    /**
     * 请求连接服务端
     */
    public void requestConnectTcp(final String ipAdress) {
        mConnectThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                initSocketAndReceiveMsgLoop(ipAdress);
            }
        });
    }

    /**
     *
     */
    private void initSocketAndReceiveMsgLoop(String ipAdress) {
        try {
            mSocketChannel = SocketChannel.open();
            // 设置为非阻塞方式
            mSocketChannel.configureBlocking(false);
            // 连接服务端地址和端口
            mSocketChannel.connect(new InetSocketAddress(ipAdress, 9999));

            // 注册到Selector，请求连接
            mSelector = Selector.open();
            mSocketChannel.register(mSelector, SelectionKey.OP_CONNECT);
            while (mSelector != null && mSelector.isOpen() && mSocketChannel != null && mSocketChannel.isOpen()) {
                // 选择一组对应Channel已准备好进行I/O的Key
                int select = mSelector.select();     // 当没有消息时，这里也是会阻塞的
                if (select <= 0) {
                    continue;
                }
                Set<SelectionKey> selectionKeys = mSelector.selectedKeys();
                Iterator<SelectionKey> iterator = selectionKeys.iterator();
                while (iterator.hasNext()) {
                    SelectionKey selectionKey = iterator.next();

                    // 移除当前的key
                    iterator.remove();

                    if (selectionKey.isValid() && selectionKey.isConnectable()) {
                        handleConnect();
                    }
                    if (selectionKey.isValid() && selectionKey.isReadable()) {
                        try {
                            handleRead();
                        } catch (Exception e) {
                            System.out.println("handleRead() 报错：" + e.getMessage());
                        }
                    }
                    if (selectionKey.isValid() && selectionKey.isWritable()) {
                        handleWrite();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close();
        }
    }

    private void handleConnect() throws IOException {
        // 判断此通道上是否正在进行连接操作。
        if (mSocketChannel.isConnectionPending()) {
            mSocketChannel.finishConnect();
            mSocketChannel.register(mSelector, SelectionKey.OP_READ);
            Log.i(TAG, mClientName + " 请求跟服务端建立连接");
        }
    }

    private void handleRead() throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
        int bytesRead = mSocketChannel.read(byteBuffer);
        if (bytesRead > 0) {
            String inMsg = new String(byteBuffer.array(), 0, bytesRead);
            Log.i(TAG, mClientName + " (更新 UI 前)收到服务端数据： " + inMsg);

            //更新UI：客户端界面显示【服务器返回的消息：】
            Message msg = new Message();
            msg.obj = inMsg;
            msg.what = MainActivity.CLIENTGETMSG;
            mHandler.sendMessage(msg);

            Log.i(TAG, mClientName + " (更新 UI 后)收到服务端数据： " + inMsg);
        } else {
            Log.i(TAG, mClientName + "  断开跟 服务端的连接");

            //更新UI：服务器界面显示【客户端返回的消息：】
            Message mMessage= new Message();
            mMessage.obj = mClientName + " 断开跟 服务端的连接";
            mMessage.what = MainActivity.LOG;
            mHandler.sendMessage(mMessage);
            disconnectTcp();
        }
    }

    private void handleWrite() throws IOException {
        if (TextUtils.isEmpty(mSendMsg)) {

            //更新UI：服务器界面显示【客户端返回的消息：】
            Message mMessage = new Message();
            mMessage.obj = "handleWrite 内容为空";
            mMessage.what = MainActivity.LOG;
            mHandler.sendMessage(mMessage);
            return;
        }
        ByteBuffer sendBuffer = ByteBuffer.allocate(1024);
        sendBuffer.put(mSendMsg.getBytes());
        sendBuffer.flip();

        mSocketChannel.write(sendBuffer);

        Log.i(TAG, "--------------------------------------");
        Log.i(TAG, mClientName + " 发送数据： " + mSendMsg);

        mSendMsg = null;
        mSocketChannel.register(mSelector, SelectionKey.OP_READ);
    }

    /**
     * 发送数据
     *
     * @param msg
     * @throws IOException
     */
    public void sendMsg(String msg) {
        if (mSelector == null || !mSelector.isOpen() || mSocketChannel == null || !mSocketChannel.isOpen()) {

            //更新UI：服务器界面显示【客户端返回的消息：】
            Message mMessage = new Message();
            mMessage.obj = "sendMsg 失败！值为null或未开启";
            mMessage.what = MainActivity.LOG;
            mHandler.sendMessage(mMessage);

            return;
        }
        try {
            mSendMsg = msg;
            mSocketChannel.register(mSelector, SelectionKey.OP_WRITE);
            mSelector.wakeup();

            //更新UI：服务器界面显示【客户端返回的消息：】
            Message mMessage= new Message();
            mMessage.obj = mClientName + " 已发送：" + mSendMsg;
            mMessage.what = MainActivity.LOG;
            mHandler.sendMessage(mMessage);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 断开连接
     */
    public void disconnectTcp() {
        Log.i(TAG, "--------------------------------------");
        Log.i(TAG, mClientName + " 主动断开跟服务端连接");

        //更新UI：服务器界面显示【客户端返回的消息：】
        Message mMessage= new Message();
        mMessage.obj = mClientName + " 主动断开跟服务端连接";
        mMessage.what = MainActivity.LOG;
        mHandler.sendMessage(mMessage);
        close();
    }

    /**
     * 断开连接
     */
    private void close() {
        try {
            if (mSelector != null && mSelector.isOpen()) {
                mSelector.close();
            }
            if (mSocketChannel != null && mSocketChannel.isOpen()) {
                mSocketChannel.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
