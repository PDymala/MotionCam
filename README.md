# MotionCam
Image frequency analyzer

Following app uses captured frames (center point from the middle and its color) to analize their potential freqency. Those can be useful when looking at stroboscopic light or rotating parts

It works as follows:
image capture (64 frames) -> aggregate color and FPS-> FFT -> push to chart

Image capturing is continous, when it reaches 64 it runs on a second thread the analizys.

TODO
* frames to be captured - number to be optional
* points from the frame - number to be optional


Some images of the app"

<img src="lm.png" width="400" />
<BR><BR>
A sample label that is genuine:
<img src="lm1_genuine_label.png" width="150" />
