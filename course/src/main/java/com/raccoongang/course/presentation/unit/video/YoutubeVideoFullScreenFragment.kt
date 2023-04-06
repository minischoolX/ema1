package com.raccoongang.course.presentation.unit.video

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.core.os.bundleOf
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.ui.DefaultPlayerUiController
import com.raccoongang.core.extension.requestApplyInsetsWhenAttached
import com.raccoongang.core.presentation.global.WindowSizeHolder
import com.raccoongang.core.presentation.global.viewBinding
import com.raccoongang.core.ui.WindowType
import com.raccoongang.course.R
import com.raccoongang.course.databinding.FragmentYoutubeVideoFullScreenBinding
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class YoutubeVideoFullScreenFragment : Fragment(R.layout.fragment_youtube_video_full_screen) {

    private val binding by viewBinding(FragmentYoutubeVideoFullScreenBinding::bind)

    private val viewModel by viewModel<VideoViewModel> {
        parametersOf(requireArguments().getString(ARG_COURSE_ID, ""))
    }

    private var blockId = ""
    private var isTabletDevice = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.videoUrl = requireArguments().getString(ARG_BLOCK_VIDEO_URL, "")
        blockId = requireArguments().getString(ARG_BLOCK_ID, "")
        if (viewModel.currentVideoTime == 0L) {
            viewModel.currentVideoTime = requireArguments().getLong(ARG_VIDEO_TIME, 0)
        }
        setOrientationBasedOnDeviceType()
    }

    private fun setOrientationBasedOnDeviceType() {
        val windowSize = requireActivity() as WindowSizeHolder
        isTabletDevice = windowSize.windowSize.width != WindowType.Compact &&
                windowSize.windowSize.height != WindowType.Compact
        if (!isTabletDevice) {
            requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.root.setOnApplyWindowInsetsListener { _, insets ->
            val insetsCompat = WindowInsetsCompat.toWindowInsetsCompat(insets)
                .getInsets(WindowInsetsCompat.Type.systemBars())

            val statusBarParams = binding.youtubePlayerView.layoutParams as FrameLayout.LayoutParams
            statusBarParams.topMargin = insetsCompat.top
            statusBarParams.bottomMargin = insetsCompat.bottom
            statusBarParams.marginStart = insetsCompat.left
            statusBarParams.marginEnd = insetsCompat.right
            binding.youtubePlayerView.layoutParams = statusBarParams
            insets
        }
        binding.root.requestApplyInsetsWhenAttached()

        lifecycle.addObserver(binding.youtubePlayerView)
        val options = IFramePlayerOptions.Builder()
            .controls(0)
            .rel(0)
            .build()


        binding.youtubePlayerView.initialize(object :
            AbstractYouTubePlayerListener() {
            override fun onStateChange(
                youTubePlayer: YouTubePlayer,
                state: PlayerConstants.PlayerState
            ) {
                super.onStateChange(youTubePlayer, state)
                if (state == PlayerConstants.PlayerState.ENDED) {
                    viewModel.markBlockCompleted(blockId)
                }
            }

            override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
                super.onCurrentSecond(youTubePlayer, second)
                viewModel.currentVideoTime = (second * 1000f).toLong()
            }

            override fun onReady(youTubePlayer: YouTubePlayer) {
                super.onReady(youTubePlayer)
                binding.youtubePlayerView.isVisible = true
                val defPlayerUiController =
                    DefaultPlayerUiController(binding.youtubePlayerView, youTubePlayer)
                defPlayerUiController.setFullScreenButtonClickListener {
                    parentFragmentManager.popBackStack()
                }

                binding.youtubePlayerView.setCustomPlayerUi(defPlayerUiController.rootView)

                val videoId = viewModel.videoUrl.split("watch?v=")[1]
                youTubePlayer.loadVideo(videoId, viewModel.currentVideoTime.toFloat())

            }

        }, options)
    }

    override fun onDestroyView() {
        viewModel.sendTime()
        super.onDestroyView()
    }

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onDestroy() {
        if (!isTabletDevice) {
            requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        super.onDestroy()
    }

    companion object {
        private const val ARG_BLOCK_VIDEO_URL = "blockVideoUrl"
        private const val ARG_VIDEO_TIME = "videoTime"
        private const val ARG_BLOCK_ID = "blockID"
        private const val ARG_COURSE_ID = "courseId"

        fun newInstance(
            videoUrl: String,
            videoTime: Long,
            blockId: String,
            courseId: String,
        ): YoutubeVideoFullScreenFragment {
            val fragment = YoutubeVideoFullScreenFragment()
            fragment.arguments = bundleOf(
                ARG_BLOCK_VIDEO_URL to videoUrl,
                ARG_VIDEO_TIME to videoTime,
                ARG_BLOCK_ID to blockId,
                ARG_COURSE_ID to courseId

            )
            return fragment
        }
    }

}