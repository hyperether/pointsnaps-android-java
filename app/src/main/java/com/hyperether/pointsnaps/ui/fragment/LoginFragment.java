package com.hyperether.pointsnaps.ui.fragment;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;

import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.DialogFragment;

import com.hyperether.pointsnaps.R;
import com.hyperether.pointsnaps.databinding.FragmentLoginBinding;
import com.hyperether.pointsnaps.manager.FragmentHandler;
import com.hyperether.pointsnaps.ui.activity.MainActivity;
import com.hyperether.pointsnaps.utils.Constants;
import com.hyperether.pointsnapssdk.repository.SharedPref;
import com.hyperether.pointsnapssdk.repository.api.ApiResponse;
import com.hyperether.pointsnapssdk.repository.api.Repository;
import com.hyperether.pointsnapssdk.repository.api.request.LoginRequest;
import com.hyperether.pointsnapssdk.repository.api.response.LoginResponse;

/**
 * Fragment for log in
 *
 * @author Slobodan Prijic
 * @version 1.0 - 07/21/2015
 */
public class LoginFragment extends ToolbarFragment {

    public static final String TAG = Constants.LOGIN_FRAGMENT_TAG;
    private FragmentLoginBinding binding;

    public static LoginFragment newInstance() {
        return new LoginFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.FullScreenDialogTheme);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_login, container, false);
        View view = binding.getRoot();
        getDialog().getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
        binding.email.requestFocus();

        binding.buttonLoginOk.setOnClickListener(buttonLoginOkListener);
        binding.register.setOnClickListener(buttonRegisterListener);

        return view;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new Dialog(getActivity(), getTheme()) {
            @Override
            public void onBackPressed() {
                getActivity().finish();
            }
        };
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.action_login).setVisible(false);
        super.onPrepareOptionsMenu(menu);
    }

    private View.OnClickListener buttonLoginOkListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String email = binding.email.getText().toString();
            String password = binding.passText.getText().toString();
            if (!password.isEmpty() && !email.isEmpty()) {
                Repository.getInstance().login(new LoginRequest(email, password), new ApiResponse() {
                    @Override
                    public void onSuccess(Object response) {
                        LoginResponse loginResponse = (LoginResponse) response;
                        SharedPref.saveToken(loginResponse.getToken());
                        SharedPref.saveRefreshToken(loginResponse.getRefreshToken());

                        if (isAdded() && getActivity() != null) {
                            getActivity().runOnUiThread(() ->
                                    FragmentHandler.getInstance((MainActivity) getActivity()).closeLoginDialog());
                        }
                    }

                    @Override
                    public void onError(String message) {
                        if (message != null && !message.isEmpty())
                            alertDialog(getString(R.string.error), message);
                        else
                            alertDialog(getString(R.string.error), getString(R.string.login_failed));
                    }
                });
            } else {
                alertDialog(getString(R.string.error), getString(R.string.empty_fields));
            }
        }
    };

    private View.OnClickListener buttonRegisterListener = view ->
            FragmentHandler.getInstance((MainActivity) getActivity()).openRegisterDialog();
}