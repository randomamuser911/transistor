/*
 * MainActivityLayoutHolder.kt
 * Implements the MainActivityLayoutHolder class
 * A MainActivityLayoutHolder hold references to the views used in MainActivity
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015-26 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */

package org.y20k.transistor.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.ColorDrawable
import android.media.AudioManager
import android.os.Build
import android.support.v4.media.session.PlaybackStateCompat
import android.util.TypedValue
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.constraintlayout.widget.Group
import androidx.core.graphics.Insets
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.signature.ObjectKey
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.slider.Slider
import com.google.android.material.textview.MaterialTextView
import org.y20k.transistor.Keys
import org.y20k.transistor.R
import org.y20k.transistor.SettingsFragment
import org.y20k.transistor.core.Station
import org.y20k.transistor.helpers.DateTimeHelper
import org.y20k.transistor.helpers.PreferencesHelper
import org.y20k.transistor.helpers.UiHelper
import java.io.File


/*
 * MainActivityLayoutHolder class
 */
data class MainActivityLayoutHolder (var rootView: View) : MainFragmentLayoutHolder.StationListDragListener, SettingsFragment.SettingsListDragListener {

    /* Define log tag */
    private val TAG: String = MainActivityLayoutHolder::class.java.simpleName

    /* Main class variables */
    private lateinit var systemBars: Insets
    private val playerCardView: MaterialCardView
    private var playerPlaybackViews: Group
    var sleepTimerRunningViews: Group
    private var downloadProgressIndicator: ProgressBar
    private var stationImageView: ImageView
    private var stationNameView: TextView
    private var metadataView: TextView
    var playButtonView: ImageButton
    var bufferingIndicator: ProgressBar
    private var playerStreamingLinkView: TextView
    private var playerMetadataHistoryView: TextView
    var playerNextMetadataView: ImageButton
    var playerPreviousMetadataView: ImageButton
    var playerCopyMetadataButtonView: ImageButton
    var playerSleepTimerStartButtonView: ImageButton
    var playerSleepTimerCancelButtonView: ImageButton
    var sheetSleepTimerRemainingTimeView: TextView
    private var metadataHistory: MutableList<String>
    private var metadataHistoryPosition: Int
    private var isBuffering: Boolean
    var userInterfaceTransparencyEffectActive: Boolean

    /* Full screen now playing sheet */
    private val playerSheetView: View
    private val playerSheetContentView: View
    private val playerSheetStationImage: ShapeableImageView
    private val playerSheetStationName: MaterialTextView
    private val playerSheetStationMetadata: MaterialTextView
    var playerSheetPlayButton: ImageButton
    private var playerSheetBufferingIndicator: ProgressBar
    private var playerSheetCollapseButton: ImageButton
    private var playerSheetMoreButton: ImageButton
    private var playerSheetVolumeSlider: Slider
    private val audioManager: AudioManager?


