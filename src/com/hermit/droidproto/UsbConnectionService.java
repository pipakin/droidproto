package com.hermit.droidproto;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Semaphore;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.IBinder;
import android.util.Log;

public class UsbConnectionService extends Service implements PrinterService {
	
	private static final int BUFFER_SIZE = 64;
	private static final String OK_RESPONSE = "ok";
	private static final int TIMEOUT_MILLIS = 1000;
	private static final double NO_PROGRESS = 10.0;
	private UsbSerialDriver _driver;
	private final String TAG = "UsbConnectionService"; 
	private Queue<QueuedPrinterCommand> _queue;
	private Semaphore _commandMutex;
	private boolean _waiting;
	private QueuedPrinterCommand _currentCommand;
	private final String ACTION_USB_PERMISSION = "com.hermit.droidproto.usb.permission";
	private InputStream _currentFile;
	private Semaphore _fileCommandBuffer = new Semaphore(5);
	private boolean _abortPrint = false;
	private Semaphore _pauseFile = new Semaphore(1);
	private long _fileProgress = 0;
	private long _fileLength;
	private boolean _paused = false;
	public UsbConnectionService() {
		
		_queue = new LinkedList<QueuedPrinterCommand>();
		_commandMutex = new Semaphore(1);
		_waiting = false;
	}
	
	public void startFile(InputStream file, long length)
	{
		_paused = false;
		_currentFile = file;
		_fileLength = length;
		_fileProgress = 0;
		_abortPrint = false;
		new Thread(_printFile).start();
	}
	
