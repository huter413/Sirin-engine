package com.sirin.engine;

import android.content.Context;

import com.android.apksig.ApkSigner;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.io.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.zip.*;

public final class ApkExportBackend {
    private ApkExportBackend(){}

    public static File buildAndSign(Context context, File projectDir, String projectName) throws Exception {
        if(projectDir==null || !projectDir.isDirectory()) throw new IOException("Proje klasörü yok.");
        File template = new File(context.getCacheDir(), "runtime-template.apk");
        copyAsset(context, "runtime-template.apk", template);

        File work = new File(context.getCacheDir(), "apk-export");
        delete(work);
        if(!work.mkdirs()) throw new IOException("APK export klasörü oluşturulamadı.");

        File unsigned = new File(work, "game-unsigned.apk");
        File signed = new File(work, "game-release.apk");
        injectProject(template, unsigned, projectDir);

        KeyPair pair = createKeyPair();
        X509Certificate cert = createCertificate(pair);
        ApkSigner.SignerConfig signer = new ApkSigner.SignerConfig.Builder(
                "sirin-export", pair.getPrivate(), Collections.singletonList(cert)).build();

        new ApkSigner.Builder(Collections.singletonList(signer))
                .setInputApk(unsigned)
                .setOutputApk(signed)
                .setV1SigningEnabled(true)
                .setV2SigningEnabled(true)
                .setV3SigningEnabled(true)
                .setV4SigningEnabled(false)
                .setMinSdkVersion(26)
                .build()
                .sign();

        if(!signed.isFile() || signed.length()==0) throw new IOException("İmzalı APK oluşmadı.");
        return signed;
    }

    private static void injectProject(File template, File out, File projectDir) throws Exception {
        try(ZipFile in=new ZipFile(template);
            ZipOutputStream zip=new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(out)))) {
            byte[] b=new byte[65536];
            Enumeration<? extends ZipEntry> entries=in.entries();
            while(entries.hasMoreElements()){
                ZipEntry src=entries.nextElement();
                if(src.getName().startsWith("META-INF/")) continue;
                ZipEntry dst=new ZipEntry(src.getName());
                if("resources.arsc".equals(src.getName())){
                    byte[] data=readEntry(in,src);
                    dst.setMethod(ZipEntry.STORED);
                    dst.setSize(data.length);
                    dst.setCompressedSize(data.length);
                    dst.setCrc(crc(data));
                    dst.setExtra(new byte[0]);
                    zip.putNextEntry(dst); zip.write(data); zip.closeEntry();
                } else {
                    dst.setMethod(src.getMethod());
                    if(src.getMethod()==ZipEntry.STORED){
                        byte[] data=readEntry(in,src);
                        dst.setSize(data.length); dst.setCompressedSize(data.length); dst.setCrc(crc(data));
                        zip.putNextEntry(dst); zip.write(data);
                    } else {
                        zip.putNextEntry(dst);
                        try(InputStream x=in.getInputStream(src)){int n;while((n=x.read(b))!=-1)zip.write(b,0,n);}
                    }
                    zip.closeEntry();
                }
            }
            addDirectory(zip, projectDir, projectDir, b);
        }
        validateResourcesEntry(out);
    }

    private static void addDirectory(ZipOutputStream zip, File root, File file, byte[] b) throws IOException {
        String rel=root.toURI().relativize(file.toURI()).getPath();
        if(file.isDirectory()){
            if(!rel.isEmpty()){
                ZipEntry d=new ZipEntry("assets/sirin_project/"+rel);
                zip.putNextEntry(d); zip.closeEntry();
            }
            File[] children=file.listFiles();
            if(children!=null) for(File child:children) addDirectory(zip,root,child,b);
            return;
        }
        ZipEntry e=new ZipEntry("assets/sirin_project/"+rel);
        zip.putNextEntry(e);
        try(InputStream in=new BufferedInputStream(new FileInputStream(file))){
            int n;while((n=in.read(b))!=-1)zip.write(b,0,n);
        }
        zip.closeEntry();
    }

    private static void validateResourcesEntry(File apk) throws IOException {
        try (ZipFile z = new ZipFile(apk)) {
            ZipEntry e = z.getEntry("resources.arsc");
            if (e == null) throw new IOException("resources.arsc eksik.");
            if (e.getMethod() != ZipEntry.STORED) {
                throw new IOException("resources.arsc sıkıştırılmış olamaz.");
            }
        }
    }

    private static KeyPair createKeyPair() throws GeneralSecurityException {
        KeyPairGenerator g=KeyPairGenerator.getInstance("RSA");
        g.initialize(3072);
        return g.generateKeyPair();
    }

    private static X509Certificate createCertificate(KeyPair pair) throws Exception {
        if(Security.getProvider("BC")==null) Security.addProvider(new BouncyCastleProvider());
        long now=System.currentTimeMillis();
        Date from=new Date(now-60000L), to=new Date(now+3650L*86400000L);
        X500Name name=new X500Name("CN=Sirin Engine Local Export");
        BigInteger serial=new BigInteger(160,new SecureRandom()).abs();
        X509v3CertificateBuilder builder=new JcaX509v3CertificateBuilder(
                name, serial, from, to, name, pair.getPublic());
        ContentSigner signer=new JcaContentSignerBuilder("SHA256withRSA")
                .setProvider("BC").build(pair.getPrivate());
        X509CertificateHolder holder=builder.build(signer);
        return new JcaX509CertificateConverter().setProvider("BC").getCertificate(holder);
    }

    private static void copyAsset(Context c,String name,File out)throws IOException{
        try(InputStream in=c.getAssets().open(name);OutputStream o=new BufferedOutputStream(new FileOutputStream(out))){
            byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1)o.write(b,0,n);
        }
    }
    private static byte[] readEntry(ZipFile z,ZipEntry e)throws IOException{
        try(InputStream in=z.getInputStream(e);ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1)out.write(b,0,n);
            return out.toByteArray();
        }
    }
    private static long crc(byte[] data){
        CRC32 c=new CRC32();c.update(data);return c.getValue();
    }
    private static void delete(File f){
        if(f==null||!f.exists())return;File[] a=f.listFiles();if(a!=null)for(File x:a)delete(x);f.delete();
    }
}
