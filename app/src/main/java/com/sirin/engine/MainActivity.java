package com.sirin.engine;

import android.Manifest;
import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.InputType;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import android.database.Cursor;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.AlgorithmParameterSpec;
import java.util.*;
import java.util.zip.*;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.json.*;

public class MainActivity extends Activity {
    private static final int PICK_ZIP = 41;
    private static final int CREATE_APK = 42;
    private static final int STORAGE_PERMISSION = 43;
    private static final String PREFS = "sirin";
    private TextView status;
    private WebView preview;
    private File projectDir;
    private File selectedZip;
    private String projectName = "Sirin Project";
    private String githubRepo = "huter413/Sirin-engine";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        buildHome();
    }

    private int dp(int n) { return (int)(n * getResources().getDisplayMetrics().density + .5f); }

    private Button btn(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(16);
        b.setAllCaps(false);
        b.setMinHeight(dp(64));
        return b;
    }

    private TextView label(String text, float size) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextColor(Color.WHITE);
        t.setTextSize(size);
        return t;
    }

    private LinearLayout base() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(18), dp(24), dp(18));
        root.setBackgroundColor(Color.rgb(10,14,20));
        return root;
    }

    private void buildHome() {
        LinearLayout root = base();
        root.addView(label("SIRIN ENGINE", 32));
        TextView sub = label("ZIP → 2D/3D Editor → APK  •  Android  •  YATAY", 16);
        sub.setTextColor(Color.LTGRAY);
        root.addView(sub);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);

        Button open = btn("📦  Proje ZIP Aç");
        open.setOnClickListener(v -> pickZip());
        row.addView(open, new LinearLayout.LayoutParams(0, dp(72), 1));

        Button settings = btn("⚙  Ayarlar");
        settings.setOnClickListener(v -> showSettings());
        row.addView(settings, new LinearLayout.LayoutParams(0, dp(72), 1));

        Button build = btn("🚀  APK Derle");
        build.setOnClickListener(v -> startBuild());
        row.addView(build, new LinearLayout.LayoutParams(0, dp(72), 1));

        root.addView(row);

        status = label("Durum: Hazır\nZIP: Seçilmedi\nÇıkış: Android APK\nEkran: Yatay\nEditör: 2D / 3D", 17);
        status.setPadding(0, dp(18), 0, 0);
        root.addView(status, new LinearLayout.LayoutParams(-1, 0, 1));

        TextView help = label("ZIP içinde project.json + main.html + icon.png/icon.svg kullanılabilir. APK ikonu ZIP içinden alınır.", 14);
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

    @Override protected void onActivityResult(int req, int result, Intent data) {
        super.onActivityResult(req, result, data);
        if (req == PICK_ZIP && result == RESULT_OK && data != null) {
            Uri u = data.getData();
            try {
                selectedZip = new File(getCacheDir(), "selected-project.zip");
                copyUriToFile(u, selectedZip);
                projectDir = new File(getCacheDir(), "sirin_project");
                deleteRecursive(projectDir);
                if (!projectDir.mkdirs()) throw new IOException("Klasör oluşturulamadı");
                unzipSafely(selectedZip, projectDir);
                readProjectMetadata();
                setStatus("ZIP açıldı: " + projectName + "\nEditör hazır.");
                showEditor();
            } catch (Exception e) {
                setStatus("ZIP hatası: " + e.getMessage());
            }
        }
        if (req == CREATE_APK && result == RESULT_OK && data != null) {
            File built = new File(getCacheDir(), "SirinEngine-APK.apk");
            try (InputStream in = new FileInputStream(built);
                 OutputStream out = getContentResolver().openOutputStream(data.getData())) {
                byte[] buf = new byte[8192]; int n;
                while ((n = in.read(buf)) > 0) out.write(buf,0,n);
                Toast.makeText(this, "APK kaydedildi.", Toast.LENGTH_LONG).show();
            } catch (Exception e) { setStatus("APK kaydetme hatası: " + e.getMessage()); }
        }
    }

    private void showEditor() {
        LinearLayout root = base();
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        Button back = btn("← Ana Sayfa"); back.setOnClickListener(v -> buildHome());
        top.addView(back, new LinearLayout.LayoutParams(dp(150), dp(58)));
        Button b2 = btn("2D Editor"); b2.setOnClickListener(v -> show2DEditor());
        top.addView(b2, new LinearLayout.LayoutParams(0, dp(58), 1));
        Button b3 = btn("3D Editor"); b3.setOnClickListener(v -> show3DEditor());
        top.addView(b3, new LinearLayout.LayoutParams(0, dp(58), 1));
        Button files = btn("Dosyalar");
        files.setOnClickListener(v -> showFiles());
        top.addView(files, new LinearLayout.LayoutParams(0, dp(58), 1));
        Button settings = btn("Ayarlar"); settings.setOnClickListener(v -> showSettings());
        top.addView(settings, new LinearLayout.LayoutParams(0, dp(58), 1));
        root.addView(top);
        status = label("Proje: " + projectName + "\n3D Editor önizlemesi hazır.", 16);
        root.addView(status);
        preview = new WebView(this);
        WebSettings ws = preview.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setAllowFileAccess(true);
        ws.setAllowContentAccess(true);
        ws.setDomStorageEnabled(true);
        root.addView(preview, new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root);
        show3DEditor();
    }

    private void show2DEditor() {
        if (preview == null) return;
        String html = "<html><body style='margin:0;background:#101720;color:#fff;font-family:sans-serif'>" +
                "<div style='padding:12px'>2D SAHNE EDİTÖRÜ • " + esc(projectName) + "</div>" +
                "<canvas id='c' width='1200' height='600' style='width:100%;background:#182432'></canvas>" +
                "<script>let c=document.getElementById('c'),x=c.getContext('2d');" +
                "x.strokeStyle='#34495e';for(let i=0;i<1200;i+=50){x.beginPath();x.moveTo(i,0);x.lineTo(i,600);x.stroke()}for(let i=0;i<600;i+=50){x.beginPath();x.moveTo(0,i);x.lineTo(1200,i);x.stroke()}" +
                "x.fillStyle='#56d6ff';x.fillRect(500,230,200,100);x.fillStyle='#fff';x.fillText('Sahne alanı',550,285);</script></body></html>";
        preview.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
    }

    private void show3DEditor() {
        if (projectDir == null || preview == null) return;
        File entry = new File(projectDir, getEntryFile());
        if (entry.exists()) preview.loadUrl("file://" + entry.getAbsolutePath());
        else show2DEditor();
    }

    private String getEntryFile() {
        try {
            JSONObject o = new JSONObject(readText(new File(projectDir,"project.json")));
            return o.optString("entry","main.html");
        } catch(Exception e) { return "main.html"; }
    }

    private void showFiles() {
        if (projectDir == null) return;
        LinearLayout root=base();
        LinearLayout top=new LinearLayout(this); top.setOrientation(LinearLayout.HORIZONTAL);
        Button back=btn("← Editör"); back.setOnClickListener(v -> showEditor()); top.addView(back,new LinearLayout.LayoutParams(dp(160),dp(58)));
        root.addView(top);
        TextView list=label("PROJE DOSYALARI\n\n"+listFiles(projectDir, ""),16);
        root.addView(list,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root);
    }

    private String listFiles(File d,String p){
        StringBuilder s=new StringBuilder();
        File[] fs=d.listFiles(); if(fs==null)return "";
        Arrays.sort(fs);
        for(File f:fs){ if(f.isDirectory())s.append(p+"📁 "+f.getName()+"\n"); else s.append(p+"📄 "+f.getName()+"  ("+f.length()+" B)\n"); if(f.isDirectory())s.append(listFiles(f,p+"  "));}
        return s.toString();
    }

    private void showSettings() {
        final EditText repo=new EditText(this); repo.setText(getRepo()); repo.setHint("owner/repo");
        final EditText token=new EditText(this); token.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD); token.setHint("GitHub token");
        new AlertDialog.Builder(this).setTitle("Sirin Engine Ayarları")
            .setMessage("APK bulut derlemesi için GitHub token gerekir. Gerekli izinler: Contents yazma + Actions yazma/okuma.")
            .setView(makeSettingsView(repo,token))
            .setPositiveButton("Kaydet", (d,w)->{ saveRepo(repo.getText().toString().trim()); if(!token.getText().toString().trim().isEmpty()) saveSecret(token.getText().toString().trim()); setStatus("Ayarlar kaydedildi.");})
            .setNegativeButton("İptal",null).show();
    }

    private LinearLayout makeSettingsView(EditText repo, EditText token){
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(18),0,dp(18),0);
        box.addView(repo); box.addView(token); return box;
    }

    private String getRepo(){ return getSharedPreferences(PREFS,0).getString("repo",githubRepo); }
    private void saveRepo(String r){ if(r.contains("/")){githubRepo=r;getSharedPreferences(PREFS,0).edit().putString("repo",r).apply();}}
    private void saveSecret(String s){ try{
        KeyStore ks=KeyStore.getInstance("AndroidKeyStore"); ks.load(null);
        if(!ks.containsAlias("sirin_key")){ KeyGenerator kg=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore"); kg.init(new KeyGenParameterSpec.Builder("sirin_key",KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build()); kg.generateKey();}
        javax.crypto.Cipher c=javax.crypto.Cipher.getInstance("AES/GCM/NoPadding");
        c.init(javax.crypto.Cipher.ENCRYPT_MODE,((KeyStore.SecretKeyEntry)ks.getEntry("sirin_key",null)).getSecretKey());
        byte[] iv=c.getIV(), enc=c.doFinal(s.getBytes(StandardCharsets.UTF_8));
        String joined=Base64.getEncoder().encodeToString(iv)+":"+Base64.getEncoder().encodeToString(enc);
        getSharedPreferences(PREFS,0).edit().putString("token",joined).apply();
    }catch(Exception e){ setStatus("Token kaydedilemedi.");}}
    private String getSecret(){ try{
        String v=getSharedPreferences(PREFS,0).getString("token",null); if(v==null)return "";
        String[] a=v.split(":"); byte[] iv=Base64.getDecoder().decode(a[0]),enc=Base64.getDecoder().decode(a[1]);
        KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);
        SecretKey key=((KeyStore.SecretKeyEntry)ks.getEntry("sirin_key",null)).getSecretKey();
        javax.crypto.Cipher c=javax.crypto.Cipher.getInstance("AES/GCM/NoPadding");
        c.init(javax.crypto.Cipher.DECRYPT_MODE,key,new GCMParameterSpec(128,iv));
        return new String(c.doFinal(enc),StandardCharsets.UTF_8);
    }catch(Exception e){return "";}}
    
    private void startBuild(){
        if(selectedZip==null || projectDir==null){setStatus("Önce bir ZIP proje aç.");return;}
        if(getSecret().isEmpty()){ showSettings(); setStatus("Önce GitHub tokenını Ayarlar'a ekle."); return; }
        setStatus("ZIP GitHub'a yükleniyor…");
        new Thread(() -> {
            try{
                String base=readText(projectFile("project.json"));
                uploadZipToGitHub();
                String path=getBuildInputPath();
                dispatchBuild(path);
                waitForBuildAndDownload();
            }catch(Exception e){runOnUiThread(()->setStatus("Derleme hatası: "+e.getMessage()));}
        }).start();
    }

    private String getBuildInputPath(){ return "projects/inbox/" + "current-project.zip"; }

    private void uploadZipToGitHub() throws Exception{
        byte[] bytes=readBytes(selectedZip); if(bytes.length>15*1024*1024)throw new IOException("ZIP 15 MB sınırını aşıyor.");
        String repo=getRepo(); String api="https://api.github.com/repos/"+repo+"/contents/"+getBuildInputPath();
        JSONObject body=new JSONObject(); body.put("message","Sirin Engine: upload project"); body.put("content",Base64.getEncoder().encodeToString(bytes)); body.put("branch","main");
        putJson(api,body.toString(),getSecret());
    }

    private void dispatchBuild(String zipPath) throws Exception{
        String repo=getRepo(); String api="https://api.github.com/repos/"+repo+"/actions/workflows/android-apk.yml/dispatches";
        JSONObject body=new JSONObject(); body.put("ref","main"); JSONObject inputs=new JSONObject(); inputs.put("project_zip",zipPath); body.put("inputs",inputs);
        postJson(api,body.toString(),getSecret());
    }

    private void waitForBuildAndDownload() throws Exception{
        String repo=getRepo(); long started=System.currentTimeMillis();
        while(System.currentTimeMillis()-started < 15*60*1000){
            Thread.sleep(5000);
            JSONObject runs=new JSONObject(getJson("https://api.github.com/repos/"+repo+"/actions/workflows/android-apk.yml/runs?event=workflow_dispatch&per_page=10",getSecret()));
            JSONArray arr=runs.optJSONArray("workflow_runs"); if(arr==null)continue;
            for(int i=0;i<arr.length();i++){
                JSONObject r=arr.getJSONObject(i); String status=r.optString("status"); String conclusion=r.optString("conclusion");
                if("completed".equals(status) && "success".equals(conclusion)){
                    long id=r.getLong("id"); downloadArtifact(repo,id); return;
                }
            }
            runOnUiThread(()->setStatus("APK CI derleniyor…"));
        }
        throw new IOException("Derleme zaman aşımına uğradı.");
    }

    private void downloadArtifact(String repo,long runId) throws Exception{
        JSONObject a=new JSONObject(getJson("https://api.github.com/repos/"+repo+"/actions/runs/"+runId+"/artifacts",getSecret()));
        JSONArray arr=a.optJSONArray("artifacts"); if(arr==null||arr.length()==0)throw new IOException("APK artifact bulunamadı.");
        long id=arr.getJSONObject(0).getLong("id");
        byte[] zip=downloadBytes("https://api.github.com/repos/"+repo+"/actions/artifacts/"+id+"/zip",getSecret());
        File art=new File(getCacheDir(),"artifact.zip"); writeBytes(art,zip);
        File temp=new File(getCacheDir(),"artifact");deleteRecursive(temp);temp.mkdirs();unzipSafely(art,temp);
        File apk=findApk(temp); if(apk==null)throw new IOException("APK dosyası bulunamadı.");
        File out=new File(getCacheDir(),"SirinEngine-APK.apk");copyFile(apk,out);
        runOnUiThread(()->saveApkPrompt(out));
    }

    private void saveApkPrompt(File file){
        Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.setType("application/vnd.android.package-archive");
        i.putExtra(Intent.EXTRA_TITLE,projectName.replaceAll("[^A-Za-z0-9._-]","_")+".apk");
        startActivityForResult(i,CREATE_APK);
    }

    private File findApk(File d){File[]fs=d.listFiles();if(fs==null)return null;for(File f:fs){if(f.isDirectory()){File x=findApk(f);if(x!=null)return x;}else if(f.getName().endsWith(".apk"))return f;}return null;}

    private void readProjectMetadata(){
        File meta=projectFile("project.json");
        try{JSONObject o=new JSONObject(readText(meta)); projectName=o.optString("name","Sirin Project");}catch(Exception e){projectName="Sirin Project";}
    }
    private File projectFile(String p){return new File(projectDir,p);}
    private void setStatus(String s){runOnUiThread(()->{if(status!=null)status.setText("Durum: "+s);});}
    private String esc(String s){return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace(""","&quot;");}

    private void copyUriToFile(Uri u,File out)throws Exception{try(InputStream in=getContentResolver().openInputStream(u);OutputStream o=new FileOutputStream(out)){byte[]b=new byte[8192];int n;while((n=in.read(b))>0)o.write(b,0,n);}}
    private void copyFile(File a,File b)throws Exception{try(InputStream i=new FileInputStream(a);OutputStream o=new FileOutputStream(b)){byte[]x=new byte[8192];int n;while((n=i.read(x))>0)o.write(x,0,n);}}
    private byte[] readBytes(File f)throws Exception{try(InputStream i=new FileInputStream(f);ByteArrayOutputStream b=new ByteArrayOutputStream()){byte[]x=new byte[8192];int n;while((n=i.read(x))>0)b.write(x,0,n);return b.toByteArray();}}
    private String readText(File f)throws Exception{return new String(readBytes(f),StandardCharsets.UTF_8);}
    private void writeBytes(File f,byte[] b)throws Exception{try(OutputStream o=new FileOutputStream(f)){o.write(b);}}

    private void unzipSafely(File zip,File out)throws Exception{
        try(ZipInputStream z=new ZipInputStream(new FileInputStream(zip))){
            ZipEntry e; byte[] buf=new byte[8192];
            while((e=z.getNextEntry())!=null){
                File dest=new File(out,e.getName());
                String root=out.getCanonicalPath()+File.separator;
                if(!dest.getCanonicalPath().startsWith(root))throw new IOException("Güvensiz ZIP yolu.");
                if(e.isDirectory()){dest.mkdirs();continue;}
                File parent=dest.getParentFile();if(parent!=null)parent.mkdirs();
                try(OutputStream o=new FileOutputStream(dest)){int n;while((n=z.read(buf))>0)o.write(buf,0,n);}
            }
        }
    }
    private void deleteRecursive(File f){if(f==null||!f.exists())return;File[]fs=f.listFiles();if(fs!=null)for(File x:fs)deleteRecursive(x);f.delete();}
    
    private String getJson(String url,String token)throws Exception{ return request(url,"GET",null,token); }
    private void postJson(String url,String body,String token)throws Exception{ request(url,"POST",body,token); }
    private void putJson(String url,String body,String token)throws Exception{ request(url,"PUT",body,token); }
    private String request(String u,String method,String body,String token)throws Exception{
        HttpURLConnection c=(HttpURLConnection)new URL(u).openConnection();c.setRequestMethod(method);c.setConnectTimeout(20000);c.setReadTimeout(30000);c.setRequestProperty("Accept","application/vnd.github+json");c.setRequestProperty("Authorization","Bearer "+token);c.setRequestProperty("User-Agent","SirinEngine");c.setRequestProperty("X-GitHub-Api-Version","2022-11-28");
        if(body!=null){c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");try(OutputStream o=c.getOutputStream()){o.write(body.getBytes(StandardCharsets.UTF_8));}}
        int code=c.getResponseCode();InputStream in=code>=200&&code<300?c.getInputStream():c.getErrorStream();String s=new String(readAll(in),StandardCharsets.UTF_8);if(code<200||code>=300)throw new IOException("GitHub HTTP "+code+": "+s);return s;
    }
    private byte[] downloadBytes(String u,String token)throws Exception{
        HttpURLConnection c=(HttpURLConnection)new URL(u).openConnection();c.setInstanceFollowRedirects(true);c.setRequestProperty("Authorization","Bearer "+token);c.setRequestProperty("User-Agent","SirinEngine");int code=c.getResponseCode();if(code<200||code>=300)throw new IOException("Download HTTP "+code);return readAll(c.getInputStream());
    }
    private byte[] readAll(InputStream in)throws Exception{try(InputStream x=in;ByteArrayOutputStream b=new ByteArrayOutputStream()){byte[]z=new byte[16384];int n;while((n=x.read(z))>0)b.write(z,0,n);return b.toByteArray();}}
}
