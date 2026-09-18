package com.sirin.engine;

import android.app.Activity;
import android.os.Bundle;
import android.os.Build;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.text.InputType;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceError;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.*;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private static final int PICK_ZIP = 41;
    private static final int CREATE_APK = 42;
    private static final String PREFS = "sirin";
    private static final String KEY_ALIAS = "sirin_token_key";

    private TextView status;
    private TextView errorText;
    private WebView preview;
    private LinearLayout editorRoot;
    private LinearLayout errorPanel;
    private File projectDir;
    private File selectedZip;
    private String projectName = "Sirin Project";
    private String entryFile = "main.html";
    private final ArrayList<String> errors = new ArrayList<>();

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setFlags(1024, 1024);
        getWindow().addFlags(128);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        if (hasBundledProject()) buildBundledRuntime();
        else buildHome();
    }

    private int dp(int n) {
        return (int)(n * getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView text(String s, float size) {
        TextView v = new TextView(this);
        v.setText(s);
        v.setTextColor(Color.WHITE);
        v.setTextSize(size);
        return v;
    }

    private Button button(String s) {
        Button b = new Button(this);
        b.setText(s);
        b.setTextSize(16);
        b.setAllCaps(false);
        b.setMinHeight(dp(60));
        return b;
    }

    private LinearLayout base() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(14), dp(20), dp(14));
        root.setBackgroundColor(Color.rgb(10, 14, 20));
        return root;
    }

    private boolean hasBundledProject() {
        try {
            String[] files = getAssets().list("");
            if (files == null) return false;
            for (String f : files) if ("project.zip".equals(f)) return true;
        } catch (Exception ignored) {}
        return false;
    }

    private void buildBundledRuntime() {
        try {
            File z = new File(getCacheDir(), "bundled-project.zip");
            try (InputStream in = getAssets().open("project.zip");
                 OutputStream out = new FileOutputStream(z)) {
                byte[] b = new byte[8192];
                int n;
                while ((n = in.read(b)) > 0) out.write(b, 0, n);
            }
            selectedZip = z;
            projectDir = new File(getCacheDir(), "bundled_project");
            deleteRecursive(projectDir);
            if (!projectDir.mkdirs()) throw new IOException("Proje klasörü oluşturulamadı");
            unzipSafely(z, projectDir);
            readProjectMetadata();
            launchGame();
        } catch (Exception e) {
            buildHome();
            setStatus("Dahili demo açılamadı: " + e.getMessage());
        }
    }

    private void configureWebView(WebView w) {
        WebSettings s = w.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        w.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        w.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                injectErrorHooks(view);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                addError("WebView: " + error.getErrorCode() + " " + error.getDescription());
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                addError("WebView: " + errorCode + " " + description);
            }

            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request, android.webkit.WebResourceResponse response) {
                addError("HTTP " + response.getStatusCode() + ": " + request.getUrl());
            }
        });

        w.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage msg) {
                ConsoleMessage.MessageLevel level = msg.messageLevel();
                if (level == ConsoleMessage.MessageLevel.ERROR) {
                    addError("JS: " + msg.message() + " (" + msg.sourceId() + ":" + msg.lineNumber() + ")");
                }
                return true;
            }
        });
    }

    private void injectErrorHooks(WebView w) {
        String js =
            "(function(){" +
            "window.addEventListener('error',function(e){console.error('SirinTest '+e.message+' @'+e.filename+':'+e.lineno)});" +
            "window.addEventListener('unhandledrejection',function(e){console.error('SirinTest Promise '+e.reason)});" +
            "})();";
        w.evaluateJavascript(js, null);
    }

    private void buildHome() {
        LinearLayout root = base();
        root.addView(text("SIRIN ENGINE", 32));
        TextView sub = text("ZIP → 2D / 3D EDITÖR → OYUN TEST → APK  •  LANDSCAPE", 15);
        sub.setTextColor(Color.LTGRAY);
        root.addView(sub);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);

        Button open = button("📦  ZIP Aç");
        open.setOnClickListener(v -> pickZip());
        row.addView(open, new LinearLayout.LayoutParams(0, dp(72), 1));

        Button settings = button("⚙  Ayarlar");
        settings.setOnClickListener(v -> showSettings());
        row.addView(settings, new LinearLayout.LayoutParams(0, dp(72), 1));

        Button build = button("🚀  APK Derle");
        build.setOnClickListener(v -> startBuild());
        row.addView(build, new LinearLayout.LayoutParams(0, dp(72), 1));

        root.addView(row);

        status = text(
            "Durum: Hazır\nZIP: Seçilmedi\nEditör: 2D / 3D\nTest: Hata Paneli aktif\nAPK: GitHub Actions → Downloads",
            17
        );
        status.setPadding(0, dp(18), 0, 0);
        root.addView(status, new LinearLayout.LayoutParams(-1, 0, 1));

        TextView help = text(
            "ZIP: project.json + main.html. icon.png veya icon.svg otomatik alınır.\n" +
            "APK çıktısı sistem dosya seçicisinde Download klasörüne kaydedilebilir.",
            14
        );
        help.setTextColor(Color.GRAY);
        root.addView(help);
        setContentView(root);
    }

    private void pickZip() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.setType("application/zip");
        i.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(i, PICK_ZIP);
    }

    @Override
    protected void onActivityResult(int req, int result, Intent data) {
        super.onActivityResult(req, result, data);

        if (req == PICK_ZIP && result == RESULT_OK && data != null) {
            try {
                selectedZip = new File(getCacheDir(), "selected-project.zip");
                copyUriToFile(data.getData(), selectedZip);

                projectDir = new File(getCacheDir(), "sirin_project");
                deleteRecursive(projectDir);
                if (!projectDir.mkdirs()) throw new IOException("Proje klasörü oluşturulamadı");

                unzipSafely(selectedZip, projectDir);
                readProjectMetadata();
                showEditor();
                setStatus("ZIP açıldı: " + projectName + " • Editör hazır.");
            } catch (Exception e) {
                setStatus("ZIP hatası: " + e.getMessage());
                addError("ZIP: " + e.getMessage());
            }
        }

        if (req == CREATE_APK && result == RESULT_OK && data != null) {
            File built = new File(getCacheDir(), "SirinEngine-APK.apk");
            try (InputStream in = new FileInputStream(built);
                 OutputStream out = getContentResolver().openOutputStream(data.getData())) {
                byte[] buf = new byte[16384];
                int n;
                while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                Toast.makeText(this, "APK kaydedildi.", Toast.LENGTH_LONG).show();
                setStatus("APK Download/Seçilen klasöre kaydedildi.");
            } catch (Exception e) {
                setStatus("APK kaydetme hatası: " + e.getMessage());
                addError("APK kaydetme: " + e.getMessage());
            }
        }
    }

    private void showEditor() {
        editorRoot = base();

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);

        Button back = button("← Ana");
        back.setOnClickListener(v -> buildHome());
        top.addView(back, new LinearLayout.LayoutParams(dp(100), dp(56)));

        Button b2 = button("2D Editör");
        b2.setOnClickListener(v -> show2DEditor());
        top.addView(b2, new LinearLayout.LayoutParams(0, dp(56), 1));

        Button b3 = button("3D Editör");
        b3.setOnClickListener(v -> show3DEditor());
        top.addView(b3, new LinearLayout.LayoutParams(0, dp(56), 1));

        Button play = button("▶ Oyun Oyna");
        play.setOnClickListener(v -> launchGame());
        top.addView(play, new LinearLayout.LayoutParams(0, dp(56), 1));

        Button err = button("⚠ Hata Paneli");
        err.setOnClickListener(v -> toggleErrorPanel());
        top.addView(err, new LinearLayout.LayoutParams(0, dp(56), 1));

        Button files = button("Dosyalar");
        files.setOnClickListener(v -> showFiles());
        top.addView(files, new LinearLayout.LayoutParams(0, dp(56), 1));

        Button settings = button("Ayarlar");
        settings.setOnClickListener(v -> showSettings());
        top.addView(settings, new LinearLayout.LayoutParams(0, dp(56), 1));

        editorRoot.addView(top);

        status = text("Proje: " + projectName + "\nGiriş: " + entryFile, 15);
        editorRoot.addView(status);

        preview = new WebView(this);
        configureWebView(preview);
        editorRoot.addView(preview, new LinearLayout.LayoutParams(-1, 0, 1));

        errorPanel = new LinearLayout(this);
        errorPanel.setOrientation(LinearLayout.VERTICAL);
        errorPanel.setBackgroundColor(Color.rgb(35, 20, 24));
        errorPanel.setVisibility(View.GONE);

        errorText = text("HATA PANELİ\nHenüz hata yok.", 14);
        errorText.setPadding(dp(10), dp(8), dp(10), dp(8));
        ScrollView scroll = new ScrollView(this);
        scroll.addView(errorText);
        errorPanel.addView(scroll, new LinearLayout.LayoutParams(-1, dp(120)));
        editorRoot.addView(errorPanel);

        setContentView(editorRoot);
        show3DEditor();
    }

    private void show2DEditor() {
        if (preview == null) return;
        clearErrors();
        String html =
            "<!doctype html><html><body style='margin:0;background:#101720;color:#fff;font-family:sans-serif'>" +
            "<div style='padding:10px;font-weight:bold'>2D SAHNE EDİTÖRÜ • " + esc(projectName) + "</div>" +
            "<canvas id='c' width='1200' height='620' style='width:100%;height:calc(100vh - 50px)'></canvas>" +
            "<script>const c=document.getElementById('c'),x=c.getContext('2d');" +
            "x.fillStyle='#182432';x.fillRect(0,0,c.width,c.height);" +
            "x.strokeStyle='#33495e';for(let i=0;i<c.width;i+=50){x.beginPath();x.moveTo(i,0);x.lineTo(i,c.height);x.stroke()}" +
            "for(let i=0;i<c.height;i+=50){x.beginPath();x.moveTo(0,i);x.lineTo(c.width,i);x.stroke()}" +
            "x.fillStyle='#56d6ff';x.fillRect(500,240,200,110);x.fillStyle='#fff';x.font='26px sans-serif';x.fillText('Sirin 2D Sahne',520,305);" +
            "</script></body></html>";
        preview.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
        setStatus("2D Editör açık.");
    }

    private void show3DEditor() {
        if (preview == null || projectDir == null) return;
        clearErrors();
        File entry = new File(projectDir, entryFile);
        if (entry.exists()) {
            preview.loadUrl(Uri.fromFile(entry).toString());
            setStatus("3D Editör / önizleme açık.");
        } else {
            addError("Giriş dosyası bulunamadı: " + entryFile);
            setStatus("3D önizleme açılamadı.");
        }
    }

    private void launchGame() {
        if (projectDir == null) {
            setStatus("Önce ZIP proje aç.");
            return;
        }
        clearErrors();
        if (preview == null) {
            preview = new WebView(this);
            configureWebView(preview);
            setContentView(preview);
        }
        File entry = new File(projectDir, entryFile);
        if (!entry.exists()) {
            addError("Oyun girişi bulunamadı: " + entryFile);
            return;
        }
        preview.loadUrl(Uri.fromFile(entry).toString());
        setStatus("▶ Oyun test modu çalışıyor. Hatalar Hata Paneli'ne aktarılıyor.");
    }

    private void toggleErrorPanel() {
        if (errorPanel == null) return;
        errorPanel.setVisibility(errorPanel.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
    }

    private void clearErrors() {
        errors.clear();
        refreshErrorPanel();
    }

    private void addError(String msg) {
        if (msg == null || msg.trim().isEmpty()) return;
        String line = "[" + new java.text.SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date()) + "] " + msg;
        errors.add(line);
        while (errors.size() > 80) errors.remove(0);
        runOnUiThread(this::refreshErrorPanel);
    }

    private void refreshErrorPanel() {
        if (errorText == null) return;
        if (errors.isEmpty()) {
            errorText.setText("HATA PANELİ\nHenüz hata yok.");
            return;
        }
        StringBuilder b = new StringBuilder("HATA PANELİ\n");
        for (String e : errors) b.append("• ").append(e).append("\n");
        errorText.setText(b.toString());
    }

    private void showFiles() {
        if (projectDir == null) return;
        LinearLayout root = base();

        Button back = button("← Editöre dön");
        back.setOnClickListener(v -> showEditor());
        root.addView(back, new LinearLayout.LayoutParams(-1, dp(56)));

        root.addView(text("PROJE DOSYALARI\n\n" + listFiles(projectDir, ""), 16),
            new LinearLayout.LayoutParams(-1, 0, 1));

        setContentView(root);
    }

    private String listFiles(File dir, String prefix) {
        StringBuilder out = new StringBuilder();
        File[] fs = dir.listFiles();
        if (fs == null) return "";
        Arrays.sort(fs, Comparator.comparing(File::getName));
        for (File f : fs) {
            out.append(prefix).append(f.isDirectory() ? "📁 " : "📄 ")
               .append(f.getName()).append("\n");
            if (f.isDirectory()) out.append(listFiles(f, prefix + "  "));
        }
        return out.toString();
    }

    private void showSettings() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(18), 0, dp(18), 0);

        EditText repo = new EditText(this);
        repo.setText(getRepo());
        repo.setHint("owner/repo");
        box.addView(repo);

        EditText token = new EditText(this);
        token.setHint("GitHub token");
        token.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        box.addView(token);

        new AlertDialog.Builder(this)
            .setTitle("Sirin Engine Ayarları")
            .setMessage(
                "ZIP proje GitHub'a yüklenir ve GitHub Actions APK derlemesini yapar. " +
                "Token Android Keystore ile şifreli tutulur. Android 10+ sistem dosya seçicisi " +
                "özel depolama izni istemeden Download klasörüne kaydetmeye izin verir."
            )
            .setView(box)
            .setPositiveButton("Kaydet", (d, w) -> {
                saveRepo(repo.getText().toString().trim());
                String t = token.getText().toString().trim();
                if (!t.isEmpty()) saveSecret(t);
                setStatus("Ayarlar kaydedildi.");
            })
            .setNegativeButton("İptal", null)
            .show();
    }

    private String getRepo() {
        return getSharedPreferences(PREFS, 0).getString("repo", "huter413/Sirin-engine");
    }

    private void saveRepo(String repo) {
        if (repo.matches("[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+")) {
            getSharedPreferences(PREFS, 0).edit().putString("repo", repo).apply();
        }
    }

    private void saveSecret(String secret) {
        try {
            KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
            ks.load(null);

            if (!ks.containsAlias(KEY_ALIAS)) {
                KeyGenerator kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
                kg.init(new KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT
                ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                 .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                 .build());
                kg.generateKey();
            }

            SecretKey key = ((KeyStore.SecretKeyEntry) ks.getEntry(KEY_ALIAS, null)).getSecretKey();
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, key);

            String value =
                Base64.getEncoder().encodeToString(c.getIV()) + ":" +
                Base64.getEncoder().encodeToString(c.doFinal(secret.getBytes(StandardCharsets.UTF_8)));

            getSharedPreferences(PREFS, 0).edit().putString("token", value).apply();
        } catch (Exception e) {
            addError("Token: " + e.getMessage());
        }
    }

    private String getSecret() {
        try {
            String stored = getSharedPreferences(PREFS, 0).getString("token", "");
            if (stored.isEmpty()) return "";

            String[] p = stored.split(":", 2);
            KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
            ks.load(null);

            SecretKey key = ((KeyStore.SecretKeyEntry) ks.getEntry(KEY_ALIAS, null)).getSecretKey();
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, Base64.getDecoder().decode(p[0])));

            return new String(
                c.doFinal(Base64.getDecoder().decode(p[1])),
                StandardCharsets.UTF_8
            );
        } catch (Exception e) {
            return "";
        }
    }

    private void startBuild() {
        if (selectedZip == null || projectDir == null) {
            setStatus("Önce ZIP proje aç.");
            return;
        }
        if (getSecret().isEmpty()) {
            showSettings();
            setStatus("APK derlemesi için GitHub tokenını Ayarlar'a ekle.");
            return;
        }

        setStatus("ZIP GitHub'a yükleniyor…");
        new Thread(() -> {
            try {
                long previous = latestRunId();
                uploadZip();
                dispatchBuild();
                waitForBuild(previous);
            } catch (Exception e) {
                addError("Build: " + e.getMessage());
                setStatus("Derleme hatası: " + e.getMessage());
            }
        }).start();
    }

    private String buildPath() {
        return "projects/inbox/current-project.zip";
    }

    private long latestRunId() throws Exception {
        JSONObject o = new JSONObject(getJson(
            "https://api.github.com/repos/" + getRepo() +
            "/actions/workflows/android-apk.yml/runs?event=workflow_dispatch&per_page=1",
            getSecret()
        ));
        JSONArray a = o.optJSONArray("workflow_runs");
        return (a == null || a.length() == 0) ? 0 : a.getJSONObject(0).optLong("id", 0);
    }

    private void uploadZip() throws Exception {
        byte[] bytes = readBytes(selectedZip);
        if (bytes.length > 15 * 1024 * 1024) throw new IOException("ZIP 15 MB'dan büyük.");

        String url = "https://api.github.com/repos/" + getRepo() + "/contents/" + buildPath();
        JSONObject body = new JSONObject();
        body.put("message", "Sirin Engine: upload project");
        body.put("content", Base64.getEncoder().encodeToString(bytes));
        body.put("branch", "main");

        try {
            putJson(url, body.toString(), getSecret());
        } catch (IOException ex) {
            if (!ex.getMessage().contains("HTTP 422")) throw ex;
            JSONObject existing = new JSONObject(getJson(url, getSecret()));
            body.put("sha", existing.optString("sha"));
            putJson(url, body.toString(), getSecret());
        }
    }

    private void dispatchBuild() throws Exception {
        String url = "https://api.github.com/repos/" + getRepo() + "/actions/workflows/android-apk.yml/dispatches";
        JSONObject body = new JSONObject();
        body.put("ref", "main");
        postJson(url, body.toString(), getSecret());
    }

    private void waitForBuild(long previousRun) throws Exception {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < 15 * 60 * 1000L) {
            Thread.sleep(5000);

            JSONObject runs = new JSONObject(getJson(
                "https://api.github.com/repos/" + getRepo() +
                "/actions/workflows/android-apk.yml/runs?event=workflow_dispatch&per_page=10",
                getSecret()
            ));

            JSONArray a = runs.optJSONArray("workflow_runs");
            if (a == null) continue;

            for (int i = 0; i < a.length(); i++) {
                JSONObject r = a.getJSONObject(i);
                long id = r.optLong("id", 0);
                if (id == previousRun) continue;

                String state = r.optString("status", "");
                String conclusion = r.optString("conclusion", "");

                if ("completed".equals(state) && "failure".equals(conclusion)) {
                    throw new IOException("GitHub Actions derlemesi başarısız.");
                }

                if ("completed".equals(state) && "success".equals(conclusion)) {
                    downloadArtifact(id);
                    return;
                }
            }

            runOnUiThread(() -> setStatus("APK CI derleniyor…"));
        }

        throw new IOException("Derleme 15 dakikada tamamlanmadı.");
    }

    private void downloadArtifact(long runId) throws Exception {
        String url = "https://api.github.com/repos/" + getRepo() +
                     "/actions/runs/" + runId + "/artifacts";

        JSONObject o = new JSONObject(getJson(url, getSecret()));
        JSONArray a = o.optJSONArray("artifacts");
        if (a == null || a.length() == 0) throw new IOException("APK artifact bulunamadı.");

        long artifactId = a.getJSONObject(0).optLong("id", 0);

        byte[] artifactZip = downloadBytes(
            "https://api.github.com/repos/" + getRepo() +
            "/actions/artifacts/" + artifactId + "/zip",
            getSecret()
        );

        File art = new File(getCacheDir(), "artifact.zip");
        writeBytes(art, artifactZip);

        File dir = new File(getCacheDir(), "artifact");
        deleteRecursive(dir);
        if (!dir.mkdirs()) throw new IOException("Artifact klasörü oluşturulamadı");
        unzipSafely(art, dir);

        File apk = findApk(dir);
        if (apk == null) throw new IOException("APK artifact içinde yok.");

        File local = new File(getCacheDir(), "SirinEngine-APK.apk");
        copyFile(apk, local);

        runOnUiThread(this::saveApkPrompt);
    }

    private void saveApkPrompt() {
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("application/vnd.android.package-archive");
        i.putExtra(Intent.EXTRA_TITLE, safeFileName(projectName) + ".apk");

        if (Build.VERSION.SDK_INT >= 26) {
            try {
                i.putExtra(
                    DocumentsContract.EXTRA_INITIAL_URI,
                    Uri.parse("content://com.android.providers.downloads.documents/root/downloads")
                );
            } catch (Exception ignored) {}
        }

        startActivityForResult(i, CREATE_APK);
    }

    private String safeFileName(String s) {
        String v = s.replaceAll("[^A-Za-z0-9._-]+", "_");
        return v.isEmpty() ? "SirinProject" : v;
    }

    private File findApk(File dir) {
        File[] fs = dir.listFiles();
        if (fs == null) return null;

        for (File f : fs) {
            if (f.isDirectory()) {
                File x = findApk(f);
                if (x != null) return x;
            } else if (f.getName().endsWith(".apk")) {
                return f;
            }
        }
        return null;
    }

    private void readProjectMetadata() {
        File meta = new File(projectDir, "project.json");

        try {
            JSONObject o = new JSONObject(readText(meta));
            projectName = o.optString("name", "Sirin Project");
            entryFile = o.optString("entry", "main.html");

            String mode = o.optString("mode", "3d");
            if ("2d".equalsIgnoreCase(mode)) {
                entryFile = o.optString("entry", "main.html");
            }
        } catch (Exception e) {
            projectName = "Sirin Project";
            entryFile = "main.html";
            addError("project.json: " + e.getMessage());
        }
    }

    private void setStatus(String s) {
        runOnUiThread(() -> {
            if (status != null) status.setText("Durum: " + s);
        });
    }

    private String esc(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace(""", "&quot;");
    }

    private void copyUriToFile(Uri uri, File out) throws Exception {
        InputStream in = getContentResolver().openInputStream(uri);
        if (in == null) throw new IOException("ZIP okunamadı");

        try (InputStream src = in; OutputStream dst = new FileOutputStream(out)) {
            byte[] b = new byte[16384];
            int n;
            while ((n = src.read(b)) > 0) dst.write(b, 0, n);
        }
    }

    private byte[] readBytes(File file) throws Exception {
        try (InputStream in = new FileInputStream(file);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] b = new byte[16384];
            int n;
            while ((n = in.read(b)) > 0) out.write(b, 0, n);
            return out.toByteArray();
        }
    }

    private String readText(File file) throws Exception {
        return new String(readBytes(file), StandardCharsets.UTF_8);
    }

    private void writeBytes(File file, byte[] bytes) throws Exception {
        try (OutputStream out = new FileOutputStream(file)) {
            out.write(bytes);
        }
    }

    private void copyFile(File from, File to) throws Exception {
        try (InputStream in = new FileInputStream(from);
             OutputStream out = new FileOutputStream(to)) {
            byte[] b = new byte[16384];
            int n;
            while ((n = in.read(b)) > 0) out.write(b, 0, n);
        }
    }

    private void unzipSafely(File zip, File out) throws Exception {
        try (ZipInputStream zin = new ZipInputStream(new FileInputStream(zip))) {
            ZipEntry e;
            byte[] b = new byte[8192];
            String root = out.getCanonicalPath() + File.separator;

            while ((e = zin.getNextEntry()) != null) {
                File dest = new File(out, e.getName());

                if (!dest.getCanonicalPath().startsWith(root)) {
                    throw new IOException("Güvensiz ZIP yolu.");
                }

                if (e.isDirectory()) {
                    if (!dest.mkdirs() && !dest.isDirectory()) {
                        throw new IOException("Klasör oluşturulamadı");
                    }
                    continue;
                }

                File parent = dest.getParentFile();
                if (parent != null) parent.mkdirs();

                try (OutputStream output = new FileOutputStream(dest)) {
                    int n;
                    while ((n = zin.read(b)) > 0) output.write(b, 0, n);
                }
            }
        }
    }

    private void deleteRecursive(File f) {
        if (f == null || !f.exists()) return;
        File[] fs = f.listFiles();
        if (fs != null) for (File x : fs) deleteRecursive(x);
        f.delete();
    }

    private String getJson(String url, String token) throws Exception {
        return request(url, "GET", null, token);
    }

    private void postJson(String url, String body, String token) throws Exception {
        request(url, "POST", body, token);
    }

    private void putJson(String url, String body, String token) throws Exception {
        request(url, "PUT", body, token);
    }

    private String request(String url, String method, String body, String token) throws Exception {
        HttpURLConnection c = (HttpURLConnection)new URL(url).openConnection();
        c.setRequestMethod(method);
        c.setConnectTimeout(20000);
        c.setReadTimeout(30000);
        c.setRequestProperty("Accept", "application/vnd.github+json");
        c.setRequestProperty("Authorization", "Bearer " + token);
        c.setRequestProperty("User-Agent", "SirinEngine");
        c.setRequestProperty("X-GitHub-Api-Version", "2022-11-28");

        if (body != null) {
            c.setDoOutput(true);
            c.setRequestProperty("Content-Type", "application/json");
            try (OutputStream out = c.getOutputStream()) {
                out.write(body.getBytes(StandardCharsets.UTF_8));
            }
        }

        int code = c.getResponseCode();
        InputStream in = code >= 200 && code < 300 ? c.getInputStream() : c.getErrorStream();
        String response = readAll(in);

        if (code < 200 || code >= 300) {
            throw new IOException("GitHub HTTP " + code + ": " + response);
        }

        return response;
    }

    private String readAll(InputStream in) throws Exception {
        if (in == null) return "";
        try (InputStream src = in; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] b = new byte[16384];
            int n;
            while ((n = src.read(b)) > 0) out.write(b, 0, n);
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private byte[] downloadBytes(String url, String token) throws Exception {
        HttpURLConnection c = (HttpURLConnection)new URL(url).openConnection();
        c.setInstanceFollowRedirects(true);
        c.setConnectTimeout(20000);
        c.setReadTimeout(60000);
        c.setRequestProperty("Authorization", "Bearer " + token);
        c.setRequestProperty("User-Agent", "SirinEngine");

        int code = c.getResponseCode();
        if (code < 200 || code >= 300) {
            throw new IOException("Artifact HTTP " + code);
        }

        try (InputStream in = c.getInputStream();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] b = new byte[16384];
            int n;
            while ((n = in.read(b)) > 0) out.write(b, 0, n);
            return out.toByteArray();
        }
    }
}
