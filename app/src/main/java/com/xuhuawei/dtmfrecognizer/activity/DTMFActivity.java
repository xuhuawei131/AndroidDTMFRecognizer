package com.xuhuawei.dtmfrecognizer.activity;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.xuhuawei.dtmfrecognizer.OnReconzerHelperListener;
import com.xuhuawei.dtmfrecognizer.R;
import com.xuhuawei.dtmfrecognizer.dtmfhelper.Controller;
import com.xuhuawei.dtmfrecognizer.dtmfhelper.Spectrum;
import com.xuhuawei.dtmfrecognizer.dtmfhelper.util.Apphance;

public class DTMFActivity extends AppCompatActivity {
    public static final String APP_KEY = "806785c1fb7aed8a867039282bc21993eedbc4e4";
    private Controller controller;
    private TextView text_result;
    private Button stateButton;
    private StringBuilder stringBuilder=new StringBuilder();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Apphance.start(this,APP_KEY);
        setContentView(R.layout.activity_dtmf);

        controller = new Controller(this,mOnReconzerHelperListener);


        text_result=findViewById(R.id.text_result);
        stateButton= (Button) this.findViewById(R.id.stateButton);
        stateButton.setOnClickListener(mOnClickListener);

        Button clearButton = (Button) this.findViewById(R.id.clearButton);
        clearButton.setOnClickListener(mOnClickListener);
    }
    @Override
    protected void onPause() {
        if (controller.isStarted())
            controller.changeState();
        super.onPause();
    }

    private View.OnClickListener mOnClickListener=new View.OnClickListener(){

        @Override
        public void onClick(View v) {
            if (v.getId()==R.id.stateButton){
                controller.changeState();
            }else if (v.getId()==R.id.clearButton){
                stringBuilder=new StringBuilder();
                text_result.setText("");
            }
        }
    };

    private OnReconzerHelperListener mOnReconzerHelperListener=new OnReconzerHelperListener(){
        @Override
        public void start() {
            stateButton.setText(R.string.stop);
        }
        @Override
        public void stop() {
            stateButton.setText(R.string.start);
        }
        @Override
        public void addText(char key) {
            Log.v("xhw","key="+key);
            stringBuilder.append(key);
            text_result.setText(stringBuilder.toString());
        }

        @Override
        public void drawSpectrum(Spectrum spectrum) {

        }
    };

}
