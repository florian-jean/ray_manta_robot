int number_buckey_per_edge = 4;
float ratio_to_aim=0.5;
float ratio;
float number_right=1.0;
float number_left=1.0;
bool last_left=false;
bool last_right=false;

char rcv_buf[20];

int t_delay = 100;

void setup() {
  int i;
  for(i=0;i<2*number_buckey_per_edge;i++){
    pinMode(i, OUTPUT);
  }  
  Serial.begin(115200);
}
 
void loop() {
  int data_len;
  float data_received;
  while(1)
  {
    data_len=0; 
    while(Serial.available())                 
    {
       rcv_buf[(data_len++)%20] =Serial.read();       
    }
    data_received = *(float*)&rcv_buf;
    if(data_received>=0&&data_received<=1){
      ratio_to_aim=data_received;
      number_right=1;
      number_left=1;
    }
    else if(data_received>=11){
      t_delay= (int) data_received;
    }
    
    ratio = number_right/(number_right+number_left);
    if(ratio < ratio_to_aim){
      if (last_right){
        forward_right(false);
        last_right=false;
      }
      else {
        forward_right(true);
        last_right=true;
      }
      number_right++;
    }
    else {
      if (last_left){
        forward_left(false);
        last_left=false;
      }
      else {
        forward_left(true);
        last_left=true;
      }
    number_left++;
    }
    delay(t_delay);
  }
}

void forward_left(bool b){
  int i;
  bool previous=false;
  if(b){
    for(i=number_buckey_per_edge;i>0;i--){
      if (previous){
        digitalWrite(2*i-1, LOW);
        digitalWrite(2*i-2, HIGH);
        previous = false;
      }
      else {
        digitalWrite(2*i-2, LOW);
        digitalWrite(2*i-1, HIGH);
        previous = true;
      }
    }
  }
  else {
   for(i=number_buckey_per_edge;i>0;i--){
      if (previous){
        digitalWrite(2*i-2, LOW);
        digitalWrite(2*i-1, HIGH);
        previous = false;
      }
      else {
        digitalWrite(2*i-1, LOW);
        digitalWrite(2*i-2, HIGH);
        previous = true;
      }
    }
  }
}

void forward_right(bool b){
  int i;
  bool previous=false;
  if(b){
    for(i=2*number_buckey_per_edge;i>number_buckey_per_edge;i--){
      if (previous){
        digitalWrite(2*i-1, LOW);
        digitalWrite(2*i-2, HIGH);
        previous = false;
      }
      else {
        digitalWrite(2*i-2, LOW);
        digitalWrite(2*i-1, HIGH);
        previous = true;
      }
    }
  }
  else {
   for(i=2*number_buckey_per_edge;i>number_buckey_per_edge;i--){
      if (previous){
        digitalWrite(2*i-2, LOW);
        digitalWrite(2*i-1, HIGH);
        previous = false;
      }
      else {
        digitalWrite(2*i-1, LOW);
        digitalWrite(2*i-2, HIGH);
        previous = true;
      }
    }
  }
}



void forward_left_max_speed(bool b){
  if(b){
  
    digitalWrite(7, LOW);
    digitalWrite(6, HIGH);

    digitalWrite(4, LOW);
    digitalWrite(5, HIGH);

    digitalWrite(3, LOW);
    digitalWrite(2, HIGH);
    
    digitalWrite(0, LOW);
    digitalWrite(1, HIGH);

  }
  else {
    digitalWrite(6, LOW);
    digitalWrite(7, HIGH);
    
    digitalWrite(5, LOW);
    digitalWrite(4, HIGH);

    digitalWrite(2, LOW);
    digitalWrite(3, HIGH);
    
    digitalWrite(1, LOW);
    digitalWrite(0, HIGH);

  }
}

void forward_right_max_speed(bool b){
  if(b){

    digitalWrite(15, LOW);
    digitalWrite(14, HIGH);

    digitalWrite(12, LOW);
    digitalWrite(13, HIGH);

    digitalWrite(11, LOW);
    digitalWrite(10, HIGH);
    
    digitalWrite(8, LOW);
    digitalWrite(9, HIGH);
  }
  else {
    digitalWrite(14, LOW);
    digitalWrite(15, HIGH);
        
    digitalWrite(13, LOW);
    digitalWrite(12, HIGH);
        
    digitalWrite(10, LOW);
    digitalWrite(11, HIGH);
    
    digitalWrite(9, LOW);
    digitalWrite(8, HIGH);

  }
}

