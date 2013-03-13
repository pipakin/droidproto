package com.hermit.droidproto;

public interface OnPrinterResponseListener {
	public void response(String command, String response);
	public void complete(String command);
}
