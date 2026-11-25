package com.ayoub.roadrush;

import android.Manifest;
import android.database.Cursor;
import android.os.Build;
import android.provider.MediaStore;

import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.PermissionState;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;

@CapacitorPlugin(
    name = "MusicLibrary",
    permissions = {
        @Permission(alias = "audio", strings = { Manifest.permission.READ_EXTERNAL_STORAGE }),
        @Permission(alias = "audio33", strings = { "android.permission.READ_MEDIA_AUDIO" })
    }
)
public class MusicLibraryPlugin extends Plugin {

    private String alias() {
        return Build.VERSION.SDK_INT >= 33 ? "audio33" : "audio";
    }

    @PluginMethod
    public void list(PluginCall call) {
        if (getPermissionState(alias()) != PermissionState.GRANTED) {
            requestPermissionForAlias(alias(), call, "permCallback");
        } else {
            query(call);
        }
    }

    @PermissionCallback
    private void permCallback(PluginCall call) {
        if (getPermissionState(alias()) == PermissionState.GRANTED) {
            query(call);
        } else {
            call.reject("Permission denied");
        }
    }

    private void query(PluginCall call) {
        JSArray tracks = new JSArray();
        String[] projection = {
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DATA
        };
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0 AND "
            + MediaStore.Audio.Media.DURATION + " > 60000";
        Cursor cursor = null;
        try {
            cursor = getContext().getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection, null, null);
            int count = 0;
            while (cursor != null && cursor.moveToNext() && count < 500) {
                JSObject track = new JSObject();
                track.put("title", cursor.getString(0));
                track.put("artist", cursor.getString(1));
                track.put("path", cursor.getString(2));
                tracks.put(track);
                count++;
            }
        } catch (Exception e) {
            call.reject("Could not read music: " + e.getMessage());
            return;
        } finally {
            if (cursor != null) cursor.close();
        }
        JSObject result = new JSObject();
        result.put("tracks", tracks);
        call.resolve(result);
    }
}
