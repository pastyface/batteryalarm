package uk.me.dewi.android.batteryalarm;

import static uk.me.dewi.android.batteryalarm.BatteryAlarm.*;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Launches the battery alarm on system startup
 * @author dewi
 *
 */
public class BatteryAlarmStartup extends BroadcastReceiver {

    static final String ACTION = "android.intent.action.BOOT_COMPLETED";
    
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(ACTION)) {
            
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
            
            if(settings.getBoolean(BatteryAlarm.PREF_LAUNCH_ON_STARTUP, DEFAULT_LAUNCH_ON_STARTUP)){
                BatteryAlarmLauncher launcher = new BatteryAlarmLauncher();
                launcher.launch(context);
            }
        }
    }
}