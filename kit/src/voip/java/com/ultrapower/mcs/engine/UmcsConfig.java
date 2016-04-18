package com.ultrapower.mcs.engine;

public class UmcsConfig {
	private TransportType transportType;
	private ITransportCallback transportCallback;
	private int traceFilter;
	private boolean isMultiMode;
	TransportType getTransportType() {
		return transportType;
	}
	public void setTransportType(TransportType transportType) {
		this.transportType = transportType;
	}
	public ITransportCallback getTransportCallback() {
		return transportCallback;
	}
	public void setTransportCallback(ITransportCallback transportCallback) {
		this.transportCallback = transportCallback;
	}
	public int getTraceFilter() {
		return traceFilter;
	}
	public void setTraceFilter(int traceFilter) {
		this.traceFilter = traceFilter;
	}
	public boolean isMultiMode() {
		return isMultiMode;
	}
	public void setMultiMode(boolean isMultiMode) {
		this.isMultiMode = isMultiMode;
	}
}
