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
@RequestMapping(value = "admin/", produces = APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class PurgeController {

    private final PurgeService purgeService;

    @DeleteMapping("purge/{env}")
    public boolean purgeDate(
            @RequestParam(name = "start") Instant start,
            @PathVariable(name = "env") String environment,
            @RequestParam(required = false, name = "appname") List<String> appNameList,
            @RequestParam(required = false, name = "version") List<String> versionList
    ){
        var purged = purgeService.purgeData(List.of(environment),appNameList,start,versionList);
        if(purged){
            purgeService.vaccum();
        }
        return purged;
    }
}
