package com.hogent.jan.attblegateway.ATTBLE;

public final class Broker {

    public  static String  broker   = "broker.smartliving.io";
    public  static int     port     = 1883;
    private static boolean ssl      = false;
    private static String  protocol = ssl == true ? "ssl://" : "tcp://";

    public static String getBrokerUrl()
    {
        return protocol + broker + ":" + port;
    }
}