	public void pauseFile()
	{
		try {
			_pauseFile.acquire();
			_paused = true;
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public synchronized double getProgress()
	{
		if(_currentFile == null || _paused)
			return NO_PROGRESS;
		
		if(_fileProgress == _fileLength)
			return 1.0;
		
		return (double)_fileProgress/(double)_fileLength;
	}
	
	public boolean getPaused()
	{
		return _paused;
	}
	
	private synchronized void setPostion(long pos)
	{
		_fileProgress = pos;
	}
	
	public void resumeFile()
	{
		_paused = false;
		_pauseFile.release();
	}
	
	public void abortFile()
	{
		_abortPrint = true;
		if(_paused)
		{
			resumeFile();
		}
	}
	
	private Runnable _printFile = new Runnable() {
		@Override
		public void run() {
			BufferedReader reader = new BufferedReader(new InputStreamReader(_currentFile));
			String line;
			try {
				while((line = reader.readLine()) != null && !_abortPrint)
				{
					setPostion(_fileProgress + line.length() + 1);
					String trimmedLine = line.trim();
					if(!trimmedLine.startsWith("G") && !trimmedLine.startsWith("M"))
						continue;
					
					_pauseFile.acquire();
					_pauseFile.release();
					
					//add the command...
					_fileCommandBuffer.acquire(1);
					Log.d(TAG + " PrintThread", "Adding command...(" + _fileProgress + "): " + trimmedLine);
					addCommand(trimmedLine, new OnPrinterResponseListener() {
						@Override
						public void response(String command, String response) {
							
						}
						
						@Override
						public void complete(String command) {
							Log.d(TAG + " PrintThread", "command complete!");
							_fileCommandBuffer.release(1);
						}
					});
				}
				_currentFile.close();
				addCommand("M84", new OnPrinterResponseListener() {
					@Override
					public void response(String command, String response) {
						
					}
					
					@Override
					public void complete(String command) {
						Log.d(TAG + " PrintThread", "command complete!");
						_fileCommandBuffer.release(1);
					}
				});
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			_currentFile = null;
			setPostion(_fileLength);
		}
	};
	
	/*private BroadcastReceiver _usbReceiver = new BroadcastReceiver() {

	    public void onReceive(Context context, Intent intent) {
	        if(_driver != null)
	        	return;
	    	
	    	String action = intent.getAction();
	        if (ACTION_USB_PERMISSION.equals(action)) {
	        	synchronized (this) {
	        		UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
	        		UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

	                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
	                   if(device != null){
	                	   tryDevice(manager, device);
	                   }
	                } 
	                else {
	                    Log.d(TAG, "permission denied for device " + device);
	                }
	            }
	        }
	    }
	};*/
	
	private boolean tryDevice(UsbManager manager, UsbDevice device)
	{
		Log.d(TAG, "trying serial for device: " + device);
		UsbSerialDriver driver = UsbSerialProber.acquire(manager, device);
		try {
			if(driver != null)
	        {
	          Log.d(TAG, "!!!!got serial for device: " + device);
	      	  _driver = driver;
	      	  _driver.open();
			
	      	  checkQueue(null);
	      	  _readThread = new Thread(_readRunnable);
	      	  _readThread.start();
	      	  return true;
	        }
		} catch (IOException e) {
			Log.e(TAG, "unable to get serial for device: " + device + " error: " + e.getLocalizedMessage());
		}
        return false;
	}
	
	public boolean findPrinter()
	{
		if(_driver == null)
		{
			UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
			HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
			Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
			while(deviceIterator.hasNext()){
			    UsbDevice device = deviceIterator.next();
			    if (manager.hasPermission(device))
			    {
			    //	Log.d(TAG, "asking permission for device: " + device);
			    //	PendingIntent pi = PendingIntent.getBroadcast(
				//			this, 0, new Intent(ACTION_USB_PERMISSION), 0);
				//	IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
				//	registerReceiver(_usbReceiver, filter);
				//	manager.requestPermission(device, pi);
			    //}else{
			    	if(tryDevice(manager, device))
			    		return true;
			    }
			}
		}
		
		return _driver != null;
	}
	
	public void closePrinter()
	{
		if(_driver != null)
		{
			try {
				_driver.close();
				_driver = null;
			} catch (IOException e) {
				//don't do anything, though log a message.
				Log.e(TAG, "Unable to close printer: " + e.getMessage());
			}
		}
	}

	public void addCommand(String command, OnPrinterResponseListener listener)
	{
		findPrinter();
		checkQueue(new QueuedPrinterCommand(command, listener));
	}
	
	private void checkQueue(QueuedPrinterCommand newCommand)
	{
		Log.i(TAG, "Acquiring Mutex...");
		try {
			_commandMutex.acquire();
			try{
				Log.i(TAG, "Adding item...");
				if(newCommand != null)
					_queue.add(newCommand);
				if(!_waiting && !_queue.isEmpty() && _driver != null)
				{
					Log.i(TAG, "Sending command...");
					sendCommand(_queue.poll());
					_waiting = true;
				}
			}finally{
				Log.i(TAG, "Releasing Mutex...");
				_commandMutex.release();
			}
		} catch (InterruptedException e) {
			Log.e(TAG, "Unable to acquire mutex: " + e.getMessage());
		}
	}
	
	private void sendCommand(QueuedPrinterCommand command)
	{
		if(_driver == null)
			return;

		_currentCommand = command;
		try {
			String commandText = command.getCommand();
			if(commandText.indexOf(";") > -1)
				commandText = commandText.substring(0, commandText.indexOf(";"));
			if(commandText.indexOf("#") > -1)
				commandText = commandText.substring(0, commandText.indexOf("#"));
			Log.i(TAG, "sending command: " + command.getCommand() + " (Actual command): " + commandText);
			_driver.write((commandText + "\n").getBytes("US-ASCII"), TIMEOUT_MILLIS);
		} catch (UnsupportedEncodingException e) {
			Log.e(TAG, "Unable to send command: " + e.getMessage());
		} catch (IOException e) {
			Log.e(TAG, "Unable to send command: " + e.getMessage());
		}
	}
	
	private void recievedLine(String line) {
		Log.i(TAG, "received response: " + line);
		if(line.equals(OK_RESPONSE))
		{
			_currentCommand.complete();
			try {
				_commandMutex.acquire();
				try{
					if(_waiting)
					{
						if(_queue.peek() != null) {
							sendCommand(_queue.poll());
						}else{
							_waiting = false;
							_currentCommand = null;
						}
					}
				} finally{
					_commandMutex.release();
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}else{
			if(_currentCommand != null)
				_currentCommand.response(line);
		}
	}
	
	private Runnable _readRunnable = new Runnable() {
		@Override
		public void run() {
			String lineBuffer = "";
			byte[] buffer = new byte[BUFFER_SIZE];
			int readSize = 0;
			try {
				Log.d(TAG, "start reading");
				while(_driver != null && (readSize = _driver.read(buffer, TIMEOUT_MILLIS)) > -1)
				{
					lineBuffer += new String(buffer, 0, readSize);
					
					if(lineBuffer.contains("\n"))
					{
						int index = lineBuffer.indexOf('\n');
						recievedLine(lineBuffer.substring(0, index).trim());
						lineBuffer = lineBuffer.substring(index + 1);
					}
				}
			} catch (IOException e) {
				//crud, do something useful here?
				Log.e(TAG, "Error reading: " + e.getMessage());
			}
			Log.d(TAG, "done reading");
			_driver = null;
		}
	};
	
	private Thread _readThread;
		
	@Override
	public void onCreate() {
		if(!findPrinter())
		{
			Log.e(TAG, "Unable to open printer.");
		}
		super.onCreate();
	}
	
	@Override
	public void onDestroy() {
		closePrinter();
		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return new PrinterServiceBinder(this);
	}

	@Override
	public void resetConnection() {
		try {
			if(_driver != null)
			{
				_driver.setDTR(true);
				Thread.sleep(100);
				_driver.setDTR(false);
			}
			_commandMutex.acquire();
			_queue.clear();
			_waiting = false;
			_commandMutex.release();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
