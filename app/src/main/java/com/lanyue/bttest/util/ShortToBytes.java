package com.lanyue.bttest.util;

public class ShortToBytes {

//    public static void main(String[] args) {
//        short a=127;
//        shortToBytes_LH(a);
//    }
    public ShortToBytes(){}
    /*** 将short数值转换为占两个字节的byte数组，本方法适用于(低位在前，高位在后)的顺序。
     * <p>与bytesToShort_LH配套使用</p>
     *
     * @param shortVal short 要转换的short值
     * @return byte[] Byte数组
     */
    public  byte[] shortToBytes_LH(short shortVal) {
        byte[] bytes = new byte[2];
        bytes[0] = (byte) (shortVal & 0xff);
        bytes[1] = (byte) (shortVal >> 8 & 0xff);
        return bytes;
    }
}
