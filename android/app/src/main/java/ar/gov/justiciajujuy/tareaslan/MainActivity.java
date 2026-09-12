package ar.gov.justiciajujuy.tareaslan;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import org.json.JSONObject;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final int REQUEST_DICTATION = 21;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private WebView web;
    private Switch alerts;
    private String base = "";
    private boolean updatingSwitch;
    private long apkDownloadId = -1L;
    private String apkDownloadName = "";
    private boolean receiverRegistered;
    private final BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())
                    && intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L) == apkDownloadId) {
                openDownloadedApk();
            }
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        base = getPreferences(MODE_PRIVATE).getString("server", BuildConfig.DEFAULT_SERVER);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            view.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                    insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets;
        });
        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setPadding(12, 4, 12, 4);
        alerts = new Switch(this);
        alerts.setText("Avisos  ");
        alerts.setMinHeight((int) (48 * getResources().getDisplayMetrics().density));
        toolbar.addView(alerts, new LinearLayout.LayoutParams(0, -2, 1));
        TextView version = new TextView(this);
        version.setText("v" + BuildConfig.VERSION_NAME);
        version.setTextColor(Color.DKGRAY);
        version.setGravity(android.view.Gravity.CENTER);
        version.setPadding(8, 0, 8, 0);
        toolbar.addView(version);
        Button settings = new Button(this);
        settings.setText("Ajustes");
        toolbar.addView(settings);
        settings.setOnClickListener(v -> settings());
        alerts.setOnCheckedChangeListener((button, checked) -> {
            if (updatingSwitch) return;
            if (checked) enableAlerts(); else stopService(new Intent(this, AvisosService.class));
        });
        web = new WebView(this);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.getSettings().setAllowFileAccess(false);
        web.getSettings().setAllowContentAccess(false);
        web.getSettings().setGeolocationEnabled(false);
        web.getSettings().setSafeBrowsingEnabled(false);
        web.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        web.getSettings().setUserAgentString(web.getSettings().getUserAgentString()
                + " InventarioLAN/1 InventarioLANVersion/" + BuildConfig.VERSION_NAME);
        web.addJavascriptInterface(new VoiceBridge(), "TareasLan");
        web.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            if (LanClient.sameOrigin(base, url) && Uri.parse(url).getPath().equals("/api/v1/movil/apk")) {
                downloadApkUpdate();
            }
        });
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, false);
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (!LanClient.sameOrigin(base, request.getUrl().toString())) return true;
                String path = request.getUrl().getPath();
                if ("/".equals(path) || (path != null && path.startsWith("/admin"))) {
                    view.loadUrl(base + "/movil/tareas");
                    return true;
                }
                return false;
            }
            @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                if (LanClient.sameOrigin(base, request.getUrl().toString())) return null;
                return new WebResourceResponse("text/plain", "UTF-8", new ByteArrayInputStream(new byte[0]));
            }
            @Override public void onPageFinished(WebView view, String url) {
                String path = Uri.parse(url).getPath();
                if ("/movil/login".equals(path) || "/login".equals(path) || "/".equals(path)
                        || (path != null && path.startsWith("/admin"))) {
                    stopService(new Intent(MainActivity.this, AvisosService.class));
                    setAlerts(false);
                    if ("/".equals(path) || "/login".equals(path)) web.loadUrl(base + "/movil/login");
                    else if (path.startsWith("/admin")) web.loadUrl(base + "/movil/tareas");
                }
            }
        });
        root.addView(toolbar);
        root.addView(web, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
        if (base.isEmpty()) configureServer(); else loadTask(getIntent().getLongExtra("tareaId", 0));
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        loadTask(intent.getLongExtra("tareaId", 0));
    }

    @Override protected void onResume() {
        super.onResume();
        if (alerts != null) setAlerts(AvisosService.running);
    }

    private void setAlerts(boolean enabled) {
        updatingSwitch = true;
        alerts.setChecked(enabled);
        updatingSwitch = false;
    }

    private void loadTask(long id) {
        if (base.isEmpty()) return;
        worker.execute(() -> {
            try {
                LanClient.requireLan(base);
                // La APK es el modo operativo del tecnico: siempre aterriza en la pantalla movil.
                runOnUiThread(() -> web.loadUrl(base + "/movil/tareas" + (id > 0 ? "?tarea=" + id : "")));
            } catch (Exception e) { runOnUiThread(() -> toast("No se pudo conectar al servidor de la intranet. Revise Ajustes.")); }
        });
    }

    private void configureServer() {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_URI);
        input.setHint(BuildConfig.DEBUG ? "http://192.168.1.8:8081" : "https://inventario.interno");
        input.setText(base);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Servidor de tareas").setView(input)
                .setPositiveButton("Abrir tareas", null).setNegativeButton("Cancelar", null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            try {
                String next = LanClient.normalize(input.getText().toString());
                stopService(new Intent(this, AvisosService.class)); setAlerts(false);
                base = next;
                getPreferences(MODE_PRIVATE).edit().putString("server", base).apply();
                web.clearHistory();
                dialog.dismiss(); loadTask(0);
            } catch (Exception e) { input.setError(e.getMessage()); }
        }));
        dialog.show();
    }

    private void settings() {
        if (base.isEmpty()) {
            showSettings("Buscar actualizacion", "Servidor no configurado.");
            return;
        }
        worker.execute(() -> {
            String updateLabel = "Buscar actualizacion";
            String subtitle = "Instalada v" + BuildConfig.VERSION_NAME;
            try {
                JSONObject apk = LanClient.get(base, "/api/v1/movil/apk/info");
                String published = apk.optString("version", "");
                // La comparacion se hace por version legible del servidor; Android igual valida la firma al instalar.
                boolean available = apk.optBoolean("disponible") && !published.isBlank()
                        && !published.equalsIgnoreCase(BuildConfig.VERSION_NAME);
                updateLabel = available ? "Actualizar a v" + published : "Buscar actualizacion";
                subtitle = available ? "Instalada v" + BuildConfig.VERSION_NAME + " | publicada v" + published
                        : "Instalada v" + BuildConfig.VERSION_NAME;
            } catch (Exception ignored) {
                subtitle = "Instalada v" + BuildConfig.VERSION_NAME + " | sin conexion de diagnostico";
            }
            String finalUpdateLabel = updateLabel;
            String finalSubtitle = subtitle;
            runOnUiThread(() -> showSettings(finalUpdateLabel, finalSubtitle));
        });
    }

    private void showSettings(String updateLabel, String subtitle) {
        String[] options = { "Servidor", "Diagnostico", updateLabel, "Bateria", "Notificaciones", "Probar sonido", "Recargar tareas" };
        new AlertDialog.Builder(this).setTitle("Tareas LAN").setMessage(subtitle).setItems(options, (dialog, which) -> {
            switch (which) {
                case 0 -> configureServer();
                case 1 -> diagnostics();
                case 2 -> downloadApkUpdate();
                case 3 -> openBatterySettings();
                case 4 -> startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName()));
                case 5 -> AvisosService.testSound(this);
                case 6 -> loadTask(0);
            }
        }).show();
    }

    private void diagnostics() {
        if (base.isEmpty()) {
            toast("Configure el servidor de la intranet.");
            return;
        }
        worker.execute(() -> {
            String message;
            try {
                JSONObject session = LanClient.get(base, "/api/v1/movil/sesion");
                JSONObject apk = LanClient.get(base, "/api/v1/movil/apk/info");
                JSONObject user = session.getJSONObject("usuario");
                String sha = apk.optString("sha256", "");
                message = "Servidor: " + base
                        + "\nUsuario: " + user.optString("username", "sin sesion")
                        + "\nEditar tareas: " + yesNo(session.optBoolean("puedeEditar"))
                        + "\nAvisos activos: " + yesNo(AvisosService.running)
                        + "\nVersion instalada: " + BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")"
                        + "\nAPK publicada: " + (apk.optBoolean("disponible") ? apk.optString("nombre", "disponible") : "no disponible")
                        + "\nVersion publicada: " + apk.optString("version", "sin dato")
                        + "\nTamano APK: " + formatBytes(apk.optLong("bytes", 0))
                        + "\nSHA-256: " + (sha.length() > 16 ? sha.substring(0, 16) + "..." : sha);
            } catch (Exception e) {
                message = "No se pudo completar el diagnostico.\n\nRevise servidor, red, sesion y permisos.";
            }
            String result = message;
            runOnUiThread(() -> new AlertDialog.Builder(this).setTitle("Diagnostico Tareas LAN")
                    .setMessage(result).setPositiveButton("Cerrar", null).show());
        });
    }

    private void downloadApkUpdate() {
        if (base.isEmpty()) {
            toast("Configure el servidor de la intranet.");
            return;
        }
        worker.execute(() -> {
            try {
                JSONObject apk = LanClient.get(base, "/api/v1/movil/apk/info");
                if (!apk.optBoolean("disponible")) throw new IllegalStateException("APK no disponible.");
                String fileName = apk.optString("nombre", "tecnico-taller-san-pedro-lan-release.apk");
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(base + "/api/v1/movil/apk"));
                request.setTitle("Tareas LAN");
                request.setDescription("Descargando actualizacion piloto");
                request.setMimeType("application/vnd.android.package-archive");
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                request.setDestinationInExternalFilesDir(this, Environment.DIRECTORY_DOWNLOADS, fileName);
                request.addRequestHeader("Accept", "application/vnd.android.package-archive");
                String cookies = CookieManager.getInstance().getCookie(base);
                if (cookies != null) request.addRequestHeader("Cookie", cookies);
                DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                apkDownloadName = fileName;
                apkDownloadId = manager.enqueue(request);
                ensureDownloadReceiver();
                runOnUiThread(() -> toast("Descarga iniciada. Al finalizar se abrira el instalador."));
            } catch (Exception e) {
                runOnUiThread(() -> toast("Ingrese al sistema y verifique permisos antes de descargar la actualizacion."));
            }
        });
    }

    private void ensureDownloadReceiver() {
        if (receiverRegistered) return;
        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(downloadReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(downloadReceiver, filter);
        receiverRegistered = true;
    }

    private void openDownloadedApk() {
        worker.execute(() -> {
            try {
                DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                try (Cursor cursor = manager.query(new DownloadManager.Query().setFilterById(apkDownloadId))) {
                    if (cursor == null || !cursor.moveToFirst()
                            || cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)) != DownloadManager.STATUS_SUCCESSFUL) {
                        runOnUiThread(() -> toast("No se pudo completar la descarga de la actualizacion."));
                        return;
                    }
                }
                File file = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), apkDownloadName);
                Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".files", file);
                Intent install = new Intent(Intent.ACTION_VIEW)
                        .setDataAndType(uri, "application/vnd.android.package-archive")
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(install);
            } catch (Exception e) {
                runOnUiThread(() -> toast("Descarga lista. Abra la notificacion para instalar la actualizacion."));
            }
        });
    }

    private void enableAlerts() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] { Manifest.permission.POST_NOTIFICATIONS }, 10);
            return;
        }
        if (!getSystemService(NotificationManager.class).areNotificationsEnabled()) {
            setAlerts(false); toast("Habilite las notificaciones en Ajustes."); return;
        }
        worker.execute(() -> {
            try {
                String username = LanClient.get(base, "/api/v1/movil/sesion").getJSONObject("usuario").getString("username");
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    startForegroundService(new Intent(this, AvisosService.class).putExtra("server", base).putExtra("username", username));
                    setAlerts(true);
                    if (!getSystemService(PowerManager.class).isIgnoringBatteryOptimizations(getPackageName())) {
                        new AlertDialog.Builder(this).setTitle("Avisos con pantalla bloqueada")
                                .setMessage("Permita el uso de bateria sin restricciones para recibir avisos durante la jornada.")
                                .setPositiveButton("Abrir bateria", (d, w) -> openBatterySettings())
                                .setNegativeButton("Ahora no", null).show();
                    }
                });
            } catch (Exception e) { runOnUiThread(() -> { setAlerts(false); toast("Ingrese con su usuario y verifique la conexion antes de activar avisos."); }); }
        });
    }

    private void openBatterySettings() {
        Intent direct = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                .setData(Uri.parse("package:" + getPackageName()));
        try {
            startActivity(direct);
        } catch (Exception e) {
            startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
        }
    }

    @Override public void onRequestPermissionsResult(int request, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(request, permissions, results);
        if (request == 10) {
            if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) enableAlerts();
            else setAlerts(false);
        } else if (request == REQUEST_DICTATION && results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
            new VoiceBridge().dictarTarea();
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult scan = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (scan != null) {
            dispatchBarcode(scan.getContents());
            return;
        }
        if (requestCode != REQUEST_DICTATION) return;
        if (resultCode != RESULT_OK || data == null) return;
        ArrayList<String> matches = data.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS);
        if (matches == null || matches.isEmpty()) return;
        String text = matches.get(0);
        web.post(() -> web.evaluateJavascript(
                "window.dispatchEvent(new CustomEvent('tareas-lan-dictado',{detail:" + JSONObject.quote(text) + "}));",
                null));
    }

    private void dispatchBarcode(String code) {
        if (code == null || code.isBlank()) return;
        String script = "if(window.__inventarioStockApplyScan){window.__inventarioStockApplyScan("
                + JSONObject.quote(code)
                + ")}else{window.dispatchEvent(new CustomEvent('inventario-stock-scan',{detail:"
                + JSONObject.quote(code)
                + "}));}";
        web.post(() -> web.evaluateJavascript(
                script,
                null));
    }

    @Override public void onBackPressed() {
        if (web.canGoBack()) web.goBack(); else super.onBackPressed();
    }

    @Override protected void onDestroy() {
        worker.shutdownNow();
        if (receiverRegistered) unregisterReceiver(downloadReceiver);
        web.destroy();
        super.onDestroy();
    }

    private String yesNo(boolean value) { return value ? "si" : "no"; }

    private String formatBytes(long bytes) {
        if (bytes <= 0) return "sin datos";
        if (bytes < 1024 * 1024) return Math.max(1, bytes / 1024) + " KB";
        return String.format(java.util.Locale.US, "%.1f MB", bytes / 1024.0 / 1024.0);
    }

    private void toast(String text) { Toast.makeText(this, text, Toast.LENGTH_LONG).show(); }

    public class VoiceBridge {
        @JavascriptInterface public void dictarTarea() {
            runOnUiThread(() -> {
                try {
                    if (Build.VERSION.SDK_INT >= 23
                            && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(new String[] { Manifest.permission.RECORD_AUDIO }, REQUEST_DICTATION);
                        return;
                    }
                    Intent intent = new Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                            .putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                    android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            .putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, "es-AR")
                            .putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT,
                                    "Dicte solicitante y problema a resolver");
                    startActivityForResult(intent, REQUEST_DICTATION);
                } catch (Exception e) {
                    toast("El telefono no tiene reconocimiento de voz disponible.");
                }
            });
        }

        @JavascriptInterface public String disponible() {
            return "true";
        }

        @JavascriptInterface public String versionInstalada() {
            return BuildConfig.VERSION_NAME;
        }

        @JavascriptInterface public void actualizarApk() {
            downloadApkUpdate();
        }

        @JavascriptInterface public void escanearCodigo() {
            runOnUiThread(() -> {
                try {
                    new IntentIntegrator(MainActivity.this)
                            .setCaptureActivity(PortraitCaptureActivity.class)
                            .setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES)
                            .setPrompt("Escanee el codigo del componente")
                            .setBeepEnabled(true)
                            .setOrientationLocked(true)
                            .initiateScan();
                } catch (Exception e) {
                    toast("No se pudo abrir la camara. Ingrese el codigo manualmente.");
                }
            });
        }
    }
}
