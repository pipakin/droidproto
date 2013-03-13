package com.hermit.droidproto;

import android.os.Binder;

public class PrinterServiceBinder extends Binder {
	private PrinterService _service;
	
	public PrinterService getService()
	{
		return _service;
	}
	
	public PrinterServiceBinder(PrinterService service)
	{
		_service = service;
	}
}
