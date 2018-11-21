package com.xuhuawei.dtmfrecognizer;

import com.xuhuawei.dtmfrecognizer.dtmfhelper.Spectrum;

public interface OnReconzerHelperListener {

    public void start();
    public void stop();
    public void addText(char key);
    public void drawSpectrum(Spectrum spectrum);
}
