package com.example.iptvplayer;

import android.util.Log;
import android.content.Context;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import com.example.iptvplayer.data.EpgProgram;
import com.example.iptvplayer.data.Channel;

public class XmltvEpgService {
    private static final String XMLTV_TAG = "XMLTV_EPG_DEBUG";
    
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private String baseUrl;
    private String username;
    private String password;
    private CacheManager cacheManager;
    private Map<String, String> channelIdMap = new HashMap<>(); // Mapeia stream_id para channel_id do XMLTV
    
    public XmltvEpgService(String baseUrl, String username, String password) {
        this.baseUrl = baseUrl;
        this.username = username;
        this.password = password;
    }
    
    public void setCacheManager(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }
    
    public interface XmltvEpgCallback {
        void onSuccess(Map<String, String> currentPrograms); // streamId -> programa atual
        void onFailure(String error);
    }
    
    public interface ChannelUpdateCallback {
        void onChannelUpdated(String streamId, String currentProgram);
    }
    
    /**
     * Busca EPG XMLTV e retorna a programação atual para todos os canais
     */
    public void fetchCurrentPrograms(List<Channel> allChannels, XmltvEpgCallback callback) {
        executor.execute(() -> {
            Log.d(XMLTV_TAG, "fetchCurrentPrograms - Starting XMLTV EPG fetch");
            try {
                String xmltvUrl = generateXmltvUrl();
                Log.d(XMLTV_TAG, "XMLTV URL: " + xmltvUrl.replace(password, "******"));
                
                URL url = new URL(xmltvUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(60000);
                
                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String inputLine;

                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();
                    
                    Log.d(XMLTV_TAG, "XMLTV Response length: " + response.length());
                    Log.d(XMLTV_TAG, "XMLTV Raw Response (first 500 chars): " + response.toString().substring(0, Math.min(response.toString().length(), 500)));
                    
                    Map<String, String> currentPrograms = parseXmltvForCurrentPrograms(response.toString(), allChannels);
                    Log.i(XMLTV_TAG, "Successfully parsed current programs for " + currentPrograms.size() + " channels");
                    
                    callback.onSuccess(currentPrograms);
                    
                } else {
                    Log.e(XMLTV_TAG, "Failed to fetch XMLTV. HTTP error code: " + responseCode);
                    callback.onFailure("Failed to fetch XMLTV EPG. HTTP error code: " + responseCode);
                }
            } catch (Exception e) {
                Log.e(XMLTV_TAG, "Error fetching XMLTV EPG: ", e);
                callback.onFailure("Error: " + e.getMessage());
            }
        });
    }
    
    /**
     * Atualiza continuamente a programação atual dos canais
     */
    public void startContinuousUpdate(List<Channel> channels, ChannelUpdateCallback callback) {
        executor.execute(() -> {
            while (true) {
                try {
                    fetchCurrentPrograms(channels, new XmltvEpgCallback() {
                        @Override
                        public void onSuccess(Map<String, String> currentPrograms) {
                            for (Channel channel : channels) {
                                String streamId = channel.getStreamId();
                                if (streamId != null && currentPrograms.containsKey(streamId)) {
                                    String currentProgram = currentPrograms.get(streamId);
                                    if (currentProgram != null && !currentProgram.equals(channel.getCurrentProgramTitle())) {
                                        channel.setCurrentProgramTitle(currentProgram);
                                        callback.onChannelUpdated(streamId, currentProgram);
                                    }
                                }
                            }
                        }
                        
                        @Override
                        public void onFailure(String error) {
                            Log.e(XMLTV_TAG, "Continuous update failed: " + error);
                        }
                    });
                    
                    // Aguarda 5 minutos antes da próxima atualização
                    Thread.sleep(5 * 60 * 1000);
                    
                } catch (InterruptedException e) {
                    Log.d(XMLTV_TAG, "Continuous update interrupted");
                    break;
                } catch (Exception e) {
                    Log.e(XMLTV_TAG, "Error in continuous update: ", e);
                    try {
                        Thread.sleep(60 * 1000); // Aguarda 1 minuto em caso de erro
                    } catch (InterruptedException ie) {
                        break;
                    }
                }
            }
        });
    }
    
