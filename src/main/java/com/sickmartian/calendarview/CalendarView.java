package com.sickmartian.calendarview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.annotation.IntDef;
import android.support.v4.view.GestureDetectorCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.View;
import android.view.ViewGroup;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by sickmartian on 2/20/2016.
 */
public abstract class CalendarView extends ViewGroup implements GestureDetector.OnGestureListener {
    public static boolean DEBUG = false;
    private static final String SINGLE_DIGIT_DAY_WIDTH_TEMPLATE = "7";
    private static final String DOUBLE_DIGIT_DAY_WIDTH_TEMPLATE = "30";
    private static final String SPECIAL_DAY_THAT_NEEDS_WORKAROUND = "31";
    protected static final int INITIAL = -1;
    public static final int DAYS_IN_WEEK = 7;
    protected String[] mWeekDays;

    @IntDef({SUNDAY_SHIFT, SATURDAY_SHIFT, MONDAY_SHIFT})
    @interface PossibleWeekShift {
    }

    public static final int SUNDAY_SHIFT = 0;
    public static final int SATURDAY_SHIFT = 1;
    public static final int MONDAY_SHIFT = 6;
    protected int mFirstDayOfTheWeekShift = SUNDAY_SHIFT;

    public interface DayString {
        String getDayString();
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
    public static class DayMetadata implements DayString {
        int year;
        int month;
        int day;
        String dayString;

        public DayMetadata(int year, int month, int day) {
            this.year = year;
            this.month = month;
            setDay(day);
        }

        public int getYear() {
            return year;
        }

        public void setYear(int year) {
            this.year = year;
        }

        public int getMonth() {
            return month;
        }

        public void setMonth(int month) {
            this.month = month;
        }

        public int getDay() {
            return day;
        }

        public void setDay(int day) {
            this.day = day;
            this.dayString = Integer.toString(day);
        }

        @Override
        public String getDayString() {
            return dayString;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            DayMetadata that = (DayMetadata) o;

            return year == that.year && month == that.month && day == that.day;
        }

        @Override
        public int hashCode() {
            int result = year;
            result = 31 * result + month;
            result = 31 * result + day;
            return result;
        }
    }

    protected final Paint mActiveTextColor;
    protected final Paint mSeparationPaint;
    protected final Paint mInactiveTextColor;
    protected final Paint mInactiveBackgroundColor;
    protected final Paint mActiveBackgroundColor;
    protected final Paint mSelectedBackgroundColor;
    protected final Drawable mCurrentDayDrawable;
    protected final float mDecorationSize;
    protected final float mBetweenSiblingsPadding;
    protected float mMaterialLeftRightPadding;
    protected boolean mShowOverflow;
    protected boolean mIgnoreMaterialGrid;
    protected boolean mSeparateDaysVertically;
    protected final Paint mOverflowPaint;
    protected final float mOverflowHeight;
    protected final float mTextSize;
    protected final Paint mCurrentDayTextColor;
    protected final float dp1;
    protected final float dp4;
    protected final Rect mReusableTextBound = new Rect();
    protected final float mEndOfHeaderWithoutWeekday;
    protected final float mEndOfHeaderWithWeekday;
    protected final int mSingleLetterWidth;
    protected final int mSingleLetterHeight;

    public CalendarView(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.MonthView,
                0, 0);

