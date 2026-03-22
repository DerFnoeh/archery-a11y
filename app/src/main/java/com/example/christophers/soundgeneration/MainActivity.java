package com.example.christophers.soundgeneration;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.media.AudioManager;
import android.os.Build;
import android.Manifest;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.example.christophers.soundgeneration.Configuration.ConfigurationActivity;
import com.example.christophers.soundgeneration.SoundPipeline.BluetoothHandler;
import com.example.christophers.soundgeneration.SoundPipeline.GeneratorHandler;
import com.example.christophers.soundgeneration.SoundPipeline.SoundHandler;

import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

public class MainActivity extends AppCompatActivity {
    private int preferredVolume = 100;
    private int sampleRate = 8000;  //currently not used will be set by onCreate
    private int baseFreq = 80;
    private int spectrum = 256;

    private Toast lastToast;

    private SoundHandler gHandler;
    public BluetoothHandler btHandler;
    private ProfileHandler profileHandler;

    private Context currentContext;

    private final LinkedBlockingQueue<byte[]> soundQueue = new LinkedBlockingQueue<byte[]>(1);

    TextView textViewBullseyePercent;
    SeekBar seekBarBullseye;
    TextView textViewTrimmingPosition;
    SeekBar seekBarY;
    SeekBar seekBarX;
    SeekBar seekBarStereoTrimming;
    SeekBar seekBarBaseFreq;
    Switch switchBullseye;
    ProgressBar progressBarSignal;

