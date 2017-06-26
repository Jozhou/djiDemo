package com.dji.demo;

import android.content.ContentValues;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LogcatUtils {
    private static File file = null;
    private static String createLogFileTime = null;
    private static final String SDF_YYYYMMDD = "yyyy-MM-dd";
    private static final String SDF_YYYYMMDDHHMMSS = "MM-dd HH:mm:ss,SSS";
    private static ExecutorService executorAppendLog = Executors.newSingleThreadExecutor();

    public LogcatUtils() {
    }

    private static void init() {
        if(Environment.getExternalStorageState().equals("mounted")) {
            try {
                createLogFileTime = (new SimpleDateFormat("yyyy-MM-dd")).format(new Date());
                file = new File(FileUtils.getCachePath() + FileUtils.CACHELOGPATH + "/dji-" + createLogFileTime + ".log");
                if(!file.getParentFile().exists()) {
                    file.getParentFile().mkdirs();
                }

                if(!file.exists()) {
                    file.createNewFile();
                }
            } catch (IOException var1) {
                var1.printStackTrace();
            }
        } else {
            Log.w("LogUtils", "can\'t find sdcard for log store.");
        }

    }

    private static boolean checkStoreLog() {
        return file != null && file.exists() && !TextUtils.isEmpty(createLogFileTime) && createLogFileTime.equals((new SimpleDateFormat("yyyy-MM-dd")).format(new Date()));
    }

    private static void appendLog(final File f, final String c, final int l) {
        if(LogcatUtils.file != null && LogcatUtils.file.exists()) {
            executorAppendLog.execute(new Runnable() {
                public void run() {
                    Class var1 = LogcatUtils.class;
                    synchronized(LogcatUtils.class) {
                        BufferedWriter out = null;

                        try {
                            out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f, true), "UTF-8"), 8192);
                            StringBuffer e = new StringBuffer();
                            e.append((new SimpleDateFormat("MM-dd HH:mm:ss,SSS")).format(new Date()));
                            e.append("\t ");
                            e.append(l == 0?"debug":(l == 1?"info":(l == 2?"warn":(l == 100?"crash":"error"))));
                            e.append("\t");
                            e.append(c);
                            e.append("\r\n");
                            out.write(e.toString());
                        } catch (Exception var13) {
                            var13.printStackTrace();
                        } finally {
                            try {
                                if(out != null) {
                                    out.close();
                                    out = null;
                                }
                            } catch (IOException var12) {
                                var12.printStackTrace();
                            }

                        }

                    }
                }
            });
        }
    }

    public static void d(String tag, String msg) {
        if(getIsDebug()) {
            Log.d(tag, "thread Id: " + Thread.currentThread().getId() + "  " + msg);
        }

        if(getIsStoreLog()) {
            if(!checkStoreLog()) {
                init();
            }

            appendLog(file, tag + "\t" + "thread Id: " + Thread.currentThread().getId() + "  " + msg, 0);
        }

    }

    public static void i(String tag, String msg) {
        if(getIsDebug()) {
            Log.i(tag, "thread Id: " + Thread.currentThread().getId() + "  " + msg);
        }

        if(getIsStoreLog()) {
            if(!checkStoreLog()) {
                init();
            }

            appendLog(file, tag + "\t" + "thread Id: " + Thread.currentThread().getId() + "  " + msg, 1);
        }

    }

    public static void w(String tag, String msg) {
        if(getIsDebug()) {
            Log.w(tag, "thread Id: " + Thread.currentThread().getId() + "  " + msg);
        }

        if(getIsStoreLog()) {
            if(!checkStoreLog()) {
                init();
            }

            appendLog(file, tag + "\t" + "thread Id: " + Thread.currentThread().getId() + "  " + msg, 2);
        }

    }

    public static void e(String tag, String msg) {
        if(getIsDebug()) {
            Log.e(tag, "thread Id: " + Thread.currentThread().getId() + "  " + msg);
        }

        if(getIsStoreLog()) {
            if(!checkStoreLog()) {
                init();
            }

            appendLog(file, tag + "\t" + "thread Id: " + Thread.currentThread().getId() + "  " + msg, 0);
        }

    }

    public static void e(String tag, Throwable e) {
        String msg = " StackTrace:" + getErrorInfo(e);
        if(getIsDebug()) {
            Log.e(tag, "thread Id: " + Thread.currentThread().getId() + "  " + msg);
        }

        if(getIsStoreLog()) {
            if(!checkStoreLog()) {
                init();
            }

            appendLog(file, tag + "\t" + "thread Id: " + Thread.currentThread().getId() + "  " + msg, 0);
        }

    }

    public static void e(String tag, String msg, Throwable e) {
        msg = msg + " StackTrace:" + getErrorInfo(e);
        if(getIsDebug()) {
            Log.e(tag, "thread Id: " + Thread.currentThread().getId() + "  " + msg);
        }

        if(getIsStoreLog()) {
            if(!checkStoreLog()) {
                init();
            }

            appendLog(file, tag + "\t" + "thread Id: " + Thread.currentThread().getId() + "  " + msg, 0);
        }

    }

    public static String getErrorInfo(Throwable throwable) {
        try {
            if(throwable != null) {
                StringWriter writer = new StringWriter();
                PrintWriter pw = new PrintWriter(writer);
                throwable.printStackTrace(pw);
                pw.close();
                return writer.toString();
            }
        } catch (Exception var3) {
            ;
        }

        return "";
    }

    public static String crash(String msg) {
        if(getIsDebug()) {
            Log.e("dji", "crash occured : " + Thread.currentThread().getId() + "  " + msg);
        }

        String path = FileUtils.getCachePath() + FileUtils.CACHECRASHPATH + "/dji-" + (new SimpleDateFormat("yyyy-MM-dd")).format(new Date()) + ".log";

        try {
            File e = new File(path);
            if(!e.getParentFile().exists()) {
                e.getParentFile().mkdirs();
            }

            if(!e.exists()) {
                e.createNewFile();
            }

            RandomAccessFile newFile = new RandomAccessFile(e, "rw");
            if(e.length() > 1048576L) {
                newFile.seek(0L);
            } else {
                newFile.seek(e.length());
            }

            newFile.write((msg + "\n").getBytes());
            newFile.close();
        } catch (IOException var4) {
            var4.printStackTrace();
        }

        return path;
    }

    public static void sqllog(String tablename, String action, String exec, ContentValues initialValues, String whereClause, String[] whereArgs) {
        StringBuffer sbmsg = new StringBuffer();
        boolean isdebug = getIsDebug();
        boolean isstorelog = getIsStoreLog();
        if(isdebug || isstorelog) {
            sbmsg.append("table:").append(tablename).append("; action:").append(action).append("; exec:").append(exec).append("; values:");
            if(initialValues != null && initialValues.size() > 0) {
                Set path = initialValues.valueSet();
                Iterator e = path.iterator();
                boolean newFile = false;

                while(e.hasNext()) {
                    if(newFile) {
                        sbmsg.append(",");
                    }

                    newFile = true;
                    Entry entry = (Entry)e.next();
                    sbmsg.append("[").append((String)entry.getKey()).append("] ");
                    sbmsg.append(entry.getValue());
                }
            }

            sbmsg.append("; where:[").append(whereClause).append("] ");
            if(whereArgs != null && whereArgs.length > 0) {
                boolean var14 = false;

                for(int var16 = 0; var16 < whereArgs.length; ++var16) {
                    if(var14) {
                        sbmsg.append(",");
                    }

                    var14 = true;
                    sbmsg.append(whereArgs[var16]);
                }
            }
        }

        if(isdebug) {
            Log.i("dji", Thread.currentThread().getId() + "  " + sbmsg.toString());
        }

        if(isstorelog) {
            String var15 = FileUtils.getCachePath() + FileUtils.CACHEDBPATH + "/dji-" + (new SimpleDateFormat("yyyy-MM-dd")).format(new Date()) + ".log";

            try {
                File var17 = new File(var15);
                if(!var17.getParentFile().exists()) {
                    var17.getParentFile().mkdirs();
                }

                if(!var17.exists()) {
                    var17.createNewFile();
                }

                RandomAccessFile var18 = new RandomAccessFile(var17, "rw");
                if(var17.length() > 1048576L) {
                    var18.seek(0L);
                } else {
                    var18.seek(var17.length());
                }

                var18.write((sbmsg.toString() + "\n").getBytes());
                var18.close();
            } catch (IOException var13) {
                var13.printStackTrace();
            }
        }

    }

    private static boolean getIsDebug() {
        return true;
    }

    private static boolean getIsStoreLog() {
        return true;
    }
}
