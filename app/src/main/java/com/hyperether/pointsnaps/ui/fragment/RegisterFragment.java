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
import com.hyperether.pointsnaps.databinding.FragmentRegisterBinding;
import com.hyperether.pointsnaps.manager.FragmentHandler;
import com.hyperether.pointsnapssdk.repository.SharedPref;
import com.hyperether.pointsnapssdk.repository.api.ApiResponse;
import com.hyperether.pointsnapssdk.repository.api.Repository;
import com.hyperether.pointsnapssdk.repository.api.request.LoginRequest;
import com.hyperether.pointsnapssdk.repository.api.request.RegisterRequest;
import com.hyperether.pointsnapssdk.repository.api.response.LoginResponse;
import com.hyperether.pointsnaps.ui.activity.MainActivity;
import com.hyperether.pointsnaps.utils.Constants;

public class RegisterFragment extends ToolbarFragment {

    public static final String TAG = Constants.REGISTER_FRAGMENT_TAG;

    private FragmentRegisterBinding binding;

    public static RegisterFragment newInstance() {
        return new RegisterFragment();
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
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_register, container, false);
        View view = binding.getRoot();
        setupToolbarTitle(view, getString(R.string.sign_up));
        getDialog().getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
        binding.usernamereg.requestFocus();

        binding.regButton.setOnClickListener(registerListener);
        binding.login.setOnClickListener(buttonLoginListener);

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

    private View.OnClickListener registerListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            String strUsername = binding.usernamereg.getText().toString();
            String strFirstName = binding.firstNameReg.getText().toString();
            String strLastName = binding.lastNameReg.getText().toString();
            String strEmail = binding.emailreg.getText().toString();
            String strPassword = binding.passwordreg.getText().toString();

            if (strUsername.equals("") || strFirstName.equals("") || strLastName.equals("")
                    || strEmail.equals("") || strPassword.equals("")) {
                alertDialog(getString(R.string.error), getString(R.string.empty_fields));
            } else {
                Repository.getInstance().register(new RegisterRequest(strUsername, strFirstName,
                        strLastName, strEmail, strPassword), new ApiResponse() {
                    @Override
                    public void onSuccess(Object response) {
                        Repository.getInstance().login(new LoginRequest(strUsername,
                                strPassword), new ApiResponse() {
                            @Override
                            public void onSuccess(Object response) {
                                LoginResponse loginResponse = (LoginResponse) response;
                                SharedPref.saveToken(loginResponse.getToken());
                                SharedPref.saveRefreshToken(loginResponse.getRefreshToken());
                                SharedPref.saveUsername(loginResponse.getUser().getUsername());
                                SharedPref.saveUserId(loginResponse.getUser().get_id());

                                if (isAdded() && getActivity() != null) {
                                    getActivity().runOnUiThread(() ->
                                            FragmentHandler.getInstance((MainActivity) getActivity()).closeRegisterDialog());
                                }
                            }

                            @Override
                            public void onError(String message) {
                                if (isAdded() && getActivity() != null) {
                                    getActivity().runOnUiThread(() ->
                                            FragmentHandler.getInstance((MainActivity) getActivity()).openLoginDialog());
                                }
                            }
                        });
                    }

                    @Override
                    public void onError(String message) {
                        alertDialog(getString(R.string.error), getString(R.string.register_failed));
                    }
                });
            }
        }
    };

    private View.OnClickListener buttonLoginListener = view ->
            FragmentHandler.getInstance((MainActivity) getActivity()).openLoginDialog();
}
