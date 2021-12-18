package com.diplabs.motioncam3;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.hardware.camera2.CameraAccessException;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

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
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import static android.content.ContentValues.TAG;
import static org.opencv.core.Core.mixChannels;
import static org.opencv.core.Core.split;
import static org.opencv.core.CvType.CV_8UC1;
import static org.opencv.core.CvType.CV_8UC3;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2, PopupMenu.OnMenuItemClickListener {

    //TODO: Dodac menu do innej ilosci pikseli analizowanych
    //TODO: DODAC w tym samym menu ilosc FRAME'ów do analizy (UWAGA, DO FPS TEZ!)
    //TODO: CZY DA SIE ZMIENIC NAZWE?  taka juz istnieje...

    // JAVA camera 2 dziala. jest zoom! :) ale cos fpsy nizsze niz przy camera 1 (?) Podobnot ak ejst....


    private static final String TAG = "FreqencyCam3";
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

//        Mat tempMat = new Mat(mRgba.rows(), mRgba.cols(), mRgba.type());


        // po co color modifier podstawowy skoro mozna to z kanalow wyciagnac.
//        switch (filterColorType){
//            case 0:
//
//                colorModifierMat(mRgba).copyTo(tempMat);
//                break;
//            case 1:
//
//                colorModifierMat(mRgba).copyTo(tempMat);
//                break;
//            case 2:
//
//                colorModifierMat(mRgba).copyTo(tempMat);
//                break;
//            case 3:
//
//             grayScaleMod(mRgba).copyTo(tempMat);;
//                break;
//            default:
//           grayScaleMod(mRgba).copyTo(tempMat);;
//                break;
//        }


        //mozna wszystko zawrzec w orginale
//        switch (filterColorType) {
//            case 0:
//            case 2:
//            case 1:
//
//                mRgba.copyTo(tempMat);
//                break;
//            case 3:
//
//                grayScaleMod(mRgba).copyTo(tempMat);
//                break;
//            default:
//                grayScaleMod(mRgba).copyTo(tempMat);
//                break;
//        }


        ArrayList<Mat> dst = new ArrayList<>(3);

        if (filterColorType == 3) {
            grayScaleMod(mRgba).copyTo(mRgba);
        } else {
            Core.split(mRgba, dst);

        }


        if (graphOn) {


            switch (filterColorType) {
                case 0:

//                    pushValueToQueue(mRgba.get(mRgba.rows() / 2, mRgba.cols() / 2)[0]);
                    pushValueToQueue(dst.get(0).get(mRgba.rows() / 2, mRgba.cols() / 2)[0]);
                    dst.set(1,Mat.zeros(mRgba.size(),dst.get(1).type()));
                    dst.set(2,Mat.zeros(mRgba.size(),dst.get(2).type()));

                    Core.merge(dst,mRgba);

                    break;
                case 1:

//                    pushValueToQueue(mRgba.get(mRgba.rows() / 2, mRgba.cols() / 2)[1]);
                    pushValueToQueue(dst.get(1).get(mRgba.rows() / 2, mRgba.cols() / 2)[0]);
                    dst.set(0,Mat.zeros(mRgba.size(),dst.get(0).type()));
                    dst.set(2,Mat.zeros(mRgba.size(),dst.get(2).type()));

                    Core.merge(dst,mRgba);


                    break;
                case 2:

//                    pushValueToQueue(mRgba.get(mRgba.rows() / 2, mRgba.cols() / 2)[2]);
                    pushValueToQueue(dst.get(2).get(mRgba.rows() / 2, mRgba.cols() / 2)[0]);
                    dst.set(0,Mat.zeros(mRgba.size(),dst.get(0).type()));
                    dst.set(1,Mat.zeros(mRgba.size(),dst.get(1).type()));

                    Core.merge(dst,mRgba);

                    break;
                case 3:
                    //Linear luminance. Can be change to any other grayscale coversion
//                    pushValueToQueue(
//                            (0.2126 * mRgba.get(mRgba.rows() / 2, mRgba.cols() / 2)[0]) +
//                                    (0.7152 * mRgba.get(mRgba.rows() / 2, mRgba.cols() / 2)[1]) +
//                                    (0.0722 * mRgba.get(mRgba.rows() / 2, mRgba.cols() / 2)[2])
//
//                    );

                    pushValueToQueue(mRgba.get(mRgba.rows() / 2, mRgba.cols() / 2)[0]);

                    break;
                default:
//                    pushValueToQueue(
//                            (0.2126 * mRgba.get(mRgba.rows() / 2, mRgba.cols() / 2)[0]) +
//                                    (0.7152 * mRgba.get(mRgba.rows() / 2, mRgba.cols() / 2)[1]) +
//                                    (0.0722 * mRgba.get(mRgba.rows() / 2, mRgba.cols() / 2)[2])
//
//                    );
                    pushValueToQueue(mRgba.get(mRgba.rows() / 2, mRgba.cols() / 2)[0]);

                    break;
            }


        }



        if (cameraOn) {

            switch (filterColorType) {
                case 0:
//                    return dst.get(0);
                    return mRgba;

                case 1:
                                       return mRgba;
//                    return dst.get(1);
                case 2:
                    return mRgba;
//                    return dst.get(2);
                default:
                    return mRgba;

            }

        } else {
            return null;
        }

    }

    Queue<Double> valuesToAnalize = new LinkedList<Double>();
    Queue<Double> FPStoAnalize = new LinkedList<Double>();

    public void pushValueToQueue(double value) {
        synchronized (monitor) {

            valuesToAnalize.add(value);
            FPStoAnalize.add(javaCameraView.mFpsMeter.getFPS());
            monitor.notifyAll();

        }
    }

    private final Object monitor = new Object();
    private Thread thread;


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


    private void analizeThread() throws InterruptedException {

        while (!thread.isInterrupted()) {

            synchronized (monitor) {
                while (valuesToAnalize.size() < 64) {
                    monitor.wait();
                }

                double[] test = new double[64];
                double[] fpsArray = new double[64];


                for (int i = 0; i < 64; i++) {
                    test[i] = valuesToAnalize.poll();
                }

                for (int i = 0; i < 64; i++) {
                    fpsArray[i] = FPStoAnalize.poll();
                }


                Phasor[] testDFT = singlalTransform(test, average(fpsArray)); // do pobrania fps;


                double[] ampParcel = new double[testDFT.length / 2];

                double[] freqParcel = new double[testDFT.length / 2];


                for (int i = 0; i < testDFT.length / 2; i++) {
                    //   Log.i(TAG, "run: " + testDFT[i].toString2());
                    // freqList[i]=testDFT[i].getAmp();

                    ampParcel[i] = testDFT[i].getAmp();
                    freqParcel[i] = testDFT[i].getFreq();

                }


                List<Entry> entries = new ArrayList<Entry>();
                for (int i = 5; i < ampParcel.length; i++) {

                    entries.add(new Entry((float) freqParcel[i], (float) ampParcel[i]));

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
        //System.out.println(java.util.Arrays.toString(data[0]) + "\n");
        Phasor[] temp = new Phasor[data[0].length];
        for (int i = 0; i < data[0].length; i++) {


            double freq = i * sampleRate / samples.length;
            double amp = Math.sqrt(data[0][i] * data[0][i] + data[1][i] * data[1][i]);
            double phase = Math.atan2(data[1][i], data[0][i]);
            temp[(int) i] = new Phasor(amp, freq, phase);

        }


        return temp;
    }


    public double average(double[] data) {
        double sum = 0;
        double average;

        for (int i = 0; i < data.length; i++) {
            sum = sum + data[i];
        }
        average = (double) sum / data.length;
        return average;
    }


    public Mat grayScaleMod(Mat inputMat) {

        Mat tempMat = inputMat;
        Imgproc.cvtColor(inputMat, tempMat, Imgproc.COLOR_RGB2GRAY);

        Imgproc.cvtColor(tempMat, tempMat, inputMat.type());
        return tempMat;
    }


//    int initialColor = 0xffffff;
//    int filterColor = initialColor;
//    public Mat colorModifierMat(Mat inputMat){
//        // Log.i(TAG, "colorModifierMat: " +inputMat.type());
//        double redPercent = Color.red(filterColor) / 255.0 ;
//        double greenPercent = Color.green(filterColor) / 255.0;
//        double bluePercent = Color.blue(filterColor) /255.0;
//
////    Log.i(TAG, "colorModifierMat: "+inputMat.cols() + "cols" + inputMat.rows() + "rows");
//
//        inputMat.convertTo(inputMat, CvType.CV_64FC3);
////    inputMat.convertTo(inputMat, CvType.CV_8UC3);
//        int size = (int) (inputMat.total() * inputMat.channels());
//        double[] temp = new double[size];
////    Log.i(TAG, "colorModifierMat: size" + size);
//
//        inputMat.get(0, 0, temp);
//        for (int i = 0; i < size-4; i=i+4)
//        {
//
//            temp[i] =  (temp[i]*redPercent);
//
//            temp[i+1] =  (temp[i+1]*greenPercent);
//            temp[i+2] =  (temp[i+2] *bluePercent);
//            temp[i+3] =  255;
//        }
//
//        inputMat.put(0, 0, temp);
//        inputMat.convertTo(inputMat, CvType.CV_8UC4);
//        return inputMat;
//    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.red:
                filterColorType = 0;
//                filterColor = 0xff0000;
                return true;
            case R.id.green:
                filterColorType = 1;
//                filterColor = 0x00ff00;
               return true;
            case R.id.blue:
                filterColorType = 2;
//                filterColor = 0x0000ff;
                return true;
            case R.id.gray:
                filterColorType = 3;

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void flashOnOff(View view) throws CameraAccessException {

        javaCameraView.toggleFlashMode();
    }


    public int getIntFromColor(int R, int G, int B) {

        R = (R << 16) & 0x00FF0000;
        G = (G << 8) & 0x0000FF00;
        B = B & 0x000000FF;

        return 0xFF000000 | R | G | B;
    }


}
