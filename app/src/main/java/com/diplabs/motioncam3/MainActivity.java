package com.diplabs.motioncam3;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.DoubleSummaryStatistics;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2, PopupMenu.OnMenuItemClickListener , Settings.SettingDialogListener{

    private static final String TAG = "MotionCam3";
    private static final int MY_CAMERA_REQUEST_CODE = 100;
    private final int activeCamera = CameraBridgeViewBase.CAMERA_ID_BACK;
    private CustomCameraView javaCameraView;// JavaCameraView javaCameraView;
    private LineChart chart;
    private BaseLoaderCallback baseLoaderCallback;

    private ImageButton imageButtonStopPlayCamera;
    private ImageButton imageButtonStopPlayGraph;
    private boolean cameraOn = true;
    private boolean graphOn = true;
    private int filterColorType = 3;
    private Settings settings;
    private int framesToAnalize = 64;
    private double minValueOnChart = 1.0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        javaCameraView = (CustomCameraView) findViewById(R.id.cameraView1);//javaCameraView = (JavaCameraView) findViewById(R.id.cameraView1);
        chart = (LineChart) findViewById(R.id.chart);


        imageButtonStopPlayCamera = findViewById(R.id.imageButtonStopPlayCamView);
        imageButtonStopPlayGraph = findViewById(R.id.imageButtonStopPlayGraph);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Permissions granted");
            initializeCamera(javaCameraView, activeCamera);
        } else {
            // prompt system dialog
            Log.d(TAG, "Permission prompt");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, MY_CAMERA_REQUEST_CODE);
        }


        baseLoaderCallback = new BaseLoaderCallback(this) {
            @Override
            public void onManagerConnected(int status) {
                super.onManagerConnected(status);

                switch (status) {

                    case BaseLoaderCallback.SUCCESS:
                        javaCameraView.enableView();
                        break;
                    default:
                        super.onManagerConnected(status);
                        break;
                }


            }

        };


    }

    static {
        System.loadLibrary("opencv_java4");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_CAMERA_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // camera can be turned on
                Toast.makeText(this, "camera permission granted", Toast.LENGTH_LONG).show();
                initializeCamera(javaCameraView, activeCamera);
            } else {
                // camera will stay off
                Toast.makeText(this, "camera permission denied", Toast.LENGTH_LONG).show();
            }
        }
    }

    public void stopPlayCam(View view) {

        if (cameraOn) {
            cameraOn = false;
            imageButtonStopPlayCamera.setImageResource(R.drawable.ic_baseline_play_circle_outline_24);
        } else {
            cameraOn = true;
            imageButtonStopPlayCamera.setImageResource(R.drawable.ic_baseline_pause_circle_outline_24);
        }

    }

    public void stopPlayGraph(View view) {
        if (graphOn) {
            graphOn = false;
            imageButtonStopPlayGraph.setImageResource(R.drawable.ic_baseline_play_circle_outline_24);
        } else {
            graphOn = true;
            imageButtonStopPlayGraph.setImageResource(R.drawable.ic_baseline_pause_circle_outline_24);
        }

    }

    public void showMenuColorFilter(View view) {
        PopupMenu popupMenu = new PopupMenu(this, view);
        popupMenu.setOnMenuItemClickListener(this);
        popupMenu.inflate(R.menu.mwnu);
        popupMenu.show();
    }


    private void initializeCamera(CustomCameraView javaCameraView, int activeCamera) {


        javaCameraView.setCameraPermissionGranted();
        javaCameraView.setCameraIndex(activeCamera);
        javaCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
        javaCameraView.setCvCameraViewListener(this);
        javaCameraView.enableFpsMeter();

    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat();
        startListening();
    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
        thread.interrupt();
    }

    Mat mRgba;

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();


        ArrayList<Mat> dst = new ArrayList<>(3);

        if (filterColorType == 3) {
            grayScaleMod(mRgba).copyTo(mRgba);
        } else {
            Core.split(mRgba, dst);

        }


        switch (filterColorType) {
            case 0:

                if (graphOn) {
                    pushValueToQueue(dst.get(0).get(mRgba.rows() / 2, mRgba.cols() / 2)[0]);
                }
                dst.set(1, Mat.zeros(mRgba.size(), dst.get(1).type()));
                dst.set(2, Mat.zeros(mRgba.size(), dst.get(2).type()));

                Core.merge(dst, mRgba);

                break;
            case 1:


                if (graphOn) {
                    pushValueToQueue(dst.get(1).get(mRgba.rows() / 2, mRgba.cols() / 2)[0]);
                }
                dst.set(0, Mat.zeros(mRgba.size(), dst.get(0).type()));
                dst.set(2, Mat.zeros(mRgba.size(), dst.get(2).type()));

                Core.merge(dst, mRgba);


                break;
            case 2:

                if (graphOn) {
                    pushValueToQueue(dst.get(2).get(mRgba.rows() / 2, mRgba.cols() / 2)[0]);
                }
                dst.set(0, Mat.zeros(mRgba.size(), dst.get(0).type()));
                dst.set(1, Mat.zeros(mRgba.size(), dst.get(1).type()));

                Core.merge(dst, mRgba);

                break;
            case 3:
                //Linear luminance. Can be change to any other grayscale coversion
//                    pushValueToQueue(
//                            (0.2126 * mRgba.get(mRgba.rows() / 2, mRgba.cols() / 2)[0]) +
//                                    (0.7152 * mRgba.get(mRgba.rows() / 2, mRgba.cols() / 2)[1]) +
//                                    (0.0722 * mRgba.get(mRgba.rows() / 2, mRgba.cols() / 2)[2])
//
//                    );

                if (graphOn) {
                    pushValueToQueue(mRgba.get(mRgba.rows() / 2, mRgba.cols() / 2)[0]);
                }

                break;
            default:

                if (graphOn) {
                    pushValueToQueue(mRgba.get(mRgba.rows() / 2, mRgba.cols() / 2)[0]);
                }

                break;


        }


        if (cameraOn) {
            return mRgba;
        } else {
            return null;
        }

    }

    Queue<Double> valuesToAnalize = new LinkedList<Double>();
    Queue<Double> FPStoAnalize = new LinkedList<Double>();

    /**
     * Pushes values to thread that analizes
     * @param value
     */
    public void pushValueToQueue(double value) {
        synchronized (monitor) {

            valuesToAnalize.add(value);
            FPStoAnalize.add(javaCameraView.mFpsMeter.getFPS());
            monitor.notifyAll();

        }
    }

    private final Object monitor = new Object();
    private Thread thread;


    //Thread that listens to values that are being pushed
    public void startListening() {
        // background thread start
        thread = new Thread(() -> {

            try {
                analizeThread();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }


        });
        thread.start();

    }



    /**
     * Thread waits for certaing numer of values, then analizes them , creates a chart and pushes them to view. Then it cleans it self and wait for another bunch
     * @throws InterruptedException
     */
    private void analizeThread() throws InterruptedException {

        while (!thread.isInterrupted()) {

            synchronized (monitor) {
                while (valuesToAnalize.size() < framesToAnalize) {
                    monitor.wait();
                }

                double[] test = new double[framesToAnalize];
                double[] fpsArray = new double[framesToAnalize];


                for (int i = 0; i < framesToAnalize; i++) {
                    test[i] = valuesToAnalize.poll();
                }

                for (int i = 0; i < framesToAnalize; i++) {
                    fpsArray[i] = FPStoAnalize.poll();
                }



                //https://stackoverflow.com/questions/1484347/finding-the-max-min-value-in-an-array-of-primitives-using-java
                DoubleSummaryStatistics statFps = Arrays.stream(fpsArray).summaryStatistics();
                Double averFps = statFps.getAverage();

                Phasor[] testDFT = singlalTransform(test, averFps); // do pobrania fps;


                double[] ampParcel = new double[testDFT.length / 2];

                double[] freqParcel = new double[testDFT.length / 2];




                for (int i = 0; i < testDFT.length / 2; i++) {
                    //   Log.i(TAG, "run: " + testDFT[i].toString2());
                    // freqList[i]=testDFT[i].getAmp();

                    ampParcel[i] = testDFT[i].getAmp();
                    freqParcel[i] = testDFT[i].getFreq();

                }
                //show on chart only those values that are above % of max value
                //https://stackoverflow.com/questions/1484347/finding-the-max-min-value-in-an-array-of-primitives-using-java
                DoubleSummaryStatistics statAmp = Arrays.stream(ampParcel).summaryStatistics();
                 Double maxAmp = statAmp.getMax();


                //pushing to chart...
                List<Entry> entries = new ArrayList<Entry>();
                for (int i = 5; i < ampParcel.length; i++) {

                    if (ampParcel[i] < maxAmp*(1.0-minValueOnChart)){
                        entries.add(new Entry((float) freqParcel[i], (float) 0.0));
                    } else{

                        entries.add(new Entry((float) freqParcel[i], (float) ampParcel[i]));
                    }



                }

                LineDataSet dataSet = new LineDataSet(entries, "Signal");
                // dataSet.setLabel("Signal strength");

                dataSet.setDrawValues(false);
                LineData lineData = new LineData(dataSet);
                lineData.setHighlightEnabled(true);

                XAxis xAxis = chart.getXAxis();
                xAxis.setDrawLabels(true);


                YAxis yAxis = chart.getAxisRight();
                yAxis.setEnabled(false);

                YAxis yAxis1 = chart.getAxisLeft();
                yAxis1.setDrawLabels(true);


                chart.setData(lineData);
                chart.getDescription().setText("Signal strength for given frequency (e.g. rotations per second)");


                chart.invalidate();

            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (OpenCVLoader.initDebug()) {
            Log.d(TAG, "OpenCV is Configured or Connected successfully.");
            baseLoaderCallback.onManagerConnected(BaseLoaderCallback.SUCCESS);
        } else {
            Log.d(TAG, "OpenCV not Working or Loaded.");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, baseLoaderCallback);
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (javaCameraView != null) {
            javaCameraView.disableView();

        }
        if (thread != null) {
            thread.interrupt();
        }

    }

    @Override
    protected void onPause() {
        super.onPause();

        if (javaCameraView != null) {
            javaCameraView.disableView();

        }
        if (thread != null) {
            thread.interrupt();
        }

    }


    public static Phasor[] singlalTransform(double[] samples, double sampleRate) {
        double[][] data = new double[2][samples.length];
        for (int i = 0; i < data[0].length; i++) {
            data[0][i] = samples[i];
            data[1][i] = 0;
        }

        FastFourierTransformer.transformInPlace(data, DftNormalization.STANDARD, TransformType.FORWARD);
        Phasor[] temp = new Phasor[data[0].length];
        for (int i = 0; i < data[0].length; i++) {


            double freq = i * sampleRate / samples.length;
            double amp = Math.sqrt(data[0][i] * data[0][i] + data[1][i] * data[1][i]);
            double phase = Math.atan2(data[1][i], data[0][i]);
            temp[(int) i] = new Phasor(amp, freq, phase);

        }


        return temp;
    }



    public Mat grayScaleMod(Mat inputMat) {

        Mat tempMat = inputMat;
        Imgproc.cvtColor(inputMat, tempMat, Imgproc.COLOR_RGB2GRAY);

        Imgproc.cvtColor(tempMat, tempMat, inputMat.type());
        return tempMat;
    }

    public void flashOnOff(View view) throws CameraAccessException {

        javaCameraView.toggleFlashMode();
    }

    //Data from menu
    @Override
    public boolean onMenuItemClick(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.red:
                filterColorType = 0;
                return true;
            case R.id.green:
                filterColorType = 1;
                return true;
            case R.id.blue:
                filterColorType = 2;
                return true;
            case R.id.gray:
                filterColorType = 3;

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }




    //Data from Settings
    @Override
    public void applySettings(int framesToAnalize, double minValueOnChart) {
        this.framesToAnalize = framesToAnalize;
        this.minValueOnChart= minValueOnChart;
    }

    public void openSettings(View view) {
        settings = new Settings(framesToAnalize,minValueOnChart);
        settings.show(getSupportFragmentManager(), "Settings");
    }


}
