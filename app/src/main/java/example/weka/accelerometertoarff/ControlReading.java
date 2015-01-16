package example.weka.accelerometertoarff;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.UserHandle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.View;
import android.view.MenuItem;
import android.widget.Button;
import android.content.ServiceConnection;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.LinkedList;


public class ControlReading extends ActionBarActivity {

    private boolean mRecording;

    private Button mButton;
    private TextView mAccelText;
    private boolean mIsBound;
    private static final String TAG = "ControlReading";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control_reading);
        mButton =(Button)findViewById(R.id.toggleButton);
        mButton.setText("Start");
        mAccelText = (TextView)findViewById(R.id.accelView);

        doBindService();

    }

    /* Checks if external storage is available for read and write */
    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    public File getDocumentsDir(String dataDirName) {
        // Get the directory for the user's public pictures directory.
        File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS), dataDirName);
        if(file.exists()) {
            Log.e(TAG, "Directory exists");
            return file;
        }

        if (!file.mkdirs()) {
            Log.e(TAG, "Directory not created");
        }
        return file;
    }

    private void scanDataFile(File file) {
        MediaScannerConnection.scanFile(this,
                new String[]{file.toString()}, null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, Uri uri) {
                        Log.i("ExternalStorage", "Scanned " + path + ":");
                        Log.i("ExternalStorage", "-> uri=" + uri);
                    }
                });
    }

    private void writeHeader(){
        if(!isExternalStorageWritable()){
            Log.e(TAG, "External Storage unavailable");
            return;
        }

        try {
            FileWriter outputStream;
            String testString = "HelloWorld";

            File dir = getDocumentsDir("arff");
            File file = new File(dir,"accel.arff");
            if( file.exists() ){
                Log.d(TAG, "accel exists");
            }
            else{
                file.createNewFile();
                Log.d(TAG, "accel created");
            }
            outputStream =  new FileWriter(file); //openFileOutput("accelARFF", Context.MODE_WORLD_READABLE);
            if(outputStream == null)
                throw new IOException("wtf");


            outputStream.write(getString(R.string.arff_header));
            Log.d(TAG, "writing:" + testString);
            outputStream.close();
            scanDataFile(file);
            Toast.makeText(getBaseContext(),"file saved",
                    Toast.LENGTH_SHORT).show();

        }catch (Exception e){
            Log.e(TAG, e.toString() );
            return;
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_control_reading, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Start/Stop mRecording accelerometer readings
     */
    public void toggleRecord(View view){

        if(mButton == null){
            Log.d(TAG,"no button ref");
            return;
        }

        if(mRecording) {
            mButton.setText("Start");
            writeHeader();
        }
        else
            mButton.setText("Stop");

        mRecording = !mRecording;
        mBoundService.setActive(mRecording);
    }

    private SampleBatteryService mBoundService;

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  Because we have bound to a explicit
            // service that we know is running in our own process, we can
            // cast its IBinder to a concrete class and directly access it.
            mBoundService = ((SampleBatteryService.LocalBinder)service).getService();

            // Tell the user about this for our demo.
            Toast.makeText(ControlReading.this, R.string.local_service_connected,
                    Toast.LENGTH_SHORT).show();
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // Because it is running in our same process, we should never
            // see this happen.
            mBoundService = null;
            Toast.makeText(ControlReading.this, R.string.local_service_disconnected,
                    Toast.LENGTH_SHORT).show();
        }

    };

    void doBindService() {
        // Establish a connection with the service.  We use an explicit
        // class name because we want a specific service implementation that
        // we know will be running in our own process (and thus won't be
        // supporting component replacement by other applications).
        bindService(new Intent(ControlReading.this,
                SampleAccelerometer.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }

    void doUnbindService() {
        if (mIsBound) {
            // Detach our existing connection.
            unbindService(mConnection);
            mIsBound = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        doUnbindService();
    }
}
