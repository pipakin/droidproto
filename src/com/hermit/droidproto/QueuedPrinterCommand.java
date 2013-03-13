package com.hermit.droidproto;

public class QueuedPrinterCommand {
	private String _command;
	private OnPrinterResponseListener _listener;
	
	public QueuedPrinterCommand(String command, OnPrinterResponseListener listener) {
		_command = command;
		_listener = listener;
	}

	public String getCommand()
	{
		return _command;
	}
	
	public void response(String response)
	{
		if(_listener != null)
			_listener.response(_command, response);
	}
	
	public void complete()
	{
		if(_listener != null)
			_listener.complete(_command);
	}
}
