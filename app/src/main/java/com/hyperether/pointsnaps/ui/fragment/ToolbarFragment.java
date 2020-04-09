package com.hyperether.pointsnaps.ui.fragment;

import android.app.AlertDialog;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.DialogFragment;

import com.hyperether.pointsnaps.R;

public abstract class ToolbarFragment extends DialogFragment {

    protected void setupToolbar(View view, String title) {
        ImageView imageButton = view.findViewById(R.id.toolbar_image_back);
        imageButton.setOnClickListener(v -> dismiss());

        TextView fragmentTitle = view.findViewById(R.id.toolbar_title);
        fragmentTitle.setText(title);
    }

    protected void setupToolbarTitle(View view, String title) {
        ImageView imageButton = view.findViewById(R.id.toolbar_image_back);
        imageButton.setVisibility(View.GONE);

        TextView fragmentTitle = view.findViewById(R.id.toolbar_title);
        fragmentTitle.setText(title);
    }

    protected void alertDialog(String title, String message) {
        if (isAdded() && getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                new AlertDialog.Builder(getActivity())
                        .setTitle(title)
                        .setMessage(message)
                        .setPositiveButton(R.string.ok, (dialog1, which) -> dialog1.dismiss())
                        .show();
            });
        }
    }
}

