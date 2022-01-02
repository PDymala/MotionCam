float counter;
int framerate =2000;

void setup(){
  size(500,500);
  frameRate(framerate);
  //fullScreen();
}


void draw(){
  background(255);
 translate(width/2,height/2);

strokeWeight(5);
 rotate(radians(counter));
 line(0,0,300,300);
  
  counter++;
  
}
