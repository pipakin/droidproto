package com.hermit.droidproto;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import com.ipaulpro.afilechooser.utils.FileUtils;

import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;

public class DroidProto extends Activity {
	
	public static final String COMMAND_ENTRY_INDEX = "commandEntryIndex";
	public static final String COMMAND_ENTRY = "commandEntry";
	private ArrayList<PrinterCommandEntry> _entries;
	private int _bedSizeX = 200;
	private int _bedSizeY = 200;
	private PrinterService _service;
	private static final int REQUEST_CODE_NEW_COMMAND = 100;
	private static final int REQUEST_CODE_EDIT_COMMAND = 101;
	private static final int REQUEST_CODE = 1;
	private static final String CHOOSER_TITLE = "Select a gcode file";
	protected static final String TAG = "DroidProto UI";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_droid_proto);
		
		SharedPreferences settings = getPreferences(MODE_PRIVATE);
		//get em...
		
		_entries = new ArrayList<PrinterCommandEntry>();
		
		loadCommands();
	
	}
	
	@Override
	protected void onResume() 
	{
		bindService(new Intent(this, UsbConnectionService.class), _connection, Context.BIND_AUTO_CREATE);
		super.onResume();
	};
	
	private void loadCommands()
	{
		_entries.clear();
		
		SharedPreferences settings = getPreferences(MODE_PRIVATE);
		int commandCount = settings.getInt("Entries", 0);
		for(int i=0;i<commandCount;i++)
		{
			_entries.add(PrinterCommandEntry.getFromSettings(settings, i));
		}
		
		if(commandCount == 0)
		{
			_entries.add(new PrinterCommandEntry("Home All", "Return all axes to home position", new String[]{"G28","M84"}));
			_entries.add(new PrinterCommandEntry("Z+", "Move Z axis +1", new String[]{"G91", "G1 Z1", "G90","M84"}));
			_entries.add(new PrinterCommandEntry("Z-", "Move Z axis -1", new String[]{"G91", "G1 Z-1", "G90","M84"}));
			_entries.add(new PrinterCommandEntry("Extruder+", "Move E axis +1", new String[]{"G91", "G1 E1 F100", "G90","M84"}));
			_entries.add(new PrinterCommandEntry("GET DOWN TONIGHT!", "Do a little dance...", new String[]{"G28", "G1 X150 Y150 F10000", "G1 X0 Y150 F10000", "G1 X150 Y0 F10000", "G1 X0 Y0 F10000", "G90","M84"}));
			saveCommands();
		}
	}
	
	private void refreshCommands()
	{
		final ListView lv = (ListView)findViewById(R.id.customCommands);
		lv.setAdapter(new PrinterCommandEntryArrayAdapter(DroidProto.this, android.R.layout.simple_list_item_2, android.R.id.text1, android.R.id.text2, _entries));
	}
	
	private void saveCommands()
	{
		SharedPreferences settings = getPreferences(MODE_PRIVATE);
		SharedPreferences.Editor editor = settings.edit();
		editor.putInt("Entries", _entries.size());
		for(PrinterCommandEntry entry : _entries)
		{
			entry.SaveToSettings(editor, _entries.indexOf(entry));
		}
		editor.commit();
	}
	
	private ServiceConnection _connection = new ServiceConnection() {
		@Override
		public void onServiceDisconnected(ComponentName name) {
			_service = null;				
		}
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			_service = ((PrinterServiceBinder)service).getService();

			refreshCommands();
			final ListView lv = (ListView)findViewById(R.id.customCommands);
			lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> av, View v, int i, long l) {
					PrinterCommandEntry entry = (PrinterCommandEntry) lv.getItemAtPosition(i);
					for(String command : entry.getCommand())
					{
						_service.addCommand(command, null);
					}
				}
			});
			lv.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
				@Override
				public void onCreateContextMenu(ContextMenu menu, View v,
						ContextMenuInfo menuInfo) {
					AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
					final int position = info.position;
				    menu.setHeaderTitle(DroidProto.this._entries.get(info.position).getTitle());
				    if(position > 0)
				    menu.add(Menu.NONE, 1, 1, "Move Up").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() 
				    {
						@Override
						public boolean onMenuItemClick(MenuItem item) {
							PrinterCommandEntry entry = _entries.get(position);
							_entries.set(position, _entries.get(position -1));
							_entries.set(position - 1, entry);
							saveCommands();
							refreshCommands();
							return false;
						}
					});
				    if(position < _entries.size() - 1)
				    menu.add(Menu.NONE, 2, 2, "Move Down").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() 
				    {
						@Override
						public boolean onMenuItemClick(MenuItem item) {
							PrinterCommandEntry entry = _entries.get(position);
							_entries.set(position, _entries.get(position + 1));
							_entries.set(position + 1, entry);
							saveCommands();
							refreshCommands();
							return false;
						}
					});
				    menu.add(Menu.NONE, 3, 3, "Edit").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() 
				    {
						@Override
						public boolean onMenuItemClick(MenuItem item) {
							Intent intent = new Intent(DroidProto.this, EditCommandActivity.class);
							intent.putExtra(COMMAND_ENTRY, _entries.get(position));
							intent.putExtra(COMMAND_ENTRY_INDEX, position);
							startActivityForResult(intent, REQUEST_CODE_EDIT_COMMAND);
					
							return false;
						}
					});
				    menu.add(Menu.NONE, 4, 4, "Delete").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() 
				    {
						@Override
						public boolean onMenuItemClick(MenuItem item) {
							//prompt?
							AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(DroidProto.this);
							dialogBuilder.setTitle("Delete Command");
							dialogBuilder.setMessage("Are you sure you want to delete command \"" + _entries.get(position).getTitle() + "\"?");
							dialogBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									_entries.remove(position);
									saveCommands();
									refreshCommands();
									dialog.dismiss();
								}
							});
							dialogBuilder.setNegativeButton("No", new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									dialog.dismiss();
								}
							});
							return false;
						}
					});
				}
			});
			RepRapSurface sv = (RepRapSurface)findViewById(R.id.Clicklayout);
			sv.setOnTouchListener(new View.OnTouchListener() {
				
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					double height = v.getMeasuredHeight();
					double width = v.getMeasuredWidth();
					double x = (double)(event.getX() - v.getX() + width/2)/width * (double)_bedSizeX;
					double y = (double)(event.getY() - v.getY())/height * (double)_bedSizeY;
					
					_service.addCommand("G1 X" + x + " Y" + y + "F10000", null);
					_service.addCommand("M84", null);					
					return false;
				}
			});
			
			Button sendCommand = (Button)findViewById(R.id.sendCommand);
			sendCommand.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					EditText command = (EditText)findViewById(R.id.commandText);
					String text = command.getText().toString().trim();
					if(text != "" && !text.startsWith("#"))
					{
						_service.addCommand(text, null);
						command.setText("");
					}
				}
			});

			if(_service.getProgress() <= 1.0)
			{
				showProgress();
			}else if(_service.getPaused())
			{
				showPaused();
			}else {
				_service.resetConnection();
			}
		}
	};	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_droid_proto, menu);
		return true;
	}
	
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		
		if(item.getItemId() == R.id.menu_print_file)
		{
			Intent target = FileUtils.createGetContentIntent();
		    Intent intent = Intent.createChooser(target, CHOOSER_TITLE);
		    try {
		        startActivityForResult(intent, REQUEST_CODE);
		    } catch (ActivityNotFoundException e) {
		        // The reason for the existence of aFileChooser
		    }
		}
		
		if(item.getItemId() == R.id.menu_reset_MCU)
		{
			_service.abortFile();
			_service.resetConnection();
		}
		
		if(item.getItemId() == R.id.menu_new_command)
		{
			startActivityForResult(new Intent(this, EditCommandActivity.class), REQUEST_CODE_NEW_COMMAND);
		}

		return super.onMenuItemSelected(featureId, item);
	}
	
	private void showPaused()
	{
		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
		dialogBuilder.setTitle("Paused");
		dialogBuilder.setMessage("Cancel or Resume?");
		dialogBuilder.setPositiveButton("Resume", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				_service.resumeFile();
				showProgress();
			}
		});
		dialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				_service.abortFile();
				dialog.dismiss();
			}
		});
		
		dialogBuilder.create().show();
	}
	
	private void showProgress()
	{
		Log.d(TAG, "Showing progress...");
		final ProgressDialog dialog = new ProgressDialog(this);
		dialog.setTitle("Printing...");
		dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		dialog.setCancelable(false);
		dialog.setButton(DialogInterface.BUTTON_POSITIVE, "Pause", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				//close the dialog and show the paused dialog...
				_service.pauseFile();
				showPaused();
			}
		});
		dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				_service.abortFile();
			}
		});
		dialog.setMax(100);
		dialog.show();
		new Thread(new Runnable() {
			@Override
			public void run() {
				double lastProgress = 0.0;
				double progress = 0.0;
				Log.d(TAG, "Staring progress loop...");
				while((progress = _service.getProgress()) <= 1.0)
				{
					if(progress != lastProgress)
					{
						Log.d(TAG, "Progress! " + Double.toString(progress));
						final int val = (int)(100 * progress);
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								dialog.setProgress(val);
							}
						});
						lastProgress = progress;
					}
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				Log.d(TAG, "Last progress: " + Double.toString(progress));
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						dialog.dismiss();
					}
				});
			}
		}).start();
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	    switch (requestCode) {
	    case REQUEST_CODE_NEW_COMMAND:
	    	if(resultCode == RESULT_OK) {
	    		//add the new command
	    		PrinterCommandEntry entry = (PrinterCommandEntry)data.getParcelableExtra(COMMAND_ENTRY);
	    		_entries.add(entry);
	    		saveCommands();
	    		refreshCommands();
	    	}
	    	break;
	    case REQUEST_CODE_EDIT_COMMAND:
	    	if(resultCode == RESULT_OK) {
	    		//add the new command
	    		PrinterCommandEntry entry = (PrinterCommandEntry)data.getParcelableExtra(COMMAND_ENTRY);
	    		int index = data.getIntExtra(COMMAND_ENTRY_INDEX, -1);
	    		if(index != -1)
	    		{
		    		_entries.set(index, entry);
		    		saveCommands();
		    		refreshCommands();
	    		}
	    	}
	    	break;
	    case REQUEST_CODE:  
	        if (resultCode == RESULT_OK) {  
	            // The URI of the selected file 
	            final Uri uri = data.getData();
	            // Create a File from this Uri
	            final File file = FileUtils.getFile(uri);
	            
	            String name = file.getName().toLowerCase();
	            	            
	            if(!name.endsWith(".gcode") && !name.endsWith(".g"))
	            {
	            	//alert the user that they may not want to use this file...
	            	AlertDialog errorAlert = new AlertDialog.Builder(this).create();
	        		errorAlert.setTitle("Warning");
	        		errorAlert.setMessage("This file may not be a gcode file.  Are you sure you wish to proceed?");
	        		errorAlert.setButton(DialogInterface.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
	        			@Override
	        			public void onClick(DialogInterface dialog, int which) {
	    	            	try {
	    						_service.startFile(new FileInputStream(file), file.length());
	    						runOnUiThread(new Runnable() {
									@Override
									public void run() {
										showProgress();
									}
								});
	    					} catch (FileNotFoundException e) {
	    						e.printStackTrace();
	    					}
	        			}
	        		});
	        		errorAlert.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
	        			@Override
	        			public void onClick(DialogInterface dialog, int which) {
	        			}
	        		});
	        		errorAlert.show();
	            }else{
	            	try {
	            		Log.d(TAG, "Staring print...");
						_service.startFile(new FileInputStream(file), file.length());
						Log.d(TAG, "Attempting to display progress...");
						showProgress();
						Log.d(TAG, "Done displaying progress!");
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					}
	            }
	        }
	    }
	}


}
