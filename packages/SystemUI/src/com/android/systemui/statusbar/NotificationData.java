/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.statusbar;

import android.app.Notification;
import android.os.SystemClock;
import android.service.notification.NotificationListenerService;
import android.service.notification.NotificationListenerService.Ranking;
import android.service.notification.NotificationListenerService.RankingMap;
import android.service.notification.StatusBarNotification;
import android.util.ArrayMap;
import android.view.View;

import com.android.systemui.statusbar.phone.NotificationGroupManager;
import com.android.systemui.statusbar.policy.HeadsUpManager;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * The list of currently displaying notifications.
 */
public class NotificationData {

    private final Environment mEnvironment;
    private HeadsUpManager mHeadsUpManager;

    public static final class Entry {
        private static final long LAUNCH_COOLDOWN = 2000;
        private static final long NOT_LAUNCHED_YET = -LAUNCH_COOLDOWN;
        public String key;
        public StatusBarNotification notification;
        public StatusBarIconView icon;
        public ExpandableNotificationRow row; // the outer expanded view
        private boolean interruption;
        public boolean autoRedacted; // whether the redacted notification was generated by us
        public boolean legacy; // whether the notification has a legacy, dark background
        public int targetSdk;
        private long lastFullScreenIntentLaunchTime = NOT_LAUNCHED_YET;

        public Entry(StatusBarNotification n, StatusBarIconView ic) {
            this.key = n.getKey();
            this.notification = n;
            this.icon = ic;
        }

        public void setInterruption() {
            interruption = true;
        }

        public boolean hasInterrupted() {
            return interruption;
        }

        /**
         * Resets the notification entry to be re-used.
         */
        public void reset() {
            // NOTE: Icon needs to be preserved for now.
            // We should fix this at some point.
            autoRedacted = false;
            legacy = false;
            lastFullScreenIntentLaunchTime = NOT_LAUNCHED_YET;
            if (row != null) {
                row.reset();
            }
        }

        public View getContentView() {
            return row.getPrivateLayout().getContractedChild();
        }

        public View getExpandedContentView() {
            return row.getPrivateLayout().getExpandedChild();
        }

        public View getHeadsUpContentView() {
            return row.getPrivateLayout().getHeadsUpChild();
        }

        public View getPublicContentView() {
            return row.getPublicLayout().getContractedChild();
        }

        public void notifyFullScreenIntentLaunched() {
            lastFullScreenIntentLaunchTime = SystemClock.elapsedRealtime();
        }

        public boolean hasJustLaunchedFullScreenIntent() {
            return SystemClock.elapsedRealtime() < lastFullScreenIntentLaunchTime + LAUNCH_COOLDOWN;
        }
    }

    private final ArrayMap<String, Entry> mEntries = new ArrayMap<>();
    private final ArrayList<Entry> mSortedAndFiltered = new ArrayList<>();

    private NotificationGroupManager mGroupManager;

    private RankingMap mRankingMap;
    private final Ranking mTmpRanking = new Ranking();

    public void setHeadsUpManager(HeadsUpManager headsUpManager) {
        mHeadsUpManager = headsUpManager;
    }

    private final Comparator<Entry> mRankingComparator = new Comparator<Entry>() {
        private final Ranking mRankingA = new Ranking();
        private final Ranking mRankingB = new Ranking();

        @Override
        public int compare(Entry a, Entry b) {
            final StatusBarNotification na = a.notification;
            final StatusBarNotification nb = b.notification;
            final int aPriority = na.getNotification().priority;
            final int bPriority = nb.getNotification().priority;

            String mediaNotification = mEnvironment.getCurrentMediaNotificationKey();

            // PRIORITY_MIN media streams are allowed to drift to the bottom
            final boolean aMedia = a.key.equals(mediaNotification)
                    && aPriority > Notification.PRIORITY_MIN;
            final boolean bMedia = b.key.equals(mediaNotification)
                    && bPriority > Notification.PRIORITY_MIN;

            boolean aSystemMax = aPriority >= Notification.PRIORITY_MAX &&
                    isSystemNotification(na);
            boolean bSystemMax = bPriority >= Notification.PRIORITY_MAX &&
                    isSystemNotification(nb);
            int d = nb.getScore() - na.getScore();

            boolean isHeadsUp = a.row.isHeadsUp();
            if (isHeadsUp != b.row.isHeadsUp()) {
                return isHeadsUp ? -1 : 1;
            } else if (isHeadsUp) {
                // Provide consistent ranking with headsUpManager
                return mHeadsUpManager.compare(a, b);
            } else if (aMedia != bMedia) {
                // Upsort current media notification.
                return aMedia ? -1 : 1;
            } else if (aSystemMax != bSystemMax) {
                // Upsort PRIORITY_MAX system notifications
                return aSystemMax ? -1 : 1;
            } else if (mRankingMap != null) {
                // RankingMap as received from NoMan
                mRankingMap.getRanking(a.key, mRankingA);
                mRankingMap.getRanking(b.key, mRankingB);
                return mRankingA.getRank() - mRankingB.getRank();
            } if (d != 0) {
                return d;
            } else {
                return (int) (nb.getNotification().when - na.getNotification().when);
            }
        }
    };

    public NotificationData(Environment environment) {
        mEnvironment = environment;
        mGroupManager = environment.getGroupManager();
    }

    /**
     * Returns the sorted list of active notifications (depending on {@link Environment}
     *
     * <p>
     * This call doesn't update the list of active notifications. Call {@link #filterAndSort()}
     * when the environment changes.
     * <p>
     * Don't hold on to or modify the returned list.
     */
    public ArrayList<Entry> getActiveNotifications() {
        return mSortedAndFiltered;
    }

