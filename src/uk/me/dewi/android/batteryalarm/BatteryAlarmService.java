package uk.me.dewi.android.batteryalarm;

import java.util.Calendar;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;

 /**
  * This application service will run in response to an alarm, 
  * allowing us to move long duration work out of an intent receiver.
  * 
  * @see AlarmService
  * @see AlarmService_Alarm
  */
public class BatteryAlarmService extends Service {
    public static final long PLAYBACK_DURATION = 5000;
     
    NotificationManager mNM;

    int mThreshold = BatteryAlarm.DEFAULT_THRESHOLD;
    Uri mSoundUri;
     
    boolean mReceivedBatteryStatus = false;

    @Override
    public void onCreate() {
        super.onCreate();
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        
        if(!isEnabledTime(settings)){
            return;
        }
        
        try{
            mThreshold = Integer.parseInt(settings.getString(BatteryAlarm.PREF_THRESHOLD, 
                                          Integer.valueOf(BatteryAlarm.DEFAULT_THRESHOLD)
                                          .toString()));
        }
        catch(Exception e){
            Log.e(getClass().getName(), "Could not parse threshold", e);
        }
        
        if(settings.contains(BatteryAlarm.PREF_NOTIFICATION_SOUND)){
            String sound = settings.getString(BatteryAlarm.PREF_NOTIFICATION_SOUND, "");
            if(sound.length() > 0){
                mSoundUri = Uri.parse(sound);
            }
        }
        
        // Start up the thread running the service.
        Thread thr = new Thread(null, mTask, "BatteryAlarmService");
        thr.start();
    }

    @Override
    public void onDestroy() {
        
        super.onDestroy();
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
    
    Runnable mPlayerTask = new Runnable() {
        public void run() {
         // Make a noise
            playCustomSound();
        }
    };
     
    BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            try{
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
            }
            finally {
                unregisterReceiver(mBatInfoReceiver);
                BatteryAlarmService.this.stopSelf();
            }
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
        
        // Start up the player thread
        Thread thr = new Thread(null, mPlayerTask, "BatteryAlarmPlayer");
        thr.start();
            
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

    private void playCustomSound() {
        MediaPlayer mediaPlayer = null;
        try{
            mediaPlayer = MediaPlayer.create(this, mSoundUri);
            mediaPlayer.setVolume(1, 1);
            mediaPlayer.start();
            
            try{
                Thread.sleep(PLAYBACK_DURATION);
            }catch(InterruptedException ignored){}
            
            
        }
        catch (Exception e){
            Log.e(getClass().getName(), "Could not play Uri "+mSoundUri + " - " + e);
            
            // Play the default sound instead
            playDefaultSound();
        }
        finally {
            try{
                if(mediaPlayer != null) mediaPlayer.release();
            }
            catch(Exception ignored){}
        }
    }

    private void playDefaultSound() {
        MediaPlayer mediaPlayer = null;
        try {
            mediaPlayer = new MediaPlayer();
            AssetFileDescriptor fd = getResources().openRawResourceFd(R.raw.record_stop1);
            mediaPlayer.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getLength());
            fd.close();
            mediaPlayer.setVolume(1, 1);
            
            mediaPlayer.prepare();
            mediaPlayer.start();
            
            try{
                Thread.sleep(PLAYBACK_DURATION);
            }catch(InterruptedException ignored){}
            
        } catch (Exception e) {
            Log.e(getClass().getName(), "Could not play raw sound "+R.raw.record_stop1 + " - " +e);
        }
        finally{
            try{
                if(mediaPlayer != null) mediaPlayer.release();
            }
            catch(Exception ignored){}
        }
    }
    
    private boolean isEnabledTime(SharedPreferences settings) {
        boolean disableAtNight = settings.getBoolean(BatteryAlarm.PREF_DISABLE_AT_NIGHT, 
                                                     BatteryAlarm.DEFAULT_DISABLE_AT_NIGHT);
        if(!disableAtNight){
            return true;
        }
        
        String minTimeStr = settings.getString(BatteryAlarm.PREF_MIN_TIME, BatteryAlarm.DEFAULT_MIN_TIME);
        String maxTimeStr = settings.getString(BatteryAlarm.PREF_MAX_TIME, BatteryAlarm.DEFAULT_MAX_TIME);
        
        int minHrs = BatteryAlarm.parseHours(minTimeStr);
        int minMins = BatteryAlarm.parseMinutes(minTimeStr);
        int maxHrs = BatteryAlarm.parseHours(maxTimeStr);
        int maxMins = BatteryAlarm.parseMinutes(maxTimeStr);
        
        Calendar now = Calendar.getInstance();
        Calendar min = Calendar.getInstance();
        Calendar max = Calendar.getInstance();
        
        if(max.before(min)){
            max.add(Calendar.DATE, 1);
        }
        
        min.set(Calendar.HOUR_OF_DAY, minHrs);
        min.set(Calendar.MINUTE, minMins);
        if(now.before(min)) return false;
        
        max.set(Calendar.HOUR_OF_DAY, maxHrs);
        max.set(Calendar.MINUTE, maxMins);
        if(now.after(max)) return false;
        
        return true;
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
