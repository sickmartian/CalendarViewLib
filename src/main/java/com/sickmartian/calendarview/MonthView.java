package com.sickmartian.calendarview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Calendar;

/**
 * Created by sickmartian on 11/24/2015.
 */
public class MonthView extends CalendarView implements GestureDetector.OnGestureListener {
    public static final int ROWS = 6;
    public static final int DAYS_IN_GRID = 42;

    // User set state
    ArrayList<ArrayList<View>> mChildInDays;
    int mCurrentDay;
    int mSelectedDay = INITIAL;
    int mYear;
    int mMonth;

    // Things we calculate and use to draw
    RectF[] mDayCells = new RectF[DAYS_IN_GRID];
    DayNumber[] mDayNumbers = new DayNumber[DAYS_IN_GRID];
    ArrayList<Integer> mCellsWithOverflow;
    int mLastDayOfMonth;
    int mFirstCellOfMonth = INITIAL;

    private static class DayNumber implements DayString {
        private String dayString;

        public DayNumber(int day) {
            this.dayString = Integer.toString(day);
        }

        @Override
        public String getDayString() {
            return dayString;
        }
    }

    public MonthView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    private void setDateInternal(int month, int year) {
        mYear = year;
        mMonth = month;

        sharedSetDate();
    }

    private void sharedSetDate() {
        // Get first day of the week
        Calendar cal = getUTCCalendar();
        makeCalendarBeginningOfDay(cal);
        cal.set(mYear, mMonth, 1);
        int firstDayInWeekOfMonth = cal.get(Calendar.DAY_OF_WEEK) - 1;

        // Get last day of the week
        mLastDayOfMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        cal.set(mYear, mMonth, mLastDayOfMonth);
        firstDayInWeekOfMonth = (firstDayInWeekOfMonth + mFirstDayOfTheWeekShift) % DAYS_IN_WEEK;

        cal.add(Calendar.MONTH, -1);
        cal.set(Calendar.DATE, 1);
        int lastDayOfLastMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        int day;
        mFirstCellOfMonth = INITIAL;
        for (int i = 0; i < DAYS_IN_GRID; i++) {
            if (i < firstDayInWeekOfMonth) {
                day = lastDayOfLastMonth - firstDayInWeekOfMonth + i + 1;
            } else if (i < firstDayInWeekOfMonth + mLastDayOfMonth) {
                day = i - firstDayInWeekOfMonth + 1;
                if (mFirstCellOfMonth == INITIAL) {
                    mFirstCellOfMonth = i;
                }
            } else {
                day = i - firstDayInWeekOfMonth - mLastDayOfMonth + 1;
            }
            mDayNumbers[i] = new DayNumber(day);
        }

        invalidate();
    }

    // Convenience methods to interact
    public void removeAllContent() {
        removeAllViews();

        mCellsWithOverflow = new ArrayList<>();
        mChildInDays = new ArrayList<>();
        for (int i = 0; i < DAYS_IN_GRID; i++) {
            mChildInDays.add(i, new ArrayList<View>());
        }
    }

    public void addViewToCell(int cellNumber, View viewToAppend) {
        if (cellNumber < 0 || cellNumber > DAYS_IN_GRID) return;

        addView(viewToAppend);

        ArrayList<View> dayArray = mChildInDays.get(cellNumber);
        dayArray.add(viewToAppend);
        mChildInDays.set(cellNumber, dayArray);

        invalidate();
    }

    public void setCurrentDay(Calendar currentDay) {
        // Only mark as current if it is this month
        if (currentDay != null &&
                currentDay.get(Calendar.YEAR) == mYear &&
                currentDay.get(Calendar.MONTH) == mMonth) {
            mCurrentDay = currentDay.get(Calendar.DATE);
            invalidate();
        } else if (currentDay == null || mCurrentDay != INITIAL) {
            // Only invalidate previous layout if we had a current day before
            mCurrentDay = INITIAL;
            invalidate();
        }
    }

    public void setCurrentDay(DayMetadata dayMetadata) {
        // Only mark as current if it is this month
        if (dayMetadata != null &&
                dayMetadata.getYear() == mYear &&
                dayMetadata.getMonth() == mMonth + 1) {
            mCurrentDay = dayMetadata.getDay();
            invalidate();
        } else if (dayMetadata == null || mCurrentDay != INITIAL) {
            // Only invalidate previous layout if we had a current day before
            mCurrentDay = INITIAL;
            invalidate();
        }
    }

