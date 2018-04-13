/*#######################################################
 *
 *   Maintained by Gregor Santner, 2017-
 *   https://gsantner.net/
 *
 *   License: Apache 2.0
 *  https://github.com/gsantner/opoc/#licensing
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
/*
 * Add dependencies:
    implementation "com.android.support:preference-v7:${version_library_appcompat}"
    implementation "com.android.support:preference-v14:${version_library_appcompat}"

 * Apply to activity using setTheme(), add to styles.xml/theme:
        <item name="preferenceTheme">@style/PreferenceThemeOverlay.v14.Material</item>
 * OR
    <style name="AppTheme" ...
        <item name="preferenceTheme">@style/AppTheme.PreferenceTheme</item>
    </style>
    <style name="AppTheme.PreferenceTheme" parent="PreferenceThemeOverlay.v14.Material">
      <item name="preferenceCategoryStyle">@style/AppTheme.PreferenceTheme.CategoryStyle</item>
    </style>
    <style name="AppTheme.PreferenceTheme.CategoryStyle" parent="Preference.Category">
        <item name="android:layout">@layout/pref_category_text</item>
    </style>

 * Layout file:
    <?xml version="1.0" encoding="utf-8"?>
    <TextView xmlns:android="http://schemas.android.com/apk/res/android"
        android:id="@android:id/title"
        style="?android:attr/listSeparatorTextViewStyle"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:textAllCaps="false"
        android:textColor="@color/colorAccent" />


 */
package net.gsantner.opoc.preference;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.annotation.XmlRes;
import android.support.v4.app.Fragment;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import android.view.View;

