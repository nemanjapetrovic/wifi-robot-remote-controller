package rs.ac.ni.elfak.robot.wifiremotecontroller.wifirrobotapi;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.math.BigInteger;
import rs.ac.ni.elfak.robot.wifiremotecontroller.CRC16Modbus;


public class RobCom {

    public static final int SENDER_TIME = 25;
    public static final String ROBOT_IP = "192.168.1.106";
    public static final int CONTROL_PORT = 15000;
    public static final int DATA_PORT = 15010;
    public static final int STOP = 0;
    public static final int FORWARD = 1;
    public static final int BACKWARD = 2;
    public static final int LEFT = 3;
    public static final int RIGHT = 4;
    private static RobCom instance;
    private DatagramSocket csControl;
    private DatagramSocket csData;
    private InetAddress IP;
    private int portData;
    private int portControl;
    public SensorData dataR;
    public SensorData dataL;

    private RobCom(String IP, int pData, int pControl) throws UnknownHostException, SocketException {
        this.csControl = new DatagramSocket();
        this.csData = new DatagramSocket();
        this.IP = InetAddress.getByName(IP);
        this.portData = pData;
        this.portControl = pControl;
        dataR = new SensorData();
        dataL = new SensorData();
        csData.setSoTimeout(500);
        csControl.setSoTimeout(500);
    }

    /***
     *
     * @param IP ip address of robot
     * @param pData data port 15010
     * @param pControl control port 15000
     * @return singleton
     * @throws UnknownHostException
     * @throws SocketException
     */
    public static RobCom getInstance(String IP, int pData, int pControl) throws UnknownHostException, SocketException {
        if (instance == null)
            instance = new RobCom(IP, pData, pControl);
        return instance;
    }

    private String _getSensorData() throws IOException {
        //Log.d("CSD:", "START!");
        String _init = "init";
        String _ok = "ok";
        String _data = "data";
        byte[] rData = new byte[1024];
        //Log.d("CSD:", "SP!");
        DatagramPacket sp = new DatagramPacket(_init.getBytes(), _init.getBytes().length, this.IP, this.portData);
        csData.send(sp);
        //Log.d("CSD:", "SP SEND OK!");
        //Log.d("CSD:", "RP!");
        DatagramPacket rp = new DatagramPacket(rData, rData.length);
        csData.receive(rp);
        String recv = new String(rp.getData());
        //.d("CSD:", "RP GET OK!");
        if (recv.indexOf(_ok) != -1) {
            sp = new DatagramPacket(_data.getBytes(), _data.getBytes().length, this.IP, this.portData);
            csData.send(sp);
            //Log.d("CSD:", "NEW Datagram packet!");
            rp = new DatagramPacket(rData, rData.length);
           // Log.d("CSD:","WAIT receive!");
            csData.receive(rp);
            //Log.d("CSD:", "OK receive!");
            recv = new String(rp.getData());
            //.d("CSD:", "RECV STRING GET OK!");
        }
        //Log.d("CSD:", "RETURN!");
        return recv;
    }

    //poziva se kad zelimo da procitamo senzore funkcija puni dataR i dataL
    public void getSensorData() throws IOException {
        String data = new String(this._getSensorData());

        byte[] sbuf = data.getBytes();
        dataL.speedFront = (int) ((sbuf[1] << 8) + sbuf[0]);

        if (dataL.speedFront > 32767)
            dataL.speedFront = dataL.speedFront - 65536;

        dataL.batLevel = sbuf[2];
        dataL.IR1 = sbuf[3];
        dataL.IR2 = sbuf[4];
        //dataL.odometry = ((((long) sbuf[8] << 24)) + (((long) sbuf[7] << 16)) + (((long) sbuf[6] << 8)) + ((long) sbuf[5]));
        byte [] x = {sbuf[8],sbuf[7],sbuf[6],sbuf[5]};
        dataL.odometry = new BigInteger(1, x);
        dataR.speedFront = (int) (sbuf[10] << 8) + sbuf[9];

        if (dataR.speedFront > 32767)
            dataR.speedFront = dataR.speedFront - 65536;

        dataR.batLevel = 0;
        dataR.IR1 = sbuf[11];
        dataR.IR2 = sbuf[12];
        //dataR.odometry = ((((long) sbuf[16] << 24)) + (((long) sbuf[15] << 16)) + (((long) sbuf[14] << 8)) + ((long) sbuf[13]));
        byte [] x2 = {sbuf[16],sbuf[15],sbuf[14],sbuf[13]};
        dataR.odometry = new BigInteger(1, x2);

        dataL.current = sbuf[17];
        dataR.current = sbuf[17];
        dataL.version = sbuf[18];
        dataR.version = sbuf[18];
    }

