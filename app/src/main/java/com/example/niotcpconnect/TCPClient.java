package com.example.niotcpconnect;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

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
    private String mClientName;                                                 // 客户端命名
    private Selector mSelector;
    private SocketChannel mSocketChannel;

    private ThreadPoolExecutor mConnectThreadPool;                              // 消息连接和接收的线程池
    private Context context;

    public TCPClient(Context context, String clientName) {
        this.context = context;
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
                int select = mSelector.select();                            // 当没有消息时，这里也是会阻塞的
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
                        handleRead();
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
            Log.d(TAG, mClientName + " 请求跟服务端建立连接");
        }
    }

    private void handleRead() throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
        int bytesRead = mSocketChannel.read(byteBuffer);
        if (bytesRead > 0) {
            String inMsg = new String(byteBuffer.array(), 0, bytesRead);
            Toast.makeText(context, inMsg, Toast.LENGTH_SHORT).show();
            Log.d(TAG, mClientName + " 收到服务端数据： " + inMsg);
        } else {
            Log.d(TAG, mClientName + "  断开跟 服务端的连接");
            disconnectTcp();
        }
    }

    private void handleWrite() throws IOException {
        if (TextUtils.isEmpty(mSendMsg)) {
            return;
        }
        ByteBuffer sendBuffer = ByteBuffer.allocate(1024);
        sendBuffer.put(mSendMsg.getBytes());
        sendBuffer.flip();

        mSocketChannel.write(sendBuffer);

        Log.d(TAG, "--------------------------------------");
        Log.d(TAG, mClientName + " 发送数据： " + mSendMsg);

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
            return;
        }
        try {
            mSendMsg = msg;
            mSocketChannel.register(mSelector, SelectionKey.OP_WRITE);
            mSelector.wakeup();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 断开连接
     */
    public void disconnectTcp() {
        Log.d(TAG, "--------------------------------------");
        Log.d(TAG, mClientName + " 主动断开跟服务端连接");

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
