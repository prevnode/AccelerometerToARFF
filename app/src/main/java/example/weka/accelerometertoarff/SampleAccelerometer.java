package example.weka.accelerometertoarff;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.IBinder;
import android.os.Binder;
import android.app.NotificationManager;
import android.util.Log;
import android.widget.Toast;
import android.app.Notification;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.content.Context;


public class SampleAccelerometer extends Service implements SensorEventListener {
    public SampleAccelerometer() {
    }

    private static final String TAG = "ControlReading";
    private NotificationManager mNM;

    private SensorManager mSensorManager;
    private Sensor mSensor;
    private SensorEventListener mClientListener;
    private boolean mActive;

    //Used for filtering accelerometer data
    final float alpha = 0.8f;

    float[] gravity = new float[3];
    float[] linear_acceleration = new float[3];

    // Unique Identification Number for the Notification.
    // We use it on Notification start, and to cancel it.
    private int NOTIFICATION = R.string.local_service_started;

    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class LocalBinder extends Binder {
        SampleAccelerometer getService() {
            return SampleAccelerometer.this;
        }
    }

    public void registerAccelListener(SensorEventListener sensorListener){
        mClientListener = sensorListener;
    }

    private void getSensor(){
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        // Use the accelerometer.
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null){
            mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            mSensorManager.registerListener(this,mSensor,SensorManager.SENSOR_DELAY_FASTEST);
        }
        else{
            Log.e(TAG,"Cant find accel");
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy){

    }

    @Override
    public void onSensorChanged(SensorEvent event){

        if(!mActive)
            return;

        // In this example, alpha is calculated as t / (t + dT),
        // where t is the low-pass filter's time-constant and
        // dT is the event delivery rate.

        // Isolate the force of gravity with the low-pass filter.
        gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
        gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
        gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

        // Remove the gravity contribution with the high-pass filter.
        linear_acceleration[0] = event.values[0] - gravity[0];
        linear_acceleration[1] = event.values[1] - gravity[1];
        linear_acceleration[2] = event.values[2] - gravity[2];

        //Replace the original values with the cleaned up ones and pass them on
        event.values[0] = linear_acceleration[0];
        event.values[1] = linear_acceleration[1];
        event.values[2] = linear_acceleration[2];

        if(mClientListener != null)
            mClientListener.onSensorChanged(event);
    }

    public float[] getAccel(){
        return linear_acceleration;
    }

    public void setActive(boolean active){
        mActive = active;
    }


    @Override
    public void onCreate() {
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        // Display a notification about us starting.  We put an icon in the status bar.
        showNotification();
        getSensor();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("LocalService", "Received start id " + startId + ": " + intent);





        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        // Cancel the persistent notification.
        mNM.cancel(NOTIFICATION);

        // Tell the user we stopped.
        Toast.makeText(this, R.string.local_service_stopped, Toast.LENGTH_SHORT).show();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    // This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
    private final IBinder mBinder = new LocalBinder();

    /**
     * Show a notification while this service is running.
     */
    private void showNotification() {
        // In this sample, we'll use the same text for the ticker and the expanded notification
        CharSequence text = getText(R.string.local_service_started);

        // Set the icon, scrolling text and timestamp
        Notification notification = new Notification(R.drawable.ic_launcher, text,
                System.currentTimeMillis());

        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, ControlReading.class), 0);

        // Set the info for the views that show in the notification panel.
        notification.setLatestEventInfo(this, getText(R.string.local_service_label),
                text, contentIntent);

        // Send the notification.
        mNM.notify(NOTIFICATION, notification);
    }
}
