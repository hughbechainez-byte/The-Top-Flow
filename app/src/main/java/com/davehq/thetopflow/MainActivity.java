package com.davehq.thetopflow;

import android.Manifest;
import android.animation.TimeInterpolator;
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
import android.graphics.RenderEffect;
import android.graphics.Shader;
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
import android.text.InputType;
import android.text.Layout;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.VelocityTracker;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.textclassifier.TextClassifier;
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

import androidx.activity.ComponentActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.graphics.Insets;
import androidx.core.content.FileProvider;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewTreeLifecycleOwner;
import androidx.lifecycle.ViewTreeViewModelStoreOwner;
import androidx.savedstate.ViewTreeSavedStateRegistryOwner;

import com.davehq.thetopflow.ui.TopFlowUiBackdropBridge;

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

public class MainActivity extends ComponentActivity {
    private static final String TAG = "TopFlow";
    private static final int REQ_SONG = 10;
    private static final int REQ_AUDIO = 11;
    private static final int REQ_NOTIFY = 12;
    private static final String CHANNEL_UPDATES = "updates";
    private static final int C_BG = Color.BLACK;
    private static final int C_SURFACE = Color.argb(238, 5, 7, 11);
    private static final int C_SURFACE_SOFT = Color.argb(214, 8, 11, 16);
    private static final int C_SURFACE_HIGH = Color.argb(246, 12, 16, 22);
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
    private static final int SUGGESTION_DEBOUNCE_MS = 220;
    private static final int FAST_RHYME_LIMIT = 6;
    private static final int FAST_RHYME_CANDIDATE_LIMIT = 360;
    private static final int EXPANDED_RHYME_CANDIDATE_LIMIT = 720;
    private static final int RHYME_CACHE_LIMIT = 96;
    private static final int DEFAULT_EDITOR_FONT_SIZE_SP = 18;
    private static final int MIN_EDITOR_FONT_SIZE_SP = 14;
    private static final int MAX_EDITOR_FONT_SIZE_SP = 28;
    private static final int DEFAULT_NOTE_GLOW_STRENGTH = 1;
    // Motion: tap and selection feedback stays quick for touch response.
    private static final int MOTION_TAP_PRESS_MS = 78;
    private static final int MOTION_TAP_RELEASE_MS = 96;
    private static final float MOTION_TAP_PRESS_SCALE = 0.984f;
    private static final float MOTION_TAP_RELEASE_SCALE = 1f;
    private static final int MOTION_SELECTION_PRESS_MS = 74;
    private static final int MOTION_SELECTION_RELEASE_MS = 106;
    private static final float MOTION_SELECTION_SCALE = 1.03f;
    private static final float MOTION_SELECTION_ALPHA = 0.93f;

    // Motion: panels and sheets are slower than tap feedback for a premium shell feel.
    private static final int MOTION_PANEL_SHOW_MS = 240;
    private static final int MOTION_PANEL_HIDE_MS = 190;
    private static final int MOTION_PANEL_SWAP_MS = 280;
    private static final int MOTION_SHEET_REVEAL_MS = 220;
    private static final int MOTION_SHEET_DISMISS_MS = 210;
    private static final int MOTION_SHEET_RESTORE_MS = 200;
    private static final int SHEET_BLUR_RADIUS_DP = 24;
    private static final int SHEET_DISMISS_DISTANCE_DP = 50;
    private static final int SHEET_DISMISS_VELOCITY = 700;

    // Motion: dock and edge gestures remain responsive while sharing the same easing.
    private static final int MOTION_DOCK_FEEDBACK_MS = 138;
    private static final float MOTION_DOCK_ACTIVE_SCALE = 1.03f;
    private static final float MOTION_DOCK_INACTIVE_SCALE = 1f;
    private static final float MOTION_DOCK_ACTIVE_ALPHA = 1f;
    private static final float MOTION_DOCK_INACTIVE_ALPHA = 0.68f;
    private static final float MOTION_SWIPE_SCALE_REDUCE_X = 0.05f;
    private static final float MOTION_SWIPE_SCALE_REDUCE_Y = 0.02f;
    private static final int MOTION_SWIPE_COMPLETE_MS = 132;
    private static final int MOTION_SWIPE_RESTORE_MS = 170;

