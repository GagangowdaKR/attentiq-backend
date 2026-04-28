package com.attentiq.websocket;

import com.attentiq.dto.response.EventResponse;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DataListener;
import com.corundumstudio.socketio.listener.DisconnectListener;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SocketIOService {

    private final SocketIOServer server;

    private final Map<Long, UUID>              hostSockets        = new ConcurrentHashMap<>();
    private final Map<Long, Set<UUID>>         meetingSockets     = new ConcurrentHashMap<>();
    private final Map<UUID, UserSession>       sessionUsers       = new ConcurrentHashMap<>();
    private final Map<Long, Map<Long, UUID>>   meetingUserSessions= new ConcurrentHashMap<>();

    @Data static class UserSession  { Long meetingId; Long userId; String userName; String role; }
    @Data static class AnnouncePayload { String meetingId; Long userId; String userName; String role; }
    @Data static class MediaStatePayload { String meetingId; Long userId; boolean isAudioOn; boolean isVideoOn; }
    @Data static class ChatPayload  { String meetingId; String id; Long userId; String userName; String text; String timestamp; }
    @Data static class WebRtcOfferPayload { String meetingId; Long to; Long from; String sdp; }
    @Data static class WebRtcIcePayload   { String meetingId; Long to; Long from; Object candidate; }

    public SocketIOService(
            @Value("${socketio.host}") String host,
            @Value("${socketio.port}") int port) {

        Configuration config = new Configuration();
        config.setHostname(host);
        config.setPort(port);
        config.setOrigin("*");
        server = new SocketIOServer(config);

        server.addConnectListener(onConnect());
        server.addDisconnectListener(onDisconnect());

        // ── meeting:announce ──────────────────────────────────────────────────
        // This is the SINGLE entry point. After registering the user we:
        //   1. Broadcast participant_joined to everyone else
        //   2. Send the NEW user a webrtc:peer_list of everyone already in the room
        //      so WebRTC can start immediately — no separate webrtc:join needed.
        server.addEventListener("meeting:announce", AnnouncePayload.class,
                (DataListener<AnnouncePayload>) (client, data, ack) -> {
                    try {
                        Long mid = Long.parseLong(data.getMeetingId());

                        // ── Register this user ────────────────────────────────────
                        UserSession s = new UserSession();
                        s.setMeetingId(mid); s.setUserId(data.getUserId());
                        s.setUserName(data.getUserName()); s.setRole(data.getRole());
                        sessionUsers.put(client.getSessionId(), s);

                        Set<UUID> roomSessions = meetingSockets
                                .computeIfAbsent(mid, k -> ConcurrentHashMap.newKeySet());

                        Map<Long, UUID> userMap = meetingUserSessions
                                .computeIfAbsent(mid, k -> new ConcurrentHashMap<>());

                        // ── Collect existing peers BEFORE adding the new user ─────
                        List<Long> existingPeerIds = new ArrayList<>(userMap.keySet());
                        existingPeerIds.remove(data.getUserId()); // exclude self

                        // Now register the new user
                        roomSessions.add(client.getSessionId());
                        userMap.put(data.getUserId(), client.getSessionId());

                        if ("HOST".equalsIgnoreCase(data.getRole()))
                            hostSockets.put(mid, client.getSessionId());

                        // ── 1. Tell everyone else this user joined ────────────────
                        Map<String, Object> joinedPayload = new HashMap<>();
                        joinedPayload.put("userId",   data.getUserId());
                        joinedPayload.put("userName", data.getUserName());
                        joinedPayload.put("role",     data.getRole());
                        broadcastExcept(mid, client.getSessionId(), "meeting:participant_joined", joinedPayload);

                        // ── 3. Tell existing users to create WebRTC offer to newcomer ─
                        Map<String, Object> newPeerPayload = new HashMap<>();
                        newPeerPayload.put("userId", data.getUserId());
                        broadcastExcept(mid, client.getSessionId(), "webrtc:new_peer", newPeerPayload);

                        // ── 2. Send new user the list of existing peers for WebRTC ─
                        Map<String, Object> peerList = new HashMap<>();
                        peerList.put("peers", existingPeerIds);
                        client.sendEvent("webrtc:peer_list", peerList);

                        log.info("[WS] {} joined meeting {}. Existing peers: {}", data.getUserName(), mid, existingPeerIds);

                    } catch (Exception e) {
                        log.warn("[WS] announce error: {}", e.getMessage());
                    }
                });

        // ── meeting:media_state ───────────────────────────────────────────────
        server.addEventListener("meeting:media_state", MediaStatePayload.class,
                (DataListener<MediaStatePayload>) (client, data, ack) -> {
                    try {
                        Long mid = Long.parseLong(data.getMeetingId());
                        Map<String, Object> p = new HashMap<>();
                        p.put("userId", data.getUserId());
                        p.put("isAudioOn", data.isAudioOn());
                        p.put("isVideoOn", data.isVideoOn());
                        broadcastExcept(mid, client.getSessionId(), "meeting:media_state", p);
                    } catch (Exception e) { log.warn("[WS] media_state: {}", e.getMessage()); }
                });

        // ── chat:message ──────────────────────────────────────────────────────
        server.addEventListener("chat:message", ChatPayload.class,
                (DataListener<ChatPayload>) (client, data, ack) -> {
                    try {
                        Long mid = Long.parseLong(data.getMeetingId());
                        Map<String, Object> p = new HashMap<>();
                        p.put("id", data.getId()); p.put("userId", data.getUserId());
                        p.put("userName", data.getUserName()); p.put("text", data.getText());
                        p.put("timestamp", data.getTimestamp());
                        broadcastExcept(mid, client.getSessionId(), "chat:message", p);
                    } catch (Exception e) { log.warn("[WS] chat: {}", e.getMessage()); }
                });

        // ── WebRTC signalling relay ───────────────────────────────────────────
        // webrtc:join is NO LONGER NEEDED — peer list is sent in meeting:announce
        // Kept for backward compat but does nothing
        server.addEventListener("webrtc:join", Object.class,
                (DataListener<Object>) (client, data, ack) -> {
                    log.debug("[WebRTC] webrtc:join received (handled via announce now)");
                });

        server.addEventListener("webrtc:offer", WebRtcOfferPayload.class,
                (DataListener<WebRtcOfferPayload>) (client, data, ack) -> {
                    relaySdp("webrtc:offer", data.getMeetingId(), data.getTo(), data.getFrom(), data.getSdp());
                });

        server.addEventListener("webrtc:answer", WebRtcOfferPayload.class,
                (DataListener<WebRtcOfferPayload>) (client, data, ack) -> {
                    relaySdp("webrtc:answer", data.getMeetingId(), data.getTo(), data.getFrom(), data.getSdp());
                });

        server.addEventListener("webrtc:ice", WebRtcIcePayload.class,
                (DataListener<WebRtcIcePayload>) (client, data, ack) -> {
                    try {
                        Long mid = Long.parseLong(data.getMeetingId());
                        UUID targetSid = meetingUserSessions
                                .getOrDefault(mid, Collections.emptyMap()).get(data.getTo());
                        if (targetSid == null) return;
                        SocketIOClient target = server.getClient(targetSid);
                        if (target == null || !target.isChannelOpen()) return;
                        Map<String, Object> p = new HashMap<>();
                        p.put("from", data.getFrom());
                        p.put("candidate", data.getCandidate());
                        target.sendEvent("webrtc:ice", p);
                    } catch (Exception e) { log.warn("[WebRTC] ICE relay: {}", e.getMessage()); }
                });

        server.start();
        log.info("✅ Socket.IO server started on port {}", port);
    }

    // ── Send AI alert to host only ─────────────────────────────────────────────
    public void sendAlertToHost(Long meetingId, EventResponse alert) {
        UUID hostSid = hostSockets.get(meetingId);
        if (hostSid == null) return;
        SocketIOClient c = server.getClient(hostSid);
        if (c != null && c.isChannelOpen()) c.sendEvent("attention:alert", alert);
    }

    // ── Broadcast to everyone in meeting except sender ─────────────────────────
    private void broadcastExcept(Long meetingId, UUID exclude, String event, Object data) {
        Set<UUID> sessions = meetingSockets.get(meetingId);
        if (sessions == null) return;
        sessions.forEach(sid -> {
            if (sid.equals(exclude)) return;
            SocketIOClient c = server.getClient(sid);
            if (c != null && c.isChannelOpen()) c.sendEvent(event, data);
        });
    }

    public void broadcastToMeeting(Long meetingId, String event, Object data) {
        Set<UUID> sessions = meetingSockets.get(meetingId);
        if (sessions == null) return;
        sessions.forEach(sid -> {
            SocketIOClient c = server.getClient(sid);
            if (c != null && c.isChannelOpen()) c.sendEvent(event, data);
        });
    }

    private void relaySdp(String event, String meetingIdStr, Long to, Long from, String sdp) {
        try {
            Long mid = Long.parseLong(meetingIdStr);
            UUID targetSid = meetingUserSessions
                    .getOrDefault(mid, Collections.emptyMap()).get(to);
            if (targetSid == null) {
                log.warn("[WebRTC] relay target not found: userId={} in meeting={}", to, mid);
                return;
            }
            SocketIOClient target = server.getClient(targetSid);
            if (target == null || !target.isChannelOpen()) return;
            Map<String, Object> p = new HashMap<>();
            p.put("from", from); p.put("sdp", sdp);
            target.sendEvent(event, p);
            log.debug("[WebRTC] relayed {} from {} to {} in meeting {}", event, from, to, mid);
        } catch (Exception e) { log.warn("[WebRTC] relay error: {}", e.getMessage()); }
    }

    private ConnectListener onConnect() {
        return client -> log.info("[WS] Connected: {}", client.getSessionId());
    }

    private DisconnectListener onDisconnect() {
        return client -> {
            UUID sid = client.getSessionId();
            UserSession s = sessionUsers.remove(sid);
            if (s == null) return;
            Long mid = s.getMeetingId();
            Set<UUID>      sessions = meetingSockets.get(mid);
            Map<Long, UUID> userMap = meetingUserSessions.get(mid);
            if (sessions != null) sessions.remove(sid);
            if (userMap  != null) userMap.remove(s.getUserId());
            hostSockets.entrySet().removeIf(e -> e.getValue().equals(sid));

            Map<String, Object> leftPayload = new HashMap<>();
            leftPayload.put("userId",   s.getUserId());
            leftPayload.put("userName", s.getUserName());
            broadcastToMeeting(mid, "meeting:participant_left", leftPayload);
            broadcastToMeeting(mid, "webrtc:peer_left", Map.of("userId", s.getUserId()));

            log.info("[WS] {} left meeting {}", s.getUserName(), mid);
        };
    }

    @Bean
    public SocketIOServer socketIOServer() { return server; }
}