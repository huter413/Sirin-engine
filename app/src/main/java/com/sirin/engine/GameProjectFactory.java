package com.sirin.engine;

import android.content.Context;
import java.io.*;
import java.nio.charset.StandardCharsets;

public final class GameProjectFactory {
    private GameProjectFactory(){}

    public static File create(Context context, String name, String editor) throws IOException {
        String safe=name==null? "Sirin Game" : name.trim();
        if(safe.isEmpty()) safe="Sirin Game";
        File root=new File(context.getCacheDir(), "created-project");
        delete(root);
        if(!root.mkdirs()) throw new IOException("Proje klasörü oluşturulamadı.");

        String project="{\n"+
                "  \"format\": 1,\n"+
                "  \"engine\": \"Sirin Engine\",\n"+
                "  \"engine_version\": \"1.0.0\",\n"+
                "  \"project_name\": \""+json(safe)+"\",\n"+
                "  \"version\": \"1.0.0\",\n"+
                "  \"entry\": \"main.html\",\n"+
                "  \"icon\": \"icon.svg\",\n"+
                "  \"editor\": \""+json(editor)+"\",\n"+
                "  \"platforms\": [\"android\", \"windows\"],\n"+
                "  \"orientation\": \"landscape\",\n"+
                "  \"scene\": \"scene.json\"\n"+
                "}\n";
        write(new File(root,"project.sr"),project);
        write(new File(root,"scene.json"),"{\n  \"type\": \""+json(editor)+"\",\n  \"objects\": []\n}\n");
        write(new File(root,"main.html"),html(safe,editor));
        write(new File(root,"icon.svg"),"<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 128 128\"><rect width=\"128\" height=\"128\" rx=\"24\" fill=\"#101820\"/><rect x=\"18\" y=\"18\" width=\"92\" height=\"92\" rx=\"18\" fill=\"#56d6ff\"/><path d=\"M36 86l8-40 15-18h10l15 18 8 40H36z\" fill=\"#101820\"/></svg>\n");
        return root;
    }

    private static String html(String name,String editor){
        String mode="3D".equals(editor) ? "3D" : "2D";
        return "<!doctype html><html><head><meta name=\"viewport\" content=\"width=device-width,initial-scale=1,user-scalable=no\"><title>"+esc(name)+"</title>"+
        "<style>html,body{margin:0;width:100%;height:100%;overflow:hidden;background:#101820;color:white;font-family:sans-serif}canvas{display:block;width:100%;height:100%}.hud{position:fixed;left:16px;top:16px;padding:10px 14px;border-radius:10px;background:#0008}</style></head>"+
        "<body><div class=\"hud\">"+esc(name)+" • Sirin Engine • "+mode+"</div><canvas id=\"game\"></canvas><script>"+
        "const c=document.getElementById('game'),x=c.getContext('2d');function resize(){c.width=innerWidth*devicePixelRatio;c.height=innerHeight*devicePixelRatio;x.setTransform(devicePixelRatio,0,0,devicePixelRatio,0,0)}addEventListener('resize',resize);resize();"+
        "let t=0;function loop(){t+=.016;x.clearRect(0,0,innerWidth,innerHeight);x.fillStyle='#162432';x.fillRect(0,0,innerWidth,innerHeight);x.strokeStyle='#2b4155';for(let i=0;i<innerWidth;i+=50){x.beginPath();x.moveTo(i,0);x.lineTo(i,innerHeight);x.stroke()}for(let i=0;i<innerHeight;i+=50){x.beginPath();x.moveTo(0,i);x.lineTo(innerWidth,i);x.stroke()}x.fillStyle='#56d6ff';x.fillRect(innerWidth/2-45+Math.sin(t)*120,innerHeight/2-45,90,90);requestAnimationFrame(loop)}loop();</script></body></html>";
    }
    private static String json(String s){return s.replace("\\","\\\\").replace("\"","\\\"");}
    private static String esc(String s){return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");}
    private static void write(File f,String s)throws IOException{try(OutputStream o=new FileOutputStream(f)){o.write(s.getBytes(StandardCharsets.UTF_8));}}
    private static void delete(File f){if(f==null||!f.exists())return;File[] a=f.listFiles();if(a!=null)for(File c:a)delete(c);f.delete();}
}
