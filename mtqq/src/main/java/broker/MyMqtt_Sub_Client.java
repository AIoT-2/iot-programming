package broker;

import org.eclipse.paho.client.mqttv3.*;

public class MyMqtt_Sub_Client implements MqttCallback{
    //브로커와 통신하는 역할. subscriber, publisher의 역할이라 볼 수 있음.
    private MqttClient mqttClient;
    //브로커에 연결하면서 연결정보를 설정할 수 있는 객체
    private MqttConnectOptions mqttOptions;

    public MyMqtt_Sub_Client init(String server, String clientId){
        try{
            mqttOptions = new MqttConnectOptions();
            mqttOptions.setCleanSession(true);
            mqttOptions.setKeepAliveInterval(30);
            //subscriber 클라이언트 객체 생성
            mqttClient = new MqttClient(server, clientId);
            mqttClient.setCallback((org.eclipse.paho.client.mqttv3.MqttCallback) this);
            mqttClient.connect(mqttOptions);
        }catch(MqttException e){
            e.printStackTrace();
        }
        return this;
    }

    @Override
    public void connectionLost(Throwable throwable) {

    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        System.out.println("=====================메세지 도착=================");
        System.out.println(message);
        System.out.println("topic: " +topic +",id: "+ message.getId() + ",payload: " + new String(message.getPayload()));
    }

    public boolean subscriber(String topic){
        boolean result = true;
        try{
            if(topic != null){
                //Qos는 메세지가 도착하기 위한 품질
                mqttClient.subscribe(topic, 0);
            }
        }catch(MqttException e){
            e.printStackTrace();
            result = false;
        }
        return result;
    }

    public static void main(String[] args){
        MyMqtt_Sub_Client subobj = new MyMqtt_Sub_Client();
        //브로커 서버 호출
        subobj.init("tcp://주소 입력하기", "myid2").subscriber("topic");
    }
}
