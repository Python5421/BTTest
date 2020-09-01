package com.lanyue.bttest.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class BytesToInt {
    private byte[] bytes;
    private byte b;
    public BytesToInt(byte[] bytesin){
        bytes=bytesin;
    }
    public BytesToInt(byte bytesin){
        b=bytesin;
    }
//    public Integer getInt(){
//        int value=0;
//        for(int i = 0; i < 4; i++) {
//            int shift= (3-i) * 8;
//            value +=(bytes[i] & 0xFF) << shift;
//        }
//        return value;
//    }
//    public  int getInt1(){
////        int l = Integer.valueOf(b);
//        int l=b& 0xFF;
//        return l;
//    }

    public Integer getInt(){
        ByteBuffer buf = ByteBuffer.allocateDirect(4);
        buf = buf.order(ByteOrder.LITTLE_ENDIAN);//默认大端，小端用这行
        buf.put(bytes);
        buf.rewind();
        Integer f2 = buf.getInt();
        return f2;
    }
    public  int getInt1(){
        ByteBuffer buf = ByteBuffer.allocateDirect(4);
        buf = buf.order(ByteOrder.LITTLE_ENDIAN);//默认大端，小端用这行
        buf.put(b);
        buf.rewind();
        Integer f2 = buf.getInt();
        return f2;
    }
}
