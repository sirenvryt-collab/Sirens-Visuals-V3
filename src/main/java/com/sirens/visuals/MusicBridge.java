package com.sirens.visuals;

import com.google.gson.*;
import com.sirens.visuals.cfg.Mods;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Local bridge between a browser userscript (YouTube Music) and the game.
 * The userscript POSTs player state to 127.0.0.1:8765; the mod also serves the (token-stamped) script itself at
 * /sirens.user.js so installing it is a single click in Tampermonkey.
 */
public class MusicBridge {
    public record Line(double t, String s) { }
    public static final int PORT = 8765;
    /** Token baked into the original userscript, so scripts you already installed keep working. */
    static final String LEGACY_TOKEN = "36a795c46374b9531a6d9d18256524b0";
    public static volatile String id = "", title = "", artist = "", source = "";
    public static volatile double duration, position;
    public static volatile boolean playing, ad;
    public static volatile long recv = 0;
    public static volatile List<Line> lines = List.of();
    public static volatile String lyricState = "";
    public static volatile String bindError = "";
    public static final Queue<String> cmds = new ConcurrentLinkedQueue<>();
    static volatile String lastKey = "";
    static final Pattern LRC = Pattern.compile("^\\[(\\d+):(\\d+(?:\\.\\d+)?)\\](.*)$");

    public static boolean isActive() { return recv != 0 && (System.nanoTime() - recv) < 5_000_000_000L; }
    public static double elapsed() { return position + (playing && !ad ? (System.nanoTime() - recv) / 1e9 : 0); }
    public static double pos() { return elapsed() + Mods.LYR_OFFSET.f(); }

    public static String token() {
        String t = Mods.MU_TOKEN.get();
        if (t == null || t.length() < 16) {
            t = UUID.randomUUID().toString().replace("-", "");
            Mods.MU_TOKEN.set(t);
        }
        return t;
    }

    public static String scriptUrl() { return "http://127.0.0.1:" + PORT + "/sirens.user.js"; }

    public static String status() {
        if (!bindError.isEmpty()) return "Bridge error: " + bindError;
        if (!Mods.MUSIC.on()) return "Bridge is off.";
        if (!isActive()) return "Waiting for the browser userscript...";
        if (ad) return "Connected - ad playing (muted).";
        return "Connected - " + (title.isEmpty() ? "nothing playing" : title)
                + (Mods.MU_LYR.on() ? " | lyrics: " + (lyricState.isEmpty() ? "-" : lyricState) : "");
    }

    public static void start() {
        token();
        Thread t = new Thread(MusicBridge::loop, "sirens-music");
        t.setDaemon(true);
        t.start();
    }

    static void loop() {
        try (ServerSocket ss = new ServerSocket(PORT, 20, InetAddress.getLoopbackAddress())) {
            while (true) {
                Socket s = ss.accept();
                try { s.setSoTimeout(3000); handle(s); } catch (Exception ignored) { } finally { try { s.close(); } catch (Exception ignored) { } }
            }
        } catch (Exception e) {
            bindError = "port " + PORT + " unavailable (" + e.getMessage() + ")";
            System.err.println("[Sirens Visuals] music bridge could not start: " + e);
        }
    }

    static String readLine(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder(); int c;
        while ((c = in.read()) != -1) { if (c == '\n') break; if (c != '\r') sb.append((char) c); }
        return (c == -1 && sb.length() == 0) ? null : sb.toString();
    }

