package com.metapercept.fff_dita_converter.service;

import com.metapercept.fff_dita_converter.model.ResponseModel;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class LogStreamingService {

    private final List<SseEmitter> emitters = new ArrayList<>();

    public SseEmitter registerClient() {
        SseEmitter emitter = new SseEmitter();
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        return emitter;
    }

    private void sendEventToClients(String eventName, Object data) {
        List<SseEmitter> deadEmitters = new ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (IOException e) {
                deadEmitters.add(emitter);
            }
        }
        emitters.removeAll(deadEmitters);
    }

    public void logInfo(String message) {
        System.out.println(message);
        sendEventToClients("log", message);
    }

    public void logError(String message) {
        System.err.println(message);
        sendEventToClients("log", message);
    }

    public void logDebug(String message) {
        System.out.println(message);
        sendEventToClients("log", message);
    }

    public void streamResponse(ResponseModel response) {
        sendEventToClients("response", response);
    }
}