    // Motion: startup keeps the existing preload timing with smoother fill/close.
    private static final int SPLASH_MIN_MS = 760;
    private static final int SPLASH_MAX_MS = 1800;
    private static final int MOTION_SPLASH_FILL_MS = 1040;
    private static final int MOTION_SPLASH_READY_FILL_MS = 220;
    private static final int MOTION_SPLASH_CLOSE_FILL_MS = 160;
    private static final int MOTION_SPLASH_CLOSE_MS = 240;
    private static final int RHYME_PRELOAD_COMMON_LIMIT = 64;
    private static final int RHYME_PRELOAD_EXPANDED_LIMIT = 12;
    private static final int DOCK_STATE_NONE = 0;
    private static final int DOCK_STATE_NOTES = 1;
    private static final int DOCK_STATE_RHYME = 2;
    private static final int DOCK_STATE_STYLE = 3;
    private static final int DOCK_STATE_SETTINGS = 4;
    private static final int EDITOR_BACK_SWIPE_EDGE_DP = 74;
    private static final int EDITOR_BACK_SWIPE_TRIGGER_DP = 150;
    private static final int EDITOR_BACK_SWIPE_VELOCITY_PX = 1450;
    private static final int EDITOR_BACK_SWIPE_START_DP = 12;
    private static final int EDITOR_BACK_SWIPE_VERTICAL_BREAK_DP = 18;
    private static final float WORKFLOW_SWIPE_TRIGGER_RATIO = 0.52f;
    private static final float WORKFLOW_SWIPE_FLICK_RATIO = 1.45f;
    private static final float WORKFLOW_SWIPE_STRAIGHTNESS_RATIO = 1.75f;
    private static final int WORKFLOW_SWIPE_RAIL_WIDTH_DP = 4;
    private static final int SHEET_MENU_PREVIEW_LIMIT = 4;
    private static final TimeInterpolator MOTION_INTERPOLATOR = new DecelerateInterpolator(1.35f);
    private static final int SHEET_SCROLL_CAP_ATTEMPTS = 3;
    private static final float SHEET_DRAG_START_RATIO = 0.58f;
    private static final int DOCK_STATE_ELEVATION_DP = 12;
    private static final boolean RHYME_TRACE = true;
    private static final String UPDATE_MANIFEST_JSONBLOB = "https://jsonblob.com/api/jsonBlob/019f13c7-dc3f-7cf2-bf88-038a846852bd";
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
    private View editorSwipeRail;
    private View notesSwipeRail;
    private LinearLayout menuPanel;
    private LinearLayout editorPanel;
    private ScrollView editorScroll;
    private View sheetOverlay;
    private LinearLayout sheetCard;
    private TextView sheetTitle;
    private LinearLayout sheetBody;
    private ScrollView sheetBodyScroll;
    private View sheetDragHandle;
    private View sheetHeader;
    private Button dockNotesButton;
    private Button dockRhymeButton;
    private Button dockStyleButton;
    private Button dockSettingsButton;
    private LinearLayout studioDock;
    private int activeDockState = DOCK_STATE_NONE;
    private LinearLayout noteList;
    private LinearLayout editor;
    private FrameLayout editorGlowFrame;
    private LinearLayout editorCard;
    private LinearLayout editorChrome;
    private View editorSignalRail;
    private View editorMiniSignal;
    private TextView editorHeaderTitle;
    private TextView editorHeaderMeta;
    private TextView shellStatusLabel;
    private View shellSignal;
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
    private TextView splashStatus;
    private TextView splashProgressLabel;
    private View splashFill;
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
    private int suggestionPanelWidth = -1;
    private int suggestionPanelHeight = -1;
    private boolean suggestionPanelDirty = true;
    private volatile int expandedRhymeRequestId = 0;
    private Future<?> expandedRhymeFuture;
    private boolean bodyDraftDirty = false;
    private boolean lastImeVisible = false;
    private boolean lastEditorFocus = false;
    private boolean splashClosing = false;
    private long splashStartedAtMs = 0L;
    private volatile boolean rhymePreloadComplete = false;
    private volatile long rhymePreloadCompletedAtMs = 0L;
    private volatile boolean firstQueryAfterPreloadLogged = false;
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
    private final Runnable saveDraftRunnable = this::saveCurrentDraft;
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
        saveCurrentDraft();
        handler.removeCallbacks(updatePoll);
        playbackHandler.removeCallbacks(playbackTicker);
        editHandler.removeCallbacks(saveDraftRunnable);
        editHandler.removeCallbacks(suggestionRunnable);
        cancelSuggestionJob();
        cancelExpandedRhymeJob();
        rhymeExecutor.shutdownNow();
        dismissSuggestionPopup();
        stopRecording(false);
        stopSongPlayback();
        stopRecordingPlayback();
        super.onDestroy();
    }

    private void buildUi() {
        root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        attachActivityViewTreeOwners(root, "activity_root", "compose_owner_attached");
        setContentView(root);

        View premiumBackdrop = TopFlowUiBackdropBridge.createPremiumBackdrop(this);
        root.addView(premiumBackdrop, 0, new FrameLayout.LayoutParams(-1, -1));

        shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        FrameLayout.LayoutParams shellLp = new FrameLayout.LayoutParams(
                Math.min(getResources().getDisplayMetrics().widthPixels - dimen(R.dimen.topflow_space_xl), dimen(R.dimen.topflow_max_content_width)),
                -1);
        shellLp.gravity = Gravity.CENTER_HORIZONTAL;
        root.addView(shell, shellLp);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            shell.setPadding(dimen(R.dimen.topflow21_space_screen), bars.top + dimen(R.dimen.topflow_space_sm), dimen(R.dimen.topflow21_space_screen), bars.bottom + dimen(R.dimen.topflow_space_sm));
            boolean imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime());
            if (lastImeVisible != imeVisible) {
                lastImeVisible = imeVisible;
                logRhymeTrace("ime_visibility", 0L, "visible=" + imeVisible + " focus=" + (bodyInput != null && bodyInput.hasFocus()) + " noteLen=" + editorTextLength());
            }
            return insets;
        });

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dimen(R.dimen.topflow_space_lg), dimen(R.dimen.topflow_space_sm), dimen(R.dimen.topflow_space_lg), dimen(R.dimen.topflow_space_sm));
        header.setBackground(TopFlowUiKit.floatingPanel(this, 24));
        TopFlowUiKit.applyFloating(header, 10);

        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);
        left.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        LinearLayout titleWrap = new LinearLayout(this);
        titleWrap.setOrientation(LinearLayout.HORIZONTAL);
        titleWrap.setGravity(Gravity.CENTER_VERTICAL);
        TextView brand = new TextView(this);
        brand.setText("Top Flow");
        textStyle(brand, R.style.TextAppearance_TopFlow21_Title);
        brand.setIncludeFontPadding(false);
        brand.setLetterSpacing(0f);
        brand.setMaxLines(1);
        brand.setEllipsize(TextUtils.TruncateAt.END);
        titleWrap.addView(brand);
        TextView version = new TextView(this);
        version.setText("  v" + BuildConfig.VERSION_NAME);
        textStyle(version, R.style.TextAppearance_TopFlow21_Caption);
        version.setTextColor(TopFlowUiKit.MINT);
        version.setIncludeFontPadding(false);
        version.setLetterSpacing(0f);
        version.setMaxLines(1);
        titleWrap.addView(version);
        left.addView(titleWrap);

        shellStatusLabel = new TextView(this);
        shellStatusLabel.setText("Studio ready");
        shellStatusLabel.setTextColor(TopFlowUiKit.TEXT_SOFT);
        shellStatusLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        shellStatusLabel.setIncludeFontPadding(false);
        shellStatusLabel.setLetterSpacing(0f);
        shellStatusLabel.setMaxLines(1);
        shellStatusLabel.setEllipsize(TextUtils.TruncateAt.END);
        shellStatusLabel.setPadding(0, dp(2), 0, 0);
        left.addView(shellStatusLabel);

        shellSignal = buildSessionMiniSignal(C_CYAN, 18, true);
        if (shellSignal != null) {
            LinearLayout signalWrap = new LinearLayout(this);
            signalWrap.setOrientation(LinearLayout.HORIZONTAL);
            signalWrap.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            signalWrap.setPadding(0, dp(4), 0, 0);
            signalWrap.addView(shellSignal);
            left.addView(signalWrap);
        }

        header.addView(left);
        Button menu = button("Menu");
        menu.setOnClickListener(v -> showMainMenu());
        header.addView(menu);
        LinearLayout.LayoutParams headerLp = new LinearLayout.LayoutParams(-1, -2);
        headerLp.bottomMargin = dimen(R.dimen.topflow_space_md);
        shell.addView(header, headerLp);
        updateShellStatus();

        contentHost = new FrameLayout(this);
        shell.addView(contentHost, new LinearLayout.LayoutParams(-1, 0, 1));

        editorSwipeRail = createSwipeRail(false);
        notesSwipeRail = createSwipeRail(true);
        contentHost.addView(editorSwipeRail, swipeRailLayoutParams(false));
        contentHost.addView(notesSwipeRail, swipeRailLayoutParams(true));

        LinearLayout dock = buildStudioDock();
        LinearLayout.LayoutParams dockLp = new LinearLayout.LayoutParams(-1, -2);
        dockLp.topMargin = dimen(R.dimen.topflow_space_md);
        shell.addView(dock, dockLp);

        menuPanel = new LinearLayout(this);
        menuPanel.setOrientation(LinearLayout.VERTICAL);
        menuPanel.setPadding(dimen(R.dimen.topflow_space_lg), dimen(R.dimen.topflow_space_lg), dimen(R.dimen.topflow_space_lg), dimen(R.dimen.topflow_space_lg));
        menuPanel.setBackgroundColor(Color.TRANSPARENT);
        menuPanel.setClipToPadding(false);
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
        editorPanel.setPadding(dimen(R.dimen.topflow_space_lg), dimen(R.dimen.topflow_space_lg), dimen(R.dimen.topflow_space_lg), dimen(R.dimen.topflow_space_lg));
        editorPanel.setBackgroundColor(Color.TRANSPARENT);
        editorPanel.setClipToPadding(false);
        contentHost.addView(editorPanel, new FrameLayout.LayoutParams(-1, -1));

        editorScroll = new ScrollView(this);
        editorScroll.setFillViewport(true);
        editor = new LinearLayout(this);
        editor.setOrientation(LinearLayout.VERTICAL);
        editor.setPadding(0, 0, 0, 0);
        editorScroll.addView(editor);
        editorPanel.addView(editorScroll, new LinearLayout.LayoutParams(-1, -1, 1));
        attachEditorBackSwipe(editorScroll);
        attachNotesBackSwipe(listScroll);

        editorGlowFrame = new FrameLayout(this);
        editorGlowFrame.setClipToPadding(false);
        editorGlowFrame.setPadding(dp(12), dp(10), dp(12), dp(12));
        LinearLayout.LayoutParams editorCardLp = new LinearLayout.LayoutParams(-1, -2);
        editorCardLp.bottomMargin = dimen(R.dimen.topflow_space_md);
        editor.addView(editorGlowFrame, editorCardLp);

        editorCard = createCardSurface();
        editorCard.setPadding(dimen(R.dimen.topflow_space_lg), dimen(R.dimen.topflow_space_md), dimen(R.dimen.topflow_space_lg), dimen(R.dimen.topflow_space_lg));
        editorGlowFrame.addView(editorCard, new FrameLayout.LayoutParams(-1, -2));

        editorChrome = new LinearLayout(this);
        editorChrome.setOrientation(LinearLayout.HORIZONTAL);
        editorChrome.setGravity(Gravity.CENTER_VERTICAL);
        editorChrome.setPadding(0, 0, 0, dimen(R.dimen.topflow_space_md));
        editorChrome.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        editorCard.addView(editorChrome);

        editorSignalRail = new View(this);
        editorSignalRail.setLayoutParams(new LinearLayout.LayoutParams(dp(4), dp(48)));
        editorChrome.addView(editorSignalRail);

        View railGap = new View(this);
        railGap.setLayoutParams(new LinearLayout.LayoutParams(dp(10), 0));
        editorChrome.addView(railGap);

        LinearLayout headerCopy = new LinearLayout(this);
        headerCopy.setOrientation(LinearLayout.VERTICAL);
        headerCopy.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));
        editorChrome.addView(headerCopy);

        editorHeaderTitle = new TextView(this);
        editorHeaderTitle.setText("Current session");
        editorHeaderTitle.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        editorHeaderTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        editorHeaderTitle.setTextColor(TopFlowUiKit.TEXT);
        editorHeaderTitle.setIncludeFontPadding(false);
        editorHeaderTitle.setLetterSpacing(0f);
        editorHeaderTitle.setMaxLines(1);
        editorHeaderTitle.setEllipsize(TextUtils.TruncateAt.END);
        headerCopy.addView(editorHeaderTitle);

        editorHeaderMeta = new TextView(this);
        editorHeaderMeta.setText("Draft body");
        editorHeaderMeta.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        editorHeaderMeta.setTextColor(TopFlowUiKit.TEXT_SOFT);
        editorHeaderMeta.setIncludeFontPadding(false);
        editorHeaderMeta.setLetterSpacing(0f);
        editorHeaderMeta.setMaxLines(1);
        editorHeaderMeta.setEllipsize(TextUtils.TruncateAt.END);
        headerCopy.addView(editorHeaderMeta);

        editorMiniSignal = buildSessionMiniSignal(C_CYAN, 7, true);
        if (editorMiniSignal != null) {
            LinearLayout.LayoutParams signalLp = new LinearLayout.LayoutParams(dp(42), dp(34));
            signalLp.leftMargin = dp(8);
            editorChrome.addView(editorMiniSignal, signalLp);
        }

        titleInput = new EditText(this);
        titleInput.setSingleLine(true);
        titleInput.setHint("Untitled track");
        titleInput.setBackgroundColor(Color.TRANSPARENT);
        textStyle(titleInput, R.style.TextAppearance_TopFlow21_Title);
        titleInput.setPadding(dimen(R.dimen.topflow_space_lg), dimen(R.dimen.topflow_space_md), dimen(R.dimen.topflow_space_lg), dimen(R.dimen.topflow_space_md));
        titleInput.setIncludeFontPadding(false);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(-1, -2);
        titleLp.topMargin = dp(2);
        titleLp.bottomMargin = dp(2);
        editorCard.addView(titleInput, titleLp);

        bodyInput = new RuledEditText(this);
        bodyInput.setGravity(Gravity.TOP | Gravity.START);
        bodyInput.setMinLines(18);
        bodyInput.setHint("Start writing...");
        textStyle(bodyInput, R.style.TextAppearance_TopFlow21_Body);
        bodyInput.setLineSpacing(dp(2), 1.06f);
        bodyInput.setPadding(dimen(R.dimen.topflow_space_lg), dimen(R.dimen.topflow_space_lg), dimen(R.dimen.topflow_space_lg), dimen(R.dimen.topflow_space_xl));
        bodyInput.setIncludeFontPadding(false);
        configureEditorInput(bodyInput);
        editorCard.addView(bodyInput, new LinearLayout.LayoutParams(-1, 0, 1));

        suggestionPanel = buildSuggestionRow("Rhymes");
        Log.d(TAG, "popup_host_created host=suggestion_popup content=native_rhyme_row");
        attachActivityViewTreeOwners(suggestionPanel, "suggestion_popup", "popup_owner_attached");
        suggestionPanel.setVisibility(View.GONE);
        suggestionPopup = new PopupWindow(suggestionPanel, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, false);
        suggestionPopup.setTouchable(true);
        suggestionPopup.setOutsideTouchable(false);
        suggestionPopup.setClippingEnabled(true);
        suggestionPopup.setInputMethodMode(PopupWindow.INPUT_METHOD_NOT_NEEDED);
        suggestionPopup.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
        Log.d(TAG, "popup_content_set host=suggestion_popup compose=false");
        logRhymeTrace("popup_created", 0L, "focusable=false inputMethod=not_needed softInput=adjust_nothing reuse=single_instance");

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
        String[] fonts = TopFlowUiKit.fontPreviewIds();
        fontSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, fonts));
        fontSpinner.setVisibility(View.GONE);
        colorPreview = label("");
        colorPreview.setVisibility(View.GONE);

        titleInput.addTextChangedListener(simpleWatcher(() -> {
            if (!suppressSave && current != null) {
                current.title = titleInput.getText().toString();
                saveAndRenderList();
            }
            if (editorHeaderTitle != null && current != null) {
                editorHeaderTitle.setText(noteTitleForDisplay(current));
            }
            updateShellStatus();
        }));
        bodyInput.addTextChangedListener(simpleWatcher(() -> {
            if (!suppressSave && current != null) {
                bodyDraftDirty = true;
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
        setActiveDockState(DOCK_STATE_NOTES);
        applyInsetsNow();
        animateScreenSwap(menuPanel, 1f);
    }

    private void attachActivityViewTreeOwners(View view, String host, String event) {
        ViewTreeLifecycleOwner.set(view, this);
        ViewTreeSavedStateRegistryOwner.set(view, this);
        ViewTreeViewModelStoreOwner.set(view, this);
        Log.d(TAG, event + " host=" + host + " lifecycle=true savedState=true viewModel=true");
    }

    private LinearLayout buildStudioDock() {
        LinearLayout dock = new LinearLayout(this);
        studioDock = dock;
        dock.setOrientation(LinearLayout.HORIZONTAL);
        dock.setGravity(Gravity.CENTER_VERTICAL);
        dock.setPadding(dimen(R.dimen.topflow_space_sm), dp(7), dimen(R.dimen.topflow_space_sm), dp(7));
        dock.setMinimumHeight(dp(66));
        dock.setBackground(dockSurfaceDrawable(dockAccent()));
        TopFlowUiKit.applyFloating(dock, DOCK_STATE_ELEVATION_DP);
        Button notes = button("Notes");
        dockNotesButton = notes;
        styleDockButton(notes);
        notes.setOnClickListener(v -> showMenuScreen());
        Button rhymes = button("Rhyme");
        dockRhymeButton = rhymes;
        styleDockButton(rhymes);
        rhymes.setOnClickListener(v -> {
            if (current == null) {
                Toast.makeText(this, "Open a note first", Toast.LENGTH_SHORT).show();
                return;
            }
            showExpandedRhymes();
        });
        Button style = button("Style");
        dockStyleButton = style;
        styleDockButton(style);
        style.setOnClickListener(v -> {
            if (current == null) {
                Toast.makeText(this, "Open a note first", Toast.LENGTH_SHORT).show();
                return;
            }
            showStyleMenu();
        });
        Button settings = button("Tune");
        dockSettingsButton = settings;
        styleDockButton(settings);
        settings.setOnClickListener(v -> showRhymeSettingsMenu());
        dock.addView(notes, dockButtonLp(0));
        dock.addView(rhymes, dockButtonLp(dimen(R.dimen.topflow_space_xs)));
        dock.addView(style, dockButtonLp(dimen(R.dimen.topflow_space_xs)));
        dock.addView(settings, dockButtonLp(dimen(R.dimen.topflow_space_xs)));
        refreshDockVisuals();
        return dock;
    }

    private void updateShellStatus() {
        if (shellStatusLabel == null) return;
        boolean editorVisible = editorPanel != null && editorPanel.getVisibility() == View.VISIBLE;
        String status;
        int accent = C_CYAN;
        if (editorVisible && current != null) {
            status = buildEditorShellStatus(current);
            accent = current.accentColor;
        } else {
            status = buildNotesShellStatus();
            if (current != null) {
                accent = current.accentColor;
            }
        }
        shellStatusLabel.setText(status);
        if (shellSignal != null && shellSignal.getParent() instanceof ViewGroup) {
            ViewGroup signalHost = (ViewGroup) shellSignal.getParent();
            signalHost.removeView(shellSignal);
            shellSignal = buildSessionMiniSignal(accent, 18, true);
            if (shellSignal != null) signalHost.addView(shellSignal);
        }
    }

    private String buildNotesShellStatus() {
        int totalNotes = notes == null ? 0 : notes.size();
        String label = "Notes · " + totalNotes + " session" + (totalNotes == 1 ? "" : "s");
        if (current == null) return label + " · No active session";
        String title = shellTitlePreview(current.title);
        return label + " · Active " + title;
    }

    private String buildEditorShellStatus(Note note) {
        if (note == null) return "Editor open · 0 lines · 0 words";
        String title = shellTitlePreview(note.title);
        String body = bodyInput != null && bodyInput.getText() != null ? bodyInput.getText().toString() : note.body;
        int lines = lyricLineCount(body);
        int words = lyricWordCount(body);
        return "Draft · " + title + " · " + lines + " line" + (lines == 1 ? "" : "s") + " · " + words + " word" + (words == 1 ? "" : "s");
    }

    private String shellTitlePreview(String title) {
        String safeTitle = (title == null || title.trim().isEmpty()) ? "Untitled" : title.trim();
        if (safeTitle.length() <= 28) return safeTitle;
        return safeTitle.substring(0, 25) + "…";
    }

    private LinearLayout.LayoutParams dockButtonLp(int leftMargin) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(52), 1);
        lp.leftMargin = leftMargin;
        return lp;
    }

    private int dockAccent() {
        return current != null ? current.accentColor : C_CYAN;
    }

    private void styleDockButton(Button button) {
        if (button == null) return;
        button.setGravity(Gravity.CENTER);
        button.setMinHeight(dp(52));
        button.setMinimumHeight(dp(52));
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setMaxLines(1);
        button.setSingleLine(true);
        button.setHorizontallyScrolling(false);
        button.setEllipsize(TextUtils.TruncateAt.END);
        button.setIncludeFontPadding(false);
        button.setLetterSpacing(0f);
        button.setPadding(dp(6), dp(5), dp(6), dp(5));
        button.setCompoundDrawablePadding(dp(4));
        button.setStateListAnimator(null);
        applyDockButtonVisual(button, false);
    }

    private void refreshDockVisuals() {
        if (studioDock != null) {
            studioDock.setBackground(dockSurfaceDrawable(dockAccent()));
        }
        applyDockButtonVisual(dockNotesButton, activeDockState == DOCK_STATE_NOTES);
        applyDockButtonVisual(dockRhymeButton, activeDockState == DOCK_STATE_RHYME);
        applyDockButtonVisual(dockStyleButton, activeDockState == DOCK_STATE_STYLE);
        applyDockButtonVisual(dockSettingsButton, activeDockState == DOCK_STATE_SETTINGS);
    }

    private void applyDockButtonVisual(Button button, boolean active) {
        if (button == null) return;
        int accent = dockAccent();
        button.setBackground(dockButtonSurface(accent, active));
        button.setTextColor(active ? TopFlowUiKit.TEXT : TopFlowUiKit.TEXT_SOFT);
        button.setTypeface(android.graphics.Typeface.create("sans-serif-medium", active ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL));
        button.setForeground(TopFlowUiKit.ripple(accent));
        TopFlowUiKit.applyFloating(button, active ? DOCK_STATE_ELEVATION_DP : 3);
        if (Build.VERSION.SDK_INT >= 28) {
            int shadow = active ? Color.argb(150, Color.red(accent), Color.green(accent), Color.blue(accent)) : Color.BLACK;
            button.setOutlineAmbientShadowColor(shadow);
            button.setOutlineSpotShadowColor(shadow);
        }
        applyButtonIcon(button, button.getText() == null ? "" : button.getText().toString());
    }

    private void showStartupSplash() {
        if (root == null) return;
        rhymePreloadComplete = false;
        firstQueryAfterPreloadLogged = false;
        rhymePreloadCompletedAtMs = 0L;
        splashClosing = false;
        splashStartedAtMs = System.currentTimeMillis();
        splashOverlay = new FrameLayout(this);
        splashOverlay.setBackgroundColor(Color.BLACK);
        splashOverlay.setClickable(true);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        box.setPadding(dp(24), dp(22), dp(24), dp(24));

        View signalRail = buildSplashSignalRail();
        box.addView(signalRail, new LinearLayout.LayoutParams(-1, -2));

        TextView intro = new TextView(this);
        intro.setText("THE TOP FLOW");
        intro.setTextColor(TopFlowUiKit.TEXT);
        intro.setTextSize(TypedValue.COMPLEX_UNIT_SP, 36);
        intro.setTypeface(android.graphics.Typeface.create("sans-serif-condensed", android.graphics.Typeface.BOLD));
        intro.setGravity(Gravity.CENTER);
        intro.setLetterSpacing(0f);
        intro.setIncludeFontPadding(false);
        intro.setSingleLine(true);
        intro.setEllipsize(TextUtils.TruncateAt.END);
        intro.setPadding(0, dp(14), 0, dp(8));
        box.addView(intro, new LinearLayout.LayoutParams(-1, -2));

        TextView subtitle = new TextView(this);
        subtitle.setText("Offline rhyme studio");
        subtitle.setTextColor(C_TEXT_MUTED);
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setIncludeFontPadding(false);
        subtitle.setSingleLine(true);
        subtitle.setEllipsize(TextUtils.TruncateAt.END);
        box.addView(subtitle, new LinearLayout.LayoutParams(-1, -2));

        splashStatus = new TextView(this);
        splashStatus.setText(formatSplashStatus("Loading rhyme index"));
        splashStatus.setTextColor(C_TEXT);
        splashStatus.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        splashStatus.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        splashStatus.setGravity(Gravity.CENTER);
        splashStatus.setPadding(0, dp(18), 0, dp(8));
        splashStatus.setIncludeFontPadding(false);
        splashStatus.setMaxLines(1);
        splashStatus.setEllipsize(TextUtils.TruncateAt.END);
        box.addView(splashStatus, new LinearLayout.LayoutParams(-1, -2));

        View signalRow = buildSplashStatusRail();
        box.addView(signalRow);

        FrameLayout track = new FrameLayout(this);
        track.setBackground(trackForSplash());
        track.setPadding(dp(1), dp(1), dp(1), dp(1));
        LinearLayout.LayoutParams trackLp = new LinearLayout.LayoutParams(-1, dp(12));
        trackLp.topMargin = dp(18);
        trackLp.bottomMargin = dp(6);
        box.addView(track, trackLp);
        View fill = new View(this);
        splashFill = fill;
        fill.setBackground(splashFillDrawable());
        fill.setPivotX(0f);
        fill.setScaleX(0f);
        track.addView(fill, new FrameLayout.LayoutParams(-1, -1));

        splashProgressLabel = metadataLabel("Initializing");
        splashProgressLabel.setSingleLine(true);
        splashProgressLabel.setEllipsize(TextUtils.TruncateAt.END);
        splashProgressLabel.setGravity(Gravity.END);
        splashProgressLabel.setPadding(0, 0, dp(2), 0);
        box.addView(splashProgressLabel, new LinearLayout.LayoutParams(-1, -2));

        updateSplashProgressLabel();
        FrameLayout.LayoutParams boxLp = new FrameLayout.LayoutParams(-1, -2, Gravity.CENTER);
        boxLp.leftMargin = dp(24);
        boxLp.rightMargin = dp(24);
        splashOverlay.addView(box, boxLp);
        root.addView(splashOverlay, new FrameLayout.LayoutParams(-1, -1));
        fill.animate().scaleX(0.82f).setDuration(MOTION_SPLASH_FILL_MS).start();
        handler.postDelayed(() -> maybeFinishStartupSplash(false), SPLASH_MIN_MS);
        handler.postDelayed(() -> maybeFinishStartupSplash(true), SPLASH_MAX_MS);
    }

    private void updateSplashStatus(String status) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            editHandler.post(() -> updateSplashStatus(status));
            return;
        }
        if (splashStatus != null) splashStatus.setText(formatSplashStatus(status));
        if (splashFill != null && "Rhyme engine ready".equals(status)) {
            splashFill.animate().scaleX(1f).setDuration(MOTION_SPLASH_READY_FILL_MS).start();
        }
        updateSplashProgressLabel();
    }

    private void maybeFinishStartupSplash(boolean force) {
        if (splashOverlay == null || splashClosing) return;
        long elapsed = System.currentTimeMillis() - splashStartedAtMs;
        if (elapsed < SPLASH_MIN_MS) {
            handler.postDelayed(() -> maybeFinishStartupSplash(force), SPLASH_MIN_MS - elapsed);
            return;
        }
        if (!force && !rhymePreloadComplete && elapsed < SPLASH_MAX_MS) return;
        splashClosing = true;
        if (splashFill != null) splashFill.animate().scaleX(1f).setDuration(MOTION_SPLASH_CLOSE_FILL_MS).start();
        splashOverlay.animate().alpha(0f).setDuration(MOTION_SPLASH_CLOSE_MS).withEndAction(() -> {
            if (root != null && splashOverlay != null) root.removeView(splashOverlay);
            splashOverlay = null;
            splashStatus = null;
            splashProgressLabel = null;
            splashFill = null;
        }).start();
    }

    private View buildSplashSignalRail() {
        LinearLayout rail = new LinearLayout(this);
        rail.setOrientation(LinearLayout.HORIZONTAL);
        rail.setGravity(Gravity.CENTER);
        rail.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        for (int i = 0; i < 7; i++) {
            View node = new View(this);
            int alpha = i % 2 == 0 ? 210 : 120;
            node.setBackground(topflowDimLine(Color.argb(alpha, 90, 215, 255)));
            int h = i == 3 ? dp(46) : dp(16 + (i % 3) * 4);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, h, 1f);
            lp.rightMargin = i == 6 ? 0 : dp(4);
            node.setLayoutParams(lp);
            rail.addView(node);
        }
        rail.setPadding(0, dp(2), 0, dp(8));
        return rail;
    }

    private View buildSplashStatusRail() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(0, 0, 0, dp(8));
        for (int i = 0; i < 3; i++) {
            View segment = new View(this);
            segment.setBackground(topflowDimLine(i == 0 ? C_CYAN : C_TEXT_MUTED));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(3), 1f);
            lp.rightMargin = i == 2 ? 0 : dp(10);
            segment.setLayoutParams(lp);
            bar.addView(segment);
        }
        return bar;
    }

    private Drawable trackForSplash() {
        GradientDrawable d = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{
                        Color.argb(64, 20, 24, 36),
                        Color.argb(120, 24, 32, 46),
                        Color.argb(64, 20, 24, 36)
                }
        );
        d.setCornerRadius(dp(10));
        d.setStroke(dp(1), Color.argb(124, Color.red(C_CYAN), Color.green(C_CYAN), Color.blue(C_CYAN)));
        return d;
    }

    private Drawable splashFillDrawable() {
        GradientDrawable d = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{
                        Color.argb(240, Math.max(0, Color.red(C_CYAN) - 12), Math.max(0, Color.green(C_CYAN) - 12), Math.max(0, Color.blue(C_CYAN) - 8)),
                        C_CYAN,
                        Color.argb(240, Math.max(0, Color.red(C_GREEN) + 12), Math.max(0, Color.green(C_GREEN) + 5), Math.max(0, Color.blue(C_GREEN) + 2))
                }
        );
        d.setCornerRadius(dp(8));
        return d;
    }

    private String formatSplashStatus(String status) {
        if ("Loading rhyme index".equals(status)) return "Loading index";
        if ("Warming common rhymes".equals(status)) return "Warming cache";
        if ("Rhyme engine ready".equals(status)) return "Ready";
        return status;
    }

    private void updateSplashProgressLabel() {
        if (splashProgressLabel == null) return;
        if (splashStatus == null) {
            splashProgressLabel.setText("Loading");
            return;
        }
        String text = splashStatus.getText().toString();
        if (text == null || text.isEmpty()) {
            splashProgressLabel.setText("Loading");
            return;
        }
        if (text.contains("index")) {
            splashProgressLabel.setText("Index");
        } else if (text.contains("cache")) {
            splashProgressLabel.setText("Cache");
        } else if (text.contains("Ready")) {
            splashProgressLabel.setText("Ready");
        } else {
            splashProgressLabel.setText("Starting");
        }
    }

    private Drawable topflowDimLine(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(Color.argb(140, Color.red(color), Color.green(color), Color.blue(color)));
        d.setCornerRadius(dp(999));
        return d;
    }

    private void renderNoteList() {
        noteList.removeAllViews();
        LinearLayout header = buildNotesCommandHeader();
        LinearLayout.LayoutParams headerLp = new LinearLayout.LayoutParams(-1, -2);
        headerLp.bottomMargin = dimen(R.dimen.topflow_space_sm);
        noteList.addView(header, headerLp);
        if (notes.isEmpty()) {
            noteList.addView(buildEmptyNotesState(), new LinearLayout.LayoutParams(-1, -2));
            return;
        }
        for (int i = 0; i < notes.size(); i++) {
            Note note = notes.get(i);
            View row = buildNoteRow(note, note == current);
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
        updateShellStatus();
    }

    private LinearLayout buildNotesCommandHeader() {
        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.HORIZONTAL);
        shell.setGravity(Gravity.CENTER_VERTICAL);
        shell.setPadding(dimen(R.dimen.topflow_space_lg), dimen(R.dimen.topflow_space_md), dimen(R.dimen.topflow_space_md), dimen(R.dimen.topflow_space_md));
        int accent = current != null ? current.accentColor : C_CYAN;
        shell.setBackground(oledCommandSurface(18, accent, current != null));
        TopFlowUiKit.applyFloating(shell, current != null ? 8 : 4);

        View rail = new View(this);
        rail.setBackground(commandRailDrawable(accent));
        LinearLayout.LayoutParams railLp = new LinearLayout.LayoutParams(dp(4), dp(58));
        railLp.rightMargin = dimen(R.dimen.topflow_space_md);
        shell.addView(rail, railLp);

        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);
        left.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));

        TextView eyebrow = sheetEyebrow(current != null ? "Current session" : "Session dashboard");
        left.addView(eyebrow);

        TextView title = new TextView(this);
        title.setText(current == null ? "No note open" : noteTitleForDisplay(current));
        title.setTextColor(TopFlowUiKit.TEXT);
        title.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.topflow21_text_title));
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setIncludeFontPadding(false);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        left.addView(title);

        int noteCount = notes.size();
        String countLine = noteCount + " saved session" + (noteCount == 1 ? "" : "s");
        TextView sub = new TextView(this);
        sub.setText(current == null ? countLine : noteMetadataLine(current) + "  -  " + countLine);
        sub.setTextColor(TopFlowUiKit.TEXT_SOFT);
        sub.setIncludeFontPadding(false);
        sub.setLetterSpacing(0f);
        sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        sub.setSingleLine(true);
        sub.setEllipsize(TextUtils.TruncateAt.END);
        sub.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        sub.setPadding(0, dp(4), 0, 0);
        left.addView(sub);

        if (current != null) {
            TextView openTag = new TextView(this);
            openTag.setText("Active");
            openTag.setTextColor(TopFlowUiKit.MINT);
            openTag.setTextSize(10);
            openTag.setPadding(dp(8), dp(3), dp(8), dp(3));
            openTag.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            openTag.setBackgroundResource(R.drawable.bg_chip);
            openTag.setSingleLine(true);
            openTag.setEllipsize(TextUtils.TruncateAt.END);
            openTag.setIncludeFontPadding(false);
            openTag.setMaxWidth(dp(220));
            LinearLayout.LayoutParams tagLp = new LinearLayout.LayoutParams(-2, -2);
            tagLp.topMargin = dp(6);
            left.addView(openTag, tagLp);
        }

        shell.addView(left);

        View signal = buildSessionMiniSignal(accent, 7, true);
        if (signal != null) {
            LinearLayout.LayoutParams signalLp = new LinearLayout.LayoutParams(dp(42), dp(34));
            signalLp.leftMargin = dimen(R.dimen.topflow_space_sm);
            shell.addView(signal, signalLp);
        }

        Button add = button("+ Note");
        add.setOnClickListener(v -> {
            Note n = Note.create("Untitled");
            notes.add(0, n);
            saveNotes();
            renderNoteList();
            openNote(n);
        });
        add.setBackgroundResource(R.drawable.bg21_mint_control);
        add.setPadding(dimen(R.dimen.topflow_space_md), dimen(R.dimen.topflow_space_sm), dimen(R.dimen.topflow_space_md), dimen(R.dimen.topflow_space_sm));
        add.setMinHeight(dp(46));
        add.setLetterSpacing(0f);
        add.setSingleLine(true);
        add.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams addLp = new LinearLayout.LayoutParams(dp(112), -2);
        addLp.leftMargin = dp(8);
        shell.addView(add, addLp);
        return shell;
    }

    private View buildRecentSessionPreview() {
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        ArrayList<Note> recentNotes = collectRecentSessionPreviewNotes();
        if (recentNotes.isEmpty()) {
            TextView empty = label("No recent sessions");
            empty.setTextColor(C_TEXT_MUTED);
            empty.setMaxLines(1);
            empty.setEllipsize(TextUtils.TruncateAt.END);
            empty.setPadding(0, 0, 0, dimen(R.dimen.topflow_space_sm));
            list.addView(empty);
            return list;
        }
        for (int i = 0; i < recentNotes.size(); i++) {
            Note note = recentNotes.get(i);
            LinearLayout row = buildRecentSessionRow(note, note == current);
            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, -2);
            if (i + 1 < recentNotes.size()) rowLp.bottomMargin = dimen(R.dimen.topflow_space_xs);
            list.addView(row, rowLp);
        }
        return list;
    }

    private ArrayList<Note> collectRecentSessionPreviewNotes() {
        ArrayList<Note> recentNotes = new ArrayList<>();
        if (notes == null || notes.isEmpty()) return recentNotes;
        int limit = Math.max(1, SHEET_MENU_PREVIEW_LIMIT);
        if (current != null) recentNotes.add(current);
        for (int i = 0; i < notes.size() && recentNotes.size() < limit; i++) {
            Note note = notes.get(i);
            if (note == null) continue;
            if (current != null && note == current) continue;
            recentNotes.add(note);
        }
        return recentNotes;
    }

    private LinearLayout buildRecentSessionRow(Note note, boolean isCurrent) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dimen(R.dimen.topflow_space_md), dimen(R.dimen.topflow_space_sm), dimen(R.dimen.topflow_space_md), dimen(R.dimen.topflow_space_sm));
        int accent = note == null ? C_CYAN : note.accentColor;
        row.setBackground(oledCommandSurface(14, accent, isCurrent));
        TopFlowUiKit.applyFloating(row, isCurrent ? 8 : 3);
        row.setForeground(TopFlowUiKit.ripple(isCurrent ? TopFlowUiKit.MINT : C_CYAN));
        row.setClickable(true);
        row.setFocusable(true);
        attachTapAnimation(row);

        TextView title = new TextView(this);
        title.setText(noteTitleForDisplay(note));
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setTextColor(isCurrent ? TopFlowUiKit.MINT : TopFlowUiKit.TEXT);
        title.setIncludeFontPadding(false);
        title.setLetterSpacing(0f);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);

        TextView meta = new TextView(this);
        meta.setText(noteMetadataLine(note));
        meta.setTextColor(TopFlowUiKit.TEXT_SOFT);
        meta.setIncludeFontPadding(false);
        meta.setLetterSpacing(0f);
        meta.setSingleLine(true);
        meta.setEllipsize(TextUtils.TruncateAt.END);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(title, new LinearLayout.LayoutParams(0, -2, 1));

        if (isCurrent) {
            TextView tag = new TextView(this);
            tag.setText("Current");
            tag.setTextColor(C_CYAN);
            tag.setTextSize(10);
            tag.setPadding(dp(8), dp(3), dp(8), dp(3));
            tag.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            tag.setBackgroundResource(R.drawable.bg_chip);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
            lp.leftMargin = dp(6);
            header.addView(tag, lp);
        }

        row.addView(header);
        row.addView(meta);
        if (note == null) return row;
        row.setOnClickListener(v -> runSelectionAnimation(row, () -> runAfterMenuDismiss(() -> openNote(note))));
        return row;
    }

    private View buildEmptyNotesState() {
        LinearLayout empty = new LinearLayout(this);
        empty.setOrientation(LinearLayout.VERTICAL);
        empty.setGravity(Gravity.CENTER_VERTICAL);
        empty.setPadding(dimen(R.dimen.topflow_space_xl), dimen(R.dimen.topflow_space_xl), dimen(R.dimen.topflow_space_xl), dimen(R.dimen.topflow_space_xl));
        empty.setBackground(oledCommandSurface(18, C_CYAN, false));
        TopFlowUiKit.applyFloating(empty, 4);

        TextView title = label("No notes yet");
        textStyle(title, R.style.TextAppearance_TopFlow21_Section);
        title.setTextColor(TopFlowUiKit.TEXT);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setIncludeFontPadding(false);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        TextView body = label("No saved sessions");
        textStyle(body, R.style.TextAppearance_TopFlow21_Caption);
        body.setTextColor(TopFlowUiKit.TEXT_SOFT);
        body.setPadding(0, dimen(R.dimen.topflow_space_md), 0, 0);
        body.setSingleLine(false);
        body.setEllipsize(TextUtils.TruncateAt.END);
        body.setMaxLines(2);
        body.setLineSpacing(0f, 0.95f);
        body.setIncludeFontPadding(false);
        empty.addView(title);
        empty.addView(body);

        LinearLayout footer = new LinearLayout(this);
        footer.setOrientation(LinearLayout.HORIZONTAL);
        footer.setGravity(Gravity.CENTER_VERTICAL);
        footer.setPadding(0, dimen(R.dimen.topflow_space_md), 0, 0);
        TextView label = label("Session signal");
        label.setTextColor(TopFlowUiKit.TEXT_SOFT);
        label.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        label.setTextSize(12);
        label.setIncludeFontPadding(false);
        footer.addView(label);
        footer.addView(buildSessionMiniSignal(C_CYAN, 12, true));
        empty.addView(footer);
        return empty;
    }

    private View buildNoteRow(Note note, boolean selected) {
        int accent = note == null ? C_CYAN : note.accentColor;
        String titleText = noteTitleForDisplay(note);
        String previewText = compactPreview(note == null ? null : note.body);
        String metaText = noteMetadataLine(note);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dimen(R.dimen.topflow_space_lg), dimen(R.dimen.topflow_space_md), dimen(R.dimen.topflow_space_lg), dimen(R.dimen.topflow_space_md));
        row.setMinimumHeight(dp(94));
        row.setBackground(noteRowBackground(note, selected));
        TopFlowUiKit.applyFloating(row, selected ? 7 : 3);
        row.setForeground(TopFlowUiKit.ripple(accent));
        row.setClickable(true);
        row.setFocusable(true);
        attachTapAnimation(row);

        View edge = new View(this);
        edge.setBackground(commandRailDrawable(selected ? TopFlowUiKit.MINT : accent));
        edge.setAlpha(selected ? 1f : 0.96f);
        LinearLayout.LayoutParams edgeLp = new LinearLayout.LayoutParams(selected ? dp(5) : dp(4), dp(56));
        edgeLp.rightMargin = dp(10);
        row.addView(edge, edgeLp);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = new TextView(this);
        title.setText(titleText);
        title.setTextColor(selected ? TopFlowUiKit.MINT : TopFlowUiKit.TEXT);
        textStyle(title, R.style.TextAppearance_TopFlow21_Section);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setIncludeFontPadding(false);
        title.setLetterSpacing(0f);
        title.setMaxLines(1);
        title.setEllipsize(TextUtils.TruncateAt.END);

        top.addView(title, new LinearLayout.LayoutParams(0, -2, 1));

        if (selected) {
            TextView currentTag = new TextView(this);
            currentTag.setText("Active");
            currentTag.setTextColor(TopFlowUiKit.MINT);
            currentTag.setTextSize(10);
            currentTag.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            currentTag.setBackgroundResource(R.drawable.bg_chip);
            currentTag.setPadding(dp(8), dp(3), dp(8), dp(3));
            currentTag.setIncludeFontPadding(false);
            currentTag.setSingleLine(true);
            currentTag.setEllipsize(TextUtils.TruncateAt.END);
            currentTag.setMaxWidth(dp(76));
            LinearLayout.LayoutParams tagLp = new LinearLayout.LayoutParams(-2, -2);
            tagLp.leftMargin = dp(8);
            top.addView(currentTag, tagLp);
        }

        box.addView(top);

        TextView preview = new TextView(this);
        preview.setText(previewText);
        textStyle(preview, R.style.TextAppearance_TopFlow21_Caption);
        preview.setTextColor(TopFlowUiKit.TEXT_SOFT);
        preview.setIncludeFontPadding(false);
        preview.setLetterSpacing(0f);
        preview.setSingleLine(true);
        preview.setEllipsize(TextUtils.TruncateAt.END);
        preview.setMaxLines(1);
        preview.setPadding(0, dp(3), 0, 0);

        TextView meta = new TextView(this);
        textStyle(meta, R.style.TextAppearance_TopFlow21_Caption);
        meta.setText(metaText);
        meta.setTextColor(TopFlowUiKit.TEXT_SOFT);
        meta.setIncludeFontPadding(false);
        meta.setLetterSpacing(0f);
        meta.setMaxLines(1);
        meta.setEllipsize(TextUtils.TruncateAt.END);
        meta.setPadding(0, dp(2), 0, 0);
        box.addView(preview);
        box.addView(meta);
        row.addView(box, new LinearLayout.LayoutParams(0, -2, 1));

        View signal = buildSessionMiniSignal(accent, 6, false);
        if (signal != null) {
            LinearLayout.LayoutParams signalLp = new LinearLayout.LayoutParams(dp(34), dp(42));
            signalLp.leftMargin = dp(12);
            row.addView(signal, signalLp);
        }

        row.setOnClickListener(v -> runSelectionAnimation(row, () -> openNote(note)));
        return row;
    }

    private View buildSessionMiniSignal(int accent, int barCount, boolean compact) {
        LinearLayout bars = new LinearLayout(this);
        bars.setOrientation(LinearLayout.HORIZONTAL);
        bars.setGravity(Gravity.BOTTOM | Gravity.END);
        bars.setLayoutParams(new LinearLayout.LayoutParams(-2, compact ? dp(30) : dp(42)));
        bars.setPadding(0, 0, 0, 0);

        int count = Math.max(5, Math.min(compact ? 18 : 12, barCount));
        int minHeight = compact ? 4 : 6;
        int maxExtra = compact ? 6 : 11;
        int barWidth = compact ? 3 : 3;
        int gap = compact ? 2 : 2;

        for (int i = 0; i < count; i++) {
            View bar = new View(this);
            int jitter = Math.floorMod(accent * (i + 1) + 57 * (i + 3), maxExtra + 1);
            int h = minHeight + jitter;
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(barWidth), dp(h));
            if (i > 0) lp.leftMargin = dp(gap);

            int baseAlpha = compact ? 164 : 208;
            int fade = Math.max(44, 196 - (i * (compact ? 5 : 8)));
            int top = blendColor(accent, Color.BLACK, 0.2f);
            int bottom = Color.argb(Math.max(100, baseAlpha - fade / 2), Color.red(accent), Color.green(accent), Color.blue(accent));
            GradientDrawable bg = new GradientDrawable(
                    GradientDrawable.Orientation.BOTTOM_TOP,
                    new int[]{top, bottom}
            );
            bg.setCornerRadius(dp(2));
            bar.setBackground(bg);
            bar.setAlpha((compact ? 0.78f : 0.88f));
            bars.addView(bar, lp);
        }
        return bars;
    }

    private Drawable noteRowBackground(Note note, boolean selected) {
        int accent = selected ? TopFlowUiKit.MINT : (note == null ? C_CYAN : note.accentColor);
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.BLACK);
        drawable.setCornerRadius(dp(18));
        drawable.setStroke(dp(1), Color.argb(selected ? 170 : 86, Color.red(accent), Color.green(accent), Color.blue(accent)));
        return drawable;
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
        saveCurrentDraft();
        current = note;
        suppressSave = true;
        titleInput.setText(note.title);
        bodyInput.setText(note.body);
        String[] fonts = TopFlowUiKit.fontPreviewIds();
        for (int i = 0; i < fonts.length; i++) {
            if (fonts[i].equals(note.font)) fontSpinner.setSelection(i);
        }
        bodyDraftDirty = false;
        suppressSave = false;
        showEditorScreen();
        applyStyle();
        renderRecordings();
        scheduleSuggestionUpdate();
        updatePlaybackStatus();
        updateShellStatus();
    }

    private void showMenuScreen() {
        dismissSuggestionPopup();
        if (songCard != null) songCard.setVisibility(View.VISIBLE);
        if (voiceCard != null) voiceCard.setVisibility(View.VISIBLE);
        animatePanel(menuPanel, true);
        animatePanel(editorPanel, false);
        setActiveDockState(DOCK_STATE_NOTES);
        renderNoteList();
        updateShellStatus();
    }

    private void showEditorScreen() {
        if (songCard != null) songCard.setVisibility(View.GONE);
        if (voiceCard != null) voiceCard.setVisibility(View.GONE);
        animatePanel(editorPanel, true);
        animatePanel(menuPanel, false);
        setActiveDockState(DOCK_STATE_NONE);
        updateShellStatus();
    }

    private void animatePanel(View panel, boolean show) {
        if (panel == null) return;
        panel.animate().cancel();
        if (show) {
            panel.setVisibility(View.VISIBLE);
            panel.setAlpha(0f);
            panel.setScaleX(0.98f);
            panel.setScaleY(0.98f);
            panel.setTranslationY(dp(14));
            withWorkflowMotion(panel)
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .translationY(0f)
                    .setDuration(MOTION_PANEL_SHOW_MS)
                    .start();
        } else {
            withWorkflowMotion(panel)
                    .alpha(0f)
                    .scaleX(0.985f)
                    .scaleY(0.985f)
                    .translationY(dp(-10))
                    .setDuration(MOTION_PANEL_HIDE_MS)
                    .withEndAction(() -> {
                        panel.setVisibility(View.GONE);
                        resetSwipePanelState(panel);
                    })
                    .start();
        }
    }

    private void resetSwipePanelState(View panel) {
        if (panel == null) return;
        panel.setTranslationX(0f);
        panel.setTranslationY(0f);
        panel.setScaleX(1f);
        panel.setScaleY(1f);
        panel.setAlpha(1f);
        hideSwipeAffordanceForPanel(panel);
    }

    private void settleSwipePanel(View panel) {
        if (panel == null) return;
        panel.animate().cancel();
        withWorkflowMotion(panel)
                .translationX(0f)
                .translationY(0f)
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(MOTION_SWIPE_RESTORE_MS)
                .withEndAction(() -> resetSwipePanelState(panel))
                .start();
    }

    private void completeSwipePanel(View panel, boolean toRight, Runnable onComplete) {
        if (panel == null) return;
        int width = panel.getWidth();
        if (width <= 0) width = getResources().getDisplayMetrics().widthPixels;
        float delta = toRight ? width : -width;
        panel.animate().cancel();
        withWorkflowMotion(panel)
                .translationX(delta)
                .alpha(0f)
                .scaleX(0.94f)
                .scaleY(0.95f)
                .setDuration(MOTION_SWIPE_COMPLETE_MS)
                .withEndAction(() -> {
                    hideSwipeAffordanceForPanel(panel);
                    resetSwipePanelState(panel);
                    if (onComplete != null) onComplete.run();
                })
                .start();
    }

    private void animateScreenSwap(View firstVisible, float emphasis) {
        if (firstVisible == null) return;
        firstVisible.setAlpha(0f);
        firstVisible.setTranslationY(dp(10));
        firstVisible.setScaleX(0.985f);
        firstVisible.setScaleY(0.985f);
        withWorkflowMotion(firstVisible)
                .alpha(1f)
                .translationY(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration((long) (MOTION_PANEL_SWAP_MS * emphasis))
                .start();
    }

    private FrameLayout.LayoutParams swipeRailLayoutParams(boolean rightAligned) {
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(dp(WORKFLOW_SWIPE_RAIL_WIDTH_DP), -1);
        lp.gravity = rightAligned ? (Gravity.END | Gravity.TOP) : (Gravity.START | Gravity.TOP);
        return lp;
    }

    private View createSwipeRail(boolean rightAligned) {
        View rail = new View(this);
        rail.setBackground(swipeRailDrawable(dockAccent()));
        rail.setAlpha(0f);
        rail.setScaleX(0.72f);
        rail.setScaleY(0.15f);
        rail.setVisibility(View.GONE);
        rail.setRotationY(rightAligned ? 180f : 0f);
        return rail;
    }

    private void updateSwipeAffordance(View rail, float progress) {
        if (rail == null) return;
        float clamped = Math.max(0f, Math.min(1f, progress));
        if (clamped <= 0f) {
            rail.setVisibility(View.GONE);
            rail.setAlpha(0f);
            rail.setScaleY(0.15f);
            return;
        }
        rail.setVisibility(View.VISIBLE);
        rail.setAlpha(0.18f + (0.72f * clamped));
        rail.setScaleX(0.72f + (0.28f * clamped));
        rail.setScaleY(Math.max(0.15f, clamped));
    }

    private void hideSwipeAffordance(View rail) {
        if (rail == null) return;
        rail.animate().cancel();
        rail.setVisibility(View.GONE);
        rail.setAlpha(0f);
        rail.setScaleX(0.72f);
        rail.setScaleY(0.15f);
    }

    private void hideSwipeAffordanceForPanel(View panel) {
        if (panel == null) return;
        if (panel == editorPanel) {
            hideSwipeAffordance(editorSwipeRail);
        } else if (panel == menuPanel) {
            hideSwipeAffordance(notesSwipeRail);
        } else {
            hideSwipeAffordance(editorSwipeRail);
            hideSwipeAffordance(notesSwipeRail);
        }
    }

    private void applyStyle() {
        if (current == null) return;
        titleInput.setBackground(editorFieldDrawable(current.noteColor, current.accentColor, 18));
        bodyInput.setBackground(editorFieldDrawable(current.noteColor, current.accentColor, 22));
        titleInput.setTextColor(current.textColor);
        bodyInput.setTextColor(current.textColor);
        titleInput.setHintTextColor(current.accentColor);
        bodyInput.setHintTextColor(current.accentColor);
        titleInput.setTypeface(TopFlowUiKit.fontForEditor(this, current.font, android.graphics.Typeface.BOLD));
        bodyInput.setTypeface(TopFlowUiKit.fontForEditor(this, current.font, android.graphics.Typeface.NORMAL));
        titleInput.setLetterSpacing(0f);
        bodyInput.setLetterSpacing(0f);
        titleInput.setTextSize(TypedValue.COMPLEX_UNIT_SP, Math.max(18, current.fontSizeSp + 5));
        bodyInput.setTextSize(TypedValue.COMPLEX_UNIT_SP, current.fontSizeSp);
        if (Build.VERSION.SDK_INT >= 29) titleInput.setTextCursorDrawable(null);
        styleActionButtonPalette(current.accentColor);
        refreshDockVisuals();
        if (editorSignalRail != null) {
            editorSignalRail.setBackground(commandRailDrawable(current.accentColor));
        }
        if (editorSwipeRail != null) {
            editorSwipeRail.setBackground(swipeRailDrawable(current.accentColor));
        }
        if (notesSwipeRail != null) {
            notesSwipeRail.setBackground(swipeRailDrawable(current.accentColor));
        }
        if (editorHeaderTitle != null) {
            editorHeaderTitle.setText(noteTitleForDisplay(current));
            editorHeaderTitle.setTextColor(TopFlowUiKit.TEXT);
            editorHeaderTitle.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            editorHeaderTitle.setLetterSpacing(0f);
            editorHeaderTitle.setIncludeFontPadding(false);
        }
        if (editorHeaderMeta != null) {
            String meta = "Draft body  -  " + noteMetadataLine(current);
            editorHeaderMeta.setText(meta);
            editorHeaderMeta.setTextColor(TopFlowUiKit.TEXT_SOFT);
            editorHeaderMeta.setIncludeFontPadding(false);
            editorHeaderMeta.setLetterSpacing(0f);
        }
        if (editorChrome != null && editorMiniSignal != null && editorChrome.indexOfChild(editorMiniSignal) >= 0) {
            editorChrome.removeView(editorMiniSignal);
        }
        if (editorChrome != null) {
            editorMiniSignal = buildSessionMiniSignal(current.accentColor, 7, true);
            if (editorMiniSignal != null) {
                LinearLayout.LayoutParams miniLp = new LinearLayout.LayoutParams(dp(42), dp(34));
                miniLp.leftMargin = dp(8);
                editorChrome.addView(editorMiniSignal, miniLp);
            }
        }
        if (editorGlowFrame != null) {
            editorGlowFrame.setBackground(noteGlowDrawable(current.accentColor, current.noteGlow, current.glowStrength));
        }
        if (editorCard != null) {
            editorCard.setBackground(noteShellDrawable(current.accentColor, current.noteGlow, current.glowStrength));
            int elevation = current.noteGlow ? 10 + Math.max(0, current.glowStrength) * 3 : 4;
            TopFlowUiKit.applyFloating(editorCard, elevation);
            if (Build.VERSION.SDK_INT >= 28) {
                int shadow = current.noteGlow ? Color.argb(180, Color.red(current.accentColor), Color.green(current.accentColor), Color.blue(current.accentColor)) : Color.BLACK;
                editorCard.setOutlineAmbientShadowColor(shadow);
                editorCard.setOutlineSpotShadowColor(shadow);
            }
        }
        if (suggestionPanel != null) suggestionPanel.setBackground(oledCommandSurface(18, current.accentColor, true));
        if (songCard != null) songCard.setBackground(oledCommandSurface(18, current.accentColor, false));
        if (voiceCard != null) voiceCard.setBackground(oledCommandSurface(18, current.accentColor, false));
        if (colorPreview != null) colorPreview.setBackgroundColor(current.noteColor);
        if (playbackStatus != null) playbackStatus.setTextColor(current.accentColor);
        if (songStatus != null) songStatus.setTextColor(TopFlowUiKit.TEXT_SOFT);
        if (recordingStatus != null) recordingStatus.setTextColor(recorder != null ? C_RED : TopFlowUiKit.TEXT_SOFT);
        if (songSeek != null) styleSeekBar(songSeek, current.accentColor);
        if (bodyInput != null) {
            if (bodyInput instanceof RuledEditText) {
                ((RuledEditText) bodyInput).setRuleColor(current.accentColor);
            }
        }
        renderNoteList();
        renderRecordings();
        updateMediaLabels();
        scheduleSuggestionUpdate();
        updateShellStatus();
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
        wrap.setPadding(dimen(R.dimen.topflow_space_lg), dimen(R.dimen.topflow_space_md), dimen(R.dimen.topflow_space_lg), dimen(R.dimen.topflow_space_lg));
        wrap.setBackground(oledCommandSurface(18, C_CYAN, true));
        TopFlowUiKit.applyFloating(wrap, 10);
        LinearLayout heading = new LinearLayout(this);
        heading.setOrientation(LinearLayout.HORIZONTAL);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        View rail = new View(this);
        rail.setBackground(commandRailDrawable(C_CYAN));
        LinearLayout.LayoutParams railLp = new LinearLayout.LayoutParams(dp(3), dp(18));
        railLp.rightMargin = dimen(R.dimen.topflow_space_sm);
        heading.addView(rail, railLp);
        TextView label = new TextView(this);
        label.setText(title == null || title.trim().isEmpty() ? "Rhymes" : title);
        textStyle(label, R.style.TextAppearance_TopFlow21_Caption);
        label.setTextColor(TopFlowUiKit.MINT);
        label.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        label.setIncludeFontPadding(false);
        label.setLetterSpacing(0f);
        label.setSingleLine(true);
        label.setEllipsize(TextUtils.TruncateAt.END);
        heading.addView(label, new LinearLayout.LayoutParams(0, -2, 1));
        LinearLayout.LayoutParams headingLp = new LinearLayout.LayoutParams(-1, -2);
        headingLp.bottomMargin = dimen(R.dimen.topflow_space_sm);
        wrap.addView(heading, headingLp);
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
        long started = System.currentTimeMillis();
        suggestionRequestId++;
        cancelSuggestionJob();
        editHandler.removeCallbacks(suggestionRunnable);
        editHandler.postDelayed(suggestionRunnable, SUGGESTION_DEBOUNCE_MS);
        if (editorTextLength() > 3000) {
            logRhymeTrace("fast_schedule", started, "request=" + suggestionRequestId + " noteLen=" + editorTextLength() + " cursor=" + safeEditorCursor());
        }
    }

    private void scheduleDraftSave() {
        editHandler.removeCallbacks(saveDraftRunnable);
        editHandler.postDelayed(saveDraftRunnable, 250);
    }

    private void configureEditorInput(EditText input) {
        input.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        input.setImeOptions(EditorInfo.IME_ACTION_NONE
                | EditorInfo.IME_FLAG_NO_EXTRACT_UI
                | EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING);
        input.setSingleLine(false);
        input.setHorizontallyScrolling(false);
        input.setSaveEnabled(false);
        if (Build.VERSION.SDK_INT >= 26) {
            input.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS);
            input.setTextClassifier(TextClassifier.NO_OP);
        }
        logRhymeTrace("editor_services", 0L, "suggestions=off autofill=off textClassifier=noop");
        input.setOnFocusChangeListener((v, hasFocus) -> {
            if (lastEditorFocus != hasFocus) {
                lastEditorFocus = hasFocus;
                logRhymeTrace("editor_focus", 0L, "focus=" + hasFocus + " ime=" + lastImeVisible + " noteLen=" + editorTextLength());
            }
        });
    }

    private void updateSuggestionPopup() {
        long started = System.currentTimeMillis();
        if (suggestionPanel == null || bodyInput == null) return;
        if (!prefs.getBoolean(PREF_SHOW_RHYME_ROW, true)) {
            pendingSuggestionKey = "";
            dismissSuggestionPopup();
            logRhymeTrace("fast_disabled", started, "noteLen=" + editorTextLength() + " cursor=" + safeEditorCursor());
            return;
        }
        CharSequence text = bodyInput.getText() == null ? "" : bodyInput.getText();
        int cursor = Math.max(0, Math.min(bodyInput.getSelectionStart(), text.length()));
        long tokenStarted = System.currentTimeMillis();
        TokenInfo info = currentToken(text, cursor);
        if (info.word.isEmpty() && cursor > 0) {
            info = previousToken(text, cursor);
        }
        long tokenMs = System.currentTimeMillis() - tokenStarted;
        if (info.word.length() < 2) {
            pendingSuggestionKey = "";
            suggestionStart = -1;
            suggestionEnd = -1;
            dismissSuggestionPopup();
            logRhymeTrace("fast_no_token", started, "tokenMs=" + tokenMs + " noteLen=" + text.length() + " cursor=" + cursor);
            return;
        }
        int limit = Math.min(FAST_RHYME_LIMIT, configuredMaxSuggestions());
        String query = normalizeWord(info.word);
        String cacheKey = rhymeCacheKey(query, limit);
        pendingSuggestionKey = cacheKey;
        logRhymeTrace("fast_start", started, "word=" + query + " request=" + suggestionRequestId + " tokenMs=" + tokenMs + " noteLen=" + text.length() + " cursor=" + cursor + " ready=" + rhymeEngine.isReady());
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
        long cacheStarted = System.currentTimeMillis();
        ArrayList<String> cached = cachedRhymes(cacheKey);
        long cacheMs = System.currentTimeMillis() - cacheStarted;
        int requestId = suggestionRequestId;
        if (cached != null) {
            logRhymeTrace("fast_cache_hit", started, "word=" + query + " cacheMs=" + cacheMs + " count=" + cached.size() + " noteLen=" + text.length() + " cursor=" + cursor);
            applySuggestionResults(requestId, cacheKey, info.start, info.end, cursor, cached, true, 0L, started);
            return;
        }
        logRhymeTrace("fast_cache_miss", started, "word=" + query + " cacheMs=" + cacheMs + " noteLen=" + text.length() + " cursor=" + cursor);
        if (!cacheKey.equals(lastRenderedSuggestionKey)) {
            suggestionStart = info.start;
            suggestionEnd = info.end;
            renderSuggestionStatus(rhymeChips, "Finding rhymes");
            lastRenderedSuggestionKey = cacheKey;
            lastRenderedRhymes = new ArrayList<>();
            positionSuggestionPopup(cursor);
        }
        startSuggestionJob(requestId, cacheKey, info.word, info.start, info.end, cursor, limit, text.length(), started);
    }

    private void startSuggestionJob(int requestId, String cacheKey, String word, int start, int end, int cursor, int limit, int noteLen, long requestStartedAt) {
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
                    Log.d(TAG, "rhyme_trace stage=fast_generate ms=" + generateMs
                            + " thread=bg word=" + normalizeWord(word)
                            + " count=" + rhymes.size()
                            + " cache=" + cacheHit
                            + " noteLen=" + noteLen
                            + " cursor=" + cursor);
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
        logFirstQueryAfterPreload(System.currentTimeMillis() - requestStartedAt, generateMs, cacheHit, rhymes.size());
        if (rhymes.isEmpty()) {
            dismissSuggestionPopup();
            return;
        }
        if (!cacheKey.equals(lastRenderedSuggestionKey) || !sameWords(lastRenderedRhymes, rhymes)) {
            long renderStarted = System.currentTimeMillis();
            renderSuggestionChips(rhymeChips, rhymes);
            long renderMs = System.currentTimeMillis() - renderStarted;
            lastRenderedSuggestionKey = cacheKey;
            lastRenderedRhymes = new ArrayList<>(rhymes);
            logRhymeTrace("fast_render", renderStarted, "count=" + rhymes.size() + " renderMs=" + renderMs + " noteLen=" + editorTextLength() + " cursor=" + cursor);
        }
        positionSuggestionPopup(cursor);
        long uiMs = System.currentTimeMillis() - uiStarted;
        long totalMs = System.currentTimeMillis() - requestStartedAt;
        if (uiMs >= 8L || generateMs >= 16L || totalMs >= 80L) {
            Log.d(TAG, "rhyme ui count=" + rhymes.size() + " uiMs=" + uiMs + " genMs=" + generateMs + " totalMs=" + totalMs + " cache=" + cacheHit);
        }
    }

    private void logFirstQueryAfterPreload(long totalMs, long generateMs, boolean cacheHit, int count) {
        if (rhymePreloadCompletedAtMs <= 0L || firstQueryAfterPreloadLogged) return;
        firstQueryAfterPreloadLogged = true;
        Log.d(TAG, "rhyme_trace stage=first_query_after_preload ms=" + totalMs
                + " thread=main genMs=" + generateMs
                + " cache=" + cacheHit
                + " count=" + count
                + " sincePreloadMs=" + (System.currentTimeMillis() - rhymePreloadCompletedAtMs));
    }

    private void positionSuggestionPopup(int cursor) {
        long started = System.currentTimeMillis();
        if (suggestionPanel == null || bodyInput == null || suggestionPopup == null) return;
        suggestionPanel.setVisibility(View.VISIBLE);
        long measureStarted = System.currentTimeMillis();
        boolean measured = false;
        if (suggestionPanelDirty || suggestionPanelWidth <= 0 || suggestionPanelHeight <= 0) {
            suggestionPanel.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            suggestionPanelWidth = suggestionPanel.getMeasuredWidth();
            suggestionPanelHeight = suggestionPanel.getMeasuredHeight();
            suggestionPanelDirty = false;
            measured = true;
        }
        long measureMs = System.currentTimeMillis() - measureStarted;
        int[] bodyLoc = new int[2];
        long locationStarted = System.currentTimeMillis();
        bodyInput.getLocationOnScreen(bodyLoc);
        long locationMs = System.currentTimeMillis() - locationStarted;
        Layout layout = bodyInput.getLayout();
        if (layout == null) return;
        CharSequence text = bodyInput.getText() == null ? "" : bodyInput.getText();
        cursor = Math.max(0, Math.min(cursor, text.length()));
        long layoutStarted = System.currentTimeMillis();
        int line = layout.getLineForOffset(Math.max(0, Math.min(cursor, text.length())));
        float x = layout.getPrimaryHorizontal(Math.max(0, Math.min(cursor, text.length())));
        long layoutMs = System.currentTimeMillis() - layoutStarted;
        int top = bodyLoc[1] + bodyInput.getCompoundPaddingTop() + layout.getLineBottom(line) - bodyInput.getScrollY() + dp(8);
        int left = bodyLoc[0] + bodyInput.getCompoundPaddingLeft() + Math.round(x) - suggestionPanelWidth / 2;
        left = Math.max(dp(8), Math.min(left, getResources().getDisplayMetrics().widthPixels - suggestionPanelWidth - dp(8)));
        top = Math.max(dp(8), top);
        boolean wasShowing = suggestionPopup.isShowing();
        boolean moved = false;
        if (suggestionPopup.isShowing()) {
            if (Math.abs(left - lastPopupLeft) > 3 || Math.abs(top - lastPopupTop) > 3) {
                suggestionPopup.update(left, top, -1, -1);
                moved = true;
            }
        } else {
            suggestionPopup.showAtLocation(root, Gravity.TOP | Gravity.START, left, top);
            moved = true;
        }
        lastPopupLeft = left;
        lastPopupTop = top;
        logRhymeTrace("popup_position", started, "shownBefore=" + wasShowing
                + " moved=" + moved
                + " measured=" + measured
                + " measureMs=" + measureMs
                + " locMs=" + locationMs
                + " layoutMs=" + layoutMs
                + " noteLen=" + text.length()
                + " cursor=" + cursor
                + " line=" + line
                + " ime=" + lastImeVisible);
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
        clearLocalRhymeCache();
        if (rhymeEngine != null) rhymeEngine.clearCache();
    }

    private void clearLocalRhymeCache() {
        synchronized (rhymeCacheLock) {
            rhymeCache.clear();
            rhymeCacheVersion++;
        }
    }

    private void cancelSuggestionJob() {
        synchronized (suggestionJobLock) {
            if (suggestionFuture != null && !suggestionFuture.isDone()) {
                suggestionFuture.cancel(true);
            }
            suggestionFuture = null;
        }
    }

    private void cancelExpandedRhymeJob() {
        synchronized (suggestionJobLock) {
            if (expandedRhymeFuture != null && !expandedRhymeFuture.isDone()) {
                expandedRhymeFuture.cancel(true);
            }
            expandedRhymeFuture = null;
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
        long started = System.currentTimeMillis();
        if (chips == null) return;
        int reused = 0;
        int created = 0;
        while (chips.getChildCount() > words.size()) chips.removeViewAt(chips.getChildCount() - 1);
        for (int i = 0; i < words.size(); i++) {
            String word = words.get(i);
            Button chip;
            if (i < chips.getChildCount() && chips.getChildAt(i) instanceof Button) {
                chip = (Button) chips.getChildAt(i);
                reused++;
            } else {
                chip = button(word);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
                lp.rightMargin = dimen(R.dimen.topflow_space_xs);
                chips.addView(chip, lp);
                created++;
            }
            chip.setText(word);
            chip.setEnabled(true);
            chip.setAlpha(1f);
            styleRhymeChip(chip, current != null ? current.accentColor : C_CYAN);
            chip.setOnClickListener(v -> runSelectionAnimation(v, () -> applySuggestion(word)));
            chip.setOnLongClickListener(v -> {
                promptRemoveSuggestion(word);
                return true;
            });
        }
        suggestionPanelDirty = true;
        logRhymeTrace("chips_render", started, "count=" + words.size() + " reused=" + reused + " created=" + created);
    }

    private void renderSuggestionStatus(LinearLayout chips, String text) {
        long started = System.currentTimeMillis();
        if (chips == null) return;
        while (chips.getChildCount() > 1) chips.removeViewAt(chips.getChildCount() - 1);
        Button chip;
        boolean created = false;
        if (chips.getChildCount() == 1 && chips.getChildAt(0) instanceof Button) {
            chip = (Button) chips.getChildAt(0);
        } else {
            chips.removeAllViews();
            chip = button(text);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
            lp.rightMargin = dimen(R.dimen.topflow_space_xs);
            chips.addView(chip, lp);
            created = true;
        }
        chip.setText(text);
        styleRhymeChip(chip, color(R.color.topflow_accent_gold));
        chip.setEnabled(false);
        chip.setAlpha(0.78f);
        chip.setOnClickListener(null);
        chip.setOnLongClickListener(null);
        suggestionPanelDirty = true;
        logRhymeTrace("chips_status", started, "created=" + created + " text=" + text);
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
        long started = System.currentTimeMillis();
        if (suggestionPopup != null && suggestionPopup.isShowing()) {
            suggestionPopup.dismiss();
            logRhymeTrace("popup_dismiss", started, "noteLen=" + editorTextLength() + " cursor=" + safeEditorCursor() + " ime=" + lastImeVisible);
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
        bodyDraftDirty = false;
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
        long preloadStarted = System.currentTimeMillis();
        rhymePreloadComplete = false;
        firstQueryAfterPreloadLogged = false;
        Log.d(TAG, "preload_start source=rhyme_engine commonLimit=" + RHYME_PRELOAD_COMMON_LIMIT);
        logRhymeTrace("preload_start", preloadStarted, "commonLimit=" + RHYME_PRELOAD_COMMON_LIMIT);
        updateSplashStatus("Loading rhyme index");
        rhymeEngine.loadAsync(() -> {
            long indexMs = System.currentTimeMillis() - preloadStarted;
            updateSplashStatus("Warming common rhymes");
            clearLocalRhymeCache();
            int warmed = prewarmRhymeCaches(preloadStarted);
            rhymePreloadCompletedAtMs = System.currentTimeMillis();
            rhymePreloadComplete = true;
            Log.d(TAG, "preload_end source=rhyme_engine ms=" + (rhymePreloadCompletedAtMs - preloadStarted)
                    + " indexMs=" + indexMs
                    + " warmed=" + warmed
                    + " generation=" + rhymeEngine.generation());
            Log.d(TAG, "rhyme_trace stage=preload_complete ms=" + (rhymePreloadCompletedAtMs - preloadStarted)
                    + " thread=bg indexMs=" + indexMs
                    + " warmed=" + warmed
                    + " generation=" + rhymeEngine.generation());
            editHandler.post(() -> {
                updateSplashStatus("Rhyme engine ready");
                scheduleSuggestionUpdate();
                maybeFinishStartupSplash(false);
            });
        });
    }

    private int prewarmRhymeCaches(long preloadStarted) {
        if (rhymeEngine == null || !rhymeEngine.isReady()) return 0;
        int warmed = 0;
        int fastLimit = Math.min(FAST_RHYME_LIMIT, configuredMaxSuggestions());
        int expandedLimit = Math.max(12, configuredMaxSuggestions());
        RhymeEngine.Options options = rhymeOptions();
        for (int i = 0; i < COMMON_WORDS.length && i < RHYME_PRELOAD_COMMON_LIMIT; i++) {
            if (Thread.currentThread().isInterrupted()) break;
            String word = normalizeWord(COMMON_WORDS[i]);
            if (word.isEmpty()) continue;
            String cacheKey = rhymeCacheKey(word, fastLimit);
            ArrayList<String> fast = rhymeEngine.suggest(word, fastLimit, FAST_RHYME_CANDIDATE_LIMIT, options);
            putCachedRhymes(cacheKey, fast);
            warmed++;
            if (i < RHYME_PRELOAD_EXPANDED_LIMIT) {
                String expandedKey = rhymeCacheKey(word, expandedLimit) + "|expanded";
                ArrayList<String> expanded = rhymeEngine.suggest(word, expandedLimit, EXPANDED_RHYME_CANDIDATE_LIMIT, options);
                putCachedRhymes(expandedKey, expanded);
                warmed++;
            }
        }
        logRhymeTrace("preload_warm", preloadStarted, "entries=" + warmed + " localVersion=" + rhymeCacheVersion);
        return warmed;
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
        preview.setMinHeight(dp(96));
        preview.setPadding(dimen(R.dimen.topflow_space_md), dimen(R.dimen.topflow_space_md), dimen(R.dimen.topflow_space_md), dimen(R.dimen.topflow_space_md));
        preview.setBackground(buildColorPanelDrawable(startColor));
        TextView hex = settingsValuePill(String.format(Locale.US, "#%06X", 0xFFFFFF & startColor));
        hex.setPadding(dimen(R.dimen.topflow_space_md), 0, 0, dimen(R.dimen.topflow_space_xs));
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
            preview.setBackground(buildColorPanelDrawable(refined));
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
        LinearLayout previewCard = new LinearLayout(this);
        previewCard.setOrientation(LinearLayout.VERTICAL);
        previewCard.setPadding(0, dimen(R.dimen.topflow_space_sm), 0, dimen(R.dimen.topflow_space_sm));
        previewCard.addView(preview, new LinearLayout.LayoutParams(-1, -2));
        previewCard.addView(hex);
        box.addView(previewCard);
        box.addView(buildSliderRow("Saturation", sat));
        sat.setProgress((int) (hsv[1] * 100f));
        box.addView(buildSliderRow("Brightness", val));
        val.setProgress((int) (hsv[2] * 100f));
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
        box.addView(buildMainMenuContextPanel());
        box.addView(sheetDivider(current != null ? current.accentColor : C_CYAN));
        box.addView(sheetSectionTitle("Recent sessions"));
        box.addView(buildRecentSessionPreview());
        box.addView(sheetDivider(current != null ? current.accentColor : C_CYAN));
        box.addView(sheetSectionTitle("Commands"));
        box.addView(buildSheetMenuRow("Notes", "Session dashboard", C_TEXT_MUTED, C_CYAN, () -> runAfterMenuDismiss(this::showMenuScreen)));
        box.addView(buildSheetMenuRow("Note Style", "Palette · fonts · glow", C_TEXT_MUTED, C_CYAN, () -> runAfterMenuDismiss(this::showStyleMenu)));
        box.addView(buildSheetMenuRow("Expanded Rhymes", "Focused word", C_TEXT_MUTED, C_CYAN, () -> runAfterMenuDismiss(() -> showExpandedRhymes())));
        box.addView(buildSheetMenuRow("Rhyme Settings", "Strictness · row limits", C_TEXT_MUTED, C_CYAN, () -> runAfterMenuDismiss(() -> showRhymeSettingsMenu())));
        box.addView(buildSheetMenuRow("Deleted Rhymes", "Removed suggestions", C_TEXT_MUTED, C_CYAN, () -> runAfterMenuDismiss(() -> showDeletedRhymesMenu())));
        box.addView(buildSheetMenuRow("Check for updates", "Installed v" + BuildConfig.VERSION_NAME, C_TEXT_MUTED, C_CYAN, () -> runAfterMenuDismiss(() -> checkForUpdates(true))));
        Button close = button("Close");
        close.setOnClickListener(v -> dismissSheet());
        box.addView(close);
        showSheet("Main Menu", box);
    }

    private View buildMainMenuContextPanel() {
        int accent = current != null ? current.accentColor : C_CYAN;
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.HORIZONTAL);
        panel.setGravity(Gravity.CENTER_VERTICAL);
        panel.setPadding(dimen(R.dimen.topflow_space_lg), dimen(R.dimen.topflow_space_md), dimen(R.dimen.topflow_space_lg), dimen(R.dimen.topflow_space_md));
        panel.setBackground(oledCommandSurface(18, accent, current != null));
        TopFlowUiKit.applyFloating(panel, current != null ? 9 : 5);

        View rail = new View(this);
        rail.setBackground(commandRailDrawable(accent));
        LinearLayout.LayoutParams railLp = new LinearLayout.LayoutParams(dp(4), dp(48));
        railLp.rightMargin = dimen(R.dimen.topflow_space_md);
        panel.addView(rail, railLp);

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));

        TextView label = sheetEyebrow(current != null ? "Current session" : "Session");
        copy.addView(label);

        TextView title = new TextView(this);
        title.setText(current == null ? "No note open" : noteTitleForDisplay(current));
        title.setTextColor(TopFlowUiKit.TEXT);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        title.setIncludeFontPadding(false);
        title.setLetterSpacing(0f);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        copy.addView(title);

        TextView meta = new TextView(this);
        meta.setText(current == null ? sessionCountLine() : noteMetadataLine(current));
        meta.setTextColor(TopFlowUiKit.TEXT_SOFT);
        meta.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        meta.setIncludeFontPadding(false);
        meta.setLetterSpacing(0f);
        meta.setSingleLine(true);
        meta.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams metaLp = new LinearLayout.LayoutParams(-1, -2);
        metaLp.topMargin = dp(4);
        copy.addView(meta, metaLp);

        panel.addView(copy);
        return panel;
    }

    private String sessionCountLine() {
        int count = notes == null ? 0 : notes.size();
        return count + " saved session" + (count == 1 ? "" : "s");
    }

    private void runAfterMenuDismiss(Runnable action) {
        if (action == null) return;
        if (sheetOverlay == null || sheetOverlay.getVisibility() != View.VISIBLE) {
            action.run();
            return;
        }
        dismissSheet();
        editHandler.postDelayed(action, MOTION_SHEET_DISMISS_MS + 12L);
    }

    private void showStyleMenu() {
        setActiveDockState(DOCK_STATE_STYLE);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        TextView heading = sheetSectionTitle("Note Style");
        heading.setTextColor(TopFlowUiKit.TEXT_SOFT);
        box.addView(heading);
        box.addView(buildStyleHubRow(
                "Note Color",
                current == null ? "--" : formatColor(current.noteColor),
                buildColorSample(current == null ? DEFAULT_NOTE_COLOR : current.noteColor),
                () -> {
                    if (current == null) return;
                    selectedColorTarget = 0;
                    showColorEditor(current.noteColor);
                }
        ));
        box.addView(buildStyleHubRow(
                "Text Color",
                current == null ? "--" : formatColor(current.textColor),
                buildColorSample(current == null ? C_TEXT : current.textColor),
                () -> {
                    if (current == null) return;
                    selectedColorTarget = 1;
                    showColorEditor(current.textColor);
                }
        ));
        box.addView(buildStyleHubRow(
                "Accent Color",
                current == null ? "--" : formatColor(current.accentColor),
                buildColorSample(current == null ? C_CYAN : current.accentColor),
                () -> {
                    if (current == null) return;
                    selectedColorTarget = 2;
                    showColorEditor(current.accentColor);
                }
        ));
        box.addView(buildStyleHubRow(
                "Font",
                current == null ? "--" : fontLabel(current.font),
                buildFontMiniPreview(current == null ? "sans" : current.font),
                () -> {
                    if (current == null) return;
                    showFontMenu();
                }
        ));
        box.addView(buildStyleHubRow(
                "Font Size",
                current == null ? "--" : (current.fontSizeSp + "sp"),
                buildTextSizePreview(current == null ? DEFAULT_EDITOR_FONT_SIZE_SP : current.fontSizeSp),
                () -> {
                    if (current == null) return;
                    showFontSizeMenu();
                }
        ));
        box.addView(buildStyleHubRow(
                "Note Glow",
                current == null ? "--" : (current.noteGlow ? "On" : "Off") + "  •  " + current.glowStrength,
                buildGlowPreview(current == null ? C_CYAN : current.accentColor),
                () -> {
                    if (current == null) return;
                    showGlowMenu();
                }
        ));
        Button close = button("Close");
        close.setOnClickListener(v -> dismissSheet());
        box.addView(close);
        showSheet("Note Style", box);
    }

    private void showFontSizeMenu() {
        if (current == null) return;
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.addView(sheetSectionTitle("Font Size"));
        View preview = buildLiveTextPreview("whisper my name", current.font, current.fontSizeSp, current.textColor);
        box.addView(preview);
        TextView value = settingsValuePill(current.fontSizeSp + "sp");
        value.setPadding(dimen(R.dimen.topflow_space_md), 0, 0, dimen(R.dimen.topflow_space_md));
        box.addView(value);
        SeekBar bar = new SeekBar(this);
        bar.setMax(MAX_EDITOR_FONT_SIZE_SP - MIN_EDITOR_FONT_SIZE_SP);
        bar.setProgress(current.fontSizeSp - MIN_EDITOR_FONT_SIZE_SP);
        styleSeekBar(bar, current.accentColor);
        box.addView(bar, new LinearLayout.LayoutParams(-1, -2));
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser || current == null) return;
                current.fontSizeSp = MIN_EDITOR_FONT_SIZE_SP + progress;
                value.setText(current.fontSizeSp + "sp");
                updateTextPreview(preview, current.fontSizeSp, current.font, current.textColor);
                applyStyle();
                saveNotes();
            }
            public void onStartTrackingTouch(SeekBar seekBar) {}
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        Button close = button("Close");
        close.setOnClickListener(v -> dismissSheet());
        box.addView(close);
        showSheet("Font Size", box);
    }

    private void showGlowMenu() {
        if (current == null) return;
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.addView(sheetSectionTitle("Note Glow"));
        LinearLayout glowPreview = new LinearLayout(this);
        glowPreview.setOrientation(LinearLayout.VERTICAL);
        glowPreview.addView(buildGlowDemoPanel(current.accentColor, current.noteGlow, current.glowStrength), new LinearLayout.LayoutParams(-1, -2));
        box.addView(glowPreview);
        TextView status = settingsValuePill(glowStatus());
        status.setPadding(0, 0, 0, dimen(R.dimen.topflow_space_md));
        box.addView(status);
        Button toggle = button("Glow: " + (current.noteGlow ? "On" : "Off"));
        toggle.setOnClickListener(v -> {
            if (current == null) return;
            current.noteGlow = !current.noteGlow;
            status.setText(glowStatus());
            toggle.setText("Glow: " + (current.noteGlow ? "On" : "Off"));
            applyButtonIcon(toggle, toggle.getText().toString());
            saveNotes();
            applyStyle();
            glowPreview.removeAllViews();
            glowPreview.addView(buildGlowDemoPanel(current.accentColor, current.noteGlow, current.glowStrength), new LinearLayout.LayoutParams(-1, -2));
        });
        box.addView(toggle);
        TextView strength = settingsValuePill("Strength " + current.glowStrength);
        box.addView(strength);
        SeekBar bar = new SeekBar(this);
        bar.setMax(4);
        bar.setProgress(current.glowStrength);
        styleSeekBar(bar, current.accentColor);
        box.addView(bar, new LinearLayout.LayoutParams(-1, -2));
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser || current == null) return;
                current.glowStrength = progress;
                strength.setText("Strength " + progress);
                if (progress > 0) current.noteGlow = true;
                status.setText(glowStatus());
                toggle.setText("Glow: " + (current.noteGlow ? "On" : "Off"));
                saveNotes();
                applyStyle();
                glowPreview.removeAllViews();
                glowPreview.addView(buildGlowDemoPanel(current.accentColor, current.noteGlow, current.glowStrength), new LinearLayout.LayoutParams(-1, -2));
            }
            public void onStartTrackingTouch(SeekBar seekBar) {}
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        Button close = button("Close");
        close.setOnClickListener(v -> dismissSheet());
        box.addView(close);
        showSheet("Note Glow", box);
    }

    private String glowStatus() {
        if (current == null) return "Glow: Off";
        return "Glow: " + (current.noteGlow ? "On" : "Off") + "  Strength: " + current.glowStrength;
    }

    private void showFontMenu() {
        String[] fonts = TopFlowUiKit.fontPreviewIds();
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.addView(sheetSectionTitle("Font"));
        for (String f : fonts) {
            View b = buildFontPreviewRow(f);
            b.setOnClickListener(v -> {
                if (current != null) {
                    runSelectionAnimation(v, () -> {
                        current.font = f;
                        saveNotes();
                        applyStyle();
                        dismissSheet();
                    });
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
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dimen(R.dimen.topflow_space_lg), dimen(R.dimen.topflow_space_md), dimen(R.dimen.topflow_space_lg), dimen(R.dimen.topflow_space_md));
        boolean selected = current != null && fontId.equals(current.font);
        int accent = selected && current != null ? current.accentColor : C_CYAN;
        row.setBackground(oledCommandSurface(16, accent, selected));
        row.setClickable(true);
        row.setFocusable(true);
        row.setMinimumHeight(dp(84));
        TopFlowUiKit.applyFloating(row, selected ? 7 : 3);
        row.setForeground(TopFlowUiKit.ripple(selected ? accent : TopFlowUiKit.MINT));
        TextView name = new TextView(this);
        name.setText(fontLabel(fontId));
        textStyle(name, R.style.TextAppearance_TopFlow21_Caption);
        name.setTextColor(selected ? TopFlowUiKit.MINT : TopFlowUiKit.TEXT_SOFT);
        name.setSingleLine(true);
        name.setEllipsize(TextUtils.TruncateAt.END);
        TextView preview = new TextView(this);
        preview.setText("whisper my name");
        textStyle(preview, R.style.TextAppearance_TopFlow21_Section);
        preview.setTypeface(TopFlowUiKit.fontForPreview(this, fontId));
        preview.setTextColor(TopFlowUiKit.TEXT);
        preview.setPadding(0, dimen(R.dimen.topflow_space_xs), 0, 0);
        preview.setSingleLine(true);
        preview.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));
        copy.addView(name);
        copy.addView(preview);
        row.addView(copy);
        row.addView(settingsValuePill(selected ? "Active" : "Use"));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = dimen(R.dimen.topflow_space_sm);
        row.setLayoutParams(lp);
        return row;
    }

    private String fontLabel(String fontId) {
        if ("slim".equals(fontId)) return "Slim · Space Grotesk";
        if ("pixel".equals(fontId)) return "Pixel · Silkscreen";
        if ("terminal".equals(fontId)) return "Terminal · Share Tech Mono";
        if ("rounded".equals(fontId)) return "Modern Rounded";
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
        setActiveDockState(DOCK_STATE_RHYME);
        long started = System.currentTimeMillis();
        String word = focusedRhymeWord();
        int cursor = safeEditorCursor();
        int noteLen = editorTextLength();
        int requestId = ++expandedRhymeRequestId;
        cancelSuggestionJob();
        cancelExpandedRhymeJob();
        dismissSuggestionPopup();
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        if (word.isEmpty()) {
            box.addView(buildRhymeContextPanel("No word selected", "Place the cursor on a word", C_CYAN));
            Button close = button("Close");
            close.setOnClickListener(v -> dismissSheet());
            box.addView(close);
            showSheet("Expanded Rhymes", box);
            logRhymeTrace("expanded_tap_empty", started, "noteLen=" + noteLen + " cursor=" + cursor);
            return;
        } else {
            box.addView(buildRhymeContextPanel(word, rhymeEngine.isReady() ? "Finding rhymes" : "Loading rhyme index", current != null ? current.accentColor : C_CYAN));
        }
        Button close = button("Close");
        close.setOnClickListener(v -> dismissSheet());
        box.addView(close);
        showSheet("Expanded Rhymes", box);
        logRhymeTrace("expanded_sheet_visible", started, "word=" + word + " request=" + requestId + " noteLen=" + noteLen + " cursor=" + cursor + " ready=" + rhymeEngine.isReady());
        startExpandedRhymeJob(requestId, word, box, started, noteLen, cursor);
    }

    private View buildRhymeContextPanel(String titleText, String metaText, int accent) {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.HORIZONTAL);
        panel.setGravity(Gravity.CENTER_VERTICAL);
        panel.setPadding(dimen(R.dimen.topflow_space_lg), dimen(R.dimen.topflow_space_md), dimen(R.dimen.topflow_space_lg), dimen(R.dimen.topflow_space_md));
        panel.setBackground(oledCommandSurface(18, accent, true));
        TopFlowUiKit.applyFloating(panel, 5);

        View rail = new View(this);
        rail.setBackground(commandRailDrawable(accent));
        LinearLayout.LayoutParams railLp = new LinearLayout.LayoutParams(dp(4), dp(46));
        railLp.rightMargin = dimen(R.dimen.topflow_space_md);
        panel.addView(rail, railLp);

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));
        copy.addView(sheetEyebrow("Focused word"));

        TextView title = new TextView(this);
        title.setText(titleText == null ? "" : titleText);
        title.setTextColor(TopFlowUiKit.TEXT);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        title.setIncludeFontPadding(false);
        title.setLetterSpacing(0f);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        copy.addView(title);

        TextView meta = new TextView(this);
        meta.setText(metaText == null ? "" : metaText);
        meta.setTextColor(TopFlowUiKit.TEXT_SOFT);
        meta.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        meta.setIncludeFontPadding(false);
        meta.setLetterSpacing(0f);
        meta.setSingleLine(true);
        meta.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams metaLp = new LinearLayout.LayoutParams(-1, -2);
        metaLp.topMargin = dp(4);
        copy.addView(meta, metaLp);

        panel.addView(copy);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = dimen(R.dimen.topflow_space_sm);
        panel.setLayoutParams(lp);
        return panel;
    }

    private void startExpandedRhymeJob(int requestId, String word, LinearLayout box, long requestStartedAt, int noteLen, int cursor) {
        if (rhymeExecutor.isShutdown()) return;
        synchronized (suggestionJobLock) {
            expandedRhymeFuture = rhymeExecutor.submit(() -> {
                long generateStarted = System.currentTimeMillis();
                boolean cacheHit = false;
                ArrayList<String> words;
                String cacheKey = rhymeCacheKey(word, Math.max(12, configuredMaxSuggestions())) + "|expanded";
                ArrayList<String> cached = cachedRhymes(cacheKey);
                if (cached != null) {
                    words = cached;
                    cacheHit = true;
                } else {
                    words = suggestRhymes(word, Math.max(12, configuredMaxSuggestions()), EXPANDED_RHYME_CANDIDATE_LIMIT);
                    if (Thread.currentThread().isInterrupted()) return;
                    putCachedRhymes(cacheKey, words);
                }
                long generateMs = System.currentTimeMillis() - generateStarted;
                Log.d(TAG, "rhyme_trace stage=expanded_generate ms=" + generateMs
                        + " thread=bg word=" + word
                        + " count=" + words.size()
                        + " cache=" + cacheHit
                        + " noteLen=" + noteLen
                        + " cursor=" + cursor);
                if (Thread.currentThread().isInterrupted()) return;
                final ArrayList<String> finalWords = words;
                final boolean finalCacheHit = cacheHit;
                editHandler.post(() -> renderExpandedRhymeResults(requestId, word, box, finalWords, finalCacheHit, generateMs, requestStartedAt, noteLen, cursor));
            });
        }
    }

    private void renderExpandedRhymeResults(int requestId, String word, LinearLayout box, ArrayList<String> words, boolean cacheHit, long generateMs, long requestStartedAt, int noteLen, int cursor) {
        long uiStarted = System.currentTimeMillis();
        if (requestId != expandedRhymeRequestId || sheetOverlay == null || sheetOverlay.getVisibility() != View.VISIBLE) return;
        box.removeAllViews();
        box.addView(buildRhymeContextPanel(word, words.isEmpty() ? "No matches" : words.size() + " matches", current != null ? current.accentColor : C_CYAN));
        int created = 0;
        if (words.isEmpty()) {
            TextView empty = label("No rhymes found with current settings.");
            empty.setTextColor(C_TEXT_MUTED);
            box.addView(empty);
        } else {
            for (String rhyme : words) {
                Button b = button(rhyme);
                b.setOnClickListener(v -> {
                    runSelectionAnimation(v, () -> {
                        applySuggestion(rhyme);
                        dismissSheet();
                    });
                });
                b.setOnLongClickListener(v -> {
                    promptRemoveSuggestion(rhyme);
                    return true;
                });
                box.addView(b);
                created++;
            }
        }
        Button close = button("Close");
        close.setOnClickListener(v -> dismissSheet());
        box.addView(close);
        long uiMs = System.currentTimeMillis() - uiStarted;
        long totalMs = System.currentTimeMillis() - requestStartedAt;
        Log.d(TAG, "rhyme_trace stage=expanded_visible ms=" + totalMs
                + " thread=main word=" + word
                + " genMs=" + generateMs
                + " uiMs=" + uiMs
                + " viewsCreated=" + created
                + " cache=" + cacheHit
                + " noteLen=" + noteLen
                + " cursor=" + cursor);
    }

    private void showRhymeSettingsMenu() {
        setActiveDockState(DOCK_STATE_SETTINGS);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        TextView status = label("Strictness: " + strictnessName() + "  Max: " + configuredMaxSuggestions());
        status.setTextColor(C_TEXT_MUTED);
        status.setPadding(0, 0, 0, dimen(R.dimen.topflow_space_sm));
        box.addView(status);
        addStrictnessSlider(box, status);
        addMaxSuggestionSlider(box, status);
        addToggleButton(box, "Show rhyme row", PREF_SHOW_RHYME_ROW, true);
        addToggleButton(box, "Show exact only", PREF_EXACT_ONLY, false);
        addToggleButton(box, "Include slang overrides", PREF_INCLUDE_SLANG, true);
        Button close = button("Close");
        close.setOnClickListener(v -> dismissSheet());
        box.addView(close);
        showSheet("Rhyme Settings", box);
    }

    private void addStrictnessSlider(LinearLayout box, TextView status) {
        LinearLayout section = new LinearLayout(this);
        section.setOrientation(LinearLayout.VERTICAL);
        section.setBackground(oledCommandSurface(16, current != null ? current.accentColor : C_CYAN, false));
        section.setPadding(dimen(R.dimen.topflow_space_md), dimen(R.dimen.topflow_space_md), dimen(R.dimen.topflow_space_md), dimen(R.dimen.topflow_space_sm));
        section.setLayoutParams(settingsSectionLayoutParams());
        TopFlowUiKit.applyFloating(section, 6);
        TextView label = label("Rhyme strictness");
        label.setTextColor(C_TEXT);
        section.addView(label);
        SeekBar bar = new SeekBar(this);
        bar.setMax(2);
        bar.setProgress(strictnessIndex());
        styleSeekBar(bar, current != null ? current.accentColor : C_CYAN);
        TextView value = settingsValuePill(strictnessName());
        value.setPadding(0, dimen(R.dimen.topflow_space_xs), 0, dimen(R.dimen.topflow_space_sm));
        section.addView(value);
        section.addView(bar, new LinearLayout.LayoutParams(-1, -2));
        box.addView(section);
        TextView finalValue = value;
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser) return;
                String next = progress == 0 ? "Strict" : progress == 1 ? "Balanced" : "Loose";
                prefs.edit().putString(PREF_RHYME_STRICTNESS, next).apply();
                finalValue.setText(next);
                clearRhymeCache();
                scheduleSuggestionUpdate();
                if (status != null) {
                    status.setText("Strictness: " + strictnessName() + "  Max: " + configuredMaxSuggestions());
                }
            }
            public void onStartTrackingTouch(SeekBar seekBar) {}
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void addMaxSuggestionSlider(LinearLayout box, TextView status) {
        LinearLayout section = new LinearLayout(this);
        section.setOrientation(LinearLayout.VERTICAL);
        section.setBackground(oledCommandSurface(16, current != null ? current.accentColor : C_CYAN, false));
        section.setPadding(dimen(R.dimen.topflow_space_md), dimen(R.dimen.topflow_space_md), dimen(R.dimen.topflow_space_md), dimen(R.dimen.topflow_space_sm));
        section.setLayoutParams(settingsSectionLayoutParams());
        TopFlowUiKit.applyFloating(section, 6);
        TextView label = label("Max suggestions");
        label.setTextColor(C_TEXT);
        section.addView(label);
        SeekBar bar = new SeekBar(this);
        bar.setMax(8);
        bar.setProgress(configuredMaxSuggestions() - 4);
        styleSeekBar(bar, current != null ? current.accentColor : C_CYAN);
        TextView value = settingsValuePill(String.valueOf(configuredMaxSuggestions()));
        value.setPadding(0, dimen(R.dimen.topflow_space_xs), 0, dimen(R.dimen.topflow_space_sm));
        section.addView(value);
        section.addView(bar, new LinearLayout.LayoutParams(-1, -2));
        TextView finalValue = value;
        box.addView(section);
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser) return;
                int max = progress + 4;
                prefs.edit().putInt(PREF_MAX_SUGGESTIONS, max).apply();
                finalValue.setText(String.valueOf(max));
                clearRhymeCache();
                scheduleSuggestionUpdate();
                if (status != null) {
                    status.setText("Strictness: " + strictnessName() + "  Max: " + configuredMaxSuggestions());
                }
            }
            public void onStartTrackingTouch(SeekBar seekBar) {}
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void addToggleButton(LinearLayout box, String labelText, String key, boolean defaultValue) {
        boolean enabled = prefs.getBoolean(key, defaultValue);
        final boolean[] state = {enabled};
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setBackground(oledCommandSurface(16, current != null ? current.accentColor : C_CYAN, state[0]));
        row.setPadding(dimen(R.dimen.topflow_space_md), dimen(R.dimen.topflow_space_md), dimen(R.dimen.topflow_space_md), dimen(R.dimen.topflow_space_md));
        row.setLayoutParams(settingsSectionLayoutParams());
        TopFlowUiKit.applyFloating(row, 6);
        row.setClickable(true);
        row.setFocusable(true);
        row.setForeground(TopFlowUiKit.ripple(C_CYAN));
        TextView title = label(labelText);
        title.setTextColor(C_TEXT);
        row.addView(title);
        TextView stateView = settingsValuePill(state[0] ? "On" : "Off");
        stateView.setPadding(0, dimen(R.dimen.topflow_space_xs), 0, 0);
        row.addView(stateView);
        row.setOnClickListener(v -> {
            runSelectionAnimation(v, () -> {
                state[0] = !state[0];
                stateView.setText(state[0] ? "On" : "Off");
                row.setBackground(oledCommandSurface(16, current != null ? current.accentColor : C_CYAN, state[0]));
                prefs.edit().putBoolean(key, state[0]).apply();
                clearRhymeCache();
                scheduleSuggestionUpdate();
            });
        });
        box.addView(row);
    }

    private String focusedRhymeWord() {
        if (bodyInput == null) return "";
        CharSequence text = bodyInput.getText() == null ? "" : bodyInput.getText();
        int cursor = Math.max(0, bodyInput.getSelectionStart());
        TokenInfo info = currentToken(text, cursor);
        if (info.word.isEmpty() && cursor > 0) info = previousToken(text, cursor);
        return normalizeWord(info.word);
    }

    private View buildSheetOverlay() {
        FrameLayout overlay = new FrameLayout(this);
        overlay.setVisibility(View.GONE);
        overlay.setBackgroundColor(Color.argb(206, 0, 0, 0));
        overlay.setClickable(true);
        overlay.setOnClickListener(v -> dismissSheet());

        sheetCard = new LinearLayout(this);
        sheetCard.setOrientation(LinearLayout.VERTICAL);
        sheetCard.setPadding(dimen(R.dimen.topflow_space_xl), dimen(R.dimen.topflow_space_md), dimen(R.dimen.topflow_space_xl), dimen(R.dimen.topflow_space_xl));
        sheetCard.setBackground(oledSheetSurface(current != null ? current.accentColor : C_CYAN));
        TopFlowUiKit.applyFloating(sheetCard, 18);
        sheetCard.setClickable(true);
        sheetCard.setFocusable(false);
        attachSheetDragDismiss(sheetCard);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
        lp.leftMargin = dp(14);
        lp.rightMargin = dp(14);
        lp.bottomMargin = dp(14);
        overlay.addView(sheetCard, lp);

        FrameLayout handleHitbox = new FrameLayout(this);
        sheetDragHandle = handleHitbox;
        handleHitbox.setPadding(0, dp(14), 0, dp(14));
        View handle = new View(this);
        handle.setBackground(sheetHandleDrawable(current != null ? current.accentColor : C_CYAN));
        FrameLayout.LayoutParams handleInnerLp = new FrameLayout.LayoutParams(dp(86), dp(5), Gravity.CENTER);
        handleHitbox.addView(handle, handleInnerLp);
        LinearLayout.LayoutParams handleLp = new LinearLayout.LayoutParams(-1, dp(34));
        handleLp.gravity = Gravity.CENTER_HORIZONTAL;
        handleLp.bottomMargin = dimen(R.dimen.topflow_space_sm);
        sheetCard.addView(handleHitbox, handleLp);
        attachSheetDragDismiss(handleHitbox);

        LinearLayout header = new LinearLayout(this);
        sheetHeader = header;
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(0, 0, 0, dimen(R.dimen.topflow_space_xs));
        sheetTitle = new TextView(this);
        textStyle(sheetTitle, R.style.TextAppearance_TopFlow21_Section);
        sheetTitle.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        sheetTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
        sheetTitle.setTextColor(TopFlowUiKit.TEXT);
        sheetTitle.setIncludeFontPadding(false);
        sheetTitle.setLetterSpacing(0f);
        sheetTitle.setSingleLine(true);
        sheetTitle.setEllipsize(TextUtils.TruncateAt.END);
        header.addView(sheetTitle, new LinearLayout.LayoutParams(0, -2, 1));
        Button close = button("Close");
        close.setOnClickListener(v -> dismissSheet());
        header.addView(close);
        sheetCard.addView(header);
        attachSheetDragDismiss(header);
        sheetCard.addView(sheetDivider(current != null ? current.accentColor : C_CYAN));

        sheetBody = new LinearLayout(this);
        sheetBody.setOrientation(LinearLayout.VERTICAL);
        sheetBody.setPadding(0, dimen(R.dimen.topflow_space_md), 0, 0);
        sheetBodyScroll = new ScrollView(this);
        sheetBodyScroll.setFillViewport(true);
        sheetBodyScroll.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        sheetBodyScroll.setVerticalScrollBarEnabled(true);
        sheetBodyScroll.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        sheetBodyScroll.addView(sheetBody);
        sheetCard.addView(sheetBodyScroll);
        return overlay;
    }

    private void attachSheetDragDismiss(View dragHandle) {
        final float[] downY = {0f};
        final float[] downX = {0f};
        final boolean[] active = {false};
        final VelocityTracker[] tracker = {null};
        dragHandle.setOnTouchListener((v, event) -> {
            if (sheetCard == null || sheetOverlay == null) return false;
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    if (!canStartSheetDrag(v, event)) return false;
                    downY[0] = event.getRawY();
                    downX[0] = event.getRawX();
                    active[0] = true;
                    tracker[0] = VelocityTracker.obtain();
                    tracker[0].addMovement(event);
                    sheetCard.animate().cancel();
                    sheetOverlay.animate().cancel();
                    if (shell != null) shell.animate().cancel();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (!active[0]) return false;
                    if (tracker[0] != null) tracker[0].addMovement(event);
                    float dy = Math.max(0f, event.getRawY() - downY[0]);
                    float dx = Math.abs(event.getRawX() - downX[0]);
                    if (dx > dy * 1.8f && dy < dp(22)) return true;
                    sheetCard.setTranslationY(dy);
                    float progress = Math.min(1f, dy / Math.max(1f, dp(360)));
                    sheetOverlay.setAlpha(Math.max(0.28f, 1f - progress * 0.62f));
                    if (shell != null) {
                        float scale = 0.985f + progress * 0.015f;
                        shell.setScaleX(scale);
                        shell.setScaleY(scale);
                        shell.setAlpha(0.82f + progress * 0.18f);
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (!active[0]) return false;
                    float releaseDy = Math.max(0f, event.getRawY() - downY[0]);
                    float velocityY = 0f;
                    if (tracker[0] != null) {
                        tracker[0].addMovement(event);
                        tracker[0].computeCurrentVelocity(1000);
                        velocityY = tracker[0].getYVelocity();
                        tracker[0].recycle();
                        tracker[0] = null;
                    }
                    active[0] = false;
                    if (event.getActionMasked() != MotionEvent.ACTION_CANCEL
                            && shouldDismissSheet(releaseDy, velocityY)) {
                        dismissSheet();
                    } else {
                        settleSheetOpen();
                    }
                    return true;
                default:
                    return true;
            }
        });
    }

    private void attachEditorBackSwipe(ScrollView target) {
        if (target == null) return;
        final float[] downX = {0f};
        final float[] downY = {0f};
        final float[] deltaX = {0f};
        final float[] deltaY = {0f};
        final float[] velocityX = {0f};
        final float[] velocityY = {0f};
        final VelocityTracker[] tracker = {null};
        final boolean[] tracking = {false};
        final boolean[] horizontal = {false};
        target.setOnTouchListener((v, event) -> {
            if (editorPanel == null || editorPanel.getVisibility() != View.VISIBLE) return false;
            if (sheetOverlay != null && sheetOverlay.getVisibility() == View.VISIBLE) return false;
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                if ((bodyInput != null && bodyInput.hasFocus()) || (titleInput != null && titleInput.hasFocus())) return false;
                if (event.getX() > dp(EDITOR_BACK_SWIPE_EDGE_DP)) return false;
                downX[0] = event.getRawX();
                downY[0] = event.getRawY();
                deltaX[0] = 0f;
                deltaY[0] = 0f;
                velocityX[0] = 0f;
                velocityY[0] = 0f;
                tracking[0] = true;
                horizontal[0] = false;
                tracker[0] = VelocityTracker.obtain();
                tracker[0].addMovement(event);
                return false;
            }
            if (!tracking[0]) return false;
            if (action == MotionEvent.ACTION_MOVE) {
                if (tracker[0] != null) tracker[0].addMovement(event);
                deltaX[0] = event.getRawX() - downX[0];
                deltaY[0] = Math.abs(event.getRawY() - downY[0]);
                if (!horizontal[0]) {
                    if (deltaX[0] <= 0f) {
                        tracking[0] = false;
                        horizontal[0] = false;
                        if (tracker[0] != null) {
                            tracker[0].recycle();
                            tracker[0] = null;
                        }
                        hideSwipeAffordance(editorSwipeRail);
                        return false;
                    }
                    if (shouldTrackHorizontalSwipe(deltaX[0], deltaY[0])) {
                        horizontal[0] = true;
                    } else if (shouldAbortHorizontalSwipe(deltaX[0], deltaY[0])) {
                        tracking[0] = false;
                        horizontal[0] = false;
                        if (tracker[0] != null) {
                            tracker[0].recycle();
                            tracker[0] = null;
                        }
                        hideSwipeAffordance(editorSwipeRail);
                        return false;
                    }
                }
                if (!horizontal[0]) return false;
                float max = dp(EDITOR_BACK_SWIPE_TRIGGER_DP);
                float progress = Math.min(1f, deltaX[0] / Math.max(1f, max));
                updateSwipeAffordance(editorSwipeRail, progress);
                float translation = Math.min(deltaX[0], max);
                editorPanel.setTranslationX(translation);
                editorPanel.setAlpha(1f - (0.28f * progress));
                editorPanel.setScaleX(1f - (MOTION_SWIPE_SCALE_REDUCE_X * progress));
                editorPanel.setScaleY(1f - (MOTION_SWIPE_SCALE_REDUCE_Y * progress));
                target.requestDisallowInterceptTouchEvent(true);
                return true;
            }
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                if (tracker[0] != null) {
                    tracker[0].addMovement(event);
                    tracker[0].computeCurrentVelocity(1000);
                    velocityX[0] = tracker[0].getXVelocity();
                    velocityY[0] = tracker[0].getYVelocity();
                    tracker[0].recycle();
                    tracker[0] = null;
                }
                boolean trigger = shouldCompleteBackSwipe(deltaX[0], deltaY[0], velocityX[0], velocityY[0], true);
                if (trigger) {
                    completeSwipePanel(editorPanel, true, () -> showMenuScreen());
                } else {
                    settleSwipePanel(editorPanel);
                }
                hideSwipeAffordance(editorSwipeRail);
                tracking[0] = false;
                horizontal[0] = false;
                if (tracker[0] != null) {
                    tracker[0].recycle();
                    tracker[0] = null;
                }
                return true;
            }
            return false;
        });
    }

    private void attachNotesBackSwipe(ScrollView target) {
        if (target == null) return;
        final float[] downX = {0f};
        final float[] downY = {0f};
        final float[] deltaX = {0f};
        final float[] deltaY = {0f};
        final float[] velocityX = {0f};
        final float[] velocityY = {0f};
        final VelocityTracker[] tracker = {null};
        final boolean[] tracking = {false};
        final boolean[] horizontal = {false};
        target.setOnTouchListener((v, event) -> {
            if (menuPanel == null || menuPanel.getVisibility() != View.VISIBLE) return false;
            if (current == null) return false;
            if (sheetOverlay != null && sheetOverlay.getVisibility() == View.VISIBLE) return false;
            if (editorPanel != null && editorPanel.getVisibility() == View.VISIBLE) return false;
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                if (target.getWidth() <= 0) return false;
                if (event.getX() < target.getWidth() - dp(EDITOR_BACK_SWIPE_EDGE_DP)) return false;
                downX[0] = event.getRawX();
                downY[0] = event.getRawY();
                deltaX[0] = 0f;
                deltaY[0] = 0f;
                velocityX[0] = 0f;
                velocityY[0] = 0f;
                tracking[0] = true;
                horizontal[0] = false;
                tracker[0] = VelocityTracker.obtain();
                tracker[0].addMovement(event);
                return false;
            }
            if (!tracking[0]) return false;
            if (action == MotionEvent.ACTION_MOVE) {
                if (tracker[0] != null) tracker[0].addMovement(event);
                deltaX[0] = downX[0] - event.getRawX();
                deltaY[0] = Math.abs(event.getRawY() - downY[0]);
                if (!horizontal[0]) {
                    if (deltaX[0] <= 0f) {
                        tracking[0] = false;
                        horizontal[0] = false;
                        if (tracker[0] != null) {
                            tracker[0].recycle();
                            tracker[0] = null;
                        }
                        hideSwipeAffordance(notesSwipeRail);
                        return false;
                    }
                    if (shouldTrackHorizontalSwipe(deltaX[0], deltaY[0])) {
                        horizontal[0] = true;
                    } else if (shouldAbortHorizontalSwipe(deltaX[0], deltaY[0])) {
                        tracking[0] = false;
                        horizontal[0] = false;
                        if (tracker[0] != null) {
                            tracker[0].recycle();
                            tracker[0] = null;
                        }
                        hideSwipeAffordance(notesSwipeRail);
                        return false;
                    }
                }
                if (!horizontal[0]) return false;
                float max = dp(EDITOR_BACK_SWIPE_TRIGGER_DP);
                float progress = Math.min(1f, deltaX[0] / Math.max(1f, max));
                updateSwipeAffordance(notesSwipeRail, progress);
                float translation = -Math.min(deltaX[0], max);
                menuPanel.setTranslationX(translation);
                menuPanel.setAlpha(1f - (0.28f * progress));
                menuPanel.setScaleX(1f - (MOTION_SWIPE_SCALE_REDUCE_X * progress));
                menuPanel.setScaleY(1f - (MOTION_SWIPE_SCALE_REDUCE_Y * progress));
                target.requestDisallowInterceptTouchEvent(true);
                return true;
            }
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                if (tracker[0] != null) {
                    tracker[0].addMovement(event);
                    tracker[0].computeCurrentVelocity(1000);
                    velocityX[0] = tracker[0].getXVelocity();
                    velocityY[0] = tracker[0].getYVelocity();
                    tracker[0].recycle();
                    tracker[0] = null;
                }
                boolean trigger = shouldCompleteBackSwipe(deltaX[0], deltaY[0], velocityX[0], velocityY[0], false);
                if (trigger) {
                    completeSwipePanel(menuPanel, false, () -> {
                        if (current != null) openNote(current);
                    });
                } else {
                    settleSwipePanel(menuPanel);
                }
                hideSwipeAffordance(notesSwipeRail);
                tracking[0] = false;
                horizontal[0] = false;
                if (tracker[0] != null) {
                    tracker[0].recycle();
                    tracker[0] = null;
                }
                return true;
            }
            return false;
        });
    }

    private boolean canStartSheetDrag(View target, MotionEvent event) {
        if (target != sheetCard) return true;
        float y = event.getY();
        int height = sheetCard.getHeight() > 0 ? sheetCard.getHeight() : dp(420);
        return y <= Math.max(dp(170), height * SHEET_DRAG_START_RATIO);
    }

    private void settleSheetOpen() {
        if (sheetOverlay != null) {
            sheetOverlay.animate().cancel();
            withWorkflowMotion(sheetOverlay)
                    .alpha(1f)
                    .setDuration(MOTION_SHEET_RESTORE_MS)
                    .start();
        }
        if (sheetCard != null) {
            sheetCard.animate().cancel();
            withWorkflowMotion(sheetCard)
                    .translationY(0f)
                    .alpha(1f)
                    .setDuration(MOTION_SHEET_RESTORE_MS)
                    .start();
        }
        setSheetBackdropEnabled(true);
    }

    private void showSheet(String title, View content) {
        if (sheetOverlay == null || sheetCard == null || sheetBody == null || sheetTitle == null) return;
        if (sheetBodyScroll != null) {
            ViewGroup.LayoutParams lp = sheetBodyScroll.getLayoutParams();
            if (lp != null) {
                lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                sheetBodyScroll.setLayoutParams(lp);
            }
        }
        sheetTitle.setText(title);
        sheetBody.removeAllViews();
        sheetBody.addView(content);
        sheetCard.setBackground(oledSheetSurface(current != null ? current.accentColor : C_CYAN));
        boolean alreadyVisible = sheetOverlay.getVisibility() == View.VISIBLE;
        sheetOverlay.setVisibility(View.VISIBLE);
        sheetCard.post(() -> capSheetBodyHeight(0));
        if (!alreadyVisible) {
            sheetOverlay.setAlpha(0f);
            withWorkflowMotion(sheetOverlay).alpha(1f).setDuration(MOTION_SHEET_REVEAL_MS).start();
        } else {
            withWorkflowMotion(sheetOverlay).alpha(1f).setDuration(MOTION_SHEET_REVEAL_MS).start();
        }
        setSheetBackdropEnabled(true);
        sheetCard.animate().cancel();
        if (!alreadyVisible) {
            sheetCard.setTranslationY(dp(24));
            sheetCard.setAlpha(0f);
            withWorkflowMotion(sheetCard)
                    .translationY(0f)
                    .alpha(1f)
                    .setDuration(MOTION_SHEET_REVEAL_MS)
                    .start();
        } else {
            sheetCard.setTranslationY(0f);
            sheetCard.setAlpha(1f);
        }
    }

    private boolean shouldCompleteBackSwipe(float deltaX, float deltaY, float velocityX, float velocityY, boolean toRight) {
        if (deltaX < 0f) return false;
        float absVx = Math.abs(velocityX);
        float absVy = Math.abs(velocityY);
        if (absVx <= 0f) absVx = 1f;
        boolean enoughDistance = deltaX >= dp(EDITOR_BACK_SWIPE_TRIGGER_DP) * WORKFLOW_SWIPE_TRIGGER_RATIO;
        boolean straightEnough = deltaY <= deltaX * WORKFLOW_SWIPE_STRAIGHTNESS_RATIO;
        boolean isFlick = toRight
                ? velocityX > EDITOR_BACK_SWIPE_VELOCITY_PX && absVx > absVy * WORKFLOW_SWIPE_FLICK_RATIO
                : velocityX < -EDITOR_BACK_SWIPE_VELOCITY_PX && absVx > absVy * WORKFLOW_SWIPE_FLICK_RATIO;
        return (enoughDistance && straightEnough) || isFlick;
    }

    private boolean shouldTrackHorizontalSwipe(float deltaX, float deltaY) {
        return deltaX > dp(EDITOR_BACK_SWIPE_START_DP) && deltaX > deltaY * WORKFLOW_SWIPE_STRAIGHTNESS_RATIO;
    }

    private boolean shouldAbortHorizontalSwipe(float deltaX, float deltaY) {
        return deltaY > Math.max(dp(EDITOR_BACK_SWIPE_VERTICAL_BREAK_DP), deltaX * WORKFLOW_SWIPE_FLICK_RATIO);
    }

    private boolean shouldDismissSheet(float releaseDy, float velocityY) {
        return releaseDy > dp(SHEET_DISMISS_DISTANCE_DP) || velocityY > SHEET_DISMISS_VELOCITY;
    }

    private Drawable oledSheetSurface(int accent) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(Color.BLACK);
        d.setCornerRadius(dp(26));
        d.setStroke(dp(1), Color.argb(112, Color.red(accent), Color.green(accent), Color.blue(accent)));
        return d;
    }

    private Drawable oledCommandSurface(int radiusDp, int accent, boolean active) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(Color.BLACK);
        int alpha = active ? 152 : 74;
        d.setCornerRadius(dp(radiusDp));
        d.setStroke(dp(1), Color.argb(alpha, Color.red(accent), Color.green(accent), Color.blue(accent)));
        return d;
    }

    private Drawable dockSurfaceDrawable(int accent) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(Color.BLACK);
        d.setCornerRadius(dp(22));
        d.setStroke(dp(1), Color.argb(92, Color.red(accent), Color.green(accent), Color.blue(accent)));
        return d;
    }

    private Drawable dockButtonSurface(int accent, boolean active) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(Color.BLACK);
        d.setCornerRadius(dp(16));
        int alpha = active ? 190 : 64;
        d.setStroke(active ? dp(2) : dp(1), Color.argb(alpha, Color.red(accent), Color.green(accent), Color.blue(accent)));
        return d;
    }

    private Drawable swipeRailDrawable(int accent) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(Color.argb(218, Color.red(accent), Color.green(accent), Color.blue(accent)));
        d.setCornerRadius(dp(999));
        return d;
    }

    private Drawable sheetHandleDrawable(int accent) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(Color.argb(188, Color.red(accent), Color.green(accent), Color.blue(accent)));
        d.setCornerRadius(dp(999));
        return d;
    }

    private Drawable commandRailDrawable(int accent) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(Color.argb(206, Color.red(accent), Color.green(accent), Color.blue(accent)));
        d.setCornerRadius(dp(999));
        return d;
    }

    private View sheetDivider(int accent) {
        View divider = new View(this);
        divider.setBackgroundColor(Color.argb(58, Color.red(accent), Color.green(accent), Color.blue(accent)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(1));
        lp.topMargin = dimen(R.dimen.topflow_space_md);
        lp.bottomMargin = dimen(R.dimen.topflow_space_md);
        divider.setLayoutParams(lp);
        return divider;
    }

    private TextView sheetEyebrow(String text) {
        TextView t = new TextView(this);
        t.setText(text == null ? "" : text);
        t.setTextColor(TopFlowUiKit.MINT);
        t.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        t.setIncludeFontPadding(false);
        t.setLetterSpacing(0f);
        t.setSingleLine(true);
        t.setEllipsize(TextUtils.TruncateAt.END);
        return t;
    }

    private TextView sheetSectionTitle(String text) {
        TextView t = new TextView(this);
        t.setText(text == null ? "" : text);
        textStyle(t, R.style.TextAppearance_TopFlow21_Caption);
        t.setTextColor(TopFlowUiKit.TEXT_SOFT);
        t.setIncludeFontPadding(false);
        t.setLetterSpacing(0f);
        t.setPadding(0, dimen(R.dimen.topflow_space_md), 0, dimen(R.dimen.topflow_space_xs));
        t.setSingleLine(true);
        t.setEllipsize(TextUtils.TruncateAt.END);
        return t;
    }

    private View buildSheetMenuRow(String title, String subtitle, int detailColor, int accent, Runnable action) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dimen(R.dimen.topflow_space_md), dimen(R.dimen.topflow_space_md), dimen(R.dimen.topflow_space_md), dimen(R.dimen.topflow_space_md));
        row.setBackground(oledCommandSurface(16, accent, false));
        row.setForeground(TopFlowUiKit.ripple(accent));
        row.setClickable(true);
        row.setFocusable(true);
        row.setMinimumHeight(dp(56));
        TopFlowUiKit.applyFloating(row, 3);
        attachTapAnimation(row);

        View rail = new View(this);
        rail.setBackground(commandRailDrawable(accent));
        LinearLayout.LayoutParams railLp = new LinearLayout.LayoutParams(dp(3), dp(34));
        railLp.rightMargin = dimen(R.dimen.topflow_space_md);
        row.addView(rail, railLp);

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));

        TextView titleView = new TextView(this);
        titleView.setText(title == null ? "" : title);
        titleView.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        titleView.setTextColor(TopFlowUiKit.TEXT);
        titleView.setTextSize(15);
        titleView.setIncludeFontPadding(false);
        titleView.setLetterSpacing(0f);
        titleView.setSingleLine(true);
        titleView.setEllipsize(TextUtils.TruncateAt.END);
        copy.addView(titleView);

        if (subtitle != null && !subtitle.trim().isEmpty()) {
            TextView detail = new TextView(this);
            detail.setText(subtitle);
            detail.setTextColor(detailColor != 0 ? detailColor : TopFlowUiKit.TEXT_SOFT);
            detail.setTextSize(12);
            detail.setIncludeFontPadding(false);
            detail.setLetterSpacing(0f);
            detail.setSingleLine(true);
            detail.setEllipsize(TextUtils.TruncateAt.END);
            copy.addView(detail);
        }

        row.addView(copy);

        TextView arrow = new TextView(this);
        arrow.setText("›");
        arrow.setTextColor(TopFlowUiKit.MINT);
        arrow.setTextSize(18);
        arrow.setPadding(dp(8), 0, 0, 0);
        arrow.setIncludeFontPadding(false);
        row.addView(arrow);

        if (action != null) {
            row.setOnClickListener(v -> runSelectionAnimation(v, action));
        }

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = dimen(R.dimen.topflow_space_sm);
        row.setLayoutParams(lp);
        return row;
    }

    private void dismissSheet() {
        if (sheetOverlay == null || sheetOverlay.getVisibility() != View.VISIBLE) return;
        int travel = sheetCard != null && sheetCard.getHeight() > 0 ? sheetCard.getHeight() + dp(48) : dp(260);
        setSheetBackdropEnabled(false);
        if (sheetCard != null) {
            sheetCard.animate().cancel();
            withWorkflowMotion(sheetCard)
                    .translationY(travel)
                    .alpha(0f)
                    .setDuration(MOTION_SHEET_DISMISS_MS)
                    .start();
        }
        if (sheetOverlay != null) {
            sheetOverlay.animate().cancel();
            withWorkflowMotion(sheetOverlay).alpha(0f).setDuration(MOTION_SHEET_DISMISS_MS).withEndAction(() -> {
                sheetOverlay.setVisibility(View.GONE);
                if (sheetBody != null) sheetBody.removeAllViews();
                if (sheetBodyScroll != null) {
                    ViewGroup.LayoutParams lp = sheetBodyScroll.getLayoutParams();
                    if (lp != null) {
                        lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                        sheetBodyScroll.setLayoutParams(lp);
                    }
                }
                restoreDockStateAfterSheetDismiss();
            }).start();
        } else {
            restoreDockStateAfterSheetDismiss();
        }
    }

    private void restoreDockStateAfterSheetDismiss() {
        if (menuPanel != null && menuPanel.getVisibility() == View.VISIBLE) {
            setActiveDockState(DOCK_STATE_NOTES);
        } else {
            setActiveDockState(DOCK_STATE_NONE);
        }
    }

    private android.view.ViewPropertyAnimator withWorkflowMotion(View view) {
        if (view == null) return null;
        return view.animate()
                .setInterpolator(MOTION_INTERPOLATOR)
                .withLayer();
    }

    private void capSheetBodyHeight(int attempt) {
        if (sheetCard == null || sheetBodyScroll == null || sheetDragHandle == null || sheetHeader == null) return;
        if (attempt > SHEET_SCROLL_CAP_ATTEMPTS) return;
        if (sheetCard.getHeight() <= 0 || sheetDragHandle.getHeight() <= 0 || sheetHeader.getHeight() <= 0) {
            sheetCard.post(() -> capSheetBodyHeight(attempt + 1));
            return;
        }
        int maxHeight = (int) (getResources().getDisplayMetrics().heightPixels * 0.86f);
        if (sheetCard.getHeight() <= maxHeight) return;
        int available = maxHeight
                - sheetDragHandle.getHeight()
                - sheetHeader.getHeight()
                - sheetCard.getPaddingTop()
                - sheetCard.getPaddingBottom()
                - dimen(R.dimen.topflow_space_sm);
        if (available <= dp(180)) return;
        ViewGroup.LayoutParams lp = sheetBodyScroll.getLayoutParams();
        if (lp == null) return;
        lp.height = Math.max(dp(180), available);
        sheetBodyScroll.setLayoutParams(lp);
    }

    private void setActiveDockState(int state) {
        if (activeDockState == state) return;
        activeDockState = state;
        applyDockButtonState(dockNotesButton, state == DOCK_STATE_NOTES);
        applyDockButtonState(dockRhymeButton, state == DOCK_STATE_RHYME);
        applyDockButtonState(dockStyleButton, state == DOCK_STATE_STYLE);
        applyDockButtonState(dockSettingsButton, state == DOCK_STATE_SETTINGS);
    }

    private void applyDockButtonState(Button button, boolean active) {
        if (button == null) return;
        button.animate().cancel();
        applyDockButtonVisual(button, active);
        withWorkflowMotion(button)
                .alpha(active ? MOTION_DOCK_ACTIVE_ALPHA : MOTION_DOCK_INACTIVE_ALPHA)
                .scaleX(active ? MOTION_DOCK_ACTIVE_SCALE : MOTION_DOCK_INACTIVE_SCALE)
                .scaleY(active ? MOTION_DOCK_ACTIVE_SCALE : MOTION_DOCK_INACTIVE_SCALE)
                .translationY(active ? -dp(2) : 0)
                .setDuration(MOTION_DOCK_FEEDBACK_MS)
                .start();
    }

    private void setSheetBackdropEnabled(boolean active) {
        applySheetBlur(active);
        animateSheetBackdrop(active);
    }

    private void applySheetBlur(boolean active) {
        if (shell == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (active) {
                shell.setRenderEffect(RenderEffect.createBlurEffect(
                        getResources().getDisplayMetrics().density * SHEET_BLUR_RADIUS_DP,
                        getResources().getDisplayMetrics().density * SHEET_BLUR_RADIUS_DP,
                        Shader.TileMode.CLAMP));
            } else {
                shell.setRenderEffect(null);
            }
        }
    }

    private void animateSheetBackdrop(boolean active) {
        if (shell == null) return;
        boolean legacy = Build.VERSION.SDK_INT < Build.VERSION_CODES.S;
        withWorkflowMotion(shell)
                .alpha(active ? 0.82f : 1f)
                .scaleX(active ? (legacy ? 0.985f : 1f) : 1f)
                .scaleY(active ? (legacy ? 0.985f : 1f) : 1f)
                .setDuration(active ? MOTION_SHEET_REVEAL_MS : MOTION_SHEET_DISMISS_MS)
                .start();
    }

    private void checkForUpdates(boolean manual) {
        prefs.edit().putLong("lastUpdateCheck", System.currentTimeMillis()).apply();
        new Thread(() -> {
            try {
                String json = readUpdateManifest();
                ArrayList<AvailableUpdate> updates = parseUpdateManifest(json);
                ArrayList<AvailableUpdate> eligible = new ArrayList<>();
                for (AvailableUpdate update : updates) {
                    if (update.versionCode <= BuildConfig.VERSION_CODE) continue;
                    if (update.apkUrl == null || update.apkUrl.trim().isEmpty()) continue;
                    eligible.add(update);
                }
                Collections.sort(eligible, (a, b) -> Integer.compare(b.versionCode, a.versionCode));
                if (!eligible.isEmpty()) {
                    if (eligible.size() == 1) {
                        AvailableUpdate update = eligible.get(0);
                        runOnUiThread(() -> showUpdateFound(update.versionName, update.apkUrl));
                        notifyUpdate(update.versionName);
                    } else {
                        runOnUiThread(() -> showUpdateChooser(eligible));
                        notifyUpdate(eligible.get(0).versionName);
                    }
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

    private static class AvailableUpdate {
        final int versionCode;
        final String versionName;
        final String notes;
        final String apkUrl;

        AvailableUpdate(int versionCode, String versionName, String notes, String apkUrl) {
            this.versionCode = versionCode;
            this.versionName = versionName;
            this.notes = notes;
            this.apkUrl = apkUrl;
        }
    }

    private ArrayList<AvailableUpdate> parseUpdateManifest(String json) throws Exception {
        JSONObject manifest = new JSONObject(json);
        ArrayList<AvailableUpdate> updates = new ArrayList<>();
        if (manifest.has("versions")) {
            JSONArray versions = manifest.optJSONArray("versions");
            if (versions == null) return updates;
            for (int i = 0; i < versions.length(); i++) {
                JSONObject item = versions.optJSONObject(i);
                AvailableUpdate parsed = parseUpdateItem(item);
                if (parsed != null) updates.add(parsed);
            }
            return updates;
        }

        AvailableUpdate parsed = parseUpdateItem(manifest);
        if (parsed != null) {
            updates.add(parsed);
        } else {
            throw new Exception("Invalid legacy update manifest");
        }
        return updates;
    }

    private AvailableUpdate parseUpdateItem(JSONObject item) {
        if (item == null) return null;
        if (!item.has("versionCode") || !item.has("apkUrl")) return null;
        int versionCode;
        try {
            versionCode = item.getInt("versionCode");
        } catch (Exception ignored) {
            return null;
        }
        String versionName = item.optString("versionName", String.valueOf(versionCode));
        String notes = item.optString("notes", "");
        String apkUrl = item.optString("apkUrl", "");
        return new AvailableUpdate(versionCode, versionName, notes, apkUrl);
    }

    private void showUpdateChooser(ArrayList<AvailableUpdate> updates) {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0, 0, 0, dimen(R.dimen.topflow_space_sm));

        for (int i = 0; i < updates.size(); i++) {
            AvailableUpdate update = updates.get(i);
            View row = buildUpdateChoiceRow(update);
            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, -2);
            if (i < updates.size() - 1) rowLp.bottomMargin = dimen(R.dimen.topflow_space_sm);
            content.addView(row, rowLp);
        }
        showSheet("Choose an update", content);
    }

    private View buildUpdateChoiceRow(AvailableUpdate update) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dimen(R.dimen.topflow_space_md), dimen(R.dimen.topflow_space_md), dimen(R.dimen.topflow_space_md), dimen(R.dimen.topflow_space_md));
        card.setBackground(oledCommandSurface(18, C_CYAN, true));
        TopFlowUiKit.applyFloating(card, 6);
        card.setMinimumHeight(dp(132));

        TextView title = new TextView(this);
        title.setText("Version " + update.versionName + " · " + update.versionCode);
        textStyle(title, R.style.TextAppearance_TopFlow21_Section);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setTextColor(TopFlowUiKit.TEXT);
        title.setLetterSpacing(0f);
        title.setIncludeFontPadding(false);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        card.addView(title);

        if (update.notes != null && !update.notes.trim().isEmpty()) {
            TextView notes = new TextView(this);
            notes.setText(update.notes.trim());
            textStyle(notes, R.style.TextAppearance_TopFlow21_Caption);
            TopFlowUiKit.applyQuietText(notes);
            notes.setLetterSpacing(0f);
            notes.setPadding(0, dimen(R.dimen.topflow_space_sm), 0, dimen(R.dimen.topflow_space_sm));
            notes.setIncludeFontPadding(false);
            notes.setMaxLines(4);
            notes.setEllipsize(TextUtils.TruncateAt.END);
            card.addView(notes);
        }

        Button action = button("Download");
        action.setOnClickListener(v -> {
            dismissSheet();
            downloadAndInstall(update.apkUrl);
        });
        card.addView(action, new LinearLayout.LayoutParams(-1, -2));
        return card;
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
        saveCurrentDraft();
        if (songPlayer != null && songPlayer.isPlaying()) {
            songResumePositionMs = songPlayer.getCurrentPosition();
            songPlayer.pause();
        }
        if (recordingPlayer != null && recordingPlayer.isPlaying()) recordingPlayer.pause();
        editHandler.removeCallbacks(suggestionRunnable);
        cancelSuggestionJob();
        cancelExpandedRhymeJob();
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

    private void saveCurrentDraft() {
        if (!bodyDraftDirty || current == null || bodyInput == null) return;
        long started = System.currentTimeMillis();
        Editable text = bodyInput.getText();
        current.body = text == null ? "" : text.toString();
        bodyDraftDirty = false;
        saveNotes();
        logRhymeTrace("draft_save", started, "noteLen=" + current.body.length());
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

    private View buildStyleHubRow(String title, String value, View preview, Runnable action) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dimen(R.dimen.topflow_space_lg), dimen(R.dimen.topflow_space_md), dimen(R.dimen.topflow_space_md), dimen(R.dimen.topflow_space_md));
        int accent = current != null ? current.accentColor : C_CYAN;
        row.setBackground(oledCommandSurface(16, accent, false));
        row.setForeground(TopFlowUiKit.ripple(current != null ? current.accentColor : C_CYAN));
        row.setClickable(true);
        row.setFocusable(true);
        row.setMinimumHeight(dp(72));
        TopFlowUiKit.applyFloating(row, 3);
        attachTapAnimation(row);
        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));
        TextView titleView = label(title);
        titleView.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        titleView.setPadding(0, 0, 0, dimen(R.dimen.topflow_space_xs));
        copy.addView(titleView);
        copy.addView(settingsValuePill(value == null ? "" : value));
        row.addView(copy);
        if (preview != null) {
            LinearLayout previewHost = new LinearLayout(this);
            previewHost.setPadding(0, 0, 0, 0);
            previewHost.setGravity(Gravity.CENTER);
            previewHost.setBackground(oledCommandSurface(10, accent, false));
            previewHost.setMinimumWidth(dp(102));
            previewHost.setMinimumHeight(dp(46));
            previewHost.setLayoutParams(new LinearLayout.LayoutParams(dp(102), dp(46)));
            TopFlowUiKit.applyFloating(previewHost, 4);
            LinearLayout.LayoutParams inner = new LinearLayout.LayoutParams(dp(90), dp(36));
            previewHost.addView(preview, inner);
            row.addView(previewHost);
        }
        if (action != null) row.setOnClickListener(v -> runSelectionAnimation(v, action));
        row.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) row.getLayoutParams();
        lp.bottomMargin = dimen(R.dimen.topflow_space_sm);
        return row;
    }

    private String formatColor(int color) {
        return String.format(Locale.US, "#%06X", 0xFFFFFF & color);
    }

    private View buildColorSample(int color) {
        View sample = new View(this);
        sample.setMinimumWidth(dp(80));
        sample.setMinimumHeight(dp(32));
        sample.setBackground(buildColorPanelDrawable(color));
        return sample;
    }

    private Drawable buildColorPanelDrawable(int color) {
        GradientDrawable d = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{
                        blendColor(color, Color.WHITE, 0.12f),
                        color,
                        blendColor(color, Color.BLACK, 0.22f)
                });
        d.setCornerRadius(dp(10));
        d.setStroke(dp(1), Color.argb(150, Color.red(color), Color.green(color), Color.blue(color)));
        return d;
    }

    private View buildSliderRow(String sliderLabel, SeekBar bar) {
        LinearLayout section = new LinearLayout(this);
        section.setOrientation(LinearLayout.VERTICAL);
        section.setBackground(oledCommandSurface(16, current != null ? current.accentColor : C_CYAN, false));
        section.setPadding(dimen(R.dimen.topflow_space_md), dimen(R.dimen.topflow_space_sm), dimen(R.dimen.topflow_space_md), dimen(R.dimen.topflow_space_sm));
        section.setLayoutParams(settingsSectionLayoutParams());
        TopFlowUiKit.applyFloating(section, 6);
        TextView title = label(sliderLabel);
        title.setTextColor(TopFlowUiKit.TEXT);
        title.setPadding(0, 0, 0, dimen(R.dimen.topflow_space_xs));
        section.addView(title);
        if (bar != null) {
            styleSeekBar(bar, current != null ? current.accentColor : C_CYAN);
            section.addView(bar, new LinearLayout.LayoutParams(-1, -2));
        }
        return section;
    }

    private View buildFontMiniPreview(String fontId) {
        TextView t = new TextView(this);
        t.setText("Aa");
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        t.setTypeface(TopFlowUiKit.fontForPreview(this, fontId));
        t.setTextColor(C_TEXT);
        t.setGravity(Gravity.CENTER);
        t.setIncludeFontPadding(false);
        t.setSingleLine(true);
        t.setEllipsize(TextUtils.TruncateAt.END);
        return t;
    }

    private View buildTextSizePreview(int sizeSp) {
        TextView t = new TextView(this);
        t.setText("Aa");
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, Math.max(12f, Math.min(32f, sizeSp)));
        t.setTypeface(TopFlowUiKit.fontForPreview(this, current == null ? "sans" : current.font));
        t.setTextColor(C_TEXT);
        t.setGravity(Gravity.CENTER);
        t.setIncludeFontPadding(false);
        return t;
    }

    private View buildGlowPreview(int accent) {
        LinearLayout glow = new LinearLayout(this);
        glow.setOrientation(LinearLayout.HORIZONTAL);
        glow.setGravity(Gravity.CENTER);
        glow.setPadding(dimen(R.dimen.topflow_space_xs), dimen(R.dimen.topflow_space_xs), dimen(R.dimen.topflow_space_xs), dimen(R.dimen.topflow_space_xs));
        glow.setBackground(TopFlowUiKit.oledSurface(this, 10, Color.rgb(6, 8, 12), Color.argb(130, Color.red(accent), Color.green(accent), Color.blue(accent))));
        View marker = new View(this);
        marker.setBackground(glowDrawable(accent, true, 3));
        glow.addView(marker, new LinearLayout.LayoutParams(dp(54), dp(18)));
        return glow;
    }

    private View buildGlowDemoPanel(int accent, boolean enabled, int strength) {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dimen(R.dimen.topflow_space_md), dimen(R.dimen.topflow_space_md), dimen(R.dimen.topflow_space_md), dimen(R.dimen.topflow_space_md));
        panel.setBackground(TopFlowUiKit.floatingPanel(this, 12));
        panel.setMinimumHeight(dp(120));
        TextView sample = new TextView(this);
        sample.setText("Glow preview");
        sample.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        sample.setTextColor(Color.argb(230, Color.red(C_TEXT), Color.green(C_TEXT), Color.blue(C_TEXT)));
        sample.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        sample.setIncludeFontPadding(false);
        panel.addView(sample);
        View glowBand = new View(this);
        glowBand.setBackground(glowDrawable(accent, enabled, strength));
        LinearLayout.LayoutParams bandLp = new LinearLayout.LayoutParams(-1, dp(42));
        bandLp.topMargin = dimen(R.dimen.topflow_space_sm);
        panel.addView(glowBand, bandLp);
        return panel;
    }

    private Drawable glowDrawable(int accent, boolean enabled, int strength) {
        if (!enabled || strength <= 0) {
            return TopFlowUiKit.oledSurface(this, 10);
        }
        return new NeonGlowDrawable(accent, true, strength, dp(10), dp(8));
    }

    private View buildLiveTextPreview(String sample, String fontId, int textSizeSp, int textColor) {
        TextView preview = new TextView(this);
        preview.setPadding(dimen(R.dimen.topflow_space_md), dimen(R.dimen.topflow_space_md), dimen(R.dimen.topflow_space_md), dimen(R.dimen.topflow_space_md));
        preview.setText(sample == null ? "" : sample);
        preview.setTypeface(TopFlowUiKit.fontForPreview(this, fontId));
        preview.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp);
        preview.setTextColor(textColor);
        int previewFill = current == null ? Color.rgb(7, 9, 13) : current.noteColor;
        int previewStroke = Color.argb(145, Color.red(textColor), Color.green(textColor), Color.blue(textColor));
        preview.setBackground(TopFlowUiKit.oledSurface(this, 14, previewFill, previewStroke));
        preview.setIncludeFontPadding(false);
        preview.setSingleLine(false);
        preview.setEllipsize(TextUtils.TruncateAt.END);
        preview.setMaxLines(1);
        preview.setLineSpacing(0f, 1.05f);
        return preview;
    }

    private void updateTextPreview(View preview, int textSizeSp, String fontId, int textColor) {
        if (!(preview instanceof TextView)) return;
        TextView label = (TextView) preview;
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp);
        label.setTypeface(TopFlowUiKit.fontForPreview(this, fontId));
        label.setTextColor(textColor);
    }

    private TextView settingsValuePill(String text) {
        TextView pill = new TextView(this);
        pill.setText(text == null ? "" : text);
        pill.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        pill.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        pill.setTextColor(TopFlowUiKit.TEXT);
        pill.setBackground(chipDrawable(current != null ? current.accentColor : C_CYAN));
        pill.setPadding(dimen(R.dimen.topflow_space_sm), dimen(R.dimen.topflow_space_xs), dimen(R.dimen.topflow_space_sm), dimen(R.dimen.topflow_space_xs));
        pill.setIncludeFontPadding(false);
        pill.setLetterSpacing(0f);
        pill.setSingleLine(true);
        pill.setEllipsize(TextUtils.TruncateAt.END);
        return pill;
    }

    private LinearLayout.LayoutParams settingsSectionLayoutParams() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = dimen(R.dimen.topflow_space_sm);
        return lp;
    }

    private TextView label(String text) {
        TextView v = new TextView(this);
        v.setText(text);
        textStyle(v, R.style.TextAppearance_TopFlow_Section);
        v.setPadding(0, dimen(R.dimen.topflow_space_xs), 0, dimen(R.dimen.topflow_space_xs));
        v.setIncludeFontPadding(false);
        v.setLetterSpacing(0f);
        return v;
    }

    private TextView metadataLabel(String text) {
        TextView v = new TextView(this);
        v.setText(text);
        textStyle(v, R.style.TextAppearance_TopFlow21_Caption);
        TopFlowUiKit.applyQuietText(v);
        v.setLetterSpacing(0f);
        v.setPadding(0, dimen(R.dimen.topflow_space_xs), 0, dimen(R.dimen.topflow_space_xs));
        return v;
    }

    private Button button(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setFocusable(false);
        b.setFocusableInTouchMode(false);
        styleButton(b, current != null ? current.accentColor : C_GREEN);
        applyButtonIcon(b, text);
        attachTapAnimation(b);
        return b;
    }

    private void styleButton(Button b, int accent) {
        b.setBackgroundResource(R.drawable.bg21_quiet_control);
        b.setTextColor(TopFlowUiKit.TEXT);
        textSize(b, R.dimen.topflow21_text_label);
        b.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
        b.setMinHeight(dp(48));
        b.setMinWidth(dp(58));
        TopFlowUiKit.applyFloating(b, 3);
        b.setStateListAnimator(null);
        b.setIncludeFontPadding(false);
        b.setLetterSpacing(0f);
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
        chip.setBackground(oledCommandSurface(999, accent, true));
        chip.setTextColor(TopFlowUiKit.TEXT);
        textSize(chip, R.dimen.topflow21_text_label);
        chip.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        chip.setMinHeight(dimen(R.dimen.topflow_chip_height));
        chip.setMinWidth(dp(62));
        TopFlowUiKit.applyFloating(chip, 2);
        chip.setStateListAnimator(null);
        chip.setIncludeFontPadding(false);
        chip.setLetterSpacing(0f);
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
        Drawable[] drawables = button.getCompoundDrawablesRelative();
        if (drawables != null && drawables[0] != null) {
            drawables[0].setTintList(ColorStateList.valueOf(button.getCurrentTextColor()));
        }
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
        if (label.equals("rhyme")) return R.drawable.ic_rhyme_24;
        if (label.contains("expanded rhymes") || label.equals("rhymes")) return R.drawable.ic_rhyme_24;
        if (label.contains("rhyme settings") || label.equals("settings") || label.startsWith("set") || label.equals("tune")) return R.drawable.ic_settings_24;
        if (label.contains("check for updates")) return R.drawable.ic_update_24;
        if (label.contains("deleted rhymes")) return R.drawable.ic_restore_24;
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
        label.setLetterSpacing(0f);
        label.setPadding(0, 0, 0, dimen(R.dimen.topflow_space_sm));
        card.addView(label);
        return card;
    }

    private int lyricLineCount(String body) {
        if (body == null || body.isEmpty()) return 0;
        int lines = 1;
        for (int i = 0; i < body.length(); i++) {
            if (body.charAt(i) == '\n') lines++;
        }
        return lines;
    }

    private int lyricWordCount(String body) {
        if (body == null) return 0;
        String text = body.trim();
        if (text.isEmpty()) return 0;
        int words = 0;
        boolean inWord = false;
        for (int i = 0; i < text.length(); i++) {
            if (Character.isWhitespace(text.charAt(i))) {
                if (inWord) {
                    words++;
                    inWord = false;
                }
            } else {
                inWord = true;
            }
        }
        if (inWord) words++;
        return words;
    }

    private String noteMetadataLine(Note note) {
        if (note == null) return "0 lines · 0 words";
        int lines = lyricLineCount(note.body);
        int words = lyricWordCount(note.body);
        return lines + " line" + (lines == 1 ? "" : "s") + " · " + words + " word" + (words == 1 ? "" : "s");
    }

    private String noteTitleForDisplay(Note note) {
        if (note == null || note.title == null || note.title.trim().isEmpty()) return "Untitled";
        return note.title.trim();
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

    private Drawable noteShellDrawable(int accent, boolean glow, int strength) {
        int strokeAlpha = glow ? Math.min(230, 96 + Math.max(0, strength) * 36) : 58;
        GradientDrawable d = new GradientDrawable();
        d.setColor(Color.BLACK);
        d.setCornerRadius(dp(26));
        d.setStroke(dp(1), Color.argb(strokeAlpha, Color.red(accent), Color.green(accent), Color.blue(accent)));
        return d;
    }

    private Drawable editorRailDrawable(int accent) {
        int glow = Color.argb(212, Color.red(accent), Color.green(accent), Color.blue(accent));
        int fade = Color.argb(36, Color.red(accent), Color.green(accent), Color.blue(accent));
        GradientDrawable d = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{glow, TopFlowUiKit.OLED, fade, TopFlowUiKit.OLED}
        );
        d.setCornerRadius(dp(999));
        return d;
    }

    private Drawable editorFieldDrawable(int color, int accent, int radiusDp) {
        GradientDrawable d = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{
                        blendColor(color, TopFlowUiKit.OLED, 0.18f),
                        TopFlowUiKit.OLED,
                        blendColor(color, TopFlowUiKit.OLED, 0.06f)
                }
        );
        d.setCornerRadius(dp(radiusDp));
        d.setStroke(dp(1), Color.argb(132, Color.red(accent), Color.green(accent), Color.blue(accent)));
        return d;
    }

    private Drawable noteGlowDrawable(int accent, boolean glow, int strength) {
        return new NeonGlowDrawable(accent, glow, Math.max(0, Math.min(4, strength)), dp(28), dp(18));
    }

    private Drawable notePageDrawable(int color, int accent, int radiusDp) {
        GradientDrawable d = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{blendColor(color, Color.WHITE, 0.035f), color, blendColor(color, Color.BLACK, 0.055f)}
        );
        d.setCornerRadius(dp(radiusDp));
        d.setStroke(dp(1), Color.argb(138, Color.red(accent), Color.green(accent), Color.blue(accent)));
        return d;
    }

    private Drawable dragHandleDrawable() {
        GradientDrawable d = new GradientDrawable();
        d.setColor(Color.argb(168, 190, 204, 226));
        d.setCornerRadius(dp(999));
        return d;
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

    private void runSelectionAnimation(View view, Runnable action) {
        if (view == null) {
            if (action != null) action.run();
            return;
        }
        view.animate().cancel();
        withWorkflowMotion(view)
                .scaleX(MOTION_SELECTION_SCALE)
                .scaleY(MOTION_SELECTION_SCALE)
                .alpha(MOTION_SELECTION_ALPHA)
                .setDuration(MOTION_SELECTION_PRESS_MS)
                .withEndAction(() -> {
                    if (action != null) action.run();
                    withWorkflowMotion(view).scaleX(MOTION_TAP_RELEASE_SCALE).scaleY(MOTION_TAP_RELEASE_SCALE).alpha(1f).setDuration(MOTION_SELECTION_RELEASE_MS).start();
                })
                .start();
    }

    private void attachTapAnimation(View v) {
        v.setOnTouchListener((view, e) -> {
            if (e.getAction() == MotionEvent.ACTION_DOWN) {
                withWorkflowMotion(view)
                        .scaleX(MOTION_TAP_PRESS_SCALE)
                        .scaleY(MOTION_TAP_PRESS_SCALE)
                        .translationY(dp(1))
                        .setDuration(MOTION_TAP_PRESS_MS)
                        .start();
            } else if (e.getAction() == MotionEvent.ACTION_UP || e.getAction() == MotionEvent.ACTION_CANCEL) {
                withWorkflowMotion(view)
                        .scaleX(MOTION_TAP_RELEASE_SCALE)
                        .scaleY(MOTION_TAP_RELEASE_SCALE)
                        .translationY(0f)
                        .setDuration(MOTION_TAP_RELEASE_MS)
                        .start();
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

    private int editorTextLength() {
        if (bodyInput == null || bodyInput.getText() == null) return 0;
        return bodyInput.getText().length();
    }

    private int safeEditorCursor() {
        if (bodyInput == null) return 0;
        return Math.max(0, Math.min(bodyInput.getSelectionStart(), editorTextLength()));
    }

    private void logRhymeTrace(String stage, long startedAt, String detail) {
        if (!RHYME_TRACE) return;
        long ms = startedAt > 0L ? System.currentTimeMillis() - startedAt : 0L;
        String thread = Looper.myLooper() == Looper.getMainLooper() ? "main" : "bg";
        Log.d(TAG, "rhyme_trace stage=" + stage + " ms=" + ms + " thread=" + thread + " " + detail);
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
        int fontSizeSp = DEFAULT_EDITOR_FONT_SIZE_SP;
        int noteColor = DEFAULT_NOTE_COLOR;
        int textColor = DEFAULT_NOTE_TEXT_COLOR;
        int accentColor = DEFAULT_NOTE_ACCENT_COLOR;
        boolean noteGlow = false;
        int glowStrength = DEFAULT_NOTE_GLOW_STRENGTH;
        String songUri = "";
        ArrayList<RecordingTag> recordings = new ArrayList<>();

        static Note create(String title) {
            Note n = new Note();
            n.title = title;
            n.body = "";
            n.noteColor = DEFAULT_NOTE_COLOR;
            n.textColor = DEFAULT_NOTE_TEXT_COLOR;
            n.accentColor = DEFAULT_NOTE_ACCENT_COLOR;
            n.fontSizeSp = DEFAULT_EDITOR_FONT_SIZE_SP;
            n.noteGlow = false;
            n.glowStrength = DEFAULT_NOTE_GLOW_STRENGTH;
            return n;
        }

        JSONObject toJson() throws Exception {
            JSONObject o = new JSONObject();
            o.put("title", title);
            o.put("body", body);
            o.put("font", font);
            o.put("fontSizeSp", fontSizeSp);
            o.put("noteColor", noteColor);
            o.put("textColor", textColor);
            o.put("accentColor", accentColor);
            o.put("noteGlow", noteGlow);
            o.put("glowStrength", glowStrength);
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
            n.fontSizeSp = Math.max(MIN_EDITOR_FONT_SIZE_SP, Math.min(MAX_EDITOR_FONT_SIZE_SP, o.optInt("fontSizeSp", DEFAULT_EDITOR_FONT_SIZE_SP)));
            n.noteColor = o.optInt("noteColor", DEFAULT_NOTE_COLOR);
            n.textColor = o.optInt("textColor", DEFAULT_NOTE_TEXT_COLOR);
            n.accentColor = o.optInt("accentColor", DEFAULT_NOTE_ACCENT_COLOR);
            n.noteGlow = o.optBoolean("noteGlow", false);
            n.glowStrength = Math.max(0, Math.min(4, o.optInt("glowStrength", DEFAULT_NOTE_GLOW_STRENGTH)));
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
            line.setColor(Color.argb(10, 130, 150, 210));
            line.setStrokeWidth(1f);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            int w = getWidth();
            int h = getHeight();
            paint.setShader(new android.graphics.LinearGradient(0, 0, 0, h,
                    new int[]{Color.rgb(3, 5, 10), Color.rgb(22, 27, 49), Color.rgb(3, 5, 10)},
                    new float[]{0f, 0.55f, 1f}, android.graphics.Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, paint);

            paint.setShader(new android.graphics.RadialGradient(w * 0.2f, h * 0.15f, Math.max(w, h) * 0.8f,
                    new int[]{Color.argb(40, 90, 215, 160), Color.argb(0, 90, 215, 160)},
                    new float[]{0f, 1f}, android.graphics.Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, paint);

            paint.setShader(new android.graphics.RadialGradient(w * 0.8f, h * 0.2f, Math.max(w, h) * 0.65f,
                    new int[]{Color.argb(28, 98, 199, 255), Color.argb(0, 98, 199, 255)},
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
            // Kept for compatibility with existing style wiring; notebook rules are intentionally disabled.
        }

        @Override
        protected void onSelectionChanged(int selStart, int selEnd) {
            super.onSelectionChanged(selStart, selEnd);
            if (selectionListener != null) selectionListener.onSelectionChanged();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
        }
    }

    static class NeonGlowDrawable extends Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();
        private final int accent;
        private final boolean glow;
        private final int strength;
        private final float radius;
        private final float spread;

        NeonGlowDrawable(int accent, boolean glow, int strength, float radius, float spread) {
            this.accent = accent;
            this.glow = glow;
            this.strength = strength;
            this.radius = radius;
            this.spread = spread;
            paint.setStyle(Paint.Style.STROKE);
        }

        @Override
        public void draw(Canvas canvas) {
            if (!glow || strength <= 0) return;
            int layers = 4 + strength;
            float left = getBounds().left + spread * 0.55f;
            float top = getBounds().top + spread * 0.45f;
            float right = getBounds().right - spread * 0.55f;
            float bottom = getBounds().bottom - spread * 0.55f;
            for (int i = layers; i >= 1; i--) {
                float progress = i / (float) layers;
                float inset = spread * progress;
                int alpha = Math.min(150, 10 + strength * 12 + (layers - i) * 8);
                paint.setStrokeWidth(Math.max(2f, spread * (1.1f - progress * 0.62f)));
                paint.setColor(Color.argb(alpha, Color.red(accent), Color.green(accent), Color.blue(accent)));
                rect.set(left + inset, top + inset, right - inset, bottom - inset);
                canvas.drawRoundRect(rect, radius + inset, radius + inset, paint);
            }
            paint.setStrokeWidth(2.5f);
            paint.setColor(Color.argb(Math.min(210, 80 + strength * 32), Color.red(accent), Color.green(accent), Color.blue(accent)));
            rect.set(left + spread, top + spread, right - spread, bottom - spread);
            canvas.drawRoundRect(rect, radius, radius, paint);
        }

        @Override
        public void setAlpha(int alpha) {
            paint.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(android.graphics.ColorFilter colorFilter) {
            paint.setColorFilter(colorFilter);
        }

        @Override
        public int getOpacity() {
            return android.graphics.PixelFormat.TRANSLUCENT;
        }
    }
}
