package com.hyperether.pointsnaps.manager;

import androidx.fragment.app.FragmentManager;

import com.hyperether.pointsnaps.ui.activity.MainActivity;
import com.hyperether.pointsnaps.ui.fragment.LocationFragment;
import com.hyperether.pointsnaps.ui.fragment.LoginFragment;
import com.hyperether.pointsnaps.ui.fragment.RegisterFragment;
import com.hyperether.pointsnaps.ui.fragment.WriteFragment;
import com.hyperether.toolbox.ui.HyperFragmentStackHandler;


/**
 * Class for fragment management
 *
 * @author Marko Katic
 * @version 1.0 - 07/04/2017
 */
public class FragmentHandler extends HyperFragmentStackHandler {

    private static FragmentHandler instance;
    private static final String TAG = "FragmentHandler";
    private FragmentManager mFragmentManager;
    private LoginFragment loginFragment;
    private RegisterFragment registerFragment;

    public static synchronized FragmentHandler getInstance(MainActivity activity) {
        if (instance == null) {
            synchronized (FragmentHandler.class) {
                if (instance == null) {
                    instance = new FragmentHandler(activity);
                }
            }
        }
        return instance;
    }

    /**
     * FragmentHandler constructor
     *
     * @param a activity where fragments will be attached
     */
    private FragmentHandler(MainActivity a) {
        super(a);
        this.mFragmentManager = a.getSupportFragmentManager();
    }

    public void openLoginDialog() {
        closeRegisterDialog();
        if (loginFragment == null
                || loginFragment.getDialog() == null
                || !loginFragment.getDialog().isShowing()
                || loginFragment.isRemoving()) {
            loginFragment = LoginFragment.newInstance();
            loginFragment.show(mFragmentManager, LoginFragment.TAG);
        }
    }

    public void closeLoginDialog() {
        if (loginFragment != null) {
            loginFragment.dismiss();
            loginFragment = null;
        }
    }

    public void openRegisterDialog() {
        closeLoginDialog();
        if (registerFragment == null
                || registerFragment.getDialog() == null
                || !registerFragment.getDialog().isShowing()
                || registerFragment.isRemoving()) {
            registerFragment = RegisterFragment.newInstance();
            registerFragment.show(mFragmentManager, RegisterFragment.TAG);
        }
    }

    public void closeRegisterDialog() {
        if (registerFragment != null) {
            registerFragment.dismiss();
            registerFragment = null;
        }
    }

    public void openWriteDialog() {
        WriteFragment writeFragment = WriteFragment.newInstance();
        writeFragment.show(mFragmentManager, WriteFragment.TAG);
    }

    public void openLocationDialog() {
        LocationFragment locationFragment = LocationFragment.newInstance();
        locationFragment.show(mFragmentManager, LocationFragment.TAG);
    }

    /**
     * Method for clear Singleton instance
     */
    public static void clear() {
        instance = null;
    }
}