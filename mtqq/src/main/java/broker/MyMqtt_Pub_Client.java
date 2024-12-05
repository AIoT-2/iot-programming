package broker;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class MyMqtt_Pub_Client {
    //publisher와 Subscriber의 역할을 하기 위한 기능을 가진 객체
    private MqttClient client;
    public MyMqtt_Pub_Client(){
        try{
            //broker와 MQTT통신을 하며 메세지를 전송할 클라이언트 객체를 만들고 접속
            client = new MqttClient("tcp://192.168.70.203:1883", "myId");
            client.connect();
        }catch(MqttException e){
            e.printStackTrace();
        }
    }
    //메세지 전송을 위한 메소드
    public boolean send(String topic, String msg){
        try{
            //broker로 전송한 메세지 생성
            MqttMessage message = new MqttMessage();
            message.setPayload(msg.getBytes());
            client.publish(topic, message);
        }catch(MqttException e){
            e.printStackTrace();
        }

        return true;
    }

    public void close(){
        if(client != null){
            try{
                client.disconnect();
                client.close();
            }catch(MqttException e){
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args){
        MyMqtt_Pub_Client sender = new MyMqtt_Pub_Client();
        new Thread(new Runnable() {
            @Override
            public void run() {
                int i = 1;
                String msg = "";
                while(true){
                    if(i==5) break;
                    else{
                        if(i%2 == 1) msg = "led_on";
                        else msg = "led_off";
                    }
                    sender.send("iot", msg);
                    i++;
                    try{
                        Thread.sleep(1000);
                    }catch(InterruptedException e){
                        e.printStackTrace();
                    }
                }
                sender.close();
            }
        }).start();

    }
}