    static String script() {
        try (InputStream in = MusicBridge.class.getResourceAsStream("/assets/sirensvisuals/ytm-bridge.user.js")) {
            if (in == null) return "// script missing";
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).replace("__TOKEN__", token()).replace("__PORT__", String.valueOf(PORT));
        } catch (Exception e) { return "// " + e; }
    }

    static void respond(Socket s, int code, String type, String body) throws IOException {
        byte[] rb = body.getBytes(StandardCharsets.UTF_8);
        OutputStream out = s.getOutputStream();
        out.write(("HTTP/1.1 " + code + " OK\r\nContent-Type: " + type + "; charset=utf-8\r\nContent-Length: " + rb.length
                + "\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        out.write(rb);
        out.flush();
    }

    static void handle(Socket s) throws Exception {
        InputStream in = new BufferedInputStream(s.getInputStream());
        String req = readLine(in);
        if (req == null) return;
        int len = 0; String tok = null, host = "", l;
        while ((l = readLine(in)) != null && !l.isEmpty()) {
            int c = l.indexOf(':');
            if (c < 0) continue;
            String k = l.substring(0, c).trim().toLowerCase(), v = l.substring(c + 1).trim();
            if (k.equals("content-length")) len = Integer.parseInt(v);
            else if (k.equals("x-ds-token")) tok = v;
            else if (k.equals("host")) host = v.toLowerCase();
        }
        boolean hostOk = host.startsWith("127.0.0.1") || host.startsWith("localhost");
        if (!hostOk) { respond(s, 403, "text/plain", "forbidden"); return; }
        if (req.startsWith("GET /sirens.user.js")) { respond(s, 200, "text/javascript", script()); return; }
        if (req.startsWith("GET")) { respond(s, 200, "text/plain", "Sirens Visuals bridge is running. Install the userscript from /sirens.user.js"); return; }
        byte[] body = len > 0 ? in.readNBytes(Math.min(len, 1 << 20)) : new byte[0];
        if (!Mods.MUSIC.on() || tok == null || !(tok.equals(token()) || tok.equals(LEGACY_TOKEN))) { respond(s, 403, "application/json", "{}"); return; }
        try { update(new String(body, StandardCharsets.UTF_8)); } catch (Exception ignored) { }
        JsonObject r = new JsonObject();
        r.addProperty("muteAds", Mods.MU_MUTE.on());
        JsonArray ca = new JsonArray();
        String c; while ((c = cmds.poll()) != null) ca.add(c);
        r.add("cmds", ca);
        respond(s, 200, "application/json", r.toString());
    }

    static String str(JsonObject o, String k) { return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : ""; }
    static double num(JsonObject o, String k) { return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsDouble() : 0; }
    static boolean bool(JsonObject o, String k) { return o.has(k) && !o.get(k).isJsonNull() && o.get(k).getAsBoolean(); }

    static void update(String body) {
        JsonObject o = JsonParser.parseString(body).getAsJsonObject();
        boolean isAd = bool(o, "ad");
        String nt = str(o, "title"), na = str(o, "artist"), ni = str(o, "id");
        source = str(o, "source");
        duration = num(o, "duration"); position = num(o, "position"); playing = bool(o, "playing"); ad = isAd;
        recv = System.nanoTime();
        if (!isAd && !nt.isEmpty()) {
            title = nt; artist = na; id = ni;
            String key = ni + "|" + nt;
            if (!key.equals(lastKey)) {
                lastKey = key; lines = List.of(); lyricState = "";
                if (Mods.MU_LYR.on()) fetch(key, nt, na, duration);
            }
        }
    }

    static String clean(String s) {
        return s.replaceAll("(?i)\\s*[\\(\\[][^\\)\\]]*(official|video|lyrics|audio|visualizer|hd|4k|mv)[^\\)\\]]*[\\)\\]]", "")
                .replaceAll("(?i)\\s*-\\s*topic$", "").trim();
    }

    static String synced(JsonElement e) {
        if (e == null || !e.isJsonObject()) return null;
        JsonObject o = e.getAsJsonObject();
        return o.has("syncedLyrics") && !o.get("syncedLyrics").isJsonNull() ? o.get("syncedLyrics").getAsString() : null;
    }

    static void fetch(String key, String t, String a, double dur) {
        lyricState = "searching";
        CompletableFuture.runAsync(() -> {
            try {
                String ct = clean(t), ca = clean(a), res = null;
                List<String[]> cands = new ArrayList<>();
                cands.add(new String[]{ca, ct});
                if (t.contains(" - ")) { String[] sp = t.split(" - ", 2); cands.add(new String[]{clean(sp[0]), clean(sp[1])}); }
                for (String[] c : cands) {
                    String b = Net.text("https://lrclib.net/api/get?track_name=" + Net.enc(c[1]) + "&artist_name=" + Net.enc(c[0])
                            + (dur > 0 ? "&duration=" + Math.round(dur) : ""));
                    if (b != null) { res = synced(JsonParser.parseString(b)); if (res != null) break; }
                }
                if (res == null) {
                    String b = Net.text("https://lrclib.net/api/search?q=" + Net.enc((ca + " " + ct).trim()));
                    if (b != null) {
                        JsonElement e = JsonParser.parseString(b);
                        if (e.isJsonArray()) for (JsonElement x : e.getAsJsonArray()) { res = synced(x); if (res != null) break; }
                    }
                }
                if (!key.equals(lastKey)) return;
                if (res == null) { lyricState = "not found"; return; }
                List<Line> out = new ArrayList<>();
                for (String ln : res.split("\n")) {
                    Matcher m = LRC.matcher(ln.trim());
                    if (m.matches()) out.add(new Line(Integer.parseInt(m.group(1)) * 60 + Double.parseDouble(m.group(2)), m.group(3).trim()));
                }
                if (key.equals(lastKey)) { lines = out; lyricState = out.size() + " lines"; }
            } catch (Exception e) { lyricState = "error"; }
        });
    }
}