    public void setCurrentDay(int dayOfThisMonth) {
        // Only mark as current if it is this month
        if (dayOfThisMonth <= mLastDayOfMonth && dayOfThisMonth > 0) {
            mCurrentDay = dayOfThisMonth;
            invalidate();
        } else if (mCurrentDay != INITIAL) {
            // Only invalidate previous layout if we had a current day before
            mCurrentDay = INITIAL;
            invalidate();
        }
    }

    public void setSelectedDay(int newSelectedDay) {
        // Accept days in the month
        if (newSelectedDay <= mLastDayOfMonth && newSelectedDay > 0) {
            mSelectedDay = newSelectedDay;
            invalidate();
            // Or the initial to unset it
        } else if (newSelectedDay == INITIAL) {
            mSelectedDay = INITIAL;
            invalidate();
        }
    }

    public void setSelectedDay(Calendar date) {
        // Only mark as selected if it is this month
        if (date != null &&
                date.get(Calendar.YEAR) == mYear &&
                date.get(Calendar.MONTH) == mMonth) {
            mSelectedDay = date.get(Calendar.DATE);
            invalidate();
        } else if (date == null || mSelectedDay != INITIAL) {
            //Unset if null or not of this month and we
            // have one selected
            mSelectedDay = INITIAL;
            invalidate();
        }
    }

    public void setSelectedDay(DayMetadata dayMetadata) {
        // Only mark as selected if it is this month
        if (dayMetadata != null &&
                dayMetadata.getYear() == mYear &&
                dayMetadata.getMonth() == mMonth + 1) {
            mSelectedDay = dayMetadata.getDay();
            invalidate();
        } else if (dayMetadata == null || mSelectedDay != INITIAL) {
            // Unset if null or not of this month and we
            // have one selected
            mSelectedDay = INITIAL;
            invalidate();
        }
    }

    public void setDate(int month, int year) {
        mYear = year;
        mMonth = month - 1;

        setSelectedDay(INITIAL);
        removeAllContent();
        sharedSetDate();
    }

    public DayMetadata getSelectedDay() {
        return new DayMetadata(mYear, mMonth + 1, mSelectedDay);
    }

    public int getSelectedCell() {
        if (mSelectedDay == INITIAL) {
            return INITIAL;
        }
        return mFirstCellOfMonth + mSelectedDay - 1;
    }

    public void setFirstDayOfTheWeek(int firstDayOfTheWeekShift) {
        if (mFirstDayOfTheWeekShift != firstDayOfTheWeekShift) {
            mFirstDayOfTheWeekShift = firstDayOfTheWeekShift;

            // Save pointer to previous data we might be able to save
            int previousFirstCellOfMonth = mFirstCellOfMonth;

            // Apply changes
            mWeekDays = getWeekdaysForShift(mFirstDayOfTheWeekShift); // Reset weekday names
            setDateInternal(mMonth, mYear); // Reset cells - Invalidates the view

            // Save month's content (discard out of month data)
            ArrayList<ArrayList<View>> oldChilds = mChildInDays;
            mChildInDays = new ArrayList<>();

            // Remove out of bound views
            for (int i = 0; i < DAYS_IN_GRID; i++) {
                if (i < previousFirstCellOfMonth || i >= previousFirstCellOfMonth + mLastDayOfMonth) {
                    for (int j = 0; j < oldChilds.get(i).size(); j++) {
                        removeView(oldChilds.get(i).get(j));
                    }
                }
            }

            // Send in-month cells and add new out of bound cells
            for (int i = 0; i < mFirstCellOfMonth; i++) {
                mChildInDays.add(new ArrayList<View>());
            }
            for (int i = previousFirstCellOfMonth; i <= previousFirstCellOfMonth + mLastDayOfMonth; i++) {
                mChildInDays.add(oldChilds.get(i));
            }
            for (int i = mChildInDays.size(); i < DAYS_IN_GRID; i++) {
                mChildInDays.add(new ArrayList<View>());
            }

            requestLayout();
        }
    }

    public int getFirstCellOfMonth() {
        return mFirstCellOfMonth;
    }

    public int getLastCellOfMonth() {
        return mFirstCellOfMonth + mLastDayOfMonth;
    }

    public ArrayList<View> getDayContent(DayMetadata dayMetadata) {
        if (dayMetadata != null) {
            if (dayMetadata.getMonth() == (mMonth + 1) && dayMetadata.getYear() == mYear) {
                return getDayContent(dayMetadata.getDay());
            }
        }
        return null;
    }

