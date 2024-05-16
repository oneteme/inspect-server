package org.usf.trace.api.server.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.usf.trace.api.server.service.JqueryRequestService;
import org.usf.traceapi.core.Session;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.File;
import java.io.IOException;
import java.util.List;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;


@Slf4j
@CrossOrigin
@RestController
@RequestMapping(value = "cachesave", produces = APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class CacheSaveController {
    @Autowired
    private ObjectMapper objectMapper = new ObjectMapper();
    private final JqueryRequestService service;


    @GetMapping("/execfile")
    public String saveByFile() {
        var size = 0;
        try {
            File file = new File("C:\\Users\\ad5d65bn.ZEPRODBUR\\Documents\\cache.json"); // set directory
            var  response = objectMapper.readValue(file, new TypeReference<List<Session>>(){});
            size = response.size();
            service.addSessions(response);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return size+"";
    }

    @GetMapping("/execrest")
    public String saveByCache() {
       return "";
    }
}


