package com.sirin.engine;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.*;

public final class LocalExportOutput {
    private LocalExportOutput(){}

    public static File saveToDownloads(Context context, File source, String name) throws IOException {
        if(!source.isFile() || source.length()==0) throw new IOException("Export çıktısı yok veya boş.");
        if(Build.VERSION.SDK_INT>=29){
            ContentResolver resolver=context.getContentResolver();
            ContentValues values=new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME,name);
            values.put(MediaStore.Downloads.MIME_TYPE,"application/octet-stream");
            values.put(MediaStore.Downloads.RELATIVE_PATH,Environment.DIRECTORY_DOWNLOADS);
            values.put(MediaStore.Downloads.IS_PENDING,1);
            Uri uri=resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,values);
            if(uri==null) throw new IOException("Download kaydı oluşturulamadı.");
            try(InputStream in=new BufferedInputStream(new FileInputStream(source)); OutputStream out=new BufferedOutputStream(resolver.openOutputStream(uri))){
                if(out==null) throw new IOException("Download çıktısı açılamadı.");
                byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1)out.write(b,0,n);
            }catch(Exception e){
                resolver.delete(uri,null,null);
                if(e instanceof IOException) throw (IOException)e;
                throw new IOException("Export Download'a yazılamadı.",e);
            }
            ContentValues done=new ContentValues();done.put(MediaStore.Downloads.IS_PENDING,0);resolver.update(uri,done,null,null);
            return new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),name);
        }
        File dir=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if(!dir.exists()&&!dir.mkdirs()) throw new IOException("Download klasörü oluşturulamadı.");
        File target=new File(dir,name);
        try(InputStream in=new BufferedInputStream(new FileInputStream(source));OutputStream out=new BufferedOutputStream(new FileOutputStream(target))){
            byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1)out.write(b,0,n);
        }
        return target;
    }
}
