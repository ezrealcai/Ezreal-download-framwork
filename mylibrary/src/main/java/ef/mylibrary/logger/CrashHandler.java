package ef.mylibrary.logger;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Environment;
import android.os.Looper;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * UncaughtException处理类,当程序发生Uncaught异常的时候,有该类来接管程序,并记录发送错误报告.
 *
 * @author Ezreal.cai
 *
 */
public class CrashHandler implements UncaughtExceptionHandler {

    public static final String TAG = "CrashHandler";

    //服务器URL;
    private String URL = "http://www.baidu.com";

    //Log文件存放目录
    private String path = "/sdcard/crash/";

    //系统默认的UncaughtException处理类
    private UncaughtExceptionHandler mDefaultHandler;
    //CrashHandler实例
    private static CrashHandler INSTANCE = new CrashHandler();
    //程序的Context对象
    private Context mContext;
    //用来存储设备信息和异常信息
    private Map<String, String> infos = new HashMap<String, String>();

    //用于格式化日期,作为日志文件名的一部分
    private DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");

    /** 保证只有一个CrashHandler实例 */
    private CrashHandler() {
    }

    /** 获取CrashHandler实例 ,单例模式 */
    public static CrashHandler getInstance() {
        return INSTANCE;
    }

    /**
     * 初始化
     * @param context
     * @param path 日志本地保存目录
     * @param url  日志上传地址
     */
    public void init(Context context,String path,String url) {
        this.path = path;
        this.URL = url;
        mContext = context;
        //获取系统默认的UncaughtException处理器
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        //设置该CrashHandler为程序的默认处理器
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    /**
     * 当UncaughtException发生时会转入该函数来处理
     */
    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        if (!handleException(ex) && mDefaultHandler != null) {
            //如果用户没有处理则让系统默认的异常处理器来处理
            mDefaultHandler.uncaughtException(thread, ex);
        } else {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Log.e(TAG, "error : ", e);
            }
            //退出程序
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(1);
        }
    }

    /**
     * 自定义错误处理,收集错误信息 发送错误报告等操作均在此完成.
     *
     * @param ex
     * @return true:如果处理了该异常信息;否则返回false.
     */
    private boolean handleException(Throwable ex) {
        if (ex == null) {
            return false;
        }
        //使用Toast来显示异常信息
        new Thread() {
            @Override
            public void run() {
                Looper.prepare();
                Toast.makeText(mContext, "很抱歉,程序出现异常,即将退出.", Toast.LENGTH_LONG).show();
                Looper.loop();
            }
        }.start();
        //收集设备参数信息
        collectDeviceInfo(mContext);
        //保存日志文件
        final String fileName = saveCrashInfo2File(ex);
        //post日志文件到server
        new Thread() {
            @Override
            public void run() {
                postLog2Server(URL,path,fileName);
            }
        }.start();
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * 收集设备参数信息
     * @param ctx
     */
    public void collectDeviceInfo(Context ctx) {
        try {
            PackageManager pm = ctx.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(ctx.getPackageName(), PackageManager.GET_ACTIVITIES);
            if (pi != null) {
                String versionName = pi.versionName == null ? "null" : pi.versionName;
                String versionCode = pi.versionCode + "";
                infos.put("versionName", versionName);
                infos.put("versionCode", versionCode);
            }
        } catch (NameNotFoundException e) {
            Log.e(TAG, "an error occured when collect package info", e);
        }
        Field[] fields = Build.class.getDeclaredFields();
        for (Field field : fields) {
            try {
                field.setAccessible(true);
                infos.put(field.getName(), field.get(null).toString());
                Log.d(TAG, field.getName() + " : " + field.get(null));
            } catch (Exception e) {
                Log.e(TAG, "an error occured when collect crash info", e);
            }
        }
    }

    /**
     * 保存错误信息到文件中
     *
     * @param ex
     * @return  返回文件名称,便于将文件传送到服务器
     */
    private String saveCrashInfo2File(Throwable ex) {

        StringBuffer sb = new StringBuffer();
        for (Map.Entry<String, String> entry : infos.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            sb.append(key + "=" + value + "\n");
        }

        Writer writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        ex.printStackTrace(printWriter);
        Throwable cause = ex.getCause();
        while (cause != null) {
            cause.printStackTrace(printWriter);
            cause = cause.getCause();
        }
        printWriter.close();
        String result = writer.toString();
        sb.append(result);
        try {
            long timestamp = System.currentTimeMillis();
            String time = formatter.format(new Date());
            String fileName = "crash-" + time + "-" + timestamp + ".log";
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                File dir = new File(path);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                FileOutputStream fos = new FileOutputStream(path + fileName);
                fos.write(sb.toString().getBytes());
                fos.close();
            }
            return fileName;
        } catch (Exception e) {
            Log.e(TAG, "an error occured while writing file...", e);
        }
        return null;
    }

    /**
     * 往服务器上上传文本 比如log日志
     * @param urlstr 请求的url
     * @param path log日志的路径
     * @param newName log日志的名字 LOG.log
     * @return
     */
    public  void postLog2Server(String urlstr, String path, String newName) {
        String end = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";//边界标识
        int TIME_OUT = 10*1000; //超时时间
        HttpURLConnection con = null;
        DataOutputStream ds = null;
        try {
            URL url = new URL(urlstr);
            con = (HttpURLConnection) url.openConnection();
            con.setReadTimeout(TIME_OUT);
            con.setConnectTimeout(TIME_OUT);
            //允许Input、Output，不使用Cache
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            // 设置http连接属性
            con.setRequestMethod("POST");//请求方式
            con.setRequestProperty("Connection", "Keep-Alive");//在一次TCP连接中可以持续发送多份数据而不会断开连接
            con.setRequestProperty("Charset", "UTF-8");//设置编码
            con.setRequestProperty("Content-Type",//multipart/form-data能上传文件的编码格式
                    "multipart/form-data;boundary=" + boundary);
            ds = new DataOutputStream(con.getOutputStream());
            ds.writeBytes(twoHyphens + boundary + end);
            ds.writeBytes("Content-Disposition: form-data; "
                    + "name=\"stblog\";filename=\"" + newName + "\"" + end);
            ds.writeBytes(end);
            // 取得文件的FileInputStream
            FileInputStream fStream = new FileInputStream(path + newName);
            //设置每次写入1024bytes
            int bufferSize = 1024;
            byte[] buffer = new byte[bufferSize];
            int length = -1;
            //从文件读取数据至缓冲区
            while ((length = fStream.read(buffer)) != -1) {
            //将资料写入DataOutputStream中
                ds.write(buffer, 0, length);
            }
            ds.writeBytes(end);
            ds.writeBytes(twoHyphens + boundary + twoHyphens + end);//结束
            fStream.close();
            ds.flush();
            //取得返回码
            int code = con.getResponseCode();

            Log.e("CrashHandler", code + "");
            Log.e("CrashHandler", "上传成功");
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("CrashHandler", "上传失败");
        }finally {
            //关闭DataOutputStream
            if(ds!=null){
                try {
                    ds.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (con != null) {
                con.disconnect();
            }
        }
    }

    /**
     * 显示dialog
     * @param activity
     * @param isSuccess
     * @param uploadFile
     * @param mess
     */
    private static void showDialog(final Activity activity, final Boolean isSuccess, final String uploadFile, final String mess) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(activity).setTitle("Message")
                        .setMessage(mess)
                        .setNegativeButton("确定", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                File file = new File(uploadFile);
                                if(file.exists()&&isSuccess){//日志文件存在且上传日志成功
                                    file.delete();
                                    Toast.makeText(activity, "log日志已删除", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }).show();
            }
        });
    }

}