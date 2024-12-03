package Modbus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

public class ModbusTcpClient {
    private static final Logger log = LoggerFactory.getLogger(ModbusTcpClient.class);

    public static void main(String[] args){
        String serverIp = "192.168.70.203";
        int serverPort = 502;

        try(Socket socket = new Socket(serverIp, serverPort)){
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in  = new DataInputStream(socket.getInputStream());

            byte[] request = new byte[] {
                    0x00, 0x01,
                    0x00, 0x00,
                    0x00, 0x06,
                    0x01,
                    0x03,
                    0x00, 0x00,
                    0x00, 0x0A
            };
            out.write(request);
            System.out.println("Request sent to server");

            byte[] response = new byte[256];
            int bytesRead = in.read(response);
            log.debug("bytesRead: {}", bytesRead);

            for(int i = 0; i < bytesRead; i++){
                System.out.printf("Byte %d: 0x%02X\n", i, response[i]);
            }


        } catch(Exception e){
            log.debug("Exception: {}", e.getMessage());
        }
    }
}