        dp4 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics());
        dp1 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics());

        try {
            // Text
            mTextSize = a.getDimension(R.styleable.MonthView_textSize, getResources().getDimension(R.dimen.calendar_view_default_text_size));

            mCurrentDayTextColor = new Paint(Paint.ANTI_ALIAS_FLAG);
            mCurrentDayTextColor.setColor(a.getColor(R.styleable.MonthView_currentDayTextColor, Color.WHITE));
            mCurrentDayTextColor.setTextSize(mTextSize);

            mActiveTextColor = new Paint(Paint.ANTI_ALIAS_FLAG);
            mActiveTextColor.setColor(a.getColor(R.styleable.MonthView_activeTextColor, Color.BLACK));
            mActiveTextColor.setTextSize(mTextSize);

            mInactiveTextColor = new Paint(Paint.ANTI_ALIAS_FLAG);
            mInactiveTextColor.setColor(a.getColor(R.styleable.MonthView_inactiveTextColor, Color.DKGRAY));
            mInactiveTextColor.setTextSize(mTextSize);

            // Cell background
            mSeparationPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mSeparationPaint.setStyle(Paint.Style.STROKE);
            mSeparationPaint.setColor(a.getColor(R.styleable.MonthView_separatorColor, Color.LTGRAY));

            mActiveBackgroundColor = new Paint(Paint.ANTI_ALIAS_FLAG);
            mActiveBackgroundColor.setStyle(Paint.Style.FILL);
            mActiveBackgroundColor.setColor(a.getColor(R.styleable.MonthView_activeBackgroundColor, Color.WHITE));

            mInactiveBackgroundColor = new Paint(Paint.ANTI_ALIAS_FLAG);
            mInactiveBackgroundColor.setStyle(Paint.Style.FILL);
            mInactiveBackgroundColor.setColor(a.getColor(R.styleable.MonthView_inactiveBackgroundColor, Color.GRAY));

            mSelectedBackgroundColor = new Paint(Paint.ANTI_ALIAS_FLAG);
            mSelectedBackgroundColor.setStyle(Paint.Style.FILL);
            mSelectedBackgroundColor.setColor(a.getColor(R.styleable.MonthView_selectedBackgroundColor, Color.YELLOW));

            // Decoration
            mCurrentDayDrawable = a.getDrawable(R.styleable.MonthView_currentDayDecorationDrawable);

            mDecorationSize = a.getDimension(R.styleable.MonthView_currentDayDecorationSize, 0);
            mBetweenSiblingsPadding = dp4;

            mIgnoreMaterialGrid = a.getBoolean(R.styleable.MonthView_ignoreMaterialGrid, true);
            recalculatePadding();

            mSeparateDaysVertically = a.getBoolean(R.styleable.MonthView_separateDaysVertically, false);

            // Overflow
            mShowOverflow = a.getBoolean(R.styleable.MonthView_showOverflow, true);
            mOverflowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mOverflowPaint.setStyle(Paint.Style.FILL);
            mOverflowPaint.setColor(a.getColor(R.styleable.MonthView_overflowColor, Color.GREEN));
            mOverflowHeight = a.getDimension(R.styleable.MonthView_overflowHeight,
                    getResources().getDimension(R.dimen.calendar_view_default_overflow_height));
        } finally {
            a.recycle();
        }

        // Arrays in initial state so we can draw ourselves on the editor
        removeAllContent();

        // Calculate a bunch of no-data dependent dimensions
        mActiveTextColor.getTextBounds("W", 0, 1, mReusableTextBound);
        mSingleLetterWidth = mReusableTextBound.width();
        mSingleLetterHeight = mReusableTextBound.height();
        if (mDecorationSize > 0) {
            mEndOfHeaderWithoutWeekday = mBetweenSiblingsPadding * 2 + mDecorationSize;
            mEndOfHeaderWithWeekday = mBetweenSiblingsPadding * 3 + mDecorationSize + mSingleLetterHeight;
        } else {
            mEndOfHeaderWithoutWeekday = mBetweenSiblingsPadding * 2 + mSingleLetterHeight;
            mEndOfHeaderWithWeekday = mBetweenSiblingsPadding * 3 + mSingleLetterHeight * 2;
        }

        // Interaction
        setupInteraction(context);

        // We will draw ourselves, even if we are a ViewGroup
        setWillNotDraw(false);

        mWeekDays = getWeekdaysForShift(mFirstDayOfTheWeekShift);
    }

    protected void recalculateCells(int w, int h, RectF[] dayCells, int rowCount) {
        int firstRowExtraHeight = (int) (mSingleLetterHeight + mBetweenSiblingsPadding);

        int COLS = 7;
        float widthStep = ( w - mMaterialLeftRightPadding * 2 ) / (float) COLS;
        float heightStep = ( h - firstRowExtraHeight ) / (float) rowCount;

        for (int col = 0; col < COLS; col++) {
            float lastBottom = INITIAL;
            for (int row = 0; row < rowCount; row++) {
                if (row == 0) {
                    lastBottom = (heightStep + firstRowExtraHeight);
                    dayCells[row * COLS  + col] = new RectF(widthStep * col + mMaterialLeftRightPadding,
                            heightStep * row,
                            widthStep * (col + 1) + mMaterialLeftRightPadding,
                            lastBottom);
                } else {
                    float newBottom = (lastBottom + heightStep);
                    dayCells[row * COLS  + col] = new RectF(widthStep * col + mMaterialLeftRightPadding,
                            lastBottom,
                            widthStep * (col + 1) + mMaterialLeftRightPadding,
                            newBottom);
                    lastBottom = newBottom;
                }
            }
        }
    }

    protected void drawBackgroundForCell(Canvas canvas, int cellNumber, RectF[] dayCells,
                                         boolean selected,
                                         Paint selectedBackgroundColor,
                                         Paint backgroundColor) {
        // Just paint with the correct color if we are ignoring the material guidelines
        if (mIgnoreMaterialGrid) {
            canvas.drawRect(dayCells[cellNumber].left,
                    dayCells[cellNumber].top,
                    dayCells[cellNumber].right,
                    dayCells[cellNumber].bottom,
                    selected ? selectedBackgroundColor : backgroundColor);
            return;
        }

        // Calculate padding for this cell
        int cellMod = cellNumber % DAYS_IN_WEEK;
        float additionalLeft = cellMod == 0 ? mMaterialLeftRightPadding * -1 : 0;
        float additionalRight = cellMod == 6 ? mMaterialLeftRightPadding : 0;

        // Just paint unselected
        canvas.drawRect(dayCells[cellNumber].left + additionalLeft,
                dayCells[cellNumber].top,
                dayCells[cellNumber].right + additionalRight,
                dayCells[cellNumber].bottom, backgroundColor);

        // And then the selection with padding to the background (so the background shows on the
        // left and right extremes):
        if (selected) {
            canvas.drawRect(dayCells[cellNumber].left,
                    dayCells[cellNumber].top,
                    dayCells[cellNumber].right,
                    dayCells[cellNumber].bottom, selectedBackgroundColor);
        }
    }

    protected void drawOverflow(Canvas canvas, ArrayList<Integer> mCellsWithOverflow, RectF[] dayCells) {
        // Overflow
        if (mShowOverflow) {
            for (int cellWithOverflow : mCellsWithOverflow) {
                canvas.drawRect(dayCells[cellWithOverflow].left, dayCells[cellWithOverflow].bottom - mOverflowHeight,
                        dayCells[cellWithOverflow].right, dayCells[cellWithOverflow].bottom, mOverflowPaint);
            }
        }
    }

    protected void drawVerticalSeparation(Canvas canvas, RectF[] dayCells) {
        // Separation
        if (mSeparateDaysVertically) {
            canvas.drawLine(dayCells[0].right, 0, dayCells[0].right, getHeight(), mSeparationPaint);
            canvas.drawLine(dayCells[1].right, 0, dayCells[1].right, getHeight(), mSeparationPaint);
            canvas.drawLine(dayCells[2].right, 0, dayCells[2].right, getHeight(), mSeparationPaint);
            canvas.drawLine(dayCells[3].right, 0, dayCells[3].right, getHeight(), mSeparationPaint);
            canvas.drawLine(dayCells[4].right, 0, dayCells[4].right, getHeight(), mSeparationPaint);
            canvas.drawLine(dayCells[5].right, 0, dayCells[5].right, getHeight(), mSeparationPaint);
        }
    }

    protected void drawDayTextsInCell(Canvas canvas, int cellNumber, RectF[] dayCells,
                                      Paint mCurrentDayTextColor,
                                      Paint mCurrentWeekDayTextColor,
                                      DayString[] dayStrings) {
        float topOffset = 0;
        // Weekday
        if (cellNumber < DAYS_IN_WEEK) {
            mCurrentWeekDayTextColor.getTextBounds("S", 0, 1, mReusableTextBound);

            int decorationLeftOffset = 0;
            if (mDecorationSize > 0) {
                decorationLeftOffset = (int) ((mDecorationSize - mReusableTextBound.width()) / 2);
            }

            canvas.drawText(mWeekDays[cellNumber],
                    dayCells[cellNumber].left + mBetweenSiblingsPadding + decorationLeftOffset,
                    dayCells[cellNumber].top + mBetweenSiblingsPadding + mReusableTextBound.height(),
                    mCurrentWeekDayTextColor);
            topOffset = mBetweenSiblingsPadding + mReusableTextBound.height();
        }

        // Day number
        // Check we have something to draw first.
        if (dayStrings == null || dayStrings.length == 0 || dayStrings[0] == null) return;

        // So the days align between each other inside the decoration, we use
        // the same number to calculate the length of the text inside the decoration
        String templateDayText;
        if (dayStrings[cellNumber].getDayString().length() < 2) {
            templateDayText = SINGLE_DIGIT_DAY_WIDTH_TEMPLATE;
        } else if (dayStrings[cellNumber].getDayString().equals(SPECIAL_DAY_THAT_NEEDS_WORKAROUND)) {
            templateDayText = dayStrings[cellNumber].getDayString();
        } else {
            templateDayText = DOUBLE_DIGIT_DAY_WIDTH_TEMPLATE;
        }
        mCurrentDayTextColor.getTextBounds(templateDayText, 0, templateDayText.length(), mReusableTextBound);

        int decorationLeftOffset = 0;
        int decorationTopOffset = 0;
        if (mDecorationSize > 0) {
            decorationLeftOffset = (int) ((mDecorationSize - mReusableTextBound.width()) / 2);
            decorationTopOffset = (int) ((mDecorationSize - mReusableTextBound.height()) / 2);
        }

        canvas.drawText(dayStrings[cellNumber].getDayString(),
                dayCells[cellNumber].left + mBetweenSiblingsPadding + decorationLeftOffset,
                dayCells[cellNumber].top + mBetweenSiblingsPadding + mReusableTextBound.height() + decorationTopOffset + topOffset,
                mCurrentDayTextColor);
    }

    private void recalculatePadding() {
        if (mIgnoreMaterialGrid) {
            mMaterialLeftRightPadding = 0f;
        } else {
            mMaterialLeftRightPadding = dp4 * 4;
        }
    }

    // Utils for calendar
    @SuppressWarnings("unused")
    public static int getCalendarDayForShift(@PossibleWeekShift int weekShift) {
        int dayForShift;
        switch (weekShift) {
            case SUNDAY_SHIFT: {
                dayForShift = Calendar.SUNDAY;
                break;
            }
            case SATURDAY_SHIFT: {
                dayForShift = Calendar.SATURDAY;
                break;
            }
            case MONDAY_SHIFT: {
                dayForShift = Calendar.MONDAY;
                break;
            } default: {
                dayForShift = Calendar.MONDAY;
                break;
            }
        }
        return dayForShift;
    }

    public static Calendar getUTCCalendar() {
        return Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    }

    public static void makeCalendarBeginningOfDay(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }

    public static String[] getWeekdaysForShift(int firstDayOfTheWeekShift) {
        String[] weekDays = new String[DAYS_IN_WEEK];
        String[] namesOfDays = new DateFormatSymbols().getShortWeekdays();

        for (int i = 0; i < DAYS_IN_WEEK; i++) {
            weekDays[i] = namesOfDays[1 + (DAYS_IN_WEEK - firstDayOfTheWeekShift + i) % DAYS_IN_WEEK]
                    .toUpperCase();
            if (Locale.getDefault().getISO3Language().equalsIgnoreCase("zho")) {
                weekDays[i] = weekDays[i].substring(1, 2);
            } else {
                weekDays[i] = weekDays[i].substring(0, 1);
            }
        }
        return weekDays;
    }

    // Common interface
    public abstract void setFirstDayOfTheWeek(int firstDayOfTheWeekShift);

    public int getFirstDayOfTheWeek() {
        return mFirstDayOfTheWeekShift;
    }

    @SuppressWarnings("unused")
    public boolean hasDayVertialSeparation() {
        return mSeparateDaysVertically;
    }

    public void setSeparateDaysVertically(boolean separateDaysVertically) {
        if (separateDaysVertically != mSeparateDaysVertically) {
            mSeparateDaysVertically = separateDaysVertically;
            invalidate();
        }
    }

    public void setShowOverflow(boolean showOverflow) {
        if (showOverflow != mShowOverflow) {
            mShowOverflow = showOverflow;
            invalidate();
        }
    }

    @SuppressWarnings("unused")
    public boolean isOverflowShown() {
        return mShowOverflow;
    }

    @SuppressWarnings("unused")
    public boolean isIgnoringMaterialGrid() {
        return mIgnoreMaterialGrid;
    }

    public void setIgnoreMaterialGrid(boolean ignoreMaterialGrid) {
        if (ignoreMaterialGrid != mIgnoreMaterialGrid) {
            mIgnoreMaterialGrid = ignoreMaterialGrid;
            recalculatePadding();
            invalidate();
        }
    }

    public abstract void removeAllContent();

    public abstract void setCurrentDay(Calendar currentDay);

    @SuppressWarnings("unused")
    public abstract void setCurrentDay(DayMetadata dayMetadata);

    @SuppressWarnings("unused")
    public abstract void setSelectedDay(Calendar selectedDay);

    public abstract void setSelectedDay(DayMetadata dayMetadata);

    public abstract DayMetadata getSelectedDay();

    public abstract int getSelectedCell();

    public abstract void addViewToDay(DayMetadata dayMetadata, View viewToAppend);

    public abstract void addViewToCell(int cellNumber, View viewToAppend);

    public abstract ArrayList<View> getDayContent(DayMetadata day);

    public abstract void setDayContent(DayMetadata day, ArrayList<View> newContent);

    public abstract ArrayList<View> getCellContent(int cellNumber);

    public abstract void setCellContent(int cellNumber, ArrayList<View> newContent);

    // Interaction
    protected GestureDetectorCompat mDetector;
    protected DaySelectionListener mDaySelectionListener;

    public interface DaySelectionListener {
        void onTapEnded(CalendarView calendarView, DayMetadata day);

        void onLongClick(CalendarView calendarView, DayMetadata day);
    }

    public void setDaySelectedListener(DaySelectionListener listener) {
        this.mDaySelectionListener = listener;
    }

    private void setupInteraction(Context context) {
        mDetector = new GestureDetectorCompat(context, this);
        mDetector.setIsLongpressEnabled(true);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (DEBUG) Log.d(toString() + "@" + System.identityHashCode(this),
             "onSizeChanged: " + "w: " + w + " h: " + h + " oldw: " + oldw + " oldh" + oldh);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (DEBUG) Log.d(toString() + "@" + System.identityHashCode(this),
            "onDraw: " + canvas);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (DEBUG) Log.d(toString() + "@" + System.identityHashCode(this),
            "onLayout: " + " c: " + changed + " l: " + l + " t: " + t
            + " r: " + r + " b: " + b);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (DEBUG) Log.d(toString() + "@" + System.identityHashCode(this),
                "onMeasure: " + "w: " + widthMeasureSpec + " h: " + heightMeasureSpec);
    }
}
