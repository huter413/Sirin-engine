package com.sirin.engine;
import android.app.*;
import android.os.*;
import android.content.*;
import android.net.Uri;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.view.*;
import android.widget.*;

public class MainActivity extends Activity {
 private TextView status;
 private static final int PICK_ZIP=41;
 public void onCreate(Bundle b){
  super.onCreate(b);
  getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
  setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
  getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
  LinearLayout root=new LinearLayout(this);
  root.setOrientation(LinearLayout.VERTICAL);
  root.setPadding(32,24,32,24);
  root.setBackgroundColor(Color.rgb(12,16,22));
  TextView title=new TextView(this); title.setText("SIRIN ENGINE"); title.setTextColor(Color.WHITE); title.setTextSize(32); root.addView(title);
  TextView sub=new TextView(this); sub.setText("Dokunmatik mobil APK proje yoneticisi | YATAY"); sub.setTextColor(Color.LTGRAY); sub.setTextSize(17); root.addView(sub);
  LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL);
  Button zip=new Button(this); zip.setText("PROJE ZIP SEC"); zip.setOnClickListener(v->pickZip()); row.addView(zip,new LinearLayout.LayoutParams(0,100,1));
  Button check=new Button(this); check.setText("PROJEYI KONTROL ET"); check.setOnClickListener(v->setStatus("ZIP secildiginde proje yapisi kontrol edilecek.")); row.addView(check,new LinearLayout.LayoutParams(0,100,1));
  Button build=new Button(this); build.setText("APK DERLE"); build.setOnClickListener(v->setStatus("APK derleme CI ortaminda hazir.")); row.addView(build,new LinearLayout.LayoutParams(0,100,1));
  root.addView(row);
  status=new TextView(this); status.setTextColor(Color.WHITE); status.setTextSize(18); status.setPadding(0,32,0,0);
  status.setText("Durum: Hazir\nZIP: Secilmedi\nCikis: Android APK\nEkran: Yatay\nDonanim hizlandirma: Acik");
  root.addView(status,new LinearLayout.LayoutParams(-1,0,1));
  TextView info=new TextView(this); info.setTextColor(Color.GRAY); info.setText("Sirin Engine yerel Android arayuzudur. Keyfi ZIP projelerinin derlenmesi icin Android build ortam gerekir."); root.addView(info);
  setContentView(root);
 }
 private void pickZip(){ Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT); i.setType("application/zip"); i.addCategory(Intent.CATEGORY_OPENABLE); startActivityForResult(i,PICK_ZIP); }
 protected void onActivityResult(int r,int c,Intent d){ super.onActivityResult(r,c,d); if(r==PICK_ZIP&&c==RESULT_OK&&d!=null){ Uri u=d.getData(); setStatus("ZIP secildi: "+u); } }
 private void setStatus(String s){ if(status!=null) status.setText("Durum: "+s); }
}
