package com.sirin.engine;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.Settings;
import android.text.InputType;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.json.JSONObject;

public class MainActivity extends Activity {
    private static final int PICK_ZIP = 41;
    private static final int CREATE_APK = 42;
    private static final int STORAGE_PERMISSION = 43;

    private TextView status, errorText;
    private WebView preview;
    private LinearLayout editorRoot, errorPanel;
    private File projectDir, selectedZip;
    private String projectName = "Sirin Project";
    private String entryFile = "main.html";
    private final ArrayList<String> errors = new ArrayList<>();

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setFlags(1024, 1024);
        getWindow().addFlags(128);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        buildHome();
    }

    private int dp(int n){ return (int)(n*getResources().getDisplayMetrics().density+0.5f); }
    private TextView text(String s,float size){ TextView v=new TextView(this);v.setText(s);v.setTextColor(Color.WHITE);v.setTextSize(size);return v; }
    private Button button(String s){ Button b=new Button(this);b.setText(s);b.setTextSize(16);b.setAllCaps(false);b.setMinHeight(dp(58));return b; }
    private LinearLayout base(){ LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);r.setPadding(dp(18),dp(14),dp(18),dp(14));r.setBackgroundColor(Color.rgb(10,14,20));return r; }

    private void buildHome(){
        LinearLayout root=base();
        TextView title=text("ŞİRİN ENGINE",34); title.setGravity(View.TEXT_ALIGNMENT_GRAVITY); root.addView(title);
        TextView sub=text("Projeni aç • düzenle • test et • APK'yı doğrudan cihazda dışa aktar",16);sub.setTextColor(Color.LTGRAY);root.addView(sub);

        LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);
        Button open=button("ZIP Aç");open.setOnClickListener(v->pickZip());row.addView(open,new LinearLayout.LayoutParams(0,dp(72),1));
        Button create=button("Yeni Oyun");create.setOnClickListener(v->createGame());row.addView(create,new LinearLayout.LayoutParams(0,dp(72),1));
        Button settings=button("Ayarlar");settings.setOnClickListener(v->showSettings());row.addView(settings,new LinearLayout.LayoutParams(0,dp(72),1));
        root.addView(row);

        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(16),dp(12),dp(16),dp(12));card.setBackgroundColor(Color.rgb(20,28,38));
        card.addView(text("OYUN OLUŞTURMA + YEREL DIŞA AKTARMA",20));
        card.addView(text("Yeni Oyun ile 2D/3D proje oluşturabilir, düzenleyebilir, test edebilir ve oyun paketini Download klasörüne aktarabilirsin.",14));
        root.addView(card,new LinearLayout.LayoutParams(-1,dp(120)));

        status=text("Durum: Hazır\nProje: Seçilmedi\nDerleme: Yerel",17);root.addView(status,new LinearLayout.LayoutParams(-1,0,1));
        TextView help=text("Not: Android'in yeni sürümlerinde Download'a kaydetmek için sistem dosya erişimi kullanılır; eski sürümlerde gerekli depolama izni istenir.",14);help.setTextColor(Color.GRAY);root.addView(help);
        setContentView(root);
    }

    private void createGame(){
        final EditText name=new EditText(this); name.setHint("Oyun adı"); name.setSingleLine(true);
        LinearLayout box=base(); box.setPadding(dp(8),0,dp(8),0); box.addView(name);
        new AlertDialog.Builder(this).setTitle("Yeni Oyun").setView(box)
            .setItems(new String[]{"2D Oyun","3D Oyun"},(d,which)->{
                try{
                    String n=name.getText().toString().trim(); if(n.isEmpty()) n=which==0?"Sirin 2D Game":"Sirin 3D Game";
                    projectDir=GameProjectFactory.create(this,n,which==0?"2d":"3d");
                    selectedZip=null; readProjectMetadata(); showEditor(); setStatus("Yeni oyun oluşturuldu: "+projectName);
                }catch(Exception e){addError("Yeni oyun: "+e.getMessage());setStatus("Oyun oluşturulamadı.");}
            }).setNegativeButton("İptal",null).show();
    }

    private void pickZip(){
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("application/zip");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,PICK_ZIP);
    }

    @Override protected void onActivityResult(int req,int result,Intent data){
        super.onActivityResult(req,result,data);
        if(req==PICK_ZIP&&result==RESULT_OK&&data!=null){
            try{
                selectedZip=new File(getCacheDir(),"selected-project.zip");copyUriToFile(data.getData(),selectedZip);
                projectDir=new File(getCacheDir(),"sirin_project");deleteRecursive(projectDir);if(!projectDir.mkdirs())throw new IOException("Proje klasörü oluşturulamadı");
                unzipSafely(selectedZip,projectDir);readProjectMetadata();showEditor();setStatus("Proje açıldı: "+projectName);
            }catch(Exception e){addError("ZIP: "+e.getMessage());setStatus("ZIP hatası: "+e.getMessage());}
        }
        if(req==CREATE_APK&&result==RESULT_OK&&data!=null){
            File built=new File(getCacheDir(),"SirinEngine-APK.apk");
            try(InputStream in=new FileInputStream(built);OutputStream out=getContentResolver().openOutputStream(data.getData())){
                byte[] buf=new byte[16384];int n;while((n=in.read(buf))>0)out.write(buf,0,n);
                Toast.makeText(this,"APK Download'a kaydedildi.",Toast.LENGTH_LONG).show();setStatus("APK kaydedildi.");
            }catch(Exception e){addError("APK kaydetme: "+e.getMessage());setStatus("APK kaydetme hatası.");}
        }
    }

    private void showEditor(){
        editorRoot=base();
        LinearLayout top=new LinearLayout(this);top.setOrientation(LinearLayout.HORIZONTAL);
        Button back=button("← Ana");back.setOnClickListener(v->buildHome());top.addView(back,new LinearLayout.LayoutParams(dp(92),dp(58)));
        Button b2=button("2D");b2.setOnClickListener(v->show2DEditor());top.addView(b2,new LinearLayout.LayoutParams(0,dp(58),1));
        Button b3=button("3D");b3.setOnClickListener(v->show3DEditor());top.addView(b3,new LinearLayout.LayoutParams(0,dp(58),1));
        Button play=button("▶ Test");play.setOnClickListener(v->launchGame());top.addView(play,new LinearLayout.LayoutParams(0,dp(58),1));
        Button apk=button("OYUNU DIŞA AKTAR");apk.setTextSize(16);apk.setOnClickListener(v->startBuild());top.addView(apk,new LinearLayout.LayoutParams(0,dp(58),1));
        Button files=button("Dosyalar");files.setOnClickListener(v->showFiles());top.addView(files,new LinearLayout.LayoutParams(0,dp(58),1));
        editorRoot.addView(top);

        status=text("Proje: "+projectName+"\nGiriş: "+entryFile+"\nDerleme: Cihaz üzerinde",15);editorRoot.addView(status);
        preview=new WebView(this);configureWebView(preview);editorRoot.addView(preview,new LinearLayout.LayoutParams(-1,0,1));

        errorPanel=new LinearLayout(this);errorPanel.setOrientation(LinearLayout.VERTICAL);errorPanel.setBackgroundColor(Color.rgb(35,20,24));errorPanel.setVisibility(View.GONE);
        errorText=text("HATA PANELİ\nHenüz hata yok.",14);ScrollView scroll=new ScrollView(this);scroll.addView(errorText);errorPanel.addView(scroll,new LinearLayout.LayoutParams(-1,dp(110)));editorRoot.addView(errorPanel);
        setContentView(editorRoot);show3DEditor();
    }

    private void configureWebView(WebView w){
        WebSettings s=w.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setAllowFileAccess(true);s.setAllowContentAccess(true);s.setBuiltInZoomControls(false);s.setDisplayZoomControls(false);w.setLayerType(View.LAYER_TYPE_HARDWARE,null);
        w.setWebViewClient(new WebViewClient(){
            @Override public void onPageFinished(WebView v,String u){v.evaluateJavascript("(function(){window.addEventListener('error',function(e){console.error('Sirin '+e.message)});window.addEventListener('unhandledrejection',function(e){console.error('Sirin Promise '+e.reason)});})();",null);}
            @Override public void onReceivedError(WebView v,WebResourceRequest r,WebResourceError e){addError("WebView: "+e.getErrorCode()+" "+e.getDescription());}
        });
        w.setWebChromeClient(new WebChromeClient(){@Override public boolean onConsoleMessage(ConsoleMessage m){if(m.messageLevel()==ConsoleMessage.MessageLevel.ERROR)addError("JS: "+m.message());return true;}});
    }

    private void show2DEditor(){if(preview==null)return;String html="<!doctype html><html><body style='margin:0;background:#101720;color:white;font-family:sans-serif'><div style='padding:10px;font-weight:bold'>2D SAHNE • "+esc(projectName)+"</div><canvas id=c style='width:100%;height:calc(100vh - 50px)'></canvas><script>const c=document.getElementById('c'),x=c.getContext('2d');c.width=1200;c.height=700;x.fillStyle='#182432';x.fillRect(0,0,c.width,c.height);x.strokeStyle='#33495e';for(let i=0;i<c.width;i+=50){x.beginPath();x.moveTo(i,0);x.lineTo(i,c.height);x.stroke()}for(let i=0;i<c.height;i+=50){x.beginPath();x.moveTo(0,i);x.lineTo(c.width,i);x.stroke()}</script></body></html>";preview.loadDataWithBaseURL(null,html,"text/html","UTF-8",null);setStatus("2D editör açık.");}
    private void show3DEditor(){if(preview==null||projectDir==null)return;File e=new File(projectDir,entryFile);if(e.isFile()){preview.loadUrl(Uri.fromFile(e).toString());setStatus("3D önizleme açık.");}else{addError("Giriş dosyası bulunamadı: "+entryFile);}}
    private void launchGame(){show3DEditor();}

    private void startBuild(){
        if(projectDir==null){setStatus("Önce bir oyun oluştur veya ZIP aç.");return;}
        if(Build.VERSION.SDK_INT<=28&&checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},STORAGE_PERMISSION);return;
        }
        new AlertDialog.Builder(this).setTitle("Oyunu Dışa Aktar").setMessage("Proje doğrulanıp yerel .srgame oyun paketi oluşturulacak ve Download klasörüne kaydedilecek. ZIP yeniden adlandırılmaz; proje dosyaları gerçek bir export paketine dönüştürülür.").setPositiveButton("Dışa Aktar",(d,w)->performLocalBuild()).setNegativeButton("İptal",null).show();
    }

    private void performLocalBuild(){
        setStatus("Yerel oyun export hazırlanıyor…");
        new Thread(()->{
            try{
                File outDir=new File(getFilesDir(),"sirin-export");
                File bundle=LocalExportBackend.stageProject(projectDir,outDir);
                File downloads=LocalExportOutput.saveToDownloads(this,bundle,projectName.replaceAll("[^A-Za-z0-9._-]+","_")+".srgame");
                runOnUiThread(()->Toast.makeText(this,"Oyun paketi Download'a kaydedildi.",Toast.LENGTH_LONG).show());
                setStatus("Oyun paketi hazır: "+downloads.getName());
            }catch(Exception e){
                addError("Yerel export: "+e.getMessage());
                setStatus("Oyun dışa aktarılamadı.");
            }
        }).start();
    }

    private void showFiles(){if(projectDir==null)return;LinearLayout root=base();Button b=button("← Editöre dön");b.setOnClickListener(v->showEditor());root.addView(b);root.addView(text("PROJE DOSYALARI\n\n"+listFiles(projectDir,""),16),new LinearLayout.LayoutParams(-1,0,1));setContentView(root);}
    private String listFiles(File d,String p){StringBuilder o=new StringBuilder();File[] fs=d.listFiles();if(fs==null)return "";Arrays.sort(fs,Comparator.comparing(File::getName));for(File f:fs){o.append(p).append(f.isDirectory()?"[DIR] ":"").append(f.getName()).append("\n");if(f.isDirectory())o.append(listFiles(f,p+"  "));}return o.toString();}

    private void showSettings(){
        new AlertDialog.Builder(this).setTitle("Şirin Engine").setMessage("Yerel APK dışa aktarma modu\n\nGitHub hesabı, token ve uzak CI derlemesi kullanılmıyor. Download'a kayıt Android sistem API'leriyle yapılır.").setPositiveButton("Tamam",null).show();
    }

    @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults){super.onRequestPermissionsResult(requestCode,permissions,grantResults);if(requestCode==STORAGE_PERMISSION){if(grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED)performLocalBuild();else setStatus("Depolama izni verilmedi.");}}

    private void setStatus(String s){runOnUiThread(()->{if(status!=null)status.setText("Durum: "+s);});}
    private void addError(String s){if(s==null||s.trim().isEmpty())return;errors.add(s);while(errors.size()>80)errors.remove(0);runOnUiThread(()->{if(errorText!=null){StringBuilder b=new StringBuilder("HATA PANELİ\n");for(String e:errors)b.append("• ").append(e).append("\n");errorText.setText(b.toString());}});}
    private String esc(String s){return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");}

    private void readProjectMetadata() throws Exception{
        File meta=new File(projectDir,"project.sr");if(!meta.isFile())throw new IOException("project.sr zorunlu.");
        JSONObject o=new JSONObject(readText(meta));if(!"Sirin Engine".equals(o.optString("engine","")))throw new IOException("Bu Sirin Engine projesi değil.");
        if(!o.has("format"))throw new IOException("project.sr format eksik.");
        projectName=o.optString("project_name","Sirin Project");entryFile=o.optString("entry","main.html");if(entryFile.isEmpty())throw new IOException("entry eksik.");
        File entry=new File(projectDir,entryFile);if(!entry.getCanonicalPath().startsWith(projectDir.getCanonicalPath()+File.separator)||!entry.isFile())throw new IOException("Giriş dosyası bulunamadı: "+entryFile);
    }

    private void copyUriToFile(Uri u,File out)throws Exception{InputStream in=getContentResolver().openInputStream(u);if(in==null)throw new IOException("ZIP okunamadı");try(InputStream a=in;OutputStream b=new FileOutputStream(out)){byte[] x=new byte[16384];int n;while((n=a.read(x))>0)b.write(x,0,n);}}
    private byte[] readBytes(File f)throws Exception{try(InputStream i=new FileInputStream(f);ByteArrayOutputStream o=new ByteArrayOutputStream()){byte[] b=new byte[16384];int n;while((n=i.read(b))>0)o.write(b,0,n);return o.toByteArray();}}
    private String readText(File f)throws Exception{return new String(readBytes(f),StandardCharsets.UTF_8);}
    private void unzipSafely(File z,File out)throws Exception{try(ZipInputStream in=new ZipInputStream(new FileInputStream(z))){ZipEntry e;byte[] b=new byte[8192];String root=out.getCanonicalPath()+File.separator;while((e=in.getNextEntry())!=null){File d=new File(out,e.getName());if(!d.getCanonicalPath().startsWith(root))throw new IOException("Güvensiz ZIP yolu.");if(e.isDirectory()){d.mkdirs();continue;}File p=d.getParentFile();if(p!=null)p.mkdirs();try(OutputStream o=new FileOutputStream(d)){int n;while((n=in.read(b))>0)o.write(b,0,n);}}}}
    private void deleteRecursive(File f){if(f==null||!f.exists())return;File[] fs=f.listFiles();if(fs!=null)for(File x:fs)deleteRecursive(x);f.delete();}
}
