package com.hermit.droidproto;

import java.util.HashMap;

import android.content.SharedPreferences;
import android.os.Parcel;
import android.os.Parcelable;

public class PrinterCommandEntry implements Parcelable {
	private String _title;
	private String _description;
	private String[] _commands;
	
	public String getTitle() {
		return _title;
	}
	public void setTitle(String title) {
		_title = title;
	}
	public String getDescription() {
		return _description;
	}
	public void setDescription(String description) {
		_description = description;
	}
	public String[] getCommand() {
		return _commands;
	}
	public void setCommand(String[] commands) {
		_commands = commands;
	}
	
	static PrinterCommandEntry getFromSettings(SharedPreferences settings, int index)
	{
		return new PrinterCommandEntry(settings.getString("Entry_" + Integer.toString(index) + "_title", ""),
				settings.getString("Entry_" + Integer.toString(index) + "_description", ""),
				settings.getString("Entry_" + Integer.toString(index) + "_commands", "").split("\n"));
	}
	
	public void SaveToSettings(SharedPreferences.Editor settings, int index)
	{
		settings.putString("Entry_" + Integer.toString(index) + "_title", _title);
		settings.putString("Entry_" + Integer.toString(index) + "_description", _description);
		
		StringBuilder commandBuilder = new StringBuilder();
		for(String command : _commands)
		{
			commandBuilder.append(command);
			commandBuilder.append("\n");
		}
		settings.putString("Entry_" + Integer.toString(index) + "_commands", commandBuilder.toString());
	}
	
	public PrinterCommandEntry(String title, String description, String[] commands)
	{
		_title = title;
		_description= description;
		_commands = commands;
	}
	@Override
	public int describeContents() {
		return 0;
	}
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(_title);
		dest.writeString(_description);
		dest.writeInt(_commands.length);
		dest.writeStringArray(_commands);
	}
	
	public PrinterCommandEntry(Parcel source)
	{
		_title = source.readString();
		_description = source.readString();
		_commands = new String[source.readInt()];
		source.readStringArray(_commands);				
	}
	
	 public static final Parcelable.Creator<PrinterCommandEntry> CREATOR
	     = new Parcelable.Creator<PrinterCommandEntry>() {
		 public PrinterCommandEntry createFromParcel(Parcel in) {
		     return new PrinterCommandEntry(in);
		 }
		
		 public PrinterCommandEntry[] newArray(int size) {
		     return new PrinterCommandEntry[size];
		 }
	 };
}
