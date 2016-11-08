package ef.mylibrary.download;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Administrator on 2016/10/16.
 */
public class DownloadLogDao {

    private DBOpenHelper openHelper;

    public DownloadLogDao(Context context) {
        openHelper = new DBOpenHelper(context);
    }

    /**
     * 获取下载的文件历史记录
     *
     * @param downloadUrl
     * @return
     */
    public DownloadLog get(String downloadUrl) {
        SQLiteDatabase db = openHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("select path, downloadurl, size, time from downloadlog"
                + " where downloadurl=?", new String[]{downloadUrl});
        DownloadLog data = null;
        while (cursor.moveToNext()) {
            data = new DownloadLog();
            data.setPath(cursor.getString(0));
            data.setDownloadUrl(cursor.getString(1));
            data.setSize(cursor.getInt(2));
            data.setTime(cursor.getInt(3));
        }
        cursor.close();
        db.close();
        return data;
    }

    /**
     * 获取所有下载的文件历史记录
     *
     * @return
     */
    public Map<String, DownloadLog> getAll() {
        SQLiteDatabase db = openHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("select path, downloadurl, size, time from " +
                "downloadlog", null);
        Map<String, DownloadLog> datas = new HashMap<>();
        while (cursor.moveToNext()) {
            DownloadLog data = new DownloadLog();
            data.setPath(cursor.getString(0));
            data.setDownloadUrl(cursor.getString(1));
            data.setSize(cursor.getInt(2));
            data.setTime(cursor.getInt(3));
            datas.put(cursor.getString(1), data);
        }
        cursor.close();
        db.close();
        return datas;
    }

    public void save(DownloadLog downloadLog) {
        SQLiteDatabase db = openHelper.getWritableDatabase();
        db.execSQL("insert into downloadlog (path, downloadurl, size, time) values" +
                "(?, ?, ?, ?)", new Object[]{downloadLog.getPath(), downloadLog.getDownloadUrl(),
                downloadLog.getSize(), downloadLog.getTime()});
        db.close();
    }

}