    /**
     * Gera URL XMLTV
     */
    private String generateXmltvUrl() {
        return String.format("%s/xmltv.php?username=%s&password=%s", baseUrl, username, password);
    }
    
    /**
     * Processa XML XMLTV e extrai programação atual
     */
    private Map<String, String> parseXmltvForCurrentPrograms(String xmlContent, List<Channel> allChannels) {
        Map<String, String> currentPrograms = new HashMap<>();
        
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(xmlContent.getBytes("UTF-8")));
            
            // Criar um mapa de streamId para Channel para busca rápida
            Map<String, Channel> appChannelsMap = new HashMap<>();
            if (allChannels != null) {
                for (Channel ch : allChannels) {
                    if (ch.getStreamId() != null) {
                        appChannelsMap.put(ch.getStreamId(), ch);
                    }
                }
            }

            // Primeiro, mapear canais do XMLTV para seus display-names
            NodeList channelNodes = doc.getElementsByTagName("channel");
            Map<String, String> xmltvChannelIdToDisplayName = new HashMap<>();
            for (int i = 0; i < channelNodes.getLength(); i++) {
                Element channel = (Element) channelNodes.item(i);
                String channelId = channel.getAttribute("id");
                NodeList displayNames = channel.getElementsByTagName("display-name");
                if (displayNames.getLength() > 0) {
                    xmltvChannelIdToDisplayName.put(channelId, displayNames.item(0).getTextContent());
                }
            }
            Log.d(XMLTV_TAG, "Parsed " + xmltvChannelIdToDisplayName.size() + " XMLTV channel display names.");

            // Agora processar programas
            NodeList programNodes = doc.getElementsByTagName("programme");
            Log.d(XMLTV_TAG, "Parsing " + programNodes.getLength() + " programme nodes.");
            long currentTime = System.currentTimeMillis() / 1000; // Tempo atual em segundos

            SimpleDateFormat xmltvFormat = new SimpleDateFormat("yyyyMMddHHmmss'Z'", Locale.US);
            xmltvFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

