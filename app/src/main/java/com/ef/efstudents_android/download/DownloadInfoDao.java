package com.ef.efstudents_android.download;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Administrator on 2016/10/16.
 */
public class DownloadInfoDao {

    private DBOpenHelper openHelper;

    public DownloadInfoDao(Context context) {
        openHelper = new DBOpenHelper(context);
    }

    /**
     * 获取下载的文件信息
     *
     * @param downloadUrl
     * @return
     */
    public DownloadInfo get(String downloadUrl) {
        SQLiteDatabase db = openHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("select path, downloadurl, size, currentlength from downloadinfo" +
                " where downloadurl=?", new String[]{downloadUrl});
        DownloadInfo data = null;
        while (cursor.moveToNext()) {
            data = new DownloadInfo();
            data.setState(DownloadManager.STATE_PAUSE);
            data.setPath(cursor.getString(0));
            data.setDownloadUrl(cursor.getString(1));
            data.setSize(cursor.getInt(2));
            data.setCurrentLength(cursor.getInt(3));
        }
        cursor.close();
        db.close();
        return data;
    }

    /**
     * 获取下载的文件信息
     *
     * @return
     */
    public Map<String, DownloadInfo> getAll() {
        SQLiteDatabase db = openHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("select path, downloadurl, size, currentlength from " +
                "downloadinfo", null);
        Map<String, DownloadInfo> datas = new HashMap<>();
        while (cursor.moveToNext()) {
            DownloadInfo data = new DownloadInfo();
            data.setState(DownloadManager.STATE_PAUSE);
            data.setPath(cursor.getString(0));
            data.setDownloadUrl(cursor.getString(1));
            data.setSize(cursor.getInt(2));
            data.setCurrentLength(cursor.getInt(3));
            datas.put(cursor.getString(1), data);
        }
        cursor.close();
        db.close();
        return datas;
    }

    public void save(DownloadInfo downloadInfo) {
        SQLiteDatabase db = openHelper.getWritableDatabase();
        db.execSQL("insert into downloadinfo (path, downloadurl, size, currentlength) values" +
                "(?, ?, ?, ?)", new Object[]{downloadInfo.getPath(), downloadInfo.getDownloadUrl(),
                downloadInfo.getSize(), downloadInfo.getCurrentLength()});
        db.close();
    }

    /**
     * 更新已经下载的文件长度
     *
     * @param downloadUrl
     * @param currentLength
     */
    public void update(String downloadUrl, long currentLength) {
        SQLiteDatabase db = openHelper.getWritableDatabase();
        db.execSQL("update downloadinfo set currentlength=? where downloadurl=?",
                new Object[]{currentLength, downloadUrl});
        db.close();
    }

    /**
     * 设置文件大小
     *
     * @param downloadUrl
     * @param size
     */
    public void setSize(String downloadUrl, long size) {
        SQLiteDatabase db = openHelper.getWritableDatabase();
        db.execSQL("update downloadinfo set size=? where downloadurl=?",
                new Object[]{size, downloadUrl});
        db.close();
    }

    /**
     * 删除下载记录
     *
     * @param downloadUrl
     */
    public void delete(String downloadUrl) {
        SQLiteDatabase db = openHelper.getWritableDatabase();
        db.execSQL("delete from downloadinfo where downloadurl=?", new Object[]{downloadUrl});
        db.close();
    }

}
