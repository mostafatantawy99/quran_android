package com.quran.labs.androidquran.widgets;

import com.quran.labs.androidquran.R;
import com.quran.labs.androidquran.common.QuranAyah;
import com.quran.labs.androidquran.data.QuranInfo;
import com.quran.labs.androidquran.ui.helpers.UthmaniSpan;
import com.quran.labs.androidquran.util.QuranScreenInfo;
import com.quran.labs.androidquran.util.QuranSettings;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.annotation.StyleRes;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;

public class TranslationView extends ScrollView {
  private static final String AR_BASMALLAH = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ";

  private Context mContext;
  private int mDividerColor;
  private int mLeftRightMargin;
  private int mTopBottomMargin;
  @StyleRes private int mTextStyle;
  @StyleRes private int mHighlightedStyle;
  private int mFontSize;
  private int mHeaderColor;
  private int mHeaderStyle;
  private int mFooterSpacerHeight;
  private int mLastHighlightedAyah;
  private boolean mIsNightMode;
  private int mNightModeTextColor;
  private boolean mIsInAyahActionMode;

  private List<QuranAyah> mAyat;
  private SparseArray<TextView> mAyahMap;
  private SparseArray<TextView> mAyahHeaderMap;

  private LinearLayout mLinearLayout;
  private TranslationClickedListener mTranslationClickedListener;

  public TranslationView(Context context) {
    this(context, null);
  }

