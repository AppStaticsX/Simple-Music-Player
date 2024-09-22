package com.appstaticsx.app.musico;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity implements AudioManager.OnAudioFocusChangeListener {

    private static final int REQUEST_STORAGE_PERMISSION = 100;
    private static final int REQUEST_MANAGE_STORAGE_PERMISSION = 101;
    private static final long FIVE_MINUTES_IN_MILLIS = 15 * 60 * 1000;
    private static final long TEN_MINUTES_IN_MILLIS = 30 * 60 * 1000;
    private static final int SKIP_TIME = 10000;
    private ListView listView;
    private MediaPlayer mediaPlayer;
    private ImageButton playButton;
    private ImageButton nextButton;
    private ImageButton prevButton;
    private SeekBar seekBar;
    private TextView startTime; // TextView for current position
    private TextView endTime; // TextView for song length
    private TextView songArtist_;
    private TextView songTitle_;
    private ImageView songAlbum_;
    private LinearLayout songAlbumll;
    private ImageButton shuffleButton;
    private ArrayList<Integer> shuffledIndexList = new ArrayList<>(); // To store shuffled indices
    private boolean isShuffle = false;
    private ImageButton repeatButton;
    private boolean isRepeat = false;
    private ImageButton menuButton;
    private Button sleepTimerButton;
    private CountDownTimer countDownTimer;
    private long timeLeftInMillis;
    private boolean isTimerRunning = false;
    private int timerState = 0; // 0 = timer off, 1 = 5 min timer, 2 = 10 min timer
    private PopupWindow popupWindow;
    private boolean isPopupOpen = false;
    private AudioManager mAudioManager;
    private LinearLayout songdetails_ll;

    private ArrayList<HashMap<String, Object>> mp3List = new ArrayList<>();
    private int currentSongIndex = -1; // To keep track of the currently playing song
    private boolean isPlaying = false;
    private int currentPosition = 0; // Track current playback position
    private String currentFilePath = null; // Track current file path
    private Handler handler = new Handler(); // For updating SeekBar progress

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listView = findViewById(R.id.listView);
        playButton = findViewById(R.id.play);
        nextButton = findViewById(R.id.next);
        prevButton = findViewById(R.id.prev);
        seekBar = findViewById(R.id.simpleSeekBar);
        startTime = findViewById(R.id.start_time); // Initialize TextView for current position
        endTime = findViewById(R.id.end_time); // Initialize TextView for song length
        songTitle_ = findViewById(R.id.songTitle);
        songArtist_ = findViewById(R.id.songArtist);
        songAlbum_ = findViewById(R.id.songAlbum);
        songAlbumll = findViewById(R.id.songAlbumll);
        shuffleButton = findViewById(R.id.shuffle);
        repeatButton = findViewById(R.id.repeat);
        menuButton = findViewById(R.id.menu_icon);
        sleepTimerButton = findViewById(R.id.sleep_timer);
        songdetails_ll = findViewById(R.id.songDetails_ll);

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        checkStoragePermission();

        sleepTimerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleSleepTimerClick();
            }
        });

        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isPlaying) {
                    pauseSong();
                } else {
                    if (currentFilePath != null) {
                        playSong(currentFilePath, currentPosition);
                    }
                }
            }
        });

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playNextSong();
            }
        });

        nextButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                skipForward();
                return true;
            }
        });

        prevButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playPrevSong();
            }
        });

        prevButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                rewindBackward();
                return true;
            }
        });

        shuffleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isShuffle = !isShuffle; // Toggle shuffle state
                if (isShuffle) {
                    shuffleButton.setImageResource(R.drawable.shuffle_on);// Update icon to indicate shuffle is on
                    showCustomToast("SHUFFLE MODE: ON", Gravity.NO_GRAVITY, R.drawable.shuffle_on, Toast.LENGTH_SHORT);
                    shuffleIndexList(); // Shuffle index list
                } else {
                    shuffleButton.setImageResource(R.drawable.shuffle); // Update icon to indicate shuffle is off
                    showCustomToast("SHUFFLE MODE: OFF", Gravity.NO_GRAVITY, R.drawable.shuffle, Toast.LENGTH_SHORT);
                }
            }
        });

        repeatButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isRepeat = !isRepeat; // Toggle repeat mode
                if (isRepeat) {
                    repeatButton.setImageResource(R.drawable.repeat_one); // Update icon to indicate repeat is on
                    showCustomToast("REPEAT CURRENT SONG: ON", Gravity.NO_GRAVITY, R.drawable.repeat_one, Toast.LENGTH_SHORT);
                } else {
                    repeatButton.setImageResource(R.drawable.repeat); // Update icon to indicate repeat is off
                    showCustomToast("REPEAT CURRENT SONG: OFF", Gravity.NO_GRAVITY, R.drawable.repeat, Toast.LENGTH_SHORT);
                }
            }
        });

        menuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isPopupOpen) {
                    popupWindow.dismiss();
                    isPopupOpen = false;
                } else {
                    openMenu(v);
                }
            }
        });

        songdetails_ll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Animation slideup = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_up);
                songAlbumll.startAnimation(slideup);
                songAlbumll.setVisibility(View.VISIBLE);
                listView.setAlpha(1.0f);
                listView.animate().alpha(0.2f).setDuration(500);
            }
        });

        songAlbumll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Animation slidedown = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_down);
                songAlbumll.startAnimation(slidedown);
                songAlbumll.setVisibility(View.GONE);
                listView.setAlpha(0.2f);
                listView.animate().alpha(1.0f).setDuration(500);
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Optional: Update the SeekBar progress
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Optional: Do something when the user starts dragging the SeekBar
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Seek to the selected position when the user stops dragging
                if (mediaPlayer != null) {
                    int newPosition = seekBar.getProgress();
                    mediaPlayer.seekTo(newPosition);
                    currentPosition = newPosition; // Update currentPosition
                }
            }
        });
    }

    private void loadMp3Files() {
        mp3List.clear(); // Clear the list before reloading

        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.DATA
        };

        Cursor cursor = getContentResolver().query(uri, projection, MediaStore.Audio.Media.IS_MUSIC + " != 0", null, null);
        if (cursor != null) {
            int nameIndex = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int artistIndex = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            int durationIndex = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION);
            int sizeIndex = cursor.getColumnIndex(MediaStore.Audio.Media.SIZE);
            int dataIndex = cursor.getColumnIndex(MediaStore.Audio.Media.DATA);

            while (cursor.moveToNext()) {
                String name = nameIndex != -1 ? cursor.getString(nameIndex) : "Unknown";
                String artist = artistIndex != -1 ? cursor.getString(artistIndex) : "Unknown";
                String duration = durationIndex != -1 ? cursor.getString(durationIndex) : "0";
                String size = sizeIndex != -1 ? cursor.getString(sizeIndex) : "0";
                String filePath = dataIndex != -1 ? cursor.getString(dataIndex) : "";

                // Convert duration to minutes and seconds
                long durationInMillis = Long.parseLong(duration);
                long minutes = (durationInMillis / 1000) / 60;
                long seconds = (durationInMillis / 1000) % 60;
                String durationFormatted = String.format("%d:%02d", minutes, seconds);

                // Get file size in MB
                long sizeInBytes = Long.parseLong(size);
                String sizeFormatted = String.format("%.2f MB", sizeInBytes / (1024.0 * 1024.0));

                // Retrieve album art
                Bitmap albumArtBitmap = null;
                MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                try {
                    mmr.setDataSource(filePath);
                    byte[] artBytes = mmr.getEmbeddedPicture();
                    if (artBytes != null) {
                        albumArtBitmap = BitmapFactory.decodeByteArray(artBytes, 0, artBytes.length);
                    } else {
                        albumArtBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.music_note); // placeholder image
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        mmr.release();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }

                HashMap<String, Object> mp3Info = new HashMap<>();
                mp3Info.put("name", name);
                mp3Info.put("artist", artist);
                mp3Info.put("duration", durationFormatted);
                mp3Info.put("size", sizeFormatted);
                mp3Info.put("albumArt", albumArtBitmap);
                mp3Info.put("filePath", filePath);

                mp3List.add(mp3Info);
            }
            cursor.close();
        }

        Mp3Adapter adapter = new Mp3Adapter(this, mp3List);
        listView.setAdapter(adapter);

        // Handle item clicks
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                currentSongIndex = position; // Update current song index
                HashMap<String, Object> selectedSong = (HashMap<String, Object>) parent.getItemAtPosition(position);
                String filePath = (String) selectedSong.get("filePath");
                String title = (String) selectedSong.get("name");
                String artist = (String) selectedSong.get("artist");
                Bitmap albumartx = (Bitmap) selectedSong.get("albumArt");

                // Stop current song if playing
                if (mediaPlayer != null && isPlaying) {
                    mediaPlayer.stop();
                    mediaPlayer.release();
                    mediaPlayer = null;
                    isPlaying = false;
                    playButton.setImageResource(R.drawable.play); // Change to play icon
                    seekBar.setProgress(0); // Reset SeekBar progress
                    startTime.setText("00:00"); // Reset current position
                    endTime.setText("00:00"); // Reset song length
                    handler.removeCallbacks(updateSeekBarRunnable); // Stop updating SeekBar
                }

                // Start playing the new song
                playSong(filePath, 0);
                listView.setVisibility(View.VISIBLE);
                songArtist_.setText(artist);
                songTitle_.setText(title);
                songAlbum_.setImageBitmap(albumartx);
            }
        });
    }

    private void shuffleIndexList() {
        shuffledIndexList.clear();
        for (int i = 0; i < mp3List.size(); i++) {
            shuffledIndexList.add(i);
        }
        java.util.Collections.shuffle(shuffledIndexList); // Shuffle the indices
    }

    private void playSong(String filePath, int startPosition) {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }

        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(filePath);
            mediaPlayer.prepare();
            mediaPlayer.seekTo(startPosition); // Start from the specified position
            mediaPlayer.start();
            isPlaying = true;
            seekBar.setProgress(startPosition);
            playButton.setImageResource(R.drawable.pause); // Change to pause icon

            // Set on completion listener
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    if (isRepeat) {
                        // Repeat the current song
                        playSong(currentFilePath, 0);
                    } else {
                        playNextSong(); // Automatically play the next song
                    }
                }
            });

            currentFilePath = filePath; // Update the current file path

            // Initialize SeekBar
            int duration = mediaPlayer.getDuration();
            seekBar.setMax(duration);

            // Format and display the song length in endTime
            long minutes = (duration / 1000) / 60;
            long seconds = (duration / 1000) % 60;
            String durationFormatted = String.format("%d:%02d", minutes, seconds);
            endTime.setText(durationFormatted); // Update endTime TextView

            handler.post(updateSeekBarRunnable); // Start updating SeekBar
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void playNextSong() {
        if (mp3List.isEmpty()) {
            return; // No songs to play
        }

        if (isShuffle) {
            // Get the next song index from shuffled index list
            currentSongIndex = (currentSongIndex + 1) % shuffledIndexList.size();
            currentSongIndex = shuffledIndexList.get(currentSongIndex);
        } else {
            currentSongIndex = (currentSongIndex + 1) % mp3List.size(); // Move to the next song
        }

        playSongAtCurrentIndex();
    }

    private void playPrevSong() {
        if (mp3List.isEmpty()) {
            return; // No songs to play
        }

        if (isShuffle) {
            // Get the previous song index from shuffled index list
            currentSongIndex = (currentSongIndex - 1 + shuffledIndexList.size()) % shuffledIndexList.size();
            currentSongIndex = shuffledIndexList.get(currentSongIndex);
        } else {
            currentSongIndex = (currentSongIndex - 1 + mp3List.size()) % mp3List.size(); // Move to the previous song
        }

        playSongAtCurrentIndex();
    }

    private void playSongAtCurrentIndex() {
        if (currentSongIndex < 0 || currentSongIndex >= mp3List.size()) {
            return; // Invalid index
        }

        HashMap<String, Object> song = mp3List.get(currentSongIndex);
        String filePath = (String) song.get("filePath");
        String title = (String) song.get("name");
        String artist = (String) song.get("artist");
        Bitmap albumArt = (Bitmap) song.get("albumArt");

        // Start playing the song
        playSong(filePath, 0);
        songArtist_.setText(artist);
        songTitle_.setText(title);
        songAlbum_.setImageBitmap(albumArt);

        listView.setVisibility(View.VISIBLE);
    }

    private void pauseSong() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();
            isPlaying = false;
            playButton.setImageResource(R.drawable.play); // Change to play icon
            currentPosition = mediaPlayer.getCurrentPosition(); // Update current position
            handler.removeCallbacks(updateSeekBarRunnable); // Stop updating SeekBar
        }
    }

    private void skipForward() {
        if (mediaPlayer != null) {
            int currentPosition = mediaPlayer.getCurrentPosition();
            int duration = mediaPlayer.getDuration();

            // Check if the song can be skipped by 5 seconds
            if (currentPosition + SKIP_TIME < duration) {
                mediaPlayer.seekTo(currentPosition + SKIP_TIME);
            } else {
                mediaPlayer.seekTo(duration); // Skip to the end if less than 5 seconds are left
            }
        }
    }

    private void rewindBackward() {
        if (mediaPlayer != null) {
            int currentPosition = mediaPlayer.getCurrentPosition();

            // Check if the song can be rewound by 5 seconds
            if (currentPosition - SKIP_TIME > 0) {
                mediaPlayer.seekTo(currentPosition - SKIP_TIME);
            } else {
                mediaPlayer.seekTo(0); // Rewind to the beginning if less than 5 seconds have passed
            }
        }
    }

    private void handleSleepTimerClick() {
        if (mediaPlayer == null || !mediaPlayer.isPlaying()) {
            showCustomToast("UNABLE TO START SLEEP-TIMER", Gravity.CENTER, R.drawable.round_warning_amber_24, Toast.LENGTH_LONG);
            return;
        }
        if (timerState == 0) {
            // Start 5-minute timer
            startTimer(FIVE_MINUTES_IN_MILLIS);
            timerState = 1;
            sleepTimerButton.setText("SLEEP-TIMER SET FOR: 15 MINUTES");
            showCustomToast("TIMER SET FOR: 15 MINUTES", Gravity.NO_GRAVITY, R.drawable.round_timer_24, Toast.LENGTH_LONG);
        } else if (timerState == 1) {
            // Change to 10-minute timer
            cancelTimer();
            startTimer(TEN_MINUTES_IN_MILLIS);
            timerState = 2;
            sleepTimerButton.setText("SLEEP-TIMER SET FOR: 30 MINUTES");
            showCustomToast("TIMER SET FOR: 30 MINUTES", Gravity.NO_GRAVITY, R.drawable.round_timer_24, Toast.LENGTH_LONG);
        } else if (timerState == 2) {
            // Stop and reset the timer
            cancelTimer();
            timerState = 0;
            sleepTimerButton.setText("SLEEP-TIMER: OFF");
            showCustomToast("TIMER: OFF", Gravity.NO_GRAVITY, R.drawable.round_timer_off_24, Toast.LENGTH_LONG);
        }
    }

    private void startTimer(long durationInMillis) {
        timeLeftInMillis = durationInMillis;
        countDownTimer = new CountDownTimer(timeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
            }

            @Override
            public void onFinish() {
                // Pause or stop the media player here
                pauseSong();
                timerState = 0; // Reset the timer state when finished
                showCustomToast("TIMER FINISHED, PLAYER PAUSED", Gravity.CENTER, R.drawable.timer_off, Toast.LENGTH_LONG);
                sleepTimerButton.setText("SLEEP-TIMER: OFF");
            }
        }.start();

        isTimerRunning = true;
    }

    private void cancelTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        isTimerRunning = false;
    }

    private void openMenu(View view) {
        // Inflate your custom layout
        View popupView = getLayoutInflater().inflate(R.layout.popup_menu, null);
        popupWindow = new PopupWindow(popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        // Set the background color
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        // Set the items click listeners
        TextView aboutTextView = popupView.findViewById(R.id.item_about);
        aboutTextView.setOnClickListener(v -> {
            Intent aboutIntent = new Intent(MainActivity.this, AboutActivity.class);
            startActivity(aboutIntent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            popupWindow.dismiss();
            isPopupOpen = false;
        });

        TextView settingsTextView = popupView.findViewById(R.id.item_settings);
        settingsTextView.setOnClickListener(v -> {
            showCustomToast("NOT AVAILABLE", Gravity.CENTER, R.drawable.do_not_touch, Toast.LENGTH_LONG);
            popupWindow.dismiss();
            isPopupOpen = false;
        });

        TextView scanMediaTextView = popupView.findViewById(R.id.item_scan);
        scanMediaTextView.setOnClickListener(view1 -> {
            loadMp3Files();
            showCustomToast("MEDIA SCANNING COMPLETED", Gravity.CENTER, R.drawable.round_storage_24, Toast.LENGTH_LONG);
            // Optionally dismiss the popup after scanning
            popupWindow.dismiss();
            isPopupOpen = false;
        });

        // Show the popup window
        popupWindow.showAsDropDown(view);
        isPopupOpen = true; // Set the state to open
    }

    private void showCustomToast(String message, int gravity, int imageResId, int duration) {
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.toast_background,
                (ViewGroup) findViewById(R.id.toast_layout_root));

        ImageView image = layout.findViewById(R.id.image);
        image.setImageResource(imageResId);

        TextView text = layout.findViewById(R.id.text);
        text.setGravity(Gravity.CENTER_VERTICAL);
        text.setText(message);

        Toast toast = new Toast(getApplicationContext());
        toast.setGravity(gravity, 0, 0);
        toast.setDuration(duration);
        toast.setView(layout);
        toast.show();
    }

    private void checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // For Android 11 and above (Scoped Storage)
            if (Environment.isExternalStorageManager()) {
                // Permission granted, call loadmp3()
                loadMp3Files();
            } else {
                // Request for the Manage External Storage permission
                requestManageExternalStoragePermission();
            }
        } else {
            // For Android versions below 11
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                // Request permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_STORAGE_PERMISSION);
            } else {
                // Permission granted, call loadmp3()
                loadMp3Files();
            }
        }
    }

    private void requestManageExternalStoragePermission() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomAlertDialog);
        builder.setTitle("Permission Required")
                .setMessage("This app requires access to manage storage to function properly. Please grant the permission.")
                .setPositiveButton("Allow", null) // Set null to override later
                .setNegativeButton("Deny", null); // Set null to override later

        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

                positiveButton.setTextColor(getResources().getColor(R.color.positive_button_color));
                negativeButton.setTextColor(getResources().getColor(R.color.negative_button_color));

                positiveButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                        intent.setData(Uri.parse("package:" + getPackageName()));
                        startActivityForResult(intent, REQUEST_MANAGE_STORAGE_PERMISSION);
                        dialog.dismiss();
                    }
                });

                negativeButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showPermissionDeniedDialog();
                        dialog.dismiss();
                    }
                });
            }
        });

        dialog.show();
    }

    private void showPermissionDeniedDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomAlertDialog);
        builder.setTitle("Permission Denied")
                .setMessage("You must allow storage permission to continue using the app.")
                .setPositiveButton("Retry", null) // Set null to override later
                .setNegativeButton("Exit", null); // Set null to override later

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

                if (positiveButton != null) {
                    positiveButton.setTextColor(Color.parseColor("#FF49454F")); // Set the color for positive button
                    positiveButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            checkStoragePermission();
                            dialog.dismiss();
                        }
                    });
                }

                if (negativeButton != null) {
                    negativeButton.setTextColor(Color.parseColor("#FFFF0000")); // Set the color for negative button
                    negativeButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            finish();
                        }
                    });
                }
            }
        });

        dialog.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, call loadmp3()
                loadMp3Files();
            } else {
                // Permission denied, show dialog
                showPermissionDeniedDialog();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_MANAGE_STORAGE_PERMISSION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    // Permission granted, call loadmp3()
                    loadMp3Files();
                } else {
                    // Permission denied, show dialog
                    showPermissionDeniedDialog();
                }
            }
        }
    }

    private Runnable updateSeekBarRunnable = new Runnable() {
        @Override
        public void run() {
            if (mediaPlayer != null && isPlaying) {
                int currentPos = mediaPlayer.getCurrentPosition();
                seekBar.setProgress(currentPos);

                // Format and display the current position in startTime
                long minutes = (currentPos / 1000) / 60;
                long seconds = (currentPos / 1000) % 60;
                String currentPositionFormatted = String.format("%d:%02d", minutes, seconds);
                startTime.setText(currentPositionFormatted); // Update startTime TextView

                handler.postDelayed(this, 1000); // Update every second
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        handler.removeCallbacks(updateSeekBarRunnable);
        mAudioManager.abandonAudioFocus(this);// Clean up handler callbacks
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        if (focusChange<=0){
            pauseSong();
        }
    }
}