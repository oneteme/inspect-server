package org.usf.inspect.server.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.usf.inspect.server.service.PurgeService;

import java.time.Instant;
import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@CrossOrigin
@RestController
@RequestMapping(value = "purge/", produces = APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class PurgeController {

    private final PurgeService purgeService;

    @DeleteMapping("purgedata")
    public boolean purgeDate(
            @RequestParam(name = "start") Instant start,
            @RequestParam(name = "env") String environment,
            @RequestParam(required = false, name = "appname") List<String> appNameList,
            @RequestParam(required = false, name = "version") List<String> versionList
    ){
        return purgeService.purgeData(environment,appNameList,start,versionList);
    }
}
