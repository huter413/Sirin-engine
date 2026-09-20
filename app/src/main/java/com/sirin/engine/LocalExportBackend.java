package com.sirin.engine;

import java.io.*;
import java.util.*;
import java.util.zip.*;

public final class LocalExportBackend {
    private LocalExportBackend(){}

    public static File stageProject(File projectDir, File outputDir) throws IOException {
        if(projectDir==null || !projectDir.isDirectory()) throw new IOException("Proje klasörü yok.");
        if(outputDir.exists()) delete(outputDir);
        if(!outputDir.mkdirs()) throw new IOException("Export klasörü oluşturulamadı.");
        File bundle=new File(outputDir,"game.srgame");
        try(ZipOutputStream zip=new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(bundle)))){
            add(projectDir,projectDir,zip);
        }
        return bundle;
    }

    private static void add(File root,File file,ZipOutputStream zip)throws IOException{
        String rel=root.toURI().relativize(file.toURI()).getPath();
        if(file.isDirectory()){
            if(!rel.isEmpty()) zip.putNextEntry(new ZipEntry(rel.endsWith("/")?rel:rel+"/"));
            if(!rel.isEmpty()) zip.closeEntry();
            File[] children=file.listFiles();
            if(children!=null) for(File child:children) add(root,child,zip);
        }else{
            zip.putNextEntry(new ZipEntry(rel));
            try(InputStream in=new BufferedInputStream(new FileInputStream(file))){
                byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1)zip.write(b,0,n);
            }
            zip.closeEntry();
        }
    }
    private static void delete(File f){if(f==null||!f.exists())return;File[] a=f.listFiles();if(a!=null)for(File c:a)delete(c);f.delete();}
}