    public void setDayContent(DayMetadata dayMetadata, ArrayList<View> newContent) {
        if (dayMetadata != null) {
            if (dayMetadata.getMonth() == (mMonth + 1) && dayMetadata.getYear() == mYear) {
                setDayContent(dayMetadata.getDay(), newContent);
            }
        }
    }

    public ArrayList<View> getDayContent(int dayInMonth) {
        if (dayInMonth <= mLastDayOfMonth && dayInMonth > 0) {
            return getCellContent(mFirstCellOfMonth + dayInMonth - 1);
        }
        return null;
    }

    public void setDayContent(int dayInMonth, ArrayList<View> newContent) {
        if (dayInMonth <= mLastDayOfMonth && dayInMonth > 0) {
            setCellContent(mFirstCellOfMonth + dayInMonth - 1, newContent);
        }
    }

    public ArrayList<View> getCellContent(int cellNumber) {
        if (cellNumber < 0 || cellNumber > DAYS_IN_GRID) return null;

        return (ArrayList<View>) mChildInDays.get(cellNumber).clone();
    }

    public void setCellContent(int cellNumber, ArrayList<View> newContent) {
        if (cellNumber < 0 || cellNumber > DAYS_IN_GRID) return;

        // Add new views and remove discarded views
        ArrayList<View> oldContent = mChildInDays.get(cellNumber);
        for (View newView : newContent) {
            if (!(oldContent.contains(newView))) {
                addView(newView);
            }
        }
        for (View oldView : oldContent) {
            if (!(newContent.contains(oldView))) {
                removeView(oldView);
            }
        }

        // Set new content
        mChildInDays.set(cellNumber, newContent);
        requestLayout();
    }

    public void addViewToDay(DayMetadata dayMetadata, View viewToAppend) {
        if (dayMetadata.getMonth() == (mMonth + 1) && dayMetadata.getYear() == mYear) {
            addViewToDayInCurrentMonth(dayMetadata.getDay(), viewToAppend);
        }
    }

    public void addViewToDayInCurrentMonth(int dayInMonth, View newView) {
        if (dayInMonth < 0 || dayInMonth > mLastDayOfMonth) return;
        addViewToCell(dayInMonth + mFirstCellOfMonth - 1, newView);
    }

    // View methods
    int mLastKnownWidth;
    int mLastKnownHeight;

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        mLastKnownWidth = w;
        mLastKnownHeight = h;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        // We have a fixed size, we can omit some child views if they don't fit later
        int w = resolveSizeAndState((int) (
                        (mSingleLetterWidth + mBetweenSiblingsPadding) // Single column min size
                                * 2 // For chars in days of the month
                                * DAYS_IN_WEEK),
                widthMeasureSpec, 0);
        int h = resolveSizeAndState((int) ((mBetweenSiblingsPadding * 4 + mSingleLetterHeight) * DAYS_IN_WEEK), heightMeasureSpec, 0);

        setMeasuredDimension(w, h);

        // Measure child layouts if we have any
        recalculateCells(w, h, mDayCells, ROWS);
        if (mDayCells.length == 0 || mDayCells[0] == null) return;

