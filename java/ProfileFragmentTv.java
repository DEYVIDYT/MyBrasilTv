package com.example.iptvplayer;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

public class ProfileFragmentTv extends Fragment {

    private static final String PROFILE_TV_TAG = "ProfileFragmentTv";
    private static final String PREFS_NAME = "AppPrefs"; // Mesmas prefs da MainActivity
    private static final String KEY_SELECTED_LAYOUT = "SelectedLayout";
    private static final String LAYOUT_MOBILE = "Mobile";

    private MaterialButton addXtreamButtonTv, updateListButtonTv, openRepoButtonTv, changeLayoutButtonTv, logoutButtonTv;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_profile_tv, container, false);

        addXtreamButtonTv = root.findViewById(R.id.add_xtream_button_tv);
        updateListButtonTv = root.findViewById(R.id.button_update_list_tv);
        openRepoButtonTv = root.findViewById(R.id.button_open_repository_tv);
        changeLayoutButtonTv = root.findViewById(R.id.button_change_layout_tv);
        logoutButtonTv = root.findViewById(R.id.profile_logout_button_tv);

        setupClickListeners();

        return root;
    }

    private void setupClickListeners() {
        addXtreamButtonTv.setOnClickListener(v -> showAddXtreamDialog());
        updateListButtonTv.setOnClickListener(v -> updateList());
        openRepoButtonTv.setOnClickListener(v -> openRepository());
        logoutButtonTv.setOnClickListener(v -> performLogout());
        changeLayoutButtonTv.setOnClickListener(v -> showChangeLayoutDialog());
    }

    private void showAddXtreamDialog() {
        if (getContext() == null) return;
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_xtream_input, null);
        builder.setView(dialogView);

        TextInputEditText inputUrl = dialogView.findViewById(R.id.xtream_url_input);
        Button sendButton = dialogView.findViewById(R.id.send_xtream_button);
        Button cancelButton = dialogView.findViewById(R.id.cancel_xtream_button);

        AlertDialog dialog = builder.create();

        sendButton.setOnClickListener(view -> {
            String url = inputUrl.getText() != null ? inputUrl.getText().toString().trim() : "";
            if (!url.isEmpty()) {
                Log.d(PROFILE_TV_TAG, "Xtream URL to add: " + url);
                // TODO: Implementar lógica para salvar/processar a URL Xtream
                Toast.makeText(getContext(), "URL Xtream: " + url + " (implementar salvamento)", Toast.LENGTH_LONG).show();
                DataManager dataManager = MyApplication.getDataManager(getContext());
                dataManager.saveXtreamCredentials(url, "", ""); // Assuming username/password are not needed or derived
                Toast.makeText(getContext(), "Lista Xtream adicionada. Atualize os dados.", Toast.LENGTH_LONG).show();
                dialog.dismiss();
            } else {
                Toast.makeText(getContext(), R.string.xtream_url_empty_error, Toast.LENGTH_SHORT).show();
            }
        });
        cancelButton.setOnClickListener(view -> dialog.dismiss());
        dialog.show();
    }

    private void updateList() {
        Log.d(PROFILE_TV_TAG, "Update List button clicked");
        Toast.makeText(getContext(), R.string.profile_updating_list, Toast.LENGTH_SHORT).show();
        // Enviar broadcast para MainActivity ou diretamente chamar DataManager
        Intent intent = new Intent(ProfileFragment.ACTION_REFRESH_DATA);
        LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(intent);
        // Adicionalmente, pode-se forçar o DataManager a recarregar
        MyApplication.getDataManager(requireContext()).forceReloadData();
        Toast.makeText(getContext(), R.string.profile_update_list_success, Toast.LENGTH_LONG).show();

    }

    private void openRepository() {
        Log.d(PROFILE_TV_TAG, "Open Repository button clicked");
        Toast.makeText(getContext(), R.string.profile_opening_repository, Toast.LENGTH_SHORT).show();
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/MyBrasilTv"));
        try {
            startActivity(browserIntent);
        } catch (Exception e) {
            Log.e(PROFILE_TV_TAG, "Error opening repository URL", e);
            Toast.makeText(getContext(), "Não foi possível abrir o link.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showChangeLayoutDialog() {
        if (getContext() == null) return;
        new MaterialAlertDialogBuilder(getContext())
            .setTitle("Mudar Layout")
            .setMessage("Você será redirecionado para o layout Mobile. O aplicativo será reiniciado para aplicar as alterações.")
            .setPositiveButton("Confirmar", (dialog, which) -> changeLayoutToMobile())
            .setNegativeButton("Cancelar", null)
            .show();
    }

    private void changeLayoutToMobile() {
        if (getContext() == null) return;
        SharedPreferences prefs = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_SELECTED_LAYOUT, LAYOUT_MOBILE);
        // editor.putBoolean(KEY_LAYOUT_CHOSEN, true); // Já deve estar true
        editor.apply();

        Log.d(PROFILE_TV_TAG, "Layout preference changed to Mobile. Restarting app...");
        Toast.makeText(getContext(), "Reiniciando para aplicar o layout Mobile...", Toast.LENGTH_LONG).show();

        // Reiniciar o aplicativo
        Intent i = getActivity().getBaseContext().getPackageManager()
            .getLaunchIntentForPackage(getActivity().getBaseContext().getPackageName());
        if (i != null) {
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            getActivity().finish();
        }
    }

    private void performLogout() {
        // TODO: Implementar lógica de logout (limpar SharedPreferences, etc.)
        Log.d(PROFILE_TV_TAG, "Logout button clicked - Implementar lógica");
        Toast.makeText(getContext(), "Logout (a ser implementado)", Toast.LENGTH_SHORT).show();
    }
}
