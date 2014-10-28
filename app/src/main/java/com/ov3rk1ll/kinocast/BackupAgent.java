package com.ov3rk1ll.kinocast;

import android.app.backup.BackupAgentHelper;
import android.app.backup.FileBackupHelper;

public class BackupAgent extends BackupAgentHelper {
    // The name of the SharedPreferences file
    static final String BOOKMARKS_FILENAME = "bookmarks.dat";

    // A key to uniquely identify the set of backup data
    static final String FILES_BACKUP_KEY = "myfiles";

    // Allocate a helper and add it to the backup agent
    @Override
    public void onCreate() {
        FileBackupHelper helper = new FileBackupHelper(this, BOOKMARKS_FILENAME);
        addHelper(FILES_BACKUP_KEY, helper);
    }

}