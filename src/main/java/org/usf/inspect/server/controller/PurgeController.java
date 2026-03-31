package org.usf.inspect.server.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.usf.inspect.server.service.PurgeService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping(value = "purge", produces = APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class PurgeController {
    private final PurgeService purgeService;

    @DeleteMapping("batch")
    public void purge(){
        purgeService.launchPurge();
    }
}
