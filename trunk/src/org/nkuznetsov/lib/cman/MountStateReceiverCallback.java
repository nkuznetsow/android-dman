package org.nkuznetsov.lib.cman;

import android.content.Context;
import android.content.Intent;

public interface MountStateReceiverCallback
{
	public void onMountStateChanged(Context context, Intent intent);
}
