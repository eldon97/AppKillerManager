package com.thelittlefireman.appkillermanager.managers;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.thelittlefireman.appkillermanager.BuildConfig;
import com.thelittlefireman.appkillermanager.devices.DeviceBase;
import com.thelittlefireman.appkillermanager.models.KillerManagerActionType;
import com.thelittlefireman.appkillermanager.utils.ActionUtils;
import com.thelittlefireman.appkillermanager.utils.LogUtils;
import com.thelittlefireman.appkillermanager.utils.SystemUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class KillerManager {
    private static final int REQUEST_CODE_KILLER_MANAGER_ACTION = 52000;

    private KillerManager(Context context) {
        // log error into a distant request bin logs for helps to debug
        // please do no change the adress
        /*HyperLog.initialize(context);
        HyperLog.setLogLevel(Log.VERBOSE);
        HyperLog.setURL("API URL");*/
        mDevice = DevicesManager.getDevice();
        mCurrentNbActions = new HashMap<>();
        mCurrentNbActions.put(KillerManagerActionType.ACTION_AUTOSTART, 0);
        mCurrentNbActions.put(KillerManagerActionType.ACTION_NOTIFICATIONS, 0);
        mCurrentNbActions.put(KillerManagerActionType.ACTION_POWERSAVING, 0);
    }

    private static KillerManager sKillerManager;

    public static synchronized KillerManager getInstance(Context context) {
        if (sKillerManager == null) {
            sKillerManager = new KillerManager(context);
        }
        return sKillerManager;
    }

    private DeviceBase mDevice;

    private HashMap<KillerManagerActionType, Integer> mCurrentNbActions;

    public DeviceBase getDevice() {
        return mDevice;
    }

    public boolean isActionAvailable(Context context, KillerManagerActionType killerManagerActionType) {
        mDevice = DevicesManager.getDevice();
        boolean actionAvailable = false;
        if (mDevice != null) {
            switch (killerManagerActionType) {
                case ACTION_AUTOSTART:
                    actionAvailable = mDevice.isActionAutoStartAvailable(context);
                    break;
                case ACTION_POWERSAVING:
                    actionAvailable = mDevice.isActionPowerSavingAvailable(context);
                    break;
                case ACTION_NOTIFICATIONS:
                    actionAvailable = mDevice.isActionNotificationAvailable(context);
                    break;
                case ACTION_EMPTY:
                    actionAvailable = false;
            }
        }
        return actionAvailable;
    }

    /**
     * Return the intent for a specific killerManagerActionType
     *
     * @param context             the current context
     * @param killerManagerActionType the wanted killerManagerActionType
     * @return the intent
     */
    private List<Intent> getIntentFromAction(Context context, KillerManagerActionType killerManagerActionType) {
        mDevice = DevicesManager.getDevice();
        if (mDevice != null) {
            List<Intent> intentList = null;
            switch (killerManagerActionType) {
                case ACTION_AUTOSTART:
                    intentList = mDevice.getActionAutoStart(context).getIntentActionList();
                    break;
                case ACTION_POWERSAVING:
                    intentList = mDevice.getActionPowerSaving(context).getIntentActionList();
                    break;
                case ACTION_NOTIFICATIONS:
                    intentList = mDevice.getActionNotification(context).getIntentActionList();
                    break;
                case ACTION_EMPTY:
                    intentList = Collections.emptyList();
            }
            if (intentList != null && !intentList.isEmpty() && ActionUtils.isAtLeastOneIntentAvailable(context, intentList)) {
                // Intent found killerManagerActionType succeed
                return intentList;
            } else {
                LogUtils.e(KillerManager.class.getName(), "INTENT NOT FOUND :" +
                        ActionUtils.getExtrasDebugInformations(intentList) +
                        "LibraryVersionName :" + BuildConfig.VERSION_NAME +
                        "LibraryVersionCode :" + BuildConfig.VERSION_CODE +
                        "KillerManagerActionType \n" + killerManagerActionType.name() + "SYSTEM UTILS \n" +
                        SystemUtils.getDefaultDebugInformation() + "DEVICE \n" +
                        mDevice.getExtraDebugInformations(context));
                // Intent not found killerManagerActionType failed
                return Collections.emptyList();
            }
        } else {
            // device not found killerManagerActionType failed
            return Collections.emptyList();
               /* LogUtils.e(KillerManager.class.getName(), "DEVICE NOT FOUND" + "SYSTEM UTILS \n" +
                        SystemUtils.getDefaultDebugInformation());*/
        }
    }

    /**
     * Execute the killerManagerActionType
     *
     * @param activity            the current activity
     * @param killerManagerActionType the wanted killerManagerActionType to execute
     * @return true : killerManagerActionType succeed; false killerManagerActionType failed
     */
    public boolean doAction(Activity activity, KillerManagerActionType killerManagerActionType) {
        return doAction(activity, killerManagerActionType, mCurrentNbActions.get(killerManagerActionType));
    }

    /**
     * Execute the killerManagerActionType
     *
     * @param activity            the current activity
     * @param killerManagerActionType the wanted killerManagerActionType to execute
     * @return true : killerManagerActionType succeed; false killerManagerActionType failed
     */
    private boolean doAction(Activity activity, KillerManagerActionType killerManagerActionType, int index) {
        // Avoid main app to crash when intent denied by using try catch
        try {
            List<Intent> intentList = getIntentFromAction(activity, killerManagerActionType);
            if (intentList != null && !intentList.isEmpty() && intentList.size() > index) {
                Intent intent = intentList.get(index);
                if (ActionUtils.isIntentAvailable(activity, intent)) {
                    activity.startActivityForResult(intent, REQUEST_CODE_KILLER_MANAGER_ACTION);
                    // Intent found killerManagerActionType succeed
                    return true;
                }
            }

        } catch (Exception e) {
            // Exception handle killerManagerActionType failed
            LogUtils.e(KillerManager.class.getName(), e.getMessage());
            return false;
        }
        return false;
    }

    public void onActivityResult(Activity activity, KillerManagerActionType killerManagerActionType, int requestCode) {
        if (requestCode == REQUEST_CODE_KILLER_MANAGER_ACTION) {
            int value = mCurrentNbActions.get(killerManagerActionType);
            value++;
            mCurrentNbActions.put(killerManagerActionType, value);

            List<Intent> intentList = getIntentFromAction(activity, killerManagerActionType);
            if (intentList != null && !intentList.isEmpty() && intentList.size() > mCurrentNbActions.get(
                    killerManagerActionType)) {
                doAction(activity, killerManagerActionType, mCurrentNbActions.get(killerManagerActionType));
            } else {
                // reset if no more intent
                mCurrentNbActions.put(killerManagerActionType, 0);
            }
        }
    }

    /*private void onActivityResultForDialog(Activity activity, KillerManagerActionType killerManagerAction, int requestCode) {
        if (requestCode == REQUEST_CODE_KILLER_MANAGER_ACTION) {
            int value = mCurrentNbActions.get(killerManagerAction);
            value++;
            mCurrentNbActions.put(killerManagerAction, value);
            List<Intent> intentList = getIntentFromAction(activity, killerManagerAction);
            if (intentList == null || intentList.isEmpty() || intentList.size() <= mCurrentNbActions.get(killerManagerAction)) {
                // reset if no more intent
                mCurrentNbActions.put(killerManagerAction, 0);
            }
        }
    }*/
}
