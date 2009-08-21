package uk.me.dewi.android.batteryalarm;

import static uk.me.dewi.android.batteryalarm.BatteryAlarm.DEFAULT_DELAY_MINUTES;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

public class BatteryAlarmLauncher {

    public static PendingIntent mAlarmSender;
    
    private boolean mRunning = false;
    
    public void launch(Context context){
        
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        
        int delayMinutes = BatteryAlarm.DEFAULT_DELAY_MINUTES;
        try {
            Integer.valueOf(settings.getString(BatteryAlarm.PREF_DELAY_MINUTES,
                Integer.valueOf(DEFAULT_DELAY_MINUTES).toString()));
        }
        catch(NumberFormatException e){
            Log.e(getClass().getName(), "Could not parse delay", e);
        }
 
        mAlarmSender = PendingIntent.getService(context,
                0, new Intent(context, BatteryAlarmService.class), 0); 
        
        long firstTime = SystemClock.elapsedRealtime();
        
        AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        
        am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        firstTime, 
                        delayMinutes*60*1000, 
                        mAlarmSender);
        mRunning = true;
        
        Toast.makeText(context, 
                R.string.monitoring_battery,
                Toast.LENGTH_LONG).show();
    }

    public void stop(Context context) {
        AlarmManager am = (AlarmManager)context.getSystemService(BatteryAlarm.ALARM_SERVICE);
        am.cancel(BatteryAlarmLauncher.mAlarmSender);
        mRunning = false;
    }
    
    public boolean isRunning(){
        return this.mRunning;
    }
}
