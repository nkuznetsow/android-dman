package org.nkuznetsov.libs.dman.test;

import org.nkuznetsov.lib.dman.DownloadManager;
import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

public class AndroidDownloadManagerTestActivity extends Activity 
{
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        DownloadManager.initCache(this);
        
        
        new AsyncTask<Void, Void, Void>()
		{
        	DownloadManager dm;
        	
        	ProgressDialog progress;
        	
        	@Override
        	protected void onPreExecute()
        	{
        		progress = ProgressDialog.show(AndroidDownloadManagerTestActivity.this, "123", "123");
        	}
        	
			@Override
			protected Void doInBackground(Void... params)
			{
				dm = new DownloadManager("http://ftp.yandex.ru/opensuse/factory/repo/oss/ChangeLog");
				dm.execute(600);
				return null;
			}
        	
			@Override
			protected void onPostExecute(Void result)
			{
				Log.d("Test", dm.getResponseStream().toString());
				progress.dismiss();
			}
		}.execute();
    }
}