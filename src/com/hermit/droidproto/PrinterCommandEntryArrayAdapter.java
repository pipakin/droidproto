package com.hermit.droidproto;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class PrinterCommandEntryArrayAdapter extends
		ArrayAdapter<PrinterCommandEntry> {
	
	private int _layoutResourceId;
	private int _textViewResourceId;
	private int _subItemTextViewResourceId;

	public PrinterCommandEntryArrayAdapter(Context context, int resource, int textViewResourceId, int subItemTextViewResourceId, List<PrinterCommandEntry> objects) {
		super(context, resource, textViewResourceId, objects);

		_layoutResourceId = resource;
		_textViewResourceId = textViewResourceId;
		_subItemTextViewResourceId = subItemTextViewResourceId;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		PrinterCommandEntry entry = getItem(position);
		
		View row = convertView;
		if(row == null)
		{
			LayoutInflater inflater = ((Activity)getContext()).getLayoutInflater();
			row = inflater.inflate(_layoutResourceId, parent, false);
		}
		
		((TextView)row.findViewById(_textViewResourceId)).setText(entry.getTitle());
		((TextView)row.findViewById(_subItemTextViewResourceId)).setText(entry.getDescription());
		
		return row;
	}

}
