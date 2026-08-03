INSERT INTO e_env_ins (
    id_ins,
    va_typ,
    dh_str,
    dh_end,
    va_app,
    va_vrs,
    va_adr,
    va_env,
    va_os,
    va_re,
    va_usr,
    va_clr,
    va_brch,
    va_hsh,
    va_cnf,
    va_rsr,
    va_add_prp
)
VALUES (
           '632e5f0a-cccd-4384-a880-b99ef741b12b',
           'CLIENT',
           '2026-03-31 13:34:20.646',
           '2026-04-01 11:22:08.231',
           'inspect-app',
           '0.0.0',
           '93633160-72cd-4b82-ac15-e6e4cba707df',
           'ppd',
           'Windows 10.0',
           'Chrome',
           '',
           'inspect-ng-collector-0.0.1',
           '',
           '',
           '{
               "enabled": true,
               "scheduling": {
                   "interval": 20.000000000,
                   "state": "DISPATCH"
               },
               "monitoring": {
                   "httpRoute": {
                       "excludes": {
                           "path": ["scope"]
                       }
                   },
                   "resources": {
                       "enabled": true,
                       "disk": "/"
                   },
                   "exception": {
                       "maxStackTraceRows": 5,
                       "maxCauseDepth": -1
                   }
               },
               "tracing": {
                   "queueCapacity": 1000,
                   "remote": {
                       "@type": "02",
                       "host": "https://inspect-server-rec-asm.calamar.had.enedis.fr/",
                       "instanceURI": "v4/trace/instance",
                       "tracesURI": "v4/trace/instance/{id}/session",
                       "compressMinSize": 0,
                       "retentionMaxAge": 864000.000000000
                   },
                   "dump": {
                       "enabled": false,
                       "location": "file:///tmp/"
                   }
               },
               "debugMode": false
           }'::json,
           '{
               "minHeap": 0,
               "maxHeap": 4096,
               "diskTotalSpace": 0
           }'::json,
           '{
               "key1": "value1"
           }'::json
       );