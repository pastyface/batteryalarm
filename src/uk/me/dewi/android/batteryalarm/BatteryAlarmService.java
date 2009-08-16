package uk.me.dewi.android.batteryalarm;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.preference.PreferenceManager;

 /**
  * This application service will run in response to an alarm, 
  * allowing us to move long duration work out of an intent receiver.
  * 
  * @see AlarmService
  * @see AlarmService_Alarm
  */
public class BatteryAlarmService extends Service {
     
    NotificationManager mNM;

    int mThreshold = 15;
    Uri mSoundUri;
     
    boolean mReceivedBatteryStatus = false;

    @Override
    public void onCreate() {
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        
        mThreshold = Integer.parseInt(settings.getString(BatteryAlarm.PREF_THRESHOLD, 
                                      Integer.valueOf(BatteryAlarm.DEFAULT_THRESHOLD)
                                      .toString()));
        
        if(settings.contains(BatteryAlarm.PREF_NOTIFICATION_SOUND)){
            mSoundUri = Uri.parse(settings.getString(BatteryAlarm.PREF_NOTIFICATION_SOUND, ""));
        }
        
        // Start up the thread running the service.
        Thread thr = new Thread(null, mTask, "BatteryAlarmService");
        thr.start();
    }

    @Override
    public void onDestroy() {
        

    }

    /**
     * The function that runs in our worker thread
     */
    Runnable mTask = new Runnable() {
        public void run() {
            mReceivedBatteryStatus = false;
             
            // read battery status
            registerReceiver(mBatInfoReceiver,
                             new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        }
    };
     
    BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            if(!mReceivedBatteryStatus){
                mReceivedBatteryStatus = true;
                int level = intent.getIntExtra("level", 0);
                
                if(level < mThreshold){
                    showNotification("Battery down to "+String.valueOf(level) + "%");
                }
                else{
                  mNM.cancel(R.string.alarm_service_started);
                }
            }
            BatteryAlarmService.this.stopSelf();
            unregisterReceiver(mBatInfoReceiver);
        }
    };


    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    /**
     * Show a notification while this service is running.
     */
    private void showNotification(CharSequence text) {

        // Set the icon, scrolling text and timestamp
        Notification notification = new Notification(R.drawable.status_icon, 
                                                     text,
                                                     System.currentTimeMillis());
         
        Intent intent = new Intent(this, BatteryAlarm.class);
        
        if(mSoundUri == null){
            notification.defaults = Notification.DEFAULT_SOUND;
        }
        else {
            notification.sound = mSoundUri;
        }

        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, 0);

        // Set the info for the views that show in the notification panel.
        notification.setLatestEventInfo(this, 
                                        getText(R.string.app_name),
                                        text, 
                                        contentIntent);

        // Send the notification.
        // We use a layout id because it is a unique number.  We use it later to cancel.
        mNM.notify(R.string.alarm_service_started, notification);
    }

    /**
     * This is the object that receives interactions from clients.
     */
    private final IBinder mBinder = new Binder() {
        @Override
        protected boolean onTransact(int code, Parcel data, Parcel reply,
                int flags) throws RemoteException {
            return super.onTransact(code, data, reply, flags);
        }
    };
 
}
