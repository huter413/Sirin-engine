package com.sirin.engine;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.*;

public final class LocalApkOutput {
    private LocalApkOutput() {}

    public static Uri saveToDownloads(Context context, File apkFile, String displayName) throws IOException {
        if (!apkFile.isFile() || apkFile.length() == 0) {
            throw new IOException("APK çıktısı yok veya boş.");
        }

        if (Build.VERSION.SDK_INT >= 29) {
            ContentResolver resolver = context.getContentResolver();
            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, displayName);
            values.put(MediaStore.Downloads.MIME_TYPE, "application/vnd.android.package-archive");
            values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
            values.put(MediaStore.Downloads.IS_PENDING, 1);

            Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri == null) throw new IOException("Download kaydı oluşturulamadı.");

            try (InputStream in = new BufferedInputStream(new FileInputStream(apkFile));
                 OutputStream out = new BufferedOutputStream(resolver.openOutputStream(uri))) {
                if (out == null) throw new IOException("Download çıktısı açılamadı.");
                byte[] buffer = new byte[64 * 1024];
                int n;
                while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
                out.flush();
            } catch (Exception e) {
                resolver.delete(uri, null, null);
                if (e instanceof IOException) throw (IOException)e;
                throw new IOException("APK Download'a yazılamadı.", e);
            }

            ContentValues done = new ContentValues();
            done.put(MediaStore.Downloads.IS_PENDING, 0);
            resolver.update(uri, done, null, null);
            return uri;
        }

        File downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (!downloads.exists() && !downloads.mkdirs()) {
            throw new IOException("Download klasörü oluşturulamadı.");
        }

        File target = new File(downloads, displayName);
        try (InputStream in = new BufferedInputStream(new FileInputStream(apkFile));
             OutputStream out = new BufferedOutputStream(new FileOutputStream(target))) {
            byte[] buffer = new byte[64 * 1024];
            int n;
            while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
        }
        return Uri.fromFile(target);
    }
}
