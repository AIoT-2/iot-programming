package broker;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public interface MqttCallback {
    public void connectionLost(Throwable throwable);

    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken);

    public void messageArrived(String topic, MqttMessage message);
}
