package com.hyperether.pointsnaps.ui.fragment;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProviders;

import com.hyperether.pointsnaps.R;
import com.hyperether.pointsnaps.databinding.FragmentWriteBinding;
import com.hyperether.pointsnaps.ui.UserViewModel;
import com.hyperether.pointsnaps.utils.Constants;

/**
 * Fragment for write description
 *
 * @author Slobodan Prijic
 * @version 1.0 - 07/21/2015
 */
public class WriteFragment extends ToolbarFragment {

    public static final String TAG = Constants.WRITE_FRAGMENT_TAG;
    private FragmentWriteBinding binding;
    private View view;
    Context context;

    //ROOM db
    private UserViewModel userViewModel;

    public static WriteFragment newInstance() {
        return new WriteFragment();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
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
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_write, container, false);
        view = binding.getRoot();
        setupToolbar(view, getString(R.string.description_text));
        getDialog().getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
        binding.write.requestFocus();
        getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        binding.buttonWriteOk.setOnClickListener(buttonOkListener);

        userViewModel = ViewModelProviders.of(getActivity()).get(UserViewModel.class);
        userViewModel.getActiveCollectionLiveData().observe(this, data -> {
            binding.setData(data);
            binding.write.setText(data.getCollectionData().getDescription());
        });
        return view;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.action_login).setVisible(false);
        super.onPrepareOptionsMenu(menu);
    }

    private View.OnClickListener buttonOkListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String str = binding.write.getText().toString();
            userViewModel.updateDescription(str);
            InputMethodManager imm =
                    (InputMethodManager) binding.write.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm.isActive())
                imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
            dismiss();
        }
    };
}
