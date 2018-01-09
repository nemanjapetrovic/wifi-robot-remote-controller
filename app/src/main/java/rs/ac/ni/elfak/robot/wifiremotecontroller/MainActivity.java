package rs.ac.ni.elfak.robot.wifiremotecontroller;

import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;

import rs.ac.ni.elfak.robot.wifiremotecontroller.mjpg.MjpegInputStream;
import rs.ac.ni.elfak.robot.wifiremotecontroller.mjpg.MjpegView;
import rs.ac.ni.elfak.robot.wifiremotecontroller.wifirrobotapi.RobCom;


public class MainActivity extends Activity {

    //API RoboCommunication
    private RobCom comm;
    private Timer timer;
    private TimerTask task;
    private int zeljenaBrzina;
    private int pravac; //0-STOP, 1-PRAVO, 2-NAZAD, 3-LEVO, 4-DESNO
    private int brzina;
    private final static int constantRotationSpeed = 70;
    private boolean enableConstantSpeed;

    //CAMERA
    private MjpegView mv;
    private SurfaceView surfaceView;

    //UI
    private ImageButton forward, backward, left, right, onOffButton;

    //DATA
    private static boolean ROBOT_ON;
    private final static String IP_CAM = "http://192.168.1.106:8080/cameras/1";

    //SLIDER
    private SeekBar seekBar;

    //SLOW DOWN Timer
    private Timer timerSlow;
    private boolean pravacSet = false;

    //RobotData
    private Timer dataTimer;
    private TimerTask dataTask;
    private TextView batteryLevel;
    private TextView odometryL, odometryR;
    private long startOdometryL;
    private long startOdometryR;
    private Thread firstRead;

    //RecordButton
    private ImageButton recButton;
    private static boolean REC_ON = false;
    //Settings fragment
    private SettingsFragment settingsFragment;
    private ImageButton settingsImageButton;

    @Override
    protected void onPause() {
        super.onPause();
        mv.stopPlayback();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        //ui elements
        forward = (ImageButton) findViewById(R.id.BtnFoward);
        backward = (ImageButton) findViewById(R.id.BtnBackward);
        left = (ImageButton) findViewById(R.id.BtnLeft);
        right = (ImageButton) findViewById(R.id.BtnRight);
        onOffButton = (ImageButton) findViewById(R.id.OnOffButton);

        //speed bar
        seekBar = (SeekBar) findViewById(R.id.seekBar);

        //textview
        batteryLevel = (TextView) findViewById(R.id.batteryLevel);
        odometryL = (TextView) findViewById(R.id.odometryTextL);
        odometryR = (TextView) findViewById(R.id.odometryTextR);

        //camera
        surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        mv = new MjpegView(this, surfaceView);
        final Thread threadCamera = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mv.setSource(MjpegInputStream.read(IP_CAM));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        threadCamera.start();
        mv.setDisplayMode(MjpegView.SIZE_BEST_FIT);
        mv.showFps(true);

        //init
        onOffButton.setImageResource(R.drawable.off);
        ROBOT_ON = false;
        brzina = 20;
        pravac = 0;
        zeljenaBrzina = 60;
        enableConstantSpeed = true;

        //init api
        final Thread threadApi = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    comm = RobCom.getInstance(RobCom.ROBOT_IP, RobCom.DATA_PORT, RobCom.CONTROL_PORT);
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (SocketException e) {
                    e.printStackTrace();
                }
            }
        });
        threadApi.start();


        //get data from robot sensors
        //get inital values
       /* final Thread firstRead = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    comm.getSensorData();
                    startOdometryR = comm.dataR.odometry;
                    startOdometryL = comm.dataL.odometry;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });*/



                try {
                    comm.getSensorData();
                } catch (Exception e) {
                    e.printStackTrace();
                }

