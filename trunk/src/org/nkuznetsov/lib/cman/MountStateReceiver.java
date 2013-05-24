package org.nkuznetsov.lib.cman;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class MountStateReceiver extends BroadcastReceiver
{
	private MountStateReceiverCallback callback;
	private boolean registred = false;
	private Context context;
	
	public MountStateReceiver(Context context, MountStateReceiverCallback callback)
	{
		this.context = context;
		this.callback = callback;
	}
	
	@Override
	public void onReceive(Context context, Intent intent)
	{
		if (callback != null) callback.onMountStateChanged(context, intent);
	}
	
	public void register()
	{
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL);
		intentFilter.addAction(Intent.ACTION_MEDIA_EJECT);
		intentFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
		intentFilter.addAction(Intent.ACTION_MEDIA_REMOVED);
		intentFilter.addDataScheme("file");
		context.registerReceiver(this, intentFilter);
		registred = true;
	}
	
	public void unregister()
	{
		if (registred) context.unregisterReceiver(this);
	}
}
