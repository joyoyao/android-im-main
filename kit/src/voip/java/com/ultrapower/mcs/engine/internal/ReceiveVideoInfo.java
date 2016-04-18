package com.ultrapower.mcs.engine.internal;

public class ReceiveVideoInfo {
	public int rtt;
	
	public int ebw;
	
	public int plr;
	
	public int receiveBitrate;
	
	public int receiveFrameRate;
	
	public int rbr;
	public int rpr;
	
	public int cl;
	
	public int es = 0;
	public double gf = 0;
	public double gt = 0;
	public double go = 0;
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return String.format("rtt:%d\r\n%d:ebw\r\n%d:plr\r\n%d:cl\r\n%d:rb\r\n%d:rf\r\n%d:rbr\r\n%d:rpr\r\n%d:es" +
				"\r\n%f:gf\r\n%f:gt\r\n%f:go",rtt,ebw,plr,cl,receiveBitrate,receiveFrameRate,rbr,rpr,es,gf,gt,go);
	}
}