import net.gsantner.opoc.util.Callback;
import net.gsantner.opoc.util.ContextUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Baseclass to use as preference fragment (with support libraries)
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public abstract class GsPreferenceFragmentCompat extends PreferenceFragmentCompat
        implements SharedPreferences.OnSharedPreferenceChangeListener, PreferenceFragmentCompat.OnPreferenceStartScreenCallback {
    private static final int DEFAULT_ICON_TINT_DELAY = 200;

    //
    // Abstract
    //

    @XmlRes
    public abstract int getPreferenceResourceForInflation();

    public abstract String getFragmentTag();

    protected abstract SharedPreferencesPropertyBackend getAppSettings(Context context);

    //
    // Virtual
    //

    public Boolean onPreferenceClicked(Preference preference) {
        return null;
    }

    public String getSharedPreferencesName() {
        return "app";
    }

    protected void afterOnCreate(Bundle savedInstances, Context context) {

    }

    public void doUpdatePreferences() {

    }

    protected void onPreferenceScreenChanged(PreferenceFragmentCompat preferenceFragmentCompat, PreferenceScreen preferenceScreen) {

    }

    public Integer getIconTintColor() {
        return null;
    }

    public String getTitle() {
        return null;
    }

    //
    //
    //

    private final Set<String> _registeredPrefs = new HashSet<>();
    private final List<PreferenceScreen> _prefScreenBackstack = new ArrayList<>();
    private SharedPreferencesPropertyBackend _asb;
    protected ContextUtils _cu;

    @Override
    @Deprecated
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        _asb = getAppSettings(getActivity());
        _cu = new ContextUtils(getActivity());
        getPreferenceManager().setSharedPreferencesName(getSharedPreferencesName());
        addPreferencesFromResource(getPreferenceResourceForInflation());
        afterOnCreate(savedInstanceState, getActivity());
    }

    public final Callback.a1<PreferenceFragmentCompat> updatePreferenceIcons = (frag) -> {
        try {
            View view = frag.getView();
            final Integer color = getIconTintColor();
            if (view != null && color != null) {
                Runnable r = () -> tintAllPrefIcons(frag, color);
                for (long delayFactor : new int[]{1, 10, 50, 100, 500}) {
                    view.postDelayed(r, delayFactor * DEFAULT_ICON_TINT_DELAY);
                }
            }
        } catch (Exception ignored) {
        }
    };

    public void tintAllPrefIcons(PreferenceFragmentCompat preferenceFragment, @ColorInt int iconColor) {
        tintPrefIconsRecursive(getPreferenceScreen(), iconColor);
    }

    private void tintPrefIconsRecursive(PreferenceGroup prefGroup, @ColorInt int iconColor) {
        if (prefGroup != null) {
            int prefCount = prefGroup.getPreferenceCount();
            for (int i = 0; i < prefCount; i++) {
                Preference pref = prefGroup.getPreference(i);
                if (pref != null) {
                    pref.setIcon(_cu.tintDrawable(pref.getIcon(), iconColor));
                    if (pref instanceof PreferenceGroup) {
                        tintPrefIconsRecursive((PreferenceGroup) pref, iconColor);
                    }
                }
            }
        }
    }


    @Override
    public void onViewCreated(@NonNull final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        updatePreferenceIcons.callback(this);
    }

    private synchronized void updatePreferenceChangedListeners(boolean shouldListen) {
        String tprefname = getSharedPreferencesName();
        if (shouldListen && tprefname != null && !_registeredPrefs.contains(tprefname)) {
            SharedPreferences preferences = _asb.getContext().getSharedPreferences(tprefname, Context.MODE_PRIVATE);
            _asb.registerPreferenceChangedListener(preferences, this);
            _registeredPrefs.add(tprefname);
        } else if (!shouldListen) {
            for (String prefname : _registeredPrefs) {
                SharedPreferences preferences = _asb.getContext().getSharedPreferences(tprefname, Context.MODE_PRIVATE);
                _asb.unregisterPreferenceChangedListener(preferences, this);
            }
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        updatePreferenceChangedListeners(true);
        doUpdatePreferences(); // Invoked later
        onPreferenceScreenChangedPriv(this, getPreferenceScreen());
    }

    @Override
    public void onPause() {
        super.onPause();
        updatePreferenceChangedListeners(false);
    }

    @Override
    @Deprecated
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (isAdded()) {
            onPreferenceChanged(sharedPreferences, key);
            doUpdatePreferences();
        }
    }

    protected void onPreferenceChanged(SharedPreferences prefs, String key) {
        // Wait some ms to be sure the pref objects have changed it's internal values
        // and the new values are ready to be read ;)
        Runnable r = this::doUpdatePreferences;
        if (getView() != null) {
            getView().postDelayed(r, 350);
        } else {
            r.run();
        }
    }

    @Override
    @Deprecated
    public boolean onPreferenceTreeClick(Preference preference) {
        if (isAdded() && preference.hasKey()) {
            Boolean ret = onPreferenceClicked(preference);
            if (ret != null) {
                return ret;
            }
        }
        return super.onPreferenceTreeClick(preference);
    }


    @Override
    public Fragment getCallbackFragment() {
        return this;
    }

    @Override
    public void onStop() {
        _prefScreenBackstack.clear();
        super.onStop();
    }

    @Deprecated
    @Override
    public boolean onPreferenceStartScreen(PreferenceFragmentCompat preferenceFragmentCompat, PreferenceScreen preferenceScreen) {
        _prefScreenBackstack.add(getPreferenceScreen());
        preferenceFragmentCompat.setPreferenceScreen(preferenceScreen);
        updatePreferenceIcons.callback(this);
        onPreferenceScreenChangedPriv(preferenceFragmentCompat, preferenceScreen);
        return true;
    }

    protected void updateSummary(@StringRes int keyResId, String summary) {
        updatePreference(keyResId, 0, null, summary, null);
    }

    @Nullable
    @SuppressWarnings("SameParameterValue")
    protected Preference updatePreference(@StringRes int keyResId, @DrawableRes Integer iconRes, String title, String summary, Boolean visible) {
        Preference pref = findPreference(getString(keyResId));
        if (pref != null) {
            if (summary != null) {
                pref.setSummary(summary);
            }
            if (title != null) {
                pref.setTitle(title);
            }
            if (iconRes != null && iconRes != 0) {
                pref.setIcon(_cu.tintDrawable(iconRes, getIconTintColor()));
            }
            if (visible != null) {
                pref.setVisible(visible);
            }
        }
        return pref;
    }

    protected void removePreference(@Nullable Preference preference) {
        if (preference == null) {
            return;
        }
        PreferenceGroup parent = getPreferenceParent(getPreferenceScreen(), preference);
        if (parent == null) {
            return;
        }
        parent.removePreference(preference);
    }

    public boolean canGoBack() {
        return !_prefScreenBackstack.isEmpty();
    }

    public void goBack() {
        if (canGoBack()) {
            PreferenceScreen screen = _prefScreenBackstack.remove(_prefScreenBackstack.size() - 1);
            if (screen != null) {
                setPreferenceScreen(screen);
                onPreferenceScreenChangedPriv(this, screen);
            }
        }
    }

    protected PreferenceGroup getPreferenceParent(PreferenceGroup prefGroup, Preference pref) {
        for (int i = 0; i < prefGroup.getPreferenceCount(); ++i) {
            Preference prefChild = prefGroup.getPreference(i);
            if (prefChild == pref) {
                return prefGroup;
            }
            if (prefChild instanceof PreferenceGroup) {
                PreferenceGroup childGroup = (PreferenceGroup) prefChild;
                PreferenceGroup result = getPreferenceParent(childGroup, pref);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    private void onPreferenceScreenChangedPriv(PreferenceFragmentCompat preferenceFragmentCompat, PreferenceScreen preferenceScreen) {
        onPreferenceScreenChanged(preferenceFragmentCompat, preferenceScreen);
        updatePreferenceChangedListeners(true);
    }

    /**
     * Is key equal
     *
     * @param pref     A preference
     * @param resIdKey the resource id of the string
     * @return if equals
     */
    public boolean eq(@Nullable Preference pref, @StringRes int resIdKey) {
        return pref != null && getString(resIdKey).equals(pref.getKey());
    }


    /**
     * Is key equal
     *
     * @param key      the key
     * @param resIdKey the resource id of the string
     * @return if equals
     */
    public boolean eq(@Nullable String key, @StringRes int resIdKey) {
        return getString(resIdKey).equals(key);
    }

    public boolean hasTitle() {
        return !TextUtils.isEmpty(getTitle());
    }

    public String getTitleOrDefault(String defaultTitle) {
        return hasTitle() ? getTitle() : defaultTitle;
    }

    protected void restartActivity() {
        Activity activity;
        if (isAdded() && (activity = getActivity()) != null) {
            Intent intent = getActivity().getIntent();
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
            activity.overridePendingTransition(0, 0);
            activity.finish();
            activity.overridePendingTransition(0, 0);
            startActivity(intent);
        }
    }

    /**
     * Append a pref to given {@code target}. If target is null, the current screen is taken
     * The pref icon is tint according to color
     *
     * @param pref   Preference to add
     * @param target The target to add the pref to, or null for current screen
     * @return true if successfully added
     */
    protected boolean appendPreference(Preference pref, @Nullable PreferenceGroup target) {
        if (target == null) {
            if ((target = getPreferenceScreen()) == null) {
                return false;
            }
        }
        if (getIconTintColor() != null && pref.getIcon() != null) {
            pref.setIcon(_cu.tintDrawable(pref.getIcon(), getIconTintColor()));
        }
        return target.addPreference(pref);
    }
}
