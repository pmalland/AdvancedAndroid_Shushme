package com.example.android.shushme;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import static android.content.Context.NOTIFICATION_SERVICE;

/**
 * Created by paulm on 13-07-17.
 */

public class GeofenceBroadcastReceiver extends BroadcastReceiver {

    public static final String TAG = GeofenceBroadcastReceiver.class.getSimpleName();

    /***
    * Handles the Broadcast message sent when the Geofence Transition is triggered
    * Careful here though, this is running on the main thread so make sure you start an AsyncTask for
    * anything that takes longer than say 10 second to run
    *
    * @param context
    * @param intent
    */
    @Override
    public void onReceive(Context context, Intent intent) {
//        Log.i(TAG, "onReceive called");

        // Get the Geofence Event from the Intent sent through
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);

        if (geofencingEvent.hasError()) {
            Log.e(TAG, String.format("Error occured: %d", geofencingEvent.getErrorCode()));
            return;
        }

        // Get the transition type.

        int geofenceTransition = geofencingEvent.getGeofenceTransition();
        // Check which transition type has triggered this event
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            setRingerMode(context, AudioManager.RINGER_MODE_SILENT);
        } else if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            setRingerMode(context, AudioManager.RINGER_MODE_NORMAL);
        } else {
            // Log the error.
            Log.e(TAG, String.format("Unknown transition : %d", geofenceTransition));
            // No need to do anything else
            return;
        }

        sendNotification(context,geofenceTransition);
    }

    private void setRingerMode(Context context, int mode){

        NotificationManager nm =
                (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);

        /**
         * Changes the ringer mode on the device to either silent or back to normal
         *
         * @param context The context to access AUDIO_SERVICE
         * @param mode    The desired mode to switch device to, can be AudioManager.RINGER_MODE_SILENT or
         *                AudioManager.RINGER_MODE_NORMAL
         */
        if (Build.VERSION.SDK_INT <24 ||
                Build.VERSION.SDK_INT >=24 && !nm.isNotificationPolicyAccessGranted()){
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            audioManager.setRingerMode(mode);
        }
    }

    /**
     * Posts a notification in the notification bar when a transition is detected
     * Uses different icon drawables for different transition types
     * If the user clicks the notification, control goes to the MainActivity
     *
     * @param context        The calling context for building a task stack
     * @param transitionType The geofence transition type, can be Geofence.GEOFENCE_TRANSITION_ENTER
     *                       or Geofence.GEOFENCE_TRANSITION_EXIT
     */
    private void sendNotification(Context context, int transitionType) {
        //Building the pendingIntent for the notification
        // Create an explicit content Intent that starts the main Activity.
        Intent notificationIntent = new Intent(context,MainActivity.class);

        // Construct a task stack.
        TaskStackBuilder taskStackBuilder = TaskStackBuilder.create(context);

        // Add the main Activity to the task stack as the parent.
        taskStackBuilder.addParentStack(MainActivity.class);

        // Push the content Intent onto the stack.
        taskStackBuilder.addNextIntent(notificationIntent);

        // Get a PendingIntent containing the entire back stack.
        PendingIntent notificationPendingIntent =
                taskStackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        // Now let's craft a notification

        // Get a notification builder
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);

        // Check the transition type to display the relevant icon image
        if( transitionType == Geofence.GEOFENCE_TRANSITION_ENTER){
            builder.setSmallIcon(R.drawable.ic_volume_off_white_24dp)
                    .setLargeIcon(BitmapFactory.
                            decodeResource(context.getResources(),
                                    R.drawable.ic_volume_off_white_24dp))
                    .setContentTitle(context.getString(R.string.silent_mode_activated));

        } else  if (transitionType == Geofence.GEOFENCE_TRANSITION_EXIT){
            builder.setSmallIcon(R.drawable.ic_volume_up_white_24dp)
                    .setLargeIcon(BitmapFactory.
                            decodeResource(context.getResources(),
                                    R.drawable.ic_volume_up_white_24dp))
                    .setContentTitle(context.getString(R.string.back_to_normal));
        }
        // Continue building the notification
        builder.setContentText(context.getString(R.string.touch_to_relaunch));
        builder.setContentIntent(notificationPendingIntent);

        // Dismiss the notification once the user touch it
        builder.setAutoCancel(true);

        // Get an instance of the Notification manager

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(0, builder.build());
    }

}
