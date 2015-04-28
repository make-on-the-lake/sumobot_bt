package com.makeonthelake.sumobot;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;


public class MainActivity extends Activity {

    private static final String PREF_KEY = "SUMO_BOT_PREFS";
    private static final int REQUEST_ENABLE_BT = 10001;

    TextView botNameView;
    View runState;
    View configureState;
    EditText editBotNameTextView;
    View backButton;
    View settingsButton;
    View blutoothButton;
    String botName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        botNameView = (TextView) findViewById(R.id.sumo_bot_name);
        runState = findViewById(R.id.run_state);
        configureState = findViewById(R.id.configure_state);
        editBotNameTextView = (EditText) findViewById(R.id.set_bot_name);
        backButton = findViewById(R.id.configure_state_options);
        backButton.setOnClickListener(new BackButtonOnClickListener());
        settingsButton = findViewById(R.id.settings);
        settingsButton.setOnClickListener(new SettingsButtonOnClickListener());
        blutoothButton = findViewById(R.id.bluetooth);
        setupEditBotNameEditor();
    }

    @Override
    protected void onStart() {
        super.onStart();
        SharedPreferences sharedPreferences = getSharedPreferences(PREF_KEY, 0);
        botName = sharedPreferences.getString(getString(R.string.pref_name_key), "");
        connectToBot(botName);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_ENABLE_BT) {
            connectToBot(botName);
        }
    }

    private void connectToBot(String botName) {
        if (hasBotName(botName)) {
            botNameView.setText(botName);
            BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        } else {
            botNameView.setText(getString(R.string.empty_bot_name_message));
            showConfigurationState();
        }

    }

    private boolean hasBotName(String botName) {
        return !"".equals(botName);
    }

    private void setupEditBotNameEditor() {
        editBotNameTextView.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    saveName();
                    return true;
                }
                return false;
            }
        });
    }

    private void saveName() {
        botName = editBotNameTextView.getText().toString();
        saveBotName(botName);
        InputMethodManager imm = (InputMethodManager) getSystemService(
                Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(editBotNameTextView.getWindowToken(), 0);
        showRunState();;
        connectToBot(botName);
    }

    private void saveBotName(String botName) {
        SharedPreferences.Editor editor = getSharedPreferences(PREF_KEY, 0).edit();
        editor.putString(getString(R.string.pref_name_key), botName);
        editor.commit();
    }

    private void showConfigurationState() {
        configureState.setVisibility(View.VISIBLE);
        editBotNameTextView.requestFocus();
        findViewById(R.id.configure_state_options).setVisibility(View.VISIBLE);
        runState.setVisibility(View.GONE);
        blutoothButton.setVisibility(View.GONE);
        settingsButton.setVisibility(View.GONE);
    }

    private void showRunState() {
        configureState.setVisibility(View.GONE);
        findViewById(R.id.configure_state_options).setVisibility(View.GONE);
        runState.setVisibility(View.VISIBLE);
        blutoothButton.setVisibility(View.VISIBLE);
        settingsButton.setVisibility(View.VISIBLE);
    }

    private class BackButtonOnClickListener implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            editBotNameTextView.setText("");
            showRunState();
        }
    }

    private class SettingsButtonOnClickListener implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            showConfigurationState();
        }
    }
}
