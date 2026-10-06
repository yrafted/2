package com.local.colectivosrosario;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.GeolocationPermissions;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceError;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final int REQ_LOCATION = 41;

    private static final String URL_NEARBY = "https://comollego.rosario.gob.ar/paradas";
    private static final String URL_ARRIVALS = "https://emr.gov.ar/transporte-publico/cuando-llega";
    private static final String URL_LINES = "https://comollego.rosario.gob.ar/lineas";
    private static final String URL_SCHEDULES = "https://datosabiertos.rosario.gob.ar/dataset/0b90de4c-f00f-43ee-8989-949557f919bc";

    private WebView web;
    private ProgressBar progress;
    private TextView status;
    private EditText stopInput;
    private boolean autoLocationAttempted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
        configureWebView();
        ensureLocationPermission();
        loadNearby();
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private TextView makeTitle(String text, int sizeSp, boolean bold) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextColor(Color.WHITE);
        v.setTextSize(sizeSp);
        if (bold) v.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        return v;
    }

    private Button makeButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextSize(14);
        b.setTextColor(Color.rgb(248, 250, 252));
        b.setBackgroundTintList(ColorStateList.valueOf(Color.rgb(30, 41, 59)));
        b.setMinHeight(dp(42));
        b.setPadding(dp(12), 0, dp(12), 0);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(46));
        p.setMargins(dp(4), dp(4), dp(4), dp(4));
        b.setLayoutParams(p);
        return b;
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(7, 11, 20));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(14), dp(10), dp(14), dp(8));
        header.setBackgroundColor(Color.rgb(11, 18, 32));

        TextView title = makeTitle("Colectivos Rosario", 22, true);
        header.addView(title);

        status = makeTitle("Ubicación: comprobando permiso…", 13, false);
        status.setTextColor(Color.rgb(226, 232, 240));
        status.setPadding(0, dp(3), 0, dp(4));
        header.addView(status);

        HorizontalScrollView hsv = new HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false);
        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);

        Button nearby = makeButton("📍 Cerca mío");
        nearby.setOnClickListener(v -> loadNearby());
        buttons.addView(nearby);

        Button arrivals = makeButton("⏱ Cuándo llega");
        arrivals.setOnClickListener(v -> load(URL_ARRIVALS));
        buttons.addView(arrivals);

        Button lines = makeButton("🚌 Líneas");
        lines.setOnClickListener(v -> load(URL_LINES));
        buttons.addView(lines);

        Button schedules = makeButton("🕒 Horarios");
        schedules.setOnClickListener(v -> load(URL_SCHEDULES));
        buttons.addView(schedules);

        Button refresh = makeButton("↻ Actualizar");
        refresh.setOnClickListener(v -> web.reload());
        buttons.addView(refresh);

        hsv.addView(buttons);
        header.addView(hsv);

        LinearLayout stopRow = new LinearLayout(this);
        stopRow.setOrientation(LinearLayout.HORIZONTAL);
        stopRow.setGravity(Gravity.CENTER_VERTICAL);
        stopRow.setPadding(0, dp(3), 0, 0);

        stopInput = new EditText(this);
        stopInput.setSingleLine(true);
        stopInput.setHint("Nº de parada");
        stopInput.setTextColor(Color.WHITE);
        stopInput.setHintTextColor(Color.rgb(203, 213, 225));
        stopInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        stopInput.setBackgroundTintList(ColorStateList.valueOf(Color.rgb(51, 65, 85)));
        stopInput.setBackgroundColor(Color.rgb(15, 23, 42));
        stopInput.setPadding(dp(12), 0, dp(12), 0);
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(0, dp(44), 1f);
        ip.setMargins(dp(4), dp(3), dp(6), dp(3));
        stopInput.setLayoutParams(ip);
        stopRow.addView(stopInput);

        Button go = makeButton("Ver parada");
        go.setOnClickListener(v -> openStop());
        stopRow.addView(go);
        header.addView(stopRow);

        root.addView(header, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        progress.setProgress(0);
        root.addView(progress, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(3)));

        web = new WebView(this);
        root.addView(web, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        TextView footer = new TextView(this);
        footer.setText("No oficial · consulta fuentes públicas de movilidad de Rosario");
        footer.setTextSize(11);
        footer.setTextColor(Color.rgb(203, 213, 225));
        footer.setBackgroundColor(Color.rgb(11, 18, 32));
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(dp(6), dp(4), dp(6), dp(4));
        root.addView(footer, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        setContentView(root);
    }

    private void configureWebView() {
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setGeolocationEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);
        s.setSupportZoom(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            s.setForceDark(WebSettings.FORCE_DARK_ON);
        }
        s.setUserAgentString(s.getUserAgentString() + " ColectivosRosario/1.0");

        web.setBackgroundColor(Color.rgb(7, 11, 20));

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progress.setProgress(newProgress);
                progress.setVisibility(newProgress >= 100 ? View.GONE : View.VISIBLE);
            }

            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                boolean granted = Build.VERSION.SDK_INT < 23 ||
                        checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                        checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
                callback.invoke(origin, granted, false);
                if (!granted) ensureLocationPermission();
            }
        });

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String scheme = uri.getScheme();
                if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) return false;
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, uri));
                } catch (Exception ignored) {}
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                status.setText("Fuente: " + friendlyHost(url));
                if (url != null && url.contains("comollego.rosario.gob.ar/paradas")) {
                    tryAutoLocation();
                }
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                if (request.isForMainFrame()) {
                    String failedUrl = request.getUrl() != null ? request.getUrl().toString() : "";
                    if (failedUrl.startsWith(URL_ARRIVALS)) {
                        status.setText("Cuándo llega no respondió. Abriendo paradas cercanas…");
                        view.postDelayed(() -> loadNearby(), 350);
                    } else {
                        status.setText("No se pudo cargar. Revisá Internet y tocá Actualizar.");
                    }
                }
            }
        });
    }

    private String friendlyHost(String url) {
        try {
            String h = Uri.parse(url).getHost();
            if (h == null) return "web";
            if (h.contains("comollego")) return "¿Cómo llego? Rosario";
            if (h.contains("emr.gov.ar")) return "Ente de la Movilidad de Rosario";
            if (h.contains("datosabiertos")) return "Rosario Datos Abiertos";
            return h;
        } catch (Exception e) {
            return "web";
        }
    }

    private void load(String url) {
        autoLocationAttempted = false;
        web.loadUrl(url);
    }

    private void loadNearby() {
        autoLocationAttempted = false;
        ensureLocationPermission();
        load(URL_NEARBY);
    }

    private void tryAutoLocation() {
        if (autoLocationAttempted) return;
        autoLocationAttempted = true;
        new Handler().postDelayed(() -> {
            String js = "(function(){" +
                    "var els=[].slice.call(document.querySelectorAll('button,a,[role=button],span,div'));" +
                    "for(var i=0;i<els.length;i++){" +
                    "var t=((els[i].innerText||els[i].textContent||'')+'').trim().toLowerCase();" +
                    "if(t==='mi ubicación'||t==='mi ubicacion'||t.indexOf('mi ubicación')>=0){" +
                    "try{els[i].click();return 'clicked';}catch(e){}" +
                    "}}return 'not-found';})();";
            web.evaluateJavascript(js, null);
        }, 1300);
    }

    private void openStop() {
        String n = stopInput.getText().toString().trim();
        if (n.length() == 0 || !n.matches("[0-9]+")) {
            Toast.makeText(this, "Ingresá el número de la parada", Toast.LENGTH_SHORT).show();
            return;
        }
        load("https://comollego.rosario.gob.ar/parada/" + n);
    }

    private void ensureLocationPermission() {
        if (Build.VERSION.SDK_INT < 23) {
            status.setText("Ubicación activada");
            return;
        }
        boolean fine = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean coarse = checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (fine || coarse) {
            status.setText("Ubicación activada · buscando paradas cercanas");
        } else {
            status.setText("Necesita ubicación para ordenar paradas cercanas");
            requestPermissions(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            }, REQ_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_LOCATION) {
            boolean granted = false;
            for (int r : grantResults) if (r == PackageManager.PERMISSION_GRANTED) granted = true;
            if (granted) {
                status.setText("Ubicación activada · buscando paradas cercanas");
                autoLocationAttempted = false;
                web.reload();
            } else {
                status.setText("Ubicación desactivada · podés buscar parada o línea manualmente");
                Toast.makeText(this, "Sin ubicación, la app funciona con búsqueda manual", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (web != null && web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (web != null) {
            web.stopLoading();
            web.destroy();
        }
        super.onDestroy();
    }
}
