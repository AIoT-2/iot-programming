package modbus;
import com.intelligt.modbus.jlibmodbus.Modbus;
import com.intelligt.modbus.jlibmodbus.exception.ModbusIOException;
import com.intelligt.modbus.jlibmodbus.exception.ModbusNumberException;
import com.intelligt.modbus.jlibmodbus.exception.ModbusProtocolException;
import com.intelligt.modbus.jlibmodbus.master.ModbusMaster;
import com.intelligt.modbus.jlibmodbus.master.ModbusMasterFactory;
import com.intelligt.modbus.jlibmodbus.msg.request.ReadInputRegistersRequest;
import com.intelligt.modbus.jlibmodbus.msg.response.ReadInputRegistersResponse;
import com.intelligt.modbus.jlibmodbus.tcp.TcpParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

public class SimpleMasterTCP {
    private static final Logger log = LoggerFactory.getLogger(SimpleMasterTCP.class);
    static public void main(String[] args){
        Modbus.log().addHandler(new Handler(){
            @Override
            public void publish(LogRecord record){
                System.out.println(record.getLevel().getName() + ": "+ record.getMessage());
            }

            @Override
            public void flush(){

            }

            @Override
            public void close() throws SecurityException{

            }
        });
        Modbus.setLogLevel(Modbus.LogLevel.LEVEL_DEBUG);

        try{
            TcpParameters tcpParameters = new TcpParameters();
            tcpParameters.setHost(InetAddress.getByName("192.168.70.204"));
            tcpParameters.setKeepAlive(true);
            tcpParameters.setPort(Modbus.TCP_PORT);

            ModbusMaster m = ModbusMasterFactory.createModbusMasterTCP(tcpParameters);
            Modbus.setAutoIncrementTransactionId(true);

            int slaveId = 1;
            int offset = 100;
            int quantity = 32;

            try{
                if(!m.isConnected()){
                    m.connect();
                }

                int[] registerValues = m.readInputRegisters(slaveId, offset, quantity);

                for(int i = 0; i < registerValues.length; i++){
                    System.out.println("Address: " + (offset + i) + ", Value: " + registerValues[i]);
                }

                ReadInputRegistersRequest request = new ReadInputRegistersRequest();
                request.setServerAddress(slaveId);
                request.setStartAddress(offset);
                request.setQuantity(quantity);
                request.setTransactionId(1);
                ReadInputRegistersResponse response = (ReadInputRegistersResponse)m.processRequest(request);

                for(int i = 0; i < response.getHoldingRegisters().getQuantity(); i++){
                    System.out.println("Address: " + (offset + i) + ", Value: " + response.getHoldingRegisters().get(i));
                }

                int value = (response.getHoldingRegisters().get(16) << 16) | response.getHoldingRegisters().get(17);

                System.out.println("V1 : " + value / 100);
            }catch(ModbusProtocolException | ModbusNumberException | ModbusIOException e){
                log.debug("{}", e.getMessage());
            }finally {
                try{
                    m.disconnect();
                }catch (ModbusIOException e){
                    log.debug("{}", e.getMessage());
                }
            }
        }catch (RuntimeException e){
            throw e;
        }catch(Exception e){
            log.debug("{}", e.getMessage());
        }
    }
}
