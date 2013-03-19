package org.nkuznetsov.lib.cman;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class CacheDatabaseHelper extends SQLiteOpenHelper 
{
	private static final String DBNAME = "cman.cache";
	private static final int DBVERSION = 1;
	
	private SQLiteDatabase db;
	
	public CacheDatabaseHelper(Context context) 
	{
		super(context, DBNAME, null, DBVERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) 
	{
		db.execSQL(CacheTable.SQL_CREATE);
	}

	@Override
	public SQLiteDatabase getWritableDatabase() 
	{
		if (db == null) db = super.getWritableDatabase();
		return db;
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}

	public static class CacheTable
	{
		public static final String TABLE_NAME = "Cache";
		
		public static final String COLUMN_CACHETIME = "cachetime";
		public static final String COLUMN_CACHEEXPIREDTIME = "cacheexpiredtime";
		public static final String COLUMN_URL = "url";
		public static final String COLUMN_DATA = "data";
		
		public static final String SQL_CREATE;
		
		static
		{
			SQL_CREATE = "CREATE  TABLE `" + TABLE_NAME + "` (`" + COLUMN_CACHETIME + "` TEXT NOT NULL  DEFAULT CURRENT_TIMESTAMP, " +
					"`" + COLUMN_CACHEEXPIREDTIME + "` TEXT NOT NULL, `" + COLUMN_URL + "` TEXT NOT NULL  UNIQUE, " +
					"`" + COLUMN_DATA + "` BLOB NOT NULL)";
		}
	}
}
