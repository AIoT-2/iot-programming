package com.totalmqtt;

public class Main2 {
    public static void main(String[] args) {
        TotalMqtt totalMqtt = new TotalMqtt();
        totalMqtt.connect();
        // while(true){
        //     totalMqtt.MSGSend();
        // }
        totalMqtt.MSGSend();
    }
}
