package com.example.niotcpconnect;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;


import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private EditText et_content,et_ip;
    private Button btn_send_content;
    private static final String TAG = "测试："+MainActivity.class.getSimpleName();
    private ByteBuffer buf;
    private final String[] PERMISSIONS_STORAGE = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.ACCESS_WIFI_STATE",
            "android.permission.CHANGE_WIFI_STATE",
            "android.permission.ACCESS_NETWORK_STATE",
            "android.permission.CHANGE_NETWORK_STATE"};
    private TCPClient mTcpClient1, mTcpClient2;
    private String ipAdress;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS_STORAGE, 0);

        Intent service = new Intent(this, TCPServerService.class);
        startService(service);

        mTcpClient1 = new TCPClient(MainActivity.this, "客户端A");
        mTcpClient2 = new TCPClient(MainActivity.this, "客户端B");

        TextView tv_content = findViewById(R.id.tv_content);
        Button btnConnection1 = findViewById(R.id.btn_connection1);
        btnConnection1.setOnClickListener(this);
        Button btnSend1 = findViewById(R.id.btn_send1);
        btnSend1.setOnClickListener(this);
        Button btnDisconnect1 = findViewById(R.id.btn_disconnect1);
        btnDisconnect1.setOnClickListener(this);

        Button btnConnection2 = findViewById(R.id.btn_connection2);
        btnConnection2.setOnClickListener(this);
        Button btnSend2 = findViewById(R.id.btn_send2);
        btnSend2.setOnClickListener(this);
        Button btnDisconnect2 = findViewById(R.id.btn_disconnect2);
        btnDisconnect2.setOnClickListener(this);

        et_ip = findViewById(R.id.et_ip);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_connection1:
                ipAdress =et_ip.getText().toString();
                mTcpClient1.requestConnectTcp(ipAdress);
                break;
            case R.id.btn_disconnect1:
                mTcpClient1.disconnectTcp();
                break;
            case R.id.btn_send1:
                mTcpClient1.sendMsg("1_你好吗？");
                break;

            case R.id.btn_connection2:
                ipAdress =et_ip.getText().toString();
                mTcpClient2.requestConnectTcp(ipAdress);
                break;
            case R.id.btn_disconnect2:
                mTcpClient2.disconnectTcp();
                break;
            case R.id.btn_send2:
                mTcpClient2.sendMsg("2_吃饭了吗？");
                break;    default:
                break;
        }
    }

        /*  public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS_STORAGE, 0);
        et_content = findViewById(R.id.et_content);
        btn_send_content = findViewById(R.id.btn_send_content);
        btn_send_content.setOnClickListener(this);

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (null == socketChannel) {
                    try {
                        socketChannel = SocketChannel.open(new InetSocketAddress("192.168.1.10", 9999));
                        socketChannel.configureBlocking(false);

                        Log.i(TAG, "run: 连接成功！socketChannel = " + socketChannel);

                        selector = Selector.open();
                        socketChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE | SelectionKey.OP_CONNECT);

                        int result = 0;
                        int i = 1;
                        while ((result = selector.select()) > 0) {
                            System.out.println(String.format("selector %dth loop, ready event number is %d", i++, result));
                            Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                            while (iterator.hasNext()) {
                                SelectionKey sk = iterator.next();

                                if (sk.isReadable()) {
                                    System.out.println("有数据可读");
                                }

                                iterator.remove();
                            }
                        }
                    } catch (Exception e) {
                        Log.i(TAG, "run: 连接报错！" + e.getMessage());
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException ex) {
                        }
                    }
                }
            }
        }).start();
    }

    SocketChannel socketChannel = null;
    Selector selector = null;

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_send_content:
                if (null == socketChannel) {
                    Toast.makeText(this, " null == socketChannel ", Toast.LENGTH_SHORT).show();
                    return;
                }
                String str = et_content.getText().toString();
                ByteBuffer buf = ByteBuffer.allocate(1024);
                buf.clear();
                //buf.put((new Date().toString() + "\n客户端：" + str).getBytes());
                buf.put(str.getBytes());
                buf.flip();
                try {
                    //注意SocketChannel.write()方法的调用是在一个while循环中的。Write()方法无法保证能写多少字节到SocketChannel。
                    // 所以，我们重复调用write()直到Buffer没有要写的字节为止。
                    while (buf.hasRemaining()) {
                        Log.i(TAG, "onClick: buf = " + buf);
                        socketChannel.write(buf);
                    }

                    buf.clear();
                } catch (Exception e) {
                    Toast.makeText(this, " 客户端发送数据报错！ " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.i(TAG, "onClick: 客户端发送数据报错！" + e.getMessage());
                }
                break;
            default:
                break;
        }
    }*/
}
