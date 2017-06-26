package com.dji.demo;

import android.os.Environment;
import android.text.TextUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;

public class FileUtils {
    public static final String COREFOLDERNAME = ".djidemo";
    public static final String CACHEPATH;
    public static final String CACHELOGPATH;
    public static final String CACHECRASHPATH;
    public static final String CACHEDBPATH;
    public static final String CACHEAPKPATH;
    public static final String CACHEMEMPATH;

    static {
        CACHEPATH = File.separator + "Logs";
        CACHELOGPATH = CACHEPATH + File.separator + "Log";
        CACHECRASHPATH = CACHEPATH + File.separator + "Crash";
        CACHEDBPATH = CACHEPATH + File.separator + "Database";
        CACHEAPKPATH = CACHEPATH + File.separator + "Apk";
        CACHEMEMPATH = CACHEPATH + File.separator + "Mem";
    }

    public FileUtils() {
    }

    public static String getCachePath() {
        FPVDemoApplication context = FPVDemoApplication.getInstance();

        String cachePath;
        try {
            boolean e = false;
            boolean cacheDir1 = false;
            String exterState = Environment.getExternalStorageState();
            e = exterState.equalsIgnoreCase("mounted");
            if (e) {
                cachePath = Environment.getExternalStorageDirectory() + File.separator + "djiDemo";
            } else {
                cachePath = context.getCacheDir().getPath();
            }
        } catch (Exception var5) {
            File cacheDir = context.getCacheDir();
            if(cacheDir == null) {
                return null;
            }

            cachePath = cacheDir.getPath();
        }

        if(cachePath.endsWith(File.separator)) {
            cachePath = cachePath.substring(0, cachePath.length() - 1);
        }

        return cachePath;
    }

    public static String getStringFromFile(String filePath) {
        return getStringFromFile(filePath, (String)null);
    }

    public static String getStringFromFile(String filePath, String encoding) {
        if(TextUtils.isEmpty(filePath)) {
            return null;
        } else {
            File file = new File(filePath);
            if(!file.exists()) {
                return "";
            } else {
                String content = "";
                FileInputStream input = null;
                InputStreamReader reader = null;
                StringWriter sw = new StringWriter();

                try {
                    input = new FileInputStream(filePath);
                    if(TextUtils.isEmpty(encoding)) {
                        reader = new InputStreamReader(input);
                    } else {
                        reader = new InputStreamReader(input, encoding);
                    }

                    char[] e = new char[4096];
                    boolean n = false;

                    int n1;
                    while((n1 = reader.read(e)) != -1) {
                        sw.write(e, 0, n1);
                    }

                    content = sw.toString();
                } catch (Exception var25) {
                    var25.printStackTrace();
                } finally {
                    if(sw != null) {
                        try {
                            sw.close();
                        } catch (IOException var24) {
                            var24.printStackTrace();
                        }

                        sw = null;
                    }

                    if(reader != null) {
                        try {
                            reader.close();
                        } catch (IOException var23) {
                            var23.printStackTrace();
                        }

                        reader = null;
                    }

                    if(input != null) {
                        try {
                            input.close();
                        } catch (IOException var22) {
                            var22.printStackTrace();
                        }

                        input = null;
                    }

                }

                return content;
            }
        }
    }

    public static String saveString2File(String data, String filePath) {
        return saveString2File(data, filePath, (String)null);
    }

    public static String saveString2File(String data, String filePath, String encoding) {
        if(TextUtils.isEmpty(filePath)) {
            return null;
        } else {
            FileOutputStream out = null;

            try {
                out = openFileOutputStream(filePath);
                if(TextUtils.isEmpty(encoding)) {
                    out.write(data.getBytes());
                } else {
                    out.write(data.getBytes(encoding));
                }
            } catch (Exception var13) {
                var13.printStackTrace();
            } finally {
                if(out != null) {
                    try {
                        out.close();
                    } catch (IOException var12) {
                        var12.printStackTrace();
                    }

                    out = null;
                }

            }

            return filePath;
        }
    }

    public static FileOutputStream openFileOutputStream(String filePath) throws IOException {
        File file = new File(filePath);
        if(file.exists()) {
            if(file.isDirectory()) {
                throw new IOException("File \'" + file + "\' exists but is a directory");
            }

            if(!file.canWrite()) {
                throw new IOException("File \'" + file + "\' cannot be written to");
            }
        } else {
            File parent = file.getParentFile();
            if(parent != null && !parent.exists() && !parent.mkdirs()) {
                throw new IOException("File \'" + file + "\' could not be created");
            }

            file.createNewFile();
        }

        return new FileOutputStream(file);
    }
}
