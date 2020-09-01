package com.lanyue.bttest.mssage;


import com.lanyue.bttest.util.ShortToBytes;

/**
 * APP发往基站(Base Station)的报文
 * 注：可能会频繁修改所以单独抽出来
 */
public class AppToBSMessage {


    //上传航线 自主起飞 开始航线 暂停航线 继续航线 执行返航 自主降落
    public static int AppToBS_putAirLine = 1;
    public static int AppToBS_takeOff = 2;
    public static int AppToBS_beginAirLine = 3;
    public static int AppToBS_pauseAirLine = 4;
    public static int AppToBS_continueAirLine = 5;
    public static int AppToBS_return = 6;
    public static int AppToBS_land = 7;
    //飞行类型 0AB点 1地块 2航点
    public static int FlightType_AB = 0;
    public static int FlightType_land = 1;
    public static int FlightType_point = 2;

    int action;


    public AppToBSMessage(int action) {
        this.action = action;
    }

    public byte[] getAppToBSBytes() {
        byte[] result = null;

        if (action == AppToBS_putAirLine) {
//            result = getAppToBS_PutAirlineBytes();
        }
        if (action == AppToBS_takeOff) {
            result = getAppToBS_AutoTakeoffBytes();
        }
        if (action == AppToBS_beginAirLine) {
//            result = getAppToBS_beginAirlineBytes();
        }
        if (action == AppToBS_pauseAirLine) {
            result = getAppToBS_pauseAirlineBytes();
        }
        if (action == AppToBS_continueAirLine) {
//            result = getAppToBS_continueAirlineBytes();
        }
        if (action == AppToBS_return) {
            result = getAppToBS_returnBytes();
        }
        if (action == AppToBS_land) {
            result = getAppToBS_AutoLandBytes();
        }
        return result;

    }

    byte[] bytes_in;
    int messageLength;

    //粘报文头 入参：1 报文总长度 2 数据域长度
    private byte[] addHead(int messageLength, int sum_data) {
        //报文整体开始-----------------------
        bytes_in = new byte[messageLength];
        //同步字1、2           0-1
        bytes_in[0] = (byte) 0xAA;
        bytes_in[1] = (byte) 0x63;
        //数据域长度（不包括报文头尾）      2-3
        short sum_d = (short) (sum_data);
        byte[] sum_data_bytes = new ShortToBytes().shortToBytes_LH(sum_d);
        System.arraycopy(sum_data_bytes, 0, bytes_in, 2, 2);
//        Log.e("粘完头",new BytesToString(bytes_in).getString());
        return bytes_in;
    }

    //粘报文尾 入参：1 数据域长度 2 数据域字节数组
    private byte[] addEnd(int m, byte[] bytes_in_data) {
//        bytes_in = new byte[messageLength];
        short[] bx = new short[m];
        for (int i = 0; i < m; i++) {
            bx[i] = toUnsignedByte(bytes_in_data[i]);
        }
        //IOS求和
        byte[] IOC_SUM = ISOSum(bx);
        bytes_in[messageLength - 4] = IOC_SUM[0];
        bytes_in[messageLength - 3] = IOC_SUM[1];
        //结束字
        bytes_in[messageLength - 2] = (byte) 0x09;
        bytes_in[messageLength - 1] = (byte) 0xD7;
        return bytes_in;
    }

    int head = 4;//固定1 + 1 + 2
    int end = 4;//固定



    //自主起飞 报文
    private byte[] getAppToBS_AutoTakeoffBytes() {
        int sum_data = 3;//数据域长度 3
        messageLength = head + sum_data + 4;//报文长度 4+3+4=11
        //报文整体开始-----------------------
        bytes_in = addHead(messageLength, sum_data);
        byte[] bytes_in_data = {(byte) 0xbc, (byte) 00, (byte) 00};
        int m = bytes_in_data.length;
        System.arraycopy(bytes_in_data, 0, bytes_in, 4, m);
        bytes_in = addEnd(m, bytes_in_data);
        return bytes_in;
    }


    //暂停航线 报文
    private byte[] getAppToBS_pauseAirlineBytes() {
        int sum_data = 3;//数据域长度 1
        messageLength = head + sum_data + end;//报文长度 4+1+4=9
        //报文整体开始-----------------------
        bytes_in = addHead(messageLength, sum_data);
        byte[] bytes_in_data = {(byte) 0xac, (byte) 00, (byte) 00};
        int m = bytes_in_data.length;
        System.arraycopy(bytes_in_data, 0, bytes_in, 4, m);
        bytes_in = addEnd(m, bytes_in_data);

        return bytes_in;
    }



    //执行返航 报文
    private byte[] getAppToBS_returnBytes() {
        int sum_data = 3;//数据域长度 1
        messageLength = head + sum_data + end;//报文长度 4+1+4=9
        //报文整体开始-----------------------
        bytes_in = addHead(messageLength, sum_data);
        byte[] bytes_in_data = {(byte) 0xad, (byte) 00, (byte) 00};
        int m = bytes_in_data.length;
        System.arraycopy(bytes_in_data, 0, bytes_in, 4, m);
        bytes_in = addEnd(m, bytes_in_data);
        return bytes_in;
    }

    //自主降落 报文
    private byte[] getAppToBS_AutoLandBytes() {
        int sum_data = 3;//数据域长度 1
        messageLength = head + sum_data + end;//报文长度 4+1+4=9
        //报文整体开始-----------------------
        bytes_in = addHead(messageLength, sum_data);
        byte[] bytes_in_data = {(byte) 0xbd, (byte) 00, (byte) 00};
        int m = bytes_in_data.length;
        System.arraycopy(bytes_in_data, 0, bytes_in, 4, m);
        bytes_in = addEnd(m, bytes_in_data);
        return bytes_in;
    }

    //字节转shot
    public short toUnsignedByte(byte b) {
        return (short) ((short) b & 0xFF);
    }

    //ISO检验和
    byte[] ISOSum(short[] shorts) {
        byte[] sum = new byte[2];

        int i, k1, k2;
        int c0 = 0, c1 = 0;

        for (i = 0; i < shorts.length; ++i) {
            c0 = c0 + shorts[i];
            c1 = c1 + c0;
        }
        k1 = 0xff - ((c0 + c1) % 0xff);
        k2 = c1;
        sum[0] = (byte) (k1 % 0xff);
        sum[1] = (byte) (k2 % 0xff);
        if (sum[0] == 0) sum[0] = (byte) 0xff;
        if (sum[1] == 0) sum[1] = (byte) 0xff;
        return sum;
    }

}
