package com.sickmartian.calendarview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
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
public class WeekView extends CalendarView implements GestureDetector.OnGestureListener {
    public static final int ROWS = 1;
    public static final int DAYS_IN_GRID = 7;

    // User set state
    ArrayList<ArrayList<View>> mChildInDays;
    int mCurrentCell;
    int mSelectedCell = INITIAL;
    DayMetadata mDay;

    // Things we calculate and use to draw
    RectF[] mDayCells = new RectF[DAYS_IN_GRID];
    DayMetadata[] mDayMetadata = new DayMetadata[DAYS_IN_GRID];
    ArrayList<Integer> mCellsWithOverflow;

    public WeekView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    private void setDateInternal(DayMetadata dayMetadata) {
        mDay = dayMetadata;

        sharedSetDate();
    }

    private void sharedSetDate() {
        Calendar firstDayOfWeek = getUTCCalendar();
        makeCalendarBeginningOfDay(firstDayOfWeek);
        firstDayOfWeek.set(Calendar.YEAR, mDay.getYear());
        firstDayOfWeek.set(Calendar.MONTH, mDay.getMonth() - 1);
        firstDayOfWeek.set(Calendar.DATE, mDay.getDay());

        int anyDayOfTheWeek = firstDayOfWeek.get(Calendar.DAY_OF_WEEK) - 1; // Starting on Sunday
        int givenDayDifferentToStart = ( anyDayOfTheWeek + mFirstDayOfTheWeekShift ) % DAYS_IN_WEEK;
        firstDayOfWeek.add(Calendar.DATE, givenDayDifferentToStart * -1);

        int lastDay;
        for (int i = 0; i < DAYS_IN_GRID; i++) {
            lastDay = firstDayOfWeek.get(Calendar.DATE);
            mDayMetadata[i] = new DayMetadata(firstDayOfWeek.get(Calendar.YEAR),
                    firstDayOfWeek.get(Calendar.MONTH) + 1,
                    lastDay
            );
            firstDayOfWeek.add(Calendar.DATE, 1);
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

    @Override
    public void addViewToDay(DayMetadata dayMetadata, View viewToAppend) {
        if (dayMetadata == null) return;

        int cell = 0;
        for (DayMetadata metadata : mDayMetadata) {
            if (dayMetadata.equals(metadata)) {
                addViewToCell(cell, viewToAppend);
                break;
            }
            cell++;
        }

    }

    public void setCurrentDay(Calendar currentDay) {
        if (currentDay == null && mCurrentCell != INITIAL) {
            mCurrentCell = INITIAL;
            invalidate();
            return;
        } else if (currentDay == null) {
            return;
        }

        // Only mark and invalidate if it corresponds to our cells
        int i = 0;
        for (DayMetadata metadata : mDayMetadata) {
            if (metadata.day == currentDay.get(Calendar.DATE) &&
                    metadata.month == currentDay.get(Calendar.MONTH) + 1 &&
                    metadata.year == currentDay.get(Calendar.YEAR)) {
                mCurrentCell = i;
                invalidate();
                return;
            }
            i++;
        }

        if (mCurrentCell != INITIAL) {
            mCurrentCell = INITIAL;
            invalidate();
        }
    }

    public void setCurrentDay(DayMetadata currentDay) {
        if (currentDay == null && mCurrentCell != INITIAL) {
            mCurrentCell = INITIAL;
            invalidate();
            return;
        } else if (currentDay == null) {
            return;
        }

        // Only mark and invalidate if it corresponds to our cells
        int i = 0;
        for (DayMetadata metadata : mDayMetadata) {
            if (metadata.equals(currentDay)) {
                mCurrentCell = i;
                invalidate();
                return;
            }
            i++;
        }

        if (mCurrentCell != INITIAL) {
            mCurrentCell = INITIAL;
            invalidate();
        }
    }

    public void setSelectedDay(Calendar selectedDay) {
        if (selectedDay == null && mSelectedCell != INITIAL) {
            mSelectedCell = INITIAL;
            invalidate();
            return;
        } else if (selectedDay == null) {
            return;
        }

        // Only mark and invalidate if it corresponds to our cells
        int i = 0;
        for (DayMetadata metadata : mDayMetadata) {
            if (metadata.day == selectedDay.get(Calendar.DATE) &&
                    metadata.month == selectedDay.get(Calendar.MONTH) + 1 &&
                    metadata.year == selectedDay.get(Calendar.YEAR)) {
                mSelectedCell = i;
                invalidate();
                return;
            }
            i++;
        }

        if (mSelectedCell != INITIAL) {
            mSelectedCell = INITIAL;
            invalidate();
        }
    }

    public void setSelectedDay(DayMetadata selectedDay) {
        if (selectedDay == null && mSelectedCell != INITIAL) {
            mSelectedCell = INITIAL;
            invalidate();
            return;
        } else if (selectedDay == null) {
            return;
        }

        // Only mark and invalidate if it corresponds to our cells
        int i = 0;
        for (DayMetadata metadata : mDayMetadata) {
            if (metadata.equals(selectedDay)) {
                mSelectedCell = i;
                invalidate();
                return;
            }
            i++;
        }

        if (mSelectedCell != INITIAL) {
            mSelectedCell = INITIAL;
            invalidate();
        }
    }

    public void setDate(DayMetadata dayMetadata) {
        mDay = dayMetadata;

        setSelectedDay((DayMetadata) null);
        removeAllContent();
        sharedSetDate();
    }

    public DayMetadata getSelectedDay() {
        if (mSelectedCell == INITIAL) {
            return null;
        }
        return mDayMetadata[mSelectedCell];
    }

    public int getSelectedCell() {
        return mSelectedCell;
    }

    public void setFirstDayOfTheWeek(int firstDayOfTheWeekShift) {
        if (mFirstDayOfTheWeekShift != firstDayOfTheWeekShift) {
            mFirstDayOfTheWeekShift = firstDayOfTheWeekShift;

            // Apply changes
            mWeekDays = getWeekdaysForShift(mFirstDayOfTheWeekShift); // Reset weekday names
            setDateInternal(mDay); // Reset cells - Invalidates the view

            requestLayout();
        }
    }

    public ArrayList<View> getDayContent(DayMetadata day) {
        if (day != null) {
            int cell = 0;
            for (DayMetadata metadata : mDayMetadata) {
                if (day.equals(metadata)) {
                    return getCellContent(cell);
                }
                cell++;
            }
        }
        return null;
    }

    public void setDayContent(DayMetadata day, ArrayList<View> newContent) {
        if (day != null) {
            int cell = 0;
            for (DayMetadata metadata : mDayMetadata) {
                if (day.equals(metadata)) {
                    setCellContent(cell, newContent);
                }
                cell++;
            }
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

    @SuppressWarnings("unused")
    public DayMetadata getFirstDay() {
        if (mDayMetadata == null) return null;

        return mDayMetadata[0];
    }

    @SuppressWarnings("unused")
    public DayMetadata getLastDay() {
        if (mDayMetadata == null) return null;

        return mDayMetadata[mDayMetadata.length - 1];
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
                (mSingleLetterWidth + mBetweenSiblingsPadding ) // Single column min size
                        * 2 // For chars in days of the month
                        * DAYS_IN_WEEK),
                widthMeasureSpec, 0);
        int h = resolveSizeAndState((int) ((mBetweenSiblingsPadding * 4 + mSingleLetterHeight) * DAYS_IN_WEEK), heightMeasureSpec, 0);

        setMeasuredDimension(w, h);

        // Measure child layouts if we have
        recalculateCells(w, h, mDayCells, ROWS);
        if (mDayCells.length == 0 || mDayCells[0] == null) return;

        for (int i = 0; i < DAYS_IN_GRID; i++) {
            ArrayList<View> childArrayForDay = mChildInDays.get(i);
            for (int j = 0; j < childArrayForDay.size(); j++) {
                View viewToPlace = childArrayForDay.get(j);
                if (viewToPlace.getVisibility() != GONE) {
                    int wSpec = MeasureSpec.makeMeasureSpec(Math.round(mDayCells[i].width()), MeasureSpec.EXACTLY);
                    int hSpec = MeasureSpec.makeMeasureSpec(Math.round(mDayCells[i].height() - mEndOfHeaderWithWeekday), MeasureSpec.AT_MOST);
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
            topOffset = mEndOfHeaderWithWeekday;

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

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (getWidth() == 0 || getHeight() == 0) return; // Never got measured, nothing to draw

        canvas.drawPaint(mActiveBackgroundColor);
        drawSelectedCell(canvas, mDayCells, mSelectedCell);

        // Weekdays and day numbers
        float topOffset = mBetweenSiblingsPadding * 2 + mSingleLetterHeight;
        for (int i = 0; i < DAYS_IN_GRID; i++) {
            // Current day might have a decoration
            if (mCurrentCell == i && mCurrentDayDrawable != null) {
                // Decoration
                mCurrentDayDrawable.setBounds(
                        (int) (mDayCells[i].left + mBetweenSiblingsPadding),
                        (int) (mDayCells[i].top + topOffset),
                        (int) (mDayCells[i].left + mBetweenSiblingsPadding + mDecorationSize),
                        (int) (mDayCells[i].top + mDecorationSize + topOffset));
                mCurrentDayDrawable.draw(canvas);

                drawDayTextsInCell(canvas, i, mDayCells, mCurrentDayTextColor, mActiveTextColor, mDayMetadata);
            } else {
                drawDayTextsInCell(canvas, i, mDayCells, mActiveTextColor, mActiveTextColor, mDayMetadata);
            }
        }

        drawOverflow(canvas, mCellsWithOverflow, mDayCells);

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

    public DayMetadata getCellFromLocation(float x, float y) {
        for (int i = 0; i < mDayCells.length; i++) {
            if (mDayCells[i].contains(x, y)) {
                return mDayMetadata[i];
            }
        }
        return null;
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        if (mDaySelectionListener != null) {
            DayMetadata currentDay = getCellFromLocation(e.getX(), e.getY());
            if (currentDay != null) {
                mDaySelectionListener.onTapEnded(this, currentDay);
                return true;
            }
        }
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {
        if (mDaySelectionListener != null) {
            DayMetadata currentDay = getCellFromLocation(e.getX(), e.getY());
            if (currentDay != null) {
                mDaySelectionListener.onLongClick(this, currentDay);
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
        myOwnState.mDay = mDay;
        myOwnState.mCurrentCell = mCurrentCell;
        myOwnState.mSelectedCell = mSelectedCell;
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

        setDateInternal(myOwnState.mDay);
        mCurrentCell = myOwnState.mCurrentCell;
        mSelectedCell = myOwnState.mSelectedCell;
        mLastKnownWidth = myOwnState.mLastKnownWidth;
        mLastKnownHeight = myOwnState.mLastKnownHeight;
        recalculateCells(mLastKnownWidth, mLastKnownHeight, mDayCells, ROWS);
    }

    private static class MyOwnState extends BaseSavedState {
        int mCurrentCell;
        int mSelectedCell;
        DayMetadata mDay;
        int mLastKnownWidth;
        int mLastKnownHeight;

        MyOwnState(Parcelable superState) {
            super(superState);
        }

        MyOwnState(Parcel in) {
            super(in);
            int day = in.readInt();
            int month = in.readInt();
            int year = in.readInt();
            mDay = new DayMetadata(year, month, day);
            mCurrentCell = in.readInt();
            mSelectedCell = in.readInt();
            mLastKnownWidth = in.readInt();
            mLastKnownHeight = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(mDay.getDay());
            out.writeInt(mDay.getMonth());
            out.writeInt(mDay.getYear());
            out.writeInt(mCurrentCell);
            out.writeInt(mSelectedCell);
            out.writeInt(mLastKnownWidth);
            out.writeInt(mLastKnownHeight);
        }

        public static final Creator<MyOwnState> CREATOR =
            new Creator<WeekView.MyOwnState>() {
                public WeekView.MyOwnState createFromParcel(Parcel in) {
                    return new WeekView.MyOwnState(in);
                }
                public WeekView.MyOwnState[] newArray(int size) {
                    return new WeekView.MyOwnState[size];
                }
            };
    }

    // Other
    @Override
    public String toString() {
        return mDay.getYear() + "-" + mDay.getMonth() + "-" + mDay.getDay();
    }

}
