package org.nkuznetsov.lib.cman;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import org.nkuznetsov.lib.cman.CacheDatabaseHelper.CacheTable;
import org.nkuznetsov.lib.cman.utils.CManUtils;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

public class CacheDatabase extends Cache 
{
	private SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private CacheDatabaseHelper db;

	public CacheDatabase(Context context) 
	{
		super(context);
		db = new CacheDatabaseHelper(context);
		format.setTimeZone(TimeZone.getTimeZone("GMT"));
		clearExpired();
	}

	@Override
	public void destroy()
	{
		db.close();
		db = null;
	}
	
	@Override
	public boolean isCached(String url) 
	{
		clearExpired();
		int res = 0;
		Cursor c = db.getWritableDatabase().rawQuery("SELECT COUNT(*) FROM `" + CacheTable.TABLE_NAME + 
				"` WHERE `" + CacheTable.COLUMN_URL + "` = ?", new String[]{CManUtils.MD5Hash(url)});
		if (c.moveToFirst()) res = c.getInt(0);
		c.close();
		return (res == 0) ? false : true;
	}

	@Override
	public void clear() 
	{
		db.getWritableDatabase().delete(CacheTable.TABLE_NAME, null, null);
	}

	@Override
	public void clearExpired() 
	{
		db.getWritableDatabase().delete(CacheTable.TABLE_NAME, 
				CacheTable.COLUMN_CACHEEXPIREDTIME + " < CURRENT_TIMESTAMP", null);
	}

	@Override
	public long getMaxNewCacheFileSize()
	{
		return 1024 * 1024;
	}
	
	@Override
	public byte[] get(String url) 
	{
		byte[] bytes = null;
		if (isCached(url))
		{
			Cursor c = db.getWritableDatabase().query(CacheTable.TABLE_NAME, new String[]{CacheTable.COLUMN_DATA}, 
					CacheTable.COLUMN_URL + " = ?", new String[]{CManUtils.MD5Hash(url)}, null, null, null);
			if (c.moveToFirst()) bytes = c.getBlob(0);
			c.close();
		}
		return bytes;
	}
	
	@Override
	public InputStream getStream(String url)
	{
		byte[] bytes = get(url);
		return bytes != null ? new ByteArrayInputStream(bytes) : null;
	}

	@Override
	public void put(String url, byte[] data, int expired) 
	{
		ContentValues values = new ContentValues();
		values.put(CacheTable.COLUMN_URL, CManUtils.MD5Hash(url));
		values.put(CacheTable.COLUMN_CACHEEXPIREDTIME, format.format(new Date(getExpiredTime(expired))));
		values.put(CacheTable.COLUMN_DATA, data);
		db.getWritableDatabase().insert(CacheTable.TABLE_NAME, null, values);
	}

	@Override
	public InputStream put(String url, InputStream is, int expired) throws IOException
	{
		byte[] bytes = CManUtils.readBytes(is);
		put(url, bytes, expired);
		return new ByteArrayInputStream(bytes);
	}
	
	@Override
	public void remove(String url)
	{
		db.getWritableDatabase().delete(CacheTable.TABLE_NAME, CacheTable.COLUMN_URL + " = ?", new String[]{CManUtils.MD5Hash(url)});
	}
}