            for (int i = 0; i < programNodes.getLength(); i++) {
                Element programme = (Element) programNodes.item(i);
                String xmltvChannelId = programme.getAttribute("channel");
                String startTime = programme.getAttribute("start");
                String stopTime = programme.getAttribute("stop");

                try {
                    // Converter tempo XMLTV para timestamp
                    Date startDate = xmltvFormat.parse(startTime);
                    Date stopDate = xmltvFormat.parse(stopTime);

                    long startTimestamp = startDate.getTime() / 1000;
                    long stopTimestamp = stopDate.getTime() / 1000;
                    Log.d(XMLTV_TAG, "Program: " + xmltvChannelId + ", Start: " + startTimestamp + ", Stop: " + stopTimestamp + ", Current: " + currentTime);

                    // Verificar se o programa está sendo exibido agora
                    if (currentTime >= startTimestamp && currentTime < stopTimestamp) {
                        Log.d(XMLTV_TAG, "Program is current: " + xmltvChannelId);
                        NodeList titleNodes = programme.getElementsByTagName("title");
                        if (titleNodes.getLength() > 0) {
                            String title = titleNodes.item(0).getTextContent();
                            Log.d(XMLTV_TAG, "Found title: " + title + " for XMLTV channel: " + xmltvChannelId);

                            // Tentar mapear o XMLTV channel ID para um Channel do aplicativo
                            Channel targetChannel = null;

                            // 1. Tentar pelo streamId (assumindo que o XMLTV channel ID pode ser o streamId)
                            if (appChannelsMap.containsKey(xmltvChannelId)) {
                                targetChannel = appChannelsMap.get(xmltvChannelId);
                                Log.d(XMLTV_TAG, "Matched by streamId: " + xmltvChannelId);
                            } else {
                                // 2. Tentar pelo display-name do XMLTV (se disponível)
                                String xmltvDisplayName = xmltvChannelIdToDisplayName.get(xmltvChannelId);
                                if (xmltvDisplayName != null) {
                                    for (Channel ch : allChannels) {
                                        if (ch.getName() != null && ch.getName().equalsIgnoreCase(xmltvDisplayName)) {
                                            targetChannel = ch;
                                            Log.d(XMLTV_TAG, "Matched by display-name: " + xmltvDisplayName + " to streamId: " + ch.getStreamId());
                                            break;
                                        }
                                    }
                                }
                            }

                            if (targetChannel != null) {
                                currentPrograms.put(targetChannel.getStreamId(), title);
                                Log.d(XMLTV_TAG, "Current program for app channel " + targetChannel.getName() + " (streamId: " + targetChannel.getStreamId() + "): " + title);
                            } else {
                                Log.w(XMLTV_TAG, "Could not find matching app channel for XMLTV channel ID: " + xmltvChannelId + " (display-name: " + xmltvChannelIdToDisplayName.get(xmltvChannelId) + ")");
                            }
                        }
                    }

                } catch (Exception e) {
                    Log.w(XMLTV_TAG, "Error parsing programme time for XMLTV channel " + xmltvChannelId, e);
                }
            }

        } catch (Exception e) {
            Log.e(XMLTV_TAG, "Error parsing XMLTV content", e);
        }

        return currentPrograms;
    }

    

    /**
     * Busca EPG completo para um canal específico
     */
    public void fetchChannelEpg(String channelId, EpgService.EpgCallback callback) {
        executor.execute(() -> {
            Log.d(XMLTV_TAG, "fetchChannelEpg for channel: " + channelId);
            try {
                String xmltvUrl = generateXmltvUrl();

                URL url = new URL(xmltvUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(60000);

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String inputLine;

                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();

                    List<EpgProgram> programs = parseXmltvForChannel(response.toString(), channelId);
                    Log.i(XMLTV_TAG, "Successfully parsed " + programs.size() + " programs for channel " + channelId);

                    callback.onSuccess(programs);

                } else {
                    Log.e(XMLTV_TAG, "Failed to fetch XMLTV for channel. HTTP error code: " + responseCode);
                    callback.onFailure("Failed to fetch XMLTV EPG. HTTP error code: " + responseCode);
                }
            } catch (Exception e) {
                Log.e(XMLTV_TAG, "Error fetching channel EPG: ", e);
                callback.onFailure("Error: " + e.getMessage());
            }
        });
    }

    /**
     * Processa XML XMLTV para um canal específico
     */
    private List<EpgProgram> parseXmltvForChannel(String xmlContent, String channelId) {
        List<EpgProgram> programs = new ArrayList<>();

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(xmlContent.getBytes("UTF-8")));

            NodeList programNodes = doc.getElementsByTagName("programme");
            SimpleDateFormat xmltvFormat = new SimpleDateFormat("yyyyMMddHHmmss'Z'", Locale.US);
            xmltvFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

            for (int i = 0; i < programNodes.getLength(); i++) {
                Element programme = (Element) programNodes.item(i);
                String progChannelId = programme.getAttribute("channel");

                if (channelId.equals(progChannelId)) {
                    String startTime = programme.getAttribute("start");
                    String stopTime = programme.getAttribute("stop");

                    try {
                        Date startDate = xmltvFormat.parse(startTime);
                        Date stopDate = xmltvFormat.parse(stopTime);

                        String startTimestamp = String.valueOf(startDate.getTime() / 1000);
                        String stopTimestamp = String.valueOf(stopDate.getTime() / 1000);

                        NodeList titleNodes = programme.getElementsByTagName("title");
                        String title = titleNodes.getLength() > 0 ? titleNodes.item(0).getTextContent() : "Sem título";

                        NodeList descNodes = programme.getElementsByTagName("desc");
                        String description = descNodes.getLength() > 0 ? descNodes.item(0).getTextContent() : "";

                        String streamId = mapChannelIdToStreamId(channelId);
                        EpgProgram program = new EpgProgram(
                            String.valueOf(i),
                            title,
                            description,
                            startTimestamp,
                            stopTimestamp,
                            streamId
                        );

                        programs.add(program);

                    } catch (Exception e) {
                        Log.w(XMLTV_TAG, "Error parsing programme for channel " + channelId, e);
                    }
                }
            }

        } catch (Exception e) {
            Log.e(XMLTV_TAG, "Error parsing XMLTV for channel " + channelId, e);
        }

        return programs;
    }

    private String mapChannelIdToStreamId(String xmltvChannelId) {
        return this.channelIdMap.get(xmltvChannelId);
    }
}

