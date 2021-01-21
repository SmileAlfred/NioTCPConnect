package com.example.niotcpconnect;

import android.app.Activity;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;


import com.example.niotcpconnect.utils.FileUtils;

import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 问题描述：服务器返回的消息，客户端无法显示；
 * 是服务器没有返回？还是客户端没有接收到？
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private Button btn_client_send, btn_server_send;
    private EditText et_content_client, et_ip, et_content_server;
    private static final String TAG = "测试：" + MainActivity.class.getSimpleName();
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
    private TCPServerService tcpServerService;
    private boolean mBounded;
    private Handler handler;
    public static final int CLIENTGETMSG = 1, SERVERGETMSG = 2, LOG = 3;
    public TextView tv_content;
    private SimpleDateFormat format;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS_STORAGE, 0);

        Intent mIntent = new Intent(this, TCPServerService.class);
        //startService(mIntent);    //无法进行 activity 和 service 通信的原方法
        bindService(mIntent, conn, BIND_AUTO_CREATE);
        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                String data;
                switch (msg.what) {
                    case CLIENTGETMSG:
                        //完成主界面更新,拿到数据
                        data = (String) msg.obj;
                        et_content_server.setText(data);
                        break;
                    case SERVERGETMSG:
                        //完成主界面更新,拿到数据
                        data = (String) msg.obj;
                        et_content_client.setText(data);
                        break;
                    case LOG:
                        //完成 Log 信息的更新
                        format = new SimpleDateFormat("hh:mm:ss");
                        tv_content.append("\n [" + format.format(new Date()) + "] " + msg.obj.toString());
                        break;
                    default:
                        break;
                }
            }
        };
        mTcpClient1 = new TCPClient(MainActivity.this, handler, "客户端A");
        mTcpClient2 = new TCPClient(MainActivity.this, handler, "客户端B");

        et_ip = findViewById(R.id.et_ip);
        et_content_server = findViewById(R.id.et_content_server);
        et_content_client = findViewById(R.id.et_content_client);

        btn_server_send = findViewById(R.id.btn_server_send);
        btn_server_send.setOnClickListener(this);

        btn_client_send = findViewById(R.id.btn_client_send);
        btn_client_send.setOnClickListener(this);

        tv_content = findViewById(R.id.tv_content);

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

        Button btn_pickfile = findViewById(R.id.btn_pickfile);
        btn_pickfile.setOnClickListener(this);
    }

    private ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBounded = true;
            TCPServerService.LocalBinder binder = (TCPServerService.LocalBinder) service;
            tcpServerService = binder.getServerInstance();//获取 service 对象
            tcpServerService.setHandler(handler);

        }

        //client 和service连接意外丢失时，会调用该方法
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBounded = false;
            //更新UI：服务器界面显示【客户端返回的消息：】
            Message mMessage = new Message();
            mMessage.obj = "服务传给Activity说:断开链接了呀!";
            mMessage.what = MainActivity.LOG;
            handler.sendMessage(mMessage);
        }

    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        if (mBounded) {
            unbindService(conn);
            mBounded = false;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_connection1:
                ipAdress = et_ip.getText().toString();
                mTcpClient1.requestConnectTcp(ipAdress);
                break;
            case R.id.btn_disconnect1:
                mTcpClient1.disconnectTcp();
                break;
            case R.id.btn_send1:
                mTcpClient1.sendMsg("1_你好吗？");
                break;

            case R.id.btn_connection2:
                ipAdress = et_ip.getText().toString();
                mTcpClient2.requestConnectTcp(ipAdress);
                break;
            case R.id.btn_disconnect2:
                mTcpClient2.disconnectTcp();
                break;
            case R.id.btn_send2:
                mTcpClient2.sendMsg("2_吃饭了吗？");
                break;

            case R.id.btn_server_send:
                tcpServerService.getmTCPServer().sendMsg(et_content_server.getText().toString());
                break;
            case R.id.btn_client_send:
                mTcpClient1.sendMsg(et_content_client.getText().toString());
                mTcpClient2.sendMsg(et_content_client.getText().toString());
                break;
            case R.id.btn_pickfile:
                //TODO:选择文件
                performFileSearch();
                break;
            default:
                break;
        }
    }

    //选择文件
    private void performFileSearch() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        //允许多选 长按多选
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        //不限制选取类型
        intent.setType("*/*");
        startActivityForResult(intent, -1);
    }

    //接收返回值
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case -1:
                if (resultCode == Activity.RESULT_OK && data != null) {
                    //当单选选了一个文件后返回
                    if (data.getData() != null) {
                        handleSingleDocument(data);
                    } else {
                        //多选
                        ClipData clipData = data.getClipData();
                        if (clipData != null) {
                            Uri[] uris = new Uri[clipData.getItemCount()];
                            for (int i = 0; i < clipData.getItemCount(); i++) {
                                uris[i] = clipData.getItemAt(i).getUri();
                                Log.i(TAG, "获取到第 " + i + " 个文件目录: " + uris[i]);
                            }
                        }
                    }
                }
                break;
        }
    }

    //将uri转换为我们需要的path,多选类似
    private void handleSingleDocument(Intent data) {
        Uri uri = data.getData();
        String filePath = FileUtils.getRealPath(MainActivity.this, uri);
        Log.i(TAG, "获取到 单个 文件目录: " + filePath);
    }
}