        float alreadyUsedTop = mEndOfHeaderWithWeekday;
        for (int i = 0; i < DAYS_IN_GRID; i++) {
            if (i >= DAYS_IN_WEEK) {
                alreadyUsedTop = mEndOfHeaderWithoutWeekday;
            }

            ArrayList<View> childArrayForDay = mChildInDays.get(i);
            for (int j = 0; j < childArrayForDay.size(); j++) {
                View viewToPlace = childArrayForDay.get(j);
                if (viewToPlace.getVisibility() != GONE) {
                    int wSpec = MeasureSpec.makeMeasureSpec(Math.round(mDayCells[i].width()), MeasureSpec.EXACTLY);
                    int hSpec = MeasureSpec.makeMeasureSpec(Math.round(mDayCells[i].height() - alreadyUsedTop), MeasureSpec.AT_MOST);
                    viewToPlace.measure(wSpec, hSpec);
                }
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        mCellsWithOverflow.clear();
        float topOffset;
        for (int i = 0; i < DAYS_IN_GRID; i++) {
            ArrayList<View> childArrayForDay = mChildInDays.get(i);
            if (i >= DAYS_IN_WEEK) {
                topOffset = mEndOfHeaderWithoutWeekday;
            } else {
                topOffset = mEndOfHeaderWithWeekday;
            }

            int cellBottom = (int) (mDayCells[i].bottom - mOverflowHeight);
            for (int j = 0; j < childArrayForDay.size(); j++) {
                View viewToPlace = childArrayForDay.get(j);
                if (viewToPlace.getVisibility() != GONE) {

                    // If we overflow the cell, crop the view
                    int proposedItemBottom = (int) (mDayCells[i].top + topOffset + viewToPlace.getMeasuredHeight());
                    if (proposedItemBottom >= cellBottom) {
                        proposedItemBottom = cellBottom;
                    }

                    viewToPlace.layout(
                            (int) mDayCells[i].left,
                            (int) (mDayCells[i].top + topOffset),
                            (int) mDayCells[i].right,
                            proposedItemBottom
                    );

                    topOffset += viewToPlace.getMeasuredHeight();

                    // If we don't have more space below, stop drawing them
                    if (proposedItemBottom == cellBottom) {
                        mCellsWithOverflow.add(i);
                        break;
                    }
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

    protected void drawBackgrounds(Canvas canvas, RectF[] dayCells,
                                   int firstActiveCell, int lastActiveCell) {
        int activationRow = firstActiveCell / DAYS_IN_WEEK;
        int deactivationRow = lastActiveCell / DAYS_IN_WEEK;

        for (int row = 0; row < dayCells.length / DAYS_IN_WEEK; row++) {
            int firstCellInRow = row * DAYS_IN_WEEK;
            int lastCellInRow = firstCellInRow + (DAYS_IN_WEEK - 1);
            if (row == activationRow) {
                // If there is at least one inactive cell in the row, skip that block
                drawBlock(canvas, mActiveBackgroundColor,
                            firstActiveCell > firstCellInRow ? BLOCK.RIGHT : BLOCK.COMPLETE,
                        dayCells[firstActiveCell].left,
                        dayCells[firstCellInRow].top,
                        dayCells[lastCellInRow].right,
                        dayCells[lastCellInRow].bottom);
            } else if (row < deactivationRow) { // Whole active row
                drawBlock(canvas, mActiveBackgroundColor, BLOCK.COMPLETE,
                        dayCells[firstCellInRow].left, dayCells[firstCellInRow].top,
                        dayCells[lastCellInRow].right, dayCells[lastCellInRow].bottom);
            } else if (row == deactivationRow) {
                // If there is at least one active cell in the row, draw that block
                boolean anyActive = lastActiveCell >= firstCellInRow;
                if (anyActive) {
                    drawBlock(canvas, mActiveBackgroundColor, BLOCK.LEFT,
                            dayCells[firstCellInRow].left,
                            dayCells[firstCellInRow].top,
                            dayCells[lastActiveCell].right,
                            dayCells[lastCellInRow].bottom);
                }
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (getWidth() == 0 || getHeight() == 0) return; // Never got measured, nothing to draw

        canvas.drawPaint(mInactiveBackgroundColor);

        int lastCellOfMonth = mFirstCellOfMonth + mLastDayOfMonth - 1;
        drawBackgrounds(canvas, mDayCells, mFirstCellOfMonth, lastCellOfMonth);
        drawSelectedCell(canvas, mDayCells, mSelectedDay == INITIAL ? INITIAL :
                mSelectedDay + mFirstCellOfMonth - 1);


        // Weekdays and day numbers
        for (int i = 0; i < DAYS_IN_GRID; i++) {
            // Cell in month
            if (i >= mFirstCellOfMonth && i <= lastCellOfMonth) {
                int day = i - mFirstCellOfMonth + 1;
                // Current day might have a decoration
                if (mCurrentDay == day && mCurrentDayDrawable != null) {
                    // Decoration
                    float topOffset = mBetweenSiblingsPadding;
                    if (i < DAYS_IN_WEEK) {
                        topOffset += mBetweenSiblingsPadding + mSingleLetterHeight;
                    }
                    mCurrentDayDrawable.setBounds(
                            (int) (mDayCells[i].left + mBetweenSiblingsPadding),
                            (int) (mDayCells[i].top + topOffset),
                            (int) (mDayCells[i].left + mBetweenSiblingsPadding + mDecorationSize),
                            (int) (mDayCells[i].top + mDecorationSize + topOffset));
                    mCurrentDayDrawable.draw(canvas);

                    drawDayTextsInCell(canvas, i, mDayCells, mCurrentDayTextColor, mActiveTextColor, mDayNumbers);
                } else {
                    drawDayTextsInCell(canvas, i, mDayCells, mActiveTextColor, mActiveTextColor, mDayNumbers);
                }
            } else { // Cell not in month
                drawDayTextsInCell(canvas, i, mDayCells, mInactiveTextColor, mInactiveTextColor, mDayNumbers);
            }
        }

        drawOverflow(canvas, mCellsWithOverflow, mDayCells);

        // Separation lines
        canvas.drawLine(0, mDayCells[7].top, getWidth(), mDayCells[7].top, mSeparationPaint);
        canvas.drawLine(0, mDayCells[14].top, getWidth(), mDayCells[14].top, mSeparationPaint);
        canvas.drawLine(0, mDayCells[21].top, getWidth(), mDayCells[21].top, mSeparationPaint);
        canvas.drawLine(0, mDayCells[28].top, getWidth(), mDayCells[28].top, mSeparationPaint);
        canvas.drawLine(0, mDayCells[35].top, getWidth(), mDayCells[35].top, mSeparationPaint);

        drawVerticalSeparation(canvas, mDayCells);
    }

    // Interaction
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return mDetector.onTouchEvent(event);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return true;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return true;
    }

    public int getCellFromLocation(float x, float y) {
        for (int i = 0; i < mDayCells.length; i++) {
            if (mDayCells[i].contains(x, y)) {
                if (i >= mFirstCellOfMonth &&
                        i <= mFirstCellOfMonth + mLastDayOfMonth - 1) {
                    return i - mFirstCellOfMonth + 1;
                } else {
                    return INITIAL;
                }
            }
        }
        return INITIAL;
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        if (mDaySelectionListener != null) {
            int currentDay = getCellFromLocation(e.getX(), e.getY());
            if (currentDay != INITIAL) {
                mDaySelectionListener.onTapEnded(this, new DayMetadata(mYear, mMonth + 1, currentDay));
                return true;
            }
        }
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {
        if (mDaySelectionListener != null) {
            int currentDay = getCellFromLocation(e.getX(), e.getY());
            if (currentDay != INITIAL) {
                mDaySelectionListener.onLongClick(this, new DayMetadata(mYear, mMonth + 1, currentDay));
            }
        }
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        return false;
    }

    // Persistence
    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        MyOwnState myOwnState = new MyOwnState(superState);
        myOwnState.mYear = mYear;
        myOwnState.mMonth = mMonth;
        myOwnState.mCurrentDay = mCurrentDay;
        myOwnState.mSelectedDay = mSelectedDay;
        myOwnState.mLastKnownWidth = mLastKnownWidth;
        myOwnState.mLastKnownHeight = mLastKnownHeight;
        return myOwnState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof MyOwnState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        MyOwnState myOwnState = (MyOwnState) state;
        super.onRestoreInstanceState(myOwnState.getSuperState());

        setDateInternal(myOwnState.mMonth, myOwnState.mYear);
        setCurrentDay(myOwnState.mCurrentDay);
        setSelectedDay(myOwnState.mSelectedDay);
        mLastKnownWidth = myOwnState.mLastKnownWidth;
        mLastKnownHeight = myOwnState.mLastKnownHeight;
        recalculateCells(mLastKnownWidth, mLastKnownHeight, mDayCells, ROWS);
    }

    private static class MyOwnState extends BaseSavedState {
        int mCurrentDay;
        int mSelectedDay;
        int mYear;
        int mMonth;
        int mLastKnownWidth;
        int mLastKnownHeight;

        MyOwnState(Parcelable superState) {
            super(superState);
        }

        MyOwnState(Parcel in) {
            super(in);
            mYear = in.readInt();
            mMonth = in.readInt();
            mCurrentDay = in.readInt();
            mSelectedDay = in.readInt();
            mLastKnownWidth = in.readInt();
            mLastKnownHeight = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(mYear);
            out.writeInt(mMonth);
            out.writeInt(mCurrentDay);
            out.writeInt(mSelectedDay);
            out.writeInt(mLastKnownWidth);
            out.writeInt(mLastKnownHeight);
        }

        public static final Parcelable.Creator<MyOwnState> CREATOR =
                new Parcelable.Creator<MyOwnState>() {
                    public MyOwnState createFromParcel(Parcel in) {
                        return new MyOwnState(in);
                    }

                    public MyOwnState[] newArray(int size) {
                        return new MyOwnState[size];
                    }
                };
    }


    // Other
    @Override
    public String toString() {
        return mYear + "-" + (mMonth + 1);
    }
}
