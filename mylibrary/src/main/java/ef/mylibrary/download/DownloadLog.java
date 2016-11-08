package ef.mylibrary.download;

/**
 * 下载完成的历史记录
 */
public class DownloadLog {
    private long size;// 总大小
    private String downloadUrl;// 下载地址
    private String path;// 文件保存的绝对路径
    private long time;

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
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