    /* Init block */
    init {
        // find views - mini player
        playerCardView = rootView.findViewById(R.id.player_card)
        playerPlaybackViews = rootView.findViewById(R.id.playback_views)
        sleepTimerRunningViews = rootView.findViewById(R.id.sleep_timer_running_views)
        downloadProgressIndicator = rootView.findViewById(R.id.download_progress_indicator)
        stationImageView = rootView.findViewById(R.id.station_icon)
        stationNameView = rootView.findViewById(R.id.player_station_name)
        metadataView = rootView.findViewById(R.id.player_station_metadata)
        playButtonView = rootView.findViewById(R.id.player_play_button)
        bufferingIndicator = rootView.findViewById(R.id.player_buffering_indicator)
        playerStreamingLinkView = rootView.findViewById(R.id.player_extended_streaming_link)
        playerMetadataHistoryView = rootView.findViewById(R.id.player_extended_metadata_history)
        playerNextMetadataView = rootView.findViewById(R.id.player_extended_next_metadata_button)
        playerPreviousMetadataView = rootView.findViewById(R.id.player_extended_previous_metadata_button)
        playerCopyMetadataButtonView = rootView.findViewById(R.id.player_extended_copy_station_metadata_button)
        playerSleepTimerStartButtonView = rootView.findViewById(R.id.sleep_timer_start_button)
        playerSleepTimerCancelButtonView = rootView.findViewById(R.id.sleep_timer_cancel_button)
        sheetSleepTimerRemainingTimeView = rootView.findViewById(R.id.sleep_timer_remaining_time)

        // find views - now playing sheet
        playerSheetView = rootView.findViewById(R.id.player_sheet_root)
        playerSheetContentView = rootView.findViewById(R.id.player_sheet_content)
        playerSheetStationImage = rootView.findViewById(R.id.player_sheet_station_image)
        playerSheetStationName = rootView.findViewById(R.id.player_sheet_station_name)
        playerSheetStationMetadata = rootView.findViewById(R.id.player_sheet_station_metadata)
        playerSheetPlayButton = rootView.findViewById(R.id.player_sheet_play_button)
        playerSheetBufferingIndicator = rootView.findViewById(R.id.player_sheet_buffering_indicator)
        playerSheetCollapseButton = rootView.findViewById(R.id.player_sheet_collapse_button)
        playerSheetMoreButton = rootView.findViewById(R.id.player_sheet_more_button)
        playerSheetVolumeSlider = rootView.findViewById(R.id.player_sheet_volume_slider)

        // set up variables
        metadataHistory = PreferencesHelper.loadMetadataHistory()
        metadataHistoryPosition = metadataHistory.size - 1
        isBuffering = false
        userInterfaceTransparencyEffectActive = PreferencesHelper.loadUserInterfaceTransparencyEffect()
        audioManager = rootView.context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

        // set up metadata history next and previous buttons
        playerPreviousMetadataView.setOnClickListener {
            if (metadataHistory.isNotEmpty()) {
                if (metadataHistoryPosition > 0) {
                    metadataHistoryPosition -= 1
                } else {
                    metadataHistoryPosition = metadataHistory.size - 1
                }
                playerMetadataHistoryView.text = metadataHistory[metadataHistoryPosition]
            }
        }
        playerNextMetadataView.setOnClickListener {
            if (metadataHistory.isNotEmpty()) {
                if (metadataHistoryPosition < metadataHistory.size - 1) {
                    metadataHistoryPosition += 1
                } else {
                    metadataHistoryPosition = 0
                }
                playerMetadataHistoryView.text = metadataHistory[metadataHistoryPosition]
            }
        }
        playerMetadataHistoryView.setOnLongClickListener {
            copyMetadataHistoryToClipboard()
            return@setOnLongClickListener true
        }

        // set up edge to edge display
        setupEdgeToEdge()

        // set layout for player
        setupPlayer()
    }


    /* Overrides onStationListDragStateChanged from StationListDragListener */
    override fun onStationListDragStateChanged(newState: Int) {
        setPlayerTransparencyDuringDrag(newState)
    }


    /* Overrides onSettingsListDragStateChanged from SettingsListDragListener */
    override fun onSettingsListDragStateChanged(newState: Int) {
        setPlayerTransparencyDuringDrag(newState)
    }


    /* Updates the player views */
    fun updatePlayerViews(context: Context, station: Station, isPlaying: Boolean) {

        // set default metadata views, when playback has stopped
        if (!isPlaying) {
            metadataView.text = station.name
            playerMetadataHistoryView.text = station.name
            playerSheetStationMetadata.text = station.name
            playerSheetStationMetadata.isSelected = true
        }

        // update name
        stationNameView.text = station.name
        playerSheetStationName.text = station.name

        // update cover
        val stationImageFile = File(station.image.toUri().path ?: "")
        Glide.with(context)
            .load(station.image)
            .signature(ObjectKey(stationImageFile.lastModified()))
            .error(R.drawable.ic_default_station_image_64dp)
            .into(stationImageView)
        Glide.with(context)
            .load(station.image)
            .signature(ObjectKey(stationImageFile.lastModified()))
            .error(R.drawable.ic_default_station_image_64dp)
            .into(playerSheetStationImage)
        if (station.imageColor != -1) {
            stationImageView.setBackgroundColor(station.imageColor)
            playerSheetStationImage.setBackgroundColor(station.imageColor)
        }
        stationImageView.contentDescription = "${context.getString(R.string.descr_player_station_image)}: ${station.name}"
        playerSheetStationImage.contentDescription = "${context.getString(R.string.descr_player_station_image)}: ${station.name}"

        // tint the now playing sheet background with the station color
        setPlayerSheetBackgroundColor(station.imageColor)

        // update streaming link
        playerStreamingLinkView.text = station.getStreamUri()

        // update click listeners
        playerMetadataHistoryView.setOnClickListener { copyToClipboard(context, playerMetadataHistoryView.text) }
        playerCopyMetadataButtonView.setOnClickListener { copyToClipboard(context, playerMetadataHistoryView.text) }
        playerSheetStationMetadata.setOnClickListener { copyToClipboard(context, playerSheetStationMetadata.text) }

    }


