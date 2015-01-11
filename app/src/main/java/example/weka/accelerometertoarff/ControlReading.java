package example.weka.accelerometertoarff;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.MenuItem;
import android.widget.Button;
import android.content.ServiceConnection;
import android.widget.TextView;
import android.widget.Toast;


public class ControlReading extends ActionBarActivity {

    private boolean recording;

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
     * Start/Stop recording accelerometer readings
     */
    public void toggleRecord(View view){

        if(mButton == null){
            Log.d(TAG,"no button ref");
            return;
        }


        if(recording)
            mButton.setText("Start");
        else {
            mButton.setText("Stop");
            if(mBoundService != null){
                float[] accel = mBoundService.getAccel();
                mAccelText.append("x: " + accel[0] + " y: " + accel[1] + " z: " + accel[2]);
            }
        }

        recording = !recording;



    }

    private SampleAccelerometer mBoundService;

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  Because we have bound to a explicit
            // service that we know is running in our own process, we can
            // cast its IBinder to a concrete class and directly access it.
            mBoundService = ((SampleAccelerometer.LocalBinder)service).getService();

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
