package uk.me.dewi.android.batteryalarm;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class BatteryAlarm extends PreferenceActivity implements OnSharedPreferenceChangeListener{
    
    public static final String PREF_ENABLED = "prefEnabled";
    public static final String PREF_NOTIFICATION_SOUND = "prefNotificationSound";
    public static final String PREF_THRESHOLD = "prefThreshold";
    public static final String PREF_LAUNCH_ON_STARTUP = "prefLaunchOnStartup";
    public static final String PREF_DELAY_MINUTES = "prefDelayMinutes";
    public static final int SOUND_TYPE_DEFAULT = 0;
    public static final int SOUND_TYPE_SYSTEM = 1;
    public static final int SOUND_TYPE_CUSTOM = 2;
    
    public static final int DEFAULT_DELAY_MINUTES = 20;
    public static final int DEFAULT_THRESHOLD = 15;
    public static final boolean DEFAULT_LAUNCH_ON_STARTUP = true;
    public static final int DEFAULT_NOTIFICATION_SOUND_TYPE = SOUND_TYPE_DEFAULT;
    
    public static SharedPreferences mSettings;
    public static BatteryAlarmLauncher mLauncher;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    
        addPreferencesFromResource(R.layout.main);  
        
        mSettings = PreferenceManager.getDefaultSharedPreferences(this);
        mSettings.registerOnSharedPreferenceChangeListener(this);
        mLauncher = new BatteryAlarmLauncher();
        
        setDefaultsIfEmpty();
        
        if(mSettings.getBoolean(PREF_ENABLED, true)){
            if(!mLauncher.isRunning()){
                start();
            }
        }
        
        updateDelayPrefSummary(getPreferenceScreen().findPreference(PREF_DELAY_MINUTES));
        updateLauchOnStartupPrefSummary(getPreferenceScreen().findPreference(PREF_LAUNCH_ON_STARTUP));
        updateThresholdPrefSummary(getPreferenceScreen().findPreference(PREF_THRESHOLD));
        updateNotificationSoundPrefSummary(getPreferenceScreen().findPreference(PREF_NOTIFICATION_SOUND));
    }
    
    @Override
    protected void onDestroy() {
        mSettings.unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();
    }
    
    @Override
    protected void onStop(){
        super.onStop();
        System.exit(0);
    }
    
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference preference = getPreferenceScreen().findPreference(key);
        
        if(PREF_ENABLED.equals(preference.getKey())){
            if(preference.getSharedPreferences().getBoolean(preference.getKey(), false)){
                start();
            }
            else {
                stop();
            }
            
        }
        else if(PREF_DELAY_MINUTES.equals(preference.getKey())){
            updateDelayPrefSummary(preference);
            restart();
        }
        else if(PREF_LAUNCH_ON_STARTUP.equals(preference.getKey())){
            updateLauchOnStartupPrefSummary(preference);
        }
        else if(PREF_THRESHOLD.equals(preference.getKey())){
            updateThresholdPrefSummary(preference);
            restart();
        }
        else if(PREF_NOTIFICATION_SOUND.equals(preference.getKey())){
            updateNotificationSoundPrefSummary(preference);
            restart();
        }
    }

    public void stop() {
        mLauncher.stop(this);
    }

    public void start() {
        mLauncher.launch(this);
    }

    public void restart(){
        if(mLauncher.isRunning()){
            stop();
            start();
        }
    }

    public void updateThresholdPrefSummary(Preference preference) {
        preference.setSummary(getString(R.string.threshold_summary)
                                    + ' '
                                    + getThreshold()
                                    + '%');
    }


    public void updateDelayPrefSummary(Preference preference) {
        preference.setSummary(getString(R.string.delay_summary)
                                    + ' '
                                    + getDelayMinutes()
                                    + ' '
                                    + getString(R.string.minutes));
    }


    public void updateLauchOnStartupPrefSummary(Preference preference) {
        if(preference.getSharedPreferences().getBoolean(preference.getKey(), true)){
            preference.setSummary(getString(R.string.will_launch_on_startup));
        }
        else {
            preference.setSummary(getString(R.string.will_not_launch_on_startup));
        }
    }
    
    public void updateNotificationSoundPrefSummary(Preference preference) {
        String url = mSettings.getString(PREF_NOTIFICATION_SOUND, "");
        if(url.length() > 0){
            preference.setSummary("Ringtone "+url.substring(url.lastIndexOf('/')+1));
            //TODO support custom media
        }
        
    }
    
    public int getDelayMinutes(){
        return Integer.parseInt(mSettings.getString(PREF_DELAY_MINUTES, Integer.valueOf(DEFAULT_DELAY_MINUTES).toString()));
    }
    
    public int getThreshold(){
        return Integer.parseInt(mSettings.getString(PREF_THRESHOLD, Integer.valueOf(DEFAULT_THRESHOLD).toString()));
    }
    
    public void setDefaultsIfEmpty(){
        Editor editor = mSettings.edit();
        if(!mSettings.contains(PREF_ENABLED)){
            editor.putBoolean(PREF_ENABLED, false);
        }
        if(!mSettings.contains(PREF_DELAY_MINUTES)){
            editor.putString(PREF_DELAY_MINUTES, Integer.valueOf(DEFAULT_DELAY_MINUTES).toString());
        }
        if(!mSettings.contains(PREF_THRESHOLD)){
            editor.putString(PREF_THRESHOLD, Integer.valueOf(DEFAULT_THRESHOLD).toString());
        }
        if(!mSettings.contains(PREF_LAUNCH_ON_STARTUP)){
            editor.putBoolean(PREF_LAUNCH_ON_STARTUP, false);
        }
        editor.commit();
    }

}