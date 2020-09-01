package com.lanyue.bttest.util;

public class BytesToString {
    private byte[] bytes;
    private static final char[] HEX_CHAR = {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    };
//    public static void main(String[] args) {
//
//    }
    //构造函数
public BytesToString(byte[] bytesin){
        bytes=bytesin;
    }
    public BytesToString(){

    }
    public  String bytesToString(byte[] bytes1) {
        char[] buf = new char[bytes1.length * 2];
        int index = 0;
        for (byte b : bytes1) {
            buf[index++] = HEX_CHAR[b >>> 4 & 0xf];//取高4位
            buf[index++] = HEX_CHAR[b & 0xf];//取低四位
        }
        return new String(buf);
    }
    public  String getString() {
        char[] buf = new char[bytes.length * 2];
        int index = 0;
        for (byte b : bytes) {
            buf[index++] = HEX_CHAR[b >>> 4 & 0xf];//取高4位
            buf[index++] = HEX_CHAR[b & 0xf];//取低四位
        }
        return new String(buf);
    }


}
