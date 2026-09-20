package com.sirin.runtime;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class RuntimeActivity extends Activity {
    private File projectRoot;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setFlags(1024,1024);
        projectRoot = new File(getFilesDir(), "sirin_project");
        try {
            installProject();
            launchEntry();
        } catch (Exception e) {
            TextView t = new TextView(this);
            t.setTextColor(Color.WHITE);
            t.setTextSize(16);
            t.setPadding(32,32,32,32);
            t.setText("Şirin Engine oyunu başlatılamadı.\\n\\n" + e.getMessage());
            setContentView(t);
        }
    }

    private void installProject() throws Exception {
        delete(projectRoot);
        if (!projectRoot.mkdirs()) throw new IOException("Proje klasörü oluşturulamadı.");
        copyAssetTree("sirin_project", projectRoot);
        File meta = new File(projectRoot, "project.sr");
        if (!meta.isFile()) throw new IOException("project.sr bulunamadı.");
        JSONObject o = new JSONObject(readText(meta));
        if (!"Sirin Engine".equals(o.optString("engine",""))) throw new IOException("Geçersiz Sirin Engine projesi.");
    }

    private void launchEntry() throws Exception {
        JSONObject o = new JSONObject(readText(new File(projectRoot, "project.sr")));
        String entry = o.optString("entry", "main.html");
        File f = new File(projectRoot, entry);
        if (!f.getCanonicalPath().startsWith(projectRoot.getCanonicalPath()+File.separator) || !f.isFile())
            throw new IOException("Giriş dosyası bulunamadı: " + entry);

        WebView w = new WebView(this);
        WebSettings s = w.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        w.setWebViewClient(new WebViewClient());
        setContentView(w);
        w.loadUrl("file://" + f.getAbsolutePath());
    }

    private void copyAssetTree(String assetPath, File dest) throws Exception {
        String[] children = getAssets().list(assetPath);
        if (children == null || children.length == 0) {
            try (InputStream in=getAssets().open(assetPath); OutputStream out=new FileOutputStream(dest)) {
                byte[] b=new byte[65536]; int n; while((n=in.read(b))!=-1) out.write(b,0,n);
            }
            return;
        }
        if (!dest.exists() && !dest.mkdirs()) throw new IOException("Klasör oluşturulamadı: "+dest);
        for (String child:children) {
            copyAssetTree(assetPath+"/"+child, new File(dest, child));
        }
    }

    private String readText(File f) throws Exception {
        try(InputStream in=new FileInputStream(f); ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] b=new byte[65536]; int n; while((n=in.read(b))!=-1) out.write(b,0,n);
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private void delete(File f) {
        if (f==null || !f.exists()) return;
        File[] children=f.listFiles();
        if(children!=null) for(File c:children) delete(c);
        f.delete();
    }
}
