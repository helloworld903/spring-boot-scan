package com.test.common.utils;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.URL;
import java.util.Enumeration;

public class IpUtils {
	public static final String DEFAULT_IP="127.0.0.1";
	private static volatile String  ip;
	
	public static String getLocalIpByNetCard() {
		try {
			for(Enumeration<NetworkInterface>e=NetworkInterface.getNetworkInterfaces();e.hasMoreElements();) {
				NetworkInterface item=e.nextElement();
				for(InterfaceAddress address:item.getInterfaceAddresses()) {
					if(item.isLoopback() || !item.isUp())continue;
				
					if(address.getAddress() instanceof Inet4Address) {
						Inet4Address inet4Address=(Inet4Address)address.getAddress();
						return inet4Address.getHostAddress();
					}
				}
			}
			return InetAddress.getLocalHost().getHostAddress();
		}catch(Exception e) {
			return DEFAULT_IP;
		}
	}

	public static String getInnerIp() throws IOException {
		URL whatIsMyIp = new URL("http://checkip.amazonaws.com");
        try (java.util.Scanner scanner = new java.util.Scanner(whatIsMyIp.openStream(), "UTF-8").useDelimiter("\\A")) {
            String ip = scanner.next().trim();
            return ip;
        }
	}
	
	public static String getLocalIp() {
		if(ip==null) {
			synchronized(IpUtils.class) {
				if(ip==null) {
					ip=getLocalIpByNetCard();
				}
			}
		}
		return ip;
	}
}