  public TranslationView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public TranslationView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    init(context);
  }

  public void setIsInAyahActionMode(boolean isInAyahActionMode) {
    mIsInAyahActionMode = isInAyahActionMode;
  }

  public void init(Context context) {
    mContext = context;
    mAyahMap = new SparseArray<>();
    mAyahHeaderMap = new SparseArray<>();

    setFillViewport(true);
    mLinearLayout = new LinearLayout(context);
    mLinearLayout.setOrientation(LinearLayout.VERTICAL);
    addView(mLinearLayout, ScrollView.LayoutParams.MATCH_PARENT,
        ScrollView.LayoutParams.WRAP_CONTENT);
    mLinearLayout.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        if (mTranslationClickedListener != null) {
          mTranslationClickedListener.onTranslationClicked();
        }
      }
    });

    Resources resources = getResources();
    mDividerColor = resources.getColor(R.color.translation_hdr_color);
    mLeftRightMargin = resources.getDimensionPixelSize(
        R.dimen.translation_left_right_margin);
    mTopBottomMargin = resources.getDimensionPixelSize(
        R.dimen.translation_top_bottom_margin);
    mFooterSpacerHeight = resources.getDimensionPixelSize(
        R.dimen.translation_footer_spacer);
    mHeaderColor = resources.getColor(R.color.translation_sura_header);
    mHeaderStyle = R.style.translation_sura_title;
    initResources();
  }

  private void initResources() {
    QuranSettings settings = QuranSettings.getInstance(mContext);
    mFontSize = settings.getTranslationTextSize();

    mIsNightMode = settings.isNightMode();
    if (mIsNightMode) {
      int brightness = settings.getNightModeTextBrightness();
      mNightModeTextColor = Color.rgb(brightness, brightness, brightness);
    }
    mTextStyle = mIsNightMode ? R.style.TranslationText_NightMode :
        R.style.TranslationText;
    mHighlightedStyle = mIsNightMode ?
        R.style.TranslationText_NightMode_Highlighted :
        R.style.TranslationText_Highlighted;
  }

  public void refresh() {
    initResources();
    if (mAyat != null) {
      setAyahs(mAyat);
    }
  }

  public void setNightMode(boolean isNightMode, int textBrightness) {
    mIsNightMode = isNightMode;
    if (isNightMode) {
      mNightModeTextColor = Color.rgb(textBrightness, textBrightness, textBrightness);
    }
    mTextStyle = mIsNightMode ? R.style.TranslationText_NightMode :
        R.style.TranslationText;
    mHighlightedStyle = mIsNightMode ?
        R.style.TranslationText_NightMode_Highlighted :
        R.style.TranslationText_Highlighted;
    if (mAyat != null) {
      setAyahs(mAyat);
    }
  }

  public void setAyahs(List<QuranAyah> ayat) {
    mLastHighlightedAyah = -1;

    mLinearLayout.removeAllViews();
    mAyahMap.clear();
    mAyahHeaderMap.clear();
    mAyat = ayat;

    int currentSura = 0;
    for (int i = 0, ayatSize = ayat.size(); i < ayatSize; i++) {
      QuranAyah ayah = ayat.get(i);

      final int sura = ayah.getSura();
      if (!mIsInAyahActionMode && sura != currentSura) {
        addSuraHeader(sura);
        if (ayah.getAyah() == 1 && (sura != 1 && sura != 9)) {
          // explicitly add basmallah
          addBasmallah();
        }
        currentSura = sura;
      }
      addTextForAyah(ayah);
    }

    addFooterSpacer();
  }

  public void unhighlightAyat() {
    if (mLastHighlightedAyah > 0) {
      TextView text = mAyahMap.get(mLastHighlightedAyah);
      if (text != null) {
        text.setTextAppearance(mContext, mTextStyle);
        text.setTextSize(mFontSize);
      }

      text = mAyahHeaderMap.get(mLastHighlightedAyah);
      if (text != null) {
        styleAyahHeader(text, mTextStyle);
      }
    }
    mLastHighlightedAyah = -1;
  }

  public void highlightAyah(int ayahId) {
    if (mLastHighlightedAyah > 0) {
      unhighlightAyat();
    }

    TextView text = mAyahMap.get(ayahId);
    if (text != null) {
      text.setTextAppearance(mContext, mHighlightedStyle);
      text.setTextSize(mFontSize);
      mLastHighlightedAyah = ayahId;

      TextView header = mAyahHeaderMap.get(ayahId);
      if (header != null) {
        styleAyahHeader(header, mHighlightedStyle);
      }

      int screenHeight = QuranScreenInfo.getInstance().getHeight();
      int y = text.getTop() - (int) (0.25 * screenHeight);
      smoothScrollTo(getScrollX(), y);
    } else {
      mLastHighlightedAyah = -1;
    }
  }

  private OnClickListener mOnAyahClickListener = new OnClickListener() {
    @Override
    public void onClick(View v) {
      if (mTranslationClickedListener != null) {
        mTranslationClickedListener.onTranslationClicked();
      }
    }
  };

  private void addFooterSpacer() {
    final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
        LayoutParams.MATCH_PARENT, mFooterSpacerHeight);
    final View view = new View(mContext);
    mLinearLayout.addView(view, params);
  }

  private void addTextForAyah(QuranAyah ayah) {
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
        LayoutParams.MATCH_PARENT,
        LayoutParams.WRAP_CONTENT);
    params.setMargins(mLeftRightMargin, mTopBottomMargin,
        mLeftRightMargin, mTopBottomMargin);

    final int suraNumber = ayah.getSura();
    final int ayahNumber = ayah.getAyah();
    final int ayahId = QuranInfo.getAyahId(suraNumber, ayahNumber);
    TextView ayahHeader = new TextView(mContext);
    styleAyahHeader(ayahHeader, mTextStyle);
    ayahHeader.setText(suraNumber + ":" + ayahNumber);
    mLinearLayout.addView(ayahHeader, params);
    mAyahHeaderMap.put(ayahId, ayahHeader);

    TextView ayahView = new TextView(mContext);
    ayahView.setOnClickListener(mOnAyahClickListener);
    mAyahMap.put(ayahId, ayahView);

    ayahView.setTextAppearance(mContext, mTextStyle);
    if (mIsInAyahActionMode) {
      ayahView.setTextColor(Color.WHITE);
    } else if (mIsNightMode) {
      ayahView.setTextColor(mNightModeTextColor);
    }
    ayahView.setTextSize(mFontSize);

    // arabic
    String ayahText = ayah.getText();
    if (!TextUtils.isEmpty(ayahText)) {

      // since the basmallah is hardcoded in the db, we remove it from the
      // first verse (except for sura fatiha and sura tawbah).
      if (ayahNumber == 1 && (suraNumber != 1 && suraNumber != 9)) {

        // this code is here as a safety check (even though, in theory, it should always be true).
        // this is in case one day, we update the database and remove the basmallah from being
        // attached to each first ayah - in those cases, even old code with the new database
        // should do the right thing.
        if (ayahText.startsWith(AR_BASMALLAH)) {
          ayahText = ayahText.substring(AR_BASMALLAH.length() + 1);
        }
      }

      // Ayah Text
      ayahView.setLineSpacing(1.4f, 1.4f);

      SpannableString arabicText = new SpannableString(ayahText);
      UthmaniSpan uthmaniSpan = new UthmaniSpan(mContext);
      arabicText.setSpan(uthmaniSpan, 0, ayahText.length(),
          Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
      ayahView.setText(arabicText);
      ayahView.append("\n\n");
    }

    // translation
    String translationText = ayah.getTranslation();

    SpannableString translation = new SpannableString(translationText);
    ayahView.append(translation);

    params = new LinearLayout.LayoutParams(
        LayoutParams.MATCH_PARENT,
        LayoutParams.WRAP_CONTENT);
    params.setMargins(mLeftRightMargin, mTopBottomMargin,
        mLeftRightMargin, mTopBottomMargin);
    setTextSelectable(ayahView);
    mLinearLayout.addView(ayahView, params);
  }

  private void styleAyahHeader(TextView headerView, @StyleRes int style) {
    headerView.setTextAppearance(mContext, style);
    if (mIsInAyahActionMode) {
      headerView.setTextColor(Color.WHITE);
    } else if (mIsNightMode) {
      headerView.setTextColor(mNightModeTextColor);
    }
    headerView.setTextSize(mFontSize);
    headerView.setTypeface(null, Typeface.BOLD);
  }

  private void setTextSelectable(TextView ayahView) {
    ayahView.setTextIsSelectable(true);
  }

  private void addSuraHeader(int currentSura) {
    View view = new View(mContext);

    view.setBackgroundColor(mHeaderColor);
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
        LayoutParams.MATCH_PARENT, 2);
    params.topMargin = mTopBottomMargin;
    mLinearLayout.addView(view, params);

    String suraName = QuranInfo.getSuraName(mContext, currentSura, true);

    TextView headerView = new TextView(mContext);
    params = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,
        LayoutParams.WRAP_CONTENT);
    params.leftMargin = mLeftRightMargin;
    params.rightMargin = mLeftRightMargin;
    params.topMargin = mTopBottomMargin / 2;
    params.bottomMargin = mTopBottomMargin / 2;
    headerView.setTextAppearance(mContext, mHeaderStyle);
    headerView.setText(suraName);
    mLinearLayout.addView(headerView, params);

    view = new View(mContext);
    view.setBackgroundColor(mDividerColor);
    mLinearLayout.addView(view, LayoutParams.MATCH_PARENT, 2);
  }

  private void addBasmallah() {
    TextView tv = new TextView(mContext);
    tv.setTextAppearance(mContext, mTextStyle);
    if (mIsNightMode) {
      tv.setTextColor(mNightModeTextColor);
    }
    tv.setTextSize(mFontSize);

    SpannableString str = new SpannableString(AR_BASMALLAH);
    UthmaniSpan uthmaniSpan = new UthmaniSpan(mContext);
    str.setSpan(uthmaniSpan, 0, str.length(),
        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    tv.setText(str);

    LinearLayout.LayoutParams params =
        new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
    params.leftMargin = mLeftRightMargin;
    params.rightMargin = mLeftRightMargin;
    params.topMargin = mTopBottomMargin / 2;
    params.bottomMargin = mTopBottomMargin / 2;

    mLinearLayout.addView(tv, params);
  }

  public void setTranslationClickedListener(
      TranslationClickedListener listener) {
    mTranslationClickedListener = listener;
  }

  public interface TranslationClickedListener {

    void onTranslationClicked();
  }
}
