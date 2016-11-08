package ef.mylibrary.download;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;


public class DownloadManager {
    private Context mContext;
    public String mDownloadDir;

    //定义下载状态常量
    public static final int STATE_NONE = 0;//未下载的状态
    public static final int STATE_DOWNLOADING = 1;//下载中的状态
    public static final int STATE_PAUSE = 2;//暂停的状态
    public static final int STATE_WAITING = 3;//等待中的状态，任务对象已经创建，但是run方法没有执行
    public static final int STATE_FINISH = 4;//下载完成的状态
    public static final int STATE_ERROR = 5;//下载出错的状态

    private static DownloadManager mInstance = new DownloadManager();

    public static DownloadManager getInstance() {
        return mInstance;
    }

    //用来存放所有界面的监听器对象
    private ArrayList<DownloadObserver> observerList = new ArrayList<>();

    //用来存放所有的任务的DownloadInfo数据
    private Map<String, DownloadInfo> downloadInfoMap;

    private DownloadInfoDao downloadInfoDao;

    private DownloadManager() {
    }

    /**
     * 初始化，下载之前调用
     *
     * @param context
     * @return
     */
    public DownloadManager init(Context context) {
        mContext = context.getApplicationContext();
        mDownloadDir = Environment.getExternalStorageDirectory().getPath() +
                File.separator + mContext.getPackageName() +
                File.separator + "download";
        //初始化下载目录
        File file = new File(mDownloadDir);
        if (!file.exists()) {
            file.mkdirs();
        }

        downloadInfoDao = new DownloadInfoDao(mContext);
        downloadInfoMap = downloadInfoDao.getAll();
        return this;
    }


    /**
     * 下载
     */
    public void download(String downloadUrl) {
        //获取下载任务对应的DownloadInfo
        DownloadInfo downloadInfo = downloadInfoMap.get(downloadUrl);
        if (downloadInfo != null) {
            File file = new File(downloadInfo.getPath());
            if (!file.exists()) {
                //该任务曾下载过但已删除
                downloadInfo.setCurrentLength(0);
                downloadInfo.setState(DownloadManager.STATE_NONE);
                synchronized (DownloadTask.class) {
                    downloadInfoDao.update(downloadInfo.getDownloadUrl(), 0);
                }
            }
        }

        if (downloadInfo == null) {
            //该任务从来没有下载过
            downloadInfo = DownloadInfo.create(downloadUrl, mDownloadDir);
            //将downloadInfo存入downloadInfoMap和数据库中
            downloadInfoMap.put(downloadUrl, downloadInfo);
            synchronized (DownloadTask.class) {
                downloadInfoDao.save(downloadInfo);
            }
        }

        //获取下载任务对应的state来判断是否能够进行下载: none,pause,error
        int state = downloadInfo.getState();
        if (state == STATE_NONE || state == STATE_PAUSE || state == STATE_ERROR) {
            //可以进行下载
            //创建DownloadTask
            DownloadTask downloadTask = new DownloadTask(downloadInfo);
            //更新状态为等待中
            downloadInfo.setState(STATE_WAITING);
            notifyDownloadStateChange(downloadInfo);

            //将DownloadTask交给线程池执行
            ThreadPoolManager.getInstance().execute(downloadTask);
        }
    }

    /**
     * 下载任务
     */
    class DownloadTask implements Runnable {
        private DownloadInfo downloadInfo;

        public DownloadInfo getDownloadInfo() {
            return downloadInfo;
        }

        public DownloadTask(DownloadInfo downloadInfo) {
            this.downloadInfo = downloadInfo;
        }