    public void drive(byte brzina, byte stanje) throws IOException {    //0-stop, 1-pravo, 2-nazad, 3-levo, 4-desno
        /*
         send[6] is decomposed as follow (1 byte char -> 8 bits):
		(128) Bit 7 Left Side Closed Loop Speed control :: 1 -> ON / 0 -> OFF
		(64) Bit 6 Left Side Forward / Backward speed flag :: 1 -> Forward / 0 -> Reverse
		(32) Bit 5 Right Side Closed Loop Speed control :: 1 -> ON / 0 -> OFF
		(16) Bit 4 Right Side Forward / Backward speed flag :: 1 -> Forward / 0 -> Reverse
		(8) Bit 3 Relay 4 On/Off (DSUB15 POWER Pin 13 and 14)
		(4) Bit 2 Relay 3 On/Off (DSUB15 POWER Pin 11 and 12)
		(2) Bit 1 Relay 2 On/Off (DSUB15 POWER Pin 4 and 5)
		(1) Bit 0 Relay 1 for Sensors. On/Off: 0 is OFF 1 is ON (DSUB15 POWER Pin 3)
		 0  1  2  3  4  5  6  7  8
		ff:07:00:00:00:00:0b:40:6b - uvek salje

		 */

        byte[] poruka = new byte[6];
        byte[] send = new byte[9];
        byte[] crc = new byte[2];
        CRC16Modbus c = new CRC16Modbus();


        poruka[0] = (byte) 7;
        poruka[1] = brzina;
        poruka[2] = (byte) 0;
        poruka[3] = brzina;
        poruka[4] = (byte) 0;
        switch (stanje) {
            case 0:
                poruka[5] = (byte) 0x08;
                poruka[1] = (byte) 0;
                poruka[3] = (byte) 0;
                break;
            case 1:
                poruka[5] = (byte) 0x58;
                break;
            case 2:
                poruka[5] = (byte) 0x08;
                break;
            case 3:
                poruka[5] = (byte) 0x18;
                break;
            case 4:
                poruka[5] = (byte) 0x48;
                break;
            default:
                poruka[5] = (byte) 0x08;
                poruka[1] = (byte) 0;
                poruka[3] = (byte) 0;
        }

        crc = c.getCrcFrom(poruka);

        send[0] = (byte) 255;
        for (int i = 0; i < poruka.length; i++)
            send[i + 1] = poruka[i];

        send[7] = crc[0];
        send[8] = crc[1];

        DatagramPacket sp = new DatagramPacket(send, send.length, this.IP, this.portControl);
        csControl.send(sp); // TODO
        //csControl.close();
        //csControl.disconnect();
    }


    private static short Crc16(byte Adresse_tab, byte Taille_max) {
        int Crc = 0xFFFF;
        int Polynome = 0xA001;
        int CptOctet = 0;
        int CptBit = 0;
        int Parity = 0;
        Crc = 0xFFFF;
        Polynome = 0xA001;
        for (CptOctet = 0; CptOctet < Taille_max; CptOctet++) {
            Crc ^= (Adresse_tab + CptOctet);
            for (CptBit = 0; CptBit <= 7; CptBit++) {
                Parity = Crc;
                Crc >>= 1;
                if ((Parity % 2) == 1) Crc ^= Polynome;
            }
        }
        return (short) (Crc);
    }
}