/*
        dataTimer = new Timer();
        dataTask = new TimerTask() {
            @Override
            public void run() {
                try {

                     comm.getSensorData();
                    //startOdometryR = comm.dataR.odometry;
                    //startOdometryL = comm.dataL.odometry;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            batteryLevel.setText(String.valueOf(comm.dataL.batLevel));
                            odometryL.setText(String.valueOf(comm.dataL.odometry.toString()));
                            odometryR.setText(String.valueOf(comm.dataR.odometry.toString()));
                            //Log.d("OL:", odometryL.getText().toString());
                        }
                    });
                }
                // catch(IOException e)
                // {
                //      e.printStackTrace();
                //  }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        dataTimer.schedule(dataTask, 1, 100);
*/

        //init recButton
        recButton = (ImageButton) findViewById(R.id.RecOnOffButton);
        settingsFragment = (SettingsFragment) getFragmentManager().findFragmentById(R.id.settings_fragment);
        settingsFragment.getView().setVisibility(View.GONE);
        settingsImageButton = (ImageButton) findViewById(R.id.settingsButton);

        //init ui listeners
        initCliclListeners();
    }


    private void initCliclListeners() {

        forward.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP && ROBOT_ON) {
                    resetImages(1);
                    SlowDown(RobCom.FORWARD);
                    final Thread t = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                comm.getSensorData();

                                //batteryLevel.setText(String.valueOf(comm.dataL.batLevel));
                                //odometryL.setText(String.valueOf(comm.dataL.odometry.toString()));
                                //odometryR.setText(String.valueOf(comm.dataR.odometry.toString()));
                                Log.d("DATAL:", comm.dataL.odometry.toString());
                                Log.d("DATAR:",comm.dataR.odometry.toString());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    t.start();
                }
                if (event.getAction() == MotionEvent.ACTION_DOWN && ROBOT_ON) {
                    if (timerSlow != null)
                        timerSlow.cancel();
                    pravac = RobCom.FORWARD;
                    setImage(1);

                }
                if (ROBOT_ON)
                    SpeedUp();
                return false;
            }
        });

        backward.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP && ROBOT_ON) {
                    resetImages(2);
                    SlowDown(RobCom.BACKWARD);
                }
                if (event.getAction() == MotionEvent.ACTION_DOWN && ROBOT_ON) {
                    if (timerSlow != null)
                        timerSlow.cancel();
                    pravac = RobCom.BACKWARD;
                    setImage(2);
                }
                if (ROBOT_ON)
                    SpeedUp();
                return false;
            }
        });

        left.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP && ROBOT_ON) {
                    resetImages(3);
                    pravac = RobCom.STOP;
                    brzina = 0;
                }
                if (event.getAction() == MotionEvent.ACTION_DOWN && ROBOT_ON) {
                    if (timerSlow != null)
                        timerSlow.cancel();
                    pravac = RobCom.LEFT;
                    setImage(3);

                    if (enableConstantSpeed)
                        brzina = constantRotationSpeed;
                    else
                        brzina = zeljenaBrzina;
                }
                return false;
            }
        });

        right.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP && ROBOT_ON) {
                    resetImages(4);
                    pravac = RobCom.STOP;
                    brzina = 0;
                }
                if (event.getAction() == MotionEvent.ACTION_DOWN && ROBOT_ON) {
                    if (timerSlow != null)
                        timerSlow.cancel();
                    setImage(4);
                    pravac = RobCom.RIGHT;

                    if (enableConstantSpeed)
                        brzina = constantRotationSpeed;
                    else
                        brzina = zeljenaBrzina;
                }
                return false;
            }
        });

        onOffButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN && ROBOT_ON)//shutting down robot
                {
                    onOffButton.setImageResource(R.drawable.off);
                    ROBOT_ON = !ROBOT_ON;
                    timer.cancel();
                } else if (event.getAction() == MotionEvent.ACTION_DOWN && !ROBOT_ON)//starting robot
                {
                    onOffButton.setImageResource(R.drawable.on);
                    ROBOT_ON = !ROBOT_ON;

                    pravac = RobCom.STOP;

                    //init task for robot movement
                    task = new TimerTask() {
                        @Override
                        public void run() {
                            try {
                                comm.drive((byte) brzina, (byte) pravac);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    };
                    timer = new Timer();
                    timer.schedule(task, 1, RobCom.SENDER_TIME);
                }
                return false;
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                zeljenaBrzina = progress + 60;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Toast.makeText(getApplicationContext(), "Speed changed to: " + zeljenaBrzina, Toast.LENGTH_SHORT).show();
            }
        });

        recButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (REC_ON == false) {
                    //start recording
                    recButton.setImageResource(R.drawable.rec_on);
                    REC_ON = true;

                } else {
                    //stop recording
                    recButton.setImageResource(R.drawable.rec_off);
                    REC_ON = false;
                }
            }
        });

        settingsImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                settingsFragment.getView().setVisibility(View.VISIBLE);
            }
        });
    }

    //Setting UI images
    private void setImage(int i) {
        switch (i) {
            case 1:
                forward.setImageResource(R.drawable.foward_on);
                break;
            case 2:
                backward.setImageResource(R.drawable.backward_on);
                break;
            case 3:
                left.setImageResource(R.drawable.left_on);
                break;
            case 4:
                right.setImageResource(R.drawable.right_on);
                break;
        }
    }

    //Reset UI images
    private void resetImages(int i) {
        switch (i) {
            case 1:
                forward.setImageResource(R.drawable.foward);
                break;
            case 2:
                backward.setImageResource(R.drawable.backward);
                break;
            case 3:
                left.setImageResource(R.drawable.left);
                break;
            case 4:
                right.setImageResource(R.drawable.right);
                break;
        }
    }

    private void SpeedUp() {
        if (zeljenaBrzina > brzina) {
            brzina += 2;
        } else {
            brzina = zeljenaBrzina;
        }
        //Log.d("BRZINA", String.valueOf(brzina));
    }


    private void SlowDown(final int p) {
        TimerTask taskSlow = new TimerTask() {
            @Override
            public void run() {
                try {
                    //Log.d("BRZINA TIMER", String.valueOf(brzina));
                    if (brzina > 0) {
                        brzina -= 1;
                        if (!pravacSet) {
                            pravac = p;
                            pravacSet = true;
                        }
                    } else {
                        brzina = 0;
                        pravac = RobCom.STOP;
                        pravacSet = false;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        timerSlow = new Timer();
        timerSlow.schedule(taskSlow, 1, 1);
    }

}
