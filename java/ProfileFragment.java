package com.example.iptvplayer;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Intent;
import android.net.Uri;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class ProfileFragment extends Fragment {

    private MaterialButton addXtreamButton;
    private MaterialButton updateListButton;
    private MaterialButton openRepositoryButton;
    private MaterialButton openTelegramButton;
    private MaterialButton copyIptvListButton;

    public static final String ACTION_REFRESH_DATA = "com.example.iptvplayer.ACTION_REFRESH_DATA";


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        addXtreamButton = view.findViewById(R.id.add_xtream_button);
        addXtreamButton.setOnClickListener(v -> showXtreamInputDialog());

        updateListButton = view.findViewById(R.id.button_update_list);
        updateListButton.setOnClickListener(v -> handleUpdateList());

        openRepositoryButton = view.findViewById(R.id.button_open_repository);
        openRepositoryButton.setOnClickListener(v -> handleOpenRepository());

        openTelegramButton = view.findViewById(R.id.button_open_telegram);
        openTelegramButton.setOnClickListener(v -> handleOpenTelegram());

        copyIptvListButton = view.findViewById(R.id.button_copy_iptv_list);
        copyIptvListButton.setOnClickListener(v -> handleCopyIptvList());

        return view;
    }

    private void handleUpdateList() {
        Toast.makeText(getContext(), getString(R.string.profile_updating_list), Toast.LENGTH_SHORT).show();

        // Clear VOD cache
        MovieCacheManager.clearCache(requireContext());

        // Clear VOD cache (já estava aqui, mas DataManager.clearAllData() também limpará outros caches)
        // MovieCacheManager.clearCache(requireContext()); // Comentado pois clearAllData deve abranger isso

        // Obter instância do DataManager
        DataManager dataManager = MyApplication.getDataManager(requireContext().getApplicationContext());

        // Limpar todos os dados e cache no DataManager
        dataManager.clearAllData();

        // Toast.makeText(getContext(), getString(R.string.profile_updating_list), Toast.LENGTH_SHORT).show(); // Movido para antes da navegação

        // Navegar para DownloadProgressActivity para forçar a atualização completa
        Intent intent = new Intent(getActivity(), DownloadProgressActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Limpa a pilha de atividades
        startActivity(intent);
        if (getActivity() != null) {
            getActivity().finish(); // Finaliza a MainActivity (ou a atividade que contém este fragmento)
        }
        // Não mostrar Toast de sucesso aqui, pois a tela de progresso dará o feedback
    }

    private void handleOpenRepository() {
        Toast.makeText(getContext(), getString(R.string.profile_opening_repository), Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/DEYVIDYT/MyBrasilTv"));
        if (intent.resolveActivity(requireActivity().getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Toast.makeText(getContext(), "Nenhum aplicativo encontrado para abrir o link.", Toast.LENGTH_LONG).show();
        }
    }

    private void handleOpenTelegram() {
        String telegramUrl = "https://t.me/mybrasiltv";
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(telegramUrl));
        if (intent.resolveActivity(requireActivity().getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Toast.makeText(getContext(), "Nenhum aplicativo encontrado para abrir o link do Telegram.", Toast.LENGTH_LONG).show();
        }
    }

    private void handleCopyIptvList() {
        String iptvListUrl = "http://mybrasiltv.x10.mx/stream.php";
        ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("IPTV List URL", iptvListUrl);
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
            Toast.makeText(getContext(), getString(R.string.profile_iptv_list_copied), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "Erro ao copiar para a área de transferência.", Toast.LENGTH_SHORT).show();
        }
    }


    private void showXtreamInputDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        builder.setTitle(R.string.xtream_dialog_title);

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_xtream_input, null);
        builder.setView(dialogView);

        TextInputEditText dialogXtreamUrlEditText = dialogView.findViewById(R.id.dialog_xtream_url_edit_text);

        builder.setPositiveButton(R.string.send_button_text, (dialog, which) -> {
            String xtreamUrl = dialogXtreamUrlEditText.getText().toString();
            if (xtreamUrl.isEmpty()) {
                Toast.makeText(getContext(), R.string.xtream_url_empty_error, Toast.LENGTH_SHORT).show();
                return;
            }
            sendXtreamData(xtreamUrl);
        });
        builder.setNegativeButton(R.string.cancel_button_text, (dialog, which) -> dialog.dismiss());

        builder.show();
    }

    private void sendXtreamData(String urlString) {
        new Thread(() -> {
            try {
                // Extract components from the URL
                Pattern pattern = Pattern.compile("^(https?://[^/]+)/get\\.php\\?username=([^&]+)&password=([^&]+)&type=m3u_plus&output=ts$");
                Matcher matcher = pattern.matcher(urlString);

                if (!matcher.matches()) {
                    showToast("URL inválida. Certifique-se de que está no formato correto.");
                    return;
                }

                String server = matcher.group(1);
                String username = matcher.group(2);
                String password = matcher.group(3);

                // Create JSON object
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("id", UUID.randomUUID().toString().replace("-", ""));
                jsonObject.put("server", server);
                jsonObject.put("username", username);
                jsonObject.put("password", password);
                jsonObject.put("added_at", new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()));
                jsonObject.put("last_validated", new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()));

                JSONArray jsonArray = new JSONArray();
                jsonArray.put(jsonObject);

                // Send data to the PHP server
                URL url = new URL("http://mybrasiltv.x10.mx/data.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; utf-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonArray.toString().getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    showToast("Lista Xtream Codes enviada com sucesso!");
                } else {
                    showToast("Erro ao enviar lista: " + responseCode);
                }

            } catch (Exception e) {
                e.printStackTrace();
                showToast("Erro: " + e.getMessage());
            }
        }).start();
    }

    private void showToast(String message) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show());
        }
    }
}
