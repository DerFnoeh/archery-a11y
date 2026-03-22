package com.example.christophers.soundgeneration.Configuration;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.example.christophers.soundgeneration.ApplicationDelegate;
import com.example.christophers.soundgeneration.R;
import com.example.christophers.soundgeneration.SoundPipeline.BluetoothHandler;

public class ConfigurationActivity extends AppCompatActivity implements BluetoothDataHandler {
    private TableLayout tableLayout;
    private BluetoothHandler btHandler;
    private int[] xValues = new int[256];
    private MySimpleChart chart;
    private Toast lastToast;
    private Context currentContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        btHandler = ((ApplicationDelegate) this.getApplicationContext()).btHandler;
        currentContext = getApplicationContext();
        createView();
    }

    private void createView() {
        setContentView(R.layout.activity_configuration);
        tableLayout = (TableLayout) findViewById(R.id.tableLayout);


        chart = new MySimpleChart(this);
        chart.invert = true;
        chart.setClipBounds(new Rect(0, 0, 256, 256));

        Button showRawDataButton = new Button(this);
        showRawDataButton.setText(R.string.showRawData_Button);
        showRawDataButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startRawDataCapture();
            }
        });

        Button invertDataButton = new Button(this);
        invertDataButton.setText("Invertiere");
        invertDataButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chart.invert = !chart.invert;
            }
        });

        Button highGainButton = new Button(this);
        highGainButton.setText(R.string.highGain_button);
        highGainButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btHandler.writeToMicrochip((byte) 16);
            }
        });

        Button lowGainButton = new Button(this);
        lowGainButton.setText(R.string.lowGain_Button);
        lowGainButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btHandler.writeToMicrochip((byte) 15);
            }
        });



        SeekBar delaySeekbar = new SeekBar(this);
        delaySeekbar.setMax(254);
        delaySeekbar.setProgress(25);
        delaySeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            long systemMil = System.currentTimeMillis();
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(System.currentTimeMillis() - systemMil > 200) {
                    btHandler.writeToMicrochip((byte) 17, (byte) (1 + seekBar.getProgress()));
                    systemMil = System.currentTimeMillis();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                btHandler.writeToMicrochip((byte) 17, (byte) (1 + seekBar.getProgress()));
            }
        });

        tableLayout.setGravity(Gravity.CENTER_HORIZONTAL);
        TableRow row1 = new TableRow(tableLayout.getContext());
        row1.addView(chart);
        row1.addView(showRawDataButton);
        row1.addView(invertDataButton);

        TableRow row2 = new TableRow(tableLayout.getContext());
        row2.addView(highGainButton);
        row2.addView(lowGainButton);

        TableRow row3 = new TableRow(tableLayout.getContext());
        TextView delayText = new TextView(this);
        delayText.setText(R.string.delay_Seekbar_lable);
        row3.addView(delayText);

        TableRow row3_2 = new TableRow(tableLayout.getContext());
        TableRow.LayoutParams ob = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.WRAP_CONTENT,
                1);
        row3_2.addView(delaySeekbar, ob);

        TableRow row4 = new TableRow(tableLayout.getContext());
        TextView textViewByte_1 = new TextView(this);
        textViewByte_1.setText("Byte 1");
        TextView textViewByte_2 = new TextView(this);
        textViewByte_2.setText("Byte 2");
        row4.addView(textViewByte_1);
        row4.addView(textViewByte_2);


        TableRow row5 = new TableRow(tableLayout.getContext());
        final EditText numberPicker = new EditText(this);
        numberPicker.setInputType(InputType.TYPE_CLASS_NUMBER);
        InputFilter[] filterArray = new InputFilter[1];
        filterArray[0] = new InputFilter.LengthFilter(3);
        numberPicker.setFilters(filterArray);

        final EditText numberPicker_2 = new EditText(this);
        numberPicker_2.setInputType(InputType.TYPE_CLASS_NUMBER);
        numberPicker_2.setFilters(filterArray);

        Button sendCustomByteButton = new Button(this);
        sendCustomByteButton.setText("Sende Paket");
        sendCustomByteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String input = numberPicker.getText().toString();
                if(input.isEmpty()) input = "0";
                int message_1 = Integer.parseInt(input);
                if(message_1>255) message_1=0;
                input = numberPicker_2.getText().toString();
                if(input.isEmpty()) input = "0";
                int message_2 = Integer.parseInt(input);
                if(message_2>255) message_2=0;
                btHandler.writeToMicrochip((byte)message_1, (byte) message_2);
            }
        });

        row5.addView(numberPicker);
        row5.addView(numberPicker_2);
        row5.addView(sendCustomByteButton);

        tableLayout.setShrinkAllColumns(true);
        tableLayout.setStretchAllColumns(false);
        tableLayout.addView(row1);
        tableLayout.addView(row2);
        tableLayout.addView(row3);
        tableLayout.addView(row3_2);
        tableLayout.addView(row4);
        tableLayout.addView(row5);

        tableLayout.setColumnStretchable(2, true);
    }

    private void startRawDataCapture() {
        btHandler.writeToMicrochip((byte) 18, (byte) 6); //code for microchip to start sending the x-Value Raw Data
        btHandler.forwardIncomingDataTo(this);
    }

    int positionInPicture = 999;    //set to high so the first incoming bytes are not drawn on random position

    @Override
    public void handleData(int readBytes, byte[] data) {
        int i = 0;
        while (i < readBytes) {
            int data_i = unsignedToBytes(data[i]);
            if (data_i == 255) {    //code for the start of a picture
                positionInPicture = 0;
                chart.updateValues(xValues);
            } else if (positionInPicture < 256) {  //if we are still inside the "picture size"(256) and haven't missed a "start picture" command(255)
                xValues[positionInPicture] = data_i;
            }
            positionInPicture++;
            i++;
        }
    }

    public static int unsignedToBytes(byte b) {
        return b & 0xFF;
    }

    class MySimpleChart extends View {
        int[] values = new int[256];
        boolean invert = false;

        public MySimpleChart(Context context) {
            super(context);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            Paint mPaint = new Paint();
            mPaint.setDither(true);
            mPaint.setColor(0xFFFF0000);
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeJoin(Paint.Join.ROUND);
            mPaint.setStrokeCap(Paint.Cap.ROUND);
            mPaint.setStrokeWidth(1);

            canvas.drawColor(Color.CYAN);
            int i = 0;
            while (i < 256) {
                if(invert) canvas.drawLine(255-i, 255, 255-i, 255 - values[i], mPaint);
                else canvas.drawLine(i, 255, i, 255 - values[i], mPaint);
                i++;
            }
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            setMeasuredDimension(256, 256);
        }

        public void updateValues(int[] newValues) {
            if (newValues.length == 256) {
                values = newValues;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        invalidate();
                    }
                });
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        btHandler.unforwardData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        btHandler.unforwardData();
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
}