    boolean axisSimulatorShown = false; //determines wether or not you see the sliders to simulate a connected bow

    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        currentContext = getApplicationContext();
        getSupportActionBar().setIcon(R.mipmap.ic_launcher);    //these 2 lines enable the app icon in the top left corner
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE);

        profileHandler = new ProfileHandler(currentContext, this);


        int latency=0;
        int framesPerBufferInt=0;
        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        PackageManager pm = this.getPackageManager();
        Boolean proAud = false;
        Boolean lowL=false;
        try{
            Method m = am.getClass().getMethod("getOutputLatency", int.class);
            latency = (Integer)m.invoke(am, AudioManager.STREAM_MUSIC);
            String sampleRateStr = am.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE);
            sampleRate = Integer.parseInt(sampleRateStr);
            if(sampleRate==0) sampleRate=44100; //common value. Moto G4 uses 48000
            String framesPerBuffer = am.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER);
            framesPerBufferInt = Integer.parseInt(framesPerBuffer);
            if (framesPerBufferInt == 0) framesPerBufferInt = 256; // common value. Moto G4 uses 240
            lowL = pm.hasSystemFeature(PackageManager.FEATURE_AUDIO_LOW_LATENCY);
            proAud = pm.hasSystemFeature(PackageManager.FEATURE_AUDIO_PRO);
        }catch(Exception e){
        }
        Log.d("Delay", "Latency "+latency+" sampleRate: "+sampleRate+" framesPerBuffer: "+framesPerBufferInt);
        Log.d("Delay", "ProAudio: "+proAud+" LowL: "+lowL);
        gHandler = new GeneratorHandler(soundQueue, sampleRate, baseFreq, spectrum, framesPerBufferInt, this);
        ((ApplicationDelegate)this.getApplicationContext()).btHandler.setResources(gHandler, this);
        btHandler = ((ApplicationDelegate)this.getApplicationContext()).btHandler;
        gHandler.invertX();


        linkViewElements();

        hideBullseyeView();
        hideAxisEmulator();

        createTrimmingButtons();
        generateViewListeners();

        setZProgressBar(0);
    }

    @Override
    protected void onPause() {
        super.onPause();
        gHandler.stopSound();
    }

    @Override
    protected void onResume() {
        super.onResume();
        gHandler.changeVol(0);
        gHandler.startSound();
    }

    private void linkViewElements() {
        seekBarBullseye = (SeekBar) findViewById(R.id.seekBarBullseye);
        textViewBullseyePercent = (TextView) findViewById(R.id.textViewBullseyeSize);
        seekBarY = (SeekBar) findViewById(R.id.seekBarY);
        seekBarX = (SeekBar) findViewById(R.id.seekBarX);
        seekBarStereoTrimming = (SeekBar) findViewById(R.id.seekBarStereoTrimming);
        seekBarBaseFreq = (SeekBar) findViewById(R.id.seekBarBaseFreq);
        switchBullseye = (Switch) findViewById(R.id.switchBullsyeye);
        progressBarSignal = (ProgressBar) findViewById(R.id.progressBar_Signal);
    }

    private void hideBullseyeView() {
        textViewBullseyePercent.setVisibility(View.INVISIBLE);
        seekBarBullseye.setVisibility(View.INVISIBLE);
    }

    private void hideAxisEmulator() {
        seekBarY.setVisibility(View.INVISIBLE);
        findViewById(R.id.seekBarX).setVisibility(View.INVISIBLE);
        findViewById(R.id.textViewAxisEmulator).setVisibility(View.INVISIBLE);
        findViewById(R.id.textViewFrequenzy).setVisibility(View.INVISIBLE);
        findViewById(R.id.textViewStereo).setVisibility(View.INVISIBLE);
    }

    private void showAxisEmulator() {
        findViewById(R.id.seekBarY).setVisibility(View.VISIBLE);
        findViewById(R.id.seekBarX).setVisibility(View.VISIBLE);
        findViewById(R.id.textViewAxisEmulator).setVisibility(View.VISIBLE);
        findViewById(R.id.textViewFrequenzy).setVisibility(View.VISIBLE);
        findViewById(R.id.textViewStereo).setVisibility(View.VISIBLE);
    }


    private void createTrimmingButtons() {
        int buttonElevation = 5;
        TableLayout trimmingLayout = (TableLayout) findViewById(R.id.tableLayout);
        trimmingLayout.setGravity(Gravity.CENTER_HORIZONTAL);

        TableRow row1 = new TableRow(trimmingLayout.getContext());
        TableRow row2 = new TableRow(trimmingLayout.getContext());
        TableRow row3 = new TableRow(trimmingLayout.getContext());

        TextView fireAndForget;

        //ROW 1
        fireAndForget = new TextView(trimmingLayout.getContext());
        String input = getString(R.string.trimmingbuttons_lable);
        fireAndForget.setText(Html.fromHtml(input));
        row1.addView(fireAndForget);
        ImageButton redoButton = new ImageButton(trimmingLayout.getContext());
        redoButton.setElevation(buttonElevation);
        redoButton.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.arrow_reset, null));
        row1.addView(redoButton);

        ImageButton topButton = new ImageButton(trimmingLayout.getContext());
        topButton.setElevation(buttonElevation);
        topButton.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.arrow_up, null));
        row1.addView(topButton);

        fireAndForget = new TextView(trimmingLayout.getContext());
        fireAndForget.setText(R.string.deepLeft_TrimmingButton);
        fireAndForget.setGravity(Gravity.BOTTOM);
        row1.addView(fireAndForget);

        //ROW 2
        fireAndForget = new TextView(trimmingLayout.getContext());
        row2.addView(fireAndForget);
        ImageButton leftButton = new ImageButton(trimmingLayout.getContext());
        leftButton.setElevation(buttonElevation);
        leftButton.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.arrow_left, null));
        row2.addView(leftButton);
        textViewTrimmingPosition = new TextView(trimmingLayout.getContext());
        //textViewTrimmingPosition.setText("\nX: 0\nY: 0");
        updateTrimmingText();   //requests the actual current trimming settings from btHandler
        textViewTrimmingPosition.setGravity(Gravity.CENTER);
        row2.addView(textViewTrimmingPosition);
        ImageButton rightButton = new ImageButton(trimmingLayout.getContext());
        rightButton.setElevation(buttonElevation);
        rightButton.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.arrow_right, null));
        row2.addView(rightButton);

        //ROW 3
        fireAndForget = new TextView(trimmingLayout.getContext());
        row3.addView(fireAndForget);
        fireAndForget = new TextView(trimmingLayout.getContext());
        fireAndForget.setGravity(Gravity.RIGHT);
        fireAndForget.setText(R.string.rightUp_trimmingButton);
        row3.addView(fireAndForget);
        ImageButton downButton = new ImageButton(trimmingLayout.getContext());
        downButton.setElevation(buttonElevation);
        downButton.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.arrow_down, null));
        row3.addView(downButton);
        fireAndForget = new TextView(trimmingLayout.getContext());
        fireAndForget.setText(R.string.leftDeep_TrimmingButton);
        row3.addView(fireAndForget);

        trimmingLayout.addView(row1);
        trimmingLayout.addView(row2);
        trimmingLayout.addView(row3);

        topButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btHandler.yOffset++;
                if(btHandler.yOffset>127) btHandler.yOffset = 127;
                textViewTrimmingPosition.setText("\nX: " + btHandler.xOffset + "\nY: " + btHandler.yOffset);
            }
        });
        rightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btHandler.xOffset--;
                if(btHandler.xOffset < -127) btHandler.xOffset = -127;
                textViewTrimmingPosition.setText("\nX: " + btHandler.xOffset + "\nY: " + btHandler.yOffset);
            }
        });
        downButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btHandler.yOffset--;
                if(btHandler.yOffset<-127) btHandler.yOffset = -127;
                textViewTrimmingPosition.setText("\nX: " + btHandler.xOffset + "\nY: " + btHandler.yOffset);
            }
        });
        leftButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btHandler.xOffset++;
                if(btHandler.xOffset>127) btHandler.xOffset = 127;
                textViewTrimmingPosition.setText("\nX: " + btHandler.xOffset + "\nY: " + btHandler.yOffset);
            }
        });

        redoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btHandler.xOffset = 0;
                btHandler.yOffset = 0;
                textViewTrimmingPosition.setText("\nX: " + btHandler.xOffset + "\nY: " + btHandler.yOffset);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        int id = item.getItemId();
        if (id == R.id.invertY) {
            gHandler.invertY();
            toastMessage(getString(R.string.up_down_swap_OptionsMenu));
            return true;
        } else if (id == R.id.invertX) {
            gHandler.invertX();
            toastMessage(getString(R.string.left_right_swap_OptionsMenu_toast));
            return true;
        } else if (id == R.id.loadProfile) {
            loadProfileDialog();
            return true;
        } else if (id == R.id.saveProfile) {
            saveProfileDialog();
            return true;
        } else if (id == R.id.axisSimulateToggle) {
            axisSimulatorShown = !axisSimulatorShown;
            if (axisSimulatorShown) {
                showAxisEmulator();
                toastMessage(getString(R.string.simulate_Bow_Options_Menu_Toast));
                gHandler.startSound();
                gHandler.changeVol(preferredVolume);
            } else {
                hideAxisEmulator();
                gHandler.stopSound();
            }
            return true;
        } else if (id == R.id.deleteProfile) {
            deleteProfileDialog();
            return true;
        } else if (id == R.id.about) {
            showAboutDialog();
            return true;
        } else if (id == R.id.recalibrate) {
            btHandler.recalibrate();
            return true;
        } else if (id == R.id.configureMicrochip) {
            gHandler.stopSound();
            Intent intent = new Intent(this, ConfigurationActivity.class);
            startActivity(intent);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private void generateViewListeners() {
        seekBarY.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                gHandler.changeY(progress + btHandler.yOffset);
                gHandler.changeVol(preferredVolume);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        switchBullseye.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    toastMessage(getString(R.string.bullseye_on_explanation_toast));
                    textViewBullseyePercent.setVisibility(View.VISIBLE);
                    seekBarBullseye.setVisibility(View.VISIBLE);
                    if (!gHandler.toggleBullseye()) gHandler.toggleBullseye();
                } else {
                    toastMessage(getString(R.string.bullseye_off_explanation_toast));
                    textViewBullseyePercent.setVisibility(View.INVISIBLE);
                    seekBarBullseye.setVisibility(View.INVISIBLE);
                    if (gHandler.toggleBullseye()) gHandler.toggleBullseye();
                }
            }
        });

        seekBarBullseye.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                double i = (double) (progress * 2 + 1) / 255 * 100;
                String text = getString(R.string.middle_size_bullseye_seekbar_lable) + new DecimalFormat("##.#").format(i) + "%";
                textViewBullseyePercent.setText(text);
                gHandler.setBullseyeRadius(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                toastMessage(getString(R.string.bullseye_size_explanation_toast));
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        seekBarBaseFreq.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                gHandler.setBaseFreq(progress + 35);
                gHandler.changeY(0);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                btHandler.canControlSound = false;
                gHandler.startSound();
                gHandler.changeVol(preferredVolume);
                gHandler.changeX(127);
                gHandler.changeY(0);
                toastMessage(getString(R.string.choose_base_tone_explanation_toast));
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                btHandler.canControlSound = true;
                if (!axisSimulatorShown) gHandler.changeVol(0);
                else {
                    gHandler.changeY(seekBarY.getProgress());
                    gHandler.changeX(seekBarX.getProgress());
                }
            }
        });

        seekBarStereoTrimming.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float balancePerc = 1.0f - (float) progress * 0.01f;
                gHandler.trimBalance(balancePerc);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                btHandler.canControlSound = false;
                gHandler.startSound();
                gHandler.changeVol(preferredVolume);
                gHandler.changeX(127);
                gHandler.changeY(0);
                toastMessage(getString(R.string.balance_explanation_toast));
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                btHandler.canControlSound = true;
                gHandler.changeVol(0);
            }
        });

        seekBarX.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                gHandler.changeX(progress + btHandler.xOffset);
                gHandler.changeVol(preferredVolume);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        Button bluetoothConnectButton = (Button) findViewById(R.id.buttonBluetooth);
        bluetoothConnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectBluetoothWithPermissionCheck();
            }
        });

        Button bluetoothCloseButton = (Button) findViewById(R.id.buttonCloseBluetooth);
        bluetoothCloseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setZProgressBar(0);
                gHandler.stopSound();
                btHandler.closeBT(true);
            }
        });
    }

    private void connectBluetoothWithPermissionCheck() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ requires runtime permissions for Bluetooth
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN},
                    REQUEST_BLUETOOTH_PERMISSIONS);
                return;
            }
        }
        btHandler.connectAndRead();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                btHandler.connectAndRead();
            } else {
                toastMessage("Bluetooth-Berechtigung wird benötigt um den Bogen zu verbinden.");
            }
        }
    }

    public BluetoothDevice choice = null;

    public BluetoothDevice chooseFromDevicesDialog(Set<BluetoothDevice> devicesSet) {
        if (devicesSet.isEmpty()){
            toastMessage(getString(R.string.pleasePair_noDevice_toast));
            return null;
        }
        final Set<BluetoothDevice> devices = devicesSet;
        final Semaphore waitForUser = new Semaphore(0);
        choice = null;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final Object[] btDevices = devices.toArray();
                String[] options = new String[btDevices.length];
                for (int i = 0; i < btDevices.length; i++) {
                    BluetoothDevice curDevice = (BluetoothDevice) btDevices[i];
                    options[i] = curDevice.getName() + "  \t" + curDevice.getAddress();
                }

                AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
                dialog.setCancelable(false);
                dialog.setTitle(R.string.paired_devices_dialog_header);
                dialog.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        toastMessage(getString(R.string.pleasePair_aborted_toast));
                        dialog.cancel();
                        waitForUser.release();
                    }
                });
                dialog.setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        choice = (BluetoothDevice) btDevices[which];
                        waitForUser.release();
                    }
                }).show();
            }
        });

        try {
            waitForUser.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return choice;
    }

    private void saveProfileDialog() {
        profileHandler.getAvailableProfiles();
        final Object[] profiles = profileHandler.getAvailableProfiles().toArray();
        if (profiles.length == 0) {
            toastMessage(getString(R.string.profile_load_error_toast));
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String[] names = new String[profiles.length];   //default profile will not be shown but a "new profile" Option
                    for (int i = 1; i < profiles.length; i++) {
                        names[i - 1] = ((Profile) profiles[i]).name;
                    }
                    names[profiles.length - 1] = getString(R.string.new_profile_option_inDialog);
                    final String[] options = names;

                    AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
                    dialog.setTitle(R.string.save_settings_as);
                    dialog.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
                    dialog.setItems(options, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (which == profiles.length - 1) {   //New profile was choosen
                                createNewProfileDialog();
                            } else {   //an existing profile was chosen
                                String profileName = options[which];
                                Profile newProfile = new Profile(seekBarStereoTrimming.getProgress(), seekBarBaseFreq.getProgress(), switchBullseye.isChecked(), seekBarBullseye.getProgress(), btHandler.xOffset, btHandler.yOffset, profileName, gHandler.xIsInverted(), gHandler.yIsInverted());
                                profileHandler.overwriteProfile(newProfile);    //the old one with the same name will be overwritten
                                toastMessage(getString(R.string.saved_settings_in_toast_1) + profileName + getString(R.string.saved_settings_in_toast_2));
                            }
                            dialog.cancel();
                        }
                    }).show();
                }
            });
        }
    }

    public void createNewProfileDialog() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle(R.string.profile_name_createProfile_Dialog);
                final EditText input = new EditText(MainActivity.this);
                input.setInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS);
                builder.setView(input);
                builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String profileName = input.getText().toString().trim();
                        if (profileName != null) {
                            Profile newProfile = new Profile(seekBarStereoTrimming.getProgress(), seekBarBaseFreq.getProgress(), switchBullseye.isChecked(), seekBarBullseye.getProgress(), btHandler.xOffset, btHandler.yOffset, profileName, gHandler.xIsInverted(), gHandler.yIsInverted());
                            if (profileHandler.storeProfile(newProfile)) {
                                toastMessage(getString(R.string.profile_wasSaved_toast_1) + profileName + getString(R.string.profile_wasSaved_toast2));
                            } else
                                toastMessage(getString(R.string.profile_allready_exists_toast_1) + profileName + getString(R.string.profile_allready_exists_toast_2));
                        } else {
                            toastMessage(getString(R.string.profileNotSaved_NoNameGiven_Toast));
                        }
                        dialog.cancel();
                    }
                });
                builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        toastMessage(getString(R.string.profileNotSaved_toast));
                        dialog.cancel();
                    }
                });

                AlertDialog dialog = builder.create();
                dialog.getWindow().setSoftInputMode(
                        WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                dialog.show();
            }
        });
    }

    private void loadProfileDialog() {
        profileHandler.getAvailableProfiles();
        final Object[] profiles = profileHandler.getAvailableProfiles().toArray();
        if (profiles.length == 0)
            toastMessage(getString(R.string.profilesNotAvailable_toast));
        else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String[] names = new String[profiles.length];   //default profile will not be shown
                    for (int i = 0; i < profiles.length; i++) {
                        names[i] = ((Profile) profiles[i]).name;
                    }
                    final String[] options = names;

                    AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
                    dialog.setTitle(R.string.loadProfile_dialogTitle);
                    dialog.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
                    dialog.setItems(options, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            getSettingsFromProfile((Profile) profiles[which]);
                            dialog.cancel();
                        }
                    }).show();
                }
            });
        }
    }

    private void getSettingsFromProfile(Profile profile) {
        //Top two slider
        seekBarStereoTrimming.setProgress(profile.balance);
        float balancePerc = 1.0f - (float) profile.balance * 0.01f;
        gHandler.trimBalance(balancePerc);

        seekBarBaseFreq.setProgress(profile.baseToneHeight);
        gHandler.setBaseFreq(profile.baseToneHeight + 35);

        //Bullseye fields
        switchBullseye.setChecked(profile.bullseyeMode);
        double i = (double) (profile.bullseyeSize * 2 + 1) / 255 * 100;
        String text = getString(R.string.middle_size_bullseye_seekbar_lable) +  new DecimalFormat("##.#").format(i) + "%";
        textViewBullseyePercent.setText(text);
        gHandler.setBullseyeRadius(profile.bullseyeSize);
        seekBarBullseye.setProgress(profile.bullseyeSize);
        if (profile.bullseyeMode) {   //turn it on
            textViewBullseyePercent.setVisibility(View.VISIBLE);
            seekBarBullseye.setVisibility(View.VISIBLE);
            if (!gHandler.toggleBullseye())
                gHandler.toggleBullseye();  //if its not on==true then it will be set again
        } else {                   //turn it off
            textViewBullseyePercent.setVisibility(View.INVISIBLE);
            seekBarBullseye.setVisibility(View.INVISIBLE);
            if (gHandler.toggleBullseye())
                gHandler.toggleBullseye();   //if its not off==false then it will be set again
        }

        //Shot and bow trimming buttons
        btHandler.xOffset = profile.xOffset;
        btHandler.yOffset = profile.yOffset;
        updateTrimmingText();

        if(profile.invertX!=gHandler.xIsInverted()) gHandler.invertX();
        if(profile.invertY!=gHandler.yIsInverted()) gHandler.invertY();

        toastMessage(getString(R.string.settingsLoaded_toast_1)+profile.name+getString(R.string.settingsLoaded_toast_2));
    }

    private void deleteProfileDialog() {
        profileHandler.getAvailableProfiles();
        final Object[] profiles = profileHandler.getAvailableProfiles().toArray();
        if (profiles.length == 0)
            toastMessage(getString(R.string.profile_load_error_toast));
        else if (profiles.length == 1)
            toastMessage(getString(R.string.onlyDefaultProfile_toast));   //only the default Profile
        else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String[] names = new String[profiles.length - 1];   //default profile will not be shown
                    for (int i = 1; i < profiles.length; i++) {
                        names[i - 1] = ((Profile) profiles[i]).name;
                    }
                    final String[] options = names;

                    AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
                    dialog.setTitle(R.string.deleteProfile_dialogTitle);
                    dialog.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
                    dialog.setItems(options, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String profileName = options[which];
                            if (profileHandler.deleteProfile(profileName)) {
                                toastMessage(getString(R.string.profileDeleted_toast_1) + profileName + getString(R.string.profileDeleted_toast_2));
                            } else toastMessage(getString(R.string.noProfileErased_toast));
                            dialog.cancel();
                        }
                    }).show();
                }
            });
        }
    }

    private void showAboutDialog() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
        dialog.setTitle(R.string.about_dialogTitle);
        dialog.setMessage(getString(R.string.about));
        dialog.setPositiveButton(R.string.close_DialogButton, null);
        dialog.show();
    }

    public void toastMessage(final String message) {
        runOnUiThread(new Runnable() {
            public void run() {
                if (lastToast != null) lastToast.cancel();
                lastToast = Toast.makeText(currentContext, message, Toast.LENGTH_LONG);
                lastToast.show();
            }
        });
    }

    public void setZProgressBar(final int progress){
        final int prog = progress;
        runOnUiThread(new Runnable() {
            public void run() {
            progressBarSignal.setProgress(prog);
            progressBarSignal.getProgressDrawable().setColorFilter((0xff << 24 | 0xff*(7-progress)/7 << 16 | 0xff*progress/7 << 8), PorterDuff.Mode.MULTIPLY);
            }
        });
    }

    public void updateTrimmingText(){
        runOnUiThread(new Runnable() {
            public void run() {
                textViewTrimmingPosition.setText("\nX: " + btHandler.xOffset + "\nY: " + btHandler.yOffset);
            }
        });
    }
}