    public Entry get(String key) {
        return mEntries.get(key);
    }

    public void add(Entry entry, RankingMap ranking) {
        mEntries.put(entry.notification.getKey(), entry);
        updateRankingAndSort(ranking);
        mGroupManager.onEntryAdded(entry);
    }

    public Entry remove(String key, RankingMap ranking) {
        Entry removed = mEntries.remove(key);
        if (removed == null) return null;
        updateRankingAndSort(ranking);
        mGroupManager.onEntryRemoved(removed);
        return removed;
    }

    public void updateRanking(RankingMap ranking) {
        updateRankingAndSort(ranking);
    }

    public boolean isAmbient(String key) {
        if (mRankingMap != null) {
            mRankingMap.getRanking(key, mTmpRanking);
            return mTmpRanking.isAmbient();
        }
        return false;
    }

    public int getVisibilityOverride(String key) {
        if (mRankingMap != null) {
            mRankingMap.getRanking(key, mTmpRanking);
            return mTmpRanking.getVisibilityOverride();
        }
        return NotificationListenerService.Ranking.VISIBILITY_NO_OVERRIDE;
    }

    private void updateRankingAndSort(RankingMap ranking) {
        if (ranking != null) {
            mRankingMap = ranking;
        }
        filterAndSort();
    }

    // TODO: This should not be public. Instead the Environment should notify this class when
    // anything changed, and this class should call back the UI so it updates itself.
    public void filterAndSort() {
        mSortedAndFiltered.clear();

        final int N = mEntries.size();
        for (int i = 0; i < N; i++) {
            Entry entry = mEntries.valueAt(i);
            StatusBarNotification sbn = entry.notification;

            if (shouldFilterOut(sbn)) {
                continue;
            }

            mSortedAndFiltered.add(entry);
        }

        Collections.sort(mSortedAndFiltered, mRankingComparator);
    }

    boolean shouldFilterOut(StatusBarNotification sbn) {
        if (!(mEnvironment.isDeviceProvisioned() ||
                showNotificationEvenIfUnprovisioned(sbn))) {
            return true;
        }

        if (!mEnvironment.isNotificationForCurrentProfiles(sbn)) {
            return true;
        }

        if (sbn.getNotification().visibility == Notification.VISIBILITY_SECRET &&
                mEnvironment.shouldHideSensitiveContents(sbn.getUserId())) {
            return true;
        }

        if (!BaseStatusBar.ENABLE_CHILD_NOTIFICATIONS
                && mGroupManager.isChildInGroupWithSummary(sbn)) {
            return true;
        }
        return false;
    }

    /**
     * Return whether there are any visible notifications (i.e. without an error).
     */
    public boolean hasActiveVisibleNotifications() {
        for (Entry e : mSortedAndFiltered) {
            if (e.expanded != null) { // the view successfully inflated
                return true;
            }
        }
        return false;
    }

    /**
     * Return whether there are any clearable notifications (that aren't errors).
     */
    public boolean hasActiveClearableNotifications() {
        for (Entry e : mSortedAndFiltered) {
            if (e.getContentView() != null) { // the view successfully inflated
                if (e.notification.isClearable()) {
                    return true;
                }
            }
        }
        return false;
    }

    // Q: What kinds of notifications should show during setup?
    // A: Almost none! Only things coming from the system (package is "android") that also
    // have special "kind" tags marking them as relevant for setup (see below).
    public static boolean showNotificationEvenIfUnprovisioned(StatusBarNotification sbn) {
        return "android".equals(sbn.getPackageName())
                && sbn.getNotification().extras.getBoolean(Notification.EXTRA_ALLOW_DURING_SETUP);
    }

    public void dump(PrintWriter pw, String indent) {
        int N = mSortedAndFiltered.size();
        pw.print(indent);
        pw.println("active notifications: " + N);
        int active;
        for (active = 0; active < N; active++) {
            NotificationData.Entry e = mSortedAndFiltered.get(active);
            dumpEntry(pw, indent, active, e);
        }

        int M = mEntries.size();
        pw.print(indent);
        pw.println("inactive notifications: " + (M - active));
        int inactiveCount = 0;
        for (int i = 0; i < M; i++) {
            Entry entry = mEntries.valueAt(i);
            if (!mSortedAndFiltered.contains(entry)) {
                dumpEntry(pw, indent, inactiveCount, entry);
                inactiveCount++;
            }
        }
    }

    private void dumpEntry(PrintWriter pw, String indent, int i, Entry e) {
        pw.print(indent);
        pw.println("  [" + i + "] key=" + e.key + " icon=" + e.icon);
        StatusBarNotification n = e.notification;
        pw.print(indent);
        pw.println("      pkg=" + n.getPackageName() + " id=" + n.getId() + " score=" +
                n.getScore());
        pw.print(indent);
        pw.println("      notification=" + n.getNotification());
        pw.print(indent);
        pw.println("      tickerText=\"" + n.getNotification().tickerText + "\"");
    }

    private static boolean isSystemNotification(StatusBarNotification sbn) {
        String sbnPackage = sbn.getPackageName();
        return "android".equals(sbnPackage) || "com.android.systemui".equals(sbnPackage);
    }

    /**
     * Provides access to keyguard state and user settings dependent data.
     */
    public interface Environment {
        public boolean shouldHideSensitiveContents(int userid);
        public boolean isDeviceProvisioned();
        public boolean isNotificationForCurrentProfiles(StatusBarNotification sbn);
        public String getCurrentMediaNotificationKey();
        public NotificationGroupManager getGroupManager();
    }
}
