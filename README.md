# MotionCam
Video frequency analyzer
<img src="motioncamicon.png" width="100" />

Following app uses captured frames (center point from the middle and its color) to analyze their potential frequency in time. Those can be useful when looking at stroboscopic light or rotating parts.

It works as follows:
image capture ( e.g. 64 frames) -> aggregate color and FPS-> Math including FFT -> push to chart

Image capturing is continuous, when it reaches certain number of frames  it runs on a second thread the analysis. In the meantime gathers another package of frames


## Functions

- Apply color filters (red, green, blue, gray)
- Double tap on screen to zoom
- Settings to play around with number of frames to analyze (analysis precision), and cleaning the chart (% of max value not to push to chart)
- Start / stop of preview and chart
- Flashlight
- Interactive chart

## Images of the app:

<BR>
<img src="20211030_143553.gif" width="400" />

<BR>
<img src="20211030_143704.gif" width="400" />

  
## Available on Play Store
 
  https://play.google.com/store/apps/details?id=com.diplabs.motioncam3&hl=pl&gl=US


## Tech
LANGUAGES:
* JAVA + ANDROID

CREDITS:
* CHARTS: MPAndroidChart  https://github.com/PhilJay/MPAndroidChart
* MATH: commons-math3  https://commons.apache.org/proper/commons-math/
* IMAGE PROCESSING: OPENCV-ANDROID 4  https://opencv.org/android/
* Testing the effect: apps running on Processing https://Processing.org
  
  
## Privaty policy ane license
  
PP: https://gist.github.com/PDymala/c87c2e3ba9a69f7387bb6cfa634973e9
  
You may use this software for any private or commercial purpose. Any suggestion to make it better is welcome.
  
  
  


  
