package com.hermit.droidproto;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.support.v4.app.NavUtils;

public class EditCommandActivity extends Activity {
	private PrinterCommandEntry _entry;
	private int _entryIndex;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_edit_command);
		// Show the Up button in the action bar.
		getActionBar().setDisplayHomeAsUpEnabled(true);
		
		if(getIntent().getParcelableExtra("commandEntry") != null)
		{
			_entry = getIntent().getParcelableExtra("commandEntry");
		}else
		{
			_entry = new PrinterCommandEntry("", "", new String[] {});
		}
		
		_entryIndex = getIntent().getIntExtra(DroidProto.COMMAND_ENTRY_INDEX, -1);
		
		EditText title = (EditText)findViewById(R.id.command_title);
		EditText description = (EditText)findViewById(R.id.command_description);
		EditText commands = (EditText)findViewById(R.id.command_commands);
		
		title.setText(_entry.getTitle());
		description.setText(_entry.getDescription());
		
		StringBuilder commandBuilder = new StringBuilder();
		for(String command : _entry.getCommand())
		{
			commandBuilder.append(command);
			commandBuilder.append("\n");
		}
		
		commands.setText(commandBuilder.toString());
		
		Button save = (Button)findViewById(R.id.command_save);
		Button cancel = (Button)findViewById(R.id.command_cancel);
		
		save.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				EditText title = (EditText)findViewById(R.id.command_title);
				EditText description = (EditText)findViewById(R.id.command_description);
				EditText commands = (EditText)findViewById(R.id.command_commands);
				
				_entry.setTitle(title.getText().toString());
				_entry.setDescription(description.getText().toString());
				_entry.setCommand(commands.getText().toString().split("\n"));
				
				Intent returnIntent = new Intent();
				returnIntent.putExtra(DroidProto.COMMAND_ENTRY, _entry);
				returnIntent.putExtra(DroidProto.COMMAND_ENTRY_INDEX, _entryIndex);
				setResult(RESULT_OK,returnIntent);     
				finish();
			}
		});

		cancel.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				setResult(RESULT_CANCELED);
				finish();
			}
		});
}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_edit_command, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// This ID represents the Home or Up button. In the case of this
			// activity, the Up button is shown. Use NavUtils to allow users
			// to navigate up one level in the application structure. For
			// more details, see the Navigation pattern on Android Design:
			//
			// http://developer.android.com/design/patterns/navigation.html#up-vs-back
			//
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

}
