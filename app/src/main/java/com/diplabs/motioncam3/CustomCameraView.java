package com.diplabs.motioncam3;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Size;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.math.MathUtils;

import org.opencv.android.JavaCamera2View;
import org.opencv.android.JavaCameraView;

public class CustomCameraView extends JavaCamera2View {
    public CustomCameraView(Context context, int cameraId) {
        super(context, cameraId);
    }

    public CustomCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);

    }



    boolean firstTouch = false;
    long time= System.currentTimeMillis();
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(event.getAction() == event.ACTION_DOWN){
            if(firstTouch && (System.currentTimeMillis() - time) <= 300) {
                try {
                    zoomUpCamera();
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }


                Log.e("** DOUBLE TAP**"," second tap ");

                firstTouch = false;

            } else {
                firstTouch = true;
                time = System.currentTimeMillis();
                Log.e("** SINGLE  TAP**"," First Tap time  "+time);
                return false;
            }
        }
        return true;
    }


//
//
//
    private int zoomCounter = 0;
//    public void zoomUpCamera(){
//
//        Camera.Parameters params = mCamera.getParameters();
//        zoomCounter =params.getMaxZoom()/5;
//
//        if (params.isZoomSupported()){
//
//            params.setZoom((params.getZoom() +zoomCounter)%params.getMaxZoom());
//
//            mCamera.setParameters(params);
//
//
//        } else{
//            //not supported
//        }
//
//    }
//thanks https://stackoverflow.com/questions/66016907/manipulate-javacamera2view-to-set-parameters-for-camera-device-opencv-in-andro
    //to be read https://stackoverflow.com/questions/58902461/javacamera2view-pinch-zoom
    // !! https://stackoverflow.com/questions/52568987/camera-zoom-setting-using-camera2-api
    float zoomFloat;
    float currentZoom = 1.0f;
    public void zoomUpCamera() throws CameraAccessException {

        CameraManager mCameraManager = (CameraManager) getContext().getSystemService(Context.CAMERA_SERVICE);

        Zoom zoom = new Zoom( mCameraManager.getCameraCharacteristics(mCameraID));

        zoomFloat = (zoom.maxZoom - Zoom.DEFAULT_ZOOM_FACTOR )/ 6;
        currentZoom = (currentZoom + zoomFloat)%zoom.maxZoom;
        zoom.setZoom(mPreviewRequestBuilder,currentZoom);

        try {
            mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), null, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

//    public double getCameraZoom(){
//        Camera.Parameters params = mCamera.getParameters();
//
//        return ((double)params.getZoom() / (double)params.getMaxZoom());
//    }

//public Size[] getCameraSizes() throws CameraAccessException {
//    CameraManager manager = (CameraManager) getContext().getSystemService(Context.CAMERA_SERVICE);
//    CameraCharacteristics characteristics = manager.getCameraCharacteristics(manager.getCameraIdList()[0]);
//    StreamConfigurationMap streamConfigurationMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
//    Size[] sizes = streamConfigurationMap.getOutputSizes(SurfaceTexture.class);
//
//        return sizes;
//}

    boolean flashlightOn = false;

//    public void toggleFlashLight(){

//        Camera.Parameters params = mCamera.getParameters();
//        if (flashlightOn){
//
//            params.setFlashMode(params.FLASH_MODE_OFF);
//            flashlightOn = false;
//
//        } else{
//            params.setFlashMode(params.FLASH_MODE_TORCH);
//            flashlightOn = true;
//
//
//        }
//        mCamera.setParameters(params);
//

//    }

boolean enable = true;
    public void toggleFlashMode() throws CameraAccessException { //mPrevie changed from protected to public

        try {

                if (enable) {
                    mPreviewRequestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH);
                    mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
                    enable = false;
                } else {
                    mPreviewRequestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);
                    mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
                    enable = true;
                }
                mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), null, null);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }




    public final class Zoom
    {
        private static final float DEFAULT_ZOOM_FACTOR = 1.0f;

        @NonNull
        private final Rect mCropRegion = new Rect();

        public final float maxZoom;

        @Nullable
        private final Rect mSensorSize;

        public final boolean hasSupport;

        public Zoom(@NonNull final CameraCharacteristics characteristics)
        {
            this.mSensorSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);

            if (this.mSensorSize == null)
            {
                this.maxZoom = Zoom.DEFAULT_ZOOM_FACTOR;
                this.hasSupport = false;
                return;
            }

            final Float value = characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM);

            this.maxZoom = ((value == null) || (value < Zoom.DEFAULT_ZOOM_FACTOR))
                    ? Zoom.DEFAULT_ZOOM_FACTOR
                    : value;

            this.hasSupport = (Float.compare(this.maxZoom, Zoom.DEFAULT_ZOOM_FACTOR) > 0);
        }

        public void setZoom(@NonNull final CaptureRequest.Builder builder, final float zoom)
        {
            if (this.hasSupport == false)
            {
                return;
            }

            final float newZoom = MathUtils.clamp(zoom, Zoom.DEFAULT_ZOOM_FACTOR, this.maxZoom);

            final int centerX = this.mSensorSize.width() / 2;
            final int centerY = this.mSensorSize.height() / 2;
            final int deltaX  = (int)((0.5f * this.mSensorSize.width()) / newZoom);
            final int deltaY  = (int)((0.5f * this.mSensorSize.height()) / newZoom);

            this.mCropRegion.set(centerX - deltaX,
                    centerY - deltaY,
                    centerX + deltaX,
                    centerY + deltaY);

            builder.set(CaptureRequest.SCALER_CROP_REGION, this.mCropRegion);
        }
    }





}
