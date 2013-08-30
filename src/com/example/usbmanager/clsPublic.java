package com.example.usbmanager;
       
public class clsPublic {
    // �������ֽ�����ת��
     public static byte[] int2bytes(int n) {
     byte[] ab = new byte[4];
     ab[0] = (byte) (0xff & n);
     ab[1] = (byte) ((0xff00 & n) >> 8);
     ab[2] = (byte) ((0xff0000 & n) >> 16);
     ab[3] = (byte) ((0xff000000 & n) >> 24);
     return ab;
     }
       
     // �ֽ����鵽������ת��
     public static int bytes2int(byte b[]) {
     int s = 0;
     s = ((((b[0] & 0xff) << 8 | (b[1] & 0xff)) << 8) | (b[2] & 0xff)) << 8
     | (b[3] & 0xff);
     return s;
     }
       
     // �ֽ�ת�����ַ�
     public static char byte2char(byte b) {
     return (char) b;
     }
       
     private final static byte[] hex = "0123456789ABCDEF".getBytes();
       
     private static int parse(char c) {
     if (c >= 'a')
     return (c - 'a' + 10) & 0x0f;
     if (c >= 'A')
     return (c - 'A' + 10) & 0x0f;
     return (c - '0') & 0x0f;
     }
       
     // ���ֽ����鵽ʮ�������ַ���ת��
     public static String Bytes2HexString(byte[] b) {
     byte[] buff = new byte[2 * b.length];
     for (int i = 0; i < b.length; i++) {
     buff[2 * i] = hex[(b[i] >> 4) & 0x0f];
     buff[2 * i + 1] = hex[b[i] & 0x0f];
     }
     return new String(buff);
     }
       
     // ��ʮ�������ַ������ֽ�����ת��
     public static byte[] HexString2Bytes(String hexstr) {
     byte[] b = new byte[hexstr.length() / 2];
     int j = 0;
     for (int i = 0; i < b.length; i++) {
     char c0 = hexstr.charAt(j++);
     char c1 = hexstr.charAt(j++);
     b[i] = (byte) ((parse(c0) << 4) | parse(c1));
     }
     return b;
     }
}