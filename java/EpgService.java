package com.example.iptvplayer;

import android.util.Log;
import android.content.Context;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.example.iptvplayer.data.EpgProgram;
import android.util.Base64; // Importar Base64

public class EpgService {
    private static final String EPG_TAG = "EPG_DEBUG";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private String baseUrl;
    private String username;
    private String password;
    private CacheManager cacheManager;

    public EpgService(String baseUrl, String username, String password, Context context) {
        this.baseUrl = baseUrl;
        this.username = username;
        this.password = password;
        this.cacheManager = new CacheManager(context);
    }

    public interface EpgCallback {
        void onSuccess(List<EpgProgram> programs);
        void onFailure(String error);
    }

    /**
     * Busca EPG curto para um canal específico
     * @param streamId ID do canal
     * @param limit Limite de programas (padrão: 4)
     * @param callback Callback para resultado
     */
    public void fetchShortEpg(String streamId, int limit, EpgCallback callback) {
        executor.execute(() -> {
            // Primeiro, verifica se existe cache válido
            List<EpgProgram> cachedEpg = cacheManager.getCachedEpg(streamId);
            if (cachedEpg != null) {
                Log.d(EPG_TAG, "fetchShortEpg - Using cached data for stream " + streamId + ": " + cachedEpg.size() + " programs");
                callback.onSuccess(cachedEpg);
                return;
            }
            
            Log.d(EPG_TAG, "fetchShortEpg called for streamId: " + streamId + ", limit: " + limit);
            try {
                String apiUrl = String.format("%s/player_api.php?username=%s&password=%s&action=get_short_epg&stream_id=%s&limit=%d", 
                    baseUrl, username, password, streamId, limit);
                
                Log.d(EPG_TAG, "EPG API URL: " + apiUrl.replace(password, "******"));
                
                URL url = new URL(apiUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000); // 10 segundos
                conn.setReadTimeout(15000); // 15 segundos

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String inputLine;
                    StringBuilder response = new StringBuilder();

                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();

                    Log.d(EPG_TAG, "fetchShortEpg - Raw API Response: " + response.toString().substring(0, Math.min(response.toString().length(), 500)) + "...");
                    
                    List<EpgProgram> programs = parseShortEpgResponse(response.toString(), streamId);
                    
                    // Salva no cache
                    cacheManager.saveEpg(streamId, programs);
                    
                    Log.i(EPG_TAG, "fetchShortEpg - Successfully parsed " + programs.size() + " programs for stream " + streamId);
                    callback.onSuccess(programs);

                } else {
                    Log.e(EPG_TAG, "fetchShortEpg - Failed. HTTP error code: " + responseCode);
                    callback.onFailure("Failed to fetch EPG. HTTP error code: " + responseCode);
                }
            } catch (Exception e) {
                Log.e(EPG_TAG, "fetchShortEpg - Error: ", e);
                callback.onFailure("Error: " + e.getMessage());
            }
        });
    }

    /**
     * Busca EPG completo para um canal específico
     * @param streamId ID do canal
     * @param callback Callback para resultado
     */
    public void fetchFullEpg(String streamId, EpgCallback callback) {
        executor.execute(() -> {
            Log.d(EPG_TAG, "fetchFullEpg called for streamId: " + streamId);
            try {
                String apiUrl = String.format("%s/player_api.php?username=%s&password=%s&action=get_simple_date_table&stream_id=%s", 
                    baseUrl, username, password, streamId);
                
                Log.d(EPG_TAG, "Full EPG API URL: " + apiUrl.replace(password, "******"));
                
                URL url = new URL(apiUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(30000); // Mais tempo para EPG completo

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String inputLine;
                    StringBuilder response = new StringBuilder();

                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();

                    Log.d(EPG_TAG, "fetchFullEpg - Raw API Response length: " + response.toString().length());
                    
                    List<EpgProgram> programs = parseFullEpgResponse(response.toString(), streamId);
                    Log.i(EPG_TAG, "fetchFullEpg - Successfully parsed " + programs.size() + " programs for stream " + streamId);
                    callback.onSuccess(programs);

                } else {
                    Log.e(EPG_TAG, "fetchFullEpg - Failed. HTTP error code: " + responseCode);
                    callback.onFailure("Failed to fetch full EPG. HTTP error code: " + responseCode);
                }
            } catch (Exception e) {
                Log.e(EPG_TAG, "fetchFullEpg - Error: ", e);
                callback.onFailure("Error: " + e.getMessage());
            }
        });
    }

    /**
     * Busca EPG XML completo para todos os canais usando URL dinâmica XMLTV
     * @param callback Callback para resultado
     */
    public void fetchXmlEpg(EpgCallback callback) {
        executor.execute(() -> {
            Log.d(EPG_TAG, "fetchXmlEpg called");
            try {
                // Gera URL dinâmica XMLTV
                String xmltvUrl = generateXmltvUrl();
                
                Log.d(EPG_TAG, "XML EPG XMLTV URL: " + xmltvUrl.replace(password, "******"));
                
                URL url = new URL(xmltvUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(60000); // Mais tempo para XML completo

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String inputLine;
                    StringBuilder response = new StringBuilder();

                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();

                    Log.d(EPG_TAG, "fetchXmlEpg - Raw XML Response length: " + response.toString().length());
                    
                    // Para XML EPG, seria necessário um parser XML específico
                    // Por enquanto, retornamos uma lista vazia
                    List<EpgProgram> programs = new ArrayList<>();
                    Log.i(EPG_TAG, "fetchXmlEpg - XML EPG received but parsing not implemented yet");
                    callback.onSuccess(programs);

                } else {
                    Log.e(EPG_TAG, "fetchXmlEpg - Failed. HTTP error code: " + responseCode);
                    callback.onFailure("Failed to fetch XML EPG. HTTP error code: " + responseCode);
                }
            } catch (Exception e) {
                Log.e(EPG_TAG, "fetchXmlEpg - Error: ", e);
                callback.onFailure("Error: " + e.getMessage());
            }
        });
    }
    
    /**
     * Gera URL dinâmica XMLTV baseada no baseUrl, username e password
     * @return URL XMLTV formatada
     */
    public String generateXmltvUrl() {
        return String.format("%s/xmltv.php?username=%s&password=%s", baseUrl, username, password);
    }
    
    /**
     * Retorna a URL XMLTV para uso externo (ex: configuração de players externos)
     * @return URL XMLTV formatada
     */
    public String getXmltvUrl() {
        return generateXmltvUrl();
    }

    /**
     * Parse da resposta do EPG curto
     */
    private List<EpgProgram> parseShortEpgResponse(String response, String streamId) {
        List<EpgProgram> programs = new ArrayList<>();
        try {
            JSONObject jsonResponse = new JSONObject(response);
            JSONArray epgListings = jsonResponse.optJSONArray("epg_listings");
            
            if (epgListings != null) {
                for (int i = 0; i < epgListings.length(); i++) {
                    JSONObject program = epgListings.getJSONObject(i);
                    
                    String id = program.optString("id", "");
                    String title = program.optString("title", "Sem título");
                    String description = program.optString("description", "");
                    String start = program.optString("start", "");
                    String stop = program.optString("stop", "");
                    
                    // Decodificar o título Base64 aqui
                    try {
                        byte[] data = Base64.decode(title, Base64.DEFAULT);
                        title = new String(data, "UTF-8");
                    } catch (IllegalArgumentException e) {
                        Log.e(EPG_TAG, "Base64 decoding error for EPG title: " + title, e);
                        // Manter o título original ou definir um placeholder
                    } catch (java.io.UnsupportedEncodingException e) {
                        Log.e(EPG_TAG, "UTF-8 encoding not supported.", e);
                    }

                    EpgProgram epgProgram = new EpgProgram(id, title, description, start, stop, streamId);
                    
                    // Campos opcionais
                    epgProgram.setCategory(program.optString("category", ""));
                    epgProgram.setLanguage(program.optString("lang", ""));
                    epgProgram.setRating(program.optString("rating", ""));
                    
                    programs.add(epgProgram);
                    Log.d(EPG_TAG, "Parsed program: " + title + " (" + start + " - " + stop + ")");
                }
            } else {
                Log.w(EPG_TAG, "No epg_listings found in response for stream " + streamId);
            }
        } catch (JSONException e) {
            Log.e(EPG_TAG, "Error parsing short EPG response", e);
        }
        return programs;
    }

    /**
     * Parse da resposta do EPG completo
     */
    private List<EpgProgram> parseFullEpgResponse(String response, String streamId) {
        List<EpgProgram> programs = new ArrayList<>();
        try {
            JSONObject jsonResponse = new JSONObject(response);
            
            // O formato pode variar dependendo da implementação do servidor Xtream
            // Geralmente é um objeto com datas como chaves
            java.util.Iterator<String> keys = jsonResponse.keys();
            while(keys.hasNext()) {
                String dateKey = keys.next();
                JSONArray dayPrograms = jsonResponse.optJSONArray(dateKey);
                if (dayPrograms != null) {
                    for (int i = 0; i < dayPrograms.length(); i++) {
                        JSONObject program = dayPrograms.getJSONObject(i);
                        
                        String id = program.optString("id", "");
                        String title = program.optString("title", "Sem título");
                        String description = program.optString("description", "");
                        String start = program.optString("start", "");
                        String stop = program.optString("stop", "");
                        
                        // Decodificar o título Base64 aqui
                        try {
                            byte[] data = Base64.decode(title, Base64.DEFAULT);
                            title = new String(data, "UTF-8");
                        } catch (IllegalArgumentException e) {
                            Log.e(EPG_TAG, "Base64 decoding error for EPG title: " + title, e);
                            // Manter o título original ou definir um placeholder
                        } catch (java.io.UnsupportedEncodingException e) {
                            Log.e(EPG_TAG, "UTF-8 encoding not supported.", e);
                        }

                        EpgProgram epgProgram = new EpgProgram(id, title, description, start, stop, streamId);
                        programs.add(epgProgram);
                    }
                }
            }
            
            Log.d(EPG_TAG, "Parsed " + programs.size() + " programs from full EPG for stream " + streamId);
        } catch (JSONException e) {
            Log.e(EPG_TAG, "Error parsing full EPG response", e);
        }
        return programs;
    }
}


