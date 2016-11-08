package com.ef.efstudents_android.download;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Administrator on 2016/10/16.
 */
public class DBOpenHelper extends SQLiteOpenHelper {
    private static final String DBNAME = "download.db";
    private static final int VERSION = 1;

    public DBOpenHelper(Context context) {
        super(context, DBNAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS downloadinfo (id INTEGER primary key " +
                "autoincrement, path varchar(256), downloadurl varchar(256) UNIQUE, size INT8, " +
                "currentlength INT8)");
        db.execSQL("CREATE TABLE IF NOT EXISTS downloadlog (id INTEGER primary key " +
                "autoincrement, path varchar(256), downloadurl varchar(256), size INT8," +
                "time INT8)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS downloadinfo");
        db.execSQL("DROP TABLE IF EXISTS downloadlog");
        onCreate(db);
    }
}
