package com.tulip.controller.api;

import com.tulip.service.VirtualTryOnService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/try-on")
public class TryOnApiController {

    @Autowired
    private VirtualTryOnService tryOnService;

    @PostMapping("/execute")
    public ResponseEntity<?> executeTryOn(@RequestBody Map<String, String> payload) {
        try {
            String resultUrl = tryOnService.tryOn(
                    payload.get("personUrl"),
                    payload.get("clothUrl"),
                    payload.get("category")
            );
            return ResponseEntity.ok(Map.of("success", true, "imageUrl", resultUrl));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}