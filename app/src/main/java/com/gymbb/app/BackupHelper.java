package com.gymbb.app;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class BackupHelper {

    private static final String BACKUP_DIR_NAME = "GymBB";
    private static final String BACKUP_FILE_NAME = "gymbb_backup.db";
    private static final String DB_NAME = "gymbb.db";

    public static File getBackupFile() {
        File dir = new File(Environment.getExternalStorageDirectory(), BACKUP_DIR_NAME);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return new File(dir, BACKUP_FILE_NAME);
    }

    public static boolean exportDatabase(Context context) {
        try {
            File dbFile = context.getDatabasePath(DB_NAME);
            if (!dbFile.exists()) {
                return false;
            }

            File backupFile = getBackupFile();
            copyFile(dbFile, backupFile);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean importDatabase(Context context) {
        try {
            File backupFile = getBackupFile();
            if (!backupFile.exists() || backupFile.length() == 0) {
                return false;
            }

            File dbFile = context.getDatabasePath(DB_NAME);
            if (dbFile.getParentFile() != null && !dbFile.getParentFile().exists()) {
                dbFile.getParentFile().mkdirs();
            }

            // Remove any stale journal/WAL files
            new File(dbFile.getPath() + "-journal").delete();
            new File(dbFile.getPath() + "-wal").delete();
            new File(dbFile.getPath() + "-shm").delete();

            copyFile(backupFile, dbFile);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static String getBackupInfo() {
        try {
            File backupFile = getBackupFile();
            if (!backupFile.exists() || backupFile.length() == 0) {
                return "no backup found";
            }

            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM, HH:mm", Locale.US);
            String dateStr = sdf.format(new Date(backupFile.lastModified()));
            long kb = Math.max(1, backupFile.length() / 1024);
            return dateStr + " (" + kb + " KB)";
        } catch (Exception e) {
            return "unknown";
        }
    }

    private static void copyFile(File src, File dst) throws Exception {
        InputStream in = null;
        OutputStream out = null;
        try {
            in = new FileInputStream(src);
            out = new FileOutputStream(dst);
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            out.flush();
        } finally {
            if (in != null) try { in.close(); } catch (Exception ignored) {}
            if (out != null) try { out.close(); } catch (Exception ignored) {}
        }
    }
}
