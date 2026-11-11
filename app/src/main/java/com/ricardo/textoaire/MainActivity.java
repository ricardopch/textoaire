package com.ricardo.textoaire;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * Envuelve la app web (assets/index.html) en un WebView y le entrega el giro
 * alrededor del eje vertical real, calculado con el giroscopio y la gravedad.
 */
public class MainActivity extends Activity implements SensorEventListener {

    private WebView web;
    private SensorManager sm;
    private Sensor gyro, grav;
    private final float[] g = {0f, 1f, 0f};
    private long lastTs = 0, lastPush = 0;
    private double acc = 0;                    // grados acumulados pendientes de enviar
    private volatile boolean active = false;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        web = new WebView(this);
        web.setBackgroundColor(0xFF000000);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        web.setWebViewClient(new WebViewClient());
        web.setWebChromeClient(new WebChromeClient());
        web.addJavascriptInterface(new Bridge(), "AndroidSensors");
        setContentView(web);
        web.loadUrl("file:///android_asset/index.html");

        sm = (SensorManager) getSystemService(SENSOR_SERVICE);
        gyro = sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        grav = sm.getDefaultSensor(Sensor.TYPE_GRAVITY);
        if (grav == null) grav = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    /** Lo llama el JavaScript de la página (en un hilo secundario). */
    private class Bridge {
        @JavascriptInterface
        public void setActive(boolean on) {
            active = on;
            lastTs = 0;
            acc = 0;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (gyro != null) sm.registerListener(this, gyro, SensorManager.SENSOR_DELAY_GAME);
        if (grav != null) sm.registerListener(this, grav, SensorManager.SENSOR_DELAY_GAME);
        web.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        sm.unregisterListener(this);
        web.onPause();
    }

    @Override
    public void onSensorChanged(SensorEvent e) {
        int t = e.sensor.getType();
        if (t == Sensor.TYPE_GRAVITY || t == Sensor.TYPE_ACCELEROMETER) {
            g[0] = e.values[0]; g[1] = e.values[1]; g[2] = e.values[2];
            return;
        }
        if (t != Sensor.TYPE_GYROSCOPE || !active) return;
        if (lastTs != 0) {
            double dt = (e.timestamp - lastTs) * 1e-9;
            double n = Math.sqrt(g[0] * g[0] + g[1] * g[1] + g[2] * g[2]);
            if (n > 1e-3 && dt > 0 && dt < 0.1) {
                // velocidad angular (rad/s) proyectada sobre el eje "arriba" (opuesto a la gravedad)
                double yaw = (e.values[0] * g[0] + e.values[1] * g[1] + e.values[2] * g[2]) / n;
                double degPerSec = Math.toDegrees(yaw);
                if (Math.abs(degPerSec) < 0.25) degPerSec = 0;     // zona muerta contra deriva
                acc += degPerSec * dt;
            }
        }
        lastTs = e.timestamp;
        long now = System.nanoTime();
        if (now - lastPush >= 12_000_000L && acc != 0) {          // ~80 envíos/s como máximo
            final String js = "window.nativeYaw&&window.nativeYaw(" + acc + ")";
            acc = 0;
            lastPush = now;
            web.evaluateJavascript(js, null);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    @Override
    public void onBackPressed() {
        // Si se está mostrando el letrero, vuelve al editor; si no, cierra la app.
        web.evaluateJavascript("window.teaBack ? window.teaBack() : false", value -> {
            if (!"true".equals(value)) finish();
        });
    }
}