    /* Tints the now playing sheet background with a darkened version of the station color */
    private fun setPlayerSheetBackgroundColor(imageColor: Int) {
        val typedValue: TypedValue = TypedValue()
        playerSheetView.context.theme.resolveAttribute(com.google.android.material.R.attr.colorSurfaceContainerLow, typedValue, true)
        val defaultColor: Int = typedValue.data
        val backgroundColor: Int = if (imageColor != -1) {
            // blend the station color towards black for a dark ambient background
            androidx.core.graphics.ColorUtils.blendARGB(imageColor, Color.BLACK, 0.72f)
        } else {
            defaultColor
        }
        playerSheetView.background = ColorDrawable(backgroundColor)
    }


    /* Copies given string to clipboard */
    private fun copyToClipboard(context: Context, clipString: CharSequence) {
        val clip: ClipData = ClipData.newPlainText("simple text", clipString)
        val cm: ClipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(clip)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU){
            // since API 33 (TIRAMISU) the OS displays its own notification when content is copied to the clipboard
            Toast.makeText(context, R.string.toast_message_copied_to_clipboard, Toast.LENGTH_LONG).show()
        }
    }


    /* Copies collected metadata to clipboard */
    private fun copyMetadataHistoryToClipboard() {
        val metadataHistory: MutableList<String> = PreferencesHelper.loadMetadataHistory()
        val stringBuilder: StringBuilder = StringBuilder()
        metadataHistory.forEach { stringBuilder.append("${it.trim()}\n")}
        copyToClipboard(rootView.context, stringBuilder.toString())
    }


    /* Updates the metadata views */
    fun updateMetadata(metadataHistoryList: MutableList<String>?) {
        if (!metadataHistoryList.isNullOrEmpty()) {
            metadataHistory = metadataHistoryList
            if (metadataHistory.last() != metadataView.text) {
                metadataHistoryPosition = metadataHistory.size - 1
                val metadataString = metadataHistory[metadataHistoryPosition]
                metadataView.text = metadataString
                playerSheetStationMetadata.text = metadataString
                playerMetadataHistoryView.text = metadataString
                playerMetadataHistoryView.isSelected = true
                playerSheetStationMetadata.isSelected = true
            }
        }
    }


    /* Updates sleep timer views */
    fun updateSleepTimer(context: Context, timeRemaining: Long = 0L) {
        when (timeRemaining) {
            0L -> {
                sleepTimerRunningViews.isGone = true
            }
            else -> {
                if (playerSheetView.isVisible) sleepTimerRunningViews.isVisible = true
                val sleepTimerTimeRemaining = DateTimeHelper.convertToMinutesAndSeconds(timeRemaining)
                sheetSleepTimerRemainingTimeView.text = sleepTimerTimeRemaining
                sheetSleepTimerRemainingTimeView.contentDescription = "${context.getString(R.string.descr_expanded_player_sleep_timer_remaining_time)}: $sleepTimerTimeRemaining"            }
        }
    }


    /* Toggles play/pause button */
    fun togglePlayButton(isPlaying: Boolean) {
        if (isPlaying) {
            playButtonView.setImageResource(R.drawable.ic_player_stop_symbol_48dp)
            playerSheetPlayButton.setImageResource(R.drawable.ic_player_stop_symbol_48dp)
            bufferingIndicator.isVisible = false
            playerSheetBufferingIndicator.isVisible = false
        } else {
            playButtonView.setImageResource(R.drawable.ic_player_play_symbol_48dp)
            playerSheetPlayButton.setImageResource(R.drawable.ic_player_play_symbol_48dp)
            bufferingIndicator.isVisible = isBuffering
            playerSheetBufferingIndicator.isVisible = isBuffering
        }
    }


    /* Toggles buffering indicator */
    fun showBufferingIndicator(buffering: Boolean) {
        bufferingIndicator.isVisible = buffering
        playerSheetBufferingIndicator.isVisible = buffering
        isBuffering = buffering
    }


    /* Toggles visibility of player depending on playback state - hiding it when playback is stopped (not paused or playing) */
    fun togglePlayerVisibility(playbackState: Int): Boolean {
        when (playbackState) {
            PlaybackStateCompat.STATE_STOPPED -> return hidePlayer()
            PlaybackStateCompat.STATE_NONE -> return hidePlayer()
            PlaybackStateCompat.STATE_ERROR -> return hidePlayer()
            else -> return showPlayer()
        }
    }


    /* Toggles visibility of the download progress indicator */
    fun toggleDownloadProgressIndicator() {
        when (PreferencesHelper.loadActiveDownloads()) {
            Keys.ACTIVE_DOWNLOADS_EMPTY -> downloadProgressIndicator.isGone = true
            else -> downloadProgressIndicator.isVisible = true
        }
    }


    /* Initiates the rotation animation of the play button  */
    fun animatePlaybackButtonStateTransition(context: Context, isPlaying: Boolean) {
        when (isPlaying) {
            true -> {
                // rotate and morph to stop icon
                playButtonView.setImageResource(R.drawable.anim_play_to_stop_48dp)
                val morphDrawable: AnimatedVectorDrawable = playButtonView.drawable as AnimatedVectorDrawable
                morphDrawable.start()
                playerSheetPlayButton.setImageResource(R.drawable.anim_play_to_stop_48dp)
                val sheetMorphDrawable: AnimatedVectorDrawable = playerSheetPlayButton.drawable as AnimatedVectorDrawable
                sheetMorphDrawable.start()
            }
            false -> {
                // rotate and morph to play icon
                playButtonView.setImageResource(R.drawable.anim_stop_to_play_48dp)
                val morphDrawable: AnimatedVectorDrawable = playButtonView.drawable as AnimatedVectorDrawable
                morphDrawable.start()
                playerSheetPlayButton.setImageResource(R.drawable.anim_stop_to_play_48dp)
                val sheetMorphDrawable: AnimatedVectorDrawable = playerSheetPlayButton.drawable as AnimatedVectorDrawable
                sheetMorphDrawable.start()
            }
        }
    }


    /* Shows player */
    fun showPlayer(): Boolean {
        playerPlaybackViews.visibility = View.VISIBLE
        return true
    }


    /* Hides player */
    fun hidePlayer(): Boolean {
        playerPlaybackViews.visibility = View.GONE
        hidePlayerSheet()
        return true
    }


    /* Shows the full screen now playing sheet */
    fun showPlayerSheet() {
        if (playerSheetView.isVisible) return
        syncVolumeSlider()
        // a view that was gone has never been laid out - fall back to the root height
        val sheetHeight: Float = if (playerSheetView.height > 0) playerSheetView.height.toFloat() else rootView.height.toFloat()
        playerSheetView.isVisible = true
        sleepTimerRunningViews.isVisible = sheetSleepTimerRemainingTimeView.text.isNotEmpty()
        playerSheetView.translationY = sheetHeight
        playerSheetView.animate()
            .translationY(0f)
            .setDuration(PLAYER_SHEET_ANIMATION_DURATION)
            .withEndAction { playerSheetView.translationY = 0f }
            .start()
    }


    /* Hides the full screen now playing sheet */
    fun hidePlayerSheet() {
        if (playerSheetView.isGone) return
        playerSheetView.animate()
            .translationY(playerSheetView.height.toFloat())
            .setDuration(PLAYER_SHEET_ANIMATION_DURATION)
            .withEndAction {
                playerSheetView.isGone = true
                sleepTimerRunningViews.isGone = true
            }
            .start()
    }


    /* Hides the now playing sheet if it is visible */
    fun hidePlayerSheetIfVisible(): Boolean {
        return if (playerSheetView.isVisible) {
            hidePlayerSheet()
            true // = now playing sheet was visible and had to be hidden (= no need to interpret back press as a navigation)
        } else {
            false
        }
    }


    /* Syncs the volume slider with the current music stream volume */
    private fun syncVolumeSlider() {
        val audioManager: AudioManager = audioManager ?: return
        val maxVolume: Int = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val currentVolume: Int = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        playerSheetVolumeSlider.valueTo = maxVolume.toFloat()
        playerSheetVolumeSlider.value = currentVolume.toFloat().coerceIn(0f, maxVolume.toFloat())
    }


    /* Sets up the volume slider */
    private fun setupVolumeSlider() {
        val audioManager: AudioManager = audioManager ?: return
        playerSheetVolumeSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, value.toInt(), 0)
            }
        }
    }


    /* Sets up the 3-dot overflow menu of the now playing sheet */
    private fun setupPlayerSheetMenu() {
        playerSheetMoreButton.setOnClickListener { anchor: View ->
            val popupMenu: PopupMenu = PopupMenu(anchor.context, anchor)
            popupMenu.inflate(R.menu.menu_player_sheet)
            popupMenu.setOnMenuItemClickListener { menuItem ->
                val streamingLink: String = playerStreamingLinkView.text.toString()
                when (menuItem.itemId) {
                    R.id.menu_show_streaming_link -> {
                        showStreamingLinkDialog(anchor.context, streamingLink)
                        true
                    }
                    R.id.menu_copy_streaming_link -> {
                        copyToClipboard(anchor.context, streamingLink)
                        true
                    }
                    R.id.menu_share_streaming_link -> {
                        shareStreamingLink(anchor.context, streamingLink)
                        true
                    }
                    else -> false
                }
            }
            popupMenu.show()
        }
    }


    /* Displays the streaming link in a dialog */
    private fun showStreamingLinkDialog(context: Context, streamingLink: String) {
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.player_sheet_h2_stream_url)
            .setMessage(streamingLink)
            .setPositiveButton(R.string.dialog_generic_button_okay) { dialog, _ ->
                dialog.dismiss()
            }
            .setNeutralButton(R.string.menu_copy_streaming_link) { _, _ ->
                copyToClipboard(context, streamingLink)
            }
            .show()
    }


    /* Shares the streaming link via an intent */
    private fun shareStreamingLink(context: Context, streamingLink: String) {
        val sendIntent: Intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, streamingLink)
        }
        context.startActivity(Intent.createChooser(sendIntent, context.getString(R.string.menu_share_streaming_link)))
    }


    /* Sets up the player */
    private fun setupPlayer() {
        // tapping the mini player opens the now playing sheet
        playerCardView.setOnClickListener { showPlayerSheet() }
        stationImageView.setOnClickListener { showPlayerSheet() }
        stationNameView.setOnClickListener { showPlayerSheet() }
        metadataView.setOnClickListener { showPlayerSheet() }

        // collapse button closes the now playing sheet
        playerSheetCollapseButton.setOnClickListener { hidePlayerSheet() }

        // set up volume slider and overflow menu
        setupVolumeSlider()
        setupPlayerSheetMenu()
    }


    /* Make player semi-transparent during list drag */
    private fun setPlayerTransparencyDuringDrag(newState: Int) {
        if (userInterfaceTransparencyEffectActive) {
            when (newState) {
                RecyclerView.SCROLL_STATE_DRAGGING -> {
                    playerCardView.alpha = 0.5f
                }
                RecyclerView.SCROLL_STATE_IDLE -> {
                    // animate the alpha transition from 0.5f to 1.0f
                    playerCardView.animate()
                        .alpha(1.0f)
                        .setDuration(Keys.DEFAULT_TRANSITION_ANIMATION_DURATION)
                        .start()
                }
                RecyclerView.SCROLL_STATE_SETTLING -> {
                    // animation handled in IDLE state
                }
            }
        }
    }


    /* Sets up margins/paddings for edge to edge view */
    private fun setupEdgeToEdge() {
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
            // get measurements for status and navigation bar
            systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            // apply measurements
            downloadProgressIndicator.updateLayoutParams<FrameLayout.LayoutParams> {
                topMargin = systemBars.top
            }
            playerCardView.updateLayoutParams<FrameLayout.LayoutParams> {
                leftMargin = (Keys.PLAYER_SIDE_MARGIN * UiHelper.getDensityScalingFactor(rootView.context)).toInt() + systemBars.left
                rightMargin = (Keys.PLAYER_SIDE_MARGIN * UiHelper.getDensityScalingFactor(rootView.context)).toInt() + systemBars.right
                bottomMargin = (Keys.PLAYER_BOTTOM_MARGIN * UiHelper.getDensityScalingFactor(rootView.context)).toInt() + systemBars.bottom
            }
            playerSheetContentView.updatePadding(top = systemBars.top, bottom = systemBars.bottom + (32 * UiHelper.getDensityScalingFactor(rootView.context)).toInt())
            // return the insets
            insets
        }

    }

    /* Companion object */
    companion object {
        private const val PLAYER_SHEET_ANIMATION_DURATION: Long = 250L
    }

}
