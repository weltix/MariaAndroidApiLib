package com.resonance.ekka.mariaapilib;

public class Utils {

    public static byte reverse(byte x) {
        byte b = 0;
        for (int i = 0; i < 8; ++i) {
            b<<=1;
            b|=( x &1);
            x>>=1;
        }
        return b;
    }
    public static void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
