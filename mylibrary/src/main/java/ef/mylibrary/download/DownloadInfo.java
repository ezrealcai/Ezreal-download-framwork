package ef.mylibrary.download;

import java.io.File;

/**
 * 下载任务相关的数据
 *
 */
public class DownloadInfo {
    private int state;// 下载状态
    private long currentLength;// 已经下载的长度
    private long size;// 总大小
    private String downloadUrl;// 下载地址
    private String path;// 文件保存的绝对路径

    /**
     * 创建DownloadInfo
     *
     * @return
     */
    public static DownloadInfo create(String downloadUrl, String downloadDir) {
        DownloadInfo downloadInfo = new DownloadInfo();
        downloadInfo.setDownloadUrl(downloadUrl);
        downloadInfo.setState(DownloadManager.STATE_NONE);// 初始化状态
        downloadInfo.setCurrentLength(0);
        downloadInfo.setPath(downloadDir + File.separator
                + downloadUrl.substring(downloadUrl.lastIndexOf('/') + 1) + ".tmp");
        return downloadInfo;
    }


    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public long getCurrentLength() {
        return currentLength;
    }

    public void setCurrentLength(long currentLength) {
        this.currentLength = currentLength;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

}
