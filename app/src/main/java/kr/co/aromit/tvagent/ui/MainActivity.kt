package kr.co.aromit.tvagent.ui

import android.os.Bundle
import android.view.ViewGroup.LayoutParams
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import timber.log.Timber

/**
 * MainActivity
 *
 * 앱 실행 상태를 표시하고 TR-069 조회 버튼을 제공합니다.
 */
class MainActivity : AppCompatActivity() {

    companion object {
        /** 태그 for Timber logs */
        private const val TAG = "MainActivity"
        /** 레이아웃 패딩(dp) */
        private const val PADDING_DP = 16
        /** 상태 표시 텍스트 */
        private const val STATUS_TEXT = "TV Agent 앱이 실행되었습니다"
        /** 버튼 텍스트 */
        private const val BUTTON_TEXT = "TR-069 조회"
        /** 텍스트 크기(sp) */
        private const val TEXT_SIZE_SP = 20f
    }

    /**
     * Activity 생성 시 UI 구성 및 로그 기록을 수행합니다.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.tag(TAG).i("onCreate 호출됨")

        // 루트 레이아웃 생성
        val rootLayout = createRootLayout().also {
            Timber.tag(TAG).d("Root layout created with padding ${PADDING_DP}dp")
        }

        // 상태 표시 TextView 추가
        val statusView = createStatusView().also {
            Timber.tag(TAG).d("Status TextView created: text='${STATUS_TEXT}'")
        }
        rootLayout.addView(statusView)

        // 동작 버튼 추가
        val actionButton = createActionButton().also {
            Timber.tag(TAG).d("Action Button created: text='${BUTTON_TEXT}'")
        }
        rootLayout.addView(actionButton)

        // 버튼 클릭 리스너 설정
        actionButton.setOnClickListener {
            Timber.tag(TAG).i("Button clicked: '$BUTTON_TEXT'")
            // TODO: ReportInformUseCase.execute() 호출 등 기능 연결
        }

        // 레이아웃 설정
        setContentView(rootLayout)
        Timber.tag(TAG).i("UI 설정 완료")
    }

    /**
     * 루트 LinearLayout을 생성하고 기본 설정을 적용합니다.
     */
    private fun createRootLayout(): LinearLayout {
        val paddingPx = (PADDING_DP * resources.displayMetrics.density).toInt()
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
            )
        }
    }

    /**
     * 상태 표시용 TextView를 생성합니다.
     */
    private fun createStatusView(): TextView =
        TextView(this).apply {
            text = STATUS_TEXT
            textSize = TEXT_SIZE_SP
            layoutParams = LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
            )
        }

    /**
     * 동작 버튼을 생성합니다.
     */
    private fun createActionButton(): Button =
        Button(this).apply {
            text = BUTTON_TEXT
            layoutParams = LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
            )
        }
}
