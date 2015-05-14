package com.makeonthelake.sumobot;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;


public class MainActivity extends Activity implements SumoBotConnectionListener {

    private static final String DEBUG_TAG = MainActivity.class.getName();
    private static final String PREF_KEY = "SUMO_BOT_PREFS";
    private static final int REQUEST_ENABLE_BT = 10001;

    LateralView leftLateralView;
    LateralView rightLateralView;
    TextView botNameView;
    TextView connectionStatusView;
    View runState;
    View configureState;
    EditText editBotNameTextView;
    View backButton;
    View settingsButton;
    View blutoothButton;
    String botName;
    SumoBot sumoBot;

    @Override
    public void onSumoBotScanStarted() {
        Log.d(DEBUG_TAG, "Scan for sumobot has started");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                connectionStatusView.setText("SCANNING...");
            }
        });
    }

    @Override
    public void onSumoBotConnected() {

        Log.d(DEBUG_TAG, "Connected to SumoBot");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                connectionStatusView.setText("CONNECTED");
            }
        });
    }

    @Override
    public void onSumoBotDisconnected() {
        Log.d(DEBUG_TAG, "Disconnected from SumoBot");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                connectionStatusView.setText("DISCONNECTED");
            }
        });

    }

    @Override
    public void onSumoBotRequestsBluetoothEnabled() {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        sumoBot = new SumoBot(getApplicationContext(), BluetoothAdapter.getDefaultAdapter(), this);
        botNameView = (TextView) findViewById(R.id.sumo_bot_name);
        runState = findViewById(R.id.run_state);
        configureState = findViewById(R.id.configure_state);
        editBotNameTextView = (EditText) findViewById(R.id.set_bot_name);
        backButton = findViewById(R.id.configure_state_options);
        backButton.setOnClickListener(new BackButtonOnClickListener());
        settingsButton = findViewById(R.id.settings);
        settingsButton.setOnClickListener(new SettingsButtonOnClickListener());
        blutoothButton = findViewById(R.id.bluetooth);
        findViewById(R.id.gpio_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sumoBot.gpioPressed();
            }
        });
        leftLateralView = (LateralView) findViewById(R.id.left_lateral);
        leftLateralView.setOnLaterViewChangeListener(new OnLaterViewChangeListener() {
            @Override
            public void onMoveForward(int percentage) {
                Log.d(DEBUG_TAG, "left wheel changed moving forward: " + percentage);
                sumoBot.moveLeftWheelForward(percentage);
            }

            @Override
            public void onMoveBackward(int percentage) {
                Log.d(DEBUG_TAG, "left wheel changed moving backward: " + percentage);
                sumoBot.moveLeftWheelBackward(percentage);
            }

            @Override
            public void onMoveStop() {
                Log.d(DEBUG_TAG, "left wheel stopped moving");
                sumoBot.stopLeftWheel();
            }
        });

        rightLateralView = (LateralView) findViewById(R.id.right_lateral);
        rightLateralView.setOnLaterViewChangeListener(new OnLaterViewChangeListener() {
            @Override
            public void onMoveForward(int percentage) {
                Log.d(DEBUG_TAG, "right wheel changed moving forward: " + percentage);
                sumoBot.moveRightWheelForward(percentage);
            }

            @Override
            public void onMoveBackward(int percentage) {
                Log.d(DEBUG_TAG, "right wheel changed moving backward: " + percentage);
                sumoBot.moveRightWheelBackward(percentage);
            }

            @Override
            public void onMoveStop() {
                Log.d(DEBUG_TAG, "right wheel stopped moving");
                sumoBot.stopRightWheel();
            }

        });
        connectionStatusView = (TextView) findViewById(R.id.connection_status);
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
    protected void onPause() {
        super.onPause();
        if (sumoBot.connected()) {
            sumoBot.disConnect();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_ENABLE_BT) {
            connectToBot(botName);
        }
    }

    private void connectToBot(String botName) {
        if (sumoBot.connected()) {
            sumoBot.disConnect();
        }

        if (hasBotName(botName)) {
            sumoBot.setName(botName);
            botNameView.setText(botName);
            sumoBot.connect();
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
        showRunState();
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
            if (!editBotNameTextView.getText().toString().isEmpty()) {
                saveName();
            }
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
