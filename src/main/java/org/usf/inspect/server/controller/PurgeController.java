package org.usf.inspect.server.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.usf.inspect.server.service.PurgeService;

import java.time.Instant;
import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@RestController
@RequestMapping(value = "purge", produces = APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class PurgeController {
    private final PurgeService purgeService;

    @DeleteMapping("{env}")
    public void purge(
            @PathVariable(name = "env") String env,
            @RequestParam(name = "apps") List<String> apps,
            @RequestParam(name = "date_limit") Instant dateLimit
    ){
        purgeService.purge(env, apps, dateLimit);
    }

    @DeleteMapping("batch")
    public void purge(){
        purgeService.purge();
    }
}
