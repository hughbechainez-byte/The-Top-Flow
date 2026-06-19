package com.davehq.thetopflow;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.content.FileProvider;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int REQ_SONG = 10;
    private static final int REQ_AUDIO = 11;
    private static final int REQ_NOTIFY = 12;
    private static final String CHANNEL_UPDATES = "updates";

    private final ArrayList<Note> notes = new ArrayList<>();
    private LinearLayout root;
    private LinearLayout noteList;
    private LinearLayout editor;
    private EditText titleInput;
    private EditText bodyInput;
    private Spinner fontSpinner;
    private TextView colorPreview;
    private TextView countdownView;
    private Note current;
    private int selectedColorTarget = 0;
    private int draftColor = Color.WHITE;
    private MediaPlayer player;
    private MediaRecorder recorder;
    private File activeRecording;
    private int countdownSeconds = 3;
    private SharedPreferences prefs;
    private boolean suppressSave = false;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable updatePoll = new Runnable() {
        @Override
        public void run() {
            checkForUpdates(false);
            handler.postDelayed(this, 6L * 60L * 60L * 1000L);
        }
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("settings", MODE_PRIVATE);
        countdownSeconds = prefs.getInt("countdown", 3);
        createChannels();
        requestRuntimePermissions();
        loadNotes();
        if (notes.isEmpty()) {
            notes.add(Note.create("New Hook"));
            saveNotes();
        }
        buildUi();
        openNote(notes.get(0));
        checkForUpdates(false);
        handler.postDelayed(updatePoll, 6L * 60L * 60L * 1000L);
    }

    @Override
    protected void onResume() {
        super.onResume();
        long last = prefs.getLong("lastUpdateCheck", 0);
        if (System.currentTimeMillis() - last > 6L * 60L * 60L * 1000L) {
            checkForUpdates(false);
        }
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(updatePoll);
        stopRecording(false);
        if (player != null) player.release();
        super.onDestroy();
    }

    private void buildUi() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(8, 18, 38));
        setContentView(root);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(24, 20, 24, 14);
        TextView brand = new TextView(this);
        brand.setText("The Top Flow");
        brand.setTextColor(Color.WHITE);
        brand.setTextSize(24);
        brand.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        header.addView(brand, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        Button menu = button("Menu");
        menu.setOnClickListener(v -> showMainMenu());
        header.addView(menu);
        root.addView(header);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(content, new LinearLayout.LayoutParams(-1, 0, 1));

        ScrollView listScroll = new ScrollView(this);
        noteList = new LinearLayout(this);
        noteList.setOrientation(LinearLayout.VERTICAL);
        noteList.setPadding(12, 0, 8, 12);
        listScroll.addView(noteList);
        content.addView(listScroll, new LinearLayout.LayoutParams(dp(132), -1));

        ScrollView editorScroll = new ScrollView(this);
        editor = new LinearLayout(this);
        editor.setOrientation(LinearLayout.VERTICAL);
        editor.setPadding(14, 0, 14, 18);
        editorScroll.addView(editor);
        content.addView(editorScroll, new LinearLayout.LayoutParams(0, -1, 1));

        titleInput = new EditText(this);
        titleInput.setSingleLine(true);
        titleInput.setTextSize(22);
        editor.addView(titleInput, new LinearLayout.LayoutParams(-1, -2));

        bodyInput = new EditText(this);
        bodyInput.setGravity(Gravity.TOP | Gravity.START);
        bodyInput.setMinLines(12);
        bodyInput.setTextSize(18);
        editor.addView(bodyInput, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.VERTICAL);
        editor.addView(actions);
        Button attach = button("Attach Song");
        attach.setOnClickListener(v -> pickSong());
        Button play = button("Play / Pause Song");
        play.setOnClickListener(v -> toggleSong());
        Button record = button("Record Voice Note");
        record.setOnClickListener(v -> startCountdownThenRecord());
        Button stop = button("Stop Recording");
        stop.setOnClickListener(v -> stopRecording(true));
        actions.addView(attach);
        actions.addView(play);
        actions.addView(record);
        actions.addView(stop);
        attach.setVisibility(BuildConfig.VERSION_CODE >= 2 ? View.VISIBLE : View.GONE);
        play.setVisibility(BuildConfig.VERSION_CODE >= 2 ? View.VISIBLE : View.GONE);
        record.setVisibility(BuildConfig.VERSION_CODE >= 3 ? View.VISIBLE : View.GONE);
        stop.setVisibility(BuildConfig.VERSION_CODE >= 3 ? View.VISIBLE : View.GONE);

        countdownView = new TextView(this);
        countdownView.setTextColor(Color.WHITE);
        countdownView.setTextSize(28);
        countdownView.setGravity(Gravity.CENTER);
        editor.addView(countdownView, new LinearLayout.LayoutParams(-1, -2));
        countdownView.setVisibility(BuildConfig.VERSION_CODE >= 3 ? View.VISIBLE : View.GONE);

        editor.addView(label("Style"));
        fontSpinner = new Spinner(this);
        String[] fonts = {"sans", "serif", "monospace", "casual", "cursive"};
        fontSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, fonts));
        editor.addView(fontSpinner);

        LinearLayout colorButtons = new LinearLayout(this);
        colorButtons.setOrientation(LinearLayout.VERTICAL);
        String[] names = {"Note Color", "Text Color", "Accent Color"};
        for (int i = 0; i < names.length; i++) {
            final int target = i;
            Button b = button(names[i]);
            b.setOnClickListener(v -> {
                selectedColorTarget = target;
                int c = target == 0 ? current.noteColor : target == 1 ? current.textColor : current.accentColor;
                showColorEditor(c);
            });
            colorButtons.addView(b);
        }
        editor.addView(colorButtons);
        colorPreview = label("");
        colorPreview.setGravity(Gravity.CENTER);
        colorPreview.setMinHeight(dp(60));
        editor.addView(colorPreview, new LinearLayout.LayoutParams(-1, dp(70)));

        titleInput.addTextChangedListener(simpleWatcher(() -> {
            if (!suppressSave && current != null) {
                current.title = titleInput.getText().toString();
                saveAndRenderList();
            }
        }));
        bodyInput.addTextChangedListener(simpleWatcher(() -> {
            if (!suppressSave && current != null) {
                current.body = bodyInput.getText().toString();
                saveNotes();
            }
        }));
        fontSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                if (current != null) {
                    current.font = String.valueOf(parent.getItemAtPosition(pos));
                    applyStyle();
                    if (!suppressSave) saveNotes();
                }
            }
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void renderNoteList() {
        noteList.removeAllViews();
        Button add = button("+ Note");
        add.setOnClickListener(v -> {
            Note n = Note.create("Untitled");
            notes.add(0, n);
            saveNotes();
            renderNoteList();
            openNote(n);
        });
        noteList.addView(add);
        for (Note note : notes) {
            Button b = button(note.title == null || note.title.isEmpty() ? "Untitled" : note.title);
            b.setTextColor(note.textColor);
            b.setBackgroundColor(note.noteColor);
            b.setOnClickListener(v -> openNote(note));
            noteList.addView(b);
        }
    }

    private void openNote(Note note) {
        current = note;
        suppressSave = true;
        titleInput.setText(note.title);
        bodyInput.setText(note.body);
        String[] fonts = {"sans", "serif", "monospace", "casual", "cursive"};
        for (int i = 0; i < fonts.length; i++) {
            if (fonts[i].equals(note.font)) fontSpinner.setSelection(i);
        }
        suppressSave = false;
        renderNoteList();
        applyStyle();
    }

    private void applyStyle() {
        if (current == null) return;
        titleInput.setBackgroundColor(current.noteColor);
        bodyInput.setBackgroundColor(current.noteColor);
        titleInput.setTextColor(current.textColor);
        bodyInput.setTextColor(current.textColor);
        titleInput.setHintTextColor(current.accentColor);
        bodyInput.setHintTextColor(current.accentColor);
        titleInput.setTypeface(android.graphics.Typeface.create(current.font, android.graphics.Typeface.BOLD));
        bodyInput.setTypeface(android.graphics.Typeface.create(current.font, android.graphics.Typeface.NORMAL));
        draftColor = selectedColorTarget == 0 ? current.noteColor : selectedColorTarget == 1 ? current.textColor : current.accentColor;
        updatePreview(draftColor);
    }

    private void showColorEditor(int startColor) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(24, 12, 24, 0);
        TextView preview = new TextView(this);
        preview.setText("Preview");
        preview.setGravity(Gravity.CENTER);
        preview.setMinHeight(dp(70));
        box.addView(preview);
        int[] rgb = {Color.red(startColor), Color.green(startColor), Color.blue(startColor)};
        String[] labels = {"Red", "Green", "Blue"};
        for (int i = 0; i < 3; i++) {
            final int channel = i;
            TextView value = label(labels[i] + ": " + rgb[i]);
            SeekBar seek = new SeekBar(this);
            seek.setMax(255);
            seek.setProgress(rgb[i]);
            seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                    rgb[channel] = progress;
                    value.setText(labels[channel] + ": " + progress);
                    int color = Color.rgb(rgb[0], rgb[1], rgb[2]);
                    preview.setBackgroundColor(color);
                    preview.setText(String.format(Locale.US, "#%02X%02X%02X", rgb[0], rgb[1], rgb[2]));
                }
                public void onStartTrackingTouch(SeekBar bar) {}
                public void onStopTrackingTouch(SeekBar bar) {}
            });
            box.addView(value);
            box.addView(seek);
        }
        int color = Color.rgb(rgb[0], rgb[1], rgb[2]);
        preview.setBackgroundColor(color);
        preview.setText(String.format(Locale.US, "#%02X%02X%02X", rgb[0], rgb[1], rgb[2]));
        new AlertDialog.Builder(this)
                .setTitle("RGB Color")
                .setView(box)
                .setPositiveButton("Apply", (d, w) -> {
                    int picked = Color.rgb(rgb[0], rgb[1], rgb[2]);
                    if (selectedColorTarget == 0) current.noteColor = picked;
                    if (selectedColorTarget == 1) current.textColor = picked;
                    if (selectedColorTarget == 2) current.accentColor = picked;
                    saveNotes();
                    applyStyle();
                    renderNoteList();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updatePreview(int color) {
        if (colorPreview == null) return;
        colorPreview.setBackgroundColor(color);
        colorPreview.setText(String.format(Locale.US, "#%06X", 0xFFFFFF & color));
        colorPreview.setTextColor(readableText(color));
    }

    private int readableText(int color) {
        double y = (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color));
        return y > 150 ? Color.BLACK : Color.WHITE;
    }

    private void pickSong() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("audio/*");
        startActivityForResult(i, REQ_SONG);
    }

    private void toggleSong() {
        if (current == null || current.songUri == null || current.songUri.isEmpty()) {
            Toast.makeText(this, "Attach a song first.", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            if (player != null && player.isPlaying()) {
                player.pause();
                return;
            }
            if (player == null) {
                player = MediaPlayer.create(this, Uri.parse(current.songUri));
                player.setOnCompletionListener(mp -> {
                    mp.release();
                    player = null;
                });
            }
            player.start();
        } catch (Exception e) {
            Toast.makeText(this, "Could not play song.", Toast.LENGTH_SHORT).show();
        }
    }

    private void startCountdownThenRecord() {
        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_AUDIO);
            return;
        }
        hideKeyboard();
        runCountdown(countdownSeconds);
    }

    private void runCountdown(int remaining) {
        if (remaining <= 0) {
            countdownView.setText("");
            beginRecording();
            return;
        }
        countdownView.setText(String.valueOf(remaining));
        countdownView.postDelayed(() -> runCountdown(remaining - 1), 1000);
    }

    private void beginRecording() {
        try {
            toggleSong();
            File dir = new File(getFilesDir(), "recordings");
            if (!dir.exists()) dir.mkdirs();
            String stamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date());
            activeRecording = new File(dir, "top-flow-" + stamp + ".m4a");
            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            recorder.setOutputFile(activeRecording.getAbsolutePath());
            recorder.prepare();
            recorder.start();
            Toast.makeText(this, "Recording.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            stopRecording(false);
            Toast.makeText(this, "Could not start recording.", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopRecording(boolean keep) {
        try {
            if (recorder != null) {
                recorder.stop();
                recorder.release();
            }
        } catch (Exception ignored) {
        }
        recorder = null;
        if (keep && activeRecording != null && current != null) {
            exportRecordingToPhoneFiles(activeRecording);
            current.recordings.add(activeRecording.getAbsolutePath());
            insertRecordingMarker(activeRecording.getName());
            saveNotes();
            Toast.makeText(this, "Recording saved.", Toast.LENGTH_SHORT).show();
        }
        activeRecording = null;
    }

    private void exportRecordingToPhoneFiles(File source) {
        if (Build.VERSION.SDK_INT < 29) return;
        try {
            android.content.ContentValues values = new android.content.ContentValues();
            values.put(MediaStore.Audio.Media.DISPLAY_NAME, source.getName());
            values.put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4");
            values.put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/The Top Flow");
            Uri uri = getContentResolver().insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values);
            if (uri == null) return;
            try (FileInputStream in = new FileInputStream(source);
                 OutputStream out = getContentResolver().openOutputStream(uri)) {
                byte[] buf = new byte[8192];
                int read;
                while (out != null && (read = in.read(buf)) > -1) out.write(buf, 0, read);
            }
        } catch (Exception ignored) {
        }
    }

    private void insertRecordingMarker(String name) {
        int cursor = Math.max(0, bodyInput.getSelectionStart());
        Editable text = bodyInput.getText();
        int lineEnd = cursor;
        while (lineEnd < text.length() && text.charAt(lineEnd) != '\n') lineEnd++;
        String marker = "\n[Voice note: " + name + "]\n";
        text.insert(lineEnd, marker);
        current.body = text.toString();
    }

    private void showMainMenu() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(32, 8, 32, 0);
        TextView version = label("Version " + BuildConfig.VERSION_NAME);
        box.addView(version);
        TextView count = label("Recording countdown seconds");
        EditText countdown = new EditText(this);
        countdown.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        countdown.setText(String.valueOf(countdownSeconds));
        box.addView(count);
        box.addView(countdown);
        Button updates = button("Check for updates");
        updates.setOnClickListener(v -> checkForUpdates(true));
        box.addView(updates);
        new AlertDialog.Builder(this)
                .setTitle("Main Menu")
                .setView(box)
                .setPositiveButton("Done", (d, w) -> {
                    try {
                        countdownSeconds = Math.max(0, Integer.parseInt(countdown.getText().toString()));
                        prefs.edit().putInt("countdown", countdownSeconds).apply();
                    } catch (Exception ignored) {}
                })
                .show();
    }

    private void checkForUpdates(boolean manual) {
        prefs.edit().putLong("lastUpdateCheck", System.currentTimeMillis()).apply();
        new Thread(() -> {
            try {
                String json = readUrl(BuildConfig.UPDATE_MANIFEST_URL);
                JSONObject manifest = new JSONObject(json);
                int versionCode = manifest.getInt("versionCode");
                String versionName = manifest.optString("versionName", String.valueOf(versionCode));
                String apkUrl = manifest.getString("apkUrl");
                if (versionCode > BuildConfig.VERSION_CODE) {
                    runOnUiThread(() -> showUpdateFound(versionName, apkUrl));
                    notifyUpdate(versionName);
                } else if (manual) {
                    runOnUiThread(() -> Toast.makeText(this, "No update found.", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                if (manual) runOnUiThread(() -> Toast.makeText(this, "Update check failed.", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private String readUrl(String urlText) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(urlText).openConnection();
        c.setConnectTimeout(12000);
        c.setReadTimeout(12000);
        try (BufferedInputStream in = new BufferedInputStream(c.getInputStream())) {
            byte[] data = new byte[0];
            byte[] buf = new byte[8192];
            int read;
            while ((read = in.read(buf)) > -1) {
                byte[] next = new byte[data.length + read];
                System.arraycopy(data, 0, next, 0, data.length);
                System.arraycopy(buf, 0, next, data.length, read);
                data = next;
            }
            return new String(data, StandardCharsets.UTF_8);
        } finally {
            c.disconnect();
        }
    }

    private void showUpdateFound(String version, String apkUrl) {
        new AlertDialog.Builder(this)
                .setTitle("Update available")
                .setMessage("Version " + version + " is ready.")
                .setPositiveButton("Install Update", (d, w) -> downloadAndInstall(apkUrl))
                .setNegativeButton("Later", null)
                .show();
    }

    private void downloadAndInstall(String apkUrl) {
        new Thread(() -> {
            try {
                File out = new File(getCacheDir(), "the-top-flow-update.apk");
                HttpURLConnection c = (HttpURLConnection) new URL(apkUrl).openConnection();
                if (apkUrl.contains("temp.sh/")) c.setRequestMethod("POST");
                c.setConnectTimeout(12000);
                c.setReadTimeout(30000);
                try (BufferedInputStream in = new BufferedInputStream(c.getInputStream());
                     FileOutputStream file = new FileOutputStream(out)) {
                    byte[] buf = new byte[8192];
                    int read;
                    while ((read = in.read(buf)) > -1) file.write(buf, 0, read);
                } finally {
                    c.disconnect();
                }
                Uri uri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".files", out);
                Intent install = new Intent(Intent.ACTION_VIEW);
                install.setDataAndType(uri, "application/vnd.android.package-archive");
                install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(install);
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Update download failed.", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void notifyUpdate(String version) {
        Intent i = new Intent(this, MainActivity.class);
        PendingIntent p = PendingIntent.getActivity(this, 1, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder b = new NotificationCompat.Builder(this, CHANNEL_UPDATES)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle("The Top Flow update")
                .setContentText("Version " + version + " is available.")
                .setContentIntent(p)
                .setAutoCancel(true);
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT < 33 || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            nm.notify(300, b.build());
        }
    }

    private void createChannels() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            nm.createNotificationChannel(new NotificationChannel(CHANNEL_UPDATES, "Updates", NotificationManager.IMPORTANCE_DEFAULT));
        }
    }

    private void requestRuntimePermissions() {
        ArrayList<String> perms = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            perms.add(Manifest.permission.RECORD_AUDIO);
        }
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        if (!perms.isEmpty()) requestPermissions(perms.toArray(new String[0]), REQ_NOTIFY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_SONG && resultCode == RESULT_OK && data != null && current != null) {
            Uri uri = data.getData();
            try {
                final int flags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                getContentResolver().takePersistableUriPermission(uri, flags);
            } catch (Exception ignored) {}
            current.songUri = uri.toString();
            saveNotes();
            Toast.makeText(this, "Song attached.", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadNotes() {
        notes.clear();
        try {
            File f = new File(getFilesDir(), "notes.json");
            if (!f.exists()) return;
            JSONArray arr = new JSONArray(new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8));
            for (int i = 0; i < arr.length(); i++) notes.add(Note.fromJson(arr.getJSONObject(i)));
        } catch (Exception ignored) {
        }
    }

    private void saveNotes() {
        try {
            JSONArray arr = new JSONArray();
            for (Note n : notes) arr.put(n.toJson());
            Files.write(new File(getFilesDir(), "notes.json").toPath(), arr.toString(2).getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) {
        }
    }

    private void saveAndRenderList() {
        saveNotes();
        renderNoteList();
    }

    private TextWatcher simpleWatcher(Runnable changed) {
        return new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) { changed.run(); }
            public void afterTextChanged(Editable s) {}
        };
    }

    private TextView label(String text) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextColor(Color.WHITE);
        v.setTextSize(16);
        v.setPadding(0, 10, 0, 8);
        return v;
    }

    private Button button(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        return b;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void hideKeyboard() {
        try {
            ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(bodyInput.getWindowToken(), 0);
        } catch (Exception ignored) {}
    }

    static class Note {
        String title;
        String body;
        String font = "sans";
        int noteColor = Color.WHITE;
        int textColor = Color.rgb(20, 20, 20);
        int accentColor = Color.rgb(40, 214, 163);
        String songUri = "";
        ArrayList<String> recordings = new ArrayList<>();

        static Note create(String title) {
            Note n = new Note();
            n.title = title;
            n.body = "";
            return n;
        }

        JSONObject toJson() throws Exception {
            JSONObject o = new JSONObject();
            o.put("title", title);
            o.put("body", body);
            o.put("font", font);
            o.put("noteColor", noteColor);
            o.put("textColor", textColor);
            o.put("accentColor", accentColor);
            o.put("songUri", songUri);
            JSONArray r = new JSONArray();
            for (String path : recordings) r.put(path);
            o.put("recordings", r);
            return o;
        }

        static Note fromJson(JSONObject o) {
            Note n = create(o.optString("title", "Untitled"));
            n.body = o.optString("body", "");
            n.font = o.optString("font", "sans");
            n.noteColor = o.optInt("noteColor", Color.WHITE);
            n.textColor = o.optInt("textColor", Color.rgb(20, 20, 20));
            n.accentColor = o.optInt("accentColor", Color.rgb(40, 214, 163));
            n.songUri = o.optString("songUri", "");
            JSONArray r = o.optJSONArray("recordings");
            if (r != null) {
                for (int i = 0; i < r.length(); i++) n.recordings.add(r.optString(i));
            }
            return n;
        }
    }
}
