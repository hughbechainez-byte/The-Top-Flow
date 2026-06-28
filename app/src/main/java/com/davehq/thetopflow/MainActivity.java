package com.davehq.thetopflow;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.Path;
import android.graphics.Paint;
import android.graphics.RectF;
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
import android.text.Layout;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.PopupWindow;

import androidx.core.app.NotificationCompat;
import androidx.core.graphics.Insets;
import androidx.core.content.FileProvider;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class MainActivity extends Activity {
    private static final String TAG = "TopFlow";
    private static final int REQ_SONG = 10;
    private static final int REQ_AUDIO = 11;
    private static final int REQ_NOTIFY = 12;
    private static final String CHANNEL_UPDATES = "updates";
    private static final int C_BG = Color.rgb(4, 7, 15);
    private static final int C_SURFACE = Color.argb(238, 10, 16, 29);
    private static final int C_SURFACE_SOFT = Color.argb(214, 16, 25, 41);
    private static final int C_SURFACE_HIGH = Color.argb(246, 18, 26, 42);
    private static final int C_TEXT = Color.WHITE;
    private static final int C_TEXT_MUTED = Color.rgb(166, 178, 197);
    private static final int C_CYAN = Color.rgb(120, 230, 255);
    private static final int C_GREEN = Color.rgb(40, 214, 163);
    private static final int C_MAGENTA = Color.rgb(225, 86, 255);
    private static final int C_RED = Color.rgb(240, 75, 92);
    private static final int C_GOLD = Color.rgb(238, 194, 105);
    private static final int DEFAULT_NOTE_COLOR = Color.rgb(14, 205, 190);
    private static final int DEFAULT_NOTE_TEXT_COLOR = Color.rgb(8, 12, 14);
    private static final int DEFAULT_NOTE_ACCENT_COLOR = Color.rgb(132, 255, 238);
    private static final int DEFAULT_RECORDING_TAG_COLOR = Color.rgb(24, 28, 36);
    private static final int MAX_CONTENT_WIDTH_DP = 860;
    private static final int RADIUS_DP = 28;
    private static final int SPACE_DP = 14;
    private static final int BUTTON_PRESS_MS = 90;
    private static final int PANEL_TRANSITION_MS = 150;
    private static final int SUGGESTION_DEBOUNCE_MS = 220;
    private static final int FAST_RHYME_LIMIT = 6;
    private static final int FAST_RHYME_CANDIDATE_LIMIT = 360;
    private static final int RHYME_CACHE_LIMIT = 96;
    private static final String UPDATE_MANIFEST_JSONBLOB = "https://jsonblob.com/api/jsonBlob/019f086a-01bf-77d9-8da2-a2892669c9d5";
    private static final String PREF_RHYME_STRICTNESS = "rhymeStrictness";
    private static final String PREF_MAX_SUGGESTIONS = "rhymeMaxSuggestions";
    private static final String PREF_SHOW_RHYME_ROW = "showRhymeRow";
    private static final String PREF_EXACT_ONLY = "rhymeExactOnly";
    private static final String PREF_INCLUDE_SLANG = "rhymeIncludeSlang";
    private static final String PREF_REMOVED_RHYMES = "removedRhymes";
    private static final int RHYME_BUCKET_EXACT = 0;
    private static final int RHYME_BUCKET_NEAR = 1;
    private static final int RHYME_BUCKET_SLANT = 2;
    private static final int RHYME_BUCKET_PHRASE = 3;
    private static final int RHYME_BUCKET_FALLBACK = 4;
    private static final String[] COMMON_WORDS = {
            "a", "all", "and", "around", "back", "beat", "before", "believe", "big", "blue",
            "born", "break", "bright", "call", "change", "clean", "cold", "come", "day", "dream",
            "drop", "flow", "fly", "fire", "feel", "free", "gone", "grind", "heart", "hollow",
            "home", "hope", "hold", "hype", "line", "light", "love", "late", "move", "night",
            "open", "pain", "play", "push", "ride", "rain", "say", "sound", "shine", "slow",
            "soul", "stay", "step", "story", "time", "turn", "truth", "trail", "voice", "wave",
            "want", "way", "whole", "win", "word", "young", "zone"
    };

    private final ArrayList<Note> notes = new ArrayList<>();
    private final Map<String, ArrayList<String>> cmuPhones = new HashMap<>();
    private final Map<String, ArrayList<String>> cmuRhymeIndex = new HashMap<>();
    private final Map<String, ArrayList<String>> cmuFamilyIndex = new HashMap<>();
    private final HashSet<String> cmuDictionaryWords = new HashSet<>();
    private volatile boolean cmuLoaded = false;
    private volatile boolean cmuLoading = false;
    private RhymeEngine rhymeEngine;
    private FrameLayout root;
    private LinearLayout shell;
    private FrameLayout contentHost;
    private LinearLayout menuPanel;
    private LinearLayout editorPanel;
    private View sheetOverlay;
    private LinearLayout sheetCard;
    private TextView sheetTitle;
    private LinearLayout sheetBody;
    private LinearLayout noteList;
    private LinearLayout editor;
    private LinearLayout editorCard;
    private LinearLayout songCard;
    private LinearLayout voiceCard;
    private EditText titleInput;
    private EditText bodyInput;
    private Spinner fontSpinner;
    private TextView colorPreview;
    private TextView countdownView;
    private TextView songStatus;
    private TextView songTime;
    private TextView recordingStatus;
    private LinearLayout recordingList;
    private LinearLayout suggestionPanel;
    private LinearLayout rhymeChips;
    private PopupWindow suggestionPopup;
    private FrameLayout splashOverlay;
    private Button stopRecordingButton;
    private SeekBar songSeek;
    private Note current;
    private int selectedColorTarget = 0;
    private int draftColor = Color.WHITE;
    private int songResumePositionMs = 0;
    private boolean userSeekingSong = false;
    private MediaPlayer songPlayer;
    private MediaPlayer recordingPlayer;
    private MediaRecorder recorder;
    private File activeRecording;
    private long recordingStartedAtMs = 0L;
    private int countdownSeconds = 0;
    private int suggestionStart = -1;
    private int suggestionEnd = -1;
    private volatile int suggestionRequestId = 0;
    private volatile int rhymeCacheVersion = 0;
    private String pendingSuggestionKey = "";
    private String lastRenderedSuggestionKey = "";
    private ArrayList<String> lastRenderedRhymes = new ArrayList<>();
    private int lastPopupLeft = Integer.MIN_VALUE;
    private int lastPopupTop = Integer.MIN_VALUE;
    private final HashSet<String> removedSuggestions = new HashSet<>();
    private final Object suggestionJobLock = new Object();
    private final Object rhymeCacheLock = new Object();
    private Future<?> suggestionFuture;
    private final ExecutorService rhymeExecutor = Executors.newSingleThreadExecutor();
    private final LinkedHashMap<String, ArrayList<String>> rhymeCache = new LinkedHashMap<String, ArrayList<String>>(RHYME_CACHE_LIMIT, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, ArrayList<String>> eldest) {
            return size() > RHYME_CACHE_LIMIT;
        }
    };
    private final Handler editHandler = new Handler(Looper.getMainLooper());
    private final Runnable saveDraftRunnable = this::saveNotes;
    private final Runnable suggestionRunnable = this::updateSuggestionPopup;
    private SharedPreferences prefs;
    private boolean suppressSave = false;
    private TextView playbackStatus;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Handler playbackHandler = new Handler(Looper.getMainLooper());
    private final Runnable playbackTicker = new Runnable() {
        @Override
        public void run() {
            updatePlaybackStatus();
            if ((songPlayer != null && songPlayer.isPlaying()) || (recordingPlayer != null && recordingPlayer.isPlaying())) {
                playbackHandler.postDelayed(this, 250);
            }
        }
    };
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
        rhymeEngine = new RhymeEngine(this);
        countdownSeconds = prefs.getInt("countdown", 0);
        loadRemovedSuggestions();
        createChannels();
        requestRuntimePermissions();
        loadNotes();
        if (notes.isEmpty()) {
            notes.add(Note.create("New Hook"));
            saveNotes();
        }
        buildUi();
        showMenuScreen();
        showStartupSplash();
        startCmuLoad();
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
        playbackHandler.removeCallbacks(playbackTicker);
        editHandler.removeCallbacks(saveDraftRunnable);
        editHandler.removeCallbacks(suggestionRunnable);
        cancelSuggestionJob();
        rhymeExecutor.shutdownNow();
        dismissSuggestionPopup();
        stopRecording(false);
        stopSongPlayback();
        stopRecordingPlayback();
        super.onDestroy();
    }

    private void buildUi() {
        root = new FrameLayout(this);
        root.setBackgroundResource(R.drawable.bg_app_background);
        setContentView(root);

        root.addView(new NeonBackdropView(this), new FrameLayout.LayoutParams(-1, -1));

        shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        FrameLayout.LayoutParams shellLp = new FrameLayout.LayoutParams(
                Math.min(getResources().getDisplayMetrics().widthPixels - dimen(R.dimen.topflow_space_xl), dimen(R.dimen.topflow_max_content_width)),
                -1);
        shellLp.gravity = Gravity.CENTER_HORIZONTAL;
        root.addView(shell, shellLp);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            shell.setPadding(dimen(R.dimen.topflow_space_page), bars.top + dimen(R.dimen.topflow_space_sm), dimen(R.dimen.topflow_space_page), bars.bottom + dimen(R.dimen.topflow_space_sm));
            return insets;
        });

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dimen(R.dimen.topflow_space_xl), dimen(R.dimen.topflow_space_lg), dimen(R.dimen.topflow_space_xl), dimen(R.dimen.topflow_space_lg));
        header.setBackgroundResource(R.drawable.bg_studio_toolbar);
        header.setElevation(dimen(R.dimen.topflow_elevation_lg));
        TextView brand = new TextView(this);
        brand.setText("THE TOP FLOW");
        textStyle(brand, R.style.TextAppearance_TopFlow_Title);
        TextView sub = new TextView(this);
        sub.setText("Offline writing studio");
        textStyle(sub, R.style.TextAppearance_TopFlow_Caption);
        sub.setPadding(2, dp(2), 0, 0);
        LinearLayout titleWrap = new LinearLayout(this);
        titleWrap.setOrientation(LinearLayout.HORIZONTAL);
        titleWrap.setGravity(Gravity.CENTER_VERTICAL);
        titleWrap.addView(brand);
        TextView version = new TextView(this);
        version.setText("  v" + BuildConfig.VERSION_NAME);
        textStyle(version, R.style.TextAppearance_TopFlow_Caption);
        version.setTextColor(color(R.color.topflow_accent_gold));
        titleWrap.addView(version);
        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);
        left.addView(titleWrap);
        left.addView(sub);
        header.addView(left, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        Button menu = button("Studio");
        menu.setOnClickListener(v -> showMainMenu());
        header.addView(menu);
        LinearLayout.LayoutParams headerLp = new LinearLayout.LayoutParams(-1, -2);
        headerLp.bottomMargin = dimen(R.dimen.topflow_space_sm);
        shell.addView(header, headerLp);

        LinearLayout dock = buildStudioDock();
        LinearLayout.LayoutParams dockLp = new LinearLayout.LayoutParams(-1, -2);
        dockLp.bottomMargin = dimen(R.dimen.topflow_space_md);
        shell.addView(dock, dockLp);

        contentHost = new FrameLayout(this);
        shell.addView(contentHost, new LinearLayout.LayoutParams(-1, 0, 1));

        menuPanel = new LinearLayout(this);
        menuPanel.setOrientation(LinearLayout.VERTICAL);
        menuPanel.setPadding(dimen(R.dimen.topflow_space_xl), dimen(R.dimen.topflow_space_xl), dimen(R.dimen.topflow_space_xl), dimen(R.dimen.topflow_space_xl));
        menuPanel.setBackgroundResource(R.drawable.bg_studio_panel);
        menuPanel.setElevation(dimen(R.dimen.topflow_elevation_lg));
        contentHost.addView(menuPanel, new FrameLayout.LayoutParams(-1, -1));

        ScrollView listScroll = new ScrollView(this);
        listScroll.setFillViewport(true);
        noteList = new LinearLayout(this);
        noteList.setOrientation(LinearLayout.VERTICAL);
        noteList.setPadding(0, 0, 0, 0);
        listScroll.addView(noteList);
        menuPanel.addView(listScroll, new LinearLayout.LayoutParams(-1, -1, 1));

        editorPanel = new LinearLayout(this);
        editorPanel.setOrientation(LinearLayout.VERTICAL);
        editorPanel.setVisibility(View.GONE);
        editorPanel.setPadding(dimen(R.dimen.topflow_space_xl), dimen(R.dimen.topflow_space_lg), dimen(R.dimen.topflow_space_xl), dimen(R.dimen.topflow_space_xl));
        editorPanel.setBackgroundResource(R.drawable.bg_studio_panel);
        editorPanel.setElevation(dimen(R.dimen.topflow_elevation_lg));
        contentHost.addView(editorPanel, new FrameLayout.LayoutParams(-1, -1));

        ScrollView editorScroll = new ScrollView(this);
        editorScroll.setFillViewport(true);
        editor = new LinearLayout(this);
        editor.setOrientation(LinearLayout.VERTICAL);
        editor.setPadding(0, 0, 0, 0);
        editorScroll.addView(editor);
        editorPanel.addView(editorScroll, new LinearLayout.LayoutParams(-1, -1, 1));

        editorCard = createCardSurface();
        editorCard.setPadding(dimen(R.dimen.topflow_space_xl), dimen(R.dimen.topflow_space_xl), dimen(R.dimen.topflow_space_xl), dimen(R.dimen.topflow_space_xl));
        LinearLayout.LayoutParams editorCardLp = new LinearLayout.LayoutParams(-1, -2);
        editorCardLp.bottomMargin = dimen(R.dimen.topflow_space_md);
        editor.addView(editorCard, editorCardLp);

        TextView editorHead = new TextView(this);
        editorHead.setText("WRITING DESK");
        textStyle(editorHead, R.style.TextAppearance_TopFlow_Caption);
        editorHead.setTextColor(color(R.color.topflow_accent_gold));
        editorHead.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        editorHead.setPadding(2, 0, 2, dimen(R.dimen.topflow_space_xs));
        editorCard.addView(editorHead);

        titleInput = new EditText(this);
        titleInput.setSingleLine(true);
        titleInput.setHint("Untitled track");
        titleInput.setBackgroundResource(R.drawable.bg_text_field);
        textStyle(titleInput, R.style.TextAppearance_TopFlow_Title);
        titleInput.setPadding(dimen(R.dimen.topflow_space_md), dimen(R.dimen.topflow_space_sm), dimen(R.dimen.topflow_space_md), dimen(R.dimen.topflow_space_sm));
        editorCard.addView(titleInput, new LinearLayout.LayoutParams(-1, -2));

        bodyInput = new RuledEditText(this);
        bodyInput.setGravity(Gravity.TOP | Gravity.START);
        bodyInput.setMinLines(18);
        bodyInput.setHint("Start writing...");
        bodyInput.setBackgroundResource(R.drawable.bg_editor_surface);
        textStyle(bodyInput, R.style.TextAppearance_TopFlow_Body);
        bodyInput.setLineSpacing(dp(3), 1.08f);
        bodyInput.setPadding(dimen(R.dimen.topflow_space_lg), dimen(R.dimen.topflow_space_lg), dimen(R.dimen.topflow_space_lg), dimen(R.dimen.topflow_space_lg));
        editorCard.addView(bodyInput, new LinearLayout.LayoutParams(-1, 0, 1));

        suggestionPanel = buildSuggestionRow("Rhymes");
        suggestionPanel.setVisibility(View.GONE);
        suggestionPopup = new PopupWindow(suggestionPanel, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, false);
        suggestionPopup.setTouchable(true);
        suggestionPopup.setOutsideTouchable(false);
        suggestionPopup.setClippingEnabled(true);

        playbackStatus = label("");
        playbackStatus.setTextSize(14);
        playbackStatus.setPadding(4, 10, 4, 10);
        editorCard.addView(playbackStatus, new LinearLayout.LayoutParams(-1, -2));
        playbackStatus.setVisibility(View.GONE);

        songCard = createSectionCard("Track");
        LinearLayout.LayoutParams songLp = new LinearLayout.LayoutParams(-1, -2);
        songLp.bottomMargin = dimen(R.dimen.topflow_space_md);
        editor.addView(songCard, songLp);
        voiceCard = createSectionCard("Voice Capture");
        editor.addView(voiceCard, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout songMeta = new LinearLayout(this);
        songMeta.setOrientation(LinearLayout.VERTICAL);
        songCard.addView(songMeta);
        songStatus = metadataLabel("No song attached");
        songMeta.addView(songStatus);
        songSeek = new SeekBar(this);
        songSeek.setMax(1000);
        songSeek.setEnabled(false);
        songSeek.setProgress(0);
        songSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser) return;
                songResumePositionMs = progressToSongMs(progress);
                if (songPlayer != null) {
                    userSeekingSong = true;
                    songPlayer.seekTo(songResumePositionMs);
                    userSeekingSong = false;
                    updatePlaybackStatus();
                }
            }
            public void onStartTrackingTouch(SeekBar seekBar) { userSeekingSong = true; }
            public void onStopTrackingTouch(SeekBar seekBar) {
                userSeekingSong = false;
                if (songPlayer != null) songPlayer.seekTo(songResumePositionMs);
                updatePlaybackStatus();
            }
        });
        songMeta.addView(songSeek, new LinearLayout.LayoutParams(-1, -2));
        songTime = metadataLabel("0:00 / 0:00");
        songMeta.addView(songTime);

        Button attach = button("Attach Song");
        attach.setOnClickListener(v -> pickSong());
        Button play = button("Play / Pause Song");
        play.setOnClickListener(v -> toggleSong());
        LinearLayout songActions = horizontalCardButtons(attach, play);
        songMeta.addView(songActions);
        attach.setVisibility(BuildConfig.VERSION_CODE >= 2 ? View.VISIBLE : View.GONE);
        play.setVisibility(BuildConfig.VERSION_CODE >= 2 ? View.VISIBLE : View.GONE);
        Button record = button("Record Voice Note");
        record.setOnClickListener(v -> startCountdownThenRecord());
        recordingStatus = metadataLabel("Ready to record");
        voiceCard.addView(recordingStatus);
        voiceCard.addView(countdownView = new TextView(this));
        countdownView.setTextColor(TopFlowUiKit.TEXT);
        countdownView.setTextSize(28);
        countdownView.setGravity(Gravity.CENTER_VERTICAL);
        countdownView.setVisibility(View.GONE);
        stopRecordingButton = button("Stop");
        stopRecordingButton.setOnClickListener(v -> {
            if (recorder != null) {
                stopRecording(true);
            } else if (recordingPlayer != null && recordingPlayer.isPlaying()) {
                stopRecordingPlayback();
            }
            updateRecordingControls();
        });
        stopRecordingButton.setVisibility(View.GONE);
        LinearLayout voiceActions = horizontalCardButtons(record, stopRecordingButton);
        voiceCard.addView(voiceActions);
        record.setVisibility(BuildConfig.VERSION_CODE >= 3 ? View.VISIBLE : View.GONE);
        stopRecordingButton.setVisibility(View.GONE);

        fontSpinner = new Spinner(this);
        String[] fonts = {"sans", "serif", "monospace", "casual", "cursive"};
        fontSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, fonts));
        fontSpinner.setVisibility(View.GONE);
        colorPreview = label("");
        colorPreview.setVisibility(View.GONE);

        titleInput.addTextChangedListener(simpleWatcher(() -> {
            if (!suppressSave && current != null) {
                current.title = titleInput.getText().toString();
                saveAndRenderList();
            }
        }));
        bodyInput.addTextChangedListener(simpleWatcher(() -> {
            if (!suppressSave && current != null) {
                current.body = bodyInput.getText().toString();
            }
            scheduleDraftSave();
            scheduleSuggestionUpdate();
        }));
        if (bodyInput instanceof RuledEditText) {
            ((RuledEditText) bodyInput).setOnSelectionChangedListener(this::scheduleSuggestionUpdate);
        }
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

        recordingList = new LinearLayout(this);
        recordingList.setOrientation(LinearLayout.VERTICAL);
        voiceCard.addView(recordingList);

        sheetOverlay = buildSheetOverlay();
        root.addView(sheetOverlay, new FrameLayout.LayoutParams(-1, -1));
        styleActionButtonPalette(C_GREEN);
        applyInsetsNow();
        animateScreenSwap(menuPanel, 1f);
    }

    private LinearLayout buildStudioDock() {
        LinearLayout dock = new LinearLayout(this);
        dock.setOrientation(LinearLayout.HORIZONTAL);
        dock.setGravity(Gravity.CENTER_VERTICAL);
        dock.setPadding(dimen(R.dimen.topflow_space_sm), dimen(R.dimen.topflow_space_xs), dimen(R.dimen.topflow_space_sm), dimen(R.dimen.topflow_space_xs));
        dock.setBackgroundResource(R.drawable.bg_studio_dock);
        dock.setElevation(dimen(R.dimen.topflow_elevation_md));
        Button notes = button("Notes");
        notes.setOnClickListener(v -> showMenuScreen());
        Button rhymes = button("Rhymes");
        rhymes.setOnClickListener(v -> {
            if (current == null) {
                Toast.makeText(this, "Open a note first", Toast.LENGTH_SHORT).show();
                return;
            }
            showExpandedRhymes();
        });
        Button style = button("Style");
        style.setOnClickListener(v -> {
            if (current == null) {
                Toast.makeText(this, "Open a note first", Toast.LENGTH_SHORT).show();
                return;
            }
            showStyleMenu();
        });
        Button settings = button("Settings");
        settings.setOnClickListener(v -> showRhymeSettingsMenu());
        dock.addView(notes, dockButtonLp(0));
        dock.addView(rhymes, dockButtonLp(dimen(R.dimen.topflow_space_xs)));
        dock.addView(style, dockButtonLp(dimen(R.dimen.topflow_space_xs)));
        dock.addView(settings, dockButtonLp(dimen(R.dimen.topflow_space_xs)));
        return dock;
    }

    private LinearLayout.LayoutParams dockButtonLp(int leftMargin) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1);
        lp.leftMargin = leftMargin;
        return lp;
    }

    private void showStartupSplash() {
        if (root == null) return;
        splashOverlay = new FrameLayout(this);
        splashOverlay.setBackgroundColor(Color.rgb(2, 5, 12));
        splashOverlay.setClickable(true);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(28), dp(28), dp(28), dp(28));
        TextView intro = new TextView(this);
        intro.setText("Owens, Betcha, & Ondewey Technologies present...");
        intro.setTextColor(C_TEXT);
        intro.setTextSize(16);
        intro.setGravity(Gravity.CENTER);
        intro.setShadowLayer(8f, 0f, 0f, C_CYAN);
        box.addView(intro, new LinearLayout.LayoutParams(-1, -2));
        FrameLayout track = new FrameLayout(this);
        track.setBackground(glassDrawable(Color.argb(120, 10, 20, 32), C_CYAN, 6));
        LinearLayout.LayoutParams trackLp = new LinearLayout.LayoutParams(-1, dp(12));
        trackLp.topMargin = dp(18);
        box.addView(track, trackLp);
        View fill = new View(this);
        fill.setBackground(glassDrawable(C_CYAN, C_GREEN, 6));
        fill.setPivotX(0f);
        fill.setScaleX(0f);
        track.addView(fill, new FrameLayout.LayoutParams(-1, -1));
        FrameLayout.LayoutParams boxLp = new FrameLayout.LayoutParams(-1, -2, Gravity.CENTER);
        boxLp.leftMargin = dp(24);
        boxLp.rightMargin = dp(24);
        splashOverlay.addView(box, boxLp);
        root.addView(splashOverlay, new FrameLayout.LayoutParams(-1, -1));
        fill.animate().scaleX(1f).setDuration(650).start();
        splashOverlay.animate().alpha(0f).setStartDelay(780).setDuration(180).withEndAction(() -> {
            if (root != null && splashOverlay != null) root.removeView(splashOverlay);
            splashOverlay = null;
        }).start();
    }

    private void renderNoteList() {
        noteList.removeAllViews();
        TextView head = label("Notes");
        textStyle(head, R.style.TextAppearance_TopFlow_Caption);
        head.setTextColor(color(R.color.topflow_accent_gold));
        head.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        head.setPadding(dp(2), 0, dp(2), dimen(R.dimen.topflow_space_xs));
        noteList.addView(head);
        Button add = button("+ Note");
        styleButton(add, C_GREEN);
        add.setOnClickListener(v -> {
            Note n = Note.create("Untitled");
            notes.add(0, n);
            saveNotes();
            renderNoteList();
            openNote(n);
        });
        add.setMinHeight(dp(52));
        LinearLayout.LayoutParams addLp = new LinearLayout.LayoutParams(-1, -2);
        addLp.bottomMargin = dimen(R.dimen.topflow_space_md);
        noteList.addView(add, addLp);
        if (notes.isEmpty()) {
            noteList.addView(buildEmptyNotesState(), new LinearLayout.LayoutParams(-1, -2));
            return;
        }
        for (int i = 0; i < notes.size(); i++) {
            View row = buildNoteRow(notes.get(i));
            row.setAlpha(0f);
            row.setTranslationY(dimen(R.dimen.topflow_space_sm));
            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, -2);
            rowLp.bottomMargin = dimen(R.dimen.topflow_space_sm);
            noteList.addView(row, rowLp);
            row.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay(40L * i)
                    .setDuration(180)
                    .start();
        }
    }

    private View buildEmptyNotesState() {
        LinearLayout empty = new LinearLayout(this);
        empty.setOrientation(LinearLayout.VERTICAL);
        empty.setGravity(Gravity.CENTER_VERTICAL);
        empty.setPadding(dimen(R.dimen.topflow_space_xl), dimen(R.dimen.topflow_space_xl), dimen(R.dimen.topflow_space_xl), dimen(R.dimen.topflow_space_xl));
        empty.setBackgroundResource(R.drawable.bg_empty_state);
        TextView title = label("No notes yet");
        textStyle(title, R.style.TextAppearance_TopFlow_Section);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        TextView body = label("Tap + Note to start a writing session.");
        textStyle(body, R.style.TextAppearance_TopFlow_Caption);
        body.setPadding(0, dimen(R.dimen.topflow_space_xs), 0, 0);
        empty.addView(title);
        empty.addView(body);
        return empty;
    }

    private View buildNoteRow(Note note) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dimen(R.dimen.topflow_space_lg), dimen(R.dimen.topflow_space_lg), dimen(R.dimen.topflow_space_lg), dimen(R.dimen.topflow_space_lg));
        row.setMinimumHeight(dp(76));
        row.setBackground(glassDrawable(tintFrom(note.noteColor, 0.06f), note.accentColor, 22));
        row.setElevation(dp(4));
        row.setForeground(ripple(note.accentColor));
        row.setClickable(true);
        row.setFocusable(true);
        attachTapAnimation(row);

        View edge = new View(this);
        edge.setBackgroundColor(note.accentColor);
        LinearLayout.LayoutParams edgeLp = new LinearLayout.LayoutParams(dp(4), -1);
        edgeLp.rightMargin = dp(12);
        row.addView(edge, edgeLp);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));
        TextView title = new TextView(this);
        title.setText(note.title == null || note.title.isEmpty() ? "Untitled" : note.title);
        title.setTextColor(note.textColor);
        textSize(title, R.dimen.topflow_text_section);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setMaxLines(1);
        title.setShadowLayer(3f, 0f, 0f, Color.argb(120, Color.red(note.accentColor), Color.green(note.accentColor), Color.blue(note.accentColor)));
        TextView preview = new TextView(this);
        preview.setText(compactPreview(note.body));
        textStyle(preview, R.style.TextAppearance_TopFlow_Caption);
        preview.setMaxLines(2);
        preview.setPadding(0, dp(3), 0, 0);
        box.addView(title);
        box.addView(preview);
        row.addView(box);

        row.setOnClickListener(v -> openNote(note));
        return row;
    }

    private void renderRecordings() {
        if (recordingList == null || current == null) return;
        recordingList.removeAllViews();
        if (BuildConfig.VERSION_CODE < 3) return;
        TextView header = label("Recordings");
        header.setPadding(0, dimen(R.dimen.topflow_space_xl), 0, dimen(R.dimen.topflow_space_xs));
        recordingList.addView(header);
        for (RecordingTag tag : current.recordings) recordingList.addView(buildRecordingRow(tag));
    }

    private View buildRecordingRow(RecordingTag tag) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dimen(R.dimen.topflow_space_md), dimen(R.dimen.topflow_space_sm), dimen(R.dimen.topflow_space_md), dimen(R.dimen.topflow_space_sm));
        row.setBackgroundResource(R.drawable.bg_surface_panel);
        row.setForeground(ripple(C_CYAN));
        row.setClickable(true);
        row.setFocusable(true);
        attachTapAnimation(row);
        TextView name = label(tag.tag);
        name.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));
        Button play = button("Play");
        play.setOnClickListener(v -> playRecording(tag));
        Button rename = button("Rename");
        rename.setOnClickListener(v -> renameRecording(tag));
        Button save = button("Save");
        save.setOnClickListener(v -> saveRecordingTagToDisk(tag));
        row.addView(name);
        row.addView(play);
        row.addView(rename);
        row.addView(save);
        return row;
    }

    private void styleActionButtonPalette(int accent) {
        if (editor == null) return;
        styleButtonsIn(editor, accent);
    }

    private void styleButtonsIn(View view, int accent) {
        if (view instanceof Button) {
            styleButton((Button) view, accent);
            return;
        }
        if (!(view instanceof ViewGroup)) return;
        ViewGroup group = (ViewGroup) view;
        for (int i = 0; i < group.getChildCount(); i++) {
            styleButtonsIn(group.getChildAt(i), accent);
        }
    }

    private void playRecording(String path) {
        try {
            stopSongPlayback();
            stopRecordingPlayback();
            recordingPlayer = MediaPlayer.create(this, Uri.fromFile(new File(path)));
            if (recordingPlayer != null) {
                recordingPlayer.setOnCompletionListener(mp -> {
                    mp.release();
                    recordingPlayer = null;
                    updatePlaybackStatus();
                    updateMediaLabels();
                });
                recordingPlayer.start();
                startPlaybackTicker();
                updateMediaLabels();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Could not play recording.", Toast.LENGTH_SHORT).show();
        }
    }

    private void playRecording(RecordingTag tag) {
        playRecording(tag.path);
    }

    private void renameRecording(RecordingTag tag) {
        EditText input = new EditText(this);
        input.setText(tag.tag);
        new AlertDialog.Builder(this)
                .setTitle("Rename tag")
                .setView(input)
                .setPositiveButton("Save", (d, w) -> {
                    String value = input.getText().toString().trim();
                    if (!value.isEmpty()) tag.tag = value;
                    saveNotes();
                    renderRecordings();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveSelectedRecordingToDisk() {
        if (activeRecording != null && activeRecording.exists()) {
            saveRecordingFileToDisk(activeRecording);
            return;
        }
        if (current != null && !current.recordings.isEmpty()) {
            saveRecordingFileToDisk(new File(current.recordings.get(0).path));
            return;
        }
        Toast.makeText(this, "No recording to save.", Toast.LENGTH_SHORT).show();
    }

    private void saveRecordingTagToDisk(RecordingTag tag) {
        saveRecordingFileToDisk(new File(tag.path));
    }

    private void saveRecordingFileToDisk(File source) {
        if (source == null || !source.exists()) {
            Toast.makeText(this, "Recording file is missing.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (Build.VERSION.SDK_INT < 29) {
            Toast.makeText(this, "Disk save needs Android 10+.", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            android.content.ContentValues values = new android.content.ContentValues();
            values.put(MediaStore.Audio.Media.DISPLAY_NAME, source.getName());
            values.put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4");
            values.put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/The Top Flow");
            Uri uri = getContentResolver().insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values);
            if (uri == null) {
                Toast.makeText(this, "Could not create audio file entry.", Toast.LENGTH_SHORT).show();
                return;
            }
            try (FileInputStream in = new FileInputStream(source);
                 OutputStream out = getContentResolver().openOutputStream(uri)) {
                byte[] buf = new byte[8192];
                int read;
                while (out != null && (read = in.read(buf)) > -1) out.write(buf, 0, read);
            }
            Toast.makeText(this, "Saved to disk.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Save to disk failed.", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateRecordingControls() {
        boolean visible = recorder != null
                || (recordingPlayer != null && recordingPlayer.isPlaying())
                || (countdownView != null && countdownView.getVisibility() == View.VISIBLE && countdownView.getText() != null && countdownView.getText().length() > 0);
        if (stopRecordingButton != null) stopRecordingButton.setVisibility(visible ? View.VISIBLE : View.GONE);
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
        showEditorScreen();
        applyStyle();
        renderRecordings();
        scheduleSuggestionUpdate();
        updatePlaybackStatus();
    }

    private void showMenuScreen() {
        dismissSuggestionPopup();
        if (songCard != null) songCard.setVisibility(View.VISIBLE);
        if (voiceCard != null) voiceCard.setVisibility(View.VISIBLE);
        animatePanel(menuPanel, true);
        animatePanel(editorPanel, false);
        renderNoteList();
    }

    private void showEditorScreen() {
        if (songCard != null) songCard.setVisibility(View.GONE);
        if (voiceCard != null) voiceCard.setVisibility(View.GONE);
        animatePanel(editorPanel, true);
        animatePanel(menuPanel, false);
    }

    private void animatePanel(View panel, boolean show) {
        if (panel == null) return;
        if (show) {
            panel.setVisibility(View.VISIBLE);
            panel.setAlpha(0f);
            panel.setScaleX(0.98f);
            panel.setScaleY(0.98f);
            panel.setTranslationY(dp(14));
            panel.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .translationY(0f)
                    .setDuration(180)
                    .start();
        } else {
            panel.animate()
                    .alpha(0f)
                    .scaleX(0.985f)
                    .scaleY(0.985f)
                    .translationY(dp(-10))
                    .setDuration(130)
                    .withEndAction(() -> panel.setVisibility(View.GONE))
                    .start();
        }
    }

    private void animateScreenSwap(View firstVisible, float emphasis) {
        if (firstVisible == null) return;
        firstVisible.setAlpha(0f);
        firstVisible.setTranslationY(dp(10));
        firstVisible.setScaleX(0.985f);
        firstVisible.setScaleY(0.985f);
        firstVisible.animate()
                .alpha(1f)
                .translationY(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration((long) (220 * emphasis))
                .start();
    }

    private void applyStyle() {
        if (current == null) return;
        titleInput.setBackgroundResource(R.drawable.bg_text_field);
        bodyInput.setBackgroundResource(R.drawable.bg_editor_surface);
        titleInput.setTextColor(current.textColor);
        bodyInput.setTextColor(current.textColor);
        titleInput.setHintTextColor(current.accentColor);
        bodyInput.setHintTextColor(current.accentColor);
        titleInput.setTypeface(android.graphics.Typeface.create(current.font, android.graphics.Typeface.BOLD));
        bodyInput.setTypeface(android.graphics.Typeface.create(current.font, android.graphics.Typeface.NORMAL));
        if (Build.VERSION.SDK_INT >= 29) titleInput.setTextCursorDrawable(null);
        styleActionButtonPalette(current.accentColor);
        if (editorCard != null) editorCard.setBackground(TopFlowUiKit.floatingPanel(this, 26));
        if (songCard != null) songCard.setBackground(TopFlowUiKit.floatingPanel(this, 20));
        if (voiceCard != null) voiceCard.setBackground(TopFlowUiKit.floatingPanel(this, 20));
        if (colorPreview != null) colorPreview.setBackgroundColor(current.noteColor);
        if (playbackStatus != null) playbackStatus.setTextColor(current.accentColor);
        if (songStatus != null) songStatus.setTextColor(TopFlowUiKit.TEXT_SOFT);
        if (recordingStatus != null) recordingStatus.setTextColor(recorder != null ? C_RED : TopFlowUiKit.TEXT_SOFT);
        if (songSeek != null) styleSeekBar(songSeek, current.accentColor);
        if (bodyInput != null) {
            bodyInput.setBackgroundResource(R.drawable.bg_editor_surface);
            if (bodyInput instanceof RuledEditText) {
                ((RuledEditText) bodyInput).setRuleColor(current.accentColor);
            }
        }
        renderNoteList();
        renderRecordings();
        updateMediaLabels();
        scheduleSuggestionUpdate();
    }

    private void stopSongPlayback() {
        if (songPlayer != null) {
            try {
                songPlayer.stop();
            } catch (Exception ignored) {
            }
            songPlayer.release();
            songPlayer = null;
        }
        songResumePositionMs = 0;
        updateMediaLabels();
    }

    private void stopRecordingPlayback() {
        if (recordingPlayer != null) {
            try {
                recordingPlayer.stop();
            } catch (Exception ignored) {
            }
            recordingPlayer.release();
            recordingPlayer = null;
        }
        updateRecordingControls();
        updateMediaLabels();
    }

    private void startPlaybackTicker() {
        playbackHandler.removeCallbacks(playbackTicker);
        playbackHandler.post(playbackTicker);
    }

    private void updatePlaybackStatus() {
        if (playbackStatus == null) return;
        boolean songActive = songPlayer != null && songPlayer.isPlaying();
        boolean recordingActive = recordingPlayer != null && recordingPlayer.isPlaying();
        boolean isRecordingNow = recorder != null;
        if (!songActive && !recordingActive && !isRecordingNow) {
            playbackStatus.setVisibility(View.GONE);
            playbackStatus.setText("");
            if (songPlayer != null) {
                int pos = songPlayer.getCurrentPosition() / 1000;
                int dur = Math.max(1, songPlayer.getDuration() / 1000);
                if (songTime != null) songTime.setText(formatSeconds(pos) + " / " + formatSeconds(dur));
                if (songSeek != null && !userSeekingSong) songSeek.setProgress(progressForSongMs(songPlayer.getCurrentPosition()));
                songResumePositionMs = songPlayer.getCurrentPosition();
            } else {
                if (songTime != null) songTime.setText("0:00 / 0:00");
                if (songSeek != null && !userSeekingSong) songSeek.setProgress(songResumePositionMs > 0 ? progressForSongMs(songResumePositionMs) : 0);
            }
            return;
        }
        updateRecordingControls();
        StringBuilder text = new StringBuilder();
        if (songActive) {
            int pos = songPlayer.getCurrentPosition() / 1000;
            int dur = Math.max(1, songPlayer.getDuration() / 1000);
            text.append("Song ").append(formatSeconds(pos)).append(" / ").append(formatSeconds(dur));
            text.append("  ").append(formatSeconds(Math.max(0, dur - pos))).append(" left");
            if (songTime != null) songTime.setText(formatSeconds(pos) + " / " + formatSeconds(dur));
            if (songSeek != null && !userSeekingSong) songSeek.setProgress(progressForSongMs(songPlayer.getCurrentPosition()));
            songResumePositionMs = songPlayer.getCurrentPosition();
        } else if (songPlayer != null) {
            int pos = songPlayer.getCurrentPosition() / 1000;
            int dur = Math.max(1, songPlayer.getDuration() / 1000);
            if (songTime != null) songTime.setText(formatSeconds(pos) + " / " + formatSeconds(dur));
            if (songSeek != null && !userSeekingSong) songSeek.setProgress(progressForSongMs(songPlayer.getCurrentPosition()));
        }
        if (isRecordingNow) {
            long elapsed = Math.max(0L, System.currentTimeMillis() - recordingStartedAtMs) / 1000L;
            if (text.length() > 0) text.append("  ");
            text.append("Recording ").append(formatSeconds((int) elapsed)).append(" elapsed");
        } else if (recordingActive) {
            int pos = recordingPlayer.getCurrentPosition() / 1000;
            int dur = Math.max(1, recordingPlayer.getDuration() / 1000);
            if (text.length() > 0) text.append("  ");
            text.append("Recording ").append(formatSeconds(pos)).append(" / ").append(formatSeconds(dur));
        }
        playbackStatus.setText(text.toString());
        playbackStatus.setVisibility(View.VISIBLE);
        updateMediaLabels();
    }

    private void updateSongControls() {
        boolean enabled = current != null && current.songUri != null && !current.songUri.isEmpty();
        boolean visible = recorder != null || (recordingPlayer != null && recordingPlayer.isPlaying()) || (countdownView != null && countdownView.getVisibility() == View.VISIBLE && countdownView.getText() != null && countdownView.getText().length() > 0);
        if (songSeek != null) songSeek.setEnabled(enabled);
        if (stopRecordingButton != null) stopRecordingButton.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private String formatSeconds(int seconds) {
        int m = Math.max(0, seconds) / 60;
        int s = Math.max(0, seconds) % 60;
        return String.format(Locale.US, "%d:%02d", m, s);
    }

    private void updateMediaLabels() {
        if (songStatus != null) {
            if (current == null || current.songUri == null || current.songUri.isEmpty()) {
                songStatus.setText("No song attached");
                if (songSeek != null) songSeek.setEnabled(false);
            } else if (songPlayer != null && songPlayer.isPlaying()) {
                songStatus.setText("Playing " + displayNameForUri(current.songUri));
                if (songSeek != null) songSeek.setEnabled(true);
            } else if (songPlayer != null) {
                songStatus.setText("Paused " + displayNameForUri(current.songUri));
                if (songSeek != null) songSeek.setEnabled(true);
            } else {
                songStatus.setText("Attached: " + displayNameForUri(current.songUri));
                if (songSeek != null) songSeek.setEnabled(true);
            }
        }
        if (recordingStatus != null) {
            if (recorder != null) {
                recordingStatus.setTextColor(C_RED);
                recordingStatus.setText("Recording " + formatSeconds((int) (Math.max(0L, System.currentTimeMillis() - recordingStartedAtMs) / 1000L)));
            } else if (recordingPlayer != null && recordingPlayer.isPlaying()) {
                recordingStatus.setTextColor(C_CYAN);
                recordingStatus.setText("Playing saved voice note");
            } else {
                recordingStatus.setTextColor(C_TEXT_MUTED);
                recordingStatus.setText("Ready to record");
            }
        }
        updateRecordingControls();
    }

    private String displayNameForUri(String uriText) {
        try {
            Uri uri = Uri.parse(uriText);
            String last = uri.getLastPathSegment();
            if (last == null || last.trim().isEmpty()) return "song";
            return last;
        } catch (Exception e) {
            return "song";
        }
    }

    private LinearLayout buildSuggestionRow(String title) {
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setPadding(dimen(R.dimen.topflow21_space_panel), dimen(R.dimen.topflow_space_md), dimen(R.dimen.topflow21_space_panel), dimen(R.dimen.topflow21_space_panel));
        wrap.setBackground(TopFlowUiKit.floatingPanel(this, 22));
        TopFlowUiKit.applyFloating(wrap, 14);
        TextView label = new TextView(this);
        label.setText("RHYME ENGINE");
        textStyle(label, R.style.TextAppearance_TopFlow21_Caption);
        label.setTextColor(TopFlowUiKit.MINT);
        label.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        label.setPadding(2, 0, 2, dimen(R.dimen.topflow_space_xs));
        wrap.addView(label);
        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout chips = new LinearLayout(this);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        chips.setPadding(0, 0, dp(2), 0);
        scroll.addView(chips, new LinearLayout.LayoutParams(-2, -2));
        wrap.addView(scroll, new LinearLayout.LayoutParams(-1, -2));
        rhymeChips = chips;
        return wrap;
    }

    private void scheduleSuggestionUpdate() {
        suggestionRequestId++;
        cancelSuggestionJob();
        editHandler.removeCallbacks(suggestionRunnable);
        editHandler.postDelayed(suggestionRunnable, SUGGESTION_DEBOUNCE_MS);
    }

    private void scheduleDraftSave() {
        editHandler.removeCallbacks(saveDraftRunnable);
        editHandler.postDelayed(saveDraftRunnable, 250);
    }

    private void updateSuggestionPopup() {
        long started = System.currentTimeMillis();
        if (suggestionPanel == null || bodyInput == null) return;
        if (!prefs.getBoolean(PREF_SHOW_RHYME_ROW, true)) {
            pendingSuggestionKey = "";
            dismissSuggestionPopup();
            return;
        }
        CharSequence text = bodyInput.getText() == null ? "" : bodyInput.getText();
        int cursor = Math.max(0, Math.min(bodyInput.getSelectionStart(), text.length()));
        TokenInfo info = currentToken(text, cursor);
        if (info.word.isEmpty() && cursor > 0) {
            info = previousToken(text, cursor);
        }
        if (info.word.length() < 2) {
            pendingSuggestionKey = "";
            suggestionStart = -1;
            suggestionEnd = -1;
            dismissSuggestionPopup();
            return;
        }
        int limit = Math.min(FAST_RHYME_LIMIT, configuredMaxSuggestions());
        String query = normalizeWord(info.word);
        String cacheKey = rhymeCacheKey(query, limit);
        pendingSuggestionKey = cacheKey;
        if (!rhymeEngine.isReady()) {
            cancelSuggestionJob();
            suggestionStart = info.start;
            suggestionEnd = info.end;
            if (!cacheKey.equals(lastRenderedSuggestionKey)) {
                renderSuggestionStatus(rhymeChips, "Loading rhymes");
                lastRenderedSuggestionKey = cacheKey;
                lastRenderedRhymes = new ArrayList<>();
            }
            positionSuggestionPopup(cursor);
            Log.d(TAG, "rhyme row waiting for CMU index word=" + query);
            return;
        }
        ArrayList<String> cached = cachedRhymes(cacheKey);
        int requestId = suggestionRequestId;
        if (cached != null) {
            applySuggestionResults(requestId, cacheKey, info.start, info.end, cursor, cached, true, 0L, started);
            return;
        }
        if (!cacheKey.equals(lastRenderedSuggestionKey)) {
            dismissSuggestionPopup();
        }
        startSuggestionJob(requestId, cacheKey, info.word, info.start, info.end, cursor, limit, started);
    }

    private void startSuggestionJob(int requestId, String cacheKey, String word, int start, int end, int cursor, int limit, long requestStartedAt) {
        cancelSuggestionJob();
        if (rhymeExecutor.isShutdown()) return;
        synchronized (suggestionJobLock) {
            suggestionFuture = rhymeExecutor.submit(() -> {
                long generateStarted = System.currentTimeMillis();
                ArrayList<String> cached = cachedRhymes(cacheKey);
                ArrayList<String> rhymes;
                boolean cacheHit = cached != null;
                if (cacheHit) {
                    rhymes = cached;
                } else {
                    rhymes = suggestRhymes(word, limit, FAST_RHYME_CANDIDATE_LIMIT);
                    if (Thread.currentThread().isInterrupted()) return;
                    putCachedRhymes(cacheKey, rhymes);
                }
                long generateMs = System.currentTimeMillis() - generateStarted;
                if (generateMs >= 16L || !cacheHit) {
                    Log.d(TAG, "rhyme generation word=" + normalizeWord(word) + " count=" + rhymes.size() + " ms=" + generateMs + " cache=" + cacheHit);
                }
                if (Thread.currentThread().isInterrupted()) return;
                editHandler.post(() -> applySuggestionResults(requestId, cacheKey, start, end, cursor, rhymes, cacheHit, generateMs, requestStartedAt));
            });
        }
    }

    private void applySuggestionResults(int requestId, String cacheKey, int start, int end, int cursor, ArrayList<String> rhymes, boolean cacheHit, long generateMs, long requestStartedAt) {
        long uiStarted = System.currentTimeMillis();
        if (requestId != suggestionRequestId || !cacheKey.equals(pendingSuggestionKey)) return;
        suggestionStart = start;
        suggestionEnd = end;
        if (rhymes.isEmpty()) {
            dismissSuggestionPopup();
            return;
        }
        if (!cacheKey.equals(lastRenderedSuggestionKey) || !sameWords(lastRenderedRhymes, rhymes)) {
            renderSuggestionChips(rhymeChips, rhymes);
            lastRenderedSuggestionKey = cacheKey;
            lastRenderedRhymes = new ArrayList<>(rhymes);
        }
        positionSuggestionPopup(cursor);
        long uiMs = System.currentTimeMillis() - uiStarted;
        long totalMs = System.currentTimeMillis() - requestStartedAt;
        if (uiMs >= 8L || generateMs >= 16L || totalMs >= 80L) {
            Log.d(TAG, "rhyme ui count=" + rhymes.size() + " uiMs=" + uiMs + " genMs=" + generateMs + " totalMs=" + totalMs + " cache=" + cacheHit);
        }
    }

    private void positionSuggestionPopup(int cursor) {
        if (suggestionPanel == null || bodyInput == null || suggestionPopup == null) return;
        suggestionPanel.setVisibility(View.VISIBLE);
        suggestionPanel.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int[] bodyLoc = new int[2];
        bodyInput.getLocationOnScreen(bodyLoc);
        Layout layout = bodyInput.getLayout();
        if (layout == null) return;
        CharSequence text = bodyInput.getText() == null ? "" : bodyInput.getText();
        cursor = Math.max(0, Math.min(cursor, text.length()));
        int line = layout.getLineForOffset(Math.max(0, Math.min(cursor, text.length())));
        float x = layout.getPrimaryHorizontal(Math.max(0, Math.min(cursor, text.length())));
        int top = bodyLoc[1] + bodyInput.getCompoundPaddingTop() + layout.getLineBottom(line) - bodyInput.getScrollY() + dp(8);
        int left = bodyLoc[0] + bodyInput.getCompoundPaddingLeft() + Math.round(x) - suggestionPanel.getMeasuredWidth() / 2;
        left = Math.max(dp(8), Math.min(left, getResources().getDisplayMetrics().widthPixels - suggestionPanel.getMeasuredWidth() - dp(8)));
        top = Math.max(dp(8), top);
        if (suggestionPopup.isShowing()) {
            if (Math.abs(left - lastPopupLeft) > 3 || Math.abs(top - lastPopupTop) > 3) {
                suggestionPopup.update(left, top, -1, -1);
            }
        } else {
            suggestionPopup.showAtLocation(root, Gravity.TOP | Gravity.START, left, top);
        }
        lastPopupLeft = left;
        lastPopupTop = top;
    }

    private String rhymeCacheKey(String word, int limit) {
        return normalizeWord(word)
                + "|" + limit
                + "|" + (rhymeEngine.isReady() ? "engine" : "loading")
                + "|" + strictnessName()
                + "|" + prefs.getBoolean(PREF_EXACT_ONLY, false)
                + "|" + prefs.getBoolean(PREF_INCLUDE_SLANG, true)
                + "|" + rhymeCacheVersion
                + "|" + rhymeEngine.generation();
    }

    private ArrayList<String> cachedRhymes(String cacheKey) {
        synchronized (rhymeCacheLock) {
            ArrayList<String> cached = rhymeCache.get(cacheKey);
            return cached == null ? null : new ArrayList<>(cached);
        }
    }

    private void putCachedRhymes(String cacheKey, ArrayList<String> rhymes) {
        synchronized (rhymeCacheLock) {
            rhymeCache.put(cacheKey, new ArrayList<>(rhymes));
        }
    }

    private void clearRhymeCache() {
        synchronized (rhymeCacheLock) {
            rhymeCache.clear();
            rhymeCacheVersion++;
        }
        if (rhymeEngine != null) rhymeEngine.clearCache();
    }

    private void cancelSuggestionJob() {
        synchronized (suggestionJobLock) {
            if (suggestionFuture != null && !suggestionFuture.isDone()) {
                suggestionFuture.cancel(true);
            }
            suggestionFuture = null;
        }
    }

    private boolean sameWords(ArrayList<String> a, ArrayList<String> b) {
        if (a == null || b == null || a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            if (!a.get(i).equals(b.get(i))) return false;
        }
        return true;
    }

    private void renderSuggestionChips(LinearLayout chips, ArrayList<String> words) {
        if (chips == null) return;
        chips.removeAllViews();
        for (String word : words) {
            Button chip = button(word);
            styleRhymeChip(chip, current != null ? current.accentColor : C_CYAN);
            chip.setOnClickListener(v -> applySuggestion(word));
            chip.setOnLongClickListener(v -> {
                promptRemoveSuggestion(word);
                return true;
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
            lp.rightMargin = dimen(R.dimen.topflow_space_xs);
            chips.addView(chip, lp);
        }
    }

    private void renderSuggestionStatus(LinearLayout chips, String text) {
        if (chips == null) return;
        chips.removeAllViews();
        Button chip = button(text);
        styleRhymeChip(chip, color(R.color.topflow_accent_gold));
        chip.setEnabled(false);
        chip.setAlpha(0.78f);
        chip.setOnClickListener(null);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.rightMargin = dimen(R.dimen.topflow_space_xs);
        chips.addView(chip, lp);
    }

    private void promptRemoveSuggestion(String word) {
        new AlertDialog.Builder(this)
                .setTitle("Remove rhyme?")
                .setMessage("Remove \"" + word + "\" from suggestions permanently?")
                .setPositiveButton("Remove", (d, w) -> {
                    rememberRemovedSuggestion(word);
                    saveRemovedSuggestions();
                    scheduleSuggestionUpdate();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void dismissSuggestionPopup() {
        if (suggestionPopup != null && suggestionPopup.isShowing()) {
            suggestionPopup.dismiss();
        }
        lastPopupLeft = Integer.MIN_VALUE;
        lastPopupTop = Integer.MIN_VALUE;
    }

    private void applySuggestion(String word) {
        if (bodyInput == null || current == null) return;
        Editable text = bodyInput.getText();
        int start = suggestionStart >= 0 ? suggestionStart : Math.max(0, bodyInput.getSelectionStart());
        int end = suggestionEnd >= start ? suggestionEnd : Math.max(start, bodyInput.getSelectionStart());
        if (start > text.length()) start = text.length();
        if (end > text.length()) end = text.length();
        if (start > end) { int t = start; start = end; end = t; }
        text.replace(start, end, word + " ");
        bodyInput.setSelection(Math.min(text.length(), start + word.length() + 1));
        current.body = text.toString();
        saveNotes();
        scheduleSuggestionUpdate();
    }

    private TokenInfo currentToken(CharSequence text, int cursor) {
        if (text == null) text = "";
        cursor = Math.max(0, Math.min(cursor, text.length()));
        int start = cursor;
        while (start > 0 && isTokenChar(text.charAt(start - 1))) start--;
        int end = cursor;
        while (end < text.length() && isTokenChar(text.charAt(end))) end++;
        String token = text.subSequence(start, end).toString();
        return new TokenInfo(start, end, token, token.substring(0, Math.min(token.length(), cursor - start)));
    }

    private TokenInfo previousToken(CharSequence text, int cursor) {
        if (text == null) text = "";
        cursor = Math.max(0, Math.min(cursor, text.length()));
        int end = cursor;
        while (end > 0 && !isTokenChar(text.charAt(end - 1))) end--;
        int start = end;
        while (start > 0 && isTokenChar(text.charAt(start - 1))) start--;
        String token = text.subSequence(start, end).toString();
        return new TokenInfo(start, end, token, token);
    }

    private boolean isTokenChar(char c) {
        return Character.isLetter(c) || c == '\'' || c == '-';
    }

    private ArrayList<String> suggestRhymes(String word, int limit) {
        return suggestRhymes(word, limit, 0);
    }

    private ArrayList<String> suggestRhymes(String word, int limit, int maxCandidates) {
        if (rhymeEngine == null) return new ArrayList<>();
        return rhymeEngine.suggest(word, limit, maxCandidates, rhymeOptions());
    }

    private ArrayList<String> quickFallbackRhymes(String base, int limit) {
        ArrayList<String> out = new ArrayList<>();
        String key = rhymeKey(base);
        for (String word : COMMON_RHYME_WORDS) {
            String w = normalizeWord(word);
            if (w.isEmpty() || w.equals(base)) continue;
            if (isSuggestionRemoved(word)) continue;
            if (!fallbackTailCompatible(base, w, key)) continue;
            if (!out.contains(w)) out.add(w);
            if (out.size() >= limit) break;
        }
        return out;
    }

    private boolean fallbackTailCompatible(String base, String candidate, String baseKey) {
        String candidateKey = rhymeKey(candidate);
        String baseCmu = cmuRhymeTail(base);
        String candidateCmu = cmuRhymeTail(candidate);
        if (!baseCmu.isEmpty() && !candidateCmu.isEmpty()) return baseCmu.equals(candidateCmu);
        return smallTailDistance(candidateKey, baseKey) <= 1;
    }

    private int rhymeScore(String base, String candidate, int bucket) {
        String c = candidateRhymeWord(candidate);
        if (c.isEmpty() || c.equals(base)) return 0;
        boolean exactOnly = prefs.getBoolean(PREF_EXACT_ONLY, false);
        ArrayList<PhoneRhymeInfo> baseInfos = cmuRhymeInfos(base);
        ArrayList<PhoneRhymeInfo> candidateInfos = cmuRhymeInfos(c);
        if (!baseInfos.isEmpty() && !candidateInfos.isEmpty()) {
            int score = bestCmuRhymeScore(base, c, baseInfos, candidateInfos, bucket);
            return score >= scoreThreshold() ? score : 0;
        }
        if (exactOnly) return 0;
        if (!baseInfos.isEmpty() || !candidateInfos.isEmpty()) {
            if (!nearSlangFamily(base, c)) return 0;
            int score = 116 + internalRhymeBias(base, c) - bucketPenalty(bucket);
            return score >= scoreThreshold() ? score : 0;
        }
        String key = rhymeKey(base);
        String ck = rhymeKey(c);
        String baseFamily = phonemeFamily(base);
        String candidateFamily = phonemeFamily(c);
        if (!baseFamily.equals(candidateFamily) && !nearSlangFamily(base, c)) return 0;
        int score = 0;
        if (ck.equals(key)) score += 120;
        if (baseFamily.equals(candidateFamily)) score += 38;
        if (nearSlangFamily(base, c)) score += 24;
        score += commonRhymeBias(candidate);
        score += internalRhymeBias(base, c);
        score -= bucketPenalty(bucket);
        score -= Math.abs(c.length() - base.length()) / 2;
        return score >= scoreThreshold() ? Math.max(score, 0) : 0;
    }

    private int commonRhymeBias(String candidate) {
        String w = candidateRhymeWord(candidate);
        if ("about".equals(w)) return 101;
        for (int i = 0; i < COMMON_RHYME_WORDS.length; i++) {
            if (normalizeWord(COMMON_RHYME_WORDS[i]).equals(w)) {
                return Math.max(48, 124 - i);
            }
        }
        return 0;
    }

    private int bestCmuRhymeScore(String base, String candidate, ArrayList<PhoneRhymeInfo> baseInfos, ArrayList<PhoneRhymeInfo> candidateInfos, int bucket) {
        int best = 0;
        boolean exactOnly = prefs.getBoolean(PREF_EXACT_ONLY, false);
        for (PhoneRhymeInfo a : baseInfos) {
            for (PhoneRhymeInfo b : candidateInfos) {
                if (!a.vowelKey.equals(b.vowelKey)) continue;
                boolean exact = a.rhymeKey.equals(b.rhymeKey);
                if (exactOnly && !exact) continue;
                int overlap = phoneTailOverlap(a.rhymeKey, b.rhymeKey);
                boolean slangCompatible = nearSlangFamily(base, candidate);
                boolean codaCompatible = exact || !a.codaKey.isEmpty() && a.codaKey.equals(b.codaKey) || overlap >= 2 || slangCompatible;
                if (!codaCompatible) continue;
                int syllableDiff = Math.abs(a.syllableCount - b.syllableCount);
                if (!exact && syllableDiff > 2) continue;
                int score = 0;
                score += exact ? 260 : 122;
                score += overlap * 34;
                if (a.codaKey.equals(b.codaKey)) score += 74;
                else if (sameFinalPhone(a.codaKey, b.codaKey)) score += 28;
                if (syllableDiff == 0) score += 28;
                else score -= syllableDiff * 12;
                if (slangCompatible) score += 30;
                if (slangVariantPair(base, candidate)) score += 360;
                score += commonRhymeBias(candidate);
                score += internalRhymeBias(base, candidate);
                score -= bucketPenalty(bucket);
                best = Math.max(best, score);
            }
        }
        return best;
    }

    private boolean slangVariantPair(String a, String b) {
        String x = slangVariantKey(a);
        String y = slangVariantKey(b);
        return !x.isEmpty() && x.equals(y) && !normalizeWord(a).equals(normalizeWord(b));
    }

    private String slangVariantKey(String word) {
        String w = normalizeWord(word);
        if (w.endsWith("ing") && w.length() > 4) return w.substring(0, w.length() - 3) + "in";
        if (w.endsWith("in") && w.length() > 3) return w;
        return "";
    }

    private int bucketPenalty(int bucket) {
        if (bucket == RHYME_BUCKET_EXACT) return 0;
        if (bucket == RHYME_BUCKET_NEAR) return 12;
        if (bucket == RHYME_BUCKET_SLANT) return 52;
        if (bucket == RHYME_BUCKET_PHRASE) return 90;
        return 130;
    }

    private ArrayList<RhymeCandidate> candidatePoolFor(String base) {
        HashMap<String, Integer> pool = new HashMap<>();
        ArrayList<PhoneRhymeInfo> baseInfos = cmuRhymeInfos(base);
        if (!baseInfos.isEmpty()) {
            for (PhoneRhymeInfo info : baseInfos) {
                ArrayList<String> exact = cmuRhymeIndex.get(info.rhymeKey);
                if (exact != null) {
                    for (String word : exact) addCandidate(pool, word, RHYME_BUCKET_EXACT);
                }
                ArrayList<String> family = cmuFamilyIndex.get(info.familyKey);
                if (family != null) {
                    for (String word : family) addCandidate(pool, word, RHYME_BUCKET_NEAR);
                }
            }
            for (String word : COMMON_RHYME_WORDS) {
                String w = normalizeWord(word);
                if (w.isEmpty() || w.equals(base)) continue;
                int relation = cmuRelation(baseInfos, cmuRhymeInfos(w));
                if (relation >= 0) addCandidate(pool, word, relation);
                else if (nearSlangFamily(base, w)) addCandidate(pool, word, RHYME_BUCKET_SLANT);
            }
            for (String phrase : COMMON_RHYME_PHRASES) {
                String w = candidateRhymeWord(phrase);
                if (w.isEmpty() || w.equals(base)) continue;
                int relation = cmuRelation(baseInfos, cmuRhymeInfos(w));
                if (relation == RHYME_BUCKET_EXACT || relation == RHYME_BUCKET_NEAR || nearSlangFamily(base, w)) addCandidate(pool, phrase, RHYME_BUCKET_PHRASE);
            }
        } else {
            String baseKey = rhymeKey(base);
            String baseFamily = phonemeFamily(base);
            for (String word : COMMON_RHYME_WORDS) {
                String w = normalizeWord(word);
                if (w.isEmpty() || w.equals(base)) continue;
                String k = rhymeKey(w);
                if (k.equals(baseKey) || phonemeFamily(w).equals(baseFamily) || nearSlangFamily(base, w)) {
                    addCandidate(pool, word, RHYME_BUCKET_FALLBACK);
                }
            }
            for (String phrase : COMMON_RHYME_PHRASES) {
                String w = candidateRhymeWord(phrase);
                if (w.isEmpty() || w.equals(base)) continue;
                String k = rhymeKey(w);
                if (k.equals(baseKey) || phonemeFamily(w).equals(baseFamily) || nearSlangFamily(base, w)) {
                    addCandidate(pool, phrase, RHYME_BUCKET_PHRASE);
                }
            }
        }
        ArrayList<RhymeCandidate> out = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : pool.entrySet()) {
            out.add(new RhymeCandidate(entry.getKey(), entry.getValue()));
        }
        return out;
    }

    private void addCandidate(HashMap<String, Integer> pool, String word, int bucket) {
        if (word == null || word.isEmpty()) return;
        Integer existing = pool.get(word);
        if (existing == null || bucket < existing) pool.put(word, bucket);
    }

    private int cmuRelation(ArrayList<PhoneRhymeInfo> baseInfos, ArrayList<PhoneRhymeInfo> candidateInfos) {
        if (baseInfos.isEmpty() || candidateInfos.isEmpty()) return -1;
        int best = -1;
        for (PhoneRhymeInfo a : baseInfos) {
            for (PhoneRhymeInfo b : candidateInfos) {
                if (!a.vowelKey.equals(b.vowelKey)) continue;
                if (a.rhymeKey.equals(b.rhymeKey)) return RHYME_BUCKET_EXACT;
                int syllableDiff = Math.abs(a.syllableCount - b.syllableCount);
                int overlap = phoneTailOverlap(a.rhymeKey, b.rhymeKey);
                if (a.codaKey.equals(b.codaKey) || overlap >= 2) {
                    if (syllableDiff <= 2) best = RHYME_BUCKET_NEAR;
                } else if (sameFinalPhone(a.codaKey, b.codaKey) && syllableDiff <= 1 && best < 0) {
                    best = RHYME_BUCKET_SLANT;
                }
            }
        }
        return best;
    }

    private String candidateRhymeWord(String candidate) {
        if (candidate == null) return "";
        String[] parts = candidate.toLowerCase(Locale.US).split("[^a-z']+");
        for (int i = parts.length - 1; i >= 0; i--) {
            String w = normalizeWord(parts[i]);
            if (!w.isEmpty()) return w;
        }
        return normalizeWord(candidate);
    }

    private void rememberRemovedSuggestion(String suggestion) {
        String normalized = normalizeWord(suggestion);
        if (!normalized.isEmpty()) removedSuggestions.add(normalized);
    }

    private boolean isSuggestionRemoved(String suggestion) {
        String normalized = normalizeWord(suggestion);
        if (!normalized.isEmpty() && removedSuggestions.contains(normalized)) return true;
        String rhymeWord = candidateRhymeWord(suggestion);
        return !rhymeWord.isEmpty() && removedSuggestions.contains(rhymeWord);
    }

    private int configuredMaxSuggestions() {
        return Math.max(4, Math.min(12, prefs.getInt(PREF_MAX_SUGGESTIONS, 6)));
    }

    private String strictnessName() {
        return prefs.getString(PREF_RHYME_STRICTNESS, "Balanced");
    }

    private RhymeEngine.Options rhymeOptions() {
        return new RhymeEngine.Options(
                strictnessName(),
                prefs.getBoolean(PREF_EXACT_ONLY, false),
                prefs.getBoolean(PREF_INCLUDE_SLANG, true),
                new HashSet<>(removedSuggestions),
                current == null || current.body == null ? "" : current.body
        );
    }

    private int strictnessIndex() {
        String s = strictnessName();
        if ("Strict".equals(s)) return 0;
        if ("Loose".equals(s)) return 2;
        return 1;
    }

    private int scoreThreshold() {
        String s = strictnessName();
        if ("Strict".equals(s)) return 178;
        if ("Loose".equals(s)) return 76;
        return 112;
    }

    private int internalRhymeBias(String base, String candidate) {
        if (current == null || current.body == null) return 0;
        String family = phonemeFamily(candidate);
        if (family.isEmpty()) return 0;
        String[] parts = current.body.toLowerCase(Locale.US).split("[^a-z']+");
        int hits = 0;
        for (String part : parts) {
            String w = normalizeWord(part);
            if (w.isEmpty() || w.equals(base) || w.equals(candidate)) continue;
            if (phonemeFamily(w).equals(family)) hits++;
            if (hits >= 2) break;
        }
        return hits * 8;
    }

    private String rhymeKey(String word) {
        return pronunciationTail(normalizeWord(word));
    }

    private String tailKey(String word) {
        String w = normalizeWord(word);
        return w.length() <= 3 ? w : w.substring(w.length() - 3);
    }

    private int smallTailDistance(String a, String b) {
        String x = a == null ? "" : a;
        String y = b == null ? "" : b;
        int max = Math.max(x.length(), y.length());
        if (max == 0) return 0;
        int min = Math.min(x.length(), y.length());
        int diff = Math.abs(x.length() - y.length());
        for (int i = 0; i < min; i++) {
            if (x.charAt(x.length() - 1 - i) != y.charAt(y.length() - 1 - i)) diff++;
            if (diff > 2) break;
        }
        return diff;
    }

    private int phoneTailOverlap(String a, String b) {
        String[] x = splitPhones(a);
        String[] y = splitPhones(b);
        int overlap = 0;
        while (overlap < x.length && overlap < y.length
                && x[x.length - 1 - overlap].equals(y[y.length - 1 - overlap])) {
            overlap++;
        }
        return overlap;
    }

    private String[] splitPhones(String phones) {
        if (phones == null || phones.trim().isEmpty()) return new String[0];
        return phones.trim().split("\\s+");
    }

    private boolean sameFinalPhone(String a, String b) {
        String[] x = splitPhones(a);
        String[] y = splitPhones(b);
        return x.length > 0 && y.length > 0 && x[x.length - 1].equals(y[y.length - 1]);
    }

    private int suffixOverlapScore(String a, String b) {
        String x = normalizeWord(a);
        String y = normalizeWord(b);
        int max = Math.min(5, Math.min(x.length(), y.length()));
        int score = 0;
        for (int i = 1; i <= max; i++) {
            if (x.charAt(x.length() - i) == y.charAt(y.length() - i)) score++;
            else break;
        }
        return score;
    }

    private int vowelPatternScore(String a, String b) {
        String x = vowelPattern(normalizeWord(a));
        String y = vowelPattern(normalizeWord(b));
        int max = Math.min(x.length(), y.length());
        int same = 0;
        for (int i = 0; i < max; i++) {
            if (x.charAt(i) == y.charAt(i)) same++;
        }
        return same;
    }

    private String vowelPattern(String word) {
        StringBuilder out = new StringBuilder();
        boolean prevVowel = false;
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            boolean vowel = isVowel(c);
            if (vowel && !prevVowel) out.append(c);
            prevVowel = vowel;
        }
        return out.toString();
    }

    private String endingConsonantCluster(String word) {
        String w = normalizeWord(word);
        int i = w.length() - 1;
        while (i >= 0 && isVowel(w.charAt(i))) i--;
        int end = i;
        while (i >= 0 && !isVowel(w.charAt(i))) i--;
        if (end < 0) return "";
        return w.substring(Math.max(0, i + 1), end + 1);
    }

    private String lastVowelSound(String word) {
        String w = normalizeWord(word);
        if (w.endsWith("e") && w.length() > 3) w = w.substring(0, w.length() - 1);
        for (int i = w.length() - 1; i >= 0; i--) {
            if (isVowel(w.charAt(i))) return w.substring(i, Math.min(w.length(), i + 3));
        }
        return tailKey(w);
    }

    private boolean sameApproxEnding(String a, String b) {
        String x = normalizeWord(a);
        String y = normalizeWord(b);
        if (x.isEmpty() || y.isEmpty()) return false;
        String xa = x.length() <= 4 ? x : x.substring(x.length() - 4);
        String ya = y.length() <= 4 ? y : y.substring(y.length() - 4);
        return xa.equals(ya) || suffixOverlapScore(x, y) >= 2;
    }

    private void startCmuLoad() {
        if (rhymeEngine == null) return;
        rhymeEngine.loadAsync(() -> editHandler.post(() -> {
            clearRhymeCache();
            scheduleSuggestionUpdate();
        }));
    }

    private void loadCmuDictionary() {
        if (cmuLoaded) return;
        cmuPhones.clear();
        cmuRhymeIndex.clear();
        cmuFamilyIndex.clear();
        cmuDictionaryWords.clear();
        try (InputStream raw = getAssets().open("cmudict.dict");
             BufferedReader reader = new BufferedReader(new InputStreamReader(raw, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty() || line.startsWith(";;;")) continue;
                String[] parts = line.trim().split("\\s+", 2);
                if (parts.length != 2) continue;
                String word = normalizeCmuWord(parts[0]);
                if (word.isEmpty() || word.length() > 18) continue;
                String phones = parts[1].trim();
                if (phones.isEmpty()) continue;
                addCmuEntry(word, phones);
                cmuDictionaryWords.add(word);
            }
        } catch (Exception ignored) {
        }
        addPronunciationOverrides();
        for (String word : COMMON_RHYME_WORDS) {
            String w = normalizeWord(word);
            String phones = slangPhones(w);
            if (!w.isEmpty() && !phones.isEmpty() && !cmuDictionaryWords.contains(w)) addCmuEntry(w, phones);
        }
    }

    private void addPronunciationOverrides() {
        addCmuEntry("your", "Y AO1 R");
        addCmuEntry("yours", "Y AO1 R Z");
    }

    private void addCmuEntry(String word, String phones) {
        ArrayList<String> list = cmuPhones.get(word);
        if (list == null) {
            list = new ArrayList<>();
            cmuPhones.put(word, list);
        }
        if (!list.contains(phones)) list.add(phones);
        String rhyme = cmuRhymePartFromPhones(phones);
        if (!rhyme.isEmpty()) addIndexedWord(cmuRhymeIndex, rhymeKeyFromPhones(rhyme), word);
        String family = phonemeFamilyFromPhones(phones);
        if (!family.isEmpty()) addIndexedWord(cmuFamilyIndex, family, word);
    }

    private void addIndexedWord(Map<String, ArrayList<String>> index, String key, String word) {
        if (key == null || key.isEmpty()) return;
        ArrayList<String> words = index.get(key);
        if (words == null) {
            words = new ArrayList<>();
            index.put(key, words);
        }
        if (words.size() < 240 && !words.contains(word)) words.add(word);
    }

    private String normalizeCmuWord(String word) {
        if (word == null) return "";
        int variant = word.indexOf('(');
        if (variant >= 0) word = word.substring(0, variant);
        if (!word.matches("[A-Za-z']+")) return "";
        return normalizeWord(word);
    }

    private ArrayList<String> phonesForWord(String word) {
        String w = normalizeWord(word);
        ArrayList<String> out = new ArrayList<>();
        ArrayList<String> phones = cmuPhones.get(w);
        if (phones != null) {
            for (String p : phones) if (!out.contains(p)) out.add(p);
        }
        String slang = slangPhones(w);
        if (!slang.isEmpty() && !cmuDictionaryWords.contains(w) && !out.contains(slang)) out.add(slang);
        return out;
    }

    private ArrayList<PhoneRhymeInfo> cmuRhymeInfos(String word) {
        String w = normalizeWord(word);
        ArrayList<PhoneRhymeInfo> out = new ArrayList<>();
        ArrayList<String> phones = cmuPhones.get(w);
        if (phones != null) {
            for (String p : phones) {
                PhoneRhymeInfo info = phoneRhymeInfo(p);
                if (info != null) out.add(info);
            }
        }
        String guessedIng = guessedIngPhones(w);
        if (!guessedIng.isEmpty() && !cmuDictionaryWords.contains(w)) {
            PhoneRhymeInfo info = phoneRhymeInfo(guessedIng);
            if (info != null) out.add(info);
        }
        String slang = slangPhones(w);
        if (!slang.isEmpty() && !cmuDictionaryWords.contains(w)) {
            PhoneRhymeInfo info = phoneRhymeInfo(slang);
            if (info != null) out.add(info);
        }
        return out;
    }

    private PhoneRhymeInfo phoneRhymeInfo(String phones) {
        if (phones == null || phones.trim().isEmpty()) return null;
        String rhyme = cmuRhymePartFromPhones(phones);
        if (rhyme.isEmpty()) return null;
        String rhymeKey = rhymeKeyFromPhones(rhyme);
        String[] parts = rhyme.split("\\s+");
        String vowel = "";
        StringBuilder coda = new StringBuilder();
        for (String part : parts) {
            String p = part.replaceAll("[012]", "");
            if (vowel.isEmpty() && isVowelPhone(part)) {
                vowel = p;
            } else if (!vowel.isEmpty() && !isVowelPhone(part)) {
                if (coda.length() > 0) coda.append(' ');
                coda.append(p);
            }
        }
        int syllables = 0;
        for (String part : phones.trim().split("\\s+")) {
            if (isVowelPhone(part)) syllables++;
        }
        if (vowel.isEmpty()) return null;
        String codaKey = coda.toString();
        return new PhoneRhymeInfo(rhymeKey, vowel, codaKey, vowel + ":" + codaKey, Math.max(1, syllables));
    }

    private String slangPhones(String word) {
        String w = normalizeWord(word);
        if (w.isEmpty() || (prefs != null && !prefs.getBoolean(PREF_INCLUDE_SLANG, true))) return "";
        if (w.endsWith("in'")) w = w.substring(0, w.length() - 1);
        if (w.endsWith("ing") && w.length() > 4) return "P UH1 L IH0 N".replace("P UH1 L", guessedOnsetPhones(w.substring(0, w.length() - 3)));
        if (w.endsWith("in") && w.length() > 3) return guessedOnsetPhones(w.substring(0, w.length() - 2)) + " IH0 N";
        if ((w.endsWith("ant") || w.endsWith("ent") || w.endsWith("int") || w.endsWith("unt")) && w.length() > 5) {
            return guessedOnsetPhones(w.substring(0, w.length() - 3)) + " IH0 N";
        }
        if ("coolant".equals(w)) return "K UW1 L IH0 N";
        if ("pullin".equals(w)) return "P UH1 L IH0 N";
        return "";
    }

    private String guessedOnsetPhones(String stem) {
        String s = normalizeWord(stem);
        if (s.endsWith("ll")) return "P UH1 L";
        if (s.endsWith("l")) return "K UW1 L";
        if (s.endsWith("v")) return "M UW1 V";
        return "AH1";
    }

    private String guessedIngPhones(String word) {
        String w = normalizeWord(word);
        if (w.endsWith("ing") && w.length() > 4) return guessedOnsetPhones(w.substring(0, w.length() - 3)) + " IH0 NG";
        return "";
    }

    private String cmuRhymePartFromPhones(String phones) {
        String[] parts = phones.trim().split("\\s+");
        int stressed = -1;
        for (int i = parts.length - 1; i >= 0; i--) {
            if (isVowelPhone(parts[i]) && (parts[i].endsWith("1") || parts[i].endsWith("2"))) {
                stressed = i;
                break;
            }
        }
        if (stressed < 0) {
            for (int i = parts.length - 1; i >= 0; i--) {
                if (isVowelPhone(parts[i])) {
                    stressed = i;
                    break;
                }
            }
        }
        if (stressed < 0) return "";
        StringBuilder out = new StringBuilder();
        for (int i = stressed; i < parts.length; i++) {
            if (out.length() > 0) out.append(' ');
            out.append(parts[i]);
        }
        return out.toString();
    }

    private String rhymeKeyFromPhones(String phones) {
        return phones == null ? "" : phones.replaceAll("[012]", "").trim();
    }

    private String phonemeFamily(String word) {
        ArrayList<String> phones = phonesForWord(word);
        if (!phones.isEmpty()) return phonemeFamilyFromPhones(phones.get(0));
        return rhymeFamily(word);
    }

    private String phonemeFamilyFromPhones(String phones) {
        PhoneRhymeInfo info = phoneRhymeInfo(phones);
        return info == null ? "" : info.familyKey;
    }

    private int trailingPhoneOverlap(String base, String candidate) {
        ArrayList<String> a = phonesForWord(base);
        ArrayList<String> b = phonesForWord(candidate);
        int best = 0;
        for (String ap : a) {
            String[] at = rhymeKeyFromPhones(cmuRhymePartFromPhones(ap)).split("\\s+");
            for (String bp : b) {
                String[] bt = rhymeKeyFromPhones(cmuRhymePartFromPhones(bp)).split("\\s+");
                int overlap = 0;
                while (overlap < at.length && overlap < bt.length
                        && at[at.length - 1 - overlap].equals(bt[bt.length - 1 - overlap])) {
                    overlap++;
                }
                best = Math.max(best, overlap);
            }
        }
        return best;
    }

    private boolean nearSlangFamily(String a, String b) {
        if (prefs != null && !prefs.getBoolean(PREF_INCLUDE_SLANG, true)) return false;
        String x = hipHopNearTail(a);
        String y = hipHopNearTail(b);
        return !x.isEmpty() && x.equals(y);
    }

    private String hipHopNearTail(String word) {
        String w = normalizeWord(word);
        if (w.endsWith("ing") && w.length() > 4) w = w.substring(0, w.length() - 3) + "in";
        if ((w.endsWith("ant") || w.endsWith("ent") || w.endsWith("int") || w.endsWith("unt")) && w.length() > 5) {
            w = w.substring(0, w.length() - 3) + "in";
        }
        if (w.endsWith("in") || w.endsWith("en")) {
            int idx = Math.max(w.lastIndexOf('l'), Math.max(w.lastIndexOf('m'), w.lastIndexOf('v')));
            if (idx >= 0) return w.substring(idx, Math.min(w.length(), idx + 3));
            return "in";
        }
        return "";
    }

    private boolean isVowelPhone(String phone) {
        String p = phone == null ? "" : phone.replaceAll("[012]", "");
        return p.equals("AA") || p.equals("AE") || p.equals("AH") || p.equals("AO")
                || p.equals("AW") || p.equals("AY") || p.equals("EH") || p.equals("ER")
                || p.equals("EY") || p.equals("IH") || p.equals("IY") || p.equals("OW")
                || p.equals("OY") || p.equals("UH") || p.equals("UW");
    }

    private String rhymeFamily(String word) {
        String sound = pronunciationTail(word);
        if (sound.isEmpty()) return "";
        sound = sound
                .replace("eigh", "ay")
                .replace("igh", "y")
                .replace("oy", "oi")
                .replace("ee", "i")
                .replace("ea", "i")
                .replace("oa", "o")
                .replace("oo", "u")
                .replace("ou", "ow");
        if (sound.startsWith("ai") || sound.startsWith("ay") || sound.startsWith("ei") || sound.startsWith("ey")) return "A" + endingConsonantCluster(sound);
        if (sound.startsWith("oi")) return "OI" + endingConsonantCluster(sound);
        if (sound.startsWith("ow")) return "OW" + endingConsonantCluster(sound);
        if (sound.startsWith("i")) return "E" + endingConsonantCluster(sound);
        if (sound.startsWith("u")) return "U" + endingConsonantCluster(sound);
        if (sound.startsWith("o")) return "O" + endingConsonantCluster(sound);
        if (sound.startsWith("a")) return "AH" + endingConsonantCluster(sound);
        return sound.length() <= 4 ? sound : sound.substring(0, 4);
    }

    private String pronunciationTail(String word) {
        String w = normalizeWord(word);
        if (w.isEmpty()) return "";
        String cmu = cmuRhymeTail(w);
        if (!cmu.isEmpty()) return cmu.toLowerCase(Locale.US).replace(" ", "");
        if (prefs == null || prefs.getBoolean(PREF_INCLUDE_SLANG, true)) {
            if (w.endsWith("in'")) w = w.substring(0, w.length() - 1);
            if (w.endsWith("ing") && w.length() > 4) return "in";
            if (w.endsWith("in") && w.length() > 3) return "in";
            if (w.endsWith("en") && w.length() > 3) return "in";
            if ((w.endsWith("ent") || w.endsWith("ant") || w.endsWith("int") || w.endsWith("unt")) && w.length() > 5) return "in";
            if (w.endsWith("tion") || w.endsWith("sion")) return "shun";
            if (w.endsWith("er") || w.endsWith("or") || w.endsWith("ar")) return "ur";
        }
        return phoneticTail(w);
    }

    private String cmuRhymeTail(String word) {
        String w = normalizeWord(word);
        ArrayList<String> phones = phonesForWord(w);
        if (!phones.isEmpty()) {
            String rhyme = cmuRhymePartFromPhones(phones.get(0));
            if (!rhyme.isEmpty()) return rhymeKeyFromPhones(rhyme);
        }
        switch (w) {
            case "my": case "try": case "fly": case "sky": case "high": case "why": case "lie": case "cry": case "buy": case "eye":
                return "AY";
            case "day": case "way": case "play": case "say": case "stay": case "gray": case "spray": case "sway": case "delay":
                return "EY";
            case "made": case "fade": case "shade":
                return "EY D";
            case "pain": case "rain": case "chain": case "brain": case "gain": case "train": case "plane": case "strain":
                return "EY N";
            case "time": case "rhyme": case "dime": case "climb": case "lime": case "crime":
                return "AY M";
            case "mine": case "shine": case "line": case "fine": case "sign": case "design":
                return "AY N";
            case "night": case "light": case "flight": case "bright": case "might": case "tight": case "right": case "sight":
                return "AY T";
            case "flow": case "go": case "show": case "glow": case "throw": case "slow": case "grow": case "blow":
                return "OW";
            case "cool": case "jewel": case "rule": case "stool": case "drool": case "wool": case "mule":
                return "UW L";
            case "pullin": case "pulling": case "woolen":
                return "UH L IH N";
            case "coolant":
                return "UW L AH N T";
            case "voice": case "choice": case "noise": case "poise":
                return "OY S";
            case "toy": case "joy": case "ploy": case "boy": case "boys":
                return "OY";
            case "yours": case "soars": case "pores": case "doors": case "floors":
                return "AO R Z";
            case "your": case "soar": case "pore": case "door": case "floor":
                return "AO R";
            case "out": case "bout": case "clout": case "shout": case "doubt": case "about": case "route":
            case "scout": case "spout": case "sprout": case "trout": case "pout": case "stout":
                return "AW T";
            default:
                return "";
        }
    }

    private String phoneticTail(String word) {
        String w = normalizeWord(word);
        if (w.isEmpty()) return "";
        w = w.replace("ph", "f").replace("gh", "").replace("ck", "k");
        w = w.replace("qu", "kw").replace("x", "ks");
        w = w.replace("ch", "ch").replace("sh", "sh").replace("th", "th");
        if (w.endsWith("e") && w.length() > 3) w = w.substring(0, w.length() - 1);
        StringBuilder out = new StringBuilder();
        int vowel = -1;
        for (int i = w.length() - 1; i >= 0; i--) {
            if (isVowel(w.charAt(i))) {
                vowel = i;
                break;
            }
        }
        if (vowel < 0) return tailKey(w);
        out.append(w.substring(vowel));
        return out.toString();
    }

    private static final String[] COMMON_RHYME_WORDS = {
            "my", "try", "fly", "sky", "high", "why", "lie", "cry", "buy", "eye",
            "time", "rhyme", "dime", "climb", "lime", "mine", "shine", "line", "fine", "crime",
            "grind", "blind", "kind", "mind", "wind", "sign", "design", "behind",
            "night", "light", "flight", "bright", "might", "tight", "right", "sight",
            "flow", "go", "show", "glow", "throw", "slow", "grow", "blow",
            "way", "play", "say", "stay", "day", "made", "fade", "shade", "gray", "spray", "sway", "delay",
            "out", "bout", "clout", "shout", "doubt", "about", "route", "scout", "spout", "sprout", "trout", "pout", "stout",
            "yours", "soars", "pores", "doors", "floors", "your", "soar", "pore", "door", "floor",
            "heart", "start", "part", "smart", "chart", "art", "hard", "yard",
            "pain", "rain", "chain", "brain", "gain", "train", "plane", "strain",
            "soul", "cold", "gold", "roll", "control", "whole", "goal", "bowl",
            "voice", "choice", "noise", "poise", "boys", "toy", "joy", "ploy",
            "near", "clear", "fear", "dear", "here", "year", "peer", "tear",
            "heat", "beat", "street", "sweet", "fleet", "meet", "seat", "feat",
            "cover", "lover", "hover", "running", "runnin", "proving", "grooving",
            "pullin", "pulling", "coolant", "woolen", "bullet", "couldn't", "shouldn't", "wouldn't",
            "movin", "moving", "proven", "losing", "choosing", "ruin", "fluid", "student"
    };

    private static final String[] COMMON_RHYME_PHRASES = {
            "keep it movin", "fluid motion", "coolant flow", "pullin through",
            "all day", "same way", "late night", "bright lights",
            "on my mind", "in due time", "stay in line", "chain reaction"
    };

    private String normalizeWord(String word) {
        if (word == null) return "";
        String w = word.toLowerCase(Locale.US).replaceAll("[^a-z']", "");
        while (w.startsWith("'")) w = w.substring(1);
        while (w.endsWith("'")) w = w.substring(0, w.length() - 1);
        return w;
    }

    private boolean isVowel(char c) {
        return "aeiouy".indexOf(c) >= 0;
    }

    private static class TokenInfo {
        final int start;
        final int end;
        final String word;
        final String prefix;
        TokenInfo(int start, int end, String word, String prefix) {
            this.start = start;
            this.end = end;
            this.word = word;
            this.prefix = prefix;
        }
    }

    private static class RhymeMatch {
        final String word;
        final int score;
        final int bucket;
        final int priority;
        RhymeMatch(String word, int score, int bucket, int priority) {
            this.word = word;
            this.score = score;
            this.bucket = bucket;
            this.priority = priority;
        }
    }

    private static class RhymeCandidate {
        final String word;
        final int bucket;
        RhymeCandidate(String word, int bucket) {
            this.word = word;
            this.bucket = bucket;
        }
    }

    private static class PhoneRhymeInfo {
        final String rhymeKey;
        final String vowelKey;
        final String codaKey;
        final String familyKey;
        final int syllableCount;
        PhoneRhymeInfo(String rhymeKey, String vowelKey, String codaKey, String familyKey, int syllableCount) {
            this.rhymeKey = rhymeKey;
            this.vowelKey = vowelKey;
            this.codaKey = codaKey;
            this.familyKey = familyKey;
            this.syllableCount = syllableCount;
        }
    }

    private void showColorEditor(int startColor) {
        ColorWheelView wheel = new ColorWheelView(this);
        wheel.setColor(startColor);
        TextView preview = label(" ");
        preview.setMinHeight(dp(72));
        preview.setBackgroundColor(startColor);
        TextView hex = label(String.format(Locale.US, "#%06X", 0xFFFFFF & startColor));
        SeekBar sat = new SeekBar(this);
        sat.setMax(100);
        SeekBar val = new SeekBar(this);
        val.setMax(100);
        float[] hsv = new float[3];
        Color.colorToHSV(startColor, hsv);
        sat.setProgress((int) (hsv[1] * 100f));
        val.setProgress((int) (hsv[2] * 100f));
        final boolean[] updating = {false};
        Runnable refreshColor = () -> {
            if (updating[0]) return;
            updating[0] = true;
            int refined = Color.HSVToColor(new float[]{wheel.getHue(), sat.getProgress() / 100f, val.getProgress() / 100f});
            preview.setBackgroundColor(refined);
            hex.setText(String.format(Locale.US, "#%06X", 0xFFFFFF & refined));
            wheel.setColor(refined);
            updating[0] = false;
        };
        wheel.setOnColorChangedListener(color -> refreshColor.run());
        sat.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) { refreshColor.run(); }
            public void onStartTrackingTouch(SeekBar seekBar) {}
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        val.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) { refreshColor.run(); }
            public void onStartTrackingTouch(SeekBar seekBar) {}
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(0, 0, 0, 0);
        box.addView(wheel, new LinearLayout.LayoutParams(-1, dp(260)));
        box.addView(preview, new LinearLayout.LayoutParams(-1, dp(72)));
        box.addView(hex);
        box.addView(label("Saturation"));
        box.addView(sat);
        box.addView(label("Brightness"));
        box.addView(val);
        Button apply = button("Apply");
        apply.setOnClickListener(v -> {
            int picked = Color.HSVToColor(new float[]{wheel.getHue(), sat.getProgress() / 100f, val.getProgress() / 100f});
            if (selectedColorTarget == 0) current.noteColor = picked;
            if (selectedColorTarget == 1) current.textColor = picked;
            if (selectedColorTarget == 2) current.accentColor = picked;
            saveNotes();
            applyStyle();
            renderNoteList();
            dismissSheet();
        });
        box.addView(apply);
        showSheet("Color", box);
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
            stopRecordingPlayback();
            if (songPlayer != null && songPlayer.isPlaying()) {
                songResumePositionMs = songPlayer.getCurrentPosition();
                songPlayer.pause();
                updatePlaybackStatus();
                updateMediaLabels();
                return;
            }
            if (songPlayer == null) {
                songPlayer = MediaPlayer.create(this, Uri.parse(current.songUri));
                songPlayer.setOnCompletionListener(mp -> {
                    mp.release();
                    songPlayer = null;
                    songResumePositionMs = 0;
                    updatePlaybackStatus();
                    updateMediaLabels();
                });
                if (songResumePositionMs > 0) songPlayer.seekTo(songResumePositionMs);
            }
            songPlayer.start();
            startPlaybackTicker();
            updateMediaLabels();
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
        countdownView.setVisibility(View.VISIBLE);
        runCountdown(countdownSeconds);
    }

    private void runCountdown(int remaining) {
        if (remaining <= 0) {
            countdownView.setText("");
            beginRecording();
            return;
        }
        countdownView.setText(String.valueOf(remaining));
        if (recordingStatus != null) recordingStatus.setText("Starting in " + remaining + "...");
        countdownView.postDelayed(() -> runCountdown(remaining - 1), 1000);
    }

    private void beginRecording() {
        try {
            ensureSongPlaying();
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
            recordingStartedAtMs = System.currentTimeMillis();
            if (recordingStatus != null) recordingStatus.setText("Recording now");
            if (recordingStatus != null) recordingStatus.setTextColor(C_RED);
            updatePlaybackStatus();
            updateMediaLabels();
            Toast.makeText(this, "Recording.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            stopRecording(false);
            Toast.makeText(this, "Could not start recording.", Toast.LENGTH_SHORT).show();
        }
    }

    private void ensureSongPlaying() {
        if (current == null || current.songUri == null || current.songUri.isEmpty()) return;
        if (songPlayer != null && songPlayer.isPlaying()) return;
        stopRecordingPlayback();
        if (songPlayer == null) {
            songPlayer = MediaPlayer.create(this, Uri.parse(current.songUri));
            songPlayer.setOnCompletionListener(mp -> {
                mp.release();
                songPlayer = null;
                songResumePositionMs = 0;
                updatePlaybackStatus();
            });
            if (songResumePositionMs > 0) songPlayer.seekTo(songResumePositionMs);
        }
        if (songPlayer != null) {
            songPlayer.start();
            startPlaybackTicker();
        }
    }

    private void stopRecording(boolean keep) {
        boolean songWasPlaying = songPlayer != null && songPlayer.isPlaying();
        try {
            if (recorder != null) {
                recorder.stop();
                recorder.release();
            }
        } catch (Exception ignored) {
        }
        recorder = null;
        recordingStartedAtMs = 0L;
        if (keep && activeRecording != null && current != null) {
            current.recordings.add(0, new RecordingTag(activeRecording.getAbsolutePath(), activeRecording.getName()));
            insertRecordingMarker(activeRecording.getName());
            saveNotes();
            renderRecordings();
            Toast.makeText(this, "Recording saved.", Toast.LENGTH_SHORT).show();
        }
        activeRecording = null;
        countdownView.setVisibility(View.GONE);
        if (recordingStatus != null) recordingStatus.setText("Ready to record");
        if (recordingStatus != null) recordingStatus.setTextColor(C_TEXT_MUTED);
        countdownView.setText("");
        if (songWasPlaying) stopSongPlayback();
        updatePlaybackStatus();
        updateMediaLabels();
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
        box.setPadding(0, 0, 0, 0);
        TextView desc = label("Notes");
        desc.setTextColor(C_TEXT_MUTED);
        box.addView(desc);
        Button notes = button("Notes");
        notes.setOnClickListener(v -> {
            dismissSheet();
            showMenuScreen();
        });
        box.addView(notes);
        Button noteStyle = button("Note Style");
        noteStyle.setOnClickListener(v -> showStyleMenu());
        box.addView(noteStyle);
        Button expandedRhymes = button("Expanded Rhymes");
        expandedRhymes.setOnClickListener(v -> showExpandedRhymes());
        box.addView(expandedRhymes);
        Button rhymeSettings = button("Rhyme Settings");
        rhymeSettings.setOnClickListener(v -> showRhymeSettingsMenu());
        box.addView(rhymeSettings);
        Button deleted = button("Deleted Rhymes");
        deleted.setOnClickListener(v -> showDeletedRhymesMenu());
        box.addView(deleted);
        Button updates = button("Check for updates");
        updates.setOnClickListener(v -> checkForUpdates(true));
        box.addView(updates);
        TextView updateInfo = label("Installed v" + BuildConfig.VERSION_NAME + ". Updates use the online appcast.");
        updateInfo.setTextColor(C_TEXT_MUTED);
        updateInfo.setTextSize(13);
        box.addView(updateInfo);
        Button close = button("Close");
        close.setOnClickListener(v -> dismissSheet());
        box.addView(close);
        showSheet("Main Menu", box);
    }

    private void showStyleMenu() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        String[] names = {"Note Color", "Text Color", "Accent Color", "Font"};
        for (int i = 0; i < names.length; i++) {
            final int target = i;
            Button b = button(names[i]);
            b.setOnClickListener(v -> {
                if (current == null) return;
                if (target == 3) {
                    dismissSheet();
                    showFontMenu();
                    return;
                }
                selectedColorTarget = target;
                int c = target == 0 ? current.noteColor : target == 1 ? current.textColor : current.accentColor;
                showColorEditor(c);
            });
            box.addView(b);
        }
        Button close = button("Close");
        close.setOnClickListener(v -> dismissSheet());
        box.addView(close);
        showSheet("Note Style", box);
    }

    private void showFontMenu() {
        String[] fonts = TopFlowUiKit.fontPreviewIds();
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        for (String f : fonts) {
            View b = buildFontPreviewRow(f);
            b.setOnClickListener(v -> {
                if (current != null) {
                    current.font = f;
                    saveNotes();
                    applyStyle();
                    dismissSheet();
                }
            });
            box.addView(b);
        }
        Button close = button("Close");
        close.setOnClickListener(v -> dismissSheet());
        box.addView(close);
        showSheet("Font", box);
    }

    private View buildFontPreviewRow(String fontId) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dimen(R.dimen.topflow_space_lg), dimen(R.dimen.topflow_space_md), dimen(R.dimen.topflow_space_lg), dimen(R.dimen.topflow_space_md));
        row.setBackgroundResource(R.drawable.bg21_quiet_control);
        row.setForeground(TopFlowUiKit.ripple(TopFlowUiKit.MINT));
        row.setClickable(true);
        row.setFocusable(true);
        TopFlowUiKit.applyFloating(row, current != null && fontId.equals(current.font) ? 8 : 2);
        TextView name = new TextView(this);
        name.setText(fontLabel(fontId) + (current != null && fontId.equals(current.font) ? "  Selected" : ""));
        textStyle(name, R.style.TextAppearance_TopFlow21_Caption);
        name.setTextColor(current != null && fontId.equals(current.font) ? TopFlowUiKit.MINT : TopFlowUiKit.TEXT_SOFT);
        TextView preview = new TextView(this);
        preview.setText("whisper my name");
        textStyle(preview, R.style.TextAppearance_TopFlow21_Section);
        preview.setTypeface(TopFlowUiKit.fontForPreview(fontId));
        preview.setTextColor(TopFlowUiKit.TEXT);
        preview.setPadding(0, dimen(R.dimen.topflow_space_xs), 0, 0);
        row.addView(name);
        row.addView(preview);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = dimen(R.dimen.topflow_space_sm);
        row.setLayoutParams(lp);
        return row;
    }

    private String fontLabel(String fontId) {
        if ("serif".equals(fontId)) return "Serif";
        if ("monospace".equals(fontId)) return "Mono";
        if ("casual".equals(fontId)) return "Casual";
        if ("cursive".equals(fontId)) return "Cursive";
        return "System";
    }

    private void showDeletedRhymesMenu() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        if (removedSuggestions.isEmpty()) {
            TextView empty = label("No deleted rhyme suggestions.");
            empty.setTextColor(C_TEXT_MUTED);
            box.addView(empty);
        } else {
            ArrayList<String> words = new ArrayList<>(removedSuggestions);
            Collections.sort(words);
            for (String word : words) {
                Button restore = button("Restore " + word);
                restore.setOnClickListener(v -> {
                    removedSuggestions.remove(word);
                    saveRemovedSuggestions();
                    Toast.makeText(this, "Restored " + word, Toast.LENGTH_SHORT).show();
                    showDeletedRhymesMenu();
                    scheduleSuggestionUpdate();
                });
                box.addView(restore);
            }
        }
        Button close = button("Close");
        close.setOnClickListener(v -> dismissSheet());
        box.addView(close);
        showSheet("Deleted Rhymes", box);
    }

    private void showExpandedRhymes() {
        String word = focusedRhymeWord();
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        if (word.isEmpty()) {
            TextView empty = label("Place the cursor on a word to see expanded rhymes.");
            empty.setTextColor(C_TEXT_MUTED);
            box.addView(empty);
        } else {
            TextView title = label("For: " + word);
            title.setTextColor(C_TEXT_MUTED);
            box.addView(title);
            ArrayList<String> words = suggestRhymes(word, Math.max(12, configuredMaxSuggestions()));
            if (words.isEmpty()) {
                TextView empty = label("No rhymes found with current settings.");
                empty.setTextColor(C_TEXT_MUTED);
                box.addView(empty);
            } else {
                for (String rhyme : words) {
                    Button b = button(rhyme);
                    b.setOnClickListener(v -> {
                        applySuggestion(rhyme);
                        dismissSheet();
                    });
                    b.setOnLongClickListener(v -> {
                        promptRemoveSuggestion(rhyme);
                        return true;
                    });
                    box.addView(b);
                }
            }
        }
        Button close = button("Close");
        close.setOnClickListener(v -> dismissSheet());
        box.addView(close);
        showSheet("Expanded Rhymes", box);
    }

    private void showRhymeSettingsMenu() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        TextView status = label("Strictness: " + strictnessName() + "  Max: " + configuredMaxSuggestions());
        status.setTextColor(C_TEXT_MUTED);
        box.addView(status);
        addStrictnessSlider(box);
        addMaxSuggestionSlider(box);
        addToggleButton(box, "Show rhyme row", PREF_SHOW_RHYME_ROW, true);
        addToggleButton(box, "Show exact only", PREF_EXACT_ONLY, false);
        addToggleButton(box, "Include slang overrides", PREF_INCLUDE_SLANG, true);
        Button close = button("Close");
        close.setOnClickListener(v -> dismissSheet());
        box.addView(close);
        showSheet("Rhyme Settings", box);
    }

    private void addStrictnessSlider(LinearLayout box) {
        TextView label = label("Rhyme strictness");
        label.setTextColor(C_TEXT);
        box.addView(label);
        SeekBar bar = new SeekBar(this);
        bar.setMax(2);
        bar.setProgress(strictnessIndex());
        styleSeekBar(bar, current != null ? current.accentColor : C_CYAN);
        TextView value = label(strictnessName());
        value.setTextColor(C_TEXT_MUTED);
        box.addView(bar, new LinearLayout.LayoutParams(-1, -2));
        box.addView(value);
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser) return;
                String next = progress == 0 ? "Strict" : progress == 1 ? "Balanced" : "Loose";
                prefs.edit().putString(PREF_RHYME_STRICTNESS, next).apply();
                value.setText(next);
                clearRhymeCache();
                scheduleSuggestionUpdate();
            }
            public void onStartTrackingTouch(SeekBar seekBar) {}
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void addMaxSuggestionSlider(LinearLayout box) {
        TextView label = label("Max suggestions");
        label.setTextColor(C_TEXT);
        box.addView(label);
        SeekBar bar = new SeekBar(this);
        bar.setMax(8);
        bar.setProgress(configuredMaxSuggestions() - 4);
        styleSeekBar(bar, current != null ? current.accentColor : C_CYAN);
        TextView value = label(String.valueOf(configuredMaxSuggestions()));
        value.setTextColor(C_TEXT_MUTED);
        box.addView(bar, new LinearLayout.LayoutParams(-1, -2));
        box.addView(value);
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser) return;
                int max = progress + 4;
                prefs.edit().putInt(PREF_MAX_SUGGESTIONS, max).apply();
                value.setText(String.valueOf(max));
                clearRhymeCache();
                scheduleSuggestionUpdate();
            }
            public void onStartTrackingTouch(SeekBar seekBar) {}
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void addToggleButton(LinearLayout box, String label, String key, boolean defaultValue) {
        boolean enabled = prefs.getBoolean(key, defaultValue);
        Button b = button(label + ": " + (enabled ? "On" : "Off"));
        b.setOnClickListener(v -> {
            prefs.edit().putBoolean(key, !enabled).apply();
            clearRhymeCache();
            scheduleSuggestionUpdate();
            showRhymeSettingsMenu();
        });
        box.addView(b);
    }

    private String focusedRhymeWord() {
        if (bodyInput == null) return "";
        String text = bodyInput.getText() == null ? "" : bodyInput.getText().toString();
        int cursor = Math.max(0, bodyInput.getSelectionStart());
        TokenInfo info = currentToken(text, cursor);
        if (info.word.isEmpty() && cursor > 0) info = previousToken(text, cursor);
        return normalizeWord(info.word);
    }

    private View buildSheetOverlay() {
        FrameLayout overlay = new FrameLayout(this);
        overlay.setVisibility(View.GONE);
        overlay.setBackgroundColor(color(R.color.topflow_sheet_scrim));
        overlay.setClickable(true);
        overlay.setOnClickListener(v -> dismissSheet());

        sheetCard = new LinearLayout(this);
        sheetCard.setOrientation(LinearLayout.VERTICAL);
        sheetCard.setPadding(dimen(R.dimen.topflow_space_xxl), dimen(R.dimen.topflow_space_xl), dimen(R.dimen.topflow_space_xxl), dimen(R.dimen.topflow_space_xxl));
        sheetCard.setBackgroundResource(R.drawable.bg_surface_sheet);
        sheetCard.setElevation(dimen(R.dimen.topflow_elevation_xl));
        sheetCard.setClickable(true);
        sheetCard.setFocusable(true);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
        lp.leftMargin = dp(14);
        lp.rightMargin = dp(14);
        lp.bottomMargin = dp(14);
        overlay.addView(sheetCard, lp);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        sheetTitle = new TextView(this);
        textStyle(sheetTitle, R.style.TextAppearance_TopFlow_Section);
        sheetTitle.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        header.addView(sheetTitle, new LinearLayout.LayoutParams(0, -2, 1));
        Button close = button("Close");
        close.setOnClickListener(v -> dismissSheet());
        header.addView(close);
        sheetCard.addView(header);

        sheetBody = new LinearLayout(this);
        sheetBody.setOrientation(LinearLayout.VERTICAL);
        sheetBody.setPadding(0, dimen(R.dimen.topflow_space_md), 0, 0);
        sheetCard.addView(sheetBody);
        return overlay;
    }

    private void showSheet(String title, View content) {
        if (sheetOverlay == null || sheetCard == null || sheetBody == null || sheetTitle == null) return;
        sheetTitle.setText(title);
        sheetBody.removeAllViews();
        sheetBody.addView(content);
        sheetOverlay.setVisibility(View.VISIBLE);
        sheetOverlay.setAlpha(0f);
        sheetOverlay.animate().alpha(1f).setDuration(PANEL_TRANSITION_MS).start();
        sheetCard.setTranslationY(dp(24));
        sheetCard.setAlpha(0f);
        sheetCard.animate().translationY(0f).alpha(1f).setDuration(PANEL_TRANSITION_MS).start();
    }

    private void dismissSheet() {
        if (sheetOverlay == null || sheetOverlay.getVisibility() != View.VISIBLE) return;
        sheetCard.animate().translationY(dp(20)).alpha(0f).setDuration(110).start();
        sheetOverlay.animate().alpha(0f).setDuration(110).withEndAction(() -> {
            sheetOverlay.setVisibility(View.GONE);
            if (sheetBody != null) sheetBody.removeAllViews();
        }).start();
    }

    private void checkForUpdates(boolean manual) {
        prefs.edit().putLong("lastUpdateCheck", System.currentTimeMillis()).apply();
        new Thread(() -> {
            try {
                String json = readUpdateManifest();
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
                if (manual) runOnUiThread(() -> Toast.makeText(this, "Update check failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private String readUpdateManifest() throws Exception {
        Exception last = null;
        String[] urls = {BuildConfig.UPDATE_MANIFEST_URL, UPDATE_MANIFEST_JSONBLOB};
        HashSet<String> tried = new HashSet<>();
        for (String url : urls) {
            if (url == null || url.trim().isEmpty() || tried.contains(url)) continue;
            tried.add(url);
            try {
                return readUrl(url);
            } catch (Exception e) {
                last = e;
            }
        }
        throw last == null ? new Exception("No update source") : last;
    }

    private String readUrl(String urlText) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(urlText).openConnection();
        c.setConnectTimeout(12000);
        c.setReadTimeout(12000);
        int code = c.getResponseCode();
        if (code < 200 || code >= 300) throw new Exception("HTTP " + code);
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
            updateMediaLabels();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (songPlayer != null && songPlayer.isPlaying()) {
            songResumePositionMs = songPlayer.getCurrentPosition();
            songPlayer.pause();
        }
        if (recordingPlayer != null && recordingPlayer.isPlaying()) recordingPlayer.pause();
        editHandler.removeCallbacks(suggestionRunnable);
        cancelSuggestionJob();
        dismissSuggestionPopup();
        updatePlaybackStatus();
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

    private void loadRemovedSuggestions() {
        removedSuggestions.clear();
        String raw = prefs.getString(PREF_REMOVED_RHYMES, "");
        if (raw == null || raw.isEmpty()) return;
        String[] words = raw.split("\\n");
        for (String word : words) {
            String normalized = normalizeWord(word);
            if (!normalized.isEmpty()) removedSuggestions.add(normalized);
        }
    }

    private void saveRemovedSuggestions() {
        ArrayList<String> words = new ArrayList<>(removedSuggestions);
        Collections.sort(words);
        StringBuilder out = new StringBuilder();
        for (String word : words) {
            if (out.length() > 0) out.append('\n');
            out.append(word);
        }
        prefs.edit().putString(PREF_REMOVED_RHYMES, out.toString()).apply();
        clearRhymeCache();
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
        textStyle(v, R.style.TextAppearance_TopFlow_Section);
        v.setPadding(0, dimen(R.dimen.topflow_space_xs), 0, dimen(R.dimen.topflow_space_xs));
        v.setIncludeFontPadding(false);
        return v;
    }

    private TextView metadataLabel(String text) {
        TextView v = new TextView(this);
        v.setText(text);
        textStyle(v, R.style.TextAppearance_TopFlow21_Caption);
        TopFlowUiKit.applyQuietText(v);
        v.setPadding(0, dimen(R.dimen.topflow_space_xs), 0, dimen(R.dimen.topflow_space_xs));
        return v;
    }

    private Button button(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        styleButton(b, current != null ? current.accentColor : C_GREEN);
        applyButtonIcon(b, text);
        attachTapAnimation(b);
        return b;
    }

    private void styleButton(Button b, int accent) {
        b.setBackgroundResource(R.drawable.bg21_quiet_control);
        b.setTextColor(TopFlowUiKit.TEXT);
        textSize(b, R.dimen.topflow21_text_label);
        b.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        b.setMinHeight(dp(48));
        b.setMinWidth(dp(58));
        TopFlowUiKit.applyFloating(b, 3);
        b.setStateListAnimator(null);
        b.setIncludeFontPadding(false);
        b.setPadding(dimen(R.dimen.topflow_space_md), dimen(R.dimen.topflow_space_sm), dimen(R.dimen.topflow_space_md), dimen(R.dimen.topflow_space_sm));
        b.setForeground(TopFlowUiKit.ripple(accent));
    }

    private void styleCardButton(Button b, int fill, int accent, int text) {
        b.setBackgroundResource(R.drawable.bg_button_secondary);
        b.setTextColor(text);
        b.setElevation(dimen(R.dimen.topflow_elevation_sm));
        b.setStateListAnimator(null);
        b.setPadding(dimen(R.dimen.topflow_space_lg), dimen(R.dimen.topflow_space_lg), dimen(R.dimen.topflow_space_lg), dimen(R.dimen.topflow_space_lg));
        b.setForeground(ripple(accent));
    }

    private void styleRhymeChip(Button chip, int accent) {
        chip.setBackgroundResource(R.drawable.bg21_quiet_control);
        chip.setTextColor(TopFlowUiKit.TEXT);
        textSize(chip, R.dimen.topflow21_text_label);
        chip.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        chip.setMinHeight(dimen(R.dimen.topflow_chip_height));
        chip.setMinWidth(dp(62));
        TopFlowUiKit.applyFloating(chip, 4);
        chip.setStateListAnimator(null);
        chip.setIncludeFontPadding(false);
        chip.setPadding(dimen(R.dimen.topflow_space_lg), dimen(R.dimen.topflow_space_xs), dimen(R.dimen.topflow_space_lg), dimen(R.dimen.topflow_space_xs));
        chip.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0);
        chip.setForeground(TopFlowUiKit.ripple(accent));
        attachTapAnimation(chip);
    }

    private void applyButtonIcon(Button button, String text) {
        int icon = iconForButton(text);
        if (icon == 0) return;
        button.setCompoundDrawablesRelativeWithIntrinsicBounds(icon, 0, 0, 0);
        button.setCompoundDrawablePadding(dimen(R.dimen.topflow_space_xs));
    }

    private int iconForButton(String text) {
        if (text == null) return 0;
        String label = text.toLowerCase(Locale.US);
        if (label.equals("menu") || label.equals("studio")) return R.drawable.ic_menu_24;
        if (label.contains("+ note")) return R.drawable.ic_add_note_24;
        if (label.contains("close")) return R.drawable.ic_close_24;
        if (label.contains("attach song")) return R.drawable.ic_song_24;
        if (label.contains("play / pause song") || label.equals("play")) return R.drawable.ic_play_24;
        if (label.contains("record voice")) return R.drawable.ic_mic_24;
        if (label.equals("stop")) return R.drawable.ic_stop_24;
        if (label.contains("note style") || label.contains("font") || label.equals("style")) return R.drawable.ic_style_24;
        if (label.contains("expanded rhymes") || label.equals("rhymes")) return R.drawable.ic_rhyme_24;
        if (label.contains("rhyme settings") || label.equals("settings")) return R.drawable.ic_settings_24;
        if (label.contains("check for updates")) return R.drawable.ic_update_24;
        if (label.contains("restore")) return R.drawable.ic_restore_24;
        if (label.equals("save")) return R.drawable.ic_save_24;
        return 0;
    }

    private LinearLayout createCardSurface() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dimen(R.dimen.topflow_space_xl), dimen(R.dimen.topflow_space_xl), dimen(R.dimen.topflow_space_xl), dimen(R.dimen.topflow_space_xl));
        card.setBackground(TopFlowUiKit.floatingPanel(this, 26));
        TopFlowUiKit.applyFloating(card, 12);
        return card;
    }

    private LinearLayout createSectionCard(String title) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dimen(R.dimen.topflow_space_xl), dimen(R.dimen.topflow_space_lg), dimen(R.dimen.topflow_space_xl), dimen(R.dimen.topflow_space_lg));
        card.setBackground(TopFlowUiKit.floatingPanel(this, 20));
        TopFlowUiKit.applyFloating(card, 8);
        TextView label = new TextView(this);
        label.setText(title);
        textStyle(label, R.style.TextAppearance_TopFlow21_Section);
        label.setTextColor(TopFlowUiKit.TEXT);
        label.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        label.setIncludeFontPadding(false);
        label.setPadding(0, 0, 0, dimen(R.dimen.topflow_space_sm));
        card.addView(label);
        return card;
    }

    private LinearLayout horizontalCardButtons(View... buttons) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(8), 0, 0);
        for (int i = 0; i < buttons.length; i++) {
            View v = buttons[i];
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1);
            if (i > 0) lp.leftMargin = dp(8);
            row.addView(v, lp);
        }
        return row;
    }

    private Drawable glassDrawable(int tint, int accent, int radiusDp) {
        GradientDrawable d = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{
                        blendColor(tint, Color.WHITE, 0.045f),
                        tint,
                        blendColor(tint, Color.BLACK, 0.16f)
                });
        d.setCornerRadius(dp(radiusDp));
        d.setStroke(dp(1), Color.argb(92, Color.red(accent), Color.green(accent), Color.blue(accent)));
        return d;
    }

    private Drawable controlDrawable(int tint, int accent, int radiusDp) {
        GradientDrawable d = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{
                        blendColor(tint, accent, 0.12f),
                        tint,
                        blendColor(tint, Color.BLACK, 0.18f)
                });
        d.setCornerRadius(dp(radiusDp));
        d.setStroke(dp(1), Color.argb(126, Color.red(accent), Color.green(accent), Color.blue(accent)));
        return d;
    }

    private Drawable chipDrawable(int accent) {
        GradientDrawable d = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{
                        Color.argb(244, Math.max(0, Color.red(accent) - 68), Math.max(0, Color.green(accent) - 78), Math.max(0, Color.blue(accent) - 76)),
                        Color.argb(244, Math.max(0, Color.red(accent) - 36), Math.max(0, Color.green(accent) - 42), Math.max(0, Color.blue(accent) - 40))
                });
        d.setCornerRadius(dp(999));
        d.setStroke(dp(1), Color.argb(176, Color.red(accent), Color.green(accent), Color.blue(accent)));
        return d;
    }

    private Drawable ripple(int accent) {
        return new RippleDrawable(ColorStateList.valueOf(Color.argb(70, Color.red(accent), Color.green(accent), Color.blue(accent))), null, null);
    }

    private void attachTapAnimation(View v) {
        v.setOnTouchListener((view, e) -> {
            if (e.getAction() == MotionEvent.ACTION_DOWN) {
                view.animate().scaleX(0.985f).scaleY(0.985f).translationY(dp(1)).setDuration(BUTTON_PRESS_MS).start();
            } else if (e.getAction() == MotionEvent.ACTION_UP || e.getAction() == MotionEvent.ACTION_CANCEL) {
                view.animate().scaleX(1f).scaleY(1f).translationY(0f).setDuration(BUTTON_PRESS_MS).start();
            }
            return false;
        });
    }

    private int tintFrom(int color, float amount) {
        int a = 185;
        int r = Math.min(255, (int) (Color.red(color) * (1f + amount)));
        int g = Math.min(255, (int) (Color.green(color) * (1f + amount)));
        int b = Math.min(255, (int) (Color.blue(color) * (1f + amount)));
        return Color.argb(a, r, g, b);
    }

    private String compactPreview(String body) {
        if (body == null) return "No lyrics yet";
        String s = body.trim().replace('\n', ' ');
        return s.length() > 72 ? s.substring(0, 72) + "…" : s;
    }

    private int adjustColor(int color, float factor) {
        int r = Math.min(255, (int) (Color.red(color) * (1f + factor)));
        int g = Math.min(255, (int) (Color.green(color) * (1f + factor)));
        int b = Math.min(255, (int) (Color.blue(color) * (1f + factor)));
        return Color.rgb(r, g, b);
    }

    private int blendColor(int color, int target, float amount) {
        int a = Color.alpha(color);
        if (a == 0) a = 255;
        int r = (int) (Color.red(color) + (Color.red(target) - Color.red(color)) * amount);
        int g = (int) (Color.green(color) + (Color.green(target) - Color.green(color)) * amount);
        int b = (int) (Color.blue(color) + (Color.blue(target) - Color.blue(color)) * amount);
        return Color.argb(a, Math.max(0, Math.min(255, r)), Math.max(0, Math.min(255, g)), Math.max(0, Math.min(255, b)));
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private int dimen(int resId) {
        return getResources().getDimensionPixelSize(resId);
    }

    private int color(int resId) {
        return getColor(resId);
    }

    private void textStyle(TextView view, int styleRes) {
        view.setTextAppearance(styleRes);
    }

    private void textSize(TextView view, int dimenRes) {
        view.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(dimenRes));
    }

    private void applyInsetsNow() {
        if (root == null) return;
        ViewCompat.requestApplyInsets(root);
    }

    private void styleSeekBar(SeekBar seekBar, int accent) {
        seekBar.setProgressTintList(ColorStateList.valueOf(accent));
        seekBar.setThumbTintList(ColorStateList.valueOf(accent));
        seekBar.setProgressBackgroundTintList(ColorStateList.valueOf(Color.argb(120, Color.red(accent), Color.green(accent), Color.blue(accent))));
    }

    private int progressForSongMs(int positionMs) {
        if (songPlayer == null) return 0;
        int duration = Math.max(1, songPlayer.getDuration());
        return Math.max(0, Math.min(1000, (int) ((positionMs / (float) duration) * 1000f)));
    }

    private int progressToSongMs(int progress) {
        if (songPlayer == null) return 0;
        int duration = Math.max(1, songPlayer.getDuration());
        return Math.max(0, Math.min(duration, (int) ((progress / 1000f) * duration)));
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
        int noteColor = DEFAULT_NOTE_COLOR;
        int textColor = DEFAULT_NOTE_TEXT_COLOR;
        int accentColor = DEFAULT_NOTE_ACCENT_COLOR;
        String songUri = "";
        ArrayList<RecordingTag> recordings = new ArrayList<>();

        static Note create(String title) {
            Note n = new Note();
            n.title = title;
            n.body = "";
            n.noteColor = DEFAULT_NOTE_COLOR;
            n.textColor = DEFAULT_NOTE_TEXT_COLOR;
            n.accentColor = DEFAULT_NOTE_ACCENT_COLOR;
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
            for (RecordingTag path : recordings) r.put(path.toJson());
            o.put("recordings", r);
            return o;
        }

        static Note fromJson(JSONObject o) {
            Note n = create(o.optString("title", "Untitled"));
            n.body = o.optString("body", "");
            n.font = o.optString("font", "sans");
            n.noteColor = o.optInt("noteColor", DEFAULT_NOTE_COLOR);
            n.textColor = o.optInt("textColor", DEFAULT_NOTE_TEXT_COLOR);
            n.accentColor = o.optInt("accentColor", DEFAULT_NOTE_ACCENT_COLOR);
            n.songUri = o.optString("songUri", "");
            JSONArray r = o.optJSONArray("recordings");
            if (r != null) {
                for (int i = 0; i < r.length(); i++) n.recordings.add(RecordingTag.fromJson(r.optJSONObject(i), r.optString(i)));
            }
            return n;
        }
    }

    static class RecordingTag {
        String path;
        String tag;
        RecordingTag(String path, String tag) { this.path = path; this.tag = tag; }
        JSONObject toJson() throws Exception {
            JSONObject o = new JSONObject();
            o.put("path", path);
            o.put("tag", tag);
            return o;
        }
        static RecordingTag fromJson(JSONObject o, String fallbackPath) {
            if (o == null) return new RecordingTag(fallbackPath, new File(fallbackPath).getName());
            return new RecordingTag(o.optString("path", fallbackPath), o.optString("tag", new File(fallbackPath).getName()));
        }
    }

    static class ColorWheelView extends View {
        interface OnColorChangedListener { void onColorChanged(int color); }
        private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint thumb = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF bounds = new RectF();
        private OnColorChangedListener listener;
        private float hue = 0f;
        private float sat = 1f;
        private float val = 1f;
        ColorWheelView(Context c) {
            super(c);
            ring.setStyle(Paint.Style.STROKE);
            ring.setStrokeWidth(48f);
            thumb.setStyle(Paint.Style.FILL);
        }
        void setOnColorChangedListener(OnColorChangedListener l) { listener = l; }
        void setColor(int color) { float[] hsv = new float[3]; Color.colorToHSV(color, hsv); hue = hsv[0]; sat = hsv[1]; val = hsv[2]; invalidate(); }
        int getColor() { return Color.HSVToColor(new float[]{hue, sat, val}); }
        float getHue() { return hue; }
        @Override protected void onDraw(Canvas canvas) {
            float cx = getWidth() / 2f, cy = getHeight() / 2f;
            float radius = Math.min(cx, cy) - 32f;
            bounds.set(cx - radius, cy - radius, cx + radius, cy + radius);
            for (int i = 0; i < 360; i += 2) {
                ring.setColor(Color.HSVToColor(new float[]{i, 1f, 1f}));
                canvas.drawArc(bounds, i, 2, false, ring);
            }
            float angle = (float) Math.toRadians(hue);
            float x = cx + (float) Math.cos(angle) * radius * sat;
            float y = cy + (float) Math.sin(angle) * radius * sat;
            thumb.setColor(getColor());
            canvas.drawCircle(x, y, 22f, thumb);
        }
        @Override public boolean onTouchEvent(MotionEvent e) {
            float cx = getWidth() / 2f, cy = getHeight() / 2f;
            float dx = e.getX() - cx, dy = e.getY() - cy;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            float radius = Math.min(cx, cy) - 32f;
            if (dist > radius) dist = radius;
            hue = (float) ((Math.toDegrees(Math.atan2(dy, dx)) + 360f) % 360f);
            sat = Math.min(1f, dist / radius);
            if (listener != null) listener.onColorChanged(getColor());
            invalidate();
            return true;
        }
    }

    static class NeonBackdropView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);

        NeonBackdropView(Context c) {
            super(c);
            line.setColor(Color.argb(14, 150, 190, 210));
            line.setStrokeWidth(1f);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            int w = getWidth();
            int h = getHeight();
            paint.setShader(new android.graphics.LinearGradient(0, 0, 0, h,
                    new int[]{Color.rgb(4, 7, 15), Color.rgb(10, 14, 25), Color.rgb(3, 5, 12)},
                    new float[]{0f, 0.55f, 1f}, android.graphics.Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, paint);

            paint.setShader(new android.graphics.RadialGradient(w * 0.2f, h * 0.15f, Math.max(w, h) * 0.8f,
                    new int[]{Color.argb(64, 40, 214, 163), Color.argb(0, 40, 214, 163)},
                    new float[]{0f, 1f}, android.graphics.Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, paint);

            paint.setShader(new android.graphics.RadialGradient(w * 0.8f, h * 0.2f, Math.max(w, h) * 0.65f,
                    new int[]{Color.argb(48, 238, 194, 105), Color.argb(0, 238, 194, 105)},
                    new float[]{0f, 1f}, android.graphics.Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, paint);

            for (int y = 0; y < h; y += 48) canvas.drawLine(0, y, w, y, line);
        }
    }

    static class RuledLinesDrawable extends Drawable {
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint lines = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();
        private final int backgroundColor;

        RuledLinesDrawable(int backgroundColor, int accentColor) {
            this.backgroundColor = backgroundColor;
            fill.setColor(backgroundColor);
            lines.setColor(Color.argb(110, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor)));
            lines.setStrokeWidth(2f);
        }

        @Override
        public void draw(Canvas canvas) {
            rect.set(getBounds());
            canvas.drawRect(rect, fill);
            float top = rect.top + 24f;
            float bottom = rect.bottom - 8f;
            for (float y = top; y < bottom; y += 42f) {
                canvas.drawLine(rect.left + 8f, y, rect.right - 8f, y, lines);
            }
        }

        @Override
        public void setAlpha(int alpha) {
            fill.setAlpha(alpha);
            lines.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(android.graphics.ColorFilter colorFilter) {
            fill.setColorFilter(colorFilter);
            lines.setColorFilter(colorFilter);
        }

        @Override
        public int getOpacity() {
            return android.graphics.PixelFormat.OPAQUE;
        }
    }

    static class RuledEditText extends EditText {
        interface SelectionListener { void onSelectionChanged(); }
        private final Paint rulePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private SelectionListener selectionListener;

        RuledEditText(Context c) {
            super(c);
            setBackgroundColor(Color.TRANSPARENT);
            rulePaint.setStrokeWidth(2f);
            rulePaint.setColor(Color.argb(92, 132, 255, 238));
        }

        void setOnSelectionChangedListener(SelectionListener listener) {
            selectionListener = listener;
        }

        void setRuleColor(int accent) {
            rulePaint.setColor(Color.argb(92, Color.red(accent), Color.green(accent), Color.blue(accent)));
            invalidate();
        }

        @Override
        protected void onSelectionChanged(int selStart, int selEnd) {
            super.onSelectionChanged(selStart, selEnd);
            if (selectionListener != null) selectionListener.onSelectionChanged();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            Layout layout = getLayout();
            if (layout != null) {
                int left = getCompoundPaddingLeft();
                int right = getWidth() - getCompoundPaddingRight();
                int count = layout.getLineCount();
                for (int i = 0; i < count; i++) {
                    float y = layout.getLineBottom(i) + 2f;
                    canvas.drawLine(left, y, right, y, rulePaint);
                }
            }
            super.onDraw(canvas);
        }
    }
}
