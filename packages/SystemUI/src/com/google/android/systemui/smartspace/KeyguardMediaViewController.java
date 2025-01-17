package com.google.android.systemui.smartspace;

import android.app.smartspace.SmartspaceAction;
import android.app.smartspace.SmartspaceTarget;
import android.content.ComponentName;
import android.content.Context;
import android.media.MediaMetadata;
import android.os.UserHandle;
import android.os.Handler;
import android.os.HandlerExecutor;
import android.text.TextUtils;
import android.view.View;

import com.android.systemui.R;
import com.android.systemui.broadcast.BroadcastDispatcher;
import com.android.systemui.dagger.SysUISingleton;
import com.android.systemui.plugins.BcSmartspaceDataPlugin;
import com.android.systemui.settings.UserTracker;
import com.android.systemui.statusbar.NotificationMediaManager;
import com.android.systemui.util.concurrency.DelayableExecutor;

import javax.inject.Inject;

import kotlin.Unit;
import kotlin.jvm.internal.Intrinsics;

/* compiled from: KeyguardMediaViewController.kt */
@SysUISingleton
public final class KeyguardMediaViewController {
    private CharSequence artist;
    private final BroadcastDispatcher broadcastDispatcher;
    private final Context context;
    private final ComponentName mediaComponent;
    private final NotificationMediaManager.MediaListener mediaListener =
            new NotificationMediaManager.MediaListener() {
                @Override
                public void onPrimaryMetadataOrStateChanged(
                        final MediaMetadata mediaMetadata, final int i) {
                    DelayableExecutor uiExecutor = getUiExecutor();
                    final KeyguardMediaViewController keyguardMediaViewController =
                            KeyguardMediaViewController.this;
                    uiExecutor.execute(
                            new Runnable() {
                                @Override
                                public final void run() {
                                    updateMediaInfo(mediaMetadata, i);
                                }
                            });
                }
            };
    private final NotificationMediaManager mediaManager;
    private final BcSmartspaceDataPlugin plugin;
    private BcSmartspaceDataPlugin.SmartspaceView smartspaceView;
    private CharSequence title;
    private final DelayableExecutor uiExecutor;

    private final Handler mMainHandler;
    private final UserTracker mUserTracker;

    private final UserTracker.Callback mUserChangedCallback =
            new UserTracker.Callback() {
                @Override
                public void onUserChanged(int newUser, Context userContext) {
                   if (userContext != null) {
                    reset();
                   }
                }
            };

    @Inject
    public KeyguardMediaViewController(
            Context context,
            BcSmartspaceDataPlugin plugin,
            DelayableExecutor uiExecutor,
            NotificationMediaManager mediaManager,
            BroadcastDispatcher broadcastDispatcher,
            UserTracker userTracker,
            Handler mainHandler) {
        Intrinsics.checkNotNullParameter(context, "context");
        Intrinsics.checkNotNullParameter(plugin, "plugin");
        Intrinsics.checkNotNullParameter(uiExecutor, "uiExecutor");
        Intrinsics.checkNotNullParameter(mediaManager, "mediaManager");
        Intrinsics.checkNotNullParameter(broadcastDispatcher, "broadcastDispatcher");
        Intrinsics.checkNotNullParameter(userTracker, "userTracker");
        Intrinsics.checkNotNullParameter(mainHandler, "mainHandler");
        this.context = context;
        this.plugin = plugin;
        this.uiExecutor = uiExecutor;
        this.mediaManager = mediaManager;
        this.broadcastDispatcher = broadcastDispatcher;
        this.mUserTracker = userTracker;
        this.mMainHandler = mainHandler;
        mediaComponent = new ComponentName(context, KeyguardMediaViewController.class);
    }

    public final DelayableExecutor getUiExecutor() {
        return uiExecutor;
    }

    public final BcSmartspaceDataPlugin.SmartspaceView getSmartspaceView() {
        return smartspaceView;
    }

    public final void setSmartspaceView(BcSmartspaceDataPlugin.SmartspaceView smartspaceView) {
        this.smartspaceView = smartspaceView;
    }

    public final void init() {
        plugin.addOnAttachStateChangeListener(
                new View.OnAttachStateChangeListener() {
                    @Override
                    public void onViewAttachedToWindow(View v) {
                        NotificationMediaManager notificationMediaManager;
                        NotificationMediaManager.MediaListener keyguardMediaViewController;
                        Intrinsics.checkNotNullParameter(v, "v");
                        setSmartspaceView((BcSmartspaceDataPlugin.SmartspaceView) v);
                        notificationMediaManager = mediaManager;
                        keyguardMediaViewController = mediaListener;
                        notificationMediaManager.addCallback(keyguardMediaViewController);
                    }

                    @Override
                    public void onViewDetachedFromWindow(View v) {
                        NotificationMediaManager notificationMediaManager;
                        NotificationMediaManager.MediaListener keyguardMediaViewController;
                        Intrinsics.checkNotNullParameter(v, "v");
                        setSmartspaceView(null);
                        notificationMediaManager = mediaManager;
                        keyguardMediaViewController = mediaListener;
                        notificationMediaManager.removeCallback(keyguardMediaViewController);
                    }
                });
        mUserTracker.addCallback(mUserChangedCallback, new HandlerExecutor(mMainHandler));
    }

    public final void updateMediaInfo(MediaMetadata mediaMetadata, int i) {
        CharSequence charSequence;
        if (!NotificationMediaManager.isPlayingState(i)) {
            reset();
            return;
        }
        Unit unit = null;
        if (mediaMetadata == null) {
            charSequence = null;
        } else {
            charSequence = mediaMetadata.getText("android.media.metadata.TITLE");
            if (TextUtils.isEmpty(charSequence)) {
                charSequence = context.getResources().getString(R.string.music_controls_no_title);
            }
        }
        CharSequence text =
                mediaMetadata == null
                        ? null
                        : mediaMetadata.getText("android.media.metadata.ARTIST");
        if (TextUtils.equals(title, charSequence) && TextUtils.equals(artist, text)) {
            return;
        }
        title = charSequence;
        artist = text;
        if (charSequence != null) {
            SmartspaceAction build =
                    new SmartspaceAction.Builder("deviceMediaTitle", charSequence.toString())
                            .setSubtitle(artist)
                            .setIcon(mediaManager.getMediaIcon())
                            .build();
            SmartspaceTarget build2 =
                    new SmartspaceTarget.Builder(
                                    "deviceMedia",
                                    mediaComponent,
                                    UserHandle.of(mUserTracker.getUserId()))
                            .setFeatureType(41)
                            .setHeaderAction(build)
                            .build();
            BcSmartspaceDataPlugin.SmartspaceView smartspaceView = getSmartspaceView();
            if (smartspaceView != null) {
                smartspaceView.setMediaTarget(build2);
                unit = Unit.INSTANCE;
            }
        }
        if (unit != null) {
            return;
        }
        reset();
    }

    public final void reset() {
        title = null;
        artist = null;
        BcSmartspaceDataPlugin.SmartspaceView smartspaceView = getSmartspaceView();
        if (smartspaceView == null) {
            return;
        }
        smartspaceView.setMediaTarget(null);
    }
}