        @Override
        public void run() {
            File file = new File(downloadInfo.getPath());
            String downloadUrl = downloadInfo.getDownloadUrl();
            try {
                URL url = new URL(downloadUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5000);
                conn.setRequestMethod("GET");
                int code = conn.getResponseCode();
                if (code < 300) {
                    int length = conn.getContentLength();
                    System.out.println("文件大小为:" + length);

                    downloadInfo.setSize(length);
                    synchronized (DownloadTask.class) {
                        downloadInfoDao.setSize(downloadUrl, length);
                    }

                    RandomAccessFile raf = new RandomAccessFile(file, "rw");
                    raf.setLength(length);
                    raf.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
                removeDownloadInfo(downloadUrl);
                return;
            }


            //更新状态为下载中
            downloadInfo.setState(STATE_DOWNLOADING);
            notifyDownloadStateChange(downloadInfo);

            try {
                URL url = new URL(downloadUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                RandomAccessFile raf = new RandomAccessFile(file, "rw");
                long currentPosition = downloadInfo.getCurrentLength();
                if (currentPosition > 0) {
                    conn.setRequestProperty("Range", "bytes=" + currentPosition + "-");
                    raf.seek(currentPosition);
                } else {
                    raf.seek(0);
                }

                InputStream is = conn.getInputStream();
                byte[] buffer = new byte[1024 * 8];
                int len;
                while ((len = is.read(buffer)) > 0 && downloadInfo.getState() ==
                        STATE_DOWNLOADING) {
                    raf.write(buffer, 0, len);
                    currentPosition += len;
                    //更新currentLength
                    downloadInfo.setCurrentLength(downloadInfo.getCurrentLength() + len);
                    //通知监听器下载进度更新
                    notifyDownloadProgressChange(downloadInfo);
                }
                synchronized (DownloadTask.class) {
                    downloadInfoDao.update(downloadUrl, currentPosition);
                }
                raf.close();
                is.close();
            } catch (Exception e) {
                e.printStackTrace();
                //下载失败
                processDownloadError(file);
            }

            if (file.length() == downloadInfo.getSize() && downloadInfo.getState() ==
                    STATE_DOWNLOADING) {
                //下载完成
                downloadInfo.setState(STATE_FINISH);
                notifyDownloadStateChange(downloadInfo);

                removeDownloadInfo(downloadUrl);

                String path = downloadInfo.getPath();
                String newPath = path.substring(0, path.lastIndexOf(".tmp"));
                file.renameTo(new File(newPath));

                //保存下载完成的历史记录
                DownloadLog downloadLog = new DownloadLog();
                downloadLog.setTime(new Date().getTime());
                downloadLog.setPath(newPath);
                downloadLog.setDownloadUrl(downloadUrl);
                downloadLog.setSize(downloadInfo.getSize());
                DownloadLogDao downloadLogDao = new DownloadLogDao(mContext);
                downloadLogDao.save(downloadLog);

            } else if (downloadInfo.getState() == STATE_PAUSE) {
                //下载暂停
                notifyDownloadStateChange(downloadInfo);
            }

        }

        /**
         * 处理下载失败的情况
         *
         * @param file
         */
        private void processDownloadError(File file) {
            file.delete();//删除失败文件
            downloadInfo.setCurrentLength(0);//清空currentLength
            downloadInfo.setState(STATE_ERROR);//更改状态为error
            notifyDownloadStateChange(downloadInfo);//通知状态更改
        }

    }

    /**
     * 通知所有的监听器状态更改
     *
     * @param downloadInfo
     */
    private void notifyDownloadStateChange(final DownloadInfo downloadInfo) {
        runOnUIThread(new Runnable() {
            @Override
            public void run() {
                for (DownloadObserver observer : observerList) {
                    observer.onDownloadStateChange(downloadInfo);
                }
            }
        });
    }

    /**
     * 通知所有的监听器下载进度更新
     *
     * @param downloadInfo
     */
    private void notifyDownloadProgressChange(final DownloadInfo downloadInfo) {
        runOnUIThread(new Runnable() {
            @Override
            public void run() {
                for (DownloadObserver observer : observerList) {
                    observer.onDownloadProgressChange(downloadInfo);
                }
            }
        });
    }

    /**
     * 获取所有的DownloadInfo
     *
     * @return
     */
    public Map<String, DownloadInfo> getAllDownloadInfo() {
        return downloadInfoMap;
    }

    /**
     * 获取对应的DownloadInfo
     *
     * @param downloadUrl
     * @return
     */
    public DownloadInfo getDownloadInfo(String downloadUrl) {
        return downloadInfoMap.get(downloadUrl);
    }

    /**
     * 获取所有的下载完成的历史记录
     *
     * @return
     */
    public Map<String, DownloadLog> getAllDownloadLog() {
        DownloadLogDao downloadLogDao = new DownloadLogDao(mContext);
        return downloadLogDao.getAll();
    }

    /**
     * 获取对应的下载完成的历史记录
     *
     * @param downloadUrl
     * @return
     */
    public DownloadLog getDownloadLog(String downloadUrl) {
        DownloadLogDao downloadLogDao = new DownloadLogDao(mContext);
        return downloadLogDao.get(downloadUrl);
    }

    /**
     * 暂停下载
     */
    public void pause(String downloadUrl) {
        DownloadInfo downloadInfo = getDownloadInfo(downloadUrl);
        if (downloadInfo != null) {
            //将当前downloadInfo的state设置为pause
            downloadInfo.setState(STATE_PAUSE);//更改状态
            notifyDownloadStateChange(downloadInfo);
        }
    }

    /**
     * 取消下载
     *
     * @param downloadUrl
     */
    public void cancel(String downloadUrl) {
        Iterator<Runnable> iterator = ThreadPoolManager.getInstance().getQueue().iterator();
        for (; iterator.hasNext(); ) {
            Runnable task = iterator.next();
            if (((DownloadTask) task).getDownloadInfo().getDownloadUrl().equals(downloadUrl)) {
                ThreadPoolManager.getInstance().remove(task);
            }

        }
        removeDownloadInfo(downloadUrl);
    }

    /**
     * 移除下载信息
     *
     * @param downloadUrl
     */
    private void removeDownloadInfo(String downloadUrl) {
        synchronized (DownloadTask.class) {
            downloadInfoDao.delete(downloadUrl);
            downloadInfoMap.remove(downloadUrl);
        }
    }

    /**
     * 注册下载观察者
     *
     * @param downloadObserver
     */
    public void registerDownloadObserver(DownloadObserver downloadObserver) {
        if (!observerList.contains(downloadObserver)) {
            observerList.add(downloadObserver);
        }
    }

    /**
     * 从集合中移除下载观察者
     *
     * @param downloadObserver
     */
    public void unregisterDownloadObserver(DownloadObserver downloadObserver) {
        if (observerList.contains(downloadObserver)) {
            observerList.remove(downloadObserver);
        }
    }


    /**
     * 下载状态和进度改变的监听器
     *
     * @author Administrator
     */
    public interface DownloadObserver {
        /**
         * 下载状态改变的回调
         */
        void onDownloadStateChange(DownloadInfo downloadInfo);

        /**
         * 下载进度改变的回调
         */
        void onDownloadProgressChange(DownloadInfo downloadInfo);
    }

    /**
     * 在UI线程执行任务
     *
     * @param task
     */
    public void runOnUIThread(Runnable task) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(task);
    }
}
