package de.invisibletower.footnavi;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.Timer;
import java.util.TimerTask;

public class CalibrateActivity extends AppCompatActivity {

    TheFoot mFoot;
    Timer mTimer;

    // region UI elements
    TextView mQualiGyro;
    TextView mQualiAcc;
    TextView mQualiMag;
    TextView mQuat1;
    TextView mQuat2;
    TextView mQuat3;
    TextView mQuat4;
    TextView mHeading;
    TextView mState;
    ToggleButton mLed1;
    ToggleButton mLed2;
    ToggleButton mLed3;
    ToggleButton mLed4;
    CheckBox mVibActive;
    // endregion

    CompoundButton.OnCheckedChangeListener mLedHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calibrate);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        mFoot = new TheFoot(this);


        mQualiGyro = (TextView) findViewById(R.id.text_qual_gyro);
        mQualiAcc = (TextView) findViewById(R.id.text_qual_acc);
        mQualiMag = (TextView) findViewById(R.id.text_qual_mag);
        mQuat1 = (TextView) findViewById(R.id.text_quat1);
        mQuat2 = (TextView) findViewById(R.id.text_quat2);
        mQuat3 = (TextView) findViewById(R.id.text_quat3);
        mQuat4 = (TextView) findViewById(R.id.text_quat4);
        mHeading = (TextView) findViewById(R.id.text_heading);
        mState = (TextView) findViewById(R.id.text_state);

        mLed1 = (ToggleButton) findViewById(R.id.tog_led1);
        mLed2 = (ToggleButton) findViewById(R.id.tog_led2);
        mLed3 = (ToggleButton) findViewById(R.id.tog_led3);
        mLed4 = (ToggleButton) findViewById(R.id.tog_led4);
        mVibActive = (CheckBox) findViewById(R.id.check_vib);

        mLedHandler = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mFoot.setLeds(mLed1.isChecked(),
                        mLed2.isChecked(),
                        mLed3.isChecked(),
                        mLed4.isChecked());
            }
        };

        mLed1.setOnCheckedChangeListener(mLedHandler);
        mLed2.setOnCheckedChangeListener(mLedHandler);
        mLed3.setOnCheckedChangeListener(mLedHandler);
        mLed4.setOnCheckedChangeListener(mLedHandler);
        mVibActive.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mFoot.setVibrate(isChecked);
            }
        });

    }

    @Override
    protected void onPause() {
        super.onPause();
        mFoot.onPause();
        mTimer.cancel();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mFoot.onResume();
        mTimer = new Timer();
        mTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                final TheFoot.Orientation orient = mFoot.getLastOrientation();
                final String state = mFoot.getState();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (orient != null ) {
                            mQualiGyro.setText(String.valueOf(orient.qualiGyro));
                            mQualiAcc.setText(String.valueOf(orient.qualiAcc));
                            mQualiMag.setText(String.valueOf(orient.qualiMag));
                            mQuat1.setText(String.valueOf(orient.quat[0]));
                            mQuat2.setText(String.valueOf(orient.quat[1]));
                            mQuat3.setText(String.valueOf(orient.quat[2]));
                            mQuat4.setText(String.valueOf(orient.quat[3]));
                            mHeading.setText(String.valueOf(orient.heading));
                        }
                        mState.setText(state);
                    }
                });

            }
        }, 500, 500);
    }

}
