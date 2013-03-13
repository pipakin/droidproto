package com.hermit.droidproto;

import java.io.InputStream;

public interface PrinterService {
	public boolean findPrinter();
	public void closePrinter();
	public void addCommand(String command, OnPrinterResponseListener listener);
	public void startFile(InputStream file, long length);	
	public void pauseFile();	
	public void resumeFile();	
	public void abortFile();
	public double getProgress();
	public boolean getPaused();
	public void resetConnection();
}
