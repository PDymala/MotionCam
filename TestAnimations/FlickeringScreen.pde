

int framerate = 10;
boolean scren;
void setup() {
  size(400, 400);
  frameRate(framerate*2); //because it's on/off, would show framerate/2ł
}


void draw() {

  if (scren) {

    background(0); 
    scren = false;
  } else {

    background(255);
    scren = true;
  }
}
