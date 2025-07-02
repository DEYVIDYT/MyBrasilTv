package com.example.iptvplayer.parser;

import com.example.iptvplayer.data.Channel;
import com.example.iptvplayer.data.Movie;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class M3uParser {

    private static final String EXTINF_TAG = "#EXTINF:";
    private static final Pattern TVG_LOGO_PATTERN = Pattern.compile("tvg-logo=\"([^\"]*)\"");
    private static final Pattern GROUP_TITLE_PATTERN = Pattern.compile("group-title=\"([^\"]*)\"");
    private static final Pattern TYPE_PATTERN = Pattern.compile("type=\"([^\"]*)\""); // Adicionado para identificar o tipo

    public static List<Channel> parse(BufferedReader reader) throws IOException {
        List<Channel> channels = new ArrayList<>();
        String line;
        String channelName = null;
        String channelLogo = null;
        String groupTitle = null;

        while ((line = reader.readLine()) != null) {
            if (line.startsWith(EXTINF_TAG)) {
                Matcher logoMatcher = TVG_LOGO_PATTERN.matcher(line);
                channelLogo = logoMatcher.find() ? logoMatcher.group(1) : null;

                Matcher groupMatcher = GROUP_TITLE_PATTERN.matcher(line);
                groupTitle = groupMatcher.find() ? groupMatcher.group(1) : null;

                channelName = line.substring(line.lastIndexOf(",") + 1).trim();
            } else if (!line.startsWith("#") && channelName != null) {
                String channelUrl = line.trim();
                channels.add(new Channel(channelName, channelUrl, channelLogo, groupTitle));
                channelName = null;
            }
        }
        return channels;
    }

    public static List<Movie> parseMovies(BufferedReader reader) throws IOException {
        List<Movie> movies = new ArrayList<>();
        String line;
        String movieName = null;
        String moviePoster = null;
        String groupTitle = null;
        String type = null; // Variável para armazenar o tipo (movie, series, channel)

        while ((line = reader.readLine()) != null) {
            if (line.startsWith(EXTINF_TAG)) {
                Matcher logoMatcher = TVG_LOGO_PATTERN.matcher(line);
                moviePoster = logoMatcher.find() ? logoMatcher.group(1) : null;

                Matcher groupMatcher = GROUP_TITLE_PATTERN.matcher(line);
                groupTitle = groupMatcher.find() ? groupMatcher.group(1) : null;

                Matcher typeMatcher = TYPE_PATTERN.matcher(line); // Busca pelo tipo
                type = typeMatcher.find() ? typeMatcher.group(1) : null;

                movieName = line.substring(line.lastIndexOf(",") + 1).trim();
            } else if (!line.startsWith("#") && movieName != null) {
                String movieUrl = line.trim();
                // Adiciona à lista de filmes apenas se o tipo for 'movie' ou 'series' (ou se não houver tipo especificado, para compatibilidade)
                if (type == null || type.equalsIgnoreCase("movie") || type.equalsIgnoreCase("series")) {
                    movies.add(new Movie(movieName, moviePoster, movieUrl, groupTitle));
                }
                movieName = null;
                type = null; // Resetar o tipo para a próxima entrada
            }
        }
        return movies;
    }
}


