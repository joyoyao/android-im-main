package com.ultrapower.mcs.engine.internal;

public class SendVideoInfo {
	public int rtt;
	
	public int ebw;
	
	public int plr;
	
	public int sendBitrate;
	
	public int sendFrameRate;
	
	public int cl;
	
	public int tbss;
	public int vbss;
	public int nbss;
	
	public int rbs;
	public int rps;
	
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		
		return String.format("rtt:%d\r\nebw:%d\r\nplr:%d\r\ncl:%d\r\nsb:%d\r\nsf:%d\r\ntbss:%d\r\nvbss:%d\r\nbss:%d\r\nbs:%d\r\nps:%d",rtt,ebw,plr,cl,sendBitrate,sendFrameRate,tbss,vbss,nbss,rbs,rps);

	}
	
